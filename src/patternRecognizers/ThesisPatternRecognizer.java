package patternRecognizers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;

import org.w3c.dom.Node;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import controller.Main;
import model.*;

//PatternRecognizer for unstructured bib. refs. that contain information relative to a thesis
public class ThesisPatternRecognizer implements PatternRecognizer {
	public boolean getData(Node n, Resource referenceList, String doi, NamespaceContext nsc) {
		// All the necessary information is declared at the beginning of the
		// function
		Model m = ModelFactory.createDefaultModel();
		Model model = referenceList.getModel();
		if (n == null || n.hasChildNodes() == false)
			return false;
		if (n.getAttributes().getNamedItem("id") == null) {
			return false;
		}
		String id = n.getAttributes().getNamedItem("id").getNodeValue();
		Resource bibRef = m.createResource(nsc.getNamespaceURI("doi") + doi + "#" + id, Main.biroBibliographicReference);
		Resource coItem = m.createResource(bibRef.getURI() + "-Item", Main.coItem);
		coItem.addProperty(Main.coItemContent, bibRef);
		bibRef.addLiteral(DC.identifier, id);
		String textRef = null;
		int numberOfOtherRef = 0;
		for (Node n1 : new MyNode(n)) {
			if (n1.getNodeName().equals("ce:label")) {
				if (MyNode.containsOnlyText(n1))
					bibRef.addLiteral(RDFS.label, n1.getTextContent().trim());
			}
			if (n1.getNodeName().equals("ce:other-ref")) {
				numberOfOtherRef++;
				textRef = n1.getTextContent().trim();
			}
		}
		if (numberOfOtherRef > 1)
			return false;
		if (textRef != null && textRef instanceof String)
			textRef = textRef.trim();
		else
			return false;

		// The bib. ref. is analysed only if it matches the given regex
		Thesis t = null;
		if (textRef.matches("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, [Pp][Hh]\\.[Dd]\\. (Thesis|THESIS|thesis), .+?, \\d{3,4}(\\.)?$"))
			t = Thesis.PHD;
		if (textRef.matches("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, Master.{1,4}(Thesis|THESIS|thesis), .+?, \\d{3,4}(\\.)?$"))
			t = Thesis.MASTERS;
		if (textRef.matches("^^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, [Dd]octoral (Thesis|THESIS|thesis), .+?, \\d{3,4}(\\.)?$"))
			t = Thesis.DOCTORAL;

		if (t != null) {
			try {
				// The title of the work is extracted
				String title = textRef.replaceFirst("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+", "");
				switch (t) {
				case PHD:
					title = title.replaceFirst(", [Pp][Hh]\\.[Dd]\\. (Thesis|THESIS|thesis), .+?, \\d{3,4}(\\.)?$", "").trim();
					break;
				case MASTERS:
					title = title.replaceFirst(", Master.{1,4}(Thesis|THESIS|thesis), .+?, \\d{3,4}(\\.)?$", "").trim();
					break;
				case DOCTORAL:
					title = title.replaceFirst(", [Dd]octoral (Thesis|THESIS|thesis), .+?, \\d{3,4}(\\.)?$", "").trim();
					break;
				}
				Resource work = m.createResource(URIManager.getWorkURI(title), Main.frbrWork);
				Resource expression = m.createResource(URIManager.getExpressionURI(title));
				work.addProperty(Main.frbrRealization, expression);
				expression.addLiteral(DC.title, title);
				bibRef.addProperty(Main.biroReferences, work);
				if (!TitleAndAuthorsPatternRecognizer.analyseAuthors(textRef, m, work))
					return false;

				String university = null, locationName = null;

				switch (t) {
				case PHD:
					expression.addProperty(RDF.type, Main.fabioPhDThesis);
					if (textRef.matches("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, [Pp][Hh]\\.[Dd]\\. (Thesis|THESIS|thesis), ((?!,).)+?, \\d{3,4}(\\.)?$")) {
						String s = textRef.replaceFirst("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, [Pp][Hh]\\.[Dd]\\. (Thesis|THESIS|thesis), ", "");
						s = s.replaceFirst(", \\d{3,4}(\\.)?$", "");
						if (s.toLowerCase().contains("univers") || s.toLowerCase().contains("institu") || s.toLowerCase().contains("colleg"))
							university = s;
					}
					if (textRef.matches("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, [Pp][Hh]\\.[Dd]\\. (Thesis|THESIS|thesis), ((?!,).)+?, ((?!,).)+?, \\d{3,4}(\\.)?$")) {
						String s = textRef.replaceFirst("^((\\p{Lu}\\.-?)+( \\p{L}+)+, )+.+?, [Pp][Hh]\\.[Dd]\\. (Thesis|THESIS|thesis), ", "");
						s = s.replaceFirst(", ((?!,).)+?, \\d{3,4}(\\.)?$", "");
						if (s.toLowerCase().contains("univers") || s.toLowerCase().contains("colleg")) {
							university = s;
							s = textRef.replaceFirst("^((\\p{Lu}\\.-?)+( \\p{L}+)+, )+.+?, [Pp][Hh]\\.[Dd]\\. (Thesis|THESIS|thesis), ((?!,).)+?, ", "");
							locationName = s.replaceFirst(", \\d{3,4}(\\.)?$", "");
						} else {
							s = textRef.replaceFirst("^((\\p{Lu}\\.-?)+( \\p{L}+)+, )+.+?, [Pp][Hh]\\.[Dd]\\. (Thesis|THESIS|thesis), ((?!,).)+?, ", "");
							s = s.replaceFirst(", \\d{3,4}(\\.)?$", "");
							if (s.toLowerCase().contains("univers") || s.toLowerCase().contains("institu") || s.toLowerCase().contains("colleg")) {
								university = s;
							}
						}
					}
					break;
				case MASTERS:
					expression.addProperty(RDF.type, Main.fabioMastersThesis);
					if (textRef.matches("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, Master.{1,4}(Thesis|THESIS|thesis), ((?!,).)+?, \\d{3,4}(\\.)?$")) {
						String s = textRef.replaceFirst("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, Master.{1,4}(Thesis|THESIS|thesis), ", "");
						s = s.replaceFirst(", \\d{3,4}(\\.)?$", "");
						if (s.toLowerCase().contains("univers") || s.toLowerCase().contains("institu") || s.toLowerCase().contains("colleg"))
							university = s;
					}
					if (textRef.matches("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, Master.{1,4}(Thesis|THESIS|thesis), ((?!,).)+?, ((?!,).)+?, \\d{3,4}(\\.)?$")) {
						String s = textRef.replaceFirst("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, Master.{1,4}(Thesis|THESIS|thesis), ", "");
						s = s.replaceFirst(", ((?!,).)+?, \\d{3,4}(\\.)?$", "");
						if (s.toLowerCase().contains("univers") || s.toLowerCase().contains("colleg")) {
							university = s;
							s = textRef.replaceFirst("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, Master.{1,4}(Thesis|THESIS|thesis), ((?!,).)+?, ", "");
							locationName = s.replaceFirst(", \\d{3,4}(\\.)?$", "");
						} else {
							s = textRef.replaceFirst("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, Master.{1,4}(Thesis|THESIS|thesis), ((?!,).)+?, ", "");
							s = s.replaceFirst(", \\d{3,4}(\\.)?$", "");
							if (s.toLowerCase().contains("univers") || s.toLowerCase().contains("institu") || s.toLowerCase().contains("colleg")) {
								university = s;
							}
						}
					}
					break;
				case DOCTORAL:
					expression.addProperty(RDF.type, Main.fabioPhDThesis);
					if (textRef.matches("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, [Dd]octoral (Thesis|THESIS|thesis), ((?!,).)+?, \\d{3,4}(\\.)?$")) {
						String s = textRef.replaceFirst("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, [Dd]octoral (Thesis|THESIS|thesis), ", "");
						s = s.replaceFirst(", \\d{3,4}(\\.)?$", "");
						if (s.toLowerCase().contains("univers") || s.toLowerCase().contains("institu") || s.toLowerCase().contains("colleg"))
							university = s;
					}
					if (textRef.matches("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, [Dd]octoral (Thesis|THESIS|thesis), ((?!,).)+?, ((?!,).)+?, \\d{3,4}(\\.)?$")) {
						String s = textRef.replaceFirst("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, [Dd]octoral (Thesis|THESIS|thesis), ", "");
						s = s.replaceFirst(", ((?!,).)+?, \\d{3,4}(\\.)?$", "");
						if (s.toLowerCase().contains("univers") || s.toLowerCase().contains("colleg")) {
							university = s;
							s = textRef.replaceFirst("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, [Dd]octoral (Thesis|THESIS|thesis), ((?!,).)+?, ", "");
							locationName = s.replaceFirst(", \\d{3,4}(\\.)?$", "");
						} else {
							s = textRef.replaceFirst("^((((\\p{Lu}\\.-?)+( (\\p{L}-?)+)+)|(et al)), )+.+?, [Dd]octoral (Thesis|THESIS|thesis), ((?!,).)+?, ", "");
							s = s.replaceFirst(", \\d{3,4}(\\.)?$", "");
							if (s.toLowerCase().contains("univers") || s.toLowerCase().contains("institu") || s.toLowerCase().contains("colleg")) {
								university = s;
							}
						}
					}
					break;
				}

				// The year is extracted and added as the year of publication of the
				// work and as the year of the conference
				Resource agent = null;
				if (university != null) {
					agent = m.createResource(URIManager.getAgentURI(university), FOAF.Organization);
					Resource publisher = m.createResource(URIManager.getPublisherURI(university), Main.proRoleInTime);
					publisher.addProperty(Main.proWithRole, Main.proPublisher);
					publisher.addProperty(Main.proRelatesToDocument, work);
					agent.addLiteral(FOAF.name, university);
					agent.addProperty(Main.proHoldsRoleInTime, publisher);
				}
				if (locationName != null && agent != null) {
					Resource location = m.createResource(URIManager.getLocationURI(locationName), Main.geoSpatialThing);
					location.addLiteral(DC.description, locationName);
					agent.addProperty(Main.geoLocation, location);
				}
				Matcher matcher = Pattern.compile("\\d{3,4}(\\.)?$").matcher(textRef);
				matcher.find();
				String year = matcher.group();
				matcher = Pattern.compile("\\d{3,4}").matcher(year);
				matcher.find();
				year = matcher.group();
				expression.addProperty(Main.fabioHasPublicationYear, year, XSDDatatype.XSDgYear);
				referenceList.addProperty(Main.coHasItem, coItem);
				model.add(m);
				return true;
			} catch (Exception e) {
				System.out.println(textRef);
				e.printStackTrace();
				return false;
			}
		} else
			return false;
	}

	public static boolean analyseYear(String textRef, Resource expression) {
		if (textRef.matches("^.+?\\d{3,4}(\\.)?$")) {
			try {
				Matcher matcher = Pattern.compile("\\d{3,4}(\\.)?$").matcher(textRef);
				if (!matcher.find())
					return false;
				String year = matcher.group();
				matcher = Pattern.compile("\\d{3,4}").matcher(year);
				matcher.find();
				year = matcher.group();
				expression.addProperty(Main.fabioHasPublicationYear, year, XSDDatatype.XSDgYear);
				return true;
			} catch (Exception e) {
				System.out.println();
				e.printStackTrace();
				return false;
			}
		} else
			return true;
	}

}

enum Thesis {
	BACHELOR, PHD, DOCTORAL, MASTERS
}

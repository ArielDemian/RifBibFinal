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
import com.hp.hpl.jena.vocabulary.RDFS;

import controller.Main;
import model.MyNode;
import model.PatternRecognizer;
import model.URIManager;

// PatternRecognizer for unstructured bib. refs. that contain information relative to a work produced during a conference
public class ConferenceBibRefRecognizer implements PatternRecognizer {
	public static final String[] months = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December" };

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
		if (textRef.matches("^(((((\\p{L}-?)+ )+(\\p{Lu}-?)+)|(et al))(,|\\.) )+.+?\\. In: ((?!(,|:)).)+?; \\d{3,4}(\\.)?$")) {
			try {
				// The title of the work is extracted
				String title = textRef.replaceFirst("^(((((\\p{L}-?)+ )+(\\p{Lu}-?)+)|(et al))(,|\\.) )+", "");
				title = title.replaceFirst("\\. In: .*?; \\d{3,4}(\\.)?$", "").trim();
				Resource work = m.createResource(URIManager.getWorkURI(title), Main.frbrWork);
				Resource expression = m.createResource(URIManager.getExpressionURI(title), Main.fabioConferencePaper);
				work.addProperty(Main.frbrRealization, expression);
				expression.addLiteral(DC.title, title);
				bibRef.addProperty(Main.biroReferences, work);
				if (!analyseAuthors(textRef, m, work))
					return false;
				// The name of the conference is extracted
				Matcher matcher = Pattern.compile("\\. In: .*?; \\d{3,4}(\\.)?$").matcher(textRef);
				matcher.find();
				String conferenceName = matcher.group();
				conferenceName = conferenceName.replaceFirst("^\\. In: ", "");
				conferenceName = conferenceName.replaceFirst("; \\d{3,4}(\\.)?$", "");
				Resource location = null;
				// The location of the conference is extracted if it is present
				if (conferenceName.matches("^.+?(,|\\.)( \\p{Lu}(\\p{Ll})+)+$")) {
					matcher = Pattern.compile("( \\p{Lu}(\\p{Ll})+)+$").matcher(conferenceName);
					matcher.find();
					String locationName = matcher.group().trim();
					location = m.createResource(URIManager.getLocationURI(locationName), Main.geoSpatialThing);
					location.addLiteral(DC.description, locationName);
					conferenceName = conferenceName.replaceFirst("(,|\\.)( \\p{Lu}(\\p{Ll})+)+$", "");
				}
				Resource conferenceProceedings = m.createResource(URIManager.getConferenceProceedingsURI(conferenceName), Main.fabioConferenceProceedings);
				conferenceProceedings.addProperty(DC.title, conferenceName);
				expression.addProperty(Main.frbrPartOf, conferenceProceedings);
				Resource conferenceEvent = m.createResource(URIManager.getConferenceEvent(conferenceName), Main.swcConferenceEvent);
				conferenceProceedings.addProperty(Main.swcRelatedToEvent, conferenceEvent);
				if (location != null)
					conferenceEvent.addProperty(Main.swcHasLocation, location);
				// The year is extracted and added as the year of publication of the
				// work and as the year of the conference
				matcher = Pattern.compile("\\d{3,4}(\\.)?$").matcher(textRef);
				if (matcher.find()) {
					String year = matcher.group().replaceFirst("\\.$", "").trim();
					expression.addProperty(Main.fabioHasPublicationYear, year, XSDDatatype.XSDgYear);
					conferenceEvent.addProperty(DC.date, year, XSDDatatype.XSDgYear);
				}
				referenceList.addProperty(Main.coHasItem, coItem);
				model.add(m);
				return true;
			} catch (Exception e) {
				System.out.println(textRef);
				e.printStackTrace();
				return false;
			}
		}
		if (textRef.matches("^(((((\\p{L}-?)+ )+(\\p{Lu}-?)+)|(et al))(,|\\.) )+.+?\\. [Pp]roceedings of.+?; \\d{4}(\\.)?$")) {
			try {
				String title = textRef.replaceFirst("^(((((\\p{L}-?)+ )+(\\p{Lu}-?)+)|(et al))(,|\\.) )+", "");
				title = title.replaceFirst("\\. [Pp]roceedings of.+?; \\d{4}(\\.)?$", "").trim();
				Resource work = m.createResource(URIManager.getWorkURI(title), Main.frbrWork);
				Resource expression = m.createResource(URIManager.getExpressionURI(title), Main.fabioConferencePaper);
				work.addProperty(Main.frbrRealization, expression);
				expression.addLiteral(DC.title, title);
				bibRef.addProperty(Main.biroReferences, work);
				if (!analyseAuthors(textRef, m, work))
					return false;
				Matcher matcher = Pattern.compile("\\. [Pp]roceedings of.+?; \\d{4}(\\.)?$").matcher(textRef);
				if (!matcher.find())
					return false;
				String conferenceName = matcher.group().trim();
				conferenceName = conferenceName.replaceFirst("^\\. ", "");
				conferenceName = conferenceName.replaceFirst("; \\d{4}(\\.)?$", "");
				Resource location = null;
				String month = null;
				if (conferenceName.matches("^[Pp]roceedings of.+((,|\\.)( \\p{Lu}(\\p{L}|'|’|-)+)+){1,2}$")) {
					matcher = Pattern.compile("((,|\\.)( \\p{Lu}(\\p{L}|'|’|-)+)+){1,2}$").matcher(conferenceName);
					if (matcher.find()) {
						String locationName = matcher.group();
						locationName = locationName.replaceFirst("^(,|\\.) ", "").trim();
						for (int i = 0; i < months.length; i++) {
							if (locationName.contains(months[i])) {
								month = "" + i;
								if (month.length() == 1)
									month = "0" + month;
								if (locationName.endsWith(months[i])) {
									matcher = Pattern.compile("(,( \\p{Lu}(\\p{L}|'|’|-)+)+){3}$").matcher(conferenceName);
									if (matcher.find()) {
										locationName = matcher.group();
										locationName = locationName.replaceFirst("^(,|\\.) ", "");
										locationName = locationName.replaceFirst("(,|\\.) " + months[i], "");
									}
									locationName = locationName.replaceFirst("(,|\\.) " + months[i], "");
								}
								break;
							}
						}
						location = m.createResource(URIManager.getLocationURI(locationName), Main.geoSpatialThing);
						location.addLiteral(DC.description, locationName);
						conferenceName = conferenceName.replaceFirst("((,|\\.)( \\p{Lu}(\\p{L}|'|’|-)+)+){1,2}$", "");
					}
				}
				Resource conferenceProceedings = m.createResource(URIManager.getConferenceProceedingsURI(conferenceName), Main.fabioConferenceProceedings);
				conferenceProceedings.addProperty(DC.title, conferenceName);
				expression.addProperty(Main.frbrPartOf, conferenceProceedings);
				Resource conferenceEvent = m.createResource(URIManager.getConferenceEvent(conferenceName));
				conferenceProceedings.addProperty(Main.swcRelatedToEvent, conferenceEvent);
				if (location != null)
					conferenceEvent.addProperty(Main.swcHasLocation, location);
				matcher = Pattern.compile("\\d{3,4}(\\.)?$").matcher(textRef);
				if (matcher.find()) {
					String year = matcher.group().replaceFirst("\\.$", "").trim();
					expression.addProperty(Main.fabioHasPublicationYear, year, XSDDatatype.XSDgYear);
					if (month != null)
						conferenceEvent.addProperty(DC.date, year + "-" + month, XSDDatatype.XSDgYearMonth);
					else
						conferenceEvent.addProperty(DC.date, year, XSDDatatype.XSDgYear);
				}
				referenceList.addProperty(Main.coHasItem, coItem);
				model.add(m);
				return true;
			} catch (Exception e) {
				System.out.println(textRef);
				e.printStackTrace();
				return false;
			}
		}
		return false;
	}

	public static boolean analyseAuthors(String textRef, Model m, Resource work) {
		try {
			Matcher matcher = Pattern.compile("^(((((\\p{L}-?)+ )+(\\p{Lu}-?)+)|(et al))(,|\\.) )+").matcher(textRef);
			if (!matcher.find())
				return false;
			String authors = matcher.group();
			String[] names = authors.split("(,|\\.)");
			// The names of the authors are extracted
			for (String s : names) {
				s = s.trim();
				if (s.matches("^((\\p{L}-?)+ )+(\\p{Lu}-?)+$")) {
					String[] nameParts = s.split(" ");
					String givenName = nameParts[nameParts.length - 1].trim();
					givenName = givenName.replace("", ".").substring(1);
					givenName = givenName.replace("-.", "-");
					String surname = "";
					String suffix = "";
					for (int i = 0; i < nameParts.length - 1; i++) {
						surname += nameParts[i] + " ";
					}
					surname = surname.trim();
					Resource author = m.createResource(URIManager.getAuthorURI(givenName, surname, suffix), Main.proRoleInTime);
					Resource person = m.createResource(URIManager.getPersonURI(givenName, surname, suffix), FOAF.Person);
					author.addProperty(Main.proWithRole, Main.proAuthor);
					author.addProperty(Main.proRelatesToDocument, work);
					person.addLiteral(FOAF.givenname, givenName + (suffix != "" ? " " + suffix : ""));
					person.addLiteral(Main.foafFamilyName, surname);
					person.addProperty(Main.proHoldsRoleInTime, author);
					work.addProperty(DC.creator, author);
				}
			}
		} catch (Exception e) {
			System.out.println(textRef);
			e.printStackTrace();
			return false;
		}
		return true;
	}
}

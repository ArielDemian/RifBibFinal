package patternRecognizers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;

import org.w3c.dom.Node;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDFS;

import controller.Main;
import model.*;

public class TitleAndAuthorsPatternRecognizer implements PatternRecognizer {

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
				int numberOfTextref = 0;
				for (Node n2 : new MyNode(n1)) {
					if (n2.getNodeName().equals("ce:textref")) {
						numberOfTextref++;
						textRef = n2.getTextContent().trim();
					}
				}
				if (numberOfTextref != 1)
					return false;
			}
		}
		if (numberOfOtherRef > 1)
			return false;
		if (textRef != null && textRef instanceof String)
			textRef = textRef.trim();
		else
			return false;

		// The bib. ref. is analysed only if it matches the given regex
		if (textRef.matches("^((((\\p{Lu}\\.-?)+( (\\p{L}(-|’)?)+)+( et al\\.)?,)|(et al\\.?,?)) )+“.+”.+$")) {
			try {
				Matcher matcher = Pattern.compile("“.+”").matcher(textRef);
				matcher.find();
				String title = matcher.group();
				title = title.replace("“", "");
				title = title.replace("”", "");
				Resource work = m.createResource(URIManager.getWorkURI(title), Main.frbrWork);
				Resource expression = m.createResource(URIManager.getExpressionURI(title), Main.frbrExpression);
				work.addProperty(Main.frbrRealization, expression);
				expression.addLiteral(DC.title, title);
				bibRef.addProperty(Main.biroReferences, work);
				if (!analyseAuthors(textRef, m, work))
					return false;
				ThesisPatternRecognizer.analyseYear(textRef, expression);
				referenceList.addProperty(Main.coHasItem, coItem);
				model.add(m);
				return true;
			} catch (Exception e) {
				System.out.println(textRef);
				e.printStackTrace();
				return false;
			}
		}

		if (textRef.matches("^((((\\p{Lu}\\.-?)+( (\\p{L}(-|’)?)+)+( et al\\.)?,)|(et al\\.?,?)) )+.+?, In: .+$")) {
			try {
				String title = textRef.replaceFirst("^((((\\p{Lu}\\.-?)+( (\\p{L}(-|’)?)+)+( et al\\.)?,)|(et al\\.?,?)) )+", "");
				title = title.replaceFirst(", In: .+$", "").trim();
				Resource work = m.createResource(URIManager.getWorkURI(title), Main.frbrWork);
				Resource expression = m.createResource(URIManager.getExpressionURI(title), Main.frbrExpression);
				work.addProperty(Main.frbrRealization, expression);
				expression.addLiteral(DC.title, title);
				bibRef.addProperty(Main.biroReferences, work);
				if (!analyseAuthors(textRef, m, work))
					return false;
				ThesisPatternRecognizer.analyseYear(textRef, expression);
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
		Matcher matcher = Pattern.compile("^((((\\p{Lu}\\.-?)+( (\\p{L}(-|’)?)+)+( et al\\.)?,)|(et al\\.?,?)) )+").matcher(textRef);
		if (!matcher.find())
			return false;
		String authors = matcher.group().replaceAll("et al\\.?", "");
		String[] names = authors.split(",");
		for (String s : names) {
			s = s.trim();
			if (s.matches("^(\\p{Lu}\\.-?)+( (\\p{L}(-|’)?)+)+$")) {
				String[] nameParts = s.split(" ");
				String givenName = nameParts[0].trim();
				String suffix = "";
				String surname = "";
				for (int i = 1; i < nameParts.length; i++) {
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
		return true;
	}

}
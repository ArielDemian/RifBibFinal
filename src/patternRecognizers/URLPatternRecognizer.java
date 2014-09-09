package patternRecognizers;

import javax.xml.namespace.NamespaceContext;

import org.w3c.dom.Node;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDFS;

import controller.Main;
import model.MyNode;
import model.PatternRecognizer;

import org.apache.commons.validator.routines.UrlValidator;

public class URLPatternRecognizer implements PatternRecognizer {
	public boolean getData(Node n, Resource referenceList, String doi, NamespaceContext nsc) {
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
		String[] schemes = { "http", "https", "ftp" };
		UrlValidator urlValidator = new UrlValidator(schemes);
		String linkText = null, entireText = null;
		int numberOfOtherRef = 0;
		for (Node n1 : new MyNode(n)) {
			if (n1.getNodeName().equals("ce:label")) {
				if (MyNode.containsOnlyText(n1))
					bibRef.addLiteral(RDFS.label, n1.getTextContent().trim());
			}
			if (n1.getNodeName().equals("ce:other-ref")) {
				numberOfOtherRef++;
				for (Node n2 : new MyNode(n1)) {
					if (n2.getNodeName().equals("ce:textref")) {
						entireText = n2.getTextContent().trim();
						for (Node n3 : new MyNode(n2)) {
							if (n3.getNodeName().equals("ce:inter-ref")) {
								Node xlink = n3.getAttributes().getNamedItem("xlink:href");
								if (xlink != null)
									linkText = xlink.getNodeValue();
							}
						}
					}
				}
			}
		}
		if (numberOfOtherRef > 1)
			return false;
		if (urlValidator.isValid(linkText)) {
			Resource work = m.createResource(linkText, Main.frbrWork);
			bibRef.addProperty(Main.biroReferences, work);
			work.addProperty(Main.prismURL, linkText, XSDDatatype.XSDanyURI);
			bibRef.addLiteral(RDFS.comment, entireText);
			ConferenceBibRefRecognizer.analyseAuthors(entireText, m, work);
			referenceList.addProperty(Main.coHasItem, coItem);
			model.add(m);
			return true;
		}
		return false;
	}
}

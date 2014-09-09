package patternRecognizers;

import javax.xml.namespace.NamespaceContext;

import org.w3c.dom.Node;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDFS;

import controller.Main;
import model.MyNode;
import model.PatternRecognizer;

public class NoPatternRecognizer implements PatternRecognizer {
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
		bibRef.addLiteral(DC.description, textRef);
		referenceList.addProperty(Main.coHasItem, coItem);
		model.add(m);
		return true;
	}

}
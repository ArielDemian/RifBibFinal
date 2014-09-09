package model;

import javax.xml.namespace.NamespaceContext;
import org.w3c.dom.Node;

import com.hp.hpl.jena.rdf.model.Resource;

// Interface for the definition of method required by a PatternRecognizer
public interface PatternRecognizer {
  public boolean getData(Node n, Resource referenceList, String doi, NamespaceContext nsc);
}

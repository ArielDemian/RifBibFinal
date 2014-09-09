package model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MyNamespaceContext implements NamespaceContext {
  private static final String DEFAULT_NS = "@default";
  private Map<String, String> map = new HashMap<String, String>();
  private static final String[] necessaryNamespaces = { "fabio", "biro", "frbr", "pro", "dc", "prism", "foaf", "rdfs", "xsd", "swc", "geo", "co", "rdf", "doco", "doi" };

  /**
   * This constructor parses the document and stores all namespaces it can find.
   * If toplevelOnly is true, only namespaces in the root are used.
   * 
   * @param document
   *          source document
   * @param toplevelOnly
   *          restriction of the search to enhance performance
   */
  public MyNamespaceContext(Document document, boolean toplevelOnly) {
    Node d = document.getFirstChild();
    examineNode(d, toplevelOnly);
  }

  /**
   * A single node is read, the namespace attributes are extracted and stored.
   * 
   * @param node
   *          to examine
   * @param attributesOnly
   *          , if true no recursion happens
   */
  private void examineNode(Node node, boolean toplevelOnly) {
    NamedNodeMap attributes = node.getAttributes();
    if (attributes != null)
      for (int i = 0; i < attributes.getLength(); i++) {
        Node attribute = attributes.item(i);
        storeAttribute(attribute);
      }

    if (!toplevelOnly) {
      NodeList children = node.getChildNodes();
      if (children != null)
        for (int i = 0; i < children.getLength(); i++) {
          Node child = children.item(i);
          if (child.getNodeType() == Node.ELEMENT_NODE)
            examineNode(child, false);
        }
    }
  }

  /**
   * This method looks at an attribute and stores it, if it is a namespace
   * attribute.
   * 
   * @param attribute
   *          to examine
   */
  private void storeAttribute(Node attribute) {
    // examine the attributes in namespace xmlns
    if (attribute.getNodeType() != Node.ATTRIBUTE_NODE)
      return;
    if (attribute.getNodeName().equals("xmlns"))
      put(DEFAULT_NS, attribute.getNodeValue());
    if (attribute.getNodeName().startsWith("xmlns:"))
      put(attribute.getNodeName().substring(6), attribute.getNodeValue());
  }

  private void put(String prefix, String uri) {
    if (!map.containsKey(prefix)) {
      map.put(prefix, uri);
    }
  }

  /**
   * This method is called by XPath. It returns the default namespace, if the
   * prefix is null or "".
   * 
   * @param prefix
   *          to search for
   * @return uri
   */
  public String getNamespaceURI(String prefix) {
    String result = null;
    if (prefix == null || prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
      result = map.get(DEFAULT_NS);
      return result;
    } else {
      result = map.get(prefix);
      return result;
    }
  }

  /**
   * This method is not needed in this context, but can be implemented in a
   * similar way.
   */
  public String getPrefix(String namespaceURI) {
    for (Map.Entry<String, String> e : map.entrySet())
      if (e.getValue().equals(namespaceURI))
        return e.getKey();
    return null;
  }

  public Iterator<String> getPrefixes(String namespaceURI) throws IllegalArgumentException {
    if (namespaceURI == null)
      throw new IllegalArgumentException();
    List<String> l = new LinkedList<String>();
    for (Map.Entry<String, String> e : map.entrySet()) {
      if (e.getValue().equals(namespaceURI))
        l.add(e.getKey());
    }
    return l.iterator();
  }

  // True if and only if all the required namespaces are set
  public boolean hasAllRequiredNamespaces() {
    boolean[] nnm = new boolean[necessaryNamespaces.length];
    Arrays.fill(nnm, false);
    for (int i = 0; i < necessaryNamespaces.length; i++) {
      if (this.getNamespaceURI(necessaryNamespaces[i]) != null)
        nnm[i] = true;
    }
    for (boolean b : nnm)
      if (!b)
        return false;
    return true;
  }

  // Returns a printable string containing all the set pairs of prefix and URL
  public String getAllNamespaces() {
    String result = "";
    for (Map.Entry<String, String> e : this.map.entrySet()) {
      result += e.getKey() + " --- " + e.getValue() + "\n";
    }
    return result;
  }

  public Map<String, String> getMap() {
    return map;
  }

  //Returns a printable string containing the prefixes for all the required namespaces
  public static String getNecessaryNamespaces() {
    String result = "\"";
    for (String s : necessaryNamespaces)
      result += s + " ";
    result = (result.trim()) + "\"";
    return result;
  }
}
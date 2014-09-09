package model;

import java.util.Iterator;

import org.w3c.dom.Node;
import org.w3c.dom.Text;

// Class created to allow easy iteration over child nodes of node
public class MyNode implements Iterable<Node> {
	private Node n;

	public MyNode(Node n) {
		this.n = n;
	}

	public Iterator<Node> iterator() {
		return new MyNodeIterator(n);
	}

	public static boolean containsOnlyText(Node n) {
		if (!(n.getFirstChild() instanceof Text) || n.getChildNodes().getLength() > 1)
			return false;
		return true;
	}
}

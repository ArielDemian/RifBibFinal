package model;

import java.util.Iterator;

import org.w3c.dom.Node;

// Class created to iterate over child nodes
public class MyNodeIterator implements Iterator<Node> {
	private Node n;
	private int i;

	public MyNodeIterator(Node n) {
		this.n = n;
		i = -1;
	}

	public boolean hasNext() {
		if (n.getChildNodes().item(i + 1) == null)
			return false;
		return true;
	}

	public Node next() {
		Node next = n.getChildNodes().item(++i);
		if (next == null) {
			i--;
		}
		return next;
	}

	public void remove() {
		// not implemented because not necessary
	}

}

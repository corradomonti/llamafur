package efen.scorers.llamafur.data;

import java.util.Comparator;

public class Element implements Comparable<Element> {
	public int i, j;
	public double value;
	
	public Element(int i, int j, double x) {
		this.i = i; this.j = j; this.value = x;
	}
	
	public String toString() {
		return i + ", " + j + " -> " + value;
	}
	
	@Override
	public int compareTo(Element e) {
		return Double.compare(value, e.value);
	}
	
	public static Comparator<Element> absoluteComparator() {
		return new Comparator<Element>() { 
			public int compare(Element e1, Element e2) {
				return Double.compare(Math.abs(e1.value), Math.abs(e2.value));
			}
		};
	}

}

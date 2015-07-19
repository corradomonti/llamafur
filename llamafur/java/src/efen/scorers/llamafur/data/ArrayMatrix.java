package efen.scorers.llamafur.data;

import org.apache.commons.math3.util.MathArrays;

import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.doubles.DoubleHeapPriorityQueue;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class ArrayMatrix extends Matrix {
	private static final long serialVersionUID = 1L;
	
	private final double[][] m;
	private final int size;
	
	public ArrayMatrix(int size, double initialValue) {
		this.size = size;
		m = new double[size][];
		for (int i = 0; i < size; i++) {
			m[i] = new double[size];
			DoubleArrays.fill(m[i], initialValue);
		}
	}
	
	public ArrayMatrix(int size) {
		this(size, 0);
	}
	
	@Override
	public void addTo(int i, int j, double dx) {
		m[i][j]+=dx;
	}

	@Override
	public int computeSize() {
		return size;
	}

	@Override
	public ObjectIterator<Element> entries() {
		return new ObjectIterator<Element>() {
			int i = size - 1, j = size - 1;
			public int skip(int n) { throw new UnsupportedOperationException(); }
			public void remove() { throw new UnsupportedOperationException(); }
			
			public Element next() {
				i--;
				if (i < 0) {
					i = size - 1;
					j--;
				}
				return new Element(i, j, m[i][j]);
			}
			public boolean hasNext() {
				return i != 0 || j != 0;
			}
			
		};
	}

	@Override
	public DoubleIterator values() {
		return new DoubleIterator() {
			private int i = size - 1, j = size - 1;
			public Double next() { return Double.valueOf(nextDouble()); }
			public int skip(int n) { throw new UnsupportedOperationException(); }
			public void remove() { throw new UnsupportedOperationException(); }
			
			public double nextDouble() {
				i--;
				if (i < 0) {
					i = size - 1;
					j--;
				}
				return m[i][j];
			}
			public boolean hasNext() {
				return i != 0 || j != 0;
			}
			
		};
	}
	
	@Override
	public double get(int i, int j) {
		return m[i][j];
	}

	@Override
	public void multiplyBy(double k) {
		for (int i = 0; i < size; i++)
			for (int j = 0; j < size; j++)
				m[i][j] *= k;
				
	}
	
	@Override
	public void put(int i, int j, double x) {
		m[i][j] = x;
	}

	@Override
	public void truncateTo(int minimumNumberOfElementsToKeep) {
		DoubleHeapPriorityQueue heap = new DoubleHeapPriorityQueue();
		
		for (int i = 0; i < size; i++)
			for (double x : m[i]) {
				heap.enqueue(Math.abs(x));
				if (heap.size() > minimumNumberOfElementsToKeep)
					heap.dequeueDouble();
			}
		
		double threshold = heap.dequeueDouble();
		
		for (double[] row : m)
			for (int i = 0; i < size; i++)
				if (Math.abs(row[i]) < threshold)
					row[i] = 0;
	}
	
	@Override
	public Int2DoubleMap getRow(int i) {
		int[] ids = MathArrays.natural(m.length);
		return new Int2DoubleOpenHashMap(ids, m[i]);
	}

}

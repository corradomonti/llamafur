package efen.scorers.llamafur.data;

import it.unimi.dsi.fastutil.doubles.DoubleHeapPriorityQueue;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LongBasedMatrix extends Matrix {
	public static float LOAD_FACTOR = 1.0f;
	
	private static Logger LOGGER = LoggerFactory.getLogger(LongBasedMatrix.class);
	private static final long serialVersionUID = 1L;
	
	private final Long2DoubleOpenHashMap m;
	
	public LongBasedMatrix(int initialSize) {
		m = new Long2DoubleOpenHashMap(initialSize, LOAD_FACTOR);
		m.defaultReturnValue(0);
	}
	
	public LongBasedMatrix() {
		this(Long2DoubleOpenHashMap.DEFAULT_INITIAL_SIZE);
	}
	
	public void trim(int n) {
		m.trim(n);
	}
	
	private long index(int i, int j) {
		return ((long) i << 32) + j;
	}

	@Override
	public DoubleIterator values() {
		return m.values().iterator();
	}
	
	class ElementIterator implements ObjectIterator<Element> {
		ObjectIterator<Entry> i = m.long2DoubleEntrySet().fastIterator();
		public void remove() { throw new UnsupportedOperationException(); }
		public int skip(int i) { throw new UnsupportedOperationException(); }

		@Override
		public boolean hasNext() {
			return i.hasNext();
		}

		@Override
		public Element next() {
			Entry next = i.next();
			long nextKey = next.getLongKey();
			return new Element((int) (nextKey >> 32), (int) nextKey, next.getDoubleValue());
		}

		
	}
	
	@Override
	public ObjectIterator<Element> entries() {
		return new ElementIterator();
	}

	@Override
	public double get(int i, int j) {
		return m.get(index(i,j));
	}


	@Override
	public void addTo(int i, int j, double dx) {
		m.addTo(index(i,j), dx);
	}
	
	@Override
	public void put(int i, int j, double x) {
		if (x == 0)
			m.remove(index(i,j));
		else
			m.put(index(i,j), x);
	}

	@Override
	public int computeSize() {
		int max = 0;
		int i;
		for (long l : m.keySet()) {
			i = (int) (l >> 32);
			if (i > max)
				max = i;
			i = (int) l;
			if (i > max)
				max = i;
		}
		return max;
	}
	
	@Override
	public void multiplyBy(double k) {
		if (k == 1)
			return;
		
		if (Double.isInfinite(k) || Double.isNaN(k))
			throw new ArithmeticException("Normalizing a matrix by " + k);
		
		if (k == 0) {
			LOGGER.warn("A matrix has been multiplied by 0.");
			m.clear();
		}
		
		for (long key : m.keySet()) {
			m.put(key, m.get(key) * k);
		}
	}
	
	@Override
	public void truncateTo(int minimumNumberOfElementsToKeep) {
		if (m.size() < minimumNumberOfElementsToKeep)
			return;
		
		DoubleHeapPriorityQueue heap = new DoubleHeapPriorityQueue();
		
		DoubleIterator values = values();
		while (values.hasNext()) {
				heap.enqueue(Math.abs(values.nextDouble()));
				if (heap.size() > minimumNumberOfElementsToKeep)
					heap.dequeueDouble();
			}
		
		double threshold = heap.dequeueDouble();
		
		DoubleIterator iterator = m.values().iterator();
		while (iterator.hasNext()) {
			if (Math.abs(iterator.nextDouble()) < threshold)
				iterator.remove();
		}
	}




}

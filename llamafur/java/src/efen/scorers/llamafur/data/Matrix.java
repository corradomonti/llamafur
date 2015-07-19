package efen.scorers.llamafur.data;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.logging.ProgressLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class Matrix implements Serializable {
	final public static Logger LOGGER = LoggerFactory.getLogger(Matrix.class);
	
	private static final long serialVersionUID = 1L;
	
	public abstract ObjectIterator<Element> entries();
	public abstract DoubleIterator values();
	
	public abstract void addTo(int i, int j, double dx);
	public abstract void put(int i, int j, double x);
	public abstract double get(int i, int j);
	public abstract void multiplyBy(double k);
	
	public abstract int computeSize();
	public abstract void truncateTo(int minimumNumberOfElementsToKeep);
	
	public int numberOfNonZeroEntries() {
		int n = 0;
		DoubleIterator values = values();
		while (values.hasNext())
			if (values.nextDouble() != 0)
				n++;
		return n;
	}
	
	public void normalizeBy(double norm) {
		if (norm == 0)
			throw new ArithmeticException("Normalizing a matrix by " + norm);
		
		multiplyBy(1.0 / norm);
	}
	
	public double maxNorm() {
		double max = 0;
		DoubleIterator values = values();
		while (values.hasNext())
			max = Math.max(max, Math.abs(values.nextDouble()));
		return max;
	}
	
	public double norm2() {
		double sumOfSquares = 0;
		DoubleIterator values = values();
		double v;
		while (values.hasNext()) {
			v = values.nextDouble();
			sumOfSquares += v * v;
		}
		return Math.sqrt(sumOfSquares);
	}
	
	public double norm1() {
		double sum = 0;
		DoubleIterator values = values();
		while (values.hasNext())
			sum += Math.abs(values.nextDouble());
		return sum;
	}
	
	@SuppressWarnings("boxing")
	public String toString() {
		StringBuilder s = new StringBuilder();
		int size = computeSize();
		
		if (size < 20)
			for (int i = 0; i < size; i++) {
				for (int j = 0; j < size; j++) {
					double value = get(i,j);
					s.append(String.format("%5.2f", value) +  "   ");
				}
				
				s.append('\n');
			}
		else {
			ObjectIterator<Element> elements = entries();
			while (elements.hasNext()) {
				s.append(elements.next());
				s.append("\n");
			}
		}
		
		return s.toString();
	}
	
	public Properties stats() {
		Properties stats = new Properties();
		stats.setProperty("size", ""+computeSize());
		stats.setProperty("nonzero elements", ""+numberOfNonZeroEntries());
		stats.setProperty("max norm", ""+maxNorm());
		stats.setProperty("norm1", ""+norm1());
		stats.setProperty("norm2", ""+norm2());
		return stats;
	}
	
	public void toMatrixMarketFormat(Writer writer) throws IOException {
		writer.write("%%MatrixMarket matrix coordinate real general\n");
		
		int entriesToWrite = numberOfNonZeroEntries();
		
		if (entriesToWrite == 0)
			throw new RuntimeException("Empty matrix. Aborting Matrix Market export.");
		
		int size = computeSize();
		
		writer.write(size + " " + size + " " + entriesToWrite + "\n");
		
		ObjectIterator<Element> elements = entries();
		
		ProgressLogger pl = new ProgressLogger(LOGGER, "elements");
		pl.expectedUpdates = entriesToWrite;
		pl.start("Exporting matrix to Matrix Market format...");
		
		while (elements.hasNext()) {
				Element e = elements.next();
				if (e.value != 0) {
					writer.write((e.i+1) + " " + (e.j+1) + " " + e.value + "\n");
					entriesToWrite--;
					pl.lightUpdate();
				}
			}
		
		pl.done();
			
		if (entriesToWrite != 0)
			throw new IllegalStateException();
	}
	
	public void toMatrixMarketFormat(File file) throws IOException {
		FileWriter writer = new FileWriter(file);
		this.toMatrixMarketFormat(writer);
		writer.close();
	}
	
	public void add(Matrix m) {
		ObjectIterator<Element> entries = m.entries();
		while (entries.hasNext()) {
			Element e = entries.next();
			this.addTo(e.i, e.j, e.value);
		}
	}
	
	public Int2DoubleMap getRow(int i) {
		int size = computeSize();
		Int2DoubleOpenHashMap row = new Int2DoubleOpenHashMap(size);
		for (int j = 0; j < size; j++)
			row.put(j, get(i, j));
		return row;
	}
	
}

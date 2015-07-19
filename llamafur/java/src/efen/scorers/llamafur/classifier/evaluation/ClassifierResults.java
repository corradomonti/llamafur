package efen.scorers.llamafur.classifier.evaluation;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class ClassifierResults extends Object2DoubleOpenHashMap<Stats> {
	private static final long serialVersionUID = 1L;
	
	private long tp = 0, tn = 0, fp = 0, fn = 0;

	public void update(boolean label, boolean predicted) {
		if (label) {
			if (predicted) tp++;
			else fn++;
		} else {
			if (predicted) fp++;
			else tn++;
		}
	}
	
	public double compute(Stats stat) {
		return stat.compute(tp, tn, fp, fn);
	}
	
	public void computeAllStats() {
		for (Stats stat : Stats.values())
			try {
				super.put(stat, stat.compute(tp, tn, fp, fn));
			} catch (ArithmeticException e) {
				super.put(stat, Double.NaN);
			}
	}
	
	public String confusionMatrix() {
		int space = (int) Math.log10(tp+tn+fp+fn) + 3;
		StringBuilder s = new StringBuilder();
		
		// header
		s.append("___|");
		s.append(StringUtils.center("T", space, '_'));
		s.append('|');
		s.append(StringUtils.center("F", space, '_'));
		s.append('|');
		s.append('\n');
		
		// first row: tp, fp
		s.append("P  |");
		s.append(StringUtils.center(Long.toString(tp), space));
		s.append('|');
		s.append(StringUtils.center(Long.toString(fp), space));
		s.append('|');
		s.append('\n');
		
		// second row: tn, fn
		s.append("N  |");
		s.append(StringUtils.center(Long.toString(tn), space));
		s.append('|');
		s.append(StringUtils.center(Long.toString(fn), space));
		s.append('|');
		s.append('\n');
		
		return s.toString();
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(StringUtils.center("Confusion Matrix", 30, '=') + '\n');
		s.append(confusionMatrix() + '\n');

		s.append(StringUtils.center("Stats", 30, '=') + '\n');
		for (Stats stat : keySet()) {
			s.append(StringUtils.rightPad(stat.name(), 18) + ":\t");
			s.append(String.format("%5.4f", get(stat)));
			s.append('\n');
		}
		
		return s.toString();
	}
	
	@SuppressWarnings("boxing")
	public static String avg(Collection<ClassifierResults> results) {
		if (results.size() == 1) return results.iterator().next().toString();
		StringBuilder s = new StringBuilder();
		
		for (Stats stat : Stats.values()) {
			SummaryStatistics summary = new SummaryStatistics();
			for (ClassifierResults t : results)
				summary.addValue(t.getDouble(stat));
			s.append(StringUtils.rightPad(stat.name(), 18) + ":\t");
			s.append(String.format("%5.4f", summary.getMean()));
			s.append(" +-" + String.format("%5.4f", summary.getStandardDeviation()));
			s.append('\n');
		}
		
		return s.toString();
	}
	
}

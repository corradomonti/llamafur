package efen.scorers.llamafur.classifier;
import it.unimi.dsi.fastutil.ints.IntSet;
import efen.scorers.llamafur.classifier.evaluation.ClassifierResults;
import efen.scorers.llamafur.classifier.evaluation.Stats;
import efen.scorers.llamafur.data.LongBasedMatrix;
import efen.scorers.llamafur.data.Matrix;


public class PAClassifier implements Classifier {
	public final double C = 1.5;
	private final Matrix w;
	private ClassifierResults stats;
	
	public PAClassifier() {
		this(new LongBasedMatrix());
	}
	
	public PAClassifier(Matrix init) {
		w = init;
		stats = new ClassifierResults();
	}

	private double margin(IntSet nodeI, IntSet nodeJ) {
		return margin(nodeI, nodeJ, 1.0 / (nodeI.size() * nodeJ.size()) );
	}
	
	private double margin(IntSet nodeI, IntSet nodeJ, double invNorm) {
		double sum = 0;
		for (int a : nodeI)
			for (int b : nodeJ)
				sum += w.get(a, b);
		
		return sum * invNorm;
	}
	

	public void learn(IntSet nodeIFeatures, IntSet nodeJFeatures, boolean isArc) {
		double invNorm = 1.0 / Math.sqrt(nodeIFeatures.size() * nodeJFeatures.size());
		double margin = margin(nodeIFeatures, nodeJFeatures, invNorm);
		double update = Math.min(C, 1.0 - (isArc ? margin : - margin))
			* (isArc? invNorm : -invNorm);
		
		stats.update(isArc, margin > 0);
		
		for (int a : nodeIFeatures)
			for (int b : nodeJFeatures)
				w.addTo(a, b, update);
	}
	
	public boolean predict(IntSet nodeIFeatures, IntSet nodeJFeatures) {
		return margin(nodeIFeatures, nodeJFeatures) > 0;
	}
	
	public Matrix getEstimatedWeights() {
		return w;
	}
	
	public ClassifierResults currentResults() {
		return stats;
	}
	
	public Object shortStats() {
		return new Object() {
			public String toString() {
				return (int) (100 * stats.compute(Stats.ACCURACY)) + "% accuracy.";
			}
		};
	}
}

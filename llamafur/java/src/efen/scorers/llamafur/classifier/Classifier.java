package efen.scorers.llamafur.classifier;
import it.unimi.dsi.fastutil.ints.IntSet;
import efen.scorers.llamafur.classifier.evaluation.ClassifierResults;
import efen.scorers.llamafur.data.Matrix;

public interface Classifier {

	public abstract void learn(IntSet nodeIFeatures, IntSet nodeJFeatures,
			boolean isArc);

	public abstract boolean predict(IntSet nodeIFeatures, IntSet nodeJFeatures);

	public abstract Matrix getEstimatedWeights();
	
	public abstract Object shortStats();
	
	public ClassifierResults currentResults();

}
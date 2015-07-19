package efen.scorers.combinated;

import com.martiansoftware.jsap.ParseException;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import efen.UnexpectednessScorer;
import efen.evaluation.PoolSpecification;

public class LinearCombinationScorer extends UnexpectednessScorer {
	private final UnexpectednessScorer aScorer, bScorer;
	private final double alpha, oneMinusAlpha;
	
	public LinearCombinationScorer(String scorerA, String scorerB, String valueA) throws ParseException {
		this(
				(UnexpectednessScorer) PoolSpecification.SCORER_PARSER.parse(scorerA), 
				(UnexpectednessScorer) PoolSpecification.SCORER_PARSER.parse(scorerB),
				Double.parseDouble(valueA)
		);
	}
	
	public LinearCombinationScorer(UnexpectednessScorer scorerA, UnexpectednessScorer scorerB, double alpha) {
		super(scorerA.graph);
		if (!scorerB.graph.basename().equals(scorerA.graph.basename()))
			if (!scorerB.graph.equals(scorerA.graph))
				throw new IllegalArgumentException(scorerA + " and " + scorerB + " do not deal with the same graph.");
		this.aScorer = scorerA;
		this.bScorer = scorerB;
		this.alpha = alpha;
		oneMinusAlpha = 1 - alpha;
	}
	
	public LinearCombinationScorer(UnexpectednessScorer scorerA,UnexpectednessScorer scorerB) {
		this(scorerA, scorerB, 0.5);
	}

	@Override
	public Int2DoubleMap scores(int docI) {
		Int2DoubleMap aScores = aScorer.scores(docI);
		Int2DoubleMap bScores = bScorer.scores(docI);
		
		IntSet succs = super.successors(docI);
		Int2DoubleMap scores = new Int2DoubleOpenHashMap(succs.size());
		for (int succ : succs) {
			scores.put(succ, alpha * aScores.get(succ) + oneMinusAlpha * bScores.get(succ));
		}
		return scores;
	}
	

	
	public String toString() {
		if (alpha == 0.5)
			return aScorer.toString() + "+" + bScorer.toString();
		else
			return (alpha == .1 ? "" : alpha*10 + " ") + aScorer.toString() + "+"
				+ (oneMinusAlpha == .1 ? "" : oneMinusAlpha*10 + " ") + bScorer.toString() + "";
	}
	

}

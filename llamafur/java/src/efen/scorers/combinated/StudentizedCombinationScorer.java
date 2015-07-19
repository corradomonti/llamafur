package efen.scorers.combinated;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.IOException;
import java.util.Properties;

import com.martiansoftware.jsap.ParseException;

import efen.UnexpectednessScorer;
import efen.evaluation.PoolSpecification;
import efen.scorers.ScorerStatisticsSummarizer;

public class StudentizedCombinationScorer extends LinearCombinationScorer {

	static class StudentizedScorer extends UnexpectednessScorer {

		private double mean, stddev;
		private final UnexpectednessScorer scorer;
		
		public StudentizedScorer(UnexpectednessScorer scorer, Properties stats) {
			super(scorer.graph);
			this.scorer = scorer;
			
			this.mean = Double.parseDouble(stats.getProperty("mean"));
			this.stddev = Double.parseDouble(stats.getProperty("standard-deviation"));
		}
		
		@Override
		public Int2DoubleMap scores(int docI) {
			Int2DoubleMap originalScores = scorer.scores(docI);
			ObjectIterator<Entry> iterator = originalScores.int2DoubleEntrySet().iterator();
			Int2DoubleMap newScores = new Int2DoubleOpenHashMap(originalScores.size());
			for (Entry entry = iterator.next(); iterator.hasNext(); entry = iterator.next()) {
				newScores.put(entry.getIntKey(), 
						(entry.getDoubleValue() - mean) / stddev
						);
			}
			return newScores;
		}
		
		public String toString() {
			return "Normalized" + scorer.toString();
		}

	}
	
	public StudentizedCombinationScorer(
			String scorerSpec1, String scorerSpec2, String statistics1, String statistics2
	) throws ParseException, ClassNotFoundException, IOException {
		this( 
		(UnexpectednessScorer) PoolSpecification.SCORER_PARSER.parse(scorerSpec1),
		(UnexpectednessScorer) PoolSpecification.SCORER_PARSER.parse(scorerSpec2),
		ScorerStatisticsSummarizer.load(statistics1),
		ScorerStatisticsSummarizer.load(statistics2)
				);
	}

	public StudentizedCombinationScorer(
			UnexpectednessScorer scorer1, UnexpectednessScorer scorer2,
			Properties stat1, Properties stat2
	) {
		super(new StudentizedScorer(scorer1, stat1), new StudentizedScorer(scorer2, stat2));
	}
	

}

package efen.evaluation.measure;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.webgraph.ImmutableGraph;
import utils.CommandLine;
import utils.MapUtils;
import efen.UnexpectednessScorer;
import efen.evaluation.GroundTruth;
import efen.evaluation.PoolSpecification;
import efen.scorers.RandomScorer;

public class PrecisionRecallPlot extends AbstractMeasure {
	
	static final double[] NORM_CUTOFFS = {.01, .025, .05, .08, 0.1, .15, .25, .5, 1};
	private final ImmutableGraph checkEveryResultIsAnOutlinkInThisGraph;
	
	@SuppressWarnings("unchecked")
	@CommandLine(argNames = { "poolSpecificationFile", "evaluationDataset", "name2id"})
	public PrecisionRecallPlot(String poolSpecificationFile, String datasetPath, Object2IntMap<String> name2id)
				throws Exception {
		super(
				new PoolSpecification(poolSpecificationFile),
				GroundTruth.fromUTF8FilePath(datasetPath, name2id)
		);
		
		this.checkEveryResultIsAnOutlinkInThisGraph = super.graph;
		
		groundtruth.checkAgreementWith(super.graph, (Int2ObjectMap<String>) MapUtils.invert(name2id));
	}
	
	public double computeForQuery(int query, UnexpectednessScorer retriever) {
		IntSet relevants = groundtruth.getRelevants(query);
		IntSet evaluated = groundtruth.getEvaluatedDocs(query);
		
		int[] results = retriever.results(query);
		assert results.length > 0;
		
		if (checkEveryResultIsAnOutlinkInThisGraph != null) {
			IntSet outlinks = new IntOpenHashSet(checkEveryResultIsAnOutlinkInThisGraph.successorArray(query), 0, checkEveryResultIsAnOutlinkInThisGraph.outdegree(query));
			outlinks.rem(query); // avoid self-loops
			IntOpenHashSet resultsSet = new IntOpenHashSet(results);
			if (!outlinks.equals(resultsSet))
				throw new IllegalStateException(
						"Results of " + retriever + " for query " + query + 
						" don't match with outlinks: \n" +
						resultsSet + " are results, but outlinks are " + outlinks);
		}
		
		int[] cutoffs = new int[NORM_CUTOFFS.length];
		for (int i = 0; i < cutoffs.length; i++)
			cutoffs[i] = (int) Math.ceil(NORM_CUTOFFS[i] * results.length) - 1;
		
		assert cutoffs[cutoffs.length - 1] == results.length - 1;
		
		int nRelevantRetrieved = 0, nEvaluated = 0, iCutoff = 0;
		for (; cutoffs[iCutoff] == 0; iCutoff++ );
		double avgPrecision = 0;
		
		int i;
		for (i = 0; i < results.length && iCutoff < cutoffs.length; i++) {
			if (relevants.contains(results[i]))
				nRelevantRetrieved++;
			
			if (evaluated.contains(results[i]))
				nEvaluated++;

			while (i == cutoffs[iCutoff]) {
				
				if (nEvaluated == 0) {
					if (!(retriever instanceof RandomScorer))
						LOGGER.warn(retriever + " has no evaluated documents in its top-"+(i+1)+" results for query '"+query(query)+"'. Is this ok?");
					iCutoff++;
				} else {
					double precision = (double) nRelevantRetrieved / nEvaluated;
					double recall = (double) nRelevantRetrieved / relevants.size();
					printResult(
								this + "\t" + 
								retriever + "\t" + 
								query(query) + "\t" + 
								NORM_CUTOFFS[iCutoff] + "\t" + 
								precision + "\t" + 
								recall
					);
					avgPrecision += precision;
					iCutoff++;
					if (iCutoff >= cutoffs.length)
						break;
				}
			}
		}
		if (NORM_CUTOFFS[NORM_CUTOFFS.length - 1] == 1.0)
			assert i == results.length;
		assert iCutoff == cutoffs.length;
		
		if (!(nEvaluated == evaluated.size()))
				LOGGER.warn("For query " + query + ", retriever " + retriever + 
						" has returned "  + nEvaluated + " evaluated results, "
					  + "but the number of evaluated results was " + evaluated.size());
		
		return avgPrecision / cutoffs.length;
	}
	
	public String toString() {
		return "PRECREC";
	}
	
	

	public static void main(String[] args) throws Exception {
		AbstractMeasure.main(args, PrecisionRecallPlot.class);
	}

	
}

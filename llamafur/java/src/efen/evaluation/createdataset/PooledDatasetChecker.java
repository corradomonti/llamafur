package efen.evaluation.createdataset;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.CommandLine;
import utils.JsapUtils;
import utils.JsapUtils.JsapResultsWithObject;
import utils.MapUtils;

import com.martiansoftware.jsap.Parameter;

import efen.UnexpectednessScorer;
import efen.evaluation.GroundTruth;
import efen.evaluation.PoolSpecification;

public class PooledDatasetChecker {
	public static Logger LOGGER = LoggerFactory.getLogger(PooledDatasetChecker.class);
	
	private final GroundTruth evaluations;
	private final UnexpectednessScorer[] pool;
	private final ImmutableGraph graph;
	private final Int2ObjectMap<String> id2name;
	/** For each scorer, a statistic with the fraction of results that are its, for each query */
	private final Object2ObjectMap<UnexpectednessScorer, SummaryStatistics> retriever2itsFraction, retriever2evaluatedTopResults;
	private final SummaryStatistics unmatchStats;

	private final static double ALPHA = 0.1;
	
	@SuppressWarnings("unchecked")
	@CommandLine(argNames = { "poolSpecificationFile", "pageName2id", "evaluationDataset"})
	public PooledDatasetChecker(
			String poolSpecificationFile,
			Object2IntMap<String> name2id,
			String datasetPath
	) throws Exception  {
		this(	
				GroundTruth.fromUTF8FilePath(datasetPath, name2id),
				new PoolSpecification(poolSpecificationFile),
				(Int2ObjectMap<String>) MapUtils.invert(name2id)
			);
	}

	public PooledDatasetChecker(GroundTruth evaluations, PoolSpecification pool, Int2ObjectMap<String> id2name) throws IOException {
		this(evaluations, pool.getGraph(), pool.getPool(), id2name);
	}
	
	public PooledDatasetChecker(GroundTruth evaluations, ImmutableGraph g, UnexpectednessScorer[] pool, Int2ObjectMap<String> id2name) throws IOException {
		this.graph = g;
		this.pool = pool;
		this.evaluations = evaluations;
		this.id2name = id2name;
		SummaryStatistics[] stats = new SummaryStatistics[pool.length];
		for (int i = 0; i < stats.length; i++)
			stats[i] = new SummaryStatistics();
		retriever2itsFraction = new Object2ObjectOpenHashMap<UnexpectednessScorer, SummaryStatistics>(pool, stats);
		for (int i = 0; i < stats.length; i++)
			stats[i] = new SummaryStatistics();
		retriever2evaluatedTopResults = new Object2ObjectOpenHashMap<UnexpectednessScorer, SummaryStatistics>(pool, stats);
		unmatchStats = new SummaryStatistics();

		System.out.println(evaluations.stats());
	}
	
	public void checkAll() {
		ProgressLogger pl = new ProgressLogger(LOGGER, "queries");
		pl.expectedUpdates = evaluations.queries().size();
		pl.start("Checking every query in dataset...");
		
		for (int node : evaluations.queries()) {
			check(node);
			pl.update();
		}
		pl.done();
	}
	
	public void check(int node) {
		IntSet consideredSuccessors = evaluations.getEvaluatedDocs(node);
		IntSet unmatchedSuccessors = new IntOpenHashSet(consideredSuccessors);
		
		int howMany = (int) (graph.outdegree(node) * ALPHA);
		
		for (UnexpectednessScorer poolMember : pool ) {
			int resultsConsidered = 0;
			for (int succ : poolMember.results(node, howMany))
				if (consideredSuccessors.contains(succ)) {
					resultsConsidered++;
					unmatchedSuccessors.remove(succ);
				}
			
			if (resultsConsidered == 0)
				LOGGER.warn(
						poolMember.toString()
						+ " got no considered results for query " + id2name.get(node)
						);
			
			double fractionOfItsResultsOverConsideredResults =
					(double) resultsConsidered / consideredSuccessors.size();
			retriever2itsFraction.get(poolMember).addValue(fractionOfItsResultsOverConsideredResults);
			retriever2evaluatedTopResults.get(poolMember).addValue((double) resultsConsidered / howMany);
		}
		
		double unmatchedResultsOverConsideredResults =
				(double) unmatchedSuccessors.size() / consideredSuccessors.size();
		unmatchStats.addValue(unmatchedResultsOverConsideredResults);
	}
	
	@SuppressWarnings("boxing")
	private String readStat(SummaryStatistics stat) {
		return String.format("avg %.2f \t min %.2f \t max %.2f",
				stat.getMean(), stat.getMin(), stat.getMax());
	}
	
	public String getResults() {
		StringBuilder s = new StringBuilder();
		s.append("\n===========FRACTION OF EVALUATED RESULTS THAT BELONGS TO EACH CLASSIFIER==========\n");
		for (UnexpectednessScorer r : pool) {
			s.append(StringUtils.rightPad(r.toString(), 30));
			s.append(": ");
			s.append(readStat(retriever2itsFraction.get(r)) + "\n");
		}
		
		s.append(StringUtils.rightPad("UNMATCHED RESULTS", 30));
		s.append(": ");
		s.append(readStat(unmatchStats) + "\n");
		
		s.append("\n===========FRACTION OF EACH CLASSIFIER'S TOP LIST THAT WAS ACTUALLY EVALUATED=====\n");
		for (UnexpectednessScorer r : pool) {
			s.append(StringUtils.rightPad(r.toString(), 30));
			s.append(": ");
			s.append(readStat(retriever2evaluatedTopResults.get(r)) + "\n");
		}
		
		return s.toString();
	}
	
	public static void main(String[] args) throws Exception {
		JsapResultsWithObject<PooledDatasetChecker> jsapObj = 
				JsapUtils.constructObject(PooledDatasetChecker.class, args,
				"Checks how much every pool member is represented in the dataset.",
				new Parameter[] {

				}
				);

		PooledDatasetChecker checker = jsapObj.getObject();
		checker.checkAll();
		System.out.println(checker.getResults());
	}


}

package efen.scorers.llamafur.classifier.evaluation;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.math3.util.MathArrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.CommandLine;
import utils.JsapUtils;
import utils.JsapUtils.JsapResultsWithObject;
import utils.RandomSingleton;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.UnflaggedOption;

import efen.scorers.llamafur.classifier.Classifier;
import efen.scorers.llamafur.classifier.PAClassifier;
import efen.scorers.llamafur.data.Matrix;

public class TestMatrix {
	public static Logger LOGGER = LoggerFactory.getLogger(TestMatrix.class);
	
	private final Matrix matrix;
	protected final ImmutableGraph graph;
	private final Int2ObjectMap<IntSet> page2cat;
	
	private final int numNodes;
	private final double negativeToPositiveRatio;
	
	private final Random rnd = RandomSingleton.get();
	private final Classifier classifier;
	private final boolean verbose;
	
	public TestMatrix(Matrix matrix, ImmutableGraph graph,
			Int2ObjectMap<IntSet> page2cat, double negativeToPositiveRatio) {
		this(matrix, graph, page2cat, negativeToPositiveRatio, true);
	}
	
	@CommandLine(argNames={"matrix", "graph", "page2cat", "negativeToPositiveRatio", "verbose"})
	public TestMatrix(Matrix matrix, ImmutableGraph graph,
			Int2ObjectMap<IntSet> page2cat, double negativeToPositiveRatio, boolean verbose) {
		super();
		this.matrix = matrix;
		this.graph = graph;
		this.page2cat = page2cat;
		this.negativeToPositiveRatio = negativeToPositiveRatio;
		this.verbose = verbose;
		numNodes = graph.numNodes();
		classifier = new PAClassifier(matrix);
	}
	
	
	public int[] createTestingSet(double samplingFraction) {
		return createTestingSet( (int) (samplingFraction * numNodes) );
	}
	

	public int[] createTestingSet(int numOfSamples) {
		numOfSamples = Math.min(numOfSamples, numNodes);

		if (verbose) LOGGER.info("Creating test set with "+numOfSamples+" nodes...");
		if (numOfSamples >= (numNodes/2)) {
			final Random rnd = RandomSingleton.get();
			int[] samples = MathArrays.natural(numNodes);
			IntArrays.shuffle(samples, rnd);
			return IntArrays.trim(samples, numOfSamples);
		} else {
			IntSet set = new IntOpenHashSet();
			while (set.size() < numOfSamples) {
				set.add(rnd.nextInt(numNodes));
			}
			int[] r = set.toIntArray();
			return r;
		}
	}
	
	public ClassifierResults[] computeNExperiments(final int nExperiments, final int nSamples) {
		final ClassifierResults[] allResults = new ClassifierResults[nExperiments];
		ProgressLogger pl = new ProgressLogger(LOGGER, "experiments");
		pl.expectedUpdates = nExperiments;
		pl.start("Beginning experiments...");
		
		for (int iExperiment = 0; iExperiment < nExperiments; iExperiment++) {
			if (verbose) LOGGER.info("Starting experiment #" + (iExperiment+1) + "...");
			ClassifierResults experimentResult = computeOneExperiment(createTestingSet(nSamples));
			if (verbose) LOGGER.info("Results for experiment #" + (iExperiment+1) + ":\n\n" + experimentResult + "\n");
			allResults[iExperiment] = experimentResult;
			
			pl.update();
		}
		pl.done();
		
		return allResults;
	}


	public ClassifierResults computeOneExperiment(final int[] testingSet) {
		ClassifierResults experimentResult = new ClassifierResults();
		
		int nonSucc;
		
		ProgressLogger pl;
		pl = new ProgressLogger(verbose? LOGGER : org.slf4j.helpers.NOPLogger.NOP_LOGGER, "nodes");
		pl.expectedUpdates = testingSet.length;
		pl.start("Making prediction for each node in testing set...");
		
		for (int node : testingSet) {
			IntSet nodeIFeatures = page2cat.get(node);
			
			int outdegree = graph.outdegree(node);
			IntSet successors = new IntOpenHashSet(graph.successorArray(node), 0, outdegree);
			
			int nonSuccessorsNum = negativeToPositiveRatio == Double.POSITIVE_INFINITY ?
					numNodes :
					(int) (negativeToPositiveRatio * outdegree)
					;
			for (int succ : successors)
				experimentResult.update(true, 
						classifier.predict(nodeIFeatures, page2cat.get(succ))
				);
			
			for (int i = 0; i < nonSuccessorsNum; i++)
				if (!successors.contains(nonSucc = rnd.nextInt(numNodes)))
					experimentResult.update(false, 
							classifier.predict(nodeIFeatures, page2cat.get(nonSucc))
					);
			pl.lightUpdate();
		}
		pl.done();
		experimentResult.computeAllStats();
		return experimentResult;
	}





	public static void main(String[] args) throws IllegalArgumentException, JSAPException, IOException, ReflectiveOperationException {
		JsapResultsWithObject<TestMatrix> jsapResultsWithObject = 
				JsapUtils.constructObject(TestMatrix.class, args, 
				"Test a matrix, measuring its confusion matrix.",
				new Parameter[] {
					new UnflaggedOption( "n", 
							JSAP.INTEGER_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
							"Number of experiments." ),
					new UnflaggedOption( "numsample", 
							JSAP.INTEGER_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
							"Number of nodes in a sample." ),	
					new FlaggedOption("trunc", JSAP.INTEGER_PARSER,
							JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, 't', "trunc",
							"If an int k is provided, it tests the matrix truncated to its "
							+ " k highest-absolute-valued elements. "),
					new FlaggedOption("lessrecallthan", JSAP.DOUBLE_PARSER,
							JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED, JSAP.NO_SHORTFLAG, "lessrecallthan",
							"Show how many experiments showed a recall lower "
							+ "then then the provided value.")
				});
		
		TestMatrix t = jsapResultsWithObject.getObject();
		JSAPResult jsap = jsapResultsWithObject.getJsapResult();
		if (jsap.contains("trunc")) {
			int trunc = jsap.getInt("trunc");
			t.matrix.truncateTo(trunc);
		}
		ClassifierResults[] testResults = t.computeNExperiments(jsap.getInt("n"), jsap.getInt("numsample"));
		System.out.println("========= MEAN OF TEST RESULTS ========");
		System.out.println(ClassifierResults.avg(Arrays.asList(testResults)));
		
		if (jsap.contains("lessrecallthan")) {
			double threshold = jsap.getDouble("lessrecallthan");
			int nLower = 0;
			for (ClassifierResults test : testResults)
				if (test.getDouble(Stats.RECALL) < threshold)
					nLower++;
			System.out.println("Number of experiments with lower recall "
					+ "than " + threshold + ": "
					+ nLower + "/" + testResults.length 
					+ " (" + ((double) nLower / testResults.length * 100.0) + "%)"  );
		}
	}
	
}

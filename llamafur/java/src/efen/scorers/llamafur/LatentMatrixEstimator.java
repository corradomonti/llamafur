package efen.scorers.llamafur;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.apache.commons.math3.util.MathArrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.RandomSingleton;
import utils.SerializationUtils;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

import efen.scorers.llamafur.classifier.Classifier;
import efen.scorers.llamafur.classifier.PAClassifier;
import efen.scorers.llamafur.classifier.evaluation.ClassifierResults;
import efen.scorers.llamafur.classifier.evaluation.Stats;
import efen.scorers.llamafur.data.ArrayMatrix;
import efen.scorers.llamafur.data.LongBasedMatrix;
import efen.scorers.llamafur.data.Matrix;

public class LatentMatrixEstimator implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(LatentMatrixEstimator.class);
	
	protected final Classifier classifier;
	
	/* INPUT DATA */
	protected  final ImmutableGraph graph;
	protected  final Int2ObjectMap<IntSet> node2cat;
	protected  final int numNodes;

	/* UTILITIES */
	protected  final Random rnd;
	private  final ProgressLogger pl;
	
	/* OUTPUT PARAMETERS */
	protected static final int SAVE_STATS_EVERY = 1000000;
	private final String output;
	private PrintStream statSaver = null;
	
	/* internal state */
	protected  int nArcsLearned;
	
	
	public LatentMatrixEstimator(ImmutableGraph graph, Int2ObjectMap<IntSet> node2cat, String output, Matrix initialMatrix) {
		
		rnd = RandomSingleton.get();
		pl = new ProgressLogger(LOGGER, "node couples");
		
		this.graph = graph;
		this.node2cat = node2cat;
		if (graph.numNodes() != node2cat.size()) {
			LOGGER.warn("node2cat file and graph file have a different number of nodes: " +
					"respectively, " + node2cat.size() + " and " + graph.numNodes());
		}
		
		numNodes = graph.numNodes();
		
		classifier = new PAClassifier(initialMatrix);
		this.output = output;
		nArcsLearned = 0;
		
	}
	
	protected void save(int nPass) {
		LOGGER.info("Saving serialized matrix for pass #"+nPass+"...");
		SerializationUtils.saveSafe( getEstimatedMatrix(), output + "-" + nPass + ".ser");
		LOGGER.info("Serialized matrix saved.");
	}

	@Override
	public void close() throws IOException {
		if (statSaver != null)
			statSaver.close();
	}

	public void saveStatsTo(File file) throws FileNotFoundException {
		statSaver = new PrintStream(file);
	}
	
	public Matrix getEstimatedMatrix() {
		return classifier.getEstimatedWeights();
	}

	public void learnNPassShuffled(int n) throws ReflectiveOperationException, IOException {
		pl.info = classifier.shortStats();
		pl.expectedUpdates = graph.numArcs() * 2 * n;
		pl.start();
		
		for (int pass = 0; pass < n; pass++) {
			LOGGER.info("Starting learning pass #"+(pass+1)+"...");
			int[] nodes = MathArrays.natural(numNodes);
			nodes = IntArrays.shuffle(nodes, rnd);
			
			for (int node : nodes)
				learnNode(node, new IntOpenHashSet(
						graph.successorArray(node), 0, graph.outdegree(node)
						));
			
			save(pass+1);
			
		}

		pl.done();
	}
	
	protected void anArcHasBeenLearned() {
		nArcsLearned++;
		if (statSaver != null && nArcsLearned % SAVE_STATS_EVERY == 0) {
			ClassifierResults stats = classifier.currentResults();
			statSaver.println(stats.compute(Stats.ACCURACY) + "\t" + stats.compute(Stats.RECALL));
		}
	}

	protected void learnNode(int node, IntSet successors ) throws IOException {
		
		for (int successor : successors) {
			classifier.learn(node2cat.get(node), node2cat.get(successor), true);
			anArcHasBeenLearned();
			pl.lightUpdate();
		}
		int nonSucc;
		for (int i = successors.size()-1; i >= 0; i--) {
			do { 
				nonSucc = rnd.nextInt(numNodes);
			} while (successors.contains(nonSucc));
			classifier.learn(node2cat.get(node), node2cat.get(nonSucc), false);
			anArcHasBeenLearned();
			pl.lightUpdate();
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public static void main(String[] rawArguments) throws JSAPException, IOException, ReflectiveOperationException {
		SimpleJSAP jsap = new SimpleJSAP(
				LatentMatrixEstimator.class.getName(),
				"Estimate the latent category matrix. ",
				new Parameter[] {
			new UnflaggedOption( "node2cat", JSAP.STRING_PARSER, JSAP.REQUIRED,
						"The serialized java file with node id -> category ids." ),
			new UnflaggedOption( "graph", JSAP.STRING_PARSER, JSAP.REQUIRED,
						"The input .graph file." ),
			new UnflaggedOption( "output", JSAP.STRING_PARSER, JSAP.REQUIRED,
				"The output serialied matrix basename, a different file will be saved"
				+ " with the estimated w at the end of each pass." ),
			new FlaggedOption( "seed", JSAP.LONG_PARSER, "1234567890", JSAP.NOT_REQUIRED, 's', "seed",
					"Seed for random generation."),
			new FlaggedOption( "passes", JSAP.INTEGER_PARSER, "1", JSAP.NOT_REQUIRED,
					'k', "passes",
					"Learning will happen k times and each time nodes will be shuffled."),
			
			new FlaggedOption( "matrixsize", JSAP.INTEGER_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED,
					'm', "matrixsize",
					"If you provide an int k, a **dense matrix** of size k x k will be used to represent W."
					+ " If you don't, it will be used a sparse matrix of unknown size."),

			new FlaggedOption( "savestats", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.NOT_REQUIRED,
					JSAP.NO_SHORTFLAG, "savestats",
					"If a filepath is provided here, it will save a tsv file with"
					+ " accuracy and recall recorded every " + SAVE_STATS_EVERY + 
					" arcs learned.")

			
		});
		
		// parse arguments
		JSAPResult args = jsap.parse( rawArguments );
		if ( jsap.messagePrinted() ) System.exit( 1 );
		if (args.contains("seed"))
			RandomSingleton.seedWith(args.getLong("seed"));
		
		Matrix matrix = args.contains("matrixsize") ?
							new ArrayMatrix(args.getInt("matrixsize"))
						:	new LongBasedMatrix();
		
		// reading input data
		LOGGER.info("Reading input files...");
		Int2ObjectMap<IntSet> node2cat = (Int2ObjectMap<IntSet>) SerializationUtils.read(args.getString("node2cat"));
		ImmutableGraph graph = ImmutableGraph.load(SerializationUtils.noExtension(args.getString("graph")));
		
		// actually doing stuff
		LatentMatrixEstimator estimator = new LatentMatrixEstimator(graph, node2cat, args.getString("output"), matrix);
		if (args.contains("savestats")) estimator.saveStatsTo(new File(args.getString("savestats")));
		estimator.learnNPassShuffled(args.getInt("passes"));
		estimator.close();
		LOGGER.info("Done.");
		
	}

}

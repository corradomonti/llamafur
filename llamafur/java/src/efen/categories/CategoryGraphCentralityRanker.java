package efen.categories;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;
import it.unimi.dsi.webgraph.algo.GeometricCentralities;

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.math3.util.MathArrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.ArrayUtils;
import utils.MapUtils;
import utils.SerializationUtils;
import cern.colt.Arrays;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

public class CategoryGraphCentralityRanker {
	final static Logger LOGGER = LoggerFactory.getLogger(CategoryGraphCentralityRanker.class);
	
	final ImmutableGraph graph;
	final double[] rank;
	final int[] positions;
	Int2ObjectFunction<String> names = MapUtils.NUMBER_PRINTER;
	
	public CategoryGraphCentralityRanker(ImmutableGraph g, double[] rank) {
		this.graph = g;
		this.rank = rank;
		positions = MathArrays.natural(graph.numNodes());
		IntArrays.quickSort(positions, ArrayUtils.reverseIndirectComparator(rank));
	}
	
	public CategoryGraphCentralityRanker(ImmutableGraph g) {
		this(g, defaultRank(g));
	}
	
	public void setNames(Int2ObjectFunction<String> names) {
		this.names = names;
	}
	
	public static double[] defaultRank(ImmutableGraph g) {
		LOGGER.info("Ranking nodes...");
		final GeometricCentralities ranker = new GeometricCentralities(Transform.transpose(g), new ProgressLogger(LOGGER));
		try {
			ranker.compute();
		} catch (InterruptedException e) { throw new RuntimeException(e); }
		LOGGER.info("Nodes ranked.");
		return ranker.harmonic;
	}
	
	public void printAll(PrintWriter out) {
		printTop(out, graph.numNodes());
	}
	
	public void printTop(PrintWriter out, int topK) {
		if (topK > graph.numNodes())
			topK = graph.numNodes();
		
		for (int i = 0; i < topK; i++)
			out.println(names.get(positions[i]));
	}
	
	public IntSet getTop(int k, PrintWriter plainOutput) {
		IntSet r = getTop(k);
		printTop(plainOutput, k);
		return r;
	}
	
	public IntSet getTop(int k) {
		if (k > graph.numNodes())
			LOGGER.warn("Warning: " + k + " categories were asked, but the graph contains " + graph.numNodes());
		
		LOGGER.info("Computing top-"+k+" categories...");
		IntSet okIds = new IntOpenHashSet(IntArrays.trim(positions, k));
		LOGGER.info("Top-"+k+" categories computed.");
		return okIds;
	}
	
	public IntSet getTopExcluding(final int k, final String[] excludedStrings) {
		return getTopExcluding(k, excludedStrings, null);
	}
	
	public IntSet getTopExcluding(final int k, final String[] excludedStrings, PrintWriter plainOutput) {
		LOGGER.info("Computing top-"+k+" categories from those whose name"
				+ " don't contains (case-insensitevely) "
				+ "any of "+Arrays.toString(excludedStrings)+"...");
		
		IntSet okIds = new IntOpenHashSet(k);
		boolean isNameOk;
		String name;
		for (int category : positions) {
			isNameOk = true;
			name = names.get(category).toLowerCase();
			for (String excludedString : excludedStrings)
				if (name.indexOf(excludedString) != -1) {
					isNameOk = false;
					break;
				}
			if (isNameOk) {
				okIds.add(category);
				if (plainOutput != null)
					plainOutput.println(names.get(category));
				if (okIds.size() == k)
					break;
			}
		}

		LOGGER.info("Top-"+k+" categories computed.");
		return okIds;
	}
	
	@SuppressWarnings({ "unchecked" })
	public static void main( String rawArguments[] ) throws Exception  {
		SimpleJSAP jsap = new SimpleJSAP( CategoryGraphCentralityRanker.class.getName(), 
				"Filter category ids basing on their harmonic centrality"
				+ " in category graph, and optionally their names.",
				new Parameter[] {
						new UnflaggedOption( "inputgraph", 
								JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
								"The .graph file of categories" ),	
						new UnflaggedOption( "k", 
								JSAP.INTEGER_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
								"Number of top-k categories to retain." ),	
						new FlaggedOption( "names", 
								JSAP.STRING_PARSER, null, JSAP.NOT_REQUIRED, 
								'n', "names", 
								"The serialized Int2ObjectMap<String> file with association of categories to their names." ),	
						new UnflaggedOption( "output",
								JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, 
								"The output serialized IntSet, with the ids that are in the top-k of not excluded categories." ),
						new FlaggedOption( "rank", 
								JSAP.STRING_PARSER, null, JSAP.NOT_REQUIRED, 
								'r', "rank", 
								"If provided, the names will be assigned according to this serialized double[] file with ranks." ),
						new FlaggedOption( "save-ordered", 
								JSAP.STRING_PARSER, null, JSAP.NOT_REQUIRED, 
								's', "save-ordered", 
								"If provided, the names ordered by centrality will be saved, one per line, to this file." ),
						new FlaggedOption( "exclude", 
								JSAP.STRING_PARSER, null, JSAP.NOT_REQUIRED, 
								'e', "exclude", 
								"Exclude all those categories whose LOWERCASED name contains one of the provided strings." )
								.setAllowMultipleDeclarations(true)								
					}
				);
				
		final JSAPResult args = jsap.parse( rawArguments );
		if ( jsap.messagePrinted() ) System.exit( 1 );
		
		ImmutableGraph graph = (ImmutableGraph) SerializationUtils.read(args.getString("inputgraph"));
		
		final CategoryGraphCentralityRanker categoryRanker = (args.contains("rank")) ?
			new CategoryGraphCentralityRanker(graph, (double[]) SerializationUtils.read(args.getString("rank")))
			:
			new CategoryGraphCentralityRanker(graph);
			
		if (args.contains("names"))
			categoryRanker.names = (Int2ObjectFunction<String>) SerializationUtils.read(args.getString("names"));
		
		int k = args.getInt("k");
		IntSet okIds = args.contains("exclude") ?
				categoryRanker.getTopExcluding(k, args.getStringArray("exclude"))
			:	categoryRanker.getTop(k);
		
		
		
		if (args.contains("save-ordered")) {
			LOGGER.info("Saving textual output...");
			PrintWriter outFile = new PrintWriter(new File(args.getString("save-ordered")));
			for (int c : categoryRanker.positions) {
				if (okIds.contains(c))
					outFile.println(categoryRanker.names.get(c));
			}
			outFile.close();
		}
		

		LOGGER.info("Saving output...");
		SerializationUtils.save(okIds, args.getString("output"));
		

		LOGGER.info("Done.");
	}

}

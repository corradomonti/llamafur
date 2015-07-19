package efen.categories;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;

import java.io.PrintWriter;

import org.apache.commons.math3.util.MathArrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.MapUtils;
import utils.SerializationUtils;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

public class CategorySelectionToolchain {
	public static Logger LOGGER = LoggerFactory.getLogger(CategorySelectionToolchain.class);
	
	
	@SuppressWarnings({ "unchecked" })
	public static void main( String rawArguments[] ) throws Exception  {
		SimpleJSAP jsap = new SimpleJSAP( CategorySelectionToolchain.class.getName(), 
				"Starting from category graph, it selects the best category and outputs the new page2cat.",
				new Parameter[] {
						new UnflaggedOption( "inputgraph", 
								JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
								"The .graph file of categories" ),	
						new UnflaggedOption( "page2cat", 
								JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
								"The serialized int 2 intset that represents set of categories for each page." ),	
						new UnflaggedOption( "k", 
								JSAP.INTEGER_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY,
								"Number of top-k categories to retain." ),	
						new FlaggedOption( "names", 
								JSAP.STRING_PARSER, null, JSAP.NOT_REQUIRED, 
								'n', "names", 
								"The serialized Int2ObjectMap<String> file with association of categories to their names." ),	
						new UnflaggedOption( "output",
								JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, 
								"Updated page2cat output basepath. A file name <output>-page2cat.ser will be saved."
								),
						new FlaggedOption( "rank", 
								JSAP.STRING_PARSER, null, JSAP.NOT_REQUIRED, 
								'r', "rank", 
								"If provided, the names will be assigned according to this serialized double[] file with ranks." ),
						new FlaggedOption( "exclude", 
								JSAP.STRING_PARSER, null, JSAP.NOT_REQUIRED, 
								'e', "exclude", 
								"Exclude all those categories whose LOWERCASED name contains one of the provided strings." )
								.setAllowMultipleDeclarations(true),
						new Switch("trim", 't', "trim",
								"The saved output will re-number every category, in a way that they're numbered from 0 to (k-1)."
								+ " If you provided names, an additional file named <output>-catId2Names.ser will be saved.")
					}
				);
				
		final JSAPResult args = jsap.parse( rawArguments );
		if ( jsap.messagePrinted() ) System.exit( 1 );
		
		LOGGER.info("Reading input files...");
		ImmutableGraph graph = (ImmutableGraph) SerializationUtils.read(args.getString("inputgraph"));
		Int2ObjectMap<IntSet> page2cat = (Int2ObjectMap<IntSet>) SerializationUtils.read(args.getString("page2cat"));
		
		LOGGER.info("Applying rank and extracting top categories...");
		final CategoryGraphCentralityRanker categoryRanker = args.contains("rank") ?
			new CategoryGraphCentralityRanker(graph, (double[]) SerializationUtils.read(args.getString("rank")))
			:
			new CategoryGraphCentralityRanker(graph);
		
		Int2ObjectFunction<String> names = MapUtils.NUMBER_PRINTER;
		if (args.contains("names")) {
			names = (Int2ObjectMap<String>) SerializationUtils.read(args.getString("names"));
			categoryRanker.setNames(names);
		}
		
		PrintWriter plainTopCategoryOutput = new PrintWriter(args.getString("output")+"-categories.txt");
		IntSet topCategories = args.contains("exclude") ?
				categoryRanker.getTopExcluding(args.getInt("k"), args.getStringArray("exclude"), plainTopCategoryOutput)
			:	categoryRanker.getTop(args.getInt("k"), plainTopCategoryOutput);
        plainTopCategoryOutput.close();
				
		LOGGER.info("Finding top categories closer to old categories...");
		int[] closestTopCategories = new HittingDistanceMinimizer(graph, topCategories).compute();
		PrintWriter plainRecategorizationOutput  = new PrintWriter(args.getString("output")+"-recategorization.tsv");
		for (int i = 0; i < closestTopCategories.length; i++)
			plainRecategorizationOutput.println(
					names.get(i) + "\t" + names.get(closestTopCategories[i])
			);
		plainRecategorizationOutput.close();
		
		int countNull = 0;
		for (int i : closestTopCategories) if (i == -1) countNull++;
		LOGGER.info(countNull + " old categories don't have a parent in top categories"
				+ " ("+((double) countNull * 100.0 / closestTopCategories.length)+"%).");
		
		LOGGER.info("Creating updated page2cat map...");
		String page2catOutputPath = args.getString("output") + "-page2cat.ser";
		PagesCategorizationMover categoriesMover = new PagesCategorizationMover(closestTopCategories, page2cat, page2catOutputPath);
		categoriesMover.compute();
		
		countNull = 0;
		for (IntSet x : categoriesMover.page2cat.values()) if (x.isEmpty()) countNull++;
		LOGGER.info(countNull + " pages don't have a category"
				+ " ("+((double) countNull * 100.0 / categoriesMover.page2cat.size())+"%).");
		
		if (!args.getBoolean("trim"))
			categoriesMover.save();
		else {
			LOGGER.info("Computing map from old ids to new ids...");
			Int2IntMap old2new = new Int2IntOpenHashMap(
					/*  keys  */ topCategories.toIntArray(),
					/* values */ MathArrays.natural(topCategories.size())
					);
			old2new.defaultReturnValue(-1);
			page2cat = categoriesMover.page2cat;
			
			ProgressLogger pl = new ProgressLogger(LOGGER, "pages");
			pl.expectedUpdates = page2cat.values().size();
			pl.start("Reindexing categories in page2cat...");
			int newCat;
			for (IntSet categories : page2cat.values()) {
				int[] oldCategories = categories.toIntArray();
				categories.clear();
				for (int oldCat : oldCategories)
					if ((newCat = old2new.get(oldCat)) != -1)
						categories.add(newCat);
				pl.lightUpdate();
			}
			pl.done();

			LOGGER.info("Saving reindexed page2cat...");
			SerializationUtils.save(page2cat, page2catOutputPath);
			
			if (names != MapUtils.NUMBER_PRINTER) {
				LOGGER.info("Reindexing names of categories...");
				Int2ObjectMap<String> newNames = new Int2ObjectOpenHashMap<String>(topCategories.size());
				for (int oldCat : topCategories)
					newNames.put(
							(newCat = old2new.get(oldCat)),
							names.get(oldCat)
							);
				LOGGER.info("Saving rehashed names of categories...");
				SerializationUtils.save(newNames, args.getString("output")+"-catId2Names.ser");
			}
			
			LOGGER.info("Done.");
			
		}
		
	}
}

package efen.analysis;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.SerializationUtils;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

import efen.UnexpectednessScorer;
import efen.evaluation.GroundTruth;
import efen.evaluation.PoolSpecification;

public class ScorerComparison {
	public static Logger LOGGER = LoggerFactory.getLogger(ScorerComparison.class);

	private final UnexpectednessScorer[] scorers;
	private final GroundTruth groundtruth;
	
	
	public ScorerComparison(GroundTruth groundtruth, UnexpectednessScorer[] scorers) throws IOException {
		this.scorers = scorers;
		this.groundtruth = groundtruth;
	}
	
	public void saveComparisons(File f) throws IOException {
		final Writer out = new FileWriter(f);
		LOGGER.info("Computing and saving scores...");
		for (int node : groundtruth.queries()) {
			Int2DoubleMap[] scorersResults = new Int2DoubleMap[scorers.length];
			for (int i = 0; i < scorers.length; i++)
				scorersResults[i] = scorers[i].scores(node);
			
			for (int succ : groundtruth.getEvaluatedDocs(node)) {
				StringBuilder row = new StringBuilder();
				for (Int2DoubleMap scorersResult : scorersResults)
					row.append(scorersResult.get(succ) + "\t");
				
				row.append(groundtruth.getRelevance(node, succ) + "\n");
				out.write(row.toString());
			}
		}
		LOGGER.info("Done.");
		out.close();
	}
	
	@SuppressWarnings("unchecked")
	public static void main(String[] rawArguments) throws Exception {
		SimpleJSAP jsap = new SimpleJSAP(
				ScorerComparison.class.getName(),
				"Compare the score computed by one or more scorers with groundtruth,"
				+ " and save the result as a tsv file (where each column is a score,"
				+ " and the last column is the groundtruth).",
				new Parameter[] {
			new UnflaggedOption( "pageName2id", JSAP.STRING_PARSER, JSAP.REQUIRED,
					"The serialized pageName2id map." ),
			new UnflaggedOption( "groundruth", JSAP.STRING_PARSER, JSAP.REQUIRED,
						"The tsv file contining the human-evaluated dataset." ),
			new UnflaggedOption( "output", JSAP.STRING_PARSER, JSAP.REQUIRED,
				"The output tsv file path." ),
			new UnflaggedOption( "scorer", PoolSpecification.SCORER_PARSER, 
					JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.GREEDY,
					"A scorer specification." ),
			
		});
		
		// parse arguments
		JSAPResult args = jsap.parse( rawArguments );
		if ( jsap.messagePrinted() ) System.exit( 1 );
		
		Object2IntMap<String> pageName2id = (Object2IntMap<String>) SerializationUtils.read(args.getString("pageName2id"));
		GroundTruth groundtruth = GroundTruth.fromUTF8FilePath(args.getString("groundruth"), 
				pageName2id);
		
		File output = new File(args.getString("output"));
		UnexpectednessScorer[] scorers = (UnexpectednessScorer[]) args.getObjectArray("scorer", new UnexpectednessScorer[0]);
		new ScorerComparison(groundtruth, scorers)
			.saveComparisons(output);
	}

	
}

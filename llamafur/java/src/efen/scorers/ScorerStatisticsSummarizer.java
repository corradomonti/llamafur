package efen.scorers;

import it.unimi.dsi.logging.ProgressLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.RandomSingleton;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.UnflaggedOption;

import efen.UnexpectednessScorer;
import efen.evaluation.PoolSpecification;

public class ScorerStatisticsSummarizer {

	private static Logger LOGGER = LoggerFactory.getLogger(ScorerStatisticsSummarizer.class);
	
	public static SummaryStatistics computeStatistics(UnexpectednessScorer scorer) {
		int numNodes = scorer.graph.numNodes();
		SummaryStatistics stats = new SummaryStatistics();
		
		ProgressLogger pl = new ProgressLogger(LOGGER, "docs");
		pl.expectedUpdates = numNodes;
		pl.start("Finding statistics for values of " + scorer + "...");

		for (int i = 0; i < numNodes; i++) {
			for (double x : scorer.scores(i).values()) {
				stats.addValue(x);
				if (Double.isInfinite(x) || Double.isNaN(x))
					throw new ArithmeticException(
							"Scorer " + scorer + " has returned value "
							+ x + " for a result of document " + i);
			}
			pl.lightUpdate();
		}
		pl.done();
		
		return stats;
	}
	
	public static SummaryStatistics computeApproxStatistics(UnexpectednessScorer scorer) {
		int numNodes = scorer.graph.numNodes();
		int sampleSize = 10000;
		if (sampleSize > numNodes) sampleSize = numNodes;

		SummaryStatistics stats = new SummaryStatistics();
		
		ProgressLogger pl = new ProgressLogger(LOGGER, "docs");
		pl.expectedUpdates = sampleSize;
		pl.start("Finding statistics for values of " + scorer + " (sample of "+sampleSize+")...");

		for (int i: RandomSingleton.get().ints(sampleSize, 0, numNodes).toArray()) {
			for (double x : scorer.scores(i).values()) {
				stats.addValue(x);
				if (Double.isInfinite(x) || Double.isNaN(x))
					throw new ArithmeticException(
							"Scorer " + scorer + " has returned value "
							+ x + " for a results of document " + i);
			}
			pl.lightUpdate();
		}
		pl.done();
		
		LOGGER.info(scorer + " -- sample of " + numNodes + " -- " +  stats.toString());
		return stats;
	}
	
	public static Properties load(String filepath) throws IOException {
		Properties p = new Properties();
		FileInputStream in = new FileInputStream(filepath);
		p.load(in);
		in.close();
		return p;
	}
	
	public static Properties save(SummaryStatistics stat, File file, String scorerSpec) throws IOException {
		Properties prop = new Properties();
		prop.setProperty("scorer", scorerSpec);
		for (String line : stat.toString().split("\n")) {
			String[] tokens = line.split(":", 2);
			if (tokens.length == 2) {
				tokens[0] = tokens[0].trim().replace(" ", "-");
				tokens[1] = tokens[1].trim();
				if (tokens[0].length() > 0 && tokens[1].length() > 0)
					prop.setProperty(tokens[0], tokens[1]);
			}
		}
		OutputStream out = new FileOutputStream(file);
		String comments = SummaryStatistics.class.getSimpleName() + " saved by " + ScorerStatisticsSummarizer.class.getSimpleName();
		prop.store(out, comments);
		out.close();
		return prop;
	}
	
	public static void main(String[] rawArguments) throws JSAPException, IOException, ReflectiveOperationException {
		SimpleJSAP jsap = new SimpleJSAP(
				ScorerStatisticsSummarizer.class.getName(),
				"Compute summary statistics for a scorer ",
				new Parameter[] {
			new UnflaggedOption( "scorer", JSAP.STRING_PARSER, JSAP.REQUIRED,
						"Specification for the scorer" ),
			new UnflaggedOption( "output", JSAP.STRING_PARSER, JSAP.REQUIRED,
						"Filepath of the saved statistics summary, saved as a Property file." ),
			
		});
		
		// parse arguments
		JSAPResult args = jsap.parse( rawArguments );
		if ( jsap.messagePrinted() ) System.exit( 1 );
		
		String scorerSpec = args.getString("scorer");
		UnexpectednessScorer scorer = (UnexpectednessScorer) PoolSpecification.SCORER_PARSER.parse(scorerSpec);
		SummaryStatistics stat = computeStatistics(scorer);
		save(stat, new File(args.getString("output")), scorerSpec);
		System.out.println(scorer + " " + stat);
	}

}

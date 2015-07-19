package efen.parsewiki;

import it.unimi.di.archive4j.tool.ArchiveBuilder;
import it.unimi.dsi.util.FrontCodedStringList;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.SerializationUtils;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

public class WikipediaTextArchiveProducer {
	public static Logger LOGGER = LoggerFactory.getLogger(WikipediaTextArchiveProducer.class);
	
	public static void main(String[] rawArguments) throws Exception {
		
		SimpleJSAP jsap = new SimpleJSAP(
				WikipediaTextArchiveProducer.class.getName(),
				"Build an Archive that can be used by text based scorers" + 
				" to actually score tf-idf. It requires " +
				" hours.",
				new Parameter[] {
			new UnflaggedOption( "input", JSAP.STRING_PARSER, JSAP.REQUIRED,
						"The pages-articles.xml input file, from Wikipedia." ),
			new UnflaggedOption( "stopwords", JSAP.STRING_PARSER, JSAP.REQUIRED,
					"The stopwords file path (one stopword per line)." ),
			new Switch("bzip", 'z', "bzip", "Interpret the input file as bzipped"),
			new Switch("firstpar", 'f', "firstpar", "Parse only the first paragraph, as opposite to the full page."),
			new UnflaggedOption( "output", JSAP.STRING_PARSER, JSAP.REQUIRED,
					"output file path" ),					
		});
		
		JSAPResult args = jsap.parse( rawArguments );
		if ( jsap.messagePrinted() ) System.exit( 1 );
		
		WikipediaDocumentSequence wikipediaDocumentSequence = new WikipediaDocumentSequence(
				args.getString("input"), 
				args.getBoolean("bzip"), 
				"http://en.wikipedia.org/wiki/",
				true, // parse text article
				false // do not keep all namespaces
			);
		
		File tmp = SerializationUtils.serializeTmp(wikipediaDocumentSequence, "wikidocsequence");
		
		@SuppressWarnings("unused")
		org.apache.commons.logging.Log testIfApacheLoggingIsPresent;
		
		String indexedField = args.getBoolean("firstpar") ? "firstpar" : "text";
		
		String options = 
				"--sequence "+tmp.getAbsolutePath()
				+ " --indexed-field " + indexedField
				+ " --termprocessor "+ it.unimi.di.big.mg4j.index.snowball.EnglishStemmer.class.getName()
				+ " --min-freq 10"
				+ " --remove-mixed"
				+ " --stopwords " + args.getString("stopwords")
				+ " --min-length 4"
				+ " --max-length 50"
				+ " --random-access"
				+ " " + args.getString("output");
		
		LOGGER.info("Launching " + ArchiveBuilder.class.getName() + " with options " + options);
		ArchiveBuilder.main(options.split(" "));
		
		LOGGER.info("Please use " + FrontCodedStringList.class + " to get the inverted termmap");
		
		LOGGER.info("Done.");
		
	}
}

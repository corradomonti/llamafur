package efen.parsewiki;

import it.unimi.di.big.mg4j.tool.VirtualDocumentResolver;
import it.unimi.dsi.big.webgraph.BVGraph;
import utils.SerializationUtils;

import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

public class WikipediaGraphProducer {

	public static void main(String[] rawArguments) throws Exception {
		SimpleJSAP jsap = new SimpleJSAP(
				WikipediaTextArchiveProducer.class.getName(),
				"Build wikipedia graph.",
				new Parameter[] {
			new UnflaggedOption( "input", JSAP.STRING_PARSER, JSAP.REQUIRED,
						"The pages-articles.xml input file, from Wikipedia." ),
			new Switch("bzip", 'z', "bzip", "Interpret the input file as bzipped"),
			new UnflaggedOption( "resolver", JSAP.STRING_PARSER, JSAP.REQUIRED,
					"resolver" ),	
			new UnflaggedOption( "output", JSAP.STRING_PARSER, JSAP.REQUIRED,
					"output graph basename" )
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
		
		DocumentSequenceImmutableGraph g = new DocumentSequenceImmutableGraph(
				wikipediaDocumentSequence,
				8, // should be the anchor field
				(VirtualDocumentResolver) SerializationUtils.read(args.getString("resolver"))
				);
		
		BVGraph.store(g, args.getString("output"));
	}

}

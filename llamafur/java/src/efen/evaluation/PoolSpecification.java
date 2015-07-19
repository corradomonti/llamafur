package efen.evaluation;

import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.lang.ObjectParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.ParseException;

import efen.UnexpectednessScorer;

public class PoolSpecification {
	final static Logger LOGGER = LoggerFactory.getLogger(PoolSpecification.class);
	
	public static final ObjectParser SCORER_PARSER = new ObjectParser(
			UnexpectednessScorer.class,
			new String[] {UnexpectednessScorer.class.getPackage().getName()}
	);
	
	private final UnexpectednessScorer[] pool;
	private final ImmutableGraph graph;
	
	public PoolSpecification(String fromFilePath) throws IOException, ParseException {
		LOGGER.info("Reading scorer pool specification from file " + fromFilePath + "...");
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(fromFilePath))
		);
		
		LOGGER.info("Loading graph...");
		graph = ImmutableGraph.load(reader.readLine().trim());
		
		List<UnexpectednessScorer> pool = new ArrayList<UnexpectednessScorer>();
		for (String line; (line = reader.readLine()) != null;) {
			line = line.trim();
			if (line.length() > 0 && !line.startsWith("#")) {
				LOGGER.info("Loading scorer " + line + "...");
				UnexpectednessScorer scorer = (UnexpectednessScorer) SCORER_PARSER.parse(line);
				if (!scorer.graph.basename().equals(graph.basename())) {
					reader.close();
					throw new IllegalArgumentException(
							"The scorer referred by " + line + 
							" must refer to the declared graph " + graph.basename());
					}
				pool.add(scorer);
			}
		}
		reader.close();
		
		this.pool = pool.toArray(new UnexpectednessScorer[] {});
		LOGGER.info("Pool specification was read correctly.");
	}
	
	public UnexpectednessScorer[] getPool() {
		return pool;
	}
	
	public ImmutableGraph getGraph() {
		return graph;
	}

}

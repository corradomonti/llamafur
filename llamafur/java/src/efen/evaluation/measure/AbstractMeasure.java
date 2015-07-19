package efen.evaluation.measure;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.JsapUtils;
import utils.JsapUtils.JsapResultsWithObject;
import utils.MapUtils;
import utils.SerializationUtils;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;

import efen.UnexpectednessScorer;
import efen.evaluation.GroundTruth;
import efen.evaluation.PoolSpecification;

public abstract class AbstractMeasure implements Closeable {

	static final Logger LOGGER = LoggerFactory.getLogger(AbstractMeasure.class);

	public static void print(Object[] names, SummaryStatistics[] results) {
		for (int i = 0; i < names.length; i++) {
			System.out.println(names[i] + ": " + 
					results[i].getMean() + 
					" +-" + results[i].getStandardDeviation() + 
					" (" + results[i].getN() + " queries)"
				);
		}
	}
	
	protected final GroundTruth groundtruth;
	protected final UnexpectednessScorer[] retrievers;
	protected final ImmutableGraph graph;
	private PrintWriter out;
	private Int2ObjectFunction<String> id2names = MapUtils.NUMBER_PRINTER;
	
	public AbstractMeasure(PoolSpecification pool, GroundTruth groundtruth) throws IOException {
		this.graph = pool.getGraph();
		this.retrievers = pool.getPool();
		this.groundtruth = groundtruth;
	}
	
	public abstract double computeForQuery(int query, UnexpectednessScorer retriever);

	public void setQueryNames(final Int2ObjectFunction<String> names) {
		this.id2names = names;
	}
	
	public void setOutputToStdOut() {
		out = new PrintWriter(System.out);
	}
	
	public void setOutputPathTo(String filePath) throws IOException {
		out = new PrintWriter(new OutputStreamWriter(
			    new FileOutputStream(filePath), "UTF-8"));
	}
	
	protected void printResult(String s) {
		if (out != null) out.println(s);
	}
	
	protected String query(int id) {
		return id2names.get(id);
	}
	
	public void close() {
		if (out != null) out.close();
	}
	
	public SummaryStatistics[] computeAll() {
		SummaryStatistics[] results = new SummaryStatistics[retrievers.length];
		for (int i = 0; i < retrievers.length; i++)
			results[i] = new SummaryStatistics();
		
		IntSet queries = groundtruth.queriesWithRelevantDocs();
		
		ProgressLogger pl = new ProgressLogger(LOGGER, "query retrievals");
		pl.expectedUpdates = queries.size() * retrievers.length;
		pl.start("Computing results for each of the " + queries.size() + 
				" queries and "+retrievers.length+ " retrieving algorithms..."  );
		
		for (int query : queries) {
			for (int i = 0; i < retrievers.length; i++) {
				double value = computeForQuery(query, retrievers[i]);
				if (Double.isNaN(value))
					continue;
				results[i].addValue(value);
				pl.update();
			}
		}
		
		pl.done();
		
		long nItems = results[0].getN();
		for (SummaryStatistics s : results)
			if (s.getN() != nItems)
				throw new IllegalStateException("The measure returned NaN differently for different algorithms.");
		
		print(retrievers, results);
		
		return results;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends AbstractMeasure> void main(String[] args, Class<T> concreteMeasure) throws Exception {
		JsapResultsWithObject<T> jsapObj = JsapUtils
				.constructObject(concreteMeasure, args, "Compute " + concreteMeasure.getSimpleName(),
						new Parameter[] {
						
						new FlaggedOption("names",
								JSAP.STRING_PARSER, JSAP.NO_DEFAULT,
								JSAP.NOT_REQUIRED, 'n', "names",
								"if supplied, output will use names for each query instead of query id"),
						new FlaggedOption( "output", 
								JSAP.STRING_PARSER, null, JSAP.NOT_REQUIRED, 
								'o', "output", 
								"Output file path to save query-specific data." ),
				
				});

		T measurer = jsapObj.getObject();
		JSAPResult jsap = jsapObj.getJsapResult();
		if (jsap.contains("names"))
			measurer.setQueryNames((Int2ObjectMap<String>) SerializationUtils
					.read(jsap.getString("names")));
		
		if (jsap.contains("output"))
			measurer.setOutputPathTo(jsap.getString("output"));

		measurer.computeAll();
		measurer.close();
		
	}

}
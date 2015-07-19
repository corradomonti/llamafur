package efen.evaluation;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.objects.Object2DoubleFunction;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.webgraph.ImmutableGraph;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.MapUtils;

public class GroundTruth {

	private static final int NO_REL = -1;

	final static Logger LOGGER = LoggerFactory.getLogger(GroundTruth.class);
	
	final static public Object2DoubleFunction<String> rating2relevance 
		= new Object2DoubleOpenHashMap<String>(
			new String[] {	"TE", 	"E", 	"U", 	"TU"		},
			new double[] {	 0, 	0.25, 	0.75, 	1.0			}
		);
	
	public static final double RELVANCE_THRESHOLD = 0.5;

	public static GroundTruth fromUTF8FilePath(
			String datasetPath, Object2IntMap<String> pageName2id
	) throws IOException {
		return fromUTF8FilePath(datasetPath, pageName2id, true);
	}
	
	public static GroundTruth fromUTF8FilePath(
			String datasetPath, Object2IntMap<String> pageName2id, boolean removeUnbalancedQueries
	) throws IOException {
		GroundTruth truth = new GroundTruth(new BufferedReader(new InputStreamReader(
						new FileInputStream(datasetPath), "UTF8")), pageName2id);
		if (removeUnbalancedQueries)
			truth.removeIfNotOneRelevantAndOneIrrelevant();
		return truth;
	}
	
	private final Int2ObjectMap<Int2DoubleMap> query2doc2relevance;
	private final Int2ObjectMap<IntSet> query2relevantdocs;
	private final Int2ObjectMap<IntSet> query2evaluateddocs;
	
	private GroundTruth(final BufferedReader in, final Object2IntMap<String> name2id) throws IOException {
		final int NORESULT = name2id.defaultReturnValue();
		query2doc2relevance = new Int2ObjectOpenHashMap<Int2DoubleMap>();
		query2relevantdocs = new Int2ObjectOpenHashMap<IntSet>();
		query2evaluateddocs = new Int2ObjectOpenHashMap<IntSet>();
		
		String line;
		
		while( ( line=in.readLine() ) !=null ){
			if (line.trim().length() > 0) {
				String[] tokens = line.trim().split("\t");
				if (tokens.length != 3) throw new IllegalArgumentException("Malformed line:\n" + line);
				
				int fromPage = name2id.getInt(tokens[0]);
				int toPage = name2id.getInt(tokens[1]);
				double rating = rating2relevance.getDouble(tokens[2].trim());
				
				if (fromPage == NORESULT)
					LOGGER.error("Not found: " + tokens[0]);
				else if (toPage == NORESULT)
					LOGGER.error("Not found: " + tokens[1]);
				else {
					Int2DoubleMap queryTruth = query2doc2relevance.get(fromPage);
					if (queryTruth == null) {
						queryTruth = new Int2DoubleOpenHashMap();
						queryTruth.defaultReturnValue(NO_REL);
						query2doc2relevance.put(fromPage, queryTruth);
					}
					queryTruth.put(toPage, rating);
					if (rating >= RELVANCE_THRESHOLD) {
						IntSet relevantdocs = query2relevantdocs.get(fromPage);
						if (relevantdocs == null || relevantdocs == IntSets.EMPTY_SET) {
							relevantdocs = new IntOpenHashSet();
							query2relevantdocs.put(fromPage, relevantdocs);
						}
						relevantdocs.add(toPage);
					}
					IntSet evaluateddocs = query2evaluateddocs.get(fromPage);
					if (evaluateddocs == null || evaluateddocs == IntSets.EMPTY_SET) {
						evaluateddocs = new IntOpenHashSet();
						query2evaluateddocs.put(fromPage, evaluateddocs);
					}
					evaluateddocs.add(toPage);
				}
				
			}
			
			query2evaluateddocs.defaultReturnValue(IntSets.EMPTY_SET);
			query2relevantdocs.defaultReturnValue(IntSets.EMPTY_SET);
		}
	}
	
	public void checkAgreementWith(ImmutableGraph g) {
		checkAgreementWith(g, MapUtils.NUMBER_PRINTER);
	}
	
	public void checkAgreementWith(ImmutableGraph g, Int2ObjectFunction<String> names)  {
		for (Entry<IntSet> q2ev : query2evaluateddocs.int2ObjectEntrySet()) {
			int query = q2ev.getIntKey();
			IntSet successors = new IntOpenHashSet(g.successorArray(query), 0, g.outdegree(query));
			for (int evaluated : q2ev.getValue()) {
				if (!successors.contains(evaluated)) {
					LOGGER.error(
							"The query " + names.get(query) + " contains evaluated document "
						    + names.get(evaluated) + ", but this was not present in its successors"
							);
				}
			}
		}
	}
	
	public void removeIfNotOneRelevantAndOneIrrelevant() {
		IntSet queriesToRemove = new IntOpenHashSet();
		for (int query : queries()) {
			int nEval = query2evaluateddocs.get(query).size();
			int nRelev = query2relevantdocs.get(query).size();
			int nIrrelev = nEval - nRelev;
			if (nRelev < 1 || nIrrelev < 1)
				queriesToRemove.add(query);
		}
		
		int originalNQueries = queries().size();
		for (int q : queriesToRemove) {
			query2doc2relevance.remove(q);
			query2evaluateddocs.remove(q);
			query2relevantdocs.remove(q);
		}
		
		LOGGER.info(queriesToRemove.size() + " queries were without "
				+ "relevant or irrilevant results and were removed. "
				+ "Number of queries went from " + originalNQueries 
				+ " to " + queries().size() + ".");
	}
	
	public IntSet queries() {
		return query2evaluateddocs.keySet();
	}
	
	public IntSet queriesWithRelevantDocs() {
		return query2relevantdocs.keySet();
	}
	
	public boolean isRelevant(int query, int result) {
		return query2relevantdocs.get(query).contains(result);
	}
	
	
	public IntSet getEvaluatedDocs(int query) {
		return query2evaluateddocs.get(query);
	}
	
	public IntSet getRelevants(int query) {
		return query2relevantdocs.get(query);
	}
	
	public double getRelevance(int query, int result) {
		Int2DoubleMap doc2rel = query2doc2relevance.get(query);
		if (doc2rel == null)
			throw new NoSuchElementException("No query " + query);
		double r = doc2rel.get(result);
		if (r == NO_REL)
			throw new NoSuchElementException("No result " + result + " in evaluated docs for " + query);
		else
			return r;
	}
	
	public ObjectSet<Int2DoubleMap.Entry> getEvaluatedDocsWithNumericRelevance(int query) {
		return query2doc2relevance.get(query).int2DoubleEntrySet();
	}
	
	public double totalRelevanceFor(int query) {
		double sum = 0;
		for (double r : query2doc2relevance.get(query).values())
			sum += r;
		return sum;
	}
	
	public String stats() {
		StringBuilder s = new StringBuilder();
		
		SummaryStatistics nOfRelevantDocuments = new SummaryStatistics();
		SummaryStatistics nOfIrrelevantDocuments = new SummaryStatistics();
		SummaryStatistics nOfEvaluatedDocuments = new SummaryStatistics();
		
		for (int query : query2doc2relevance.keySet()) {
			int nRelevant = query2relevantdocs.get(query).size();
			int nEvaluated = query2evaluateddocs.get(query).size();
			nOfRelevantDocuments.addValue(nRelevant);
			nOfEvaluatedDocuments.addValue(nEvaluated);
			nOfIrrelevantDocuments.addValue(nEvaluated - nRelevant);
		}
		s.append("\n==== Statistics for groundtruth ==== \n");
		s.append("\nNumber of queries:\n");
		s.append(query2doc2relevance.size());
		s.append("\nNumber of relevant doc per query:\n");
		s.append(nOfRelevantDocuments);
		s.append("\nNumber of irrelevant doc per query:\n");
		s.append(nOfIrrelevantDocuments);
		s.append("\nNumber of evaluated doc per query:\n");
		s.append(nOfEvaluatedDocuments);
		s.append("\n==================================== \n");
		
		return s.toString();
	}

}

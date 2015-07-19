package efen;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.webgraph.ImmutableGraph;
import utils.MapUtils;

public abstract class UnexpectednessScorer {

	public final ImmutableGraph graph;
	
	public UnexpectednessScorer(ImmutableGraph graph) {
		this.graph = graph;
	}
	
	/**
	 * @return a map of all successors of docI, from their indexes to their unexpectedness
	 */
	public abstract Int2DoubleMap scores(int docI);
	
	/**
	 * @return all out-links of node, ordered by their unexpected (most unexpected first)
	 */
	public int[] results(int node) {
		Int2DoubleMap doc2score = scores(node);
		int[] results = doc2score.keySet().toIntArray();
		IntArrays.quickSort(results, MapUtils.comparatorPuttingLargestMappedValueFirst(doc2score));
		assert doc2score.get(results[0]) > doc2score.get(results[results.length - 1]);
		return results;
	}
	
	/**
	 * @return at most <i>howMany</i> out-links of node, ordered by their unexpected (most unexpected first)
	 */
	public int[] results(int node, int howMany) {
		return IntArrays.trim(results(node), howMany);
	}
	

	
	public IntSet successors(int document) {
		IntOpenHashSet succs = new IntOpenHashSet(graph.successorArray(document), 0, graph.outdegree(document));
		succs.remove(document); // avoid self-loops
		return succs;
	}
	
	public String toString() {
		return this.getClass().getSimpleName().replace("Scorer", "");
	}
	

}

package efen.scorers.aa;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.Transform;
import efen.UnexpectednessScorer;

public class AdamicAdarScorer extends UnexpectednessScorer {
	final public static Logger LOGGER = LoggerFactory.getLogger(AdamicAdarScorer.class);
	
	private ImmutableGraph undirectedGraph;
	
	public AdamicAdarScorer(String graphBasename) throws IOException {
		this(ImmutableGraph.load(graphBasename));
	}
	
	public AdamicAdarScorer(ImmutableGraph graph) throws IOException {
		super(graph);
		
		String symmBasename = graph.basename() + "-symm";
		try {
			undirectedGraph = ImmutableGraph.load(symmBasename);
		} catch (FileNotFoundException e) {
			LOGGER.info("Symmetrizying graph in order to compute Adamic-Adar...");
			BVGraph.store(Transform.symmetrize(graph), symmBasename);
			undirectedGraph = ImmutableGraph.load(symmBasename);
			LOGGER.info("Graph symmetrized.");
		}
	}

	@Override
	public Int2DoubleMap scores(int nodeX) {
		int[] neighborsOfX = IntArrays.trim(undirectedGraph.successorArray(nodeX), undirectedGraph.outdegree(nodeX));
		Int2DoubleMap scores = new Int2DoubleOpenHashMap();
		for (int nodeY : super.successors(nodeX)) {
			IntSet neighborsOfY = new IntOpenHashSet(undirectedGraph.successorArray(nodeY), 0, undirectedGraph.outdegree(nodeY));
			double expectednessOfY = 0;
			for (int nodeZ : neighborsOfX)
				if (neighborsOfY.contains(nodeZ))
					expectednessOfY += 1.0 / Math.log(undirectedGraph.outdegree(nodeZ));
			if (Double.isInfinite(expectednessOfY) || Double.isNaN(expectednessOfY))
				throw new ArithmeticException("Adamic-Adar from "+nodeX+" to " + nodeY + " has been computed as " + expectednessOfY);
			scores.put(nodeY, -expectednessOfY);
		}
		return scores;
	}

}

package efen.scorers;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.webgraph.ImmutableGraph;

import java.io.IOException;
import java.util.Random;

import utils.RandomSingleton;
import efen.UnexpectednessScorer;

public class RandomScorer extends UnexpectednessScorer {
	
	final private Random rnd;
	
	public RandomScorer(String graphBasepath) throws IOException {
		this(ImmutableGraph.load(graphBasepath));
	}

	public RandomScorer(ImmutableGraph g) {
		super(g);
		rnd = RandomSingleton.get();
	}

	@Override
	public Int2DoubleMap scores(int node) {
		int[] succs = super.successors(node).toIntArray();
		return new Int2DoubleOpenHashMap(
				succs,
				rnd.doubles(succs.length).toArray()
				);
	}
}

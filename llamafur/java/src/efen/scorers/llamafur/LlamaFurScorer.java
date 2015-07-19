package efen.scorers.llamafur;

import java.io.IOException;

import utils.SerializationUtils;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.webgraph.ImmutableGraph;
import efen.UnexpectednessScorer;
import efen.scorers.llamafur.data.Matrix;

public class LlamaFurScorer extends UnexpectednessScorer {
	private final Matrix w;
	private final Int2ObjectMap<IntSet> page2cat;
	private final String name;

	@SuppressWarnings("unchecked")
	public LlamaFurScorer(String graphBasename, String matrixPath, String page2catPath, String name) throws ClassNotFoundException, IOException {
		this(
			ImmutableGraph.load(graphBasename),
			(Matrix) SerializationUtils.read(matrixPath),
			(Int2ObjectMap<IntSet>) SerializationUtils.read(page2catPath),
			name
		);
	}
	
	public LlamaFurScorer(ImmutableGraph graph, Matrix w, Int2ObjectMap<IntSet> page2cat) {
		this(graph, w, page2cat, null);
	}
	
	public LlamaFurScorer(ImmutableGraph graph, Matrix w, Int2ObjectMap<IntSet> page2cat, String name) {
		super(graph);
		this.w = w;
		this.page2cat = page2cat;
		this.name = name;
	}
	
	private double expectedness(IntSet nodeI, IntSet nodeJ) {
		double sum = 0;
		for (int a : nodeI)
			for (int b : nodeJ)
				sum += w.get(a, b);
		
		return sum;
	}
	
	public boolean isExpected(int nodeI, int nodeJ) {
		return expectedness(page2cat.get(nodeI), page2cat.get(nodeJ)) > 0;
	}

	@Override
	public Int2DoubleMap scores(int docI) {
		IntSet successors = successors(docI);
		Int2DoubleMap results = new Int2DoubleOpenHashMap(successors.size());
		IntSet catI = page2cat.get(docI);
		for (int docJ : successors) {
			IntSet catJ = page2cat.get(docJ);
			results.put(docJ, -expectedness(catI, catJ));
		}
		return results;
	}
	
	public String toString() {
		if (name != null)
			return name;
		else return super.toString();
	}
	
}

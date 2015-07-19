package efen.scorers.textual;

import it.unimi.di.archive4j.Archive;
import it.unimi.di.archive4j.ArrayDocumentSummary;
import it.unimi.di.law.vector.ImmutableSparseVector;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.webgraph.ImmutableGraph;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.SerializationUtils;

public class JacquenetM4Scorer extends TextualUnexpectednessScorer {
	final public static Logger LOGGER = LoggerFactory.getLogger(JacquenetM4Scorer.class);
	
	@SuppressWarnings("unchecked")
	public JacquenetM4Scorer(String graphBasename, String archiveFile) throws IOException {
		this(ImmutableGraph.load(graphBasename), (Archive<ArrayDocumentSummary>) SerializationUtils.readArchive4j(archiveFile));
	}
	
	public JacquenetM4Scorer(ImmutableGraph graph, Archive<ArrayDocumentSummary> archive) {
		super(graph, archive);
	}
	
	@Override
	public Int2DoubleMap scores(int nodeIndex) {
		final int[] successors = super.successors(nodeIndex).toIntArray();
		final int outdegree = successors.length;
		Int2DoubleMap results = new Int2DoubleOpenHashMap(outdegree);
		
		// term index -> the number of documents with that term in the universe of successors
		Int2DoubleOpenHashMap n = new Int2DoubleOpenHashMap();
		n.defaultReturnValue(0);
		
		final ImmutableSparseVector[] successorVectors = new ImmutableSparseVector[outdegree];
		ImmutableSparseVector succI;
		for (int i = 0; i < outdegree; i++) {
			successorVectors[i] = succI = getTfVectorOf(successors[i]);
			for (int t = 0; t < succI.index.length; t++)
				n.addTo(succI.index[t], 1);
		}
		
		Int2DoubleOpenHashMap idf = new Int2DoubleOpenHashMap(n.size());
		
		// computing idf of terms (in the universe of successors)
		for (Entry term2n : n.int2DoubleEntrySet()) {
			idf.put(	term2n.getIntKey(),
						Math.log(outdegree / term2n.getDoubleValue())
					);
		}
		
		for (Entry e : idf.int2DoubleEntrySet())
			if (Double.isInfinite(e.getDoubleValue()) || Double.isNaN(e.getDoubleValue()))
				LOGGER.warn("IDF contains " + e);
		
		// computing measure for each document j
		for (int j = 0; j < outdegree; j++) {
			int nTermsInCurrentDoc = successorVectors[j].index.length;
			
			double maxFreqInJ = 0;
			for (double v : successorVectors[j].value)
				if (v > maxFreqInJ)
					maxFreqInJ = v;
			
			// computing w_ij for each term in doc j, to find the maximum
			// all w_ij are (normalized) frequencies multiplied by idf, so they're non-negative
			double documentUnexpectedness = 0;
			for (int index = 0; index < nTermsInCurrentDoc; index++) {
				int i = successorVectors[j].index[index]; // global index of term
				double f_ij = successorVectors[j].value[index]; // frequency of i in doc j
				double w_ij = (f_ij / maxFreqInJ) * idf.get(i); 
				if (documentUnexpectedness < w_ij)
					documentUnexpectedness = w_ij;
			}
			
			// ok, measure for document j computed
			results.put(successors[j], documentUnexpectedness);
			
		}
		
		return results;
		
	}
	
	public String toString() { return "M4"; }

}

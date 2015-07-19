package efen.scorers.textual;

import java.io.IOException;

import it.unimi.di.archive4j.Archive;
import it.unimi.di.archive4j.ArrayDocumentSummary;
import it.unimi.di.law.vector.ImmutableSparseVector;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.webgraph.ImmutableGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.SerializationUtils;

public class JacquenetM2Scorer extends TextualUnexpectednessScorer {
	final public static Logger LOGGER = LoggerFactory.getLogger(JacquenetM2Scorer.class);
	
	@SuppressWarnings("unchecked")
	public JacquenetM2Scorer(String graphBasename, String archiveFile) throws IOException {
		this(ImmutableGraph.load(graphBasename), (Archive<ArrayDocumentSummary>) SerializationUtils.readArchive4j(archiveFile));
	}
	
	public JacquenetM2Scorer(ImmutableGraph graph, Archive<ArrayDocumentSummary> archive) {
		super(graph, archive);
	}

	
	static enum MeasureFormula {
		M1 { double U(double tf_ij, double tf_ic) {
				return Math.max(0, 1 - (tf_ic / tf_ij));
		} }, 
		M2 { double U(double tf_ij, double tf_ic) {
				return Math.max(0, tf_ij - tf_ic);
		} };
		abstract double U(double tf_ij, double tf_ic);
	}
	
	final MeasureFormula M = MeasureFormula.M2;
	
	@Override
	public Int2DoubleMap scores(int nodeIndex) {
		final int[] successors = super.successors(nodeIndex).toIntArray();
		final int outdegree = successors.length;
		Int2DoubleMap scores = new Int2DoubleOpenHashMap(outdegree);
		
		// term index -> frequency of that term in the concatenation of successors
		Int2DoubleOpenHashMap fc = new Int2DoubleOpenHashMap();
		
		// build concatened document and caching vectors
		final ImmutableSparseVector[] successorVectors = new ImmutableSparseVector[outdegree];
		ImmutableSparseVector succI;
		for (int i = 0; i < outdegree; i++) {
			successorVectors[i] = succI = getTfVectorOf(successors[i]);
			for (int t = 0; t < succI.index.length; t++)
				fc.addTo(succI.index[t], succI.value[t]);
		}
		
		// computing normalization factor for concatenated document
		double normalizationOfC = 0;
		for (double v : fc.values())
			if (v > normalizationOfC)
				normalizationOfC = v;
		
		// computing measure for each document j
		for (int j = 0; j < outdegree; j++) {
			int nTermsInCurrentDoc = successorVectors[j].index.length;
			double documentUnexpectedness = 0, termUnexpectedness;
			
			double normalizationOfJ = 0;
			for (double v : successorVectors[j].value)
				if (v > normalizationOfJ)
					normalizationOfJ = v;
			
			// for this document, I'll sum U_i,j,c for each term i
			for (int i = 0; i < nTermsInCurrentDoc; i++) {
				double tf_ic = fc.get(successorVectors[j].index[i]) - successorVectors[j].value[i];
				tf_ic /= normalizationOfC;
				termUnexpectedness = M.U(
						(successorVectors[j].value[i] / normalizationOfJ) ,
						tf_ic
					);
				documentUnexpectedness += termUnexpectedness;
			}
			
			// ok, measure for document j computed
			
			documentUnexpectedness /= nOfTerms;
			
			scores.put(successors[j], documentUnexpectedness);
			
		}
		
		return scores;
		
	}
	

	public String toString() { return M.name(); }

}

package efen.scorers.textual;

import it.unimi.di.archive4j.AbstractFeaturizer;
import it.unimi.di.archive4j.Archive;
import it.unimi.di.archive4j.ArrayDocumentSummary;
import it.unimi.di.archive4j.DocumentSummary;
import it.unimi.di.law.vector.ImmutableSparseVector;
import it.unimi.di.law.vector.Vector;
import it.unimi.dsi.webgraph.ImmutableGraph;

import java.io.IOException;

import efen.UnexpectednessScorer;

public abstract class TextualUnexpectednessScorer extends UnexpectednessScorer {
	
	final Archive<ArrayDocumentSummary> archive;
	final AbstractFeaturizer<DocumentSummary, ImmutableSparseVector> featurizer;
	final int nOfTerms, numNodes;
	
	public TextualUnexpectednessScorer(
			ImmutableGraph graph,
			Archive<ArrayDocumentSummary> archive
	) {
		super(graph);
		this.archive = archive;
		this.nOfTerms = archive.numberOfTerms();
		this.featurizer = new FrequenciesFeaturizer<DocumentSummary>(archive);
		numNodes = graph.numNodes();
		assert archive.numberOfDocuments() == numNodes;
	}
	
	public ImmutableSparseVector getTfVectorOf(int nodeIndex) {
		 try {
			return featurizer.get(archive.getDocumentByIndex(nodeIndex));
		} catch (IOException e) {
			throw new RuntimeException("Archive unreadable", e);
		}
	}
	
	public static double cosineSimilarity(Vector a, Vector b) {
		return a.dotProduct(b) / (a.ell2Norm() * b.ell2Norm());
	}
	

//	@Override
//	public double score(int documentIndex, int linkedDocumentIndex) {
//		return scores(documentIndex).get(linkedDocumentIndex);
//	}

}
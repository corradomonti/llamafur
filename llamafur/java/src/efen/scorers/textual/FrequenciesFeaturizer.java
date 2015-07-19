package efen.scorers.textual;

import it.unimi.di.archive4j.AbstractFeaturizer;
import it.unimi.di.archive4j.Archive;
import it.unimi.di.archive4j.DocumentSummary;
import it.unimi.di.law.vector.ImmutableSparseVector;
import cern.colt.Sorting;

public class FrequenciesFeaturizer<T extends DocumentSummary> extends AbstractFeaturizer<T, ImmutableSparseVector> {
		
		private static final long serialVersionUID = 1992L;
		private final int totalTerms;
		private int cutAt;


		public FrequenciesFeaturizer( final Archive<? extends T> archive, int cutAt ) {
			super( archive );
			this.totalTerms = archive.numberOfTerms();
			this.cutAt = cutAt;
		}
		
		public FrequenciesFeaturizer( final Archive<? extends T> archive ) {
			this( archive, Integer.MAX_VALUE);
		}
		
		@SuppressWarnings("unchecked")
		public ImmutableSparseVector get( final Object o ) {
			final T summary = (T)o;

			final int nTerms = summary.size();

			double[] scores = new double[ nTerms ];
			final double[] scores2 = new double[ nTerms ];
			int[] terms = new int[ nTerms ];


			//final int totalOccourrences = summary.length();
			for ( int i = 0; i < nTerms; i++ ) {
				final int term = summary.term( i );
				final int count = summary.count( i );

				terms[ i ] = term;
				scores[ i ] = scores2[ i ] = (double)count;// / (double)totalOccourrences;
			}

			double threshold = 0.0;

			if ( cutAt > nTerms && cutAt < Integer.MAX_VALUE ) {
				Sorting.mergeSort( scores2, 0, scores2.length );
				threshold = scores2[ scores2.length - cutAt + 1 ];
			}

			return ImmutableSparseVector.getInstance( terms, scores, totalTerms, threshold, summary.id() );
		}


	}


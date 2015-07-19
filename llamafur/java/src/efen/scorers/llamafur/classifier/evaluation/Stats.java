package efen.scorers.llamafur.classifier.evaluation;

public enum Stats {
	ACCURACY {
		public double compute(long tp, long tn, long fp, long fn) {
			return (double) (tp + tn) / (fp + tp + fn + tn);
		}
	},
	BALANCED_ACCURACY {
		public double compute(long tp, long tn, long fp, long fn) {
			return ((double) tp / (fp + tp) + (double) tn / (fn + tn)) / 2.0;
		}
	},
	PRECISION {
		public double compute(long tp, long tn, long fp, long fn) {
			return (double) tp / (tp + fp);
		}
	},
	RECALL {
		public double compute(long tp, long tn, long fp, long fn) {
			return (double) tp / (tp + fn);
		}
	},
	FMEASURE {
		public double compute(long tp, long tn, long fp, long fn) {
			double precision = PRECISION.compute(tp, tn, fp, fn),
			 		recall = RECALL.compute(tp, tn, fp, fn);
			return 2 * (precision * recall) / (precision + recall);
		}
	};
	
	public abstract double compute(long tp, long tn, long fp, long fn);
	
}
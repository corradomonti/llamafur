package efen.evaluation.measure;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.io.IOException;

import com.martiansoftware.jsap.ParseException;

import utils.CommandLine;
import efen.UnexpectednessScorer;
import efen.evaluation.GroundTruth;
import efen.evaluation.PoolSpecification;

public class BPrefMeasure extends AbstractMeasure {
	
	@CommandLine(argNames = {"poolSpecificationFile", "evaluationDataset", "pageName2id"})
	public BPrefMeasure(
		String poolSpecificationFile, String datasetPath, Object2IntMap<String> name2id
	) throws ParseException, IOException  {
		super(
				new PoolSpecification(poolSpecificationFile),
				GroundTruth.fromUTF8FilePath(datasetPath, name2id)
			);
	}

	@Override
	public double computeForQuery(int query, UnexpectednessScorer retriever) {
		IntSet evaluatedDocs = groundtruth.getEvaluatedDocs(query);
		double R = groundtruth.getRelevants(query).size();
		double N = evaluatedDocs.size() - R;
		double minNR = Math.min(N, R);
		
		int[] retrieved = retriever.results(query);
		
		double bpref = 0, nonRelevantRankedFirst = 0;
		for (int doc : retrieved)
			if (evaluatedDocs.contains(doc)) {
				if (groundtruth.isRelevant(query, doc)) {
					bpref += 1.0 - (nonRelevantRankedFirst / minNR);
				} else {
					if (nonRelevantRankedFirst < R)
						nonRelevantRankedFirst ++;

				}
			}
		
		bpref /= R;
		printResult(this + "\t" + retriever + "\t" + query(query) + "\t" + bpref);
		
		return bpref;
	}

	public String toString() {
		return "BPREF";
	}

	
	public static void main(String[] args) throws Exception {
		AbstractMeasure.main(args, BPrefMeasure.class);
	}

}

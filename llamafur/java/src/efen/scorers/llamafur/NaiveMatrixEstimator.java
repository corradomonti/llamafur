package efen.scorers.llamafur;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.NodeIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.SerializationUtils;
import efen.scorers.llamafur.data.ArrayMatrix;
import efen.scorers.llamafur.data.Matrix;


public class NaiveMatrixEstimator {
	public static Logger LOGGER = LoggerFactory.getLogger(NaiveMatrixEstimator.class);
	
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		if (args.length != 3 || args[0].equals("--help")) {
			System.out.println("Usage:\n" + NaiveMatrixEstimator.class.getName() + " <page2cat> <graph> <outputpath>");
			System.exit(-1);
		}
		
		Int2ObjectMap<IntSet> page2cat = (Int2ObjectMap<IntSet>) SerializationUtils.read(args[0]);
		ImmutableGraph g = SerializationUtils.readGraph(args[1]);
		
		LOGGER.info("Finding number of categories...");
		int nCat = 0;
		for (IntSet cats : page2cat.values())
			for (int c : cats)
				if (c > nCat)
					nCat = c;
		nCat++;
		
		Matrix nLinked = new ArrayMatrix(nCat);
		int[] nTotal = new int[nCat];
		
		ProgressLogger pl = new ProgressLogger(LOGGER, "pages");
		pl.expectedUpdates = g.numNodes();
		pl.start("Counting links...");
		
		NodeIterator nodeIterator = g.nodeIterator();
		for (int i = nodeIterator.nextInt(); nodeIterator.hasNext(); i = nodeIterator.nextInt()) {
			IntSet catOfI = page2cat.get(i);
			for (int ci : catOfI)
				nTotal[ci]++;
			
			LazyIntIterator succs = nodeIterator.successors();
			for (int j; (j = succs.nextInt()) != -1;) {
				for (int ci : catOfI) {
					for (int cj : page2cat.get(j)) {
						nLinked.addTo(ci, cj, 1);
					}
				}
			}
			
			pl.lightUpdate();
		}
		pl.done();
		
		LOGGER.info("Saving...");
		
		double w;
		Matrix m = new ArrayMatrix(nCat);
		for (int i = 0; i < nCat; i++)
			for (int j = 0; j < nCat; j++) {
				w = 	Math.log(nLinked.get(i, j) + 1)
						- 	( 	Math.log(nTotal[i] + 1) + Math.log(nTotal[j] + 1) );
//				if (Double.isNaN(w)) {
//					LOGGER.warn("NaN value obtained for " + i + ", "+ j + ": " +
//							nLinked.get(i, j) + " linked over " + nTotal[i] + 
//							" pages for the first category and " + nTotal[j] +
//							" for the second. Substituting NaN with negative infinity."
//					);
//					w = Double.NEGATIVE_INFINITY;
//				}
				m.put(i, j, w);
			}
		
		SerializationUtils.save(m, args[2]);

		LOGGER.info("Done (additive smoothing).");
		
	}

}

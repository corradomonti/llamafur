package utils;

import it.unimi.dsi.fastutil.ints.AbstractIntComparator;
import it.unimi.dsi.fastutil.ints.IntComparator;

public class ArrayUtils {
	
	public static IntComparator reverseIndirectComparator(final double[] x) {
		return new AbstractIntComparator(){

			@Override
			public int compare(int k1, int k2) {
				return Double.compare(x[k2], x[k1]);
			}
			
		};
	}
	
	public static IntComparator indirectComparator(final double[] x) {
		return new AbstractIntComparator(){

			@Override
			public int compare(int k1, int k2) {
				return Double.compare(x[k1], x[k2]);
			}
			
		};
	}

}

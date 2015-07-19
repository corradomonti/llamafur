package utils;

import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectFunction;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.objects.AbstractObject2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.logging.ProgressLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapUtils {
	public static Logger LOGGER = LoggerFactory.getLogger(MapUtils.class);

	final public static Int2ObjectFunction<String> NUMBER_PRINTER = new AbstractInt2ObjectFunction<String>(){
		private static final long serialVersionUID = 1L;

		@Override
		public String get(int key) {
			return Integer.toString(key);
		}

		@Override
		public boolean containsKey(int key) {
			return true;
		}

		@Override
		public int size() {
			return Integer.MAX_VALUE;
		}
		
	};
	
	final public static Object2IntFunction<String> NUMBER_READER = new AbstractObject2IntFunction<String>(){
		private static final long serialVersionUID = 1L;

		@Override
		public int getInt(Object key) {
			if (! (key instanceof String))
				return defRetValue;
			try {
				return Integer.parseInt((String) key);
			} catch (NumberFormatException e) {
				return defRetValue;
			}
		}

		@Override
		public boolean containsKey(Object key) {
			if (! (key instanceof String))
				return false;
			else {
				try {
					Integer.parseInt((String) key);
					return true;
				} catch (NumberFormatException e) {
					return false;
				}
			}
		}

		@Override
		public int size() {
			return Integer.MAX_VALUE;
		}

		
	};

	public static Class<?> invertMapType(Class<?> cls) throws ClassNotFoundException {
		String[] mapType = StringUtils.splitByCharacterTypeCamelCase(cls.getSimpleName());
		String type1 = mapType[0];
		if (!mapType[1].equals("2"))
			throw new IllegalArgumentException(cls + " is not a fastutil map.");
		String type2 = mapType[2];
		mapType[0] = type2;
		mapType[2] = type1;
		String newType = StringUtils.join(mapType);
		newType = "it.unimi.dsi.fastutil." + type2.toLowerCase() + "s." + newType;
		return Class.forName(newType);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Map invert(Map inputMap) throws InstantiationException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, ClassNotFoundException {
		LOGGER.info("Inverting map...");
		Map outputMap = (Map) invertMapType(inputMap.getClass()).getConstructor(new Class[] {}).newInstance(new Object[] {});
		
		ProgressLogger pl = new ProgressLogger(LOGGER, "entries");
		pl.expectedUpdates = inputMap.size();
		pl.start();
		
		for (Object entryObj : inputMap.entrySet()) {
			Map.Entry entry = (Map.Entry) entryObj;
			Object oldValue = outputMap.put(entry.getValue(), entry.getKey());
			if (oldValue != null)
				throw new IllegalArgumentException(
						"The value " + entry.getValue() + " is associated to both '" +
						oldValue + "' and '" + entry.getKey() + "'. The map is not" +
						"bijective"
				);
			pl.lightUpdate();
		}
		pl.done();
		return outputMap;
	}
	
	public static IntComparator comparatorPuttingLargestMappedValueFirst(final Int2DoubleMap map) {
		return new IntComparator() {
			public int compare(Integer o1, Integer o2) { return compare(o1.intValue(), o2.intValue()); }
			public int compare(int k1, int k2) {
				return Double.compare(map.get(k2), map.get(k1));
			}
		};
	}
	
	public static IntComparator comparatorPuttingLargestMappedValueFirstExceptNaN(final Int2DoubleMap map) {
		return new IntComparator() {
			public int compare(Integer o1, Integer o2) { return compare(o1.intValue(), o2.intValue()); }
			public int compare(int k1, int k2) {
				double d2 = map.get(k2);
				double d1 = map.get(k1);
				if (Double.isNaN(d2)) d2 = Double.NEGATIVE_INFINITY;
				if (Double.isNaN(d1)) d1 = Double.NEGATIVE_INFINITY;
				return Double.compare(d2, d1);
			}
		};
	}
	
	public static IntComparator comparatorPuttingSmallestMappedValueFirst(final Int2DoubleMap map) {
		return new IntComparator() {
			public int compare(Integer o1, Integer o2) { return compare(o1.intValue(), o2.intValue()); }
			public int compare(int k1, int k2) {
				return Double.compare(map.get(k1), map.get(k2));
			}
		};
	}
	
	public static void truncateMapByHighestValue(Int2DoubleMap map, int howMany) {
		if (howMany >= map.size())
			return;
		
		double[] values = map.values().toDoubleArray();
		Arrays.sort(values); // not best strategy, i know
		org.apache.commons.lang.ArrayUtils.reverse(values);
		double threshold = values[howMany+1];
		
		ObjectIterator<Entry> mapIterator = map.int2DoubleEntrySet().iterator();
		int inserted = 0;
		while (mapIterator.hasNext())
				if (mapIterator.next().getDoubleValue() < threshold || inserted >= howMany)
					mapIterator.remove();
				else
					inserted++;
	}
}

package utils;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomSingleton {

	public static Logger LOGGER = LoggerFactory.getLogger(Random.class);
	private static Random instance = null;
	private static long seed = System.currentTimeMillis();
	
	public static Random get() {
		if (instance == null) {
			String caller;
			try {
				caller = new Exception().getStackTrace()[1].getClassName();
			} catch (Exception e) {
				e.printStackTrace();
				caller = "???";
			}
			LOGGER.info("Random needed by "+caller+": setting seed to " + seed);
			instance = new Random(seed);
		}
		return instance;
	}
	
	public static void seedWith(long newSeed) {
		if (instance != null)
			throw new IllegalStateException("You cannot set seed after an instance has been already requested.");
		else
			seed = newSeed;
	}
	
}

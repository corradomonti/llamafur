package efen.categories;

import java.io.IOException;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.logging.ProgressLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.martiansoftware.jsap.JSAPException;

import utils.CommandLine;
import utils.JsapUtils;
import utils.SerializationUtils;

public class PagesCategorizationMover {
	public static Logger LOGGER = LoggerFactory.getLogger(PagesCategorizationMover.class);
	
	final int[] closestMilestones;
	final Int2ObjectMap<IntSet> page2cat;
	final String outputPath;
	
	@CommandLine(argNames={"closestMilestones", "page2cat", "outputPath"})
	public PagesCategorizationMover(int[] closestMilestones, Int2ObjectMap<IntSet> page2cat, String outputPath) {
		this.closestMilestones = closestMilestones;
		this.page2cat = page2cat;
		this.outputPath = outputPath;
	}
	
	public void compute() {
		ProgressLogger pl = new ProgressLogger(LOGGER, "pages");
		pl.expectedUpdates = page2cat.size();
		pl.start("Moving old categories to closest milestones...");
		for (IntSet entry : page2cat.values()) {
			IntSet newCategories = new IntOpenHashSet();
			int milestone;
			for (int cat : entry) {
				milestone = closestMilestones[cat];
				if (milestone != -1)
					newCategories.add(milestone);
			}
			entry.clear();
			entry.addAll(newCategories);
			pl.lightUpdate();
		}
		pl.done();
		
	}


	public void save() {
		LOGGER.info("Saving new page2cat to " + outputPath + "...");
		SerializationUtils.saveSafe(page2cat, outputPath);
		LOGGER.info("Done.");
	}
	
	public static void main(String[] rawArgs) throws JSAPException, IOException, ReflectiveOperationException {
		PagesCategorizationMover mover = JsapUtils.constructObject(
				 	PagesCategorizationMover.class,
					rawArgs,
					"Move the associated categories of page->categories "
					+ "according to a vector x of closestMilestones, where "
					+ "a category i is replaced by x[i] iff x[i] != -1, else "
					+ "it is eliminated."
					);
		mover.compute();
		mover.save();
	}
	

}

package efen.categories;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;
import it.unimi.dsi.webgraph.LazyIntIterator;
import it.unimi.dsi.webgraph.Transform;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import utils.CommandLine;
import utils.JsapUtils;
import utils.SerializationUtils;

import com.martiansoftware.jsap.JSAPException;


public class HittingDistanceMinimizer {
	public static final Logger LOGGER = LoggerFactory.getLogger(HittingDistanceMinimizer.class);
	
	final ImmutableGraph graph;
	final int[] minMilestoneDistance;
	final int[] closestMilestone;
	final IntSet milestones;
	final ObjectSet<Visitor> runningVisitors;
	final IntPriorityQueue milestoneQueue;
	final ProgressLogger pl;

	@CommandLine(argNames={"graph", "milestones"})
	public HittingDistanceMinimizer(ImmutableGraph graph, IntSet milestones) {
		this.graph = Transform.transpose(graph);
		this.milestones = milestones;
		minMilestoneDistance = new int[graph.numNodes()];
		IntArrays.fill(minMilestoneDistance, Integer.MAX_VALUE);
		closestMilestone = new int[graph.numNodes()];
		IntArrays.fill(closestMilestone, -1);
		milestoneQueue = new IntArrayPriorityQueue(milestones.toIntArray());
		runningVisitors = new ObjectOpenHashSet<Visitor>();
		pl  = new ProgressLogger(LOGGER, "milestones");
		pl.expectedUpdates = milestones.size();
		
	}
	
	private class Visitor extends Thread {
		
		final int start;
		final int[] dists;
		final ImmutableGraph graph;
		
		Visitor(final ImmutableGraph graph, int startingNode) {
			this.start = startingNode;
			dists = new int[ graph.numNodes() ];
			this.graph = graph.copy();
		}
		
		@Override
		public void run() {
			final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
			
			IntArrays.fill( dists, Integer.MAX_VALUE ); // Initially, all distances are infinity.
			
			int curr, succ;
			queue.enqueue( start );
			dists[ start ] = 0;

			LazyIntIterator successors;

			while( ! queue.isEmpty() ) {
				curr = queue.dequeueInt();
				successors = graph.successors( curr );
				int d = graph.outdegree( curr );
				while( d-- != 0 ) {
					succ = successors.nextInt();
					if ( dists[ succ ] == Integer.MAX_VALUE  ) {
						dists[ succ ] = dists[ curr ] + 1;
						queue.enqueue( succ );
					}
				}
			}
			
			startNewThreadAfter(this);
		}
		
		@Override
		public int hashCode() { return start; }
		
		@Override
		public boolean equals(Object o) {
			return (((o instanceof Visitor)) &&  ((Visitor) o).start == this.start);
		}
	}
	
	private synchronized void startNewThreadAfter(Visitor thread) {
		if (thread != null) {
			if (!runningVisitors.remove(thread)) {
				throw new IllegalStateException(
						"Thread " + thread + " signaled completion but was not present.");
			}
			updateClosestMilestonesAfter(thread.start, thread.dists);
			pl.update();
		}
		
		if (!milestoneQueue.isEmpty()) {
			int milestone = milestoneQueue.dequeueInt();
			Visitor visitor = new Visitor(graph, milestone);
			runningVisitors.add(visitor);
			visitor.start();
		} else 
			if (runningVisitors.isEmpty()) {
				synchronized (this) {
					this.notifyAll();
				}
			}
	}
	

	private void updateClosestMilestonesAfter(int milestone, int[] distances) {
		final int numNodes = graph.numNodes();
		for (int node = 0; node < numNodes; node++) {
			if (distances[node] < minMilestoneDistance[node]) {
				minMilestoneDistance[node] = distances[node];
				closestMilestone[node] = milestone;
			}
		}
	}
	
	public int[] compute() {
		return compute(Runtime.getRuntime().availableProcessors());
	}
	
	public int[] compute(int nOfThreads) {
		pl.start("Starting a BFS for each milestone (with " + nOfThreads + " parallel threads)...");
		for (int i = 0; i < nOfThreads; i++) {
			startNewThreadAfter(null);
		}
		try {
			synchronized (this) {
				while (!milestoneQueue.isEmpty())
					this.wait();
			}
		} catch (InterruptedException e) { throw new RuntimeException(e); }
		
		pl.done();
		
		return closestMilestone;
		
	}
	
	public static void main(String[] rawArgs) throws JSAPException, IOException, ReflectiveOperationException {
		String outputPath = "closestMilestones.ser";
		HittingDistanceMinimizer hdm = JsapUtils.constructObject(
				HittingDistanceMinimizer.class,
				rawArgs,
				"Save a serialized vector of int[] where the int in position i "
				+ "is the index of the node in the milestones set closest to "
				+ "the node i (minimizing distance from i to the milestone set). "
				+ "This output is saved to " + outputPath + "."
				);
		int[] closestMilestones = hdm.compute();
		LOGGER.info("Saving output...");
		SerializationUtils.saveSafe(closestMilestones, outputPath);
		LOGGER.info("Done.");
	}

	
}

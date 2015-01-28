package sequence;

import gui.Event;
import gui.GUIModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import summary.EventHandlerSummary;
import summary.PathSummary;
import summary.SymbolicStates;
import analysis.Expression;

public class Job {

	private EventSequence eventSeq = new EventSequence();
	private List<PathSummary> psSeq = new ArrayList<PathSummary>();
	private JobAssistant jobAssistant = new JobAssistant();
	private boolean killMe = false;
	
	private List<EventSequence> completeSequences = new ArrayList<EventSequence>();
	private List<Job> newJobs = new ArrayList<Job>();
	
	public Job(EventSequence eventSeq, List<PathSummary> psSeq) {
		this.eventSeq = eventSeq;
		this.psSeq = psSeq;
	}
	
	public Job(EventSequence eventSeq, PathSummary ps) {
		this(eventSeq, new ArrayList<PathSummary>(Arrays.asList(ps)));
	}
	
	public void extendSequence(List<EventHandlerSummary> summaries, GUIModel guiModel, SymbolicStates initialStates) {
		/**
		 * get most recent PS and event
		 * for (each cond in PS) {
		 * 		if not about $Fstatic and #
		 * 			continue;
		 * 		find anchors;
		 * 		for (each anchors)
		 * 			extend anchor into current sequence
		 * }
		 * */
		PathSummary firstPS = psSeq.get(0);
		Event firstEvent = eventSeq.getList().get(0);
		List<List<PathSummary>> allAnchors = new ArrayList<List<PathSummary>>();
		System.out.println("total of " + firstPS.getPathCondition().size() + " conditions in first PS.");
		for (Expression cond : firstPS.getPathCondition()) {
			// check if this cond is already satisfied, no need for anchor
			SymbolicStates cumuStates = jobAssistant.getCumulativeStates(initialStates, firstPS.getSymbolicStates());
			if (jobAssistant.checkSat(cumuStates.getStates(), cond))
				continue;
			System.out.println("  *" + cond.toYicesStatement());
			List<PathSummary> anchors = jobAssistant.findAnchors(cond, summaries, initialStates);
/*			if (anchors.size() == 0) {
				System.out.println("found 0 anchor for condition: " + cond.toYicesStatement());
				killMe = true;
				return;
			}*/
			if (!allAnchors.contains(anchors))
				allAnchors.add(anchors);
		}
		//possible results:
		// 1. found 0 anchors, job is dead, no complete sequence, no new jobs. done
		// 2. found anchors, get all the "anchor + currentSequence", add connectors to them
		//    the complete sequences validate then goes into completeSequences, incomplete ones will be built as new jobs
		List<List<PathSummary>> anchorPermutations = jobAssistant.getAnchorPermutations(allAnchors);
		// sift out non-satisfiable anchors
		//System.out.println("total of " + anchorPermutations.size() + " permutations");
		List<List<PathSummary>> legitAnchorSets = new ArrayList<List<PathSummary>>();
		for (List<PathSummary> anchorSet : anchorPermutations) {
			SymbolicStates cumuStates = jobAssistant.getCumulativeStates(initialStates, new ArrayList<PathSummary>(anchorSet));
			//System.out.println("*cumulative states for "  + anchorSet.size() + "\n"+ anchorSet + ":");
			//for (Expression expr : cumuStates.getStates()) {
			//	System.out.println("   " + expr.toYicesStatement());
			//}
			if (jobAssistant.checkSat(cumuStates.getStates(), firstPS.getPathCondition()) && !legitAnchorSets.contains(anchorSet))
				legitAnchorSets.add(anchorSet);
		}
		// 
		System.out.println("found " + legitAnchorSets.size() + " anchor sets for " + firstPS);
		for (List<PathSummary> anchorSet : legitAnchorSets) {
			// gui.fillConnectors(anchorSet, currentSeq)
		}
	}
	
	public void sequenceComplete() {
		
	}

	
	
	public List<Job> getUnfinishedJobs() {
		return newJobs;
	}

	public List<EventSequence> getCompletedSequences() {
		return completeSequences;
	}
	


}

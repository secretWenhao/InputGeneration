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
	
	private List<EventSequence> extendedEventSeq = new ArrayList<EventSequence>();
	
	
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
		for (Expression cond : firstPS.getPathCondition()) {
			//TODO find anchors
			List<PathSummary> anchors = jobAssistant.findAnchors(cond, summaries, initialStates);
			if (anchors.size() == 0)
				System.out.println("this job is dead.");
			
		}
	}
	
	public void sequenceComplete() {
		
	}

	public List<Job> getUnfinishedJobs() {
		List<Job> newJobs = new ArrayList<Job>();
		
		return newJobs;
	}

	public List<EventSequence> getValidatedSequences() {
		List<EventSequence> result = new ArrayList<EventSequence>();
		
		return result;
	}
	

}

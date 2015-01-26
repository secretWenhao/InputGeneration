package sequence;

import gui.GUIModel;

import java.util.ArrayList;
import java.util.List;

import summary.EventHandlerSummary;
import summary.PathSummary;
import summary.SymbolicStates;
import analysis.Expression;

public class Generator {

	private List<PathSummary> psList;
	public List<EventHandlerSummary> summaries;
	private GUIModel guiModel;
	private SymbolicStates initialStates;

	
	private Interfacing intrf = new Interfacing();
	
	public Generator(List<PathSummary> psList, GUIModel guiModel, SymbolicStates initialStates) {
		this.psList = psList;
		this.summaries = intrf.buildEventHandlerSummary(psList);
		this.guiModel = guiModel;
		this.initialStates = initialStates;
	}
	
	public List<EventSequence> findSequences(String lineInfo) {
		List<EventSequence> results = new ArrayList<EventSequence>();
		
		List<Job> jobs = initJobs(lineInfo);
		
		System.out.println("got " + jobs.size() + " jobs");

		while (jobs.size() > 0) {
			List<Job> newJobs = new ArrayList<Job>();
			for (Job job : jobs) {
				job.extendSequence(summaries, guiModel, initialStates);
				newJobs.addAll(job.getUnfinishedJobs());
				results.addAll(job.getValidatedSequences());
			}
			jobs = newJobs;
		}
		
		return results;
	}

	public List<Job> initJobs(String lineInfo) {
		/**
		 * 1. find all path summaries
		 * 2. create job for each PS
		 * 3. initiate job:  eventSeq, psSeq
		 */
		List<Job> jobs = new ArrayList<Job>();
		for (PathSummary ps : psList) {
			if (!ps.getExecutionLog().contains(lineInfo))
				continue;
			Job job = new Job(ps.getEventSequence(), ps);
			jobs.add(job);
		}
		return jobs;
	}

	
	
}

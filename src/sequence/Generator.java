package sequence;

import gui.GUIModel;

import java.util.ArrayList;
import java.util.List;

import summary.EventHandlerSummary;
import summary.PathSummary;
import summary.SymbolicStates;
import analysis.Expression;

public class Generator {

	public List<PathSummary> psList;
	private List<EventHandlerSummary> summaries;
	private GUIModel guiModel;
	private SymbolicStates initialStates;

	private Interfacing intrf = new Interfacing();
	
	public Generator(List<PathSummary> psList, GUIModel guiModel, SymbolicStates initialStates) {
		this.psList = psList;
		this.guiModel = guiModel;
		this.initialStates = initialStates;
		trimExpressions();
		convertExpressions();
		intrf.printYicesVariableMap();
		this.summaries = intrf.buildEventHandlerSummary(psList);
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

	private void convertExpressions() {
		for (PathSummary ps : psList)
			intrf.trainYicesVariableMap(ps.getSymbolicStates());
		intrf.trainYicesVariableMap(this.initialStates.getStates());
		for (PathSummary ps : psList) {
			ArrayList<Expression> newStates = new ArrayList<Expression>();
			for (Expression expr : ps.getSymbolicStates()) {
				expr.toYiceFormat();
				newStates.add(intrf.convertToYicesVariables(expr));
			}
			ArrayList<Expression> newConds = new ArrayList<Expression>();
			for (Expression expr : ps.getPathCondition()) {
				expr.toYiceFormat();
				newConds.add(intrf.convertToYicesVariables(expr));
			}
			ps.setSymbolicStates(newStates);
			ps.setPathCondition(newConds);
		}
		ArrayList<Expression> newStates = new ArrayList<Expression>();
		for (Expression expr : this.initialStates.getStates()) {
			expr.toYiceFormat();
			newStates.add(intrf.convertToYicesVariables(expr));
		}
		this.initialStates = new SymbolicStates(newStates);
	}
	
	private void trimExpressions() {
		for (PathSummary ps : this.psList) {
			ArrayList<Expression> newStates = new ArrayList<Expression>();
			for (int i = 0, len = ps.getSymbolicStates().size(); i < len; i++) {
				Expression expr = ps.getSymbolicStates().get(i);
				Expression left = (Expression) expr.getChildAt(0);
				if (left.contains("$Fstatic"))
					newStates.add(expr);
			}
			ps.setSymbolicStates(newStates);
		}
	}
	
}

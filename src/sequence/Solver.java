package sequence;

import gui.GUIModel;

import java.util.ArrayList;
import java.util.List;

import summary.EventHandlerSummary;
import summary.PathSummary;
import summary.SymbolicStates;
import analysis.Expression;

public class Solver {

	public List<PathSummary> findAnchors(Expression cond, Job job, List<EventHandlerSummary> summaries, GUIModel guiModel, SymbolicStates initialStates) {
		List<PathSummary> result = new ArrayList<PathSummary>();
		
		
		for (EventHandlerSummary s : summaries) {
			
		}
		
		
		return result;
	}
	
}

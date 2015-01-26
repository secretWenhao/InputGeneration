package sequence;

import gui.GUIModel;

import java.util.ArrayList;
import java.util.List;

import summary.EventHandlerSummary;
import summary.PathSummary;
import summary.SymbolicStates;
import analysis.Expression;

public class JobAssistant {


	public List<PathSummary> findAnchors(Expression cond, List<EventHandlerSummary> summaries, SymbolicStates initialStates) {
		List<PathSummary> result = new ArrayList<PathSummary>();
		SymbolicStates states = initialStates.clone();
		System.out.println("[solving]" + cond.toYicesStatement());
		//TODO find possible anchors
		for (EventHandlerSummary ehs : summaries)
			for (PathSummary ps : ehs.getPSs()) {
				boolean isAnchor = false;
				for (Expression expr : ps.getSymbolicStates()) {
					Expression left = (Expression) expr.getChildAt(0);
					for (int i = 0; i < cond.getChildCount(); i++) {
						Expression condChild = (Expression) cond.getChildAt(i);
						if (condChild.getUserObject().toString().startsWith("#"))
							continue;
						if (left.equals(condChild)) {
							isAnchor = true;
							break;
						}
					}
					if (isAnchor) {
						System.out.println("for cond " + cond.toYicesStatement() + ", found possible anchor " + ps.getEventSequence());
						result.add(ps);
						continue;
					}
				}
			}
		// do a cumulative symbolic states, see if the condition is sat
		List<PathSummary> realResult = new ArrayList<PathSummary>();
		for (int i = 0, len = result.size(); i < len; i++) {
			PathSummary anchor = result.get(i);
			SymbolicStates s = initialStates.clone();
			System.out.println("initial state size " + s.getStates().size());
			for (Expression expr : anchor.getSymbolicStates())
				s.updateSymbolicStates(expr);
			System.out.println("[cumulative states]");
			for (Expression expr : s.getStates())
				System.out.println("  " + expr.toYicesStatement());
			//TODO use cumulative states + cond, put it into yice to see if it's real anchor
			
			realResult.add(anchor);
		}
		return realResult;
	}
	
	
}

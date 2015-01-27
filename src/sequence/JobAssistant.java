package sequence;

import java.util.ArrayList;
import java.util.List;

import main.Paths;
import summary.EventHandlerSummary;
import summary.PathSummary;
import summary.SymbolicStates;
import analysis.Expression;
import constraintSolver.Yices;

public class JobAssistant {

	private Yices yices = new Yices(Paths.yicesPath);
	
	public List<PathSummary> findAnchors(Expression cond, List<EventHandlerSummary> summaries, SymbolicStates initialStates) {
		List<PathSummary> result = new ArrayList<PathSummary>();
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
			SymbolicStates s = getCumulativeStates(initialStates, anchor.getSymbolicStates());
			boolean sat = yices.checkSat(s.getStates(), cond);
			if (sat)
				realResult.add(anchor);
		}
		return realResult;
	}
	
	public SymbolicStates getCumulativeStates(SymbolicStates initialStates, List<Expression> followingStates) {
		SymbolicStates s = initialStates.clone();
		for (Expression expr : followingStates)
			s.updateSymbolicStates(expr.clone());
		return s;
	}
	
}

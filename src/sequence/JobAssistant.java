package sequence;

import java.util.ArrayList;
import java.util.Arrays;
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
		// first check JobHistory, see if answer is already there
		List<PathSummary> result = JobHistory.anchors_for_condition.get(cond);
		if (result != null)
			return result;
		result = new ArrayList<PathSummary>();
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
						result.add(ps);
						break;
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
		JobHistory.anchors_for_condition.put(cond, realResult);
		return realResult;
	}
	
	public SymbolicStates getCumulativeStates(SymbolicStates initialStates, List<Expression> followingStates) {
		SymbolicStates s = initialStates.clone();
		for (Expression expr : followingStates)
			s.updateSymbolicStates(expr.clone());
		return s;
	}
	
	public SymbolicStates getCumulativeStates(SymbolicStates initialStates, ArrayList<PathSummary> anchors) {
		SymbolicStates s = initialStates.clone();
		for (PathSummary ps : anchors)
			for (Expression expr : ps.getSymbolicStates())
				s.updateSymbolicStates(expr.clone());
		return s;
	}

	public List<List<PathSummary>> getAnchorPermutations(List<List<PathSummary>> l) {
		List<List<PathSummary>> result = new ArrayList<List<PathSummary>>();
		List<List<List<PathSummary>>> allOrderPerms = getPerms(l);
		for (List<List<PathSummary>> oneOrderPerm: allOrderPerms) {
			List<List<PathSummary>> seqForOneOrder = getSequencesForOneOrder(oneOrderPerm);
			result.addAll(seqForOneOrder);
			//System.out.println("\n" + oneOrderPerm + " " + seqForOneOrder.size());
			//System.out.println("  " + seqForOneOrder);
		}
		//System.out.println(result.size() + "\n" + result);
		return result;
	}
	
	private List<List<List<PathSummary>>> getPerms(List<List<PathSummary>> all) {
		if (all.size() < 1)
			return new ArrayList<List<List<PathSummary>>>();
		List<List<List<PathSummary>>> result = new ArrayList<List<List<PathSummary>>>();
		for (int i = 0; i < all.size(); i++) {
			List<PathSummary> first = all.get(i);
			List<List<PathSummary>> newAll = new ArrayList<List<PathSummary>>(all);
			newAll.remove(i);
			List<List<List<PathSummary>>> permOfRest = getPerms(newAll);
			for (List<List<PathSummary>> onePerm : permOfRest)
				onePerm.add(0, first);
			if (permOfRest.size() == 0) {
				List<List<PathSummary>> initList = new ArrayList<List<PathSummary>>();
				initList.add(first);
				permOfRest.add(initList);
			}
			result.addAll(permOfRest);
		}
		return result;
	}
	
	private List<List<PathSummary>> getSequencesForOneOrder(List<List<PathSummary>> allSet) {
		
		if (allSet.size() == 0)
			return new ArrayList<List<PathSummary>>();
		
		List<PathSummary> firstSet = allSet.remove(0);

		List<List<PathSummary>> seqOfRestSet = getSequencesForOneOrder(allSet);
		List<List<PathSummary>> result = new ArrayList<List<PathSummary>>();
		if (seqOfRestSet.size() == 0) {
			for (PathSummary anchor: firstSet)
				result.add(new ArrayList<PathSummary>(Arrays.asList(anchor)));
		}
		else {
			for (PathSummary anchor : firstSet) {
				for (List<PathSummary> eachSeq : seqOfRestSet) {
					List<PathSummary> newSeq = new ArrayList<PathSummary>(eachSeq);
					newSeq.add(0, anchor);
					result.add(newSeq);
				}
			}
		}
		return result;
	}
	
	public boolean checkSat(List<Expression> states, Expression cond) {
		return yices.checkSat(states, cond);
	}
	
	public boolean checkSat(List<Expression> states, List<Expression> conds) {
		return yices.checkSat(states, conds);
	}
	
}

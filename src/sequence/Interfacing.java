package sequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import constraintSolver.YicesVariable;
import summary.EventHandlerSummary;
import summary.PathSummary;
import analysis.Expression;
import analysis.ExpressionFormat;

public class Interfacing {

	private int yicesVIndex = 1;
	public Map<Expression, YicesVariable> yicesVariableMap = new HashMap<Expression, YicesVariable>();
	
	
	public void trainYicesVariableMap(List<Expression> exprList) {
		for (Expression expr : exprList) {
			addToMap(expr);
		}
	}
	
	public Expression convertToYicesVariables(Expression expr) {
		Expression result = expr.clone();
		if (alreadyHasExpression(expr)) {
			YicesVariable yv = this.yicesVariableMap.get(expr);
			Expression newExpr = new Expression(yv.name);
			return newExpr;
		}
		else
			for (int i = 0; i < result.getChildCount(); i++) {
				Expression child = (Expression) result.getChildAt(i);
				result.remove(i);
				result.insert(convertToYicesVariables(child), i);
			}
		return result;
	}
	
	private void addToMap(Expression expr) {
		String op = expr.getUserObject().toString();
		if (alreadyHasExpression(expr) || op.startsWith("#"))
			return;
		if (op.equals("$Fstatic")) {
			YicesVariable yv = new YicesVariable();
			yv.name = "s" + yicesVIndex++;
			yv.type = "int";
			yicesVariableMap.put(expr.clone(), yv);
			return;
		}
		if (op.equals("=") || ExpressionFormat.calculationOPs.contains(op) || ExpressionFormat.conditionOPs.contains(op)) {
			Expression left = (Expression) expr.getChildAt(0);
			Expression right = (Expression) expr.getChildAt(1);
			addToMap(left);
			addToMap(right);
		}
	}

	private boolean alreadyHasExpression(Expression expr) {
		for (Expression ex : this.yicesVariableMap.keySet())
			if (ex.equals(expr))
				return true;
		return false;
	}
	

	public List<EventHandlerSummary> buildEventHandlerSummary(List<PathSummary> psList) {
		List<EventHandlerSummary> result = new ArrayList<EventHandlerSummary>();
		for (PathSummary ps : psList) {
			String methodSig = ps.getMethodSignature();
			boolean added = false;
			for (EventHandlerSummary s : result) {
				if (s.getMethodSig().equals(methodSig)) {
					s.addPS(ps);
					added = true;
					break;
				}
			}
			if (!added) {
				EventHandlerSummary s = new EventHandlerSummary();
				s.setMethodSig(methodSig);
				s.addPS(ps);
				result.add(s);
			}
		}
		return result;
	}
	
	public void printYicesVariableMap() {
		for (Map.Entry<Expression, YicesVariable> entry : this.yicesVariableMap.entrySet()) {
			System.out.println("[map]" + entry.getKey().toYicesStatement() + " -> " + entry.getValue().name + "::" + entry.getValue().type);
		}
	}
	
}

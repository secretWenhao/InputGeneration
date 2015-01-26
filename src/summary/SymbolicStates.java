package summary;

import java.util.ArrayList;
import java.util.List;

import analysis.Expression;

public class SymbolicStates {

	private List<Expression> states;
	
	public SymbolicStates(List<Expression> states) {
		this.states = states;
	}
	
	public List<Expression> getStates() {
		return this.states;
	}
	
	public void updateSymbolicStates(Expression newEx) {
		//1  v0 = v1
		//2  v0 = v1 add v2
		//3  v0 = sig $Finstance v1
		//4  v0 = sig $Fstatic v1
		//5  v0 = $return
		Expression left = (Expression) newEx.getChildAt(0);
		Expression right = (Expression) newEx.getChildAt(1);
		// if left is '$Finstance>>a>>v0', replace v0 first
		if (left.getUserObject().toString().equals("$Finstance")) {
			Expression obj = (Expression) left.getChildAt(1);
			Expression updatedObj = this.findExistingExpression(obj);
			if (updatedObj != null) {
				left.remove(1);
				left.insert(updatedObj, 1);
			}
		}
		// if right is '$Finstance>>a>>v0', replace v0 first
		if (right.getUserObject().toString().equals("$Finstance")) {
			Expression obj = (Expression) right.getChildAt(1);
			Expression updatedObj = this.findExistingExpression(obj);
			if (updatedObj != null) {
				right.remove(1);
				right.insert(updatedObj, 1);
			}
		}
		
		int index = getIndexOfOperationWithLeft(left);
		String op = right.getUserObject().toString();
		if (op.equals("$return")) {
			// 5 v0 = $return
			int assignIndex = getIndexOfOperationWithLeft("$return");
			Expression assignEx = this.states.get(assignIndex);
			assignEx.remove(0);
			assignEx.insert(left, 0);
			Expression assignRight = (Expression) assignEx.getChildAt(1);
			ArrayList<Expression> toRemove = new ArrayList<Expression>();
			for (int i = assignIndex+1; i < this.states.size(); i++) {
				Expression ex = this.states.get(i);
				Expression thisLeft = (Expression) ex.getChildAt(0);
				if (ExpressionContains(thisLeft, "$return"))
					thisLeft.replace(new Expression("$return"), assignRight);
				int thisIndex = getIndexOfOperationWithLeft(thisLeft);
				if (thisIndex != i && thisIndex > -1)
					toRemove.add(ex.clone());
			}
			if (index > -1) {
				this.states.remove(index);
			}
			for (Expression ex : toRemove) {
				Expression thisLeft = (Expression) ex.getChildAt(0);
				int i = getIndexOfOperationWithLeft(thisLeft);
				this.states.remove(i);
			}
		}
		else if (right.getChildCount() == 0 || op.equals("$Fstatic")) {
			// 1&4 v0 = v1 or $Fstatic sig
			// left = v0
			// right = v1 or $Fstatic sig
			if (!op.startsWith("#")) {
				Expression updatedRight = this.findExistingExpression(right);
				if (updatedRight != null) {
					newEx.remove(1);
					newEx.insert(updatedRight, 1);
				}
			}
			if (index > -1)
				this.states.remove(index);
			this.states.add(newEx);
		}
		else if (op.equals("$Finstance")) {
			//3 v0 = sig $Finstance v1
			Expression quickUpdate = this.findExistingExpression(right);
			if (quickUpdate != null) {
				newEx.remove(1);
				newEx.insert(quickUpdate, 1);
			} else {
				Expression obj = (Expression) right.getChildAt(1);
				Expression updatedObj = this.findExistingExpression(obj);
				if (updatedObj != null) {
					right.remove(1);
					right.insert(updatedObj, 1);
				}
			}
			if (index > -1)
				this.states.remove(index);
			this.states.add(newEx);
		}
		else {
			//2 v0 = v1 add v2
			// left = v0
			// right = add v1 v2
			for (int i = 0; i < right.getChildCount(); i++) {
				Expression childOfRight = (Expression) right.getChildAt(i);
				Expression updatedChild = this.findExistingExpression(childOfRight);
				if (updatedChild != null) {
					right.remove(i);
					right.insert(updatedChild, i);
				}
			}
			if (index > -1)
				this.states.remove(index);
			this.states.add(newEx);
		}
	}
	
	private int getIndexOfOperationWithLeft(String vName) {
		for (int i = 0; i < this.states.size(); i++) {
			Expression ex = this.states.get(i);
			Expression left = (Expression) ex.getChildAt(0);
			if (left.getUserObject().toString().equals(vName))
				return i;
		}
		return -1;
	}
	
	private int getIndexOfOperationWithLeft(Expression left) {
		for (int i = 0; i < this.states.size(); i++) {
			Expression ex = this.states.get(i);
			Expression thisLeft = (Expression) ex.getChildAt(0);
			if (thisLeft.equals(left))
				return i;
		}
		return -1;
	}
	
	public Expression findExistingExpression(Expression leftToMatch) {
		Expression result = null;
		for (Expression ex : this.states) {
			if (!ex.getUserObject().toString().equals("="))
				continue;
			Expression left = (Expression) ex.getChildAt(0);
			if (left.equals(leftToMatch)) {
				result = ((Expression) ex.getChildAt(1)).clone();
				break;
			}
		}
		return result;
	}
	
	private boolean ExpressionContains(Expression ex, String s) {
		if (ex.getUserObject().toString().equals(s))
			return true;
		else {
			for (int i = 0; i < ex.getChildCount(); i++) {
				Expression child = (Expression) ex.getChildAt(i);
				if (ExpressionContains(child, s))
					return true;
			}
			return false;
		}
	}
	
	public SymbolicStates clone() {
		ArrayList<Expression> result = new ArrayList<Expression>();
		for (Expression ex : this.states)
			result.add(ex.clone());
		return new SymbolicStates(result);
	}
	
	public SymbolicStates getTrimmedSymbolicStates_Fstatic() {
		ArrayList<Expression> result = new ArrayList<Expression>();
		for (Expression ex : this.states) {
			Expression left = (Expression) ex.getChildAt(0);
			if (left.getUserObject().toString().equals("$Fstatic"))
				result.add(ex.clone());
		}
		return new SymbolicStates(result);
	}

}

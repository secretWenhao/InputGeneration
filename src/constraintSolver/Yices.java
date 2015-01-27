package constraintSolver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import main.Paths;
import sequence.YicesVariable;
import tools.ProcessInputListener;
import analysis.Expression;

public class Yices {

	private String yicesPath;
	private Process pc;
	private BufferedReader err;
	private OutputStream out;
	private List<YicesVariable> definedVariables = new ArrayList<YicesVariable>()	;
	
	
	public static void main(String[] args) {
		Yices y = new Yices(Paths.yicesPath);
		y.init();
		Expression ex = new Expression("=");
		ex.add(new Expression("s1"));
		ex.add(new Expression("1"));
		System.out.println(ex.toYicesStatement());
		Expression ex1 = new Expression("=");
		ex1.add(new Expression("s1"));
		ex1.add(new Expression("1"));
		
		Expression ex2 = new Expression("=");
		ex2.add(new Expression("s1"));
		ex2.add(new Expression("1"));

		List<Expression> exList = new ArrayList<Expression>(Arrays.asList(ex, ex1));
		if (y.checkSat(exList, ex2))
			System.out.println("aa");

	}
	
	public Yices(String path) {
		this.yicesPath = path;
	}
	
	
	public boolean checkSat(List<Expression> states, Expression cond) {
		init();
		for (Expression ex : states) {
			defineVariables(ex);
			input("(assert " + ex.toYicesStatement() + ")");
		}
		defineVariables(cond);
		input("(assert " + cond.toYicesStatement() + ")");
		String result = readError();
		exit();
		return result.equals("all good");
	}
	
	
	private void defineVariables(Expression ex) {
		if (ex.toString().startsWith("s")) {
			boolean found = false;
			for (YicesVariable yv : definedVariables) {
				if (yv.name.equals(ex.toString())) {
					found = true;
					break;
				}
			}
			if (!found) {
				YicesVariable yv = new YicesVariable();
				yv.name = ex.toString();
				yv.type = "int";
				definedVariables.add(yv);
				input("(define " + yv.name + "::" + yv.type + ")");
			}
		}
		else {
			for (int i = 0; i < ex.getChildCount(); i++) {
				Expression child = (Expression) ex.getChildAt(i);
				defineVariables(child);
			}
		}
	}
	
	private void init() {
		try {
			pc = Runtime.getRuntime().exec(yicesPath);
			err = new BufferedReader(new InputStreamReader(pc.getErrorStream()));
			out = pc.getOutputStream();
			definedVariables = new ArrayList<YicesVariable>();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void input(String s) {
		try {
			System.out.println("[input]" + s);
			out.write((s + "\n").getBytes());
			out.flush();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	private void exit() {
		input("(exit)");
	}
	
	private String readError() {
		String result = "";
		ProcessInputListener listener = new ProcessInputListener(err);
		ExecutorService executor = Executors.newFixedThreadPool(1);
		Future<String> newestLine = executor.submit(listener);
		try {
			result = newestLine.get(300, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			result = "all good";
		}
		executor.shutdown();
		return result;
	}
	
}

package main;

import gui.GUIModel;
import gui.ManualGUIModel;

import java.util.List;
import java.util.Map;

import sequence.Generator;
import sequence.YicesVariable;
import staticFamily.StaticApp;
import summary.PathSummary;
import summary.SymbolicStates;
import tools.Adb;
import analysis.Expression;
import analysis.StaticInfo;
import concolic.ExecutionEngine;

public class Main {

	static StaticApp testApp;
	static Adb adb = new Adb();
	
	public static void main(String[] args) {
		
		String[] apps = {
				"/home/wenhaoc/AppStorage/AndroidTest.apk"
		};
		
		String[] targetLines = {
				"com.Activity3$3:68",
				"com.Activity3:114",
		};
		
		String apkPath = apps[0];
		String targetLine = targetLines[0];
		

		testApp = StaticInfo.initAnalysis(apkPath, true);

		GUIModel guimodel = ManualGUIModel.OneGUIModelPls();
		
		ExecutionEngine ee = new ExecutionEngine(testApp, guimodel);
		List<PathSummary> psList = ee.buildPathSummaries(true);
		SymbolicStates initialStates = ee.getInitialStates().getTrimmedSymbolicStates_Fstatic();
		Generator g = new Generator(psList, guimodel, initialStates);
		g.findSequences(targetLine);
	}
	
	private static void testPSList(List<PathSummary> psList) {
		for (PathSummary ps : psList) {
			System.out.print("[Method]" + ps.getMethodSignature());
			if (ps.isConcrete())
				System.out.println(" (concrete)");
			else System.out.println("");
			System.out.println("[Sequence]:\n  " + ps.getEventSequence().toString());
			System.out.println("[exec log]" + ps.getExecutionLog().size());
			System.out.println("[s states]" + ps.getSymbolicStates().size());
			System.out.println("[pathcond]" + ps.getPathCondition().size());
		}
	}

	
	private static void printOutPathSummary(PathSummary pS) {
		System.out.println("\n Execution Log: ");
		for (String s : pS.getExecutionLog())
			System.out.println("  " + s);
		System.out.println("\n Symbolic States: ");
		for (Expression o : pS.getSymbolicStates())
			System.out.println("  " + o.toYicesStatement());
		System.out.println("\n PathCondition: ");
		for (Expression cond : pS.getPathCondition())
			System.out.println("  " + cond.toYicesStatement());
		System.out.println("\n PathChoices: ");
		for (String pC : pS.getPathChoices())
			System.out.println("  " + pC);
		System.out.println("========================");
	}
	

}

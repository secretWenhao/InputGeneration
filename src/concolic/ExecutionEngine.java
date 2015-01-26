package concolic;

import gui.Event;
import gui.GUIModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticMethod;
import summary.PathSummary;
import summary.SymbolicStates;
import analysis.Expression;

public class ExecutionEngine {

	private StaticApp testApp;
	private GUIModel guimodel;
	public boolean blackListOn = true;
	public boolean debug = true;
	
	public ExecutionEngine(StaticApp testApp, GUIModel guimodel) {
		this.testApp = testApp;
		this.guimodel = guimodel;
	}
	
	public ArrayList<PathSummary> buildPathSummaries(boolean forceAllStep) {
		
		ArrayList<PathSummary> result = new ArrayList<PathSummary>();
		
		File objFile = new File(testApp.outPath + "/path.summaries");

		if (forceAllStep || !objFile.exists()) {
			
			System.out.println("\nStarting Concolic Execution Engine...");

			Execution ex = new Execution(testApp, guimodel);
			ex.blackListOn = this.blackListOn;
			ex.debug = this.debug;
			for(Event e : guimodel.getAllEvents()) {
				
				String methodSig = e.handlerMethodSig;

				List<Event> eventSeq = guimodel.getCompleteSequence(e);
				
				ex.init();
				ex.setTargetMethod(methodSig);
				ex.setSequence(eventSeq);
				
				ArrayList<PathSummary> psList = ex.doConcolic();
				result.addAll(psList);
				
			}
			
			savePSList(result);
		}
		else {
			result = loadPSList(guimodel);
		}
		
		return result;
	}
	
	public SymbolicStates getInitialStates() {
		Blacklist blacklist = new Blacklist();
		Execution ex = new Execution(testApp);
		SymbolicStates result = new SymbolicStates(new ArrayList<Expression>());
		for (StaticClass c : testApp.getClasses()) {
			for (StaticMethod m : c.getMethods()) {
				if (blacklist.classInBlackList(c.getDexName()) || blacklist.methodInBlackList(m.getSmaliSignature()))
					continue;
				if (m.isConstructor() && m.getName().equals("<clinit>")) {
					ex.init();
					ex.setTargetMethod(m.getSmaliSignature());
					ArrayList<PathSummary> pss = ex.doFullSymbolic(false);
					for (PathSummary ps : pss)
						for (Expression newEx : ps.getSymbolicStates()) {
							result.updateSymbolicStates(newEx.clone());
						}
				}
			}
		}
		return result;
	}

	////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////

	private void savePSList(ArrayList<PathSummary> psList) {
		File objFile = new File(testApp.outPath + "/path.summaries");
		if (objFile.exists())
			objFile.delete();
		System.out.print("\nSaving Path Summaries into file... ");
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(objFile));
			out.writeObject(psList);
			out.close();
			System.out.print("Done.\n");
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<PathSummary> loadPSList(GUIModel guimodel) {
		ArrayList<PathSummary> result = new ArrayList<PathSummary>();
		File objFile = new File(testApp.outPath + "/path.summaries");
		System.out.print("\nLoading Path Summaries... ");
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(objFile));
			result = (ArrayList<PathSummary>) in.readObject();
			in.close();
			System.out.print("Done.\n");
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			if (e.getMessage().contains("local class incompatible")) {
				result = buildPathSummaries(true);
			}
			else
				e.printStackTrace();
		}
		return result;
	}
	
	
}

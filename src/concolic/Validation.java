package concolic;

import gui.Event;
import gui.GUIModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import staticFamily.StaticApp;
import tools.Adb;
import tools.Jdb;


public class Validation {

	private Jdb jdb = new Jdb();
	private Adb adb = new Adb();
	private StaticApp staticApp;
	private GUIModel guimodel;
	
	private List<Event> seq = new ArrayList<Event>();
	
	public boolean useAdb = true;
	public boolean verbose = true;
	
	private ArrayList<String> targetLines = new ArrayList<String>();
	private ArrayList<String> jdbResult = new ArrayList<String>();

	
	public Validation(StaticApp staticApp, GUIModel guimodel) {
		this.staticApp = staticApp;
		this.guimodel = guimodel;
	}
	
	public Validation(StaticApp staticApp) {
		this.staticApp = staticApp;
	}
	
	public boolean validateSequence(List<Event> seq, ArrayList<String> targetLines) {
		this.seq = seq;
		this.targetLines = targetLines;
		System.out.println("\nValidating Event Sequence:\n  " + seq + "\nFor Target Lines:\n  " + targetLines);
		
		try {
			
			preparation();
			
			applyFinalEvent();
			
			jdbResult = collectBPResult();
			
		}
		catch (Exception e) {e.printStackTrace();}
		
		boolean result =  compare(jdbResult, targetLines);
		
		if (this.verbose) {
			if (result)	System.out.println("\nValidation Result: Success. the event sequence triggered all target lines, in the same order as well.");
			else		System.out.println("\nValidation Result: Failed. jdb result and target line sequence mismatch.");
		}
		return result;
	}
	
	public boolean validateSequence(List<Event> seq, String targetLine) {
		return validateSequence(seq, new ArrayList<String>(Arrays.asList(targetLine)));
	}
	
	private boolean compare(ArrayList<String> A, ArrayList<String> B) {
		if (A.size() < 1 || B.size() < 1 || A.size() != B.size())
			return false;
		for (int i = 1, len = A.size(); i < len; i++)
			if (!A.get(i).equals(B.get(i)))
				return false;
		return true;
	}

	//////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////
	
	
	private ArrayList<String> collectBPResult() throws Exception{
		System.out.println("\nCollecting Breakpoint Hits...");
		ArrayList<String> result = new ArrayList<String>();
		String jdbLine = "";
		while (!jdbLine.equals("TIMEOUT")) {
			if (jdbLine.startsWith("Breakpoint hit: ")) {
				if (this.verbose)
					System.out.println("  [J]" + jdbLine);
				if (!jdbLine.startsWith("Breakpoint hit: Set breakpoint")){
					String lineInfo = parseJDBLine(jdbLine);
					result.add(lineInfo);
				}
				jdb.cont();
			}
			jdbLine = jdb.readLine();
			if (jdbLine == null)
				throw (new Exception("Jdb might have crashed."));
		}
		return result;
	}

	private String parseJDBLine(String jdbLine) {
		String bpInfo = jdbLine.substring(jdbLine.indexOf("Breakpoint hit: "));
		String methodInfo = bpInfo.split(", ")[1];
		String cN = methodInfo.substring(0, methodInfo.lastIndexOf("."));
		String lineInfo = bpInfo.split(", ")[2];
		String lineNo = lineInfo.substring(lineInfo.indexOf("=")+1, lineInfo.indexOf(" "));
		return cN + ":" + lineNo;
	}
	
	private void preparation() throws Exception{
		
		System.out.print("\nReinstalling and Restarting App...  ");
		adb.uninstallApp(staticApp.getPackageName());
		adb.installApp(staticApp.getSmaliAppPath());
		System.out.println("Done.");
		adb.unlockScreen();
		adb.pressHomeButton();
		adb.startApp(staticApp.getPackageName(), staticApp.getMainActivity().getJavaName());
		
		System.out.print("\nInitiating jdb...  ");
		jdb.init(staticApp.getPackageName());
		System.out.println("Done.");
		
		System.out.println("\nGoing to Target Layout...");
		
		for (int i = 0, len = seq.size()-1; i < len; i++) {
			Event e = seq.get(i);
			adb.applyEvent(e);
		}
				
		System.out.print("\nSetting Break Points for Target Lines... ");
		for (String s : this.targetLines) {
			String className = s.split(":")[0];
			int line = Integer.parseInt(s.split(":")[1]);
			jdb.setBreakPointAtLine(className, line);
		}
		System.out.println("Done.");
	}
	
	private void applyFinalEvent() {

		Event lastEvent = seq.get(seq.size()-1);
		adb.applyEvent(lastEvent);
			
	}

	public ArrayList<String> getJdbResult() {
		return jdbResult;
	}

	
}

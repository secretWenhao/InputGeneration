package concolic;

import gui.Event;
import gui.EventType;
import gui.GUIModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import smali.stmt.GotoStmt;
import smali.stmt.IfStmt;
import smali.stmt.InvokeStmt;
import smali.stmt.ReturnStmt;
import smali.stmt.SwitchStmt;
import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticMethod;
import staticFamily.StaticStmt;
import summary.PathSummary;
import tools.Adb;
import tools.Jdb;
import analysis.Expression;

public class Execution {

	private StaticApp staticApp;
	private GUIModel guimodel;
	private StaticMethod entryMethod;
	private List<Event> seq = new ArrayList<Event>();
	private Adb adb;
	private Jdb jdb;
	private ArrayList<PathSummary> pathSummaries = new ArrayList<PathSummary>();
	private ArrayList<ToDoPath> toDoPathList = new ArrayList<ToDoPath>();
	public boolean printOutPS = false;
	public boolean blackListOn = false;
	public boolean debug = false;
	public boolean useAdb = true;
	
	public Execution(StaticApp staticApp) {
		this.staticApp = staticApp;
	}
	
	public Execution(StaticApp staticApp, GUIModel guimodel) {
		this.staticApp = staticApp;
		this.guimodel = guimodel;
		this.adb = new Adb();
		this.jdb = new Jdb();
	}
	
	public void init() {
		this.adb = new Adb();
		this.jdb = new Jdb();
		this.pathSummaries = new ArrayList<PathSummary>();
		this.toDoPathList = new ArrayList<ToDoPath>();
	}
	
	public void setTargetMethod(String methodSig) {
		this.entryMethod = staticApp.findMethod(methodSig);
		if (entryMethod == null)
			System.out.println("eventhandler m null");
	}
	
	public void setSequence(List<Event> seq) {
		this.seq = seq;
	}
	
	public ArrayList<PathSummary> doFullSymbolic(boolean addSequenceToInitialPS){
		
		if (this.blackListOn && blacklistCheck(this.entryMethod)) {
			System.out.println("Skipping blacklisted method " + this.entryMethod.getSmaliSignature());
			return new ArrayList<PathSummary>();
		}
		
		if (this.entryMethod.getSmaliStmts().size() < 1) {
			System.out.println("Empty method " + this.entryMethod.getSmaliSignature());
			return new ArrayList<PathSummary>();
		}
		
		System.out.println("generating symbolic PathSummary for " + this.entryMethod.getSmaliSignature() + " ...");
		try {
			ToDoPath toDoPath = new ToDoPath();
			PathSummary initPS = new PathSummary();
			if (addSequenceToInitialPS)
				initPS.setEventSequence(seq);
			initPS.setSymbolicStates(initSymbolicStates(entryMethod));
			initPS.setMethodSignature(this.entryMethod.getSmaliSignature());
			PathSummary newPS = symbolicExecution(initPS, entryMethod, toDoPath, true);
			pathSummaries.add(newPS);
			symbolicallyFinishingUp(false);
		}	catch (Exception e) {e.printStackTrace();}
		
		return this.pathSummaries;
		
	}
	
	public ArrayList<PathSummary> doConcolic() {

		if (this.blackListOn && blacklistCheck(this.entryMethod)) {
			System.out.println("Skipping blacklisted method " + this.entryMethod.getSmaliSignature());
			return new ArrayList<PathSummary>();
		}
		
		if (this.entryMethod.getSmaliStmts().size() < 1) {
			System.out.println("Empty method " + this.entryMethod.getSmaliSignature());
			return this.pathSummaries;
		}
		
		System.out.println("\n[Method]\t\t" + this.entryMethod.getSmaliSignature());
		System.out.println("[EventSequence]\t" + this.seq);
		try {
			
			if (seqConsistsOfLaunchOnly())
				return doFullSymbolic(true);
			
			preparation();

			applyFinalEvent();

			PathSummary pS_0 = new PathSummary();
			pS_0.setSymbolicStates(initSymbolicStates(entryMethod));
			pS_0.setMethodSignature(entryMethod.getSmaliSignature());
			pS_0.setEventSequence(seq);
			pS_0.setIsConcrete(true);
			pS_0 = concreteExecution(pS_0, entryMethod, true);
			
			pathSummaries.add(pS_0);
			
			symbolicallyFinishingUp(true);
			
			jdb.exit();
			
			System.out.println("\nGenerated " + pathSummaries.size() + " Path Summaries for " + this.entryMethod.getSmaliSignature());

		}	catch (Exception e) {e.printStackTrace();}
		
		return this.pathSummaries;
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
		
		for (int i : entryMethod.getSourceLineNumbers()) {
			jdb.setBreakPointAtLine(entryMethod.getDeclaringClass(staticApp).getJavaName(), i);
		}
	}
	
	private PathSummary concreteExecution(PathSummary pS, StaticMethod m, boolean inMainMethod) throws Exception {

		boolean newPathCondition = false; StaticStmt lastPathStmt = new StaticStmt();
		
		String jdbNewLine = "";
		while (!jdbNewLine.equals("TIMEOUT")) {
			//Processing A Breakpoint Hit
			if (jdbNewLine.contains("Breakpoint hit: ")) {
				// 1. Recognize the newly hit StaticStmt, and check for errors
				String trimming = jdb.readLine();
				while (!trimming.equals("TIMEOUT"))
					trimming = jdb.readLine();
				String bpInfo = jdbNewLine.substring(jdbNewLine.indexOf("Breakpoint hit: "));
				System.out.println("  [J]" + jdbNewLine);
				if (bpInfo.contains("Set breakpoint ")) {
					jdbNewLine = jdb.readLine();
					if (jdbNewLine == null)
						throw (new Exception("Jdb is having performance problems, please reboot the device."));
					continue;
				}
				String methodInfo = bpInfo.split(", ")[1];
				String cN = methodInfo.substring(0, methodInfo.lastIndexOf("."));
				String mN = methodInfo.substring(methodInfo.lastIndexOf(".")+1).replace("(", "").replace(")", "");
				String lineInfo = bpInfo.split(", ")[2].replace(",", "");
				int newHitLine = Integer.parseInt(lineInfo.substring(lineInfo.indexOf("=")+1, lineInfo.indexOf(" ")));
				StaticClass c = staticApp.findClassByJavaName(cN);
				if (c == null)
					throw (new Exception("Can't find StaticClass object of class " + cN + ". In " + bpInfo));
				if (!m.getName().equals(mN)) {
					throw (new Exception("Mismatch between current StaticMethod and new Breakpoint method. In " + bpInfo
							 + "\ncurrent StaticMethod name: " + m.getName()));
				}
				StaticStmt s = m.getStmtByLineNumber(newHitLine);
				if (s == null)
					throw (new Exception("Can't find StaticStmt object of " + cN + ":" + newHitLine));
				// 2. Process each StaticStmt
				// 2-1. Last StaticStmt is IfStmt or SwitchStmt, need to update PathCondition
				if (newPathCondition) {
					String lastPathStmtInfo = cN + ":" + lastPathStmt.getSourceLineNumber();
					if (lastPathStmt instanceof IfStmt) {
						IfStmt ifS = (IfStmt) lastPathStmt;
						int remainingLine = -1;
						Expression cond = ifS.getJumpCondition();
						if (newHitLine == ifS.getJumpTargetLineNumber(m))
							remainingLine = ifS.getFlowThroughTargetLineNumber(m);
						else if (newHitLine == ifS.getFlowThroughTargetLineNumber(m)) {
							remainingLine = ifS.getJumpTargetLineNumber(m);
							cond = this.getReverseCondition(cond);
						}
						else throw (new Exception("Unexpected Line Number Following IfStmt" + bpInfo));
						boolean shouldAdd = true;
						for (String pC : pS.getPathChoices()) {
							if (pC.equals(lastPathStmtInfo + "," + remainingLine)) {
								shouldAdd = false;
								break;
							}
						}
						if (shouldAdd)
							pushNewToDoPath(pS.getPathChoices(), lastPathStmtInfo, "" + remainingLine);
						pS.addPathChoice(lastPathStmtInfo + "," + newHitLine);
						pS.updatePathCondition(cond);
					}
					else if (lastPathStmt instanceof SwitchStmt) {
						SwitchStmt swS = (SwitchStmt) lastPathStmt;
						ArrayList<String> remainingCases = new ArrayList<String>();
						int switchVValue = Integer.parseInt(getConcreteValue(swS.getSwitchV()));
						for (int anyCase : swS.getSwitchMap(m).keySet())
							if (anyCase != newHitLine)
								remainingCases.add(anyCase+"");
						if (newHitLine == swS.getFlowThroughLineNumber(m)) {
							for (Expression cond : swS.getFlowThroughConditions())
								pS.updatePathCondition(cond);
						}
						else {
							pS.updatePathCondition(swS.getSwitchCondition(switchVValue));
							remainingCases.add("FlowThrough");
						}
						for (String remainingCase : remainingCases)
							pushNewToDoPath(pS.getPathChoices(), lastPathStmtInfo, remainingCase);
						pS.addPathChoice(lastPathStmtInfo + "," + switchVValue);
					}
					newPathCondition = false;
					lastPathStmt = new StaticStmt();
				}
				pS.addExecutionLog(cN + ":" + newHitLine);
				// 2-2. Current StaticStmt is Return or Throw
				if (s.endsMethod()) {
					if (s instanceof ReturnStmt && !((ReturnStmt) s).returnsVoid())
						pS.updateReturnSymbol(s.getvA());
					break;
				}
				// 2-3. Current StaticStmt generates New Symbol
				else if (s.generatesSymbol()) {
					pS.updateSymbolicStates(s.getExpression());
				}
				// 2-4. Current StaticStmt contains Operation
				else if (s.hasExpression()) {
					pS.updateSymbolicStates(s.getExpression());
				}
				// 2-5. Current StaticStmt is IfStmt or SwitchStmt, prepare for PathCondition update at next hit
				else if (s.updatesPathCondition()) {
					lastPathStmt = s;
					newPathCondition = true;
				}
				// 2-6. Current StaticStmt is InvokeStmt
				else if (s instanceof InvokeStmt) {
					InvokeStmt iS = (InvokeStmt) s;
					StaticMethod targetM = staticApp.findMethod(iS.getTargetSig());
					StaticClass targetC = staticApp.findClassByDexName(iS.getTargetSig().split("->")[0]);
					if (targetC != null && targetM != null && !(this.blackListOn && blacklistCheck(targetM))) {
						for (int i : targetM.getSourceLineNumbers())
							jdb.setBreakPointAtLine(targetC.getJavaName(), i);
						jdb.cont();
						PathSummary trimmedPS = trimPSForInvoke(pS, iS.getParams());
						PathSummary subPS = concreteExecution(trimmedPS, targetM, false);
						pS.mergeWithInvokedPS(subPS, targetM.isStatic());
					}
					else if (iS.resultsMoved()) {
						Expression symbolOFromJavaAPI = generateJavaAPIReturnOperation(iS, pS);
						pS.addSymbolicState(symbolOFromJavaAPI);
					}
				}
				// 3. Finished Processing StaticStmt, let jdb continue
				jdb.cont();
			}
			// Finished Processing new JDB Line, Read Next Line
			jdbNewLine = jdb.readLine();
			if (jdbNewLine == null)
				throw (new Exception("Jdb might have crashed."));
		}
		if (inMainMethod) {
			System.out.println("");
			if (printOutPS)
				printOutPathSummary(pS);
		}
		return pS;
	}
	

	private void symbolicallyFinishingUp(boolean isConcolic) throws Exception{
		int counter = 1;
		while (toDoPathList.size()>0) {
			System.out.println("[Symbolic Execution No." + counter++ + "]\t" + this.entryMethod.getSmaliSignature());
			ToDoPath toDoPath = toDoPathList.get(toDoPathList.size()-1);
			toDoPathList.remove(toDoPathList.size()-1);
			PathSummary initPS = new PathSummary();
			initPS.setSymbolicStates(initSymbolicStates(entryMethod));
			initPS.setMethodSignature(entryMethod.getSmaliSignature());
			PathSummary newPS = symbolicExecution(initPS, entryMethod, toDoPath, true);
			if (isConcolic)
				newPS.setEventSequence(new ArrayList<Event>(Arrays.asList(seq.get(seq.size()-1))));
			pathSummaries.add(newPS);
		}
	}


	private PathSummary symbolicExecution(PathSummary pS, StaticMethod m, ToDoPath toDoPath, boolean inMainMethod) throws Exception{
		
		ArrayList<StaticStmt> allStmts = m.getSmaliStmts();
		String className = m.getDeclaringClass(staticApp).getJavaName();
		StaticStmt s = allStmts.get(0);
		while (true) {
			if (this.debug)
				System.out.println("[Current Stmt]" + className + ":" + s.getSourceLineNumber() + "   " + s.getTheStmt());
			pS.addExecutionLog(className + ":" + s.getSourceLineNumber());
			if (s.endsMethod()) {
				if (s instanceof ReturnStmt && !((ReturnStmt) s).returnsVoid())
					pS.updateReturnSymbol(s.getvA());
				break;
			}
			else if (s.generatesSymbol()) {
				pS.updateSymbolicStates(s.getExpression());
			}
			else if (s.hasExpression()) {
				pS.updateSymbolicStates(s.getExpression());
			}
			else if (s.updatesPathCondition()) {
				String stmtInfo = className + ":" + s.getSourceLineNumber();
				String pastChoice = toDoPath.getAPastChoice();
				String choice = "";
				ArrayList<Expression> pathConditions = new ArrayList<Expression>();
				ArrayList<String> remainingDirections = new ArrayList<String>();
				if (!pastChoice.equals("")) {
					if (!pastChoice.startsWith(stmtInfo + ",")) {
						System.out.println("Expecting: " + stmtInfo + ",");
						System.out.println("Got this:  " + pastChoice);
						throw (new Exception("current PathStmt not synced with toDoPath.pastChoice. " + stmtInfo));
					}
					// haven't arrived target path stmt yet. So follow past choice, do not make new ToDoPath
					choice = pastChoice;
				}
				else if (toDoPath.getTargetPathStmtInfo().equals(stmtInfo)){
					// this is the target path stmt
					choice = stmtInfo + "," + toDoPath.getNewDirection();
					toDoPath.setTargetPathStmtInfo("");
				}
				else {
					// already passed target path stmt

					/*TODO plan for loops:
					 * 1. When making own decision, check pathChoices, if there are choices of that IfStmt, find the most recent one and go the opposite direction
					 * 2. When building ToDoPath for the newDirection, if the newDirection already happened before this, then do not build ToDoPath
					 * */
					choice = makeAPathChoice(s, stmtInfo, m, pS);
					remainingDirections = getRemainingDirections(s, choice, stmtInfo, m, pS);
					for (String remainingDirection : remainingDirections) {
						pushNewToDoPath(pS.getPathChoices(), stmtInfo, remainingDirection);
					}
				}
				pS.addPathChoice(choice);
				pathConditions = retrievePathConditions(s, choice, m);
				for (Expression cond : pathConditions)
					pS.updatePathCondition(cond);
				s = m.getStmtByLineNumber(readNextLineNumber(s, choice, m));
				continue;
			}
			else if (s instanceof InvokeStmt) {
				InvokeStmt iS = (InvokeStmt) s;
				StaticMethod targetM = staticApp.findMethod(iS.getTargetSig());
				StaticClass targetC = staticApp.findClassByDexName(iS.getTargetSig().split("->")[0]);
				if (targetC != null && targetM != null &&
						targetM.getDeclaringClass(staticApp) != null &&
						targetM.getSmaliStmts().size() > 0 &&
						!(this.blackListOn && blacklistCheck(targetM))) {
					PathSummary trimmedPS = trimPSForInvoke(pS, iS.getParams());
					PathSummary subPS = symbolicExecution(trimmedPS, targetM, toDoPath, false);
					pS.mergeWithInvokedPS(subPS, targetM.isStatic());
				}
				else if (iS.resultsMoved()) {
					Expression symbolOFromJavaAPI = generateJavaAPIReturnOperation(iS, pS);
					if (symbolOFromJavaAPI != null)
						pS.addSymbolicState(symbolOFromJavaAPI);
				}
			}
			else if (s instanceof GotoStmt) {
				GotoStmt gS = (GotoStmt) s;
				s = m.getFirstStmtOfBlock(gS.getTargetLabel());
				continue;
			}
			int nextStmtID = s.getStmtID()+1;
			s = allStmts.get(nextStmtID);
		}
		if (inMainMethod && printOutPS) {
			printOutPathSummary(pS);
		}
		return pS;
	}



///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//															Utility Methods
//
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	private boolean blacklistCheck(StaticMethod m) {
		Blacklist bl = new Blacklist();
		StaticClass c = m.getDeclaringClass(staticApp);
		if (m == null || c == null)
			return false;
		return (bl.classInBlackList(c.getDexName()) || bl.methodInBlackList(m.getSmaliSignature()));
	}
	
	private void applyFinalEvent() {

		Event lastEvent = seq.get(seq.size()-1);
		adb.applyEvent(lastEvent);
			
	}

	private boolean seqConsistsOfLaunchOnly() {
		for (Event e : seq)
			if (e.type != EventType.LAUNCH)
				return false;
		return true;
	}

	private ArrayList<String> getRemainingDirections(StaticStmt theS, String choice, String stmtInfo, StaticMethod m, PathSummary pS) {
		ArrayList<String> remainingDirections = new ArrayList<String>();
		if (theS instanceof IfStmt) {
			IfStmt s = (IfStmt) theS;
			int chosenLine = Integer.parseInt(choice.split(",")[1]);
			int newDirection = -1;
			if (chosenLine == s.getFlowThroughTargetLineNumber(m))
				newDirection = s.getJumpTargetLineNumber(m);
			else newDirection = s.getFlowThroughTargetLineNumber(m);
			ArrayList<String> pathChoices = pS.getPathChoices();
			boolean shouldAdd = true;
			for (String pC : pathChoices) {
				if (pC.equals(stmtInfo + "," + newDirection)) {
					shouldAdd = false;
					break;
				}
			}
			if (shouldAdd)
				remainingDirections.add(newDirection + "");
		}
		else if (theS instanceof SwitchStmt) {
			SwitchStmt s = (SwitchStmt) theS;
			String chosenValue = choice.split(",")[1];
			if (chosenValue.equals("FlowThrough"))
				for (int i : s.getSwitchMap(m).keySet())
					remainingDirections.add(i + "");
			else {
				for (int i : s.getSwitchMap(m).keySet())
					if (i != Integer.parseInt(chosenValue))
						remainingDirections.add(i + "");
				remainingDirections.add("FlowThrough");
			}
			Collections.reverse(remainingDirections);
		}
		return remainingDirections;
	}

	private ArrayList<Expression> retrievePathConditions(StaticStmt s, String choice, StaticMethod m) {
		ArrayList<Expression> result = new ArrayList<Expression>();
		if (s instanceof IfStmt) {
			IfStmt ifS = (IfStmt) s;
			int chosenLine = Integer.parseInt(choice.split(",")[1]);
			Expression cond = ifS.getJumpCondition();
			if (chosenLine != ifS.getJumpTargetLineNumber(m))
				cond = getReverseCondition(cond);
			result.add(cond);
		}
		else if (s instanceof SwitchStmt) {
			SwitchStmt sws = (SwitchStmt) s;
			String chosenValue = choice.split(",")[1];
			if (chosenValue.equals("FlowThrough"))
				for (Expression cond : sws.getFlowThroughConditions())
					result.add(cond);
			else if (sws.getSwitchMap(m).containsKey(Integer.parseInt(chosenValue)))
				result.add(sws.getSwitchCondition(Integer.parseInt(chosenValue)));
			else
				for (Expression cond : sws.getFlowThroughConditions())
					result.add(cond);
		}
		return result;
	}
	
	private int readNextLineNumber(StaticStmt s, String choice, StaticMethod m) {
		int nextLineNumber = -1;
		if (s instanceof IfStmt)
			nextLineNumber = Integer.parseInt(choice.split(",")[1]);
		else if (s instanceof SwitchStmt) {
			SwitchStmt swS = (SwitchStmt) s;
			String valueChoice = choice.split(",")[1];
			if (valueChoice.equals("FlowThrough"))
				nextLineNumber = swS.getFlowThroughLineNumber(m);
			else if (swS.getSwitchMap(m).containsKey(Integer.parseInt(valueChoice)))
				nextLineNumber = swS.getSwitchMap(m).get(Integer.parseInt(valueChoice));
			else 
				nextLineNumber = swS.getFlowThroughLineNumber(m);
		}
		return nextLineNumber;
	}
	
	private String makeAPathChoice(StaticStmt theS, String stmtInfo, StaticMethod m, PathSummary pS) {
		String choice = "";
		if (theS instanceof IfStmt) {
			IfStmt s = (IfStmt) theS;
			int jumpLine = s.getJumpTargetLineNumber(m);
			int flowthrLine = s.getFlowThroughTargetLineNumber(m);
			int jumpTo = jumpLine;
			ArrayList<String> pathChoices = pS.getPathChoices();
			for (String pC : pathChoices) {
				if (pC.equals(stmtInfo + "," + jumpLine)) {
					jumpTo = flowthrLine;
				}
				else if (pC.equals(stmtInfo + "," + flowthrLine)) {
					jumpTo = jumpLine;
				}
			}
			choice = stmtInfo + "," + jumpTo;
		}
		else if (theS instanceof SwitchStmt) {
			choice = stmtInfo + ",FlowThrough";
		}
		return choice;
	}

	private Expression generateJavaAPIReturnOperation(InvokeStmt iS, PathSummary pS) {
		
		Expression left = new Expression("$return");
		Expression right = new Expression("$api");
		right.add(new Expression(iS.getTargetSig()));
		String rawParams = iS.getParams();
		ArrayList<String> oldParams = new ArrayList<String>();
		if (!rawParams.contains(", "))	oldParams.add(rawParams);
		else	oldParams = new ArrayList<String>(Arrays.asList(rawParams.split(", ")));
		for (String oldp : oldParams) {
			Expression newP = pS.findExistingExpression(new Expression(oldp));
			if (newP != null)
				right.add(newP);
		}
		Expression result = new Expression("=");
		result.add(left);
		result.add(right);
		return result;
	}
	
	private PathSummary trimPSForInvoke(PathSummary pS, String unparsedParams) {
		PathSummary trimmedPS = pS.clone();
		ArrayList<String> params = new ArrayList<String>();
		if (!unparsedParams.contains(", "))
			params.add(unparsedParams);
		else params = new ArrayList<String>(Arrays.asList(unparsedParams.split(", ")));
		int paramIndex = 0;
		ArrayList<Expression> trimmedSymbolicStates = new ArrayList<Expression>();
		for (Expression ex : pS.getSymbolicStates()) {
			Expression left = (Expression) ex.getChildAt(0);
			// 1. left.endwith $Fstatic
			// 2. left = parameter
			if (left.getUserObject().toString().equals("$Fstatic"))
				trimmedSymbolicStates.add(ex.clone());
			else if (params.contains(left.getUserObject().toString())) {
				Expression toAdd = ex.clone();
				toAdd.remove(0);
				toAdd.insert(new Expression("p" + paramIndex++), 0);
				params.remove(left.getUserObject().toString());
				trimmedSymbolicStates.add(toAdd);
			}
		}
		trimmedPS.setSymbolicStates(trimmedSymbolicStates);
		return trimmedPS;
	}

	private void pushNewToDoPath(ArrayList<String> pathChoices, String pathStmtInfo, String newDirection) {
		ToDoPath toDo = new ToDoPath();
		toDo.setPathChoices(pathChoices);
		toDo.setTargetPathStmtInfo(pathStmtInfo);
		toDo.setNewDirection(newDirection);
		this.toDoPathList.add(toDo);
	}
	
	private void printOutPathSummary(PathSummary pS) {
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
	
	void printOutToDoPath(ToDoPath toDoPath) {
		System.out.println("[PastChoice]");
		for (String s : toDoPath.getPathChoices())
			System.out.print("   " + s);
		System.out.println("\n[Turning Point]");
		System.out.println(" " + toDoPath.getTargetPathStmtInfo() + "," + toDoPath.getNewDirection() + "\n");
	}
	
	
	private ArrayList<Expression> initSymbolicStates(StaticMethod targetM) {
		ArrayList<Expression> symbolicStates = new ArrayList<Expression>();
		int paramCount = entryMethod.getParameterTypes().size();
		if (!entryMethod.isStatic())
			paramCount++;
		for (int i = 0; i < paramCount; i++) {
			Expression ex = new Expression("=");
			ex.add(new Expression("p" + i));
			if (!entryMethod.isStatic() && i == 0) {
				Expression thisEx = new Expression("$this");
				thisEx.add(new Expression(targetM.getDeclaringClass(this.staticApp).getDexName()));
				ex.add(thisEx);
			}
			else
				ex.add(new Expression("$parameter" + i));
			symbolicStates.add(ex);
		}
		return symbolicStates;
	}
	
	private String getConcreteValue(String vName) {
		ArrayList<String> jdbLocals = jdb.getLocals();
		for (String jL : jdbLocals) {
			String left = jL.split(" = ")[0];
			String right = jL.split(" = ")[1];
			if (left.equals("wenhao" + vName))
				return right;
		}
		return "";
	}
	
	private Expression getReverseCondition(Expression ex) {
		String op = ex.getUserObject().toString();
		if (op.equals("="))		op = "/=";
		else if (op.equals("/="))	op = "=";
		else if (op.equals("<"))	op = ">=";
		else if (op.equals("<="))	op = ">";
		else if (op.equals(">"))	op = "<=";
		else if (op.equals(">="))	op = "<";
		Expression result = new Expression(op);
		for (int i = 0; i < ex.getChildCount(); i++) {
			Expression child = (Expression) ex.getChildAt(i);
			result.add(child.clone());
		}
		return result;
	}

}

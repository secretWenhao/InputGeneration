package concolic;

import java.util.ArrayList;
import java.util.Arrays;

public class Blacklist {

	/*
	 * Some of the classes or methods(especially android.support.v4/v7) 
	 * is way too long and way too irrelevant to the behavior or the app.
	 * Therefore they will be skipped when:
	 *  1. before starting to generate PathSummary
	 *  2. symbolic execution encounters InvokeStmt that invokes them,
	 *  3. concrete execution setting break points.
	 * This is only a temporary solution, to increase the efficiency of 
	 * concolic execution. Maybe in the future there will be a way to deal
	 * with these huge methods efficiently.
	 * */
	public ArrayList<String> classes = new ArrayList<String>(Arrays.asList(
			"Landroid/support/v4/*",
			"Landroid/support/v7/*",
			"Landroid/support/annotation/*",
			"Lcom/example/androidtest/R*"
	));
	

	
	
	public ArrayList<String> methods = new ArrayList<String>(Arrays.asList(
			""
	));

	
	public boolean classInBlackList(String className) {
		for (String c : classes) {
			if (c.endsWith("*")) {
				if (className.startsWith(c.substring(0, c.lastIndexOf("*"))))
					return true;
			}
			else if (c.endsWith(";")) {
				if (className.equals(c))
					return true;
			}
		}
		return false;
	}
	
	public boolean methodInBlackList(String methodSig) {
		return methods.contains(methodSig);
	}

	
}

package summary;

import java.util.ArrayList;
import java.util.List;

public class EventHandlerSummary {
	
	private String methodSig = "";
	
	private List<PathSummary> psList = new ArrayList<PathSummary>();
	
	public String getMethodSig() {
		return methodSig;
	}
	
	public void setMethodSig(String methodSig) {
		this.methodSig = methodSig;
	}
	
	public List<PathSummary> getPSs() {
		return psList;
	}
	
	public void setPSs(List<PathSummary> psList) {
		this.psList = psList;
	}
	
	public void addPS(PathSummary ps) {
		this.psList.add(ps);
	}
	
	public PathSummary getConcretePS() {
		for (PathSummary ps : psList)
			if (ps.isConcrete())
				return ps;
		return null;
	}
	
}

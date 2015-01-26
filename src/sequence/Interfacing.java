package sequence;

import java.util.ArrayList;
import java.util.List;

import summary.EventHandlerSummary;
import summary.PathSummary;

public class Interfacing {


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
	
}

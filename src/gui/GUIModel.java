package gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
public class GUIModel implements Serializable{

	public List<GUIState> GUIStates = new ArrayList<GUIState>();
	
	public List<Event> getAllEvents() {
		List<Event> result = new ArrayList<Event>();
		
		for (GUIState gs : GUIStates) {
			result.addAll(gs.leavingEvents);
			result.addAll(gs.stayingEvents);
		}
		return result;
	}
	
	public List<Event> getCompleteSequence(Event e) {
		List<Event> result = new ArrayList<Event>();
		result.add(e);
		int tgtState = e.srcGUIStateID;
		List<Integer> visitedStates = new ArrayList<Integer>(Arrays.asList(tgtState));
		while (tgtState != 0) {
			for (GUIState gs : this.GUIStates) {
				if (visitedStates.contains(gs.id))
					continue;
				for (Event event : gs.leavingEvents) {
					if (event.tgtGUIStateID == tgtState) {
						result.add(0, event);
						tgtState = event.srcGUIStateID;
					}
				}
			}
		}
		return result;
	}
	
}

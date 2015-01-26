package sequence;

import java.util.ArrayList;
import java.util.List;
import gui.Event;

public class EventSequence {

	private List<Event> seq = new ArrayList<Event>();
	
	public EventSequence() {
		
	}
	
	public EventSequence(List<Event> seq) {
		this.seq = seq;
	}
	
	public void add(int index, Event e) {
		seq.add(index, e);
	}
	
	public List<Event> getList() {
		return seq;
	}
	
	public boolean startsFromEntry() {
		Event firstEvent = seq.get(0);
		if (firstEvent.srcGUIStateID == 0)
			return true;
		return false;
	}
	
	@Override
	public String toString() {
		String result = "[";
		for (Event e : seq)
			result += e.toString() + " ";
		result += "]";
		return result;
	}
	
}

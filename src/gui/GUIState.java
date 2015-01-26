package gui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GUIState implements Serializable{

	public List<Event> stayingEvents = new ArrayList<Event>();
	public List<Event> leavingEvents = new ArrayList<Event>();
	public int id;
	public String activityName;
	
}

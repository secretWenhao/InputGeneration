package gui;

import java.io.Serializable;

public class Event implements Serializable{

	public int type;
	public int tap_x, tap_y;
	public int keycode;
	public String handlerMethodSig;
	public boolean leavesGUIState;
	public int srcGUIStateID, tgtGUIStateID;
	
	public Event(int type, int x, int y, int srcGUIStateID) {
		this.type = type;
		this.tap_x = x;
		this.tap_y = y;
		this.srcGUIStateID = srcGUIStateID;
	}
	
	public Event(int type, int keycode, int srcGUIStateID) {
		this.type = type;
		this.keycode = keycode;
		this.srcGUIStateID = srcGUIStateID;
	}
	
	@Override
	public String toString() {
		String result = "[";
		switch (type) {
		case EventType.KEY:
			result += "keyevent, " + this.keycode;
			break;
		case EventType.TAP:
			result += "tap, " + this.tap_x + " " + this.tap_y;
			break;
		case EventType.LAUNCH:
			result += "launch";
		}
		
		result += "]";
		return result;
	}
	
}

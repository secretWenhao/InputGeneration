package gui;

import java.util.ArrayList;
import java.util.Arrays;

public class ManualGUIModel {

		
	public static GUIModel OneGUIModelPls() {
		
		GUIModel m = new GUIModel();
		
		m.GUIStates.add(activity1Model());
		m.GUIStates.add(activity2Model());
		m.GUIStates.add(activity3Model());
		
		return m;
	}
	
	public static GUIState activity1Model() {
		
		GUIState gui = new GUIState();
		gui.activityName = "com.MainActivity";
		gui.id = 0;
		
		Event e2 = new Event(EventType.TAP, 310, 920, gui.id);
		e2.handlerMethodSig = "Lcom/MainActivity$1;->onClick(Landroid/view/View;)V";
		e2.leavesGUIState = true;
		e2.tgtGUIStateID = 1;
		gui.leavingEvents = new ArrayList<Event>(Arrays.asList(e2));
		
		return gui;
	}
	
	public static GUIState activity2Model() {
		GUIState gui = new GUIState();
		gui.activityName = "com.Activity2";
		gui.id = 1;
		
		Event e1 = new Event(EventType.TAP, 310, 750, gui.id);
		e1.handlerMethodSig = "Lcom/Activity2$1;->onClick(Landroid/view/View;)V";
		Event e2 = new Event(EventType.TAP, 310, 960, gui.id);
		e2.handlerMethodSig = "Lcom/Activity2$2;->onClick(Landroid/view/View;)V";
		gui.stayingEvents = new ArrayList<Event>(Arrays.asList(e1, e2));
		
		Event e3 = new Event(EventType.TAP, 550, 1450, gui.id);
		e3.handlerMethodSig = "Lcom/Activity2$3;->onClick(Landroid/view/View;)V";
		e3.leavesGUIState = true;
		e3.tgtGUIStateID = 2;
		gui.leavingEvents = new ArrayList<Event>(Arrays.asList(e3));
		
		return gui;
	}
	
	public static GUIState activity3Model() {
		GUIState gui = new GUIState();
		gui.activityName = "com.Activity3";
		gui.id = 2;
		
		Event e1 = new Event(EventType.TAP, 570, 700, gui.id);
		e1.handlerMethodSig = "Lcom/Activity3$1;->onClick(Landroid/view/View;)V";
		Event e2 = new Event(EventType.TAP, 570, 1010, gui.id);
		e2.handlerMethodSig = "Lcom/Activity3$2;->onClick(Landroid/view/View;)V";
		Event e3 = new Event(EventType.TAP, 550, 1370, gui.id);
		e3.handlerMethodSig = "Lcom/Activity3$3;->onClick(Landroid/view/View;)V";
		gui.stayingEvents = new ArrayList<Event>(Arrays.asList(e1, e2, e3));
		
		return gui;
	}
	
}

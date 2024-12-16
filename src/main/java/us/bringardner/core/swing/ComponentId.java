package us.bringardner.core.swing;


/*
 * Utility for GUI testing
 */
public interface ComponentId {
	public static final int CLOCK_ID = 1;
	public static final int DATE_PANEL_ID = 2;
	public static final int DAY_PANEL_ID = 3;
	public static final int TIME_PANEL_ID = 4;
	
	String getComponentName();
	int getComponentId();
}

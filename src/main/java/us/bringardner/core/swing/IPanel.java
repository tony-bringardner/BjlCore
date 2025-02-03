package us.bringardner.core.swing;

import java.awt.Component;
import java.awt.LayoutManager;

public interface IPanel {
	public Component add(Component testButton);

	public void setLayout(LayoutManager object);

	public void add(Component c, Object west);
}

/**
 * <PRE>
 * 
 * Copyright Tony Bringarder 1998, 2025 <A href="http://bringardner.com/tony">Tony Bringardner</A>
 * 
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       <A href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</A>
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  </PRE>
 *   
 *   
 *	@author Tony Bringardner   
 *
 *
 * ~version~V000.01.01-V000.01.00-
 */
package us.bringardner.core.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

public class GradientButton extends GradientPanel implements IButton {

	private static final long serialVersionUID = 1L;
	private static final Color GradiantPanelStartColor = new Color(242, 206, 113);
	private static final Color GradiantPanelEndColor   = new Color(150, 123, 40);
	private List<ActionListener> actionListeners = new ArrayList<>();
	private String text="Text";
	private Color textColor=Color.white;
	private int borderSize = 2;
	private boolean pressed = false;
	
	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new GradientButton());

		frame.setSize(700, 400);
		frame.setLocationRelativeTo(null);

		//frame.pack();
		SwingUtilities.invokeLater(()->frame.setVisible(true));

	}

	/**
	 * Create the panel.
	 * 
	 */
	public GradientButton() {
		super(Direction.DiagonalLeft);
		setPreferredSize(new Dimension(122, 29));
		setBackground(GradiantPanelStartColor);
		setForeground(GradiantPanelEndColor);

		setBorder(new LineBorder(getForeground(),borderSize,true));
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				pressed = true;		
				updateUI();
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				pressed = false;
				updateUI();
				ActionEvent ae = new ActionEvent(GradientButton.this, ActionEvent.ACTION_FIRST, "");
				for(ActionListener l : actionListeners) {
					l.actionPerformed(ae);
				}

			}
		});
	}


	public GradientButton(String string) {
		this();
		setText(string);
	}

	@Override
	public void setBackground(Color bg) {
		super.setBackground(bg);
		setBorder(new LineBorder(bg, borderSize,true));		
	}

	@Override
	public void setForeground(Color fg) {
		super.setForeground(fg);
		setBorder(new LineBorder(fg, borderSize,true));
	}

	public Color getTextColor() {
		return textColor;
	}

	public void setTextColor(Color textColor) {
		this.textColor = textColor;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getBorderSize() {
		return borderSize;
	}

	public void setBorderSize(int borderSize) {
		this.borderSize = borderSize;
	}

	public void addActionListener(ActionListener listener) {
		actionListeners.add(listener);
	}

	public boolean removeActionListener(ActionListener listener) {
		return actionListeners.remove(listener);
	}

	@Override
	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		//g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		RoundRectangle2D r = new RoundRectangle2D.Float(2, 2, getWidth()-4, getHeight()-4, 10, 10);
		g2.setClip(r);
		g2.setClip(r);
		g2.clip(r);

		super.paint(g);
		
		if(text !=null && !text.isEmpty()) {

			Color c = g2.getColor();
			FontMetrics fm = g2.getFontMetrics();
			Rectangle2D b = fm.getStringBounds(text, g2);
			float x = (float) ((getWidth()/2)-((b.getWidth()/2)));
			float y = (float) ((getHeight()/2)+((b.getHeight()/2)))-2;
			g2.setColor(textColor);
			g2.drawString(text, x, y);
			g2.setColor(c);
		}
		g2.setClip(null);
		if( !pressed ) {
			g2.setColor(getForeground());
		} else {
			g2.setColor(getBackground());
		}
		g2.setStroke(new BasicStroke(getBorderSize()));
		g2.draw(r);
		
	}


}

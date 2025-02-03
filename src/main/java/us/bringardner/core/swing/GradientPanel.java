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
 * ~version~V000.01.04-V000.01.01-V000.01.00-
 */
package us.bringardner.core.swing;




import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


public class GradientPanel extends JPanel  implements IPanel {

	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new GradientPanel());

		frame.setSize(700, 400);
		frame.setLocationRelativeTo(null);

		SwingUtilities.invokeLater(()->frame.setVisible(true));

	}

	
	public static final Color GradiantPanelStartColor = new Color(242, 206, 113);
	public static final Color GradiantPanelEndColor   = new Color(150, 123, 40);
	public static final Color TRANSPARENT = new Color(255, 255, 255,0);
	
	public enum Direction {Horizonal,Vertical,DiagonalLeft,DiagonalRight};
	
	private Direction direction = Direction.DiagonalRight;
	private boolean cyclic;
	private int maxLength;

	public GradientPanel(){
		super( new BorderLayout() );
		setOpaque( false );
		setForeground(GradiantPanelEndColor);
		setBackground(GradiantPanelStartColor);                
	}

	public GradientPanel( Direction direction){
		this();
		this.direction = direction;
	}

	public Direction getDirection(){
		return direction;
	}

	public void setDirection( Direction direction ) {
		this.direction = direction;
	}

	public boolean isCyclic() {
		return cyclic;
	}

	public void setCyclic( boolean cyclic ) {
		this.cyclic = cyclic;
	}

	public void setMaxLength( int maxLength ) {
		this.maxLength = maxLength;
	}


	public void paintComponent( Graphics g ) {
		if( isOpaque() )
		{
			super.paintComponent( g );
			return;
		}

		int width = getWidth();
		int height = getHeight();

		// Create the gradient paint
		GradientPaint paint = null;

		Color sc = getForeground();
		Color ec = getBackground();

		switch( direction )
		{
		case Horizonal :
		{
			paint = new GradientPaint( 0, height / 2, sc, width, height / 2, ec, cyclic );
			break;
		}
		case Vertical :
		{
			paint = new GradientPaint( width / 2, 0, sc, width / 2, maxLength > 0 ? maxLength : height, ec, cyclic );
			break;
		}
		case DiagonalLeft :
		{
			paint = new GradientPaint( 0, 0, sc, width, height, ec, cyclic );
			break;
		}
		case DiagonalRight:
		{
			paint = new GradientPaint( width, 0, sc, 0, height, ec, cyclic );
			break;
		}
		}

		if( paint == null )
		{
			throw new RuntimeException( "Invalid direction specified in GradientPanel" );
		}

		// we need to cast to Graphics2D for this operation
		Graphics2D g2d = ( Graphics2D )g;

		// save the old paint
		Paint oldPaint = g2d.getPaint();

		// set the paint to use for this operation
		g2d.setPaint( paint );

		// fill the background using the paint
		g2d.fillRect( 0, 0, width, height );

		// restore the original paint
		g2d.setPaint( oldPaint );

		super.paintComponent( g );
	}
}
/**
 * <PRE>
 * 
 * Copyright Tony Bringarder 1998, 2025 <A href="http://bringardner.us/tony">Tony Bringardner</A>
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
 * ~version~V000.01.08-V000.00.01-V000.00.00-
 */
package us.bringardner.core.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.Calendar;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Graphical implementation of an analog clock
 */
public class Clock extends JPanel implements MouseListener, MouseMotionListener ,ComponentId {
	
	private static final long serialVersionUID = 1L;
	private static final double PI = 3.14159f;
	private static final double RAD2DEG = 57.2957795;

	private static final Color COLOR_20_20_20 = new Color(20, 20, 20);
	private static final Color COLOR_230_230_230 = new Color(230, 230, 230);
	private static final Color COLOR_170_170_170 = new Color(170, 170, 170);
	
	public static final String PROP_MIN_CHANGED = "MinuteChanged";
	public static final String PROP_HOUR_CHANGED = "HourChanged";
	private static final int TYPE_HR = 3;
	private static final int TYPE_MI = 1;
	private static final double CALC_2_PIE_60 = 2 * PI / 60;
	private static final double CALC_2_PIE_12 = 2 * PI / 12;

	private BufferedImage bimg;
	private boolean showText=true;
	private boolean savedShowText=true;;
	private boolean showPt=false;
	
	
	private int hour, minute, radius , lenH, lenM, cx, cy;
	private int startHr, startMin;
	private Ellipse2D hrPoint = null;
	private Ellipse2D miPoint = null;
	
	
	private int type;
	private Color clockBackground = COLOR_170_170_170;
	private Color shadowColor = COLOR_230_230_230;
	private Color clockForground = COLOR_20_20_20;
	private Color minuteColor=Color.green;
	private Color hourColor=Color.red;
	private int mhour;
	private int seconds;
	private int milliSeconds;
	


	public Clock() {
		init(System.currentTimeMillis());
	}

	
	public Clock(long date) {
		init(date);
	}

	public boolean isShowText() {
		return savedShowText;
	}

	public void setShowText(boolean showText) {
		this.showText = showText;
		this.savedShowText = showText;
	}

	public void init(long date)      {
		setPreferredSize(new Dimension(200,200));
		addMouseListener(this);
		addMouseMotionListener(this);
		Calendar currTime = Calendar.getInstance();
		currTime.setTimeInMillis(date);
		hour = currTime.get(Calendar.HOUR_OF_DAY);
		mhour = hour;
		minute = currTime.get(Calendar.MINUTE);
		seconds = currTime.get(Calendar.SECOND);
		milliSeconds = currTime.get(Calendar.MILLISECOND);
	} 

	public void setHourColor(Color hourColor) {
		this.hourColor = hourColor;
	}



	public void setMinuteColor(Color minuteColor) {
		this.minuteColor = minuteColor;
	}



	
	
	

	public void drawClock(Graphics g, int width, int height, int radius) {
		//  Draw clock
		drawOval(g, width, height, radius);
		int x1,x2,y1,y2;
		
		for (int i = 1; i < 13; i++)	{
			g.setColor(getClockForground());
			double deg = (double) (i * CALC_2_PIE_12);
			x2 = (int)(cx + radius * Math.sin(deg));
			y2 = (int)(cy - radius * Math.cos(deg));
			if (i % 3 != 0)			{
				x1 = (int)(cx + 0.9f * radius * Math.sin(deg));
				y1 = (int)(cy - 0.9f * radius * Math.cos(deg));
			}	else	{
				x1 = (int)(cx + 0.8f * radius * Math.sin(deg));
				y1 = (int)(cy - 0.8f * radius * Math.cos(deg));
			}
			g.drawLine(x1, y1, x2, y2);
			g.setColor(getShadowColor());
			g.drawLine(x1 - 1, y1 - 1, x2 - 1, y2 - 1);
		}
		

	}

	public Color getClockForground() {
		
		return clockForground ;
	}



	public Color getShadowColor() {
		
		return shadowColor ;
	}



	public void drawOval(Graphics g, int width, int height, int radius) {
		// oval
		int x=(width - 2 * radius) / 2;
		int y=(height - 2 * radius) / 2;
		int wh = 2 * radius;
		g.setColor(getClockBackground());
		g.fillOval(x, y, wh, wh);
		
		g.setColor(getClockForground());
		g.drawOval(x, y, wh, wh); 
		g.setColor(getShadowColor());
		g.drawOval(x-1, y-1, wh, wh); 
		
	}
	
	public Color getClockBackground() {
		
		return clockBackground ;
	}



	public Graphics2D createGraphics2D(int w, int h) {
	        Graphics2D g2 = null;
	        if (bimg == null || bimg.getWidth() != w || bimg.getHeight() != h) {
	        	Object obj =  createImage(w, h);
	            bimg = (BufferedImage)obj;
	            //reset(w, h);
	        } 
	        g2 = bimg.createGraphics();
	        g2.setBackground(getBackground());
	        g2.clearRect(0, 0, w, h);
	        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	        		RenderingHints.VALUE_ANTIALIAS_ON);
	        return g2;
	    }
	 
	 public void update(Graphics g) {
	        paint(g); 
	 }


	public void paint(Graphics rg) {
		int width = getWidth();
		int height = getHeight();

		
		//  Radius is 80% of max(w,h);
		radius = (int)((Math.min(width,height) * 0.8)/2);
	    lenH = 5 * radius / 10;
	    lenM = 8 * radius / 10;
	    cx = width / 2;
	    cy = height / 2;
	    int ptSz = (int)(lenM * 0.3);
	    
		 int ptSzC = ptSz/2;
		 
	    Graphics2D g = createGraphics2D(width, height);

		//  Draw clock
		drawClock(g,width,height,radius);
		
		int x2,y2;

		x2 = (int)(cx + lenM * Math.sin((minute ) * CALC_2_PIE_60));
		y2 = (int)(cy - lenM * Math.cos((minute ) * CALC_2_PIE_60));
		int st = (int) (lenM*0.04);
		
		
		if(st < 1) {
			st = 1;
		}
		
		
		
		miPoint = new Ellipse2D.Float(x2-ptSzC, y2-ptSzC,ptSz,ptSz);
		g.setStroke(new BasicStroke(1));
		g.setColor(getMinuteColor());
		drawTriangle(g,lenM,minute,CALC_2_PIE_60,1.0);
		
		
		if( showPt ) {
			if( type == TYPE_MI ) {
				g.fill(miPoint);
			} else {
				g.draw(miPoint);
			}
		}
		g.setStroke(new BasicStroke(st));
		g.drawLine(cx, cy, x2, y2);
		g.drawLine(cx - 1, cy - 1, x2 - 1, y2 - 1);

		g.setStroke(new BasicStroke(1));
		x2 = (int)(cx + lenH * Math.sin((hour ) * CALC_2_PIE_12));
		y2 = (int)(cy - lenH * Math.cos((hour ) * CALC_2_PIE_12));
		// Shadow
		
		hrPoint = new Ellipse2D.Float(x2-ptSzC, y2-ptSzC,ptSz,ptSz);
		g.setColor(getHourColor());
		
		if( showPt ) {
			if( type == TYPE_HR ) {
				g.fill(hrPoint);
			} else {
				g.draw(hrPoint);
			}
			
		}
		g.setStroke(new BasicStroke(st));
		g.drawLine(cx, cy, x2, y2);
		g.drawLine(cx - 1, cy - 1, x2 - 1, y2 - 1);

		drawTriangle(g,lenH,hour,CALC_2_PIE_12,0.3);
		drawTest(g);
		//g.dispose();
        rg.drawImage(bimg, 0, 0, this);

	}
	
	
	
	private void drawTriangle(Graphics2D g, int len,int value, double factor,double b2) {
		double b1 = (len*0.90);
		double b1_1 = (len*1.1);
		
		int x2 = (int)(cx + b1_1 * Math.sin((value ) * factor));
		int y2 = (int)(cy - b1_1 * Math.cos((value ) * factor));

		Polygon tri = new Polygon();
		tri.addPoint(x2, y2);
		int xt = (int)(cx + b1 * Math.sin((value-b2 ) * factor));
		int yt = (int)(cy - b1 * Math.cos((value-b2 ) * factor));

		tri.addPoint(xt, yt);
		xt = (int)(cx + b1 * Math.sin((value+b2 ) * factor));
		yt = (int)(cy - b1 * Math.cos((value+b2 ) * factor));

		tri.addPoint(xt, yt);
		g.fill(tri);
		
		
	}

	public Color getMinuteColor() {
		
		return minuteColor;
	}

	public Color getHourColor() {
		
		return hourColor;
	}


	private void drawTest(Graphics2D g) {
		if(showText) {
			AttributedString str = new AttributedString(pad(""+(hour == 0 ? 12:hour))+":"+pad(""+minute));
			str.addAttribute(TextAttribute.FOREGROUND, getHourColor(),0,2);
			str.addAttribute(TextAttribute.FOREGROUND, Color.black,2,3);
			str.addAttribute(TextAttribute.FOREGROUND, getMinuteColor(),3,5);
			g.drawString(str.getIterator(), 10, 10);
			
			str = new AttributedString(pad(""+(getHourOfDay()))+":"+pad(""+minute));
			str.addAttribute(TextAttribute.FOREGROUND, getHourColor(),0,2);
			str.addAttribute(TextAttribute.FOREGROUND, Color.black,2,3);
			str.addAttribute(TextAttribute.FOREGROUND, getMinuteColor(),3,5);
			g.drawString(str.getIterator(), 20, 20);
		}
		
	}

	private String pad(String string) {
		String ret = string;
		if( string.length() < 2 ) {
			ret = "0"+string;
		}
		return ret;
	}

	private void calcIt(int myx, int myy) {
		double dy = (double)(cy-myy);
		double dx = (double)(cx-myx);
		//  If dx or dy are 0.0 we get the wrong answer.
		if( dy == 0.0) {
			dy = 1.0;
		}
		if( dx == 0.0 ) {
			dx = 1.0;
		}
		double slope = 0.0;
		if( dx != 0.0 ) {
			slope = dy/dx;
		}
		//double rad = Math.atan(Math.abs(slope));
		double rad = Math.atan(slope);
		double deg = rad * RAD2DEG;
		
		if( dx <= 0.0 && dy >= 0.0 ) {
			// Q1
			deg = Math.abs(deg);
			//System.out.println("Q1");
		} else if( dx >= 0.0 && dy >= 0.0 ) {
			// Q2
			deg = 90+(90-Math.abs(deg));
			//System.out.println("Q2");
		} else if( dx >= 0.0 && dy <= 0.0 ) {
			// Q2
			deg = Math.abs(deg)+180;
			//System.out.println("Q3");
		} else {
			deg = 270+(90-Math.abs(deg));
			//System.out.println("Q4");
		}
		
		//  shift by 90%
		deg = deg-90;
		if( deg < 0 ){
			deg=360+deg;
		}
		
		int lastHour = hour;
		
		switch(type) {
		case TYPE_HR:
					
					hour = 12-(int)(((deg+15) /360)*12);
					if( hour > 12 ) {
						hour -= 12;
					}
					if(hour <= 0 ) {						
						hour = 12;
					}
					
					break;
		case TYPE_MI:
					int last = minute;
					minute = 60-(int)(((deg+0) /360)*60);
					if(minute>59) {
						minute=0;
					}
					//
					if( last > 50 && minute < 10) {
						//System.out.println("++ hr="+hour);
						if( ++hour > 12 ) {							
							hour = 1;		
						}
					} else if( last < 10 && minute > 50 ) {
						//System.out.println("-- hr="+hour);
						if( --hour < 1) {							
							hour = 12;							
						}
					}
					//System.out.println("Result hr="+hour);
					break;
		} 
		
		if( hour <0 || hour > 23) {
			System.out.println("Bad hr");
		}
		if(hour != lastHour) {
			// calc the mhour
			if( (hour == 1 && lastHour == 12) || (hour == 12 && lastHour == 1)) {
				if( mhour > 0 )  {
					mhour = 0;
				} else {
					mhour = 12;
				}
			} 
		}
	}





	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Clock clock = new Clock(System.currentTimeMillis());
		
		clock.setHour(15);
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.add(clock);
		frame.setSize(new Dimension(400,400));
		frame.pack();
		
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				System.out.println("hour="+clock.getHour());
				System.out.println("hour="+clock.getHourOfDay());				
			}
		});
		
	}

	
	public void mouseClicked(MouseEvent event) {
		// nothing to do
	}
	
	public void mouseEntered(MouseEvent event) {
		showPt = true;
		repaint();
	}

	public void mouseExited(MouseEvent event) {
		showPt = false;
		repaint();
		
	}

	public void mousePressed(MouseEvent event) {
		
		startHr = hour;
		startMin = minute;
		showText=true;
		if(miPoint.contains(event.getPoint()))  {
			type = TYPE_MI;
		} else if(hrPoint.contains(event.getPoint()))  {
			type=TYPE_HR;
		}  else {
			type = 0;
		}
		
	}

	public void mouseReleased(MouseEvent event) {
		calcIt(event.getX(),event.getY());
		if( hour != startHr ) {
			firePropertyChange(PROP_HOUR_CHANGED,startHr,hour);
		}
		if( minute != startMin ) {
			firePropertyChange(PROP_MIN_CHANGED,startMin,minute);
		}
		
		type = startHr = startMin = -1;
		showText = savedShowText;
		repaint();
	}

	public void mouseDragged(MouseEvent event) {
		if( type > 0 ) {
			
			int oldHr = hour;
			int oldMi = minute;
			calcIt(event.getX(),event.getY());
			if( hour != oldHr || minute != oldMi) {
				repaint();
			}
		}

	}

	public void mouseMoved(MouseEvent event) {
		// nothing to do
		
	}



	public int getHour() {
		int ret = hour;
		if( ret == 0 ) {
			ret = 12;
		}
		return ret;
	}

	public int getHourOfDay() {
		int ret = hour+mhour;
		if( ret == 24 ) {
			ret = 0;
		}
		return ret;
	}


	public void setHour(int hour) {
		this.hour = hour;
		if( hour > 12) {
			mhour = 11;
			this.hour = hour - 12;
		} else {
			mhour = 0;
		}
		
		repaint();
	}



	public int getMinute() {
		return minute;
	}



	public void setMinute(int minute) {
		this.minute = minute;
		repaint();
	}



	public void setClockBackground(Color clockBackground) {
		this.clockBackground = clockBackground;
	}



	public void setClockForground(Color clockForground) {
		this.clockForground = clockForground;
	}



	public void setShadowColor(Color shadowColor) {
		this.shadowColor = shadowColor;
	}

	/**
	 * 
	 * @return seconds from the original time
	 */
	
	public int getSeconds() {
		return seconds;
	}

	/**
	 * milliseconds from the original time
	 * @return
	 */
	public int getMilliSeconds() {
		return milliSeconds;
	}


	@Override
	public String getComponentName() {
		return "Clock";
	}


	@Override
	public int getComponentId() {
		return CLOCK_ID;
	}

	
}

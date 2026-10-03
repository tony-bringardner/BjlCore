/**
 * <PRE>
 * 
 * Copyright 1998-2026 <A href="http://bringardner.us/tony">Tony Bringardner</A>
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
 * ~version~V000.00.01-V000.00.00-
 */
package us.bringardner.core.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

public class DayPanel extends JPanel implements ComponentId {

	private static final long serialVersionUID = 1L;
	public static final String PROP_DAY = "Calendar.Day";
	private Date date;
	private int mo;
	private int day;
	private List<JTextField> days;
	

	public DayPanel() {
		this(new Date());
	}
	
	public DayPanel(Date date) {
		this.date = date;		
		init();
	}
	
	private void init() {
		//setBounds(0, 0, 150, 200);
		setPreferredSize(new Dimension(150, 250));
		days = new ArrayList<JTextField>();
		//SpringLayout layout = new SpringLayout();
		Cursor cusor = getCursor();
		//RiverLayout layout = new RiverLayout(0,0);
		
		//setLayout(layout);
		
		GridLayout glayout = new GridLayout();
		glayout.setColumns(7);
		glayout.setRows(0);
		glayout.setHgap(0);
		glayout.setVgap(0);
		setLayout(glayout);
		
		//  The calendar (and so the first day of the week) is for the default locale,
		//  Sunday in the US, Monday in most of Europe.
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		mo = cal.get(Calendar.MONTH);
		day = cal.get(Calendar.DAY_OF_MONTH);

		Calendar lastMo = (Calendar) cal.clone();
		lastMo.set(Calendar.DAY_OF_MONTH, 1);
		lastMo.add(Calendar.MONTH,-1);

		int first = cal.getFirstDayOfWeek();
		cal.set(Calendar.DAY_OF_MONTH, 1);
		int dow = cal.get(Calendar.DAY_OF_WEEK);

		//  One column per day of the week, starting with the first day of the week
		Locale locale = Locale.getDefault(Locale.Category.FORMAT);
		for(int i=0; i < 7; i++ ) {
			add(config(new JLabel("  "+dayName((first-1+i)%7+1, locale))));
		}

		String tmp = null;

		//  The end of last month fills the columns before the first day of this month
		int lead = (dow-first+7)%7;
		int lastDaMO = lastMo.getActualMaximum(Calendar.DAY_OF_MONTH)-lead+1;
		for(int i=0; i < lead; i++ ) {
			add(config(new JLabel(""+(lastDaMO+i))));
		}

		JTextField cur=null;
		int cells = lead;

		while(cal.get(Calendar.MONTH)==mo) {
			final int day1 = cal.get(Calendar.DAY_OF_MONTH);
			
			if( day1 < 10 ) {
				tmp = "0"+day1;
			} else {
				tmp = ""+day1;
			}
			
			JTextField textField = new JTextField(tmp);
			//  Named so tests (and tools) can find a day without reaching into this class
			textField.setName("day"+day1);
			days.add(textField);
			config(textField);
			textField.setBorder(null);
			textField.setFocusable(false);
			textField.setCursor(cusor);
			
			add(textField);
			cells++;
			if( day1 == day) {
				textField.setBackground(Color.blue);
				textField.setForeground(Color.white);
				cur = textField;				
			}
			
			textField.addMouseListener(new MouseAdapter(){

				public void mouseClicked(MouseEvent e) {
					if( day1 != day ) {
						int oldValue = day;
						JTextField fld = (JTextField) days.get(day-1);
						fld.setBackground(Color.white);
						fld.setForeground(Color.black);
						day = day1;
						fld = (JTextField) days.get(day-1);
						fld.setBackground(Color.blue);
						fld.setForeground(Color.white);
						firePropertyChange(PROP_DAY, oldValue, day);
					}
				}

				public void mouseEntered(MouseEvent e) {					
					((JComponent)e.getSource()).setBorder(new LineBorder(Color.blue));					
				}

				public void mouseExited(MouseEvent e) {
					((JComponent)e.getSource()).setBorder(null);					
				}
				
			});
		    cal.add(Calendar.DAY_OF_MONTH,1);
		}
		
		
		//  The start of next month fills the rest of the last week
		int trail = (7-cells%7)%7;
		for(int day1=1; day1 <= trail; day1++ ) {
			add(config(new JLabel("0"+day1)));
		}
		if( cur != null ) {
			cur.requestFocus();
		}

	}

	/**
	 * @param calendarDay a Calendar day of the week (Calendar.SUNDAY=1 ... Calendar.SATURDAY=7)
	 * @param locale
	 * @return the one letter name of the day ("S", "M", "T" ... in English)
	 */
	private static String dayName(int calendarDay, Locale locale) {
		//  DayOfWeek starts on Monday (1) and ends on Sunday (7)
		DayOfWeek dow = DayOfWeek.of((calendarDay+5)%7+1);
		return dow.getDisplayName(TextStyle.NARROW, locale);
	}
	
	 private Component config(JComponent comp) {
		 comp.setPreferredSize(new Dimension(22,24));
		return comp;
	}


	@Override
	public String getComponentName() {
		return "DayPanel";
	}

	@Override
	public int getComponentId() {
		return DAY_PANEL_ID;
	}



}

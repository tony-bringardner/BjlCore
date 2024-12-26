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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
		
		Calendar cal = Calendar.getInstance();
		Calendar lastMo = Calendar.getInstance();
		Calendar nextMo = Calendar.getInstance();
		cal.setTime(date);
		lastMo.setTime(date);
		nextMo.setTime(date);
		lastMo.add(Calendar.MONTH,-1);
		nextMo.add(Calendar.MONTH,1);

		mo = cal.get(Calendar.MONTH);
		day = cal.get(Calendar.DAY_OF_MONTH);
		
		
		
		int first = cal.getFirstDayOfWeek();
		cal.set(Calendar.DAY_OF_MONTH, first);
		int dow = cal.get(Calendar.DAY_OF_WEEK);
		//cal.getFirstDayOfWeek()
		
		add("tab",config(new JLabel("  S")));
		add(config(new JLabel("  M")));
		add(config(new JLabel("  T")));
		add(config(new JLabel("  W")));
		add(config(new JLabel("  T")));
		add(config(new JLabel("  F")));
		add(config(new JLabel("  S")));
		
		int col=first;
		String tmp = null;
		
		int lastDaMO = lastMo.getActualMaximum(Calendar.DAY_OF_MONTH);		
		lastDaMO -= (dow-2);
		
		while(col < dow) {
			if( col == 1 ) {
				add("br tab",config(new JLabel(""+lastDaMO)));
			} else {
				add(config(new JLabel(""+lastDaMO)));
			}
			col++;
			lastDaMO++;
		}
		
		JTextField cur=null;
		
		while(cal.get(Calendar.MONTH)==mo) {
			dow = cal.get(Calendar.DAY_OF_WEEK);			
			final int day1 = cal.get(Calendar.DAY_OF_MONTH);
			
			if( day1 < 10 ) {
				tmp = "0"+day1;
			} else {
				tmp = ""+day1;
			}
			
			JTextField textField = new JTextField(tmp);
			days.add(textField);
			config(textField);
			textField.setBorder(null);
			textField.setFocusable(false);
			textField.setCursor(cusor);
			
			if(col++ == 1 || dow == 1) {
				add("br tab",textField);
			} else {
				add(textField);
			}
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
		
		
		int day1 = 1;
		
		while(dow < 7) {
			add(config(new JLabel("0"+day1)));
			dow++;
			day1++;
		}
		cur.requestFocus();
		
	}
	
	 private Component config(JComponent comp) {
		 comp.setPreferredSize(new Dimension(22,24));
		return comp;
	}
	 

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		final JFrame frame = new JFrame("Day Test");
		frame.setSize(200, 400);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		DayPanel edit = new DayPanel();
		frame.getContentPane().add(edit);
		frame.setVisible(true);

		frame.addWindowListener(new WindowAdapter() {

			public void windowClosed(WindowEvent e) {
				System.out.println(frame.getSize().toString());				
			}
			
		});

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

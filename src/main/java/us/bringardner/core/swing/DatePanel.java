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
/*
 * DatePanel.java
 *
 * 
 */

package us.bringardner.core.swing;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JSpinner;

/**
 *
 * @author  Tony Bringardner
 */
public class DatePanel extends javax.swing.JPanel implements ComponentId {

	private static final long serialVersionUID = 1L;
	public static final String PROP_DATE_CHANGED = "DateChanged";
	private Calendar cal;
	private javax.swing.JPanel centerPanel;
	private us.bringardner.core.swing.DayPanel dayPanel1;
	private javax.swing.JComboBox<String> monthCombo;
	private javax.swing.JPanel northPanel;
	private javax.swing.JPanel southPanel;
	private javax.swing.JButton todayButton;
	private javax.swing.JSpinner yearSpinner;


	/** Creates new form DatePanel */
	public DatePanel() {
		this(new Date());
	}

	public DatePanel(Date date) {
		initComponents();
		JSpinner.DefaultEditor editor = new JSpinner.NumberEditor(yearSpinner,
				"####");

		yearSpinner.setEditor(editor);

		setDate(date);
	}

	public void setDate(Date date) {
		cal = Calendar.getInstance();
		cal.setTime(date);
		centerPanel.remove(dayPanel1);
		dayPanel1 = new DayPanel(date);
		dayPanel1.addPropertyChangeListener(DayPanel.PROP_DAY,
				new PropertyChangeListener() {

					public void propertyChange(PropertyChangeEvent evt) {
						Date oldValue = cal.getTime();
						Integer newDay = (Integer) evt.getNewValue();
						cal.set(Calendar.DAY_OF_MONTH, newDay.intValue());
						Date newValue = cal.getTime();
						firePropertyChange(PROP_DATE_CHANGED, oldValue,
								newValue);
					}

				});
		centerPanel.add(dayPanel1);

		yearSpinner.setValue(cal.get(Calendar.YEAR));
		monthCombo.setSelectedIndex(cal.get(Calendar.MONTH));
		centerPanel.revalidate();
		revalidate();
	}

	public Date getDate() {
		return cal.getTime();
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Date Test");
		frame.setSize(250, 275);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		DatePanel edit = new DatePanel();
		frame.getContentPane().add(edit);
		
		frame.setLocationRelativeTo(null);
		frame.pack();
		frame.setVisible(true);

	}

	/** 
	 * This method is called from within the constructor to
	 * initialize the form.
	 */
	private void initComponents() {

		northPanel = new javax.swing.JPanel();
		monthCombo = new javax.swing.JComboBox<String>();
		yearSpinner = new javax.swing.JSpinner();
		centerPanel = new javax.swing.JPanel();
		dayPanel1 = new us.bringardner.core.swing.DayPanel();
		southPanel = new javax.swing.JPanel();
		todayButton = new javax.swing.JButton();

		setBorder(javax.swing.BorderFactory.createTitledBorder("Date"));
		setLayout(new java.awt.BorderLayout());

		monthCombo.setModel(new javax.swing.DefaultComboBoxModel<String>(new String[] {
				"January", "Febuary", "March", "April", "May", "June", "July",
				"August", "Septemper", "October", "November", "December" }));
		//monthCombo.setPreferredSize(new java.awt.Dimension(100, 25));
		monthCombo.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				monthComboActionPerformed(evt);
			}
		});
		northPanel.add(monthCombo);

		yearSpinner.setPreferredSize(new java.awt.Dimension(60, 25));
		yearSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent evt) {
				yearSpinnerStateChanged(evt);
			}
		});
		northPanel.add(yearSpinner);

		add(northPanel, java.awt.BorderLayout.PAGE_START);

		centerPanel.add(dayPanel1);

		add(centerPanel, java.awt.BorderLayout.CENTER);

		todayButton.setText("Today");
		todayButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				todayButtonActionPerformed(evt);
			}
		});
		southPanel.add(todayButton);

		add(southPanel, java.awt.BorderLayout.SOUTH);
	}
	
	private void todayButtonActionPerformed(java.awt.event.ActionEvent evt) {
		Calendar cal = Calendar.getInstance();
		setDate(cal.getTime());
	}

	private void yearSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {
		Date oldValue = cal.getTime();
		Integer year = (Integer) yearSpinner.getValue();
		cal.set(Calendar.YEAR, year.intValue());
		setDate(cal.getTime());
		Date newValue = cal.getTime();
		firePropertyChange(PROP_DATE_CHANGED, oldValue, newValue);
	}

	private void monthComboActionPerformed(java.awt.event.ActionEvent evt) {
		Date oldValue = cal.getTime();
		int mo = monthCombo.getSelectedIndex();
		cal.set(Calendar.MONTH, mo);
		setDate(cal.getTime());
		Date newValue = cal.getTime();
		firePropertyChange(PROP_DATE_CHANGED, oldValue, newValue);
	}

	@Override
	public String getComponentName() {
		return "Date Panel";
	}

	@Override
	public int getComponentId() {
		return DATE_PANEL_ID;
	}


}
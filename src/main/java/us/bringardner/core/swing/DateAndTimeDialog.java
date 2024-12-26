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
 * ~version~V000.01.10-V000.01.09-V000.00.01-V000.00.00-
 */
/*
 * DateAndTimeDialog.java
 *
 * 
 */

package us.bringardner.core.swing;

import java.awt.BorderLayout;
import java.awt.Point;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JPanel;

/**
 *
 * @author  Tony Bringardner
 */
public class DateAndTimeDialog extends javax.swing.JDialog implements ICalendarDialog {
	private static final long serialVersionUID = 1L;
	private javax.swing.JButton cancelButton;
	private us.bringardner.core.swing.TimePanel timePanel;
	private us.bringardner.core.swing.DatePanel datePanel;
	private javax.swing.JLabel displayLabel;
	private javax.swing.JPanel northPanel;
	private javax.swing.JButton nowButton;
	private javax.swing.JButton okButton;
	private javax.swing.JPanel southPanel;
	private JPanel centerPanel;
	
	private boolean canceled;

	/** Creates new form DateAndTimeDialog */
	public DateAndTimeDialog() {
		super();
		setModal(true);
		initComponents();
	}

	public boolean isCanceled() {
		return canceled;
	}

	public Date showDialog(Date date, String label) {
		displayLabel.setText(label);
		datePanel.setDate(date);
		timePanel.setTime(date);
		setVisible(true);
		if (!canceled) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(datePanel.getDate());
			cal.set(Calendar.HOUR_OF_DAY, timePanel.getHour());
			cal.set(Calendar.MINUTE, timePanel.getMinute());
			cal.set(Calendar.SECOND, timePanel.getSecond());
			cal.set(Calendar.MILLISECOND, timePanel.getMillisSecond());
			date = cal.getTime();
		}

		return date;
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 */
	private void initComponents() {
		northPanel = new javax.swing.JPanel();
		displayLabel = new javax.swing.JLabel();
		southPanel = new javax.swing.JPanel();
		okButton = new javax.swing.JButton() {

			private static final long serialVersionUID = 1011903131278469103L;
			
		};
		cancelButton = new javax.swing.JButton();
		nowButton = new javax.swing.JButton();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

		displayLabel.setText("Current Time");
		northPanel.add(displayLabel);

		getContentPane().add(northPanel, java.awt.BorderLayout.PAGE_START);

		okButton.setText("OK");
		okButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				okButtonActionPerformed(evt);
			}
		});
		southPanel.add(okButton);

		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancelButtonActionPerformed(evt);
			}
		});
		southPanel.add(cancelButton);

		nowButton.setText("Now");
		nowButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				nowButtonActionPerformed(evt);
			}
		});
		southPanel.add(nowButton);

		getContentPane().add(southPanel, java.awt.BorderLayout.PAGE_END);
		
		centerPanel = new JPanel();
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		datePanel = new us.bringardner.core.swing.DatePanel();
		centerPanel.add(datePanel);
		timePanel = new us.bringardner.core.swing.TimePanel();
		centerPanel.add(timePanel);

		pack();
	}

	private void nowButtonActionPerformed(java.awt.event.ActionEvent evt) {
		Date date = new Date();
		datePanel.setDate(date);
		timePanel.setTime(date);
	}

	private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
		dispose();
	}

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
		this.canceled = true;
		dispose();
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				DateAndTimeDialog dialog = new DateAndTimeDialog();
				dialog.addWindowListener(new java.awt.event.WindowAdapter() {
					public void windowClosing(java.awt.event.WindowEvent e) {
						System.exit(0);
					}
				});
				dialog.setLocationRelativeTo(null);
				Date date = dialog.showDialog(new Date(),"Test Label");
				System.out.println("Date = " + date);
			}
		});
	}



	public boolean isEditMilliSecond (){
		return timePanel.isEditMilliSeconds();
	}

	public boolean isEditSecond (){
		return timePanel.isEditSeconds();
	}
	
	public void setEditSecond(boolean b) {
		timePanel.setEditSeconds(b);
	}

	public void setEditMilliSecond(boolean b) {
		timePanel.setEditMilliSeconds(b);
	}

	public Date showDialog(Date date, String label, Point p) {
		//  Center on this point
		int w = getWidth() / 2;
		int h = getHeight() / 2;
		if ((p.x -= h) < 0) {
			p.x = 0;
		}
		if ((p.y -= w) < 0) {
			p.y = 0;
		}

		setLocation(p);
		return showDialog(date,label);
	}

	public void setShowTime(boolean b) {
		timePanel.setVisible(b);
		centerPanel.updateUI();
		
	}

	public void setShowDate(boolean b) {
		datePanel.setVisible(b);
		centerPanel.updateUI();		
	}

	public Date showDate(Date date, Point p) {
		setLocation(p);  
		return showDialog(date, "");
	}

}
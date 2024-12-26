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
 * TimeDialog.java
 *
 * 
 */

package us.bringardner.core.swing;

import java.awt.Point;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author  Tony Bringardner
 */
public class TimeDialog extends javax.swing.JDialog implements ICalendarDialog {

	private static final long serialVersionUID = 1L;
	private boolean canceled;
	private javax.swing.JButton cancelButton;
	private javax.swing.JPanel controlPanel;
	private javax.swing.JButton okButton;
	private us.bringardner.core.swing.TimePanel timePanel;
	

	/** Creates new form TimeDialog */
	public TimeDialog(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
	}

	public TimeDialog() {
		super();
		setModal(true);
		initComponents();
	}

	public boolean isCanceled() {
		return canceled;
	}

	public Date showDialog(Date date,String label) {
		timePanel.setTime(date);
		setVisible(true);
		if (!canceled) {
			Calendar cal = timePanel.getTime();
			date = cal.getTime();
		}

		return date;
	}

	private void initComponents() {

		timePanel = new us.bringardner.core.swing.TimePanel();
		controlPanel = new javax.swing.JPanel();
		okButton = new javax.swing.JButton();
		cancelButton = new javax.swing.JButton();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		getContentPane().add(timePanel, java.awt.BorderLayout.CENTER);

		okButton.setText("OK");
		okButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				okButtonActionPerformed(evt);
			}
		});
		controlPanel.add(okButton);

		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancelButtonActionPerformed(evt);
			}
		});
		controlPanel.add(cancelButton);

		getContentPane().add(controlPanel, java.awt.BorderLayout.SOUTH);

		pack();
	}
	
	private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
		dispose();
	}

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
		canceled = true;
		dispose();
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				TimeDialog dialog = new TimeDialog(new javax.swing.JFrame(),
						true);
				dialog.addWindowListener(new java.awt.event.WindowAdapter() {
					public void windowClosing(java.awt.event.WindowEvent e) {
						System.exit(0);
					}
				});
				dialog.setLocationRelativeTo(null);
				dialog.setVisible(true);
			}
		});
	}
	public Date showDialog(Date date,String label, Point p) {
		setLocation(p);
		return showDialog(date,label);
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


}
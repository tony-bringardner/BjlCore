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
 * DateDialog.java
 *
 * 
 */

package us.bringardner.core.swing;

import java.awt.Point;
import java.util.Date;

/**
 *
 * @author  Tony Bringardner
 */
public class DateDialog extends javax.swing.JDialog implements ICalendarDialog {
	private static final long serialVersionUID = 1L;
	private boolean canceled = false;
	private javax.swing.JButton cancelButton;
	private javax.swing.JPanel centerPanel;
	private us.bringardner.core.swing.DatePanel datePanel1;
	private javax.swing.JPanel northPanel;
	private javax.swing.JButton okButton;
	private javax.swing.JPanel southPanel;


	/** Creates new form DateDialog */
	public DateDialog() {
		super();
		initComponents();
	}

	public boolean isCanceled() {
		return canceled;
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 */
	
	private void initComponents() {

		centerPanel = new javax.swing.JPanel();
		datePanel1 = new us.bringardner.core.swing.DatePanel();
		northPanel = new javax.swing.JPanel();
		southPanel = new javax.swing.JPanel();
		okButton = new javax.swing.JButton();
		cancelButton = new javax.swing.JButton();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

		centerPanel.add(datePanel1);

		getContentPane().add(centerPanel, java.awt.BorderLayout.CENTER);
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

		getContentPane().add(southPanel, java.awt.BorderLayout.PAGE_END);

		pack();
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
				DateDialog dialog = new DateDialog();
				dialog.addWindowListener(new java.awt.event.WindowAdapter() {
					public void windowClosing(java.awt.event.WindowEvent e) {

						System.exit(0);
					}
				});
				dialog.setLocationRelativeTo(null);
				Date date = dialog.showDialog(new Date(), "Test Label");
				System.out.println("Date = " + date);
			}
		});
	}

	public Date showDialog(Date date,String label) {
		setModal(true);
		datePanel1.setDate(date);
		setVisible(true);
		if (!canceled) {
			date = datePanel1.getDate();
		}

		return date;
	}


	
	public Date showDialog(Date date,String label, Point p) {
		setLocation(p);
		return showDialog(date,label);
	}

}
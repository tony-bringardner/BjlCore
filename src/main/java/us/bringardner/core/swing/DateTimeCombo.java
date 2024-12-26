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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

public class DateTimeCombo extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private JSpinner spinner;
	private JButton btnBrowse;
	private String label;
	

	public Date getDate() {
		return (Date) spinner.getValue();
	}
	
	
	
	public String getLabel() {
		return label;
	}



	public void setLabel(String label) {
		this.label = label;
	}



	public void setdate(Date date) {
		spinner.setModel(new SpinnerDateModel(date, null, null, Calendar.MILLISECOND));
	}
	
	public DateTimeCombo(long date) {
		this();
		spinner.setModel(new SpinnerDateModel(new Date(date), null, null, Calendar.MILLISECOND));
	}
	
	public DateTimeCombo(Date date) {
		this();
		spinner.setModel(new SpinnerDateModel(date, null, null, Calendar.MILLISECOND));
	}
	
	/**
	 * Create the panel.
	 */
	public DateTimeCombo() {
		FlowLayout flowLayout = (FlowLayout) getLayout();
		flowLayout.setVgap(0);
		flowLayout.setAlignment(FlowLayout.LEFT);
		flowLayout.setAlignOnBaseline(true);
		flowLayout.setHgap(0);
		
		spinner = new JSpinner();
		spinner.setPreferredSize(new Dimension(120, 23));
		spinner.setModel(new SpinnerDateModel(new Date(1392699600000L), null, null, Calendar.MILLISECOND));
		add(spinner);
		
		btnBrowse = new JButton("^");
		btnBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Date date = (Date) spinner.getValue();
				DateAndTimeDialog dialog = new DateAndTimeDialog();
				Date newDate = dialog.showDialog(date, label,btnBrowse.getLocationOnScreen());
				if( newDate != null ) {
					spinner.setValue(newDate);
				}
			}
		});
		btnBrowse.setPreferredSize(new Dimension(20, 23));
		add(btnBrowse);

	}

}

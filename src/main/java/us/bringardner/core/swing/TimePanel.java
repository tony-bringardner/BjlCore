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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TimePanel extends JPanel implements ComponentId {


	private static final long serialVersionUID = 1L;
	private static boolean military = System.getProperty("MilitaryTime", "false").toLowerCase().startsWith("t");

	public static boolean isMilitary() {
		return military;
	}

	public static void setMilitary(boolean military) {
		TimePanel.military = military;
	}

	private Clock clock;
	private JButton btnCurrentTime;
	private JRadioButton amRadio;
	private JRadioButton pmRadio;
	private JCheckBox militaryTimeCheckbox;
	private JSpinner hourSpinner;
	private JSpinner minuteSpinner;
	private final ButtonGroup amPmGroup = new ButtonGroup();
	private JPanel spinnerPanel;
	private JSpinner secondsSpinner;
	private JSpinner milliSecondSpinner;
	private JPanel milliSecondPanel;
	private JPanel secondsPanel;
	private JPanel nowPanel;

	private JPanel analogPanel;
	private boolean editMilliSeconds;
	private boolean editSeconds;
	private JPanel amPmPanel;
	private Dimension analogPreferdSize = new Dimension(200, 200);
	private boolean showAnalog=true;

	public static void main(String args[] ) {
		TimePanel panel = new TimePanel();
		
		//panel.setEditSeconds(true);
		//panel.setEditMilliSeconds(true);
		//panel.setShowAnalog(true);
		
		
		final JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		frame.getContentPane().add(panel);
		frame.setLocationRelativeTo(null);
		frame.pack();
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				frame.setVisible(true);				
			}
		});
	}



	public boolean isShowAnalog() {
		return showAnalog;
	}

	public void setShowAnalog(boolean showAnalog) {
		this.showAnalog = showAnalog;
		analogPanel.setVisible(showAnalog);
		setPreferredSize(calculatePreferdSize());
		updateUI();
	}

	public boolean isEditSeconds() {
		return editSeconds;
	}

	public void setEditSeconds(boolean editSeconds) {
		this.editSeconds = editSeconds;
		secondsPanel.setVisible(editSeconds);
		setPreferredSize(calculatePreferdSize());
		updateUI();
	}

	public boolean isEditMilliSeconds() {
		return editMilliSeconds;
	}

	public void setEditMilliSeconds(boolean editMilliSeconds) {
		this.editMilliSeconds = editMilliSeconds;
		milliSecondPanel.setVisible(editMilliSeconds);
		setPreferredSize(calculatePreferdSize());
		updateUI();
	}

	public void setTime(Calendar cal) {
		setMinute(cal.get(Calendar.MINUTE));
		setHour(cal.get(Calendar.HOUR_OF_DAY));
	}

	public void setTime(Date time) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(time);
		setTime(cal);
	}

	public void setTime(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(time);
		setTime(cal);
	}

	public Calendar getTime(Calendar cal) {
		cal.set(Calendar.MINUTE, getMinute());
		cal.set(Calendar.HOUR_OF_DAY, getHour());
		cal.set(Calendar.SECOND, getSecond());
		cal.set(Calendar.MILLISECOND, getMillisSecond());
		return cal;
	}

	public int getMillisSecond() {
		int ret = 0;
		if( editMilliSeconds) {
			ret = (Integer) milliSecondSpinner.getValue();
		}
		return ret;
	}

	public int getSecond() {
		int ret = 0;
		
		if( editSeconds) {
			ret = (Integer) secondsSpinner.getValue();
		}
		return ret;
	}

	public Calendar getTime() {
		return getTime(Calendar.getInstance());
	}

	public int getHour() {
		int ret = ((Integer) hourSpinner.getValue()).intValue();
		if (!isMilitary()) {
			if (ret == 12) {
				ret = 0;
			}
			if (pmRadio.isSelected()) {
				ret += 12;
			}
		}

		return ret;
	}

	public int getMinute() {
		int ret = ((Integer) minuteSpinner.getValue()).intValue();
		return ret;
	}

	public void setHour(int hr) {
		if (hr > 23) {
			hr = 23;
		}

		if (!isMilitary()) {
			if (hr >= 12) {
				if ((hr -= 12) == 0) {
					hr = 12;
				}
				pmRadio.setSelected(true);
			} else {
				amRadio.setSelected(true);
			}
			if (hr == 0) {
				hr = 12;
			}
			hourSpinner.setModel(new SpinnerNumberModel(1, 1, 12, 1));
			amRadio.setVisible(true);
			pmRadio.setVisible(true);
		} else {
			// this will make things accurate when changing from military time
			if (hr >= 12) {
				pmRadio.setSelected(true);
			} else {
				amRadio.setSelected(true);
			}
			hourSpinner.setModel(new SpinnerNumberModel(0, 0, 23, 1));
			amRadio.setVisible(false);
			pmRadio.setVisible(false);
		}
		hourSpinner.setValue((hr));
		clock.setHour(hr);
		revalidate();
	}

	public void setMinute(int min) {
		minuteSpinner.setValue((min));
		clock.setMinute(min);
	}

	public void setSeconds(int sec) {
		secondsSpinner.setValue(sec);
		updateUI();
	}

	public void setMilliSeconds(int mi) {
		milliSecondSpinner.setValue(mi);
	}

	private void nowButtonActionPerformed(java.awt.event.ActionEvent evt) {
		Calendar cal = Calendar.getInstance();
		setHour(cal.get(Calendar.HOUR_OF_DAY));
		setMinute(cal.get(Calendar.MINUTE));
	}

	private void militaryTimeActionPerformed(java.awt.event.ActionEvent evt) {
		int hr = getHour();
		setMilitary(militaryTimeCheckbox.isSelected());
		setHour(hr);
	}


	private void minSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {
		int newVal = ((Integer) minuteSpinner.getValue()).intValue();
		int oldVal  = clock.getMinute();
		if( newVal != oldVal ) {
			clock.setMinute(newVal);
			firePropertyChange("", oldVal, newVal);
		}
	}

	private void hourSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {
		clock.setHour(((Integer) hourSpinner.getValue()).intValue());
	}

	public TimePanel() {
		this(System.currentTimeMillis());
	}

	/**
	 * Create the panel.
	 */
	public TimePanel(long date) {
		//setPreferredSize(new Dimension(383, 320));
		setLayout(new BorderLayout(0, 0));

		analogPanel = new JPanel();
		add(analogPanel, BorderLayout.CENTER);
		analogPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));		
		clock = new Clock(date);
		clock.setShowText(false);
		clock.setMinuteColor(Color.BLACK);
		clock.setHourColor(Color.BLACK);
		//clock.setPreferredSize(new Dimension(180, 180));
		clock.addPropertyChangeListener(Clock.PROP_HOUR_CHANGED,
				new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (isMilitary()) {
					hourSpinner.setValue((clock.getHourOfDay()));
				} else {
					hourSpinner.setValue((clock.getHour()));
				}
			}

		});


		clock.addPropertyChangeListener(Clock.PROP_MIN_CHANGED,
				new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				minuteSpinner.setValue((clock.getMinute()));
			}
		});


		analogPanel.add(clock);

		JPanel controlPanel = new JPanel();
		controlPanel.setPreferredSize(new Dimension(110, 110));
		add(controlPanel, BorderLayout.SOUTH);
		controlPanel.setLayout(new GridLayout(0, 1, 0, 0));

		amPmPanel = new JPanel();
		controlPanel.add(amPmPanel);

		militaryTimeCheckbox = new JCheckBox("24 Hr");
		amPmPanel.add(militaryTimeCheckbox);
		militaryTimeCheckbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				militaryTimeActionPerformed(e);
			}
		});

		militaryTimeCheckbox.setAlignmentX(Component.RIGHT_ALIGNMENT);

		amRadio = new JRadioButton("AM");
		amPmPanel.add(amRadio);
		amPmGroup.add(amRadio);
		amRadio.setSelected(true);

		pmRadio = new JRadioButton("PM");
		amPmPanel.add(pmRadio);
		amPmGroup.add(pmRadio);

		spinnerPanel = new JPanel();
		controlPanel.add(spinnerPanel);

		hourSpinner = new JSpinner();
		spinnerPanel.add(hourSpinner);
		hourSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				hourSpinnerStateChanged(e);
			}
		});
		hourSpinner.setModel(new SpinnerNumberModel(12, 0, 24, 1));

		JLabel lblNewLabel = new JLabel(":");
		spinnerPanel.add(lblNewLabel);

		minuteSpinner = new JSpinner();
		spinnerPanel.add(minuteSpinner);
		minuteSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				minSpinnerStateChanged(e);
			}
		});
		minuteSpinner.setModel(new SpinnerNumberModel(30, 0, 59, 1));

		secondsPanel = new JPanel();
		secondsPanel.setVisible(false);
		spinnerPanel.add(secondsPanel);

		JLabel lblNewLabel_1 = new JLabel(":");
		secondsPanel.add(lblNewLabel_1);

		secondsSpinner = new JSpinner();
		secondsPanel.add(secondsSpinner);
		secondsSpinner.setModel(new SpinnerNumberModel(30, 0, 59, 1));

		milliSecondPanel = new JPanel();
		milliSecondPanel.setVisible(false);
		spinnerPanel.add(milliSecondPanel);

		JLabel lblNewLabel_2 = new JLabel(".");
		milliSecondPanel.add(lblNewLabel_2);

		milliSecondSpinner = new JSpinner();
		milliSecondPanel.add(milliSecondSpinner);
		milliSecondSpinner.setModel(new SpinnerNumberModel(0, 0, 9999, 1));

		nowPanel = new JPanel();
		controlPanel.add(nowPanel);

		btnCurrentTime = new JButton("Set Current TIme");
		nowPanel.add(btnCurrentTime);
		btnCurrentTime.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				nowButtonActionPerformed(e);
			}
		});


		if (isMilitary()) {
			militaryTimeCheckbox.setSelected(true);
			hourSpinner.setModel(new SpinnerNumberModel(0, 0, 23, 1));
			amRadio.setVisible(false);
			pmRadio.setVisible(false);
		} else {
			hourSpinner.setModel(new SpinnerNumberModel(1, 1, 12, 1));
		}
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(date);
		setHour(c.get(Calendar.HOUR_OF_DAY));
		setMinute(c.get(Calendar.MINUTE));
		setSeconds(c.get(Calendar.SECOND));
		setMilliSeconds(c.get(Calendar.MILLISECOND));
		analogPanel.setPreferredSize(analogPreferdSize);
		clock.setPreferredSize(new Dimension(150, 150));
		setPreferredSize(calculatePreferdSize());
	}
	
	Dimension calculatePreferdSize() {
		Dimension ret = getPreferredSize();
		if( amPmGroup != null && nowPanel!=null && analogPanel!=null) {
		
		
		int w = 340;
		int h = 255;
		
		
		if(! isShowAnalog()) {
			h-=145;
		}
		if(! editSeconds) {
			w-=60;
		}
		if(! editMilliSeconds) {
			w-=80;
		}
		
		ret = new Dimension(w, h);
		}
		return ret;
	}
	
	@Override
	public void updateUI() {
		System.out.println("Before="+
					calculatePreferdSize()
		);
		super.updateUI();
		
	}

	@Override
	public String getComponentName() {
		
		return "TimePanel";
	}

	@Override
	public int getComponentId() {
		return TIME_PANEL_ID;
	}
}

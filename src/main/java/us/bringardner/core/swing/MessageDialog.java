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
 * ~version~V000.01.00-
 */
package us.bringardner.core.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;



public class MessageDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	public enum Response {Yes,No}

	public static final ImageIcon warningIcon = new ImageIcon(MessageDialog.class.getResource("/graphics/Warning100x100.png"));
	public static final ImageIcon ErrorIcon   = new ImageIcon(MessageDialog.class.getResource("/graphics/Error100x100.png"));
	public static final ImageIcon MessageIcon   = new ImageIcon(MessageDialog.class.getResource("/graphics/Globe100x100.png"));



	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			MessageDialog dialog = new MessageDialog();
			dialog.setTitle("Title");
			Response res = dialog.showErrorDialog("A JDBC driver to support the URL could not found. Would you like to browse your local file system for an appropriate JAR file?");
			System.out.println("response = "+res);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private final JPanel contentPanel = new GradientPanel();
	private ImageIcon icon = MessageIcon;	
	Color startColor = new Color(242, 206, 113);
	Color endColor = new Color(150, 123, 40);
	
	private boolean canceled=false;
	private String message="";
	private String yesButtonText="Yes";
	private String noButtonText="No";
	private GradientButton yesButton;
	private GradientButton noButton;
	private JLabel iconLabel;
	private JLabel mesageLabel;

	
	
	public String getYesButtonText() {
		return yesButtonText;
	}
	public void setYesButtonText(String yesButtonText) {
		this.yesButtonText = yesButtonText;
	}
	public String getNoButtonText() {
		return noButtonText;
	}
	public void setNoButtonText(String noButtonText) {
		this.noButtonText = noButtonText;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public ImageIcon getIcon() {
		return icon;
	}
	public void setIcon(ImageIcon icon) {
		this.icon = icon;
	}
	public Color getStartColor() {
		return startColor;
	}
	public void setStartColor(Color startColor) {
		this.startColor = startColor;
	}
	public Color getEndColor() {
		return endColor;
	}
	public void setEndColor(Color endColor) {
		this.endColor = endColor;
	}

	/**
	 * Create the dialog.
	 */
	public MessageDialog() {
		setBounds(100, 100, 450, 264);

		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPanel.setBackground(startColor);
		contentPanel.setForeground(endColor);

		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);

		iconLabel = new JLabel();
		iconLabel.setIcon(icon);
		iconLabel.setBounds(312, 20, 100, 107);
		contentPanel.add(iconLabel);
		{
			mesageLabel = new JLabel( getMessage());
			mesageLabel.setFont(new Font("Times New Roman", Font.PLAIN, 16));
			mesageLabel.setBounds(39, 58, 279, 82);
			contentPanel.add(mesageLabel);
		}

		{
			JPanel buttonPanel = new GradientPanel();
			buttonPanel.setOpaque(false);
			buttonPanel.setBackground(contentPanel.getBackground());
			buttonPanel.setForeground(contentPanel.getForeground());
			buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			{
				yesButton = new GradientButton("OK");
				yesButton.setText(yesButtonText);
				yesButton.setFont(new Font("Times New Roman", Font.PLAIN, 16));
				yesButton.addActionListener((e)->{
					canceled = false;
					dispose();
				});
				buttonPanel.add(yesButton);
			}
			{
				noButton = new GradientButton("Cancel");
				noButton.setText(noButtonText);
				noButton.setFont(new Font("Times New Roman", Font.PLAIN, 16));
				noButton.addActionListener((e)->{
					canceled = true;
					dispose();
				});
				{
					Component horizontalStrut = Box.createHorizontalStrut(20);
					buttonPanel.add(horizontalStrut);
				}
				buttonPanel.add(noButton);
			}
		}
	}

	public Response showErrorDialog(String message) {
		setMessage(message);
		return showErrorDialog();
	}

	public Response showErrorDialog() {
		setIcon(ErrorIcon);
		return showDialog();
	}

	public Response showWarningDialog(String message) {
		setMessage(message);
		return showWarningDialog();
	}

	public Response showWarningDialog() {
		setIcon(warningIcon);
		return showDialog();
	}

	public Response showDialog() {
		String tmp = getMessage();
		if( !tmp.startsWith("<html>")) {
			tmp = "<html>"+tmp+"</html>";
		}
		mesageLabel.setText(tmp);
		iconLabel.setIcon(getIcon());
		yesButton.setText(getYesButtonText());
		noButton.setText(getNoButtonText());
		
		setModal(true);
		setLocationRelativeTo(null);
		setVisible(true);

		if( canceled ) {
			return Response.No;
		} else {
			return Response.Yes;
		}
	}
	public static void showMessageDialog(Component parent, String msg, String title) {
		MessageDialog d = new MessageDialog();
		d.setMessage(msg);
		d.setTitle(title);
		d.showDialog();
		
	}
}

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
 * ~version~V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;

public class CopyManagerFrameBase extends JFrame {

	
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTable table;
	private JLabel statusLabelWest;
	private JProgressBar progressBar;
	private JLabel statusLabelEast;

	
	public JTable getTable() {
		return table;
	}

	public JLabel getStatusLabelWest() {
		return statusLabelWest;
	}

	public JProgressBar getProgressBar() {
		return progressBar;
	}

	public JLabel getStatusLabelEast() {
		return statusLabelEast;
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					CopyManagerFrameBase frame = new CopyManagerFrameBase();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public CopyManagerFrameBase() {
		setBounds(100, 100, 646, 393);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		table = new JTable();
		scrollPane.setViewportView(table);
		
		JPanel statusPanel = new JPanel();
		contentPane.add(statusPanel, BorderLayout.SOUTH);
		statusPanel.setLayout(new BorderLayout(0, 0));
		
		JPanel StatusPanelWest = new JPanel();
		statusPanel.add(StatusPanelWest, BorderLayout.WEST);
		
		statusLabelWest = new JLabel("C:\\help\\me\\long\\name");
		StatusPanelWest.add(statusLabelWest);
		
		JPanel statusPanelCenter = new JPanel();
		statusPanel.add(statusPanelCenter, BorderLayout.CENTER);
		
		progressBar = new JProgressBar();
		progressBar.setValue(50);
		statusPanelCenter.add(progressBar);
		
		JPanel statusPanelEast = new JPanel();
		statusPanel.add(statusPanelEast, BorderLayout.EAST);
		
		statusLabelEast = new JLabel("Remote\\long\\file\\name");
		statusPanelEast.add(statusLabelEast);
	}

}

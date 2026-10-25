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
 * ~version~V000.01.40-V000.01.39-V000.01.27-V000.01.26-V000.01.V21_5-V000.01.21_3-V000.01.18-V000.01.17-V000.01.05-V000.01.00-
 */
package us.bringardner.parley.files.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import us.bringardner.parley.brand.ParleyBrand;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.swing.FileSourceChooserDialog;
import us.bringardner.parley.files.FileSourceFilter;
import us.bringardner.parley.files.viewer.IRegistry.CommandType;
import us.bringardner.parley.files.viewer.IRegistry.RegData;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import us.bringardner.swing.gradient.GradientButton;
import us.bringardner.swing.gradient.GradientPanel;



public class OptionSelectDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	public enum Response {Yes,No}

	public static final ImageIcon warningIcon = new ImageIcon(OptionSelectDialog.class.getResource("/us/bringardner/parley/files/viewer/icons/Warning100x100.png"));
	public static final ImageIcon ErrorIcon   = new ImageIcon(OptionSelectDialog.class.getResource("/us/bringardner/parley/files/viewer/icons/Error100x100.png"));
	public static final ImageIcon MessageIcon   = new ImageIcon(OptionSelectDialog.class.getResource("/us/bringardner/parley/files/viewer/icons/Globe100x100.png"));



	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		IRegistry reg = IRegistry.getRegistry();
		List<RegData> llist = reg.getRegisteredHandler(".txt", CommandType.Any);
		String res = showDialog(llist);
		System.out.println("res="+res);
		System.exit(0);
		
	}

	private final JPanel contentPanel = new GradientPanel(3);
	private Icon icon = MessageIcon;	
	/** null: the current theme's */
	Color startColor;
	Color endColor;
	
	private boolean canceled=false;
	private String message="";
	private String yesButtonText="Yes";
	private String noButtonText="No";
	private GradientButton yesButton;
	private GradientButton noButton;
	private List<IRegistry.RegData> list;
	private JTable table;
	private JTextField textField;
	
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
	public Icon getIcon() {
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

	public void setShowYesButton(boolean b) {
		yesButton.setVisible(b);
	}
	
	public void setShowNoButton(boolean b) {
		noButton.setVisible(b);
	}
	
	/**
	 * Create the dialog.
	 */
	public OptionSelectDialog() {
		setIconImages(ParleyBrand.appIcons("files"));
		setBounds(100, 100, 543, 326);

		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		if( startColor != null ) {
			contentPanel.setBackground(startColor);
			contentPanel.setForeground(endColor);
		}

		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = FileSourceViewerBase.createScrollBar();
		contentPanel.add(scrollPane);
		
		table = FileSourceViewerBase.createTable();
		
		table.setModel(new DefaultTableModel() {
			private static final long serialVersionUID = 1L;

			@Override
			public int getColumnCount() {
				//System.out.println("get col cnt");
				return 2;
			}
			
			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return columnIndex == 0 ? Icon.class:String.class;
			}
			
			@Override
			public int getRowCount() {
				int ret =  list == null ? 0: list.size();
				//System.out.println("get row cnt="+ret);
				return ret;
			}
			
			@Override
			public String getColumnName(int column) {
				return "";
			}
			@Override
			public Object getValueAt(int row, int column) {
				//System.out.println("Enter getValue row="+row+" col="+column);
				if( column==0) {
					try {
						Icon img = list.get(row)
								.getIcon(16, 16);
						if( img != null ) {
						icon = img;
						//System.out.println("Exit getValue row="+row+" col="+column+" icon="+icon);
						return icon;
						} else {
							return null;
						}
					} catch (IOException e) {
						//System.out.println(e);
						return null;
					}
				}
				String ret = list.get(row).name;
				//System.out.println("Exit getValue row="+row+" col="+column+" name="+ret);
				return ret;				
			}
		});
		table.getColumnModel().getColumn(0).setMaxWidth(20);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setViewportView(table);
		
		GradientPanel panel = new GradientPanel(GradientPanel.DIAGONAL_RIGHT);
		if( startColor != null ) {
			panel.setBackground(startColor);
			panel.setForeground(endColor);
		}

		contentPanel.add(panel, BorderLayout.SOUTH);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JLabel lblNewLabel = new JLabel("Other");
		panel.add(lblNewLabel);
		
		textField = new JTextField();
		panel.add(textField);
		textField.setColumns(20);
		
		GradientButton browseButton = new GradientButton("OK");
		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FileSourceChooserDialog fc = new FileSourceChooserDialog();
				//fc.setFileSelectionMode(FileSourceChooserDialog.FILES_ONLY);
				fc.addChoosableFileFilter(new FileSourceFilter() {
					
					@Override
					public String getDescription() {
						return "Test";
					}
					
					@Override
					public boolean accept(FileSource f) {
						boolean ret = false;
						try {
							if( f.isDirectory()) {
								ret = f.getName().endsWith(".app");
							} else {
								ret = f.canExecute();
							}
						} catch (IOException e) {
						}
						return ret;
					}
				});
				fc.setFileFilter(new FileSourceFilter() {
					
					@Override
					public String getDescription() {
						
						return "Executable files";
					}
					
					@Override
					public boolean accept(FileSource f) {
						boolean ret = false;
						try {
							if( f.isDirectory()) {
								ret = f.getName().endsWith(".app");
							} else {
								ret = f.canExecute();
							}
						} catch (IOException e) {
						}
						return ret;
					}
				});
				if( fc.showOpenDialog(contentPanel)== FileSourceChooserDialog.APPROVE_OPTION) {
					FileSource f = fc.getSelectedFile();
					textField.setText(f.getAbsolutePath());
				}
			}
		});
		browseButton.setPreferredSize(new Dimension(50, 30));
		browseButton.setToolTipText("Browse");
		browseButton.setText("...");
		panel.add(browseButton);

		{
			JPanel buttonPanel = new GradientPanel(GradientPanel.DIAGONAL_RIGHT);
			buttonPanel.setOpaque(false);
			if( startColor != null ) {
				buttonPanel.setBackground(startColor);
				buttonPanel.setForeground(endColor);
			}
			buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			{
				yesButton = new GradientButton("OK");
				yesButton.setText(yesButtonText);
				yesButton.addActionListener((e)->{
					canceled = false;
					dispose();
				});
				buttonPanel.add(yesButton);
			}
			{
				noButton = new GradientButton("Cancel");
				noButton.setText(noButtonText);
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


		
	public static String showDialog(List<RegData> options) {
		OptionSelectDialog d = new OptionSelectDialog();
		d.list = options;
		((DefaultTableModel)d.table.getModel()).fireTableDataChanged();
		d.table.setRowSelectionInterval(0, 0);
		d.setAlwaysOnTop(true);
		d.setMessage("");
		d.setTitle("");
		d.setNoButtonText("Cancel");
		d.setYesButtonText("Okay");
		d.setModal(true);
		d.setVisible(true);
		if( !d.canceled) {
			int idx = d.table.getSelectedRow();
			if( idx >=0 && idx < options.size()) {
				return options.get(idx).path;
			}
			String tmp = d.textField.getText().trim();
			if( !tmp.isEmpty()) {
				return tmp;
			}
		}
		
		return null;
	}
}

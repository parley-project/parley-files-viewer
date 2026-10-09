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
 * ~version~V000.01.20-V000.01.13-V000.01.08-V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreePath;

import us.bringardner.swing.datetime.DateTimeCombo;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.fileproxy.FileProxyFactory;
import us.bringardner.parley.files.viewer.SearchRequest.Operator;
import us.bringardner.swing.GradientButton;
import us.bringardner.swing.GradientPanel;

public class SearchFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JPanel byNamePanel;
	private JPanel bySizePanel;
	private JPanel byDatePanel;
	private JTextField nameField;
	private JCheckBox chckbxCaseSensative;
	private JComboBox<String> dateOperatorComboBox;
	private JComboBox<String> sizeComboBox;
	private JCheckBox chckbxSearchByName;
	private JCheckBox chckbxSeachBySize;
	private JCheckBox chckbxSearchByDate;
	private GradientButton btnSearchNow;
	private GradientButton btnAdvanced;
	//private FileSource dir;
	private ActivityLayer activityLayer;
	private DateTimeCombo startDateCombo;
	private DateTimeCombo endDateCombo;
	private JTable table;
	private SearchFileTableModel model;
	private JScrollPane scrollPane;
	private SizeEditPanel size1EditPane;
	private SizeEditPanel size2EditPane;
	private JCheckBox chckbxSearchByType;
	private JComboBox<String> typeComboBox;
	private JPopupMenu popup;
	private FileSourceViewer browser;
	private FileSource dir;
	private Component horizontalStrut;
	private GradientPanel detailPanel;
	private JMenu openMenu;

	public SearchFrame(FileSource dir, FileSourceViewer browser) {
		this();
		this.dir = dir;
		this.browser = browser;
		setTitle("Searching "+dir.getAbsolutePath());
		bySizePanel.setVisible(false);
		byDatePanel.setVisible(false);
		nameField.requestFocusInWindow();
		activityLayer = new ActivityLayer();
		startDateCombo.setdate(new Date());
		endDateCombo.setdate(new Date());
		table.setDefaultRenderer(Date.class, new FileTableCellRenderer());
		table.setDefaultRenderer(Long.class, new FileTableCellRenderer());
		table.setDefaultRenderer(String.class, new FileTableCellRenderer());
		//getContentPane().add( activityLayer);
		setGlassPane(activityLayer);
//		if(!browser.isRemote()) {
			typeComboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"File", "Dir"}));
//		}

	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					FileSourceViewer browser = new FileSourceViewer();
					SearchFrame frame = new SearchFrame(FileProxyFactory.getDefaultFactory().createFileSource("/Volumes/Data/eclipse-git/BjlFileSystemViewer/TestFiles"),browser);
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
	public SearchFrame() {
		setTitle("Search");
		setBounds(100, 100, 1086, 527);
		setLocationRelativeTo(null);
		setIconImage(Toolkit.getDefaultToolkit().getImage(SearchFrame.class.getResource(FileSourceViewer.ICON_LOCATION+"/folder.png")));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		contentPane = new GradientPanel(GradientPanel.DIAGONAL_RIGHT);
		contentPane.setBackground(FileSourceViewer.GradiantPanelStartColor);
		contentPane.setForeground(FileSourceViewer.GradiantPanelEndColor);

		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		detailPanel = new GradientPanel(GradientPanel.DIAGONAL_RIGHT);
		contentPane.add(detailPanel, BorderLayout.NORTH);
		detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));

		byNamePanel =    new GradientPanel(GradientPanel.DIAGONAL_RIGHT);
		byNamePanel.setBackground(FileSourceViewer.GradiantPanelStartColor);
		byNamePanel.setForeground(FileSourceViewer.GradiantPanelEndColor);

		detailPanel.add(byNamePanel);
		byNamePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		chckbxSearchByName = new JCheckBox("Search by name");
		chckbxSearchByName.setSelected(true);
		byNamePanel.add(chckbxSearchByName);

		nameField = new JTextField();
		byNamePanel.add(nameField);
		nameField.setColumns(45);

		chckbxCaseSensative = new JCheckBox("Case Sensative");
		byNamePanel.add(chckbxCaseSensative);

		btnSearchNow = new GradientButton("Search Now");
		btnSearchNow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				model.clear();
				if( !chckbxSearchByName.isSelected() 
						&& !chckbxSeachBySize.isSelected()
						&& !chckbxSearchByDate.isSelected()) {
					return;
				}
				//	System.out.println("Search "+dir+" exists = "+dir.exists());
				if(activityLayer != null ) {
					if( activityLayer.isRunning()) {
						activityLayer.stop();						
					} else {
						//model.clear();
						final SearchRequest sr = new SearchRequest();
						sr.model = model;
						sr.activity = activityLayer;
						sr.caseSensative = chckbxCaseSensative.isSelected();
						if( chckbxSearchByName.isSelected()) {
							sr.name = nameField.getText().trim();
							if(sr.name.length() == 0) {
								sr.name = null;
							}
						}



						if(chckbxSearchByDate.isSelected()) {
							sr.date1 = startDateCombo.getDate().getTime();
							String val = dateOperatorComboBox.getSelectedItem().toString();
							if("After".equals(val)) {
								sr.dateOp = Operator.GREATER_THAN;
							} else if("Before".equals(val)) {
								sr.dateOp = Operator.LESS_THAN;
							}  else if("Same Day".equals(val)) {
								sr.dateOp = Operator.BETWEEN;
								Calendar cal = Calendar.getInstance();
								cal.setTimeInMillis(sr.date1);
								cal.set(Calendar.HOUR_OF_DAY, 0);
								cal.set(Calendar.MINUTE, 0);
								cal.set(Calendar.SECOND, 0);
								cal.set(Calendar.MILLISECOND, 0);
								sr.date1 = cal.getTimeInMillis();
								cal.add(Calendar.DAY_OF_MONTH, 1);
								sr.date2 = cal.getTimeInMillis();
								
							} else if("Between".equals(val)) {
								sr.dateOp = Operator.BETWEEN;
								sr.date2 = endDateCombo.getDate().getTime();
							}
						}

						sr.size1 = -1;
						sr.size2 = -1;
						if(chckbxSeachBySize.isSelected()) {
							sr.size1 = size1EditPane.getValue();

							String val = sizeComboBox.getSelectedItem().toString();
							if(">".equals(val)) {
								sr.sizeOp = Operator.GREATER_THAN;
							} else if("<".equals(val)) {
								sr.sizeOp = Operator.LESS_THAN;
							}  else if("=".equals(val)) {
								sr.sizeOp = Operator.EQUAL;
							} else if("Between".equals(val)) {
								sr.sizeOp = Operator.BETWEEN;
								sr.size2 = size2EditPane.getValue();
							}
						}
						if( chckbxSearchByType.isSelected() ) {
							sr.type = SearchRequest.Type.valueOf(typeComboBox.getSelectedItem().toString().toUpperCase());
						}

						if( sr.name == null 
								&& !chckbxSearchByType.isSelected()
								&& !chckbxSeachBySize.isSelected()
								&& !chckbxSearchByDate.isSelected()) {
							return;
						}
						btnSearchNow.setText("Cancel");
						activityLayer.start();
						sr.postProcessor = new Runnable() {

							@Override
							public void run() {
								SwingUtilities.invokeLater(new Runnable() {

									@Override
									public void run() {
										btnSearchNow.setText("Search Now");	
										sr.activity.stop();
									}
								});								
							}							
						};

						new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									listFiles(sr);
								} catch (IOException e) {
									JOptionPane.showMessageDialog(table, "Error "+e, "", JOptionPane.ERROR_MESSAGE);
								}

							}

							private void listFiles(SearchRequest sr) throws IOException{
								search(sr,dir);
								sr.postProcessor.run();
							}

						
						}).start();

					}
				}
			}
		});

		byNamePanel.add(btnSearchNow);

		btnAdvanced = new GradientButton("Advanced");
		btnAdvanced.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				bySizePanel.setVisible(!bySizePanel.isVisible());
				byDatePanel.setVisible(!byDatePanel.isVisible());
			}
		});
		byNamePanel.add(btnAdvanced);

		bySizePanel = new GradientPanel(GradientPanel.DIAGONAL_RIGHT);
		bySizePanel.setBackground(FileSourceViewer.GradiantPanelStartColor);
		bySizePanel.setForeground(FileSourceViewer.GradiantPanelEndColor);

		
		detailPanel.add(bySizePanel);
		bySizePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		chckbxSeachBySize = new JCheckBox("Seach by Size");
		bySizePanel.add(chckbxSeachBySize);

		size1EditPane = new SizeEditPanel();
		bySizePanel.add(size1EditPane);

		sizeComboBox = new JComboBox<String>();
		sizeComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				String val = sizeComboBox.getSelectedItem().toString();
				if(val.equals("Between")) {
					size2EditPane.setVisible(true);
				} else {
					size2EditPane.setVisible(false);
				}				
			}
		});
		sizeComboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"Between", "<", "=", ">"}));
		bySizePanel.add(sizeComboBox);

		size2EditPane = new SizeEditPanel();
		bySizePanel.add(size2EditPane);
		
		horizontalStrut = Box.createHorizontalStrut(60);
		bySizePanel.add(horizontalStrut);
		
				chckbxSearchByType = new JCheckBox("Search by Type");
				bySizePanel.add(chckbxSearchByType);
		
				typeComboBox = new JComboBox<String>();
				bySizePanel.add(typeComboBox);
				typeComboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"File", "Dir"}));

		byDatePanel = new GradientPanel(GradientPanel.DIAGONAL_RIGHT);
		byDatePanel.setBackground(FileSourceViewer.GradiantPanelStartColor);
		byDatePanel.setForeground(FileSourceViewer.GradiantPanelEndColor);
		detailPanel.add(byDatePanel);
		byDatePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));

		chckbxSearchByDate = new JCheckBox("Search by Date");
		byDatePanel.add(chckbxSearchByDate);

		startDateCombo = new DateTimeCombo();
		startDateCombo.setLabel("Start Date");
		byDatePanel.add(startDateCombo);

		dateOperatorComboBox = new JComboBox<String>();
		dateOperatorComboBox.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				String val = dateOperatorComboBox.getSelectedItem().toString();
				if(val.equals("Between")) {
					endDateCombo.setVisible(true);
				} else {
					endDateCombo.setVisible(false);
				}				
			}
		});
		dateOperatorComboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"Between", "Before", "Same Day", "After"}));
		byDatePanel.add(dateOperatorComboBox);

		endDateCombo = new DateTimeCombo();
		endDateCombo.setLabel("End Date");
		byDatePanel.add(endDateCombo);

		scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);

		table = new JTable();
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if( evt.getButton() == 3) {
					// pop-up
					int [] rows = table.getSelectedRows();
					int touched = table.rowAtPoint(evt.getPoint());
					boolean all = false;
					for(int idx=0; idx<rows.length; idx++) {
						if( rows[idx] == touched) {
							all = true;
							break;
						}
					}
					if( !all ) {
						table.setRowSelectionInterval(touched, touched);
						Thread.yield();
					}
					ArrayList<FileSource> list = getSelectedFromTable();
					if( list !=null && list.size()>0) {
						for(FileSource file : list) {
							File local = browser.getLocalFileDownloadIfNeeded(file);
							
							
						}
						
					}
					//openMenu.removeAll();
					JMenuItem item = new JMenuItem("open");
					
					
					item.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent evt) {
							openFiles(getSelectedFromTable());
						}
					});

					openMenu.add(item);
					
					popup.show(evt.getComponent(),evt.getX(),evt.getY());
				} else if(evt.getClickCount() == 2) {
					// open
					ArrayList<FileSource> list = getSelectedFromTable();
					openFiles(list);					
				}				
			}
		});
		table.setAutoCreateRowSorter(true);
		model = new SearchFileTableModel();
		table.setModel(model);
		scrollPane.setViewportView(table);

		JPanel statusPanel = new JPanel();
		contentPane.add(statusPanel, BorderLayout.SOUTH);
		popup = new JPopupMenu("Options");
		openMenu = new JMenu("Open With");
		
		popup.add(openMenu);
		
		JMenuItem item = new JMenuItem("Open Containing Folder");
		item.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent evt) {
				ArrayList<FileSource> list = getSelectedFromTable();
				ArrayList<FileSource> dirs = new ArrayList<FileSource>();
				try {
					for (FileSource f : list) {
						dirs.add(f.getParentFile());						

					}
					openFiles(dirs);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(SearchFrame.this, e.getMessage(), "", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		popup.add(item);

	}

	protected void openFiles(ArrayList<FileSource> list) {
		if(browser != null) {
			for (FileSource f : list) {
				try {
					if( f.isDirectory()) {
						TreePath tp = browser.buildTreePath(f);
						browser.setSelectionPath(tp);
						JFrame frame = browser.getFrame();
						frame.toFront();
						frame.repaint();
					} else {
						browser.open(f);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public ArrayList<FileSource> getSelectedFromTable() {
		ArrayList<FileSource> files = new ArrayList<FileSource>();
		for (int row : table.getSelectedRows()) {
			files.add(model.getFile(table.convertRowIndexToModel(row)));
		}
		return files;
	}

	private void search(SearchRequest sr, FileSource dir) {
		if( !sr.isCanceled()) {
			try {
				FileSource[] list = dir.listFiles();
				if( list != null && !sr.isCanceled()) {

					for (FileSource f : list) {
						if(sr.isMatch(f)) {
							sr.model.add(f);
						}
					}
					for (FileSource f : list) {
						if(!sr.isCanceled() && f.isDirectory()) {
							search(sr,f);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				sr.activity.stop();
			}
		}
 		
	}
	
}

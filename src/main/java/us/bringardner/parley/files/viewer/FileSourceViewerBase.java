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
 * ~version~V000.01.48-V000.01.44-V000.01.43-V000.01.39-V000.01.36-V000.01.35-V000.01.34-V000.01.32-V000.01.31-V000.01.28-V000.01.26-V000.01.21-V000.01.17-V000.01.16-V000.01.05-V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import us.bringardner.parley.brand.ParleyBrand;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import us.bringardner.swing.gradient.GradientPanel;



public class FileSourceViewerBase {
	
	private JFrame frame;
	private JTree tree;
	private JTable table;
	private JScrollPane tableScrollPane;
	private JMenuBar menuBar;
	private JMenuItem mntmExit;
	private JMenuItem mntmTerminal;
	private JMenuItem mntmAbout;
	private JMenuItem mntmRemoteBrowser;
	private JMenuItem mntmLocalBrowser;
	private JMenuItem mntmClose;
	private JMenu mnNew;
	private JPanel statusPanel;
	private JLabel lblNewLabel;
	private JMenuItem mntmSearch;
	private JSplitPane splitPane;
	private JScrollPane treeScrollPane;
	private JCheckBoxMenuItem showHiddenFilesCheckItem;
	private JPanel rowHeaderPanel;
	private JCheckBoxMenuItem showExtentionsCheckItem;
	private GradientPanel contentPane;
	private JMenuItem mntmFont;
	private JPanel northPanel;
	private JButton testButton;
	private CopyProgressPanel copyProgressPanel;
	private JSeparator separator;
	private JSeparator separator_1;
	private JMenuItem mntmStartLocation;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					FileSourceViewerBase window = new FileSourceViewerBase();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static JScrollPane createScrollBar (){
		JScrollPane scrollPane = new JScrollPane();		
		
		
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		scrollPane.setRowHeaderView(new GradientPanel(GradientPanel.DIAGONAL_RIGHT));
		

		return scrollPane;
	}
	
	public static JTable createTable() {
		JTable table = new JTable();
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
        table.setOpaque(false);
        table.setBackground(new Color(255, 255, 255, 0));
        return table;
	}
	
	
	public CopyProgressPanel getCopyProgressPanel() {
		return copyProgressPanel;
	}
	
	public JMenuItem getMntmSearch() {
		return mntmSearch;
	}

	public JMenuItem getMntmStartLocation() {
		return mntmStartLocation;
	}

	public JMenuItem getMntmFont() {
		return mntmFont;
	}



	public JLabel getLblNewLabel() {
		return lblNewLabel;
	}


	public JPanel getStatusPanel() {
		return statusPanel;
	}



	public JMenu getMnNew() {
		return mnNew;
	}


	public JMenuItem getMntmClose() {
		return mntmClose;
	}


	public JMenuItem getMntmExit() {
		return mntmExit;
	}


	public JMenuItem getMntmAbout() {
		return mntmAbout;
	}

	public JMenuItem getMntmTerminal() {
		return mntmTerminal;
	}

	public JPanel getNorthPanel() {
		return northPanel;
	}
	
	public JCheckBoxMenuItem getShowExtention() {
		return showExtentionsCheckItem;
	}

	public JCheckBoxMenuItem getShowHidden() {
		return showHiddenFilesCheckItem;
	}

	public JMenuItem getMntmRemoteBrowser() {
		return mntmRemoteBrowser;
	}


	public JMenuItem getMntmLocalBrowser() {
		return mntmLocalBrowser;
	}


	public JFrame getFrame() {
		return frame;
	}

	public JButton getTestButton() {
		return testButton;
	}
	
	public JTree getTree() {
		return tree;
	}

	public JTable getTable() {
		return table;
	}

	public JMenuBar getMenuBar() {
		return menuBar;
	}


	public JSplitPane getSplitPane() {
		return splitPane;
	}



	public JScrollPane getTableScrollPane() {
		return tableScrollPane;
	}



	public JScrollPane getTreeScrollPane() {
		return treeScrollPane;
	}



	public void show() {
		frame.setVisible(true);
	}
	
	/**
	 * Create the application.
	 */
	public FileSourceViewerBase() {
		initialize();
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setIconImages(ParleyBrand.appIcons("files"));
		frame.setBounds(100, 100, 1252, 669);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		contentPane = new GradientPanel(GradientPanel.DIAGONAL_RIGHT);
		//contentPane.setBackground(Color.ORANGE);
		frame.setContentPane(contentPane);
		
		
		splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.3);
		contentPane.add(splitPane, BorderLayout.CENTER);
		
		treeScrollPane = new JScrollPane();
		treeScrollPane.setMinimumSize(new Dimension(175, 24));
		splitPane.setLeftComponent(treeScrollPane);
		
		tree = new JTree();
		tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
		tree.setPreferredSize(new Dimension(200, 64));
		treeScrollPane.setViewportView(tree);
		tree.setShowsRootHandles(true);

		treeScrollPane.setOpaque(false);
		treeScrollPane.setBackground(new Color(255, 255, 255, 0));
		treeScrollPane.getViewport().setOpaque(false);
		tree.setOpaque(false);
		tree.setBackground(new Color(255, 255, 255, 0));



		
		tableScrollPane = new JScrollPane();		
		splitPane.setRightComponent(tableScrollPane);
		
		table = new JTable();
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		tableScrollPane.setViewportView(table);
		
		tableScrollPane.setOpaque(false);
		tableScrollPane.getViewport().setOpaque(false);
        table.setOpaque(false);
        table.setBackground(new Color(255, 255, 255, 0));

		rowHeaderPanel = new GradientPanel(GradientPanel.DIAGONAL_RIGHT);
		tableScrollPane.setRowHeaderView(rowHeaderPanel);
		
		table.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				System.out.println("code="+e.getKeyCode()+" x"+Integer.toHexString(e.getKeyCode())
									+" isActionKey="+e.isActionKey()
									+" isAltDown="+e.isAltDown()
									+" isAltGraphDown="+e.isAltGraphDown()
									+" isControlDown="+e.isControlDown()
									+" isMetaDown="+e.isMetaDown()
									+" isShiftDown="+e.isShiftDown()
						);
				
			}
		});
		
		splitPane.setOpaque(false);
		splitPane.setBackground(FileSourceViewer.TRANSPARENT);
    	treeScrollPane.setOpaque(false);
		treeScrollPane.setBackground(FileSourceViewer.TRANSPARENT);
		treeScrollPane.getViewport().setOpaque(false);
	
        tableScrollPane.setOpaque(false);
        tableScrollPane.getViewport().setOpaque(false);
        table.setOpaque(false);
        table.setBackground(FileSourceViewer.TRANSPARENT);
        
        northPanel = new JPanel();
        northPanel.setOpaque(false);
        contentPane.add(northPanel, BorderLayout.NORTH);
        northPanel.setLayout(new BorderLayout(0, 0));
        
        statusPanel = new GradientPanel(GradientPanel.DIAGONAL_RIGHT);
        northPanel.add(statusPanel);
        statusPanel.setBorder(null);
        statusPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        
        lblNewLabel = new JLabel("Capture");
        lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusPanel.add(lblNewLabel);
        
        copyProgressPanel = new CopyProgressPanel();
        copyProgressPanel.setExpanded(false);
        copyProgressPanel.setSize(1000, 100);
        northPanel.add(copyProgressPanel, BorderLayout.NORTH);
        
        JPanel panel_1 = new JPanel();
        panel_1.setOpaque(false);
        northPanel.add(panel_1, BorderLayout.WEST);
        menuBar = new JMenuBar();
        panel_1.add(menuBar, BorderLayout.WEST);
        
        JMenu fileMenu = new JMenu("Options");
        fileMenu.setIcon(null);
        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        mntmAbout = new JMenuItem("About");
        helpMenu.add(mntmAbout);
        
        mnNew = new JMenu("New");
        fileMenu.add(mnNew);
        
        mntmTerminal = new JMenuItem("Terminal");
        mnNew.add(mntmTerminal);
        
        mntmLocalBrowser = new JMenuItem("Local Viewer");
        mnNew.add(mntmLocalBrowser);
        
        mntmRemoteBrowser = new JMenuItem("Remote Session");
        mnNew.add(mntmRemoteBrowser);
        
        mntmSearch = new JMenuItem("Search");
        fileMenu.add(mntmSearch);
        
        mntmFont = new JMenuItem("Appearance...");
        fileMenu.add(mntmFont);
        
        
        mntmClose = new JMenuItem("Close");
        fileMenu.add(mntmClose);
        
        separator = new JSeparator();
        fileMenu.add(separator);
        
        mntmStartLocation = new JMenuItem("Set Start Directory");
        fileMenu.add(mntmStartLocation);
        
        separator_1 = new JSeparator();
        fileMenu.add(separator_1);
        
        mntmExit = new JMenuItem("Exit");
        fileMenu.add(mntmExit);
        
        showHiddenFilesCheckItem = new JCheckBoxMenuItem("Show hidden files");
        showHiddenFilesCheckItem.setSelected(true);
        showExtentionsCheckItem = new JCheckBoxMenuItem("Show file extentions");
        showExtentionsCheckItem.setSelected(true);
        
        
        // (the globe button and its menu of Show Hidden Files and Show File Extensions are gone: the
        // View menu has them, sharing these check items' models)

        
        JPanel panel = new JPanel();
        contentPane.add(panel, BorderLayout.SOUTH);
        
        testButton = new JButton("Test");
        panel.add(testButton);
	
	}

	
	
	public void setTitle(String title) {
		frame.setTitle(title);
	}



	
	
}

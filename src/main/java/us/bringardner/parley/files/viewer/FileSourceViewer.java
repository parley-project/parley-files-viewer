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
 * ~version~000000-V000.01.51-V000.01.50-V000.01.48-V000.01.47-V000.01.46-V000.01.45-V000.01.44-V000.01.42-V000.01.41-V000.01.39-V000.01.38-V000.01.37-V000.01.36-V000.01.35-V000.01.34-V000.01.32-V000.01.31-V000.01.30-V000.01.29-V000.01.28-V000.01.27-V000.01.26-V000.01.25-V000.01.24-V000.01.23-V000.01.22-V000.01.21_5-V000.01.21_4-V000.01.21_3-V000.01.21_2-V000.01.21_1-V000.01.21-V000.01.20-V000.01.18-V000.01.17-V000.01.16-V000.01.15-V000.01.14-V000.01.13-V000.01.12-V000.01.11-V000.01.10-V000.01.09-V000.01.07-V000.01.06-V000.01.05-V000.01.04-V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import us.bringardner.swing.gradient.Theme;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Point;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;
import java.awt.desktop.OpenFilesEvent;
import java.awt.desktop.OpenFilesHandler;
import java.awt.desktop.OpenURIEvent;
import java.awt.desktop.OpenURIHandler;
import java.awt.desktop.PreferencesEvent;
import java.awt.desktop.PreferencesHandler;
import java.awt.desktop.PrintFilesEvent;
import java.awt.desktop.PrintFilesHandler;
import java.awt.desktop.QuitEvent;
import java.awt.desktop.QuitHandler;
import java.awt.desktop.QuitResponse;
import java.awt.desktop.QuitStrategy;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.net.ssl.TrustManager;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import us.bringardner.parley.brand.ParleyBrand;
import us.bringardner.parley.core.SecureBaseObject;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceCopy;
import us.bringardner.parley.files.swing.FileSourceChooserDialog;
import us.bringardner.parley.files.swing.FactoryPropertiesDialog;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.swing.ProgressMonitorProgress;
import us.bringardner.parley.files.swing.FileSourceTransferable;
import us.bringardner.parley.files.fileproxy.FileProxy;
import us.bringardner.parley.files.fileproxy.FileProxyFactory;
import us.bringardner.parley.files.memory.MemoryFileSourceFactory;
import us.bringardner.parley.files.viewer.IRegistry.CommandType;
import us.bringardner.parley.files.viewer.IRegistry.RegData;
import us.bringardner.parley.net.client.DynamicTrustManager;
import us.bringardner.parley.net.client.VisualCertificateValidator;
import us.bringardner.fsh.Console;

import us.bringardner.swing.gradient.GradientColors;
import us.bringardner.swing.gradient.GradientLook;
import us.bringardner.swing.dialog.MessageDialog;
import us.bringardner.swing.dialog.MessageDialog.Response;
import us.bringardner.swing.dialog.SettingsDialog;



public class FileSourceViewer extends FileSourceViewerBase implements ClipboardOwner {

	private static Preferences pref = Preferences.userNodeForPackage(FileSourceViewer.class);

	public static final String PREF_SHOW_HIDDEN = "ShowHidden";
	/** A file from another machine larger than this many MB is asked about before it is downloaded; 0 never asks. */
	public static final String PREF_LARGE_FILE_MB = "LargeFileMb";
	public static final int DEFAULT_LARGE_FILE_MB = 50;
	public static final String PREF_SHOW_EXTENTION = "ShowExtention";
	public static final String PREF_START_LOCATION = "StartLocation";

	public static final String ICON_LOCATION = "/us/bringardner/parley/files/viewer/icons";
	// the same colors the swing-widgets gradient widgets use
	public static final Color TRANSPARENT   = GradientColors.TRANSPARENT;




	protected static final String PREF_COL_WIDTHS = "ColumnWidths";

	private static Map<Integer,FileSourceViewer> runningViewers = new HashMap<>();
	private static Map<Integer,FileSourceFactory> sessions = new HashMap<Integer,FileSourceFactory>();
	private static Map<FileSourceFactory,Integer> sessionMap = new HashMap<FileSourceFactory,Integer>();

	private static int currentId = 0;
	private static File tmpdir;

	private static boolean backupAvailible; 
	private boolean showHidden = false;
	private boolean showExtention=true;
	private Set<TreePath> expandedSet = new HashSet<>();
	// set some reasonable defaults
	private int columnWidths[] = {40, 409, 277, 54, 105};

	public DefaultTreeModel model;

	private boolean inSetTableModel=false;


	static {

		try {
			Class.forName("us.bringardner.parley.files.Backup");
			backupAvailible = true;
		} catch (Exception e) {
		}

	}

	static class ActionContext {
		List<FileSource> files;
		Component source;

		public ActionContext(Component comp, List<FileSource> list) {
			source = comp;
			files = list;
		}

	}

	static class CustomTableHeaderRenderer extends DefaultTableCellRenderer  implements TableCellRenderer{
		private static final long serialVersionUID = 1L;
		final private Icon ascIcon   = UIManager.getIcon("Table.ascendingSortIcon");
		final private Icon descIcon = UIManager.getIcon("Table.descendingSortIcon");
		TableCellRenderer iTableCellRenderer = null;

		public CustomTableHeaderRenderer(TableCellRenderer tableCellRenderer)
		{
			iTableCellRenderer = tableCellRenderer;
		}           


		public Component getTableCellRendererComponent(JTable table,  Object value, boolean isSelected, boolean hasFocus, int row,  int column) {
			JLabel label = (JLabel) iTableCellRenderer.getTableCellRendererComponent( table,   value,  isSelected, hasFocus,  row,   column) ;
			List<? extends SortKey> sortKeys = table.getRowSorter().getSortKeys();
			label.setIcon(null);
			for (SortKey sortKey : sortKeys) {
				if (sortKey.getColumn() == table.convertColumnIndexToModel(column)){
					SortOrder o = sortKey.getSortOrder();
					label.setIcon(o == SortOrder.ASCENDING ? ascIcon : descIcon);
					break;
				}
			}
			System.out.println("return label");
			return label;
		}
	}

	FileSourceTreeNode searchNode(FileSourceTreeNode root, String searchText) {
		if (root.getUserObject().toString().equals(searchText)) {
			return root;
		}
		for (int i = 0; i < root.getChildCount(); i++) {
			FileSourceTreeNode foundNode = searchNode((FileSourceTreeNode) root.getChildAt(i), searchText);
			if (foundNode != null) {
				return foundNode;
			}
		}
		return null;
	}

	private static int nodeId =0;
	private static synchronized int nextNodId() {
		return ++nodeId;
	}

	class FileSourceTreeNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 1L;

		FileSource dir;
		String shortName;
		String displayName;
		int id = nextNodId();


		public FileSourceTreeNode(FileSource dir) {
			super(dir, true);
			this.dir = dir;
			shortName = displayName = dir.getName();
			int idx = shortName.lastIndexOf(".");
			if( idx > 0 ) {
				shortName = shortName.substring(0,idx);
			}
			try {
				FileSource link = dir.getLinkedTo();
				if( link != null ) {
					displayName += " -> "+link.getCanonicalPath();
					shortName += " -> "+link.getCanonicalPath();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			//displayName+=id;
		}

		@Override
		public boolean equals(Object obj) {			
			return toString().equals(obj.toString());
		}

		@Override
		public String toString() {				
			return showExtention?displayName:shortName;
		}


		/**
		 * The default implementation will not load children.  This is only used to find a node in the tree 
		 * @return the children after loaded if needed
		 */
		public Vector<TreeNode> getChildren() {
			if( children == null) {
				if( isTraversable() ) {
					try {
						FileSource[] kids = dir.listFiles();
						if( kids != null) {
							Arrays.sort(kids, new Comparator<FileSource>() {
								@Override
								public int compare(FileSource o1, FileSource o2) {
									return o1.getName().compareToIgnoreCase(o2.getName());
								}
							});

							for(FileSource kid : kids) {
								if( kid.isDirectory()) {
									boolean hide = kid.isHidden() || kid.getName().startsWith(".");
									if( showHidden || !hide) {
										add(new FileSourceTreeNode(kid));
									}
								}
							}
						}
					} catch (IOException e) {
						showError("", e);
					}
				}
			}
			return children;
		}

		@Override
		public boolean isLeaf() {
			//  Only directories are in the tree and none are leafs 
			return false;
		}

		private boolean isTraversable() {
			boolean ret = false;
			if( dir != null ) {
				try {
					if((ret=dir.canRead()) ) {
						FileSource link = dir.getLinkedTo();
						if( link != null) {
							ret = !link.isChildOfMine(dir);				
						}
					}
				} catch (IOException e) {
				}
			}
			return ret;
		}


		public FileSource getDir() {
			return dir;
		}

		public void reset() {
			try {

				List<String> names = new ArrayList<>();
				List<FileSourceTreeNode> removeNodes = new ArrayList<>();
				FileSource[] kids = dir.listFiles();
				for(FileSource kid : kids) {
					if( kid.isDirectory()) {
						FileSourceTreeNode node = findNode(kid);
						if( node == null ) {
							addTreeNode(kid);
						} else {
							node.reset();
						}
					}
				}
				if( children != null ) {
					for(int idx=0,sz=children.size(); idx < sz; idx++) {
						TreeNode kid = children.get(idx);
						if (kid instanceof FileSourceTreeNode) {
							FileSourceTreeNode node = (FileSourceTreeNode) kid;
							if(!node.dir.exists() 
									|| 
									!node.dir.isDirectory() 
									|| 
									!dir.isChildOfMine(node.dir)
									) {
								removeNodes.add(node);
							} else {
								names.add(node.dir.getAbsolutePath());
							}
						}
					}
				}				

			} catch (IOException e) {

				showError("Node Reset", e);
			}


		}
	}

	public class RootFileSourceTreeLoadingWorking extends SwingWorker<Object, Object> {
		private final FileSourceTreeNode wait;
		private List<FileSourceTreeNode> children= new ArrayList<>();
		private FileSource root1;
		private Component pane;


		RootFileSourceTreeLoadingWorking() throws IOException {
			wait = new FileSourceTreeNode(factory.createFileSource("Loading ..."));
			DefaultTreeModel model = new DefaultTreeModel(wait);
			getTree().setModel(model);	

			pane = getFrame().getGlassPane();
			pane.setVisible(true);

			pane.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
			pane.setVisible( true );

		}

		@Override
		protected Object doInBackground() throws Exception {
			try {

				FileSource[] dirs = factory.listRoots();

				root1 = dirs.length==1? dirs[0]:new RootFile(factory);
				if( !root1.canRead()) {
					root1 =  factory.getCurrentDirectory();
				}
				FileSource [] kids =  root1.listFiles();
				if( kids !=null ) {
					Arrays.sort(kids, new Comparator<FileSource>() {
						@Override
						public int compare(FileSource o1, FileSource o2) {
							return o1.getName().compareToIgnoreCase(o2.getName());
						}
					});
					for(FileSource file : kids) {
						if( file.isDirectory()) {
							boolean hide = file.isHidden() || file.getName().startsWith(".");

							if( showHidden || !hide) {
								children.add(new FileSourceTreeNode(file));
							}

						}
					}
				}
			} catch (Exception e) {
				System.out.println("e="+e);
			}
			FileSourceTreeNode rootNode = new FileSourceTreeNode(root1);

			for(FileSourceTreeNode node : children) {
				rootNode.add(node);
			}
			model = new DefaultTreeModel(rootNode);
			return root1;
		}



		@Override
		protected void done() {
			JTree tree = getTree();
			tree.setModel(model);
			TreePath path = new TreePath(model.getRoot());			
			tree.setSelectionPath(path);				
			setTableModel(root1);
			setStatus(root1);
			pane.setVisible(false);

			String prefId = PREF_START_LOCATION+"_"+factory.getTypeId();			
			String tmp = pref.get(prefId, null);
			if( tmp == null ) {
				if (factory instanceof FileProxyFactory) {
					tmp = System.getProperty("user.home");					
				}
			}

			if( tmp != null ) {
				try {
					FileSource tmpDir = factory.createFileSource(tmp);
					if( tmpDir.exists()) {
						final FileSource homeDir = tmpDir;
						SwingUtilities.invokeLater(()->{
							try {
								factory.setCurrentDirectory(homeDir);
								TreePath path2 = buildTreePath(homeDir);			
								tree.setSelectionPath(path2);					
							} catch (IOException e) {
								showError("Error setting home directory", e);
							}
						});
					}
				}catch (Exception e) {
					// Ignore 
				}
			}

		}
	}

	public class FileSourceTreeLoadingWorking extends SwingWorker<Object, Object> {
		private final FileSourceTreeNode parent;
		List<FileSourceTreeNode> children;
		private TreePath path;
		private JTree tree;
		private Component pane;
		private ProgressMonitor progress;

		// Called in AWT Event Thread
		FileSourceTreeLoadingWorking( FileSourceTreeNode lastNode, TreePath path, JTree tree) {
			this.parent = lastNode;
			this.path = path;
			this.tree = tree;

			pane = getFrame().getGlassPane();
			pane.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
			pane.setVisible( true );
		}

		// called in it's own thread, contains long taking action
		protected Object doInBackground() throws Exception {

			children = new ArrayList<FileSourceTreeNode>();

			try {
				FileSource[] kids = parent.dir.listFiles(new ProgressMonitorProgress(progress));
				// Create nodes here but do NOT insert into tree model now
				for (FileSource f : kids) {
					if(f.isDirectory()) {
						boolean hide = f.isHidden() || f.getName().startsWith(".");
						boolean show = showHidden;

						if( show || !hide ) {
							FileSourceTreeNode k = new FileSourceTreeNode(f);
							children.add(k);	
						}
					}
				}    
				Collections.sort(children, new Comparator<FileSourceTreeNode>() {

					public int compare(FileSourceTreeNode arg0, FileSourceTreeNode arg1) {
						return arg0.getDir().getName().compareToIgnoreCase(arg1.getDir().getName());
					}
				});
			} catch ( Exception exp ) {
				return exp;
			}

			return children;
		}

		// Called in AWT Event Thread, may change tree model
		public void done() {

			parent.removeAllChildren();

			for(FileSourceTreeNode node : children) {
				parent.add(node);
			}
			((DefaultTreeModel)tree.getModel()).nodeStructureChanged(parent);
			tree.expandPath(path);
			tree.setSelectionPath(path);
			tree.scrollPathToVisible(path);				
			setTableModel(parent.dir);

			pane.setVisible( false );
		}




	}

	/**
	 * I can't get Java 9 to compile so I borrowed 
	 * this from ListSelectionModel.
	 * 
	 * @param model
	 * @return
	 */
	public int[] getSelectedIndices(ListSelectionModel model) {
		int iMin = model.getMinSelectionIndex();
		int iMax = model.getMaxSelectionIndex();

		if ((iMin < 0) || (iMax < 0)) {
			return new int[0];
		}

		int[] rvTmp = new int[1+ (iMax - iMin)];
		int n = 0;
		for(int i = iMin; i <= iMax; i++) {
			if (model.isSelectedIndex(i)) {
				rvTmp[n++] = i;
			}
		}
		int[] rv = new int[n];
		System.arraycopy(rvTmp, 0, rv, 0, n);
		return rv;
	}

	public void setTableModel(FileSource dir) {
		JTable table = getTable();
		table.setModel(new FileTableModel(dir, showHidden, showExtention));
		if( columnWidths!=null ) {
			inSetTableModel = true;
			TableColumnModel cm = table.getColumnModel();
			int [] tmp = columnWidths;
			columnWidths = null;
			for (int idx = 0; idx < tmp.length; idx++) {
				cm.getColumn(idx).setPreferredWidth(tmp[idx]);
			}
			columnWidths = tmp;
			inSetTableModel = false;
			table.updateUI();
		}
	}

	/**
	 * Copies file (a folder with everything in it) into dir under its own name and adds
	 * it to the tree. Nothing happens if the file doesn't exist or is already directly in
	 * dir; a folder can't be copied into itself or one of its own subfolders (BJL-24:
	 * the check used to be !dir.isChildOfMine(file), which skipped anything anywhere
	 * below dir and let a folder be copied into itself, which never ends).
	 */
	public void copyToCurrentDir(FileSource dir, FileSource file) throws IOException {
		if( file.isDirectory() && file.exists() && file.isChildOfMine(dir)) {
			showError("Can't copy "+file.getName()+" into itself ("+dir+")", null);
			return;
		}
		if( file.exists() && !isDirectlyIn(dir, file)) {

			FileSource newFile = dir.getChild(file.getName());
			if( file.isDirectory()) {

				if(! newFile.mkdirs()) {
					showError("Can't create directory "+newFile.getCanonicalPath(), null);
				} else {
					DefaultTreeModel model = (DefaultTreeModel)getTree().getModel();
					FileSourceTreeNode parent = searchNode((FileSourceTreeNode)model.getRoot(), dir.toString());
					parent.add(new FileSourceTreeNode(newFile));
					FileSource[] kids = file.listFiles();
					if( kids != null ) {
						for(FileSource f : kids) {
							copyToCurrentDir(newFile, f);
						}
						model.nodeStructureChanged(parent);
					}

				}
			} else {
				// both streams closed even if the copy fails, moving what suits the two ends
				FileSourceCopy.copy(file, newFile);
			}

		}
	}

	/** Is file decisions    directly in dir (same file system and same canonical folder)? */
	private static boolean isDirectlyIn(FileSource dir, FileSource file) throws IOException {
		FileSource parent = file.getParentFile();
		return parent != null
				&& parent.getFileSourceFactory().isSameFileSystem(dir.getFileSourceFactory())
				&& parent.getCanonicalPath().equals(dir.getCanonicalPath());
	}

	private class FileSourceDropTargetListener extends DropTargetAdapter {

		@SuppressWarnings("unused")
		private final DropTarget dropTarget;
		@SuppressWarnings("unused")
		private final DropTarget dropTarget2;
		@SuppressWarnings("unused")
		private final JTable table;
		@SuppressWarnings("unused")
		private JTree tree;


		public FileSourceDropTargetListener(JTable table,JTree tree) {
			this.table = table;
			this.tree = tree;
			dropTarget = new DropTarget(table, DnDConstants.ACTION_COPY, this, true, null);
			dropTarget2 = new DropTarget(tree, DnDConstants.ACTION_COPY, this, true, null);
		}


		@SuppressWarnings("unchecked")
		public void drop(DropTargetDropEvent event) {

			System.out.println("Enter drop");
			try {
				Transferable tr = event.getTransferable();
				List<FileSource> newFiles = null;
				@SuppressWarnings("unused")
				DataFlavor[] ok = tr.getTransferDataFlavors();

				if (tr.isDataFlavorSupported(FileSourceTransferable. fileSourceFlavor)) {
					event.acceptDrop(DnDConstants.ACTION_COPY);
					Object obj = tr.getTransferData(FileSourceTransferable.fileSourceFlavor);
					if (obj != null && obj instanceof List) {
						newFiles = (List<FileSource>) obj;
						event.acceptDrop(DnDConstants.ACTION_COPY);
						event.dropComplete(true);
					}					
				} else if( tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					event.acceptDrop(DnDConstants.ACTION_COPY);
					Object obj =  tr.getTransferData(DataFlavor.javaFileListFlavor);

					if (obj != null && obj instanceof List) {
						newFiles = new ArrayList<FileSource>();
						List<File> list = (List<File>) obj;
						for(File f : list) {	
							newFiles.add(FileProxyFactory.fileProxyFactory.createFileSource(f.getCanonicalPath()));
						}
						event.acceptDrop(DnDConstants.ACTION_COPY);
						event.dropComplete(true);
					}
				}

				if( newFiles !=null && newFiles.size()>0 ) {
					FileTableModel m = (FileTableModel) getTable().getModel();
					FileSource destination = m.getDir();

					Point location = event.getLocation();
					Component comp = event.getDropTargetContext().getDropTarget().getComponent();
					// this drop target is for the table
					if (comp instanceof JTable) {
						JTable t = (JTable) comp;
						int row = t.rowAtPoint(location);
						if( row < 0 ) {
							destination = m.getDir();
						} else {
							int row2 = t.convertRowIndexToModel(row);
							destination = m.getFile(row2);
						}
					} else if (comp instanceof JTree) {
						JTree t = (JTree)comp;
						TreePath path = t.getPathForLocation(location.x, location.y);
						if( path != null ) {
							Object p1 = path.getLastPathComponent();
							if( p1 != null ) {
								if (p1 instanceof FileSourceTreeNode) {
									FileSourceTreeNode ftn = (FileSourceTreeNode) p1;
									destination = ftn.getDir();
								}				
							}
						}
					}
					// move into the currentDirectory if it's not already there

					if( destination == null ) {
						DefaultTreeModel m2 = (DefaultTreeModel) getTree().getModel();
						FileSourceTreeNode node = (FileSourceTreeNode) m2.getRoot();
						destination = node.dir;
					}

					if( newFiles !=null && newFiles.size()>0 && destination !=null) {
						getCopyProgressPanel().addCopy(newFiles, destination);
					}

					return;
				}



				event.rejectDrop();
			} catch (Exception e) {

				e.printStackTrace();
				event.rejectDrop();
			}
			System.out.println("Exit drop");
		}

	}


	public static File getTempDir() {
		if( tmpdir == null ) {
			try {
				tmpdir = File.createTempFile("temp", ".tmp").getParentFile();
			} catch (IOException e) {
				// static can't use handle error but this should never happen.
				e.printStackTrace();
			}
		}

		return tmpdir;
	}

	public static synchronized int registerFrame(FileSourceViewer viewer) {
		int ret = ++currentId;
		runningViewers.put(ret,viewer);
		FileSourceFactory factory = viewer.factory;
		if( factory != null ) {			
			sessions.put(ret, factory);
			Integer cnt = sessionMap.get(factory);
			if ( cnt == null ) {
				cnt = 0;
			}
			sessionMap.put(factory, cnt.intValue()+1);
		}
		return ret;
	}


	public static List<FileSourceFactory> getRegisteredSessions() {
		List<FileSourceFactory> ret = new ArrayList<FileSourceFactory>();
		for (FileSourceFactory f : sessionMap.keySet()) {
			ret.add(f);
		}

		return ret;
	}

	public static synchronized void disposeFrame(int id) {

		if( runningViewers.size() == 1) {
			MessageDialog dialog = new MessageDialog();
			Response respone = dialog.showWarningDialog("This will close all of your windows."+
					"\nAre you sure you want to quit?");

			if( respone == Response.Yes) {
				System.exit(0);
			}
		} else {
			boolean ok = true;
			FileSourceFactory factory = sessions.get(id);
			if( factory != null ) {
				Integer cnt = sessionMap.get(factory);
				if( cnt != null ) {
					if( cnt.intValue() == 1) {
						ok = false;
						MessageDialog dialog = new MessageDialog();
						Response respone = dialog.showWarningDialog("Closing this window will close your session with "+factory.getTitle()+".\nAre you sure you want to continue?");
						if( respone == Response.Yes) {
							ok = true;
							sessionMap.remove(factory);
							try{
								factory.disConnect();
							} catch(Throwable e) {
							}
						}									
					} else {
						sessionMap.put(factory, cnt.intValue()-1);
					}
				}
			}
			if( ok ) {
				sessions.remove(id);
				FileSourceViewer v = runningViewers.remove(id);
				v.getFrame().dispose();
				v.getCopyProgressPanel().close();
			}
		}
	}


	public static interface CopyProgressListner {
		public void setDescription(String description);
		public void copyStarted(long bytesExpected);
		public void updateProgress(long bytesCopied);
		public void copyComplete();
		public void error(Exception e);
		public boolean isCanceled();
		public boolean isPaused();
	}



	private ActionContext actionTarget;
	private FileSourceTreeNode currentNode;
	private JPopupMenu popup;
	private Clipboard clipboard;

	private JMenuItem itemPaste;
	private JMenuItem itemDelete;
	private JMenuItem itemBackup;
	private JMenuItem itemCopy;
	private JMenuItem itemOpen;
	private JMenuItem itemOpenWith;
	private JMenuItem itemRename;
	private JMenuItem itemRefresh;
	private JMenuItem itemOpenTerminal;
	private JMenuItem itemNewFolder;
	private JMenuItem itemEdit;
	private JMenuItem itemEditWith;
	private JMenuItem itemPrint;

	List<JMenuItem> allMenus =  new ArrayList<>();
	List<JMenuItem> fileReadMenus = new ArrayList<>();
	List<JMenuItem> fileWriteMenus= new ArrayList<>();

	List<JMenuItem> dirReadMenus  = new ArrayList<>();
	List<JMenuItem> dirWriteMenus = new ArrayList<>();

	//private Session factory;
	private FileSourceFactory factory;
	protected int id;
	protected TreePath capturedPath;

	/**
	 * This is to prevent the bug, "unwanted drop event" that occurs the first time a tree node is selected
	 *
	 */
	protected boolean importok;
	@SuppressWarnings("unused")
	private FileSourceDropTargetListener dropTargetListener;

	protected Component popupComponent;


	/*
	 * 
Tree.foreground
Tree.background


Tree.textForeground

Tree.selectionInactiveBackground

Tree.textBackground

Tree.selectionInactiveForeground
Tree.selectionBackground
Tree.selectionForeground
Tree.selectionBorderColor





Tree.paintLines





Tree.line
Tree.dropLineColor
Tree.font



Tree.collapsedIcon
Tree.expandedIcon
Tree.openIcon
Tree.leafIcon
Tree.closedIcon

	 */

	/*

Table.focusCellBackground
TableHeader.font
TableHeader.ancestorInputMap
Table.sortIconColor
Table.scrollPaneBorder
Table.focusCellHighlightBorder
TableUI
Table.font
Table.ancestorInputMap
Table.foreground
Table.gridColor
Table.selectionInactiveForeground
Table.cellFocusRing

TableHeader.background
Table.descendingSortIcon
Table.focusCellForeground
Table.dropLineShortColor
TableHeader.cellBorder
Table.dropLineColor
Table.ascendingSortIcon
Table.ancestorInputMap.RightToLeft
TableHeaderUI
Table.background
TableHeader.foreground
Table.selectionInactiveBackground
TableHeader.focusCellBackground
Table.selectionBackground
Table.selectionForeground

	 */

	/*

ScrollPane.ancestorInputMap
ScrollPane.font
ScrollPane.background
ScrollPane.foreground
ScrollBarUI

ScrollPaneUI

ScrollBar.ancestorInputMap.RightToLeft
ScrollPane.ancestorInputMap.RightToLeft
ScrollBar.focusInputMap
ScrollBar.maximumThumbSize
ScrollBar.minimumThumbSize
ScrollPane.border
ScrollBar.width
ScrollBar.focusInputMap.RightToLeft
ScrollBar.ancestorInputMap

ScrollBar.trackHighlight
ScrollBar.thumbHighlight
ScrollBar.thumbDarkShadow
ScrollBar.thumbShadow

ScrollBar.background
ScrollBar.track
ScrollBar.thumb
ScrollBar.foreground




Panel.foreground = javax.swing.plaf.ColorUIResource
PanelUI = java.lang.String
Panel.background = javax.swing.plaf.ColorUIResource
Panel.font = javax.swing.plaf.FontUIResource
	 */
	public static Icon upArrow = new ImageIcon(FileSourceViewer.class.getResource(FileSourceViewer.ICON_LOCATION+"/UpArrow20x20.png"));
	public static Icon dnArrow = new ImageIcon(FileSourceViewer.class.getResource(FileSourceViewer.ICON_LOCATION+"/DnArrow20x20.png"));



	/**
	 * Opens the appearance settings (the font and colors, shared with the other Parley applications
	 * through swing-widgets). They are saved in the viewer's preferences, and a new font is given to
	 * every open viewer's table and tree.
	 *
	 * @param owner the window the settings belong to, or null
	 */
	static void actionAppearance(java.awt.Window owner) {
		if( SettingsDialog.showDialog(owner, pref) ) {
			for(FileSourceViewer v : runningViewers.values()) {
				v.appearanceChanged();
			}
		}
	}

	/** Gives the table and tree the font now in effect, and rows to fit it. */
	private void appearanceChanged() {
		Font f = UIManager.getFont("Table.font");
		if( f == null ) {
			f = UIManager.getFont("Label.font");
		}
		getTable().setFont(f);
		getTree().setFont(f);
		setRowSize();
		// before a folder is opened the table has no FileTableModel yet
		if( getTable().getModel() instanceof FileTableModel ) {
			try {
				((FileTableModel) getTable().getModel()).resetIFile();
			} catch (IOException e) {
				// the rows keep their old height until the folder is opened again
			}
		}
		getTableScrollPane().getColumnHeader().setBackground(Theme.current().getHeader());
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		// A GraalVM native image has no java.home, and AWT needs one (font configuration): use the
		// folder of the executable, where the image keeps the AWT libraries.
		if( System.getProperty("java.home") == null ) {
			ProcessHandle.current().info().command().ifPresent(c->{
				java.io.File dir = new java.io.File(c).getParentFile();
				if( dir != null ) {
					System.setProperty("java.home", dir.getAbsolutePath());
				}
			});
		}
		/*
		 * Load the registry
		 */
		new Thread(()->{
			try {
				IRegistry r = IRegistry.getRegistry();
				r.getRegisteredHandler("file.txt",CommandType.Any);
			} catch (Exception e) {
			}


		}).start();

		// The gradient look (gradient panels and scroll bars, see-through trees and tables, the gold
		// arrows), shared with the other Parley applications through swing-widgets, with the font
		// and colors the user chose in the appearance settings
		GradientLook.install(pref);

		TrustManager[] mgr = { new DynamicTrustManager(null,new VisualCertificateValidator())};
		SecureBaseObject.setDefaultTrustManagers(mgr);

		if( java.awt.Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().setOpenFileHandler(new OpenFilesHandler() {

					@Override
					public void openFiles(OpenFilesEvent e) {
						System.out.println("In file handler");						
					}
				});

				Desktop.getDesktop().setOpenURIHandler(new OpenURIHandler() {

					@Override
					public void openURI(OpenURIEvent e) {
						System.out.println("In URI handler");
					}
				});
				Desktop.getDesktop().setPrintFileHandler(new PrintFilesHandler() {

					@Override
					public void printFiles(PrintFilesEvent e) {
						System.out.println("In print handler");
					}
				});

			} catch (Exception e) {
				// Ignore
			}
			try {
				final Taskbar taskbar = Taskbar.getTaskbar();
				// The Dock / taskbar shows the app icon large, so use the biggest one
				final Image image = ParleyBrand.icon(512, "files");

				try {
					taskbar.setIconImage(image);
				} catch (Throwable e) {
				}
				try {
					java.awt.Desktop.getDesktop().setAboutHandler(new AboutHandler() {

						@Override
						public void handleAbout(AboutEvent e) {
							AboutFrame frame = new AboutFrame();
							frame.setLocationRelativeTo(null);
							if( SwingUtilities.isEventDispatchThread()) {
								frame.setVisible(true);
							} else {
								SwingUtilities.invokeLater(()->frame.setVisible(true));
							}
						}
					});
				} catch (Throwable e) {
				}
				try {
					java.awt.Desktop.getDesktop().setPreferencesHandler(new PreferencesHandler() {

						@Override
						public void handlePreferences(PreferencesEvent e) {
							SwingUtilities.invokeLater(()->actionAppearance(null));

						}
					});
				} catch (Throwable e) {
				}

				try {
					java.awt.Desktop.getDesktop().setQuitHandler(new QuitHandler() {

						@Override
						public void handleQuitRequestWith(QuitEvent e, QuitResponse response) {

							// tell the OS not to do anything
							response.cancelQuit();
							int sz = runningViewers.size();
							int i = -1;

							if( sz > 1) {
								MessageDialog dialog = new MessageDialog();
								Response respone = dialog.showWarningDialog("This will close all of your windows."+
										"\nAre you sure you want to quit?");

								if( respone == Response.Yes) {
									i = 0;
								}
							} else {
								MessageDialog dialog = new MessageDialog();
								Response respone = dialog.showWarningDialog("Are you sure you want to quit?");

								if( respone == Response.Yes) {
									i = 0;
								}

							}
							if(i==0) {
								System.exit(0);
							} 
						}
					});
				} catch (Throwable e) {
				}

				try {
					java.awt.Desktop.getDesktop().setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS);
				} catch (Throwable e) {
				}

			} catch (Throwable e) {
			}
		}
		final FileSourceViewer viewer = new FileSourceViewer();

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					viewer.setup(FileSourceFactory.getDefaultFactory(),true);
					viewer.show();
					SwingUtilities.invokeLater(()->{
						viewer.getTableScrollPane().getColumnHeader().setBackground(Theme.current().getHeader());
						viewer.getTableScrollPane().getColumnHeader().setOpaque(false);
					});
					//dotest(viewer);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}


	/**
	 * Create the application.
	 */
	public FileSourceViewer() {
		super();

	}


	public void newFolder(ActionEvent evt) {

		if(actionTarget != null && actionTarget.files.size() == 1) {
			FileSource dir = actionTarget.files.get(0);
			String s = MessageDialog.showInputDialog("Type new directory name","New Directory",null);
			if( s != null ) {
				s = s.trim();
				if( s.length() > 0 ) {
					try {
						FileSource newDir = dir.getChild(s);
						if( newDir.mkdirs()) {
							JTree tree = (JTree) getTree();
							TreePath path = tree.getSelectionPath();
							FileSourceTreeNode parent = (FileSourceTreeNode) path.getLastPathComponent();
							parent.add(new FileSourceTreeNode(newDir));
							((DefaultTreeModel)tree.getModel()).nodeStructureChanged(parent);
							FileTableModel tableModel = (FileTableModel) getTable().getModel();
							tableModel.resetIFile();

						} else {
							showError("Could not create "+s+" in "+dir.getAbsolutePath(), null);
						}
					} catch (Throwable e) {
						showError("Could not create "+s+" in "+dir.getAbsolutePath(), e);
					} 
				}
			}

		}
	}

	public void renameActionSet() {
		if( actionTarget != null && actionTarget.files.size()>0) {
			for (FileSource f : actionTarget.files) {
				String name = f.getName();
				String s = MessageDialog.showInputDialog("Type new name for "+name, "Rename", null);


				//If a string was returned, say so.
				if ((s != null) && (s.length() > 0) && !name.equals(s)) {
					try {
						FileSource newFile = f.getFileSourceFactory().createFileSource(s);
						if( !f.renameTo(newFile)) {
							showError("Could not rename "+name+" to "+s, null);

						}
					} catch (HeadlessException | IOException e) {
						showError("Could not rename "+name+" to "+s, e);					
					}
				}
			}
		}
		forceRefresh();
		actionTarget = null;
	}



	/** "3.2 GB", "512 KB" */
	static String formatSize(long bytes) {
		String [] units = {"bytes", "KB", "MB", "GB", "TB"};
		double v = bytes;
		int i = 0;
		while( v >= 1024 && i < units.length-1 ) {
			v /= 1024;
			i++;
		}
		return i == 0 ? bytes+" bytes" : String.format("%.1f %s", v, units[i]);
	}

	/**
	 * Asks whether to download a large file, which an application can only open as a local copy.
	 * Overridden by a test, which can't answer a dialog.
	 *
	 * @return true to go on
	 */
	protected boolean askLargeDownload(String message) {
		int[] answer = new int[1];
		Runnable ask = ()->{
			answer[0] = javax.swing.JOptionPane.showConfirmDialog(getFrame(), message, "Large file",
					javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
		};
		try {
			if( SwingUtilities.isEventDispatchThread() ) {
				ask.run();
			} else {
				SwingUtilities.invokeAndWait(ask);
			}
		} catch (Exception e) {
			return false;
		}
		return answer[0] == javax.swing.JOptionPane.YES_OPTION;
	}

	/**
	 * A file on another machine is downloaded before an application can open it, so a large one
	 * is asked about first (the limit is the preference PREF_LARGE_FILE_MB, 50 MB unless set; 0
	 * never asks). A local file is never asked about.
	 *
	 * @param sendsBack the file is edited, and what is changed is sent back whole
	 * @return true to go on with the download
	 */
	boolean confirmLargeDownload(FileSource f, boolean sendsBack) {
		if( f instanceof FileProxy ) {
			return true;
		}
		long limit = pref.getInt(PREF_LARGE_FILE_MB, DEFAULT_LARGE_FILE_MB) * 1024L * 1024L;
		if( limit <= 0 ) {
			return true;
		}
		long size;
		try {
			size = f.length();
		} catch (IOException e) {
			// (the download fails in its own way, with its own message)
			return true;
		}
		if( size < limit ) {
			return true;
		}
		String message = f.getName()+" is "+formatSize(size)+" and is not on this computer.\n"
				+ "It is downloaded to a temporary folder before it can be opened.\n"
				+ (sendsBack ? "If you change it, the whole file is sent back.\n" : "")
				+ "\nDownload it?";
		return askLargeDownload(message);
	}
	/** the copies made of files that are not on this machine, by file and version */
	private static final Map<String,File> downloads = new HashMap<>();

	/**
	 * A file on this machine for an application to open. A local file is itself; any other is
	 * copied to the temporary folder (once for each version of it) under its own name, so the
	 * application chooses by its extension and shows the right title.
	 * <p>
	 * It waits for the copy, with a progress dialog that appears after two seconds, so it must not
	 * be called on the event dispatch thread (see {@link #withLocalFile}).
	 *
	 * @return the file, or null if the copy was canceled or failed
	 */
	public File getLocalFileDownloadIfNeeded(FileSource f) {
		if (f instanceof FileProxy) {
			return ((FileProxy) f).getTarget();
		}
		if (SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException("A file is downloaded off the event thread: see withLocalFile");
		}

		try {
			// (the host is part of the answer: two servers can have the same path)
			String key = System.identityHashCode(f.getFileSourceFactory())+"|"+f.getAbsolutePath()+"|"+f.lastModified()+"|"+f.length();
			synchronized (downloads) {
				File have = downloads.get(key);
				if (have != null && have.isFile()) {
					return have;
				}
			}

			// (a copy already made is used without asking: that was returned above)
			if( !confirmLargeDownload(f, false) ) {
				return null;
			}

			File dir = new File(getTempDir(), "open-"+System.currentTimeMillis()+"-"+System.nanoTime());
			if (!dir.mkdirs()) {
				throw new IOException("Can't create "+dir);
			}
			File temp = new File(dir, f.getName());
			// (a folder is removed after what is in it)
			dir.deleteOnExit();
			temp.deleteOnExit();

			FileSource local = FileSourceFactory.getDefaultFactory().createFileSource(temp.getAbsolutePath());
			CopySingleDialog dialog = new CopySingleDialog();
			dialog.setTitle("Download ");
			EditMonitorThread.download(new CopyThread(f, local, dialog), dialog);
			if (dialog.isCanceled() || !temp.isFile()) {
				temp.delete();
				dir.delete();
				return null;
			}
			// (what an application changes in its copy is not sent back: it is a read-only open)
			temp.setReadOnly();
			synchronized (downloads) {
				downloads.put(key, temp);
			}
			return temp;
		} catch (IOException e) {
			showError("Can't download "+f.getName(), e);
			return null;
		}
	}

	/**
	 * Runs an action with a file on this machine for a file source: at once for a local file, and
	 * for any other after it has been downloaded, which is done off the event thread so the
	 * progress dialog is shown. The action is not run if the download is canceled or fails.
	 */
	public void withLocalFile(FileSource f, java.util.function.Consumer<File> action) {
		if (f instanceof FileProxy) {
			action.accept(((FileProxy) f).getTarget());
			return;
		}
		Thread t = new Thread(()->{
			File local = getLocalFileDownloadIfNeeded(f);
			if (local != null) {
				action.accept(local);
			}
		}, "Download "+f.getName());
		t.setDaemon(true);
		t.start();
	}

	/** a program the user named, to be started with a file */
	private static class ProgramRegData extends RegData {

		ProgramRegData(String path) {
			super(new File(path).getName(), path);
		}

		@Override
		public List<Icon> getIcons() throws IOException {
			return new ArrayList<>();
		}

		@Override
		public Icon getIcon(int width, int height) throws IOException {
			return null;
		}

		@Override
		public String getCommand() {
			return path;
		}

		@Override
		public List<String> getCommandLine(String file) {
			List<String> ret = new ArrayList<>();
			if (System.getProperty("os.name").toLowerCase().startsWith("mac") && path.endsWith(".app")) {
				ret.add("open");
				ret.add("-a");
			}
			ret.add(path);
			ret.add(file);
			return ret;
		}
	}

	public void edit(FileSource file) {
		if (file instanceof FileProxy) {
			File local = ((FileProxy) file).getTarget();								
			try {
				Desktop.getDesktop().edit(local);
			} catch (IOException e) {
				MessageDialog.showErrorDialog(e.getMessage(),"Can't edit "+local+"\n"+e);
			}
		} else {
			IRegistry reg = IRegistry.getRegistry();
			List<RegData> list = reg.getRegisteredHandler(file.getAbsolutePath(),CommandType.Editor);
			if( list == null || list.size()==0) {
				MessageDialog.showMessageDialog("There are no editors registered for "+file, "");
			} else {

				RegData editor = list.get(0); 
				File local1 = new File(getTempDir(),""+System.currentTimeMillis()+file.getName());
				FileSource local;
				try {
					local = FileSourceFactory.getDefaultFactory().createFileSource(local1.getAbsolutePath());
					local1.deleteOnExit();
					if( confirmLargeDownload(file, true) ) { new EditMonitorThread(this, file,local, editor).start(); }					
				} catch (IOException e) {
					showError("", e);
				}

			}
		}

	}

	public void editWith(FileSource file) {


	}

	public void print(FileSource file) {
		withLocalFile(file, local->{
			try {
				Desktop.getDesktop().print(local);
			} catch (IOException e) {
				MessageDialog.showErrorDialog(e.getMessage(),"Can't print "+local+"\n"+e);
			}
		});
	}


	public void open1(FileSource file) {
		withLocalFile(file, this::open1Local);
	}

	private void open1Local(File local) {
		try {
			Desktop.getDesktop().open(local);
		} catch (IOException e1) {
			IRegistry reg = IRegistry.getRegistry();
			List<IRegistry.RegData> list = reg.getRegisteredHandler(local.getAbsolutePath(),CommandType.Any);
			String path = local.getAbsolutePath();
			Process proc=null;

			for (IRegistry.RegData exe : list) {
				try {
					proc = (new ProcessBuilder().command(exe.getCommandLine(path))).start();
					break;
				} catch (IOException e) {
					System.out.println("Error executing "+exe+e);
				}
			}
			if( proc == null ) {
				MessageDialog.showErrorDialog("","Can't open "+local+"\n"+e1);
			} else {
				try {
					int exit = proc.waitFor();
					System.out.println("Exit code = "+exit);
					try(InputStream in = proc.getErrorStream()) {
						String tmp = new String(in.readAllBytes());
						System.out.println("err: "+tmp);
					}
					try(InputStream in = proc.getInputStream()) {
						String tmp = new String(in.readAllBytes());
						System.out.println("out: "+tmp);
					}


				} catch (InterruptedException | IOException e) {
					showError("", e);
				}

			}
		}

	}



	public void setup(FileSourceFactory factory1, boolean registerFrame) {
		if( factory1 == null ) {
			factory1 = FileSourceFactory.getDefaultFactory();
		}
		this.factory = factory1;

		getFrame().setIconImages(null);
		// The screen menu bar is a Mac feature: elsewhere the menu stays in the window (and asking
		// throws UnsupportedOperationException), so it is only taken out of the window when the
		// desktop will show it.
		if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.APP_MENU_BAR)) {
			Desktop desktop = Desktop.getDesktop();
			JMenuBar menuBar = getMenuBar();
			getNorthPanel().remove(menuBar);
			desktop.setDefaultMenuBar(menuBar);
		}


		showHidden    = pref.getBoolean(PREF_SHOW_HIDDEN+"."+factory1.getTypeId(),    showHidden);
		showExtention = pref.getBoolean(PREF_SHOW_EXTENTION+"."+factory1.getTypeId(), showExtention);


		final JFrame frame = getFrame();
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		if( registerFrame ) {
			id = registerFrame(this);
			setupMenu();
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter(){
				public void windowClosing(WindowEvent e){
					FileSourceViewer.disposeFrame(id);
				}
			});
		}

		// The Parley app icon with the folder badge, in every size; the OS picks the one each place needs
		frame.setIconImages(ParleyBrand.appIcons("files"));

		frame.setLocationRelativeTo(null);
		popup = new JPopupMenu("Options");
		itemDelete = new JMenuItem("Delete");
		itemDelete.addActionListener(new ActionListener() {


			
//336-360-3289
//sabra 

			public void actionPerformed(ActionEvent evt) {
				if (popupComponent instanceof JTree) {
					actionTarget = getSelectedFromTree();					
				} else if (popupComponent instanceof JTable) {
					actionTarget = getSelectedFromTable();
				}
				deletActionSet();
			}
		});
		popup.add(itemDelete);

		if( !backupAvailible) {
			itemBackup = new JMenuItem("Backup");
			itemBackup.addActionListener(new ActionListener() {


				public void actionPerformed(ActionEvent evt) {
					backupActionSet();
				}
			});
			popup.add(itemBackup);
		}

		itemCopy = new JMenuItem("Copy");
		itemCopy.addActionListener(new ActionListener() {


			public void actionPerformed(ActionEvent evt) {
				copyActionSet();
			}
		});
		popup.add(itemCopy);

		itemPaste = new JMenuItem("Paste");
		itemPaste.addActionListener(new ActionListener() {


			public void actionPerformed(ActionEvent evt) {
				System.out.println("itemPaste");
				pasteToActionSet();
			}
		});

		popup.add(itemPaste);


		itemEdit = new JMenuItem("Edit");
		itemPrint = new JMenuItem("Print");
		if( Desktop.isDesktopSupported() ) {
			Desktop destop = Desktop.getDesktop();

			if(destop.isSupported(Action.OPEN)) {
				itemOpen = new JMenuItem("Open");
				itemOpen.addActionListener(new ActionListener() {


					public void actionPerformed(ActionEvent evt) {
						ActionContext sel = getSelectedFromTable();
						if( sel != null ) {
							try {
								for (FileSource f : sel.files) {
									if(f.isDirectory()) {
										setSelectionPath(buildTreePath(f));
									} else {
										open(f);
									}
								}
							} catch (Exception e) {
								showError("Error opening file",e);
							}
						}
					}
				});
				popup.add(itemOpen);


				itemOpenWith = new JMenuItem("Open With...");
				itemOpenWith.addActionListener(new ActionListener() {


					public void actionPerformed(ActionEvent evt) {
						ActionContext sel = getSelectedFromTable();
						if( sel != null ) {
							try {
								for (FileSource f : sel.files) {
									if(f.isDirectory()) {
										setSelectionPath(buildTreePath(f));
									} else {
										openWith(f);
									}
								}
							} catch (Exception e) {
								showError("Error opening file",e);
							}
						}
					}
				});
				popup.add(itemOpenWith);

			}

			if(destop.isSupported(Action.EDIT)) {

				itemEdit.addActionListener(new ActionListener() {


					public void actionPerformed(ActionEvent evt) {
						ActionContext sel = getSelectedFromTable();
						if( sel != null ) {
							try {
								for (FileSource f : sel.files) {
									if(!f.isDirectory()) {
										edit(f);
									}
								}
							} catch (Exception e) {
								showError("Error editing file",e);
							}
						}

					}
				});
				popup.add(itemEdit);
				itemEditWith = new JMenuItem("Edit with...");
				itemEditWith.addActionListener(new ActionListener() {


					public void actionPerformed(ActionEvent evt) {
						ActionContext sel = getSelectedFromTable();
						if( sel != null ) {
							try {
								for (FileSource f : sel.files) {
									if(!f.isDirectory()) {
										editWith(f);
									}
								}
							} catch (Exception e) {
								showError("Error editing file",e);
							}
						}

					}
				});
				popup.add(itemEditWith);

			}

			if(destop.isSupported(Action.PRINT)) {

				itemPrint = new JMenuItem("Print");
				itemPrint.addActionListener(new ActionListener() {


					public void actionPerformed(ActionEvent evt) {
						ActionContext sel = getSelectedFromTable();
						if( sel != null ) {
							try {
								for (FileSource f : sel.files) {
									if(!f.isDirectory()) {
										print(f);
									}
								}
							} catch (Exception e) {
								showError("Error printing file",e);
							}
						}
					}
				});
				popup.add(itemPrint);
			}
		} // is desktop supported 

		itemRename = new JMenuItem("Rename");
		itemRename.addActionListener(new ActionListener() {


			public void actionPerformed(ActionEvent evt) {
				renameActionSet();
			}
		});
		popup.add(itemRename);

		itemNewFolder = new JMenuItem("New Folder");
		itemNewFolder.addActionListener(new ActionListener() {


			public void actionPerformed(ActionEvent evt) {
				newFolder(evt);
			}
		});
		popup.add(itemNewFolder);


		itemOpenTerminal = new JMenuItem("New terminal at location");
		itemOpenTerminal.addActionListener((e)->{
			if( actionTarget !=null ) {
				if( actionTarget.files.size()>0) {
					actionOpenTerminal(actionTarget.files.get(0));
				}
			}
		});

		if( factory != null ) {
			setTitle(factory.getTitle());
			itemRefresh = new JMenuItem("Refresh");
			itemRefresh.addActionListener(new ActionListener() {


				public void actionPerformed(ActionEvent evt) {
					forceRefresh();
				}
			});
			popup.add(itemRefresh);

		} 

		setupTree();
		setupTable();
		setRowSize();
		//System.out.println("Setup 01");





		//System.out.println("Setup 01.2");
		setKeys(getTree(),getTable());

		//System.out.println("Setup 03");
		final JPanel status = getStatusPanel();
		status.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent arg0) {
				if(currentNode != null ) {
					final FileSourceTreeNode mynode = currentNode;
					status.removeAll();
					final String editText = currentNode.getDir().getAbsolutePath();
					final JTextField fld = new JTextField(editText);
					fld.selectAll();
					fld.addKeyListener(new KeyListener() {


						public void keyTyped(KeyEvent e) {
							if(((int)e.getKeyChar()) == KeyEvent.VK_ESCAPE) {
								SwingUtilities.invokeLater(new Runnable() {


									public void run() {
										currentNode = mynode;
										setStatus(currentNode.getDir());
									}
								});
							}
						}


						public void keyReleased(KeyEvent e) {
						}


						public void keyPressed(KeyEvent e) {
						}
					});
					fld.addActionListener(new ActionListener() {


						public void actionPerformed(ActionEvent e) {
							String txt = fld.getText().trim();
							if(!txt.equals(editText)) {
								try {
									FileSource newDir = factory.createFileSource(txt); 
									if( newDir != null && newDir.isDirectory()) {
										TreePath path = buildTreePath(newDir);
										setSelectionPath(path);
									}
								} catch (IOException er) {
									showError("", er);
								}
							} else {
								setStatus( currentNode.getDir());
							}
						}

					});

					fld.setSize(status.getSize());
					status.add(fld);
					fld.setVisible(true);
					fld.requestFocus();
					status.repaint();
				}
			}
		});


		dropTargetListener= new FileSourceDropTargetListener(getTable(),getTree());



		//System.out.println("Setup 06");
		//scroll.setTransferHandler(handler);
		//  start here
		DragSource source = DragSource.getDefaultDragSource();
		source.createDefaultDragGestureRecognizer(getTable(), DnDConstants.ACTION_COPY, new DragGestureListener() {


			public void dragGestureRecognized(DragGestureEvent dge) {
				//grab the selected files from the table model
				ActionContext files = getSelectedFromTable();
				if(files.files.size() > 0 ) {
					Transferable transferable = createTransferable(files.files);
					//and this is the magic right here
					dge.startDrag(null,transferable);
				}
			}
		});
		source.createDefaultDragGestureRecognizer(getTree(), DnDConstants.ACTION_COPY, new DragGestureListener() {


			public void dragGestureRecognized(DragGestureEvent dge) {
				//grab the selected files from the table model
				ActionContext files = getSelectedFromTree();
				if(files.files.size() > 0 ) {
					Transferable transferable = createTransferable(files.files);
					//and this is the magic right here
					dge.startDrag(null,transferable);
				}
			}
		});

		allMenus =      Arrays.asList(itemOpenTerminal,itemPaste,itemDelete,itemBackup,itemCopy,itemOpen,itemOpenWith,itemRename,itemRefresh,itemEdit,itemEditWith,itemPrint,itemNewFolder);
		fileReadMenus = Arrays.asList(itemCopy,itemOpen,itemOpenWith,itemPrint);

		fileWriteMenus= Arrays.asList(itemDelete,itemRename,itemEdit,itemEditWith);

		dirReadMenus  = Arrays.asList(itemOpenTerminal,itemCopy,itemOpen,itemRefresh);
		dirWriteMenus = Arrays.asList(itemOpenTerminal,itemDelete,itemRename,itemNewFolder);

		new Thread(()-> {
			try {
				RootFileSourceTreeLoadingWorking worker = new RootFileSourceTreeLoadingWorking();
				worker.execute();				
			} catch (Exception e) {
				showError("", e);
			}

		}).start();
		getCopyProgressPanel().setViewer(this);
		getCopyProgressPanel().setVisible(false);

	}



	protected void openWith(FileSource f) {
		IRegistry reg = IRegistry.getRegistry();
		List<RegData> list = reg.getRegisteredHandler(f.getName(), CommandType.Any);
		if( list == null || list.size()==0) {
			MessageDialog.showMessageDialog("No application availible to open "+f, "");
			return;
		}

		String exe = OptionSelectDialog.showDialog(list);
		if( exe == null || exe.isEmpty()) {
			return;
		}

		// (the application the user picked from the list knows how it is started; text typed in the
		// dialog is a program's path)
		RegData chosen = null;
		for(RegData d : list) {
			if( exe.equals(d.path) ) {
				chosen = d;
				break;
			}
		}

		if( f instanceof FileProxy ) {
			executeExternal(exe,chosen,f);
		} else {
			// A file on another machine: the application gets a copy that is downloaded, and what
			// it changes is offered to be sent back when it ends (as Edit does).
			RegData app = chosen != null ? chosen : new ProgramRegData(exe);
			File copy = new File(getTempDir(),""+System.currentTimeMillis()+f.getName());
			try {
				FileSource local = FileSourceFactory.getDefaultFactory().createFileSource(copy.getAbsolutePath());
				copy.deleteOnExit();
				if( confirmLargeDownload(f, true) ) { new EditMonitorThread(this, f, local, app).start(); }
			} catch (IOException e) {
				showError("", e);
			}
		}

	}

	private void executeExternal(String exe, RegData handler, FileSource f) {
		try {
			String path = f.getAbsolutePath();
			if( f instanceof FileProxy ) {
				path = ((FileProxy)f).getTarget().getAbsolutePath();
			}

			// One argument to a string: a ProcessBuilder takes a single string as the name of the
			// program, so "C:\Program Files\App\app.exe C:\file.txt" is a program that is not found.
			List<String> command;
			if( handler != null ) {
				command = handler.getCommandLine(path);
			} else {
				command = new ArrayList<>();
				if( System.getProperty("os.name").toLowerCase().startsWith("mac") && exe.endsWith(".app") ) {
					command.add("open");
					command.add("-a");
				}
				command.add(exe);
				command.add(path);
			}

			ProcessBuilder b = new ProcessBuilder().command(command);
			Process proc = b.start();
			if( proc == null ) {
				MessageDialog.showErrorDialog("Could not execute "+exe, "");
				return;
			}

			new Thread(()->{
				try {
					int exitCode = proc.waitFor();
					if( exitCode != 0) {
						SwingUtilities.invokeLater(()->{
							MessageDialog.showErrorDialog(exe+" existed with err code "+exitCode, "");
						});
					}
				} catch (InterruptedException e) {
				}
			}).start();

		} catch (IOException e) {
			MessageDialog.showErrorDialog(e.getMessage(), "");
		}



	}

	private void setupTable() {
		final JTable table = getTable();
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setFillsViewportHeight(true);

		//System.out.println("Setup 01.1");
		table.getSelectionModel().addListSelectionListener((e)->{			
			if( !e.getValueIsAdjusting()) {
				ListSelectionModel lsm = (ListSelectionModel)e.getSource();
				int idxs [] =  getSelectedIndices(lsm);
				if( idxs !=null && idxs.length>0) {
					FileTableModel tm = (FileTableModel) table.getModel();
					for(int row : idxs) {
						FileSource file = tm.getFile(row);
						if( file != null) {
							try {
								if(file.isDirectory()) {
									boolean readable = isReadable(file);
									if( !readable) {
										lsm.removeSelectionInterval(row, row);
									}
								}
							} catch (IOException e1) {
								showError("Error in file selection",e1);								
							}
						}
					}					
				}
			}
		});

		table.setAutoCreateRowSorter(true);
		table.setDragEnabled(true);


		table.setDefaultRenderer(Date.class, new FileTableCellRenderer());
		table.setDefaultRenderer(Long.class, new FileTableCellRenderer());
		table.setDefaultRenderer(String.class, new FileTableCellRenderer());
		table.setDefaultRenderer(Icon.class, new FileTableCellRenderer());

		table.addMouseListener(new MouseAdapter() {


			public void mouseReleased(MouseEvent evt) {				
				if( evt.getButton() == 3) {
					int [] rows = table.getSelectedRows();
					int touched = table.rowAtPoint(evt.getPoint());
					boolean all = false;
					for(int idx=0; idx<rows.length; idx++) {
						if( rows[idx] == touched) {
							all = true;
							break;
						}
					}
					if( !all && touched >=0 ) {
						table.setRowSelectionInterval(touched, touched);
						Thread.yield();
					}
					actionTarget = getSelectedFromTable();
					try {
						FileSource file = null;

						if( actionTarget.files.size() == 1 ) {
							file = actionTarget.files.get(0);
						} else {
							TableModel model = table.getModel();
							if (model instanceof FileTableModel) {
								file = ((FileTableModel)model).getDir();
								actionTarget = new ActionContext(table,  Arrays.asList(file));
							}
						}

						if( file !=null && file.exists()) {
							popup.removeAll();

							//checkForClipboardContent sets itemPaste based on clipBoard content 
							checkForClipboardContent();
							if(itemPaste.isEnabled() && file.canWrite() && file.isDirectory()) {
								popup.add(itemPaste);								 
							} 

							buildPopupFor(file);							
							popupComponent = evt.getComponent();
							popup.show(evt.getComponent(),evt.getX(),evt.getY());
						}

					} catch (Exception e) {
						showError("Error building menu",e);
					}

				} else if( evt.getClickCount() == 2) {
					ActionContext list = getSelectedFromTable();
					for (FileSource f : list.files) {
						try {
							if( f.isDirectory()) {
								final TreePath tp = buildTreePath(f);
								setSelectionPath(tp);

								capturedPath = tp;
							} else {
								open(f);
							}
						} catch (Exception e) {
							showError("Error opening file",e);
						}
					}
				}
			}

		});

		table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {

			@Override
			public void columnSelectionChanged(ListSelectionEvent e) {		
			}

			@Override
			public void columnRemoved(TableColumnModelEvent e) {				
			}

			@Override
			public void columnMoved(TableColumnModelEvent e) {				
			}

			@Override
			public void columnMarginChanged(ChangeEvent e) {
				if( !inSetTableModel) {
					Object source = e.getSource();
					if (source instanceof DefaultTableColumnModel) {
						DefaultTableColumnModel model = (DefaultTableColumnModel) source;
						columnWidths = new int[model.getColumnCount()];
						for (int idx = 0; idx < columnWidths.length; idx++) {
							TableColumn nc = model.getColumn(idx);
							columnWidths[idx] = nc.getPreferredWidth();
						}
					}
				}
			}

			@Override
			public void columnAdded(TableColumnModelEvent e) {

			}
		});


	}

	TreePath test ;

	private void setupTree() {
		final JTree tree = getTree();
		tree.setPreferredSize(null);
		tree.setExpandsSelectedPaths(true);

		tree.addTreeExpansionListener(new TreeExpansionListener() {
			//  Keep a running list of expended node so 
			// we can re-expand when the model is rebuilt.
			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				TreePath path = event.getPath();
				if( path != null) {
					expandedSet.add(path);
					if( test == null ) {
						test = path;
					}
				}

			}


			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				TreePath path = event.getPath();
				if( path != null) {
					expandedSet.remove(path);
				}

			}
		});



		tree.addTreeWillExpandListener(new TreeWillExpandListener() {
			public void treeWillCollapse(TreeExpansionEvent event)	throws ExpandVetoException {

			}


			public void treeWillExpand(final TreeExpansionEvent event) throws ExpandVetoException {
				TreePath path = event.getPath();
				Object lastNode = path.getLastPathComponent();

				// already has children ?
				if ( ((FileSourceTreeNode)lastNode).getChildCount()>0  ) {
					return;
				}

				FileSourceTreeLoadingWorking worker = new FileSourceTreeLoadingWorking(( FileSourceTreeNode )lastNode, path, (JTree) event.getSource());
				worker.execute();


			}
		});

		tree.addTreeSelectionListener(new TreeSelectionListener() {

			public void valueChanged(TreeSelectionEvent event) {

				final TreePath sel = tree.getSelectionPath();
				if( sel != null ) {
					Object obj = sel.getLastPathComponent();
					if( obj != null ) {
						if( obj instanceof FileSourceTreeNode) {

							currentNode = ((FileSourceTreeNode)obj);
							try {
								FileSource link = currentNode.getDir().getLinkedTo();
								if( link != null ) {
									if( link.isChildOfMine(currentNode.getDir())) {

									}
								}
							} catch (IOException e) {
							}

							if( !tree.isExpanded(sel)) {
								tree.expandPath(sel);
								tree.scrollPathToVisible(sel);
								importok = true;

							} else {
								setTableModel(currentNode.dir);
							}
							FileSource currentDir = currentNode.getDir();
							try {
								setTitle(currentDir.getTitle());
							} catch (IOException e) {
								setTitle(e.getMessage());
							}
							setStatus(currentDir);

						}						
					}
				}

			}
		});

		tree.setCellRenderer(new DefaultTreeCellRenderer() {
			private static final long serialVersionUID = 1L;
			private Icon closedIcon = new ImageIcon(getClass().getResource(ICON_LOCATION+"/folder_closed_20.png"));
			private Icon notReadableIcon = new ImageIcon(getClass().getResource(ICON_LOCATION+"/folder_closed_not_readable_20.png"));
			private Icon openIcon = new ImageIcon(getClass().getResource(ICON_LOCATION+"/folder_opened_20.png"));
			private Icon emptyIcon = closedIcon;


			public Component getTreeCellRendererComponent(JTree tree,Object value, boolean selected, boolean expanded,boolean isLeaf, int row, boolean focused) {
				Component c = super.getTreeCellRendererComponent(tree, value,selected, expanded, isLeaf, row, focused);
				boolean readable = true;
				if (value instanceof FileSourceTreeNode) {
					FileSourceTreeNode node = (FileSourceTreeNode) value;
					readable = isReadable(node.dir);
					if( !readable) {
						this.setEnabled(false);
					} else {
						this.setEnabled(true);
					}					
				}

				if (expanded) {
					setIcon(openIcon);
				} else {
					if( !isLeaf) {
						setIcon(readable? closedIcon:notReadableIcon);
					} else {
						setIcon(readable? emptyIcon:notReadableIcon);
					}
				}
				return c;
			}
		});

		TreeSelectionModel tsm = tree.getSelectionModel();

		tree.getSelectionModel().addTreeSelectionListener((e)->{

			TreePath[] paths =tsm.getSelectionPaths();
			if( paths !=null && paths.length>0) {
				for(TreePath path : paths) {
					Object comp = path.getLastPathComponent();
					if (comp instanceof FileSourceTreeNode) {
						FileSourceTreeNode file = (FileSourceTreeNode) comp;
						if(! isReadable(file.dir)) {
							tsm.removeSelectionPath(path);
						}
					}
				}
			}			
		});

		tree.setExpandsSelectedPaths(true);
		//tree.setDragEnabled(true);
		//tree.setTransferHandler(handler);
		tree.addMouseListener(new MouseAdapter() {


			public void mouseClicked(MouseEvent evt) {
				try {


					if( evt.getButton() == 3) {
						TreePath[] sel = tree.getSelectionPaths();
						TreePath touched = tree.getPathForLocation(evt.getX(), evt.getY());
						boolean all = false;
						if( sel != null && touched !=null) {
							for(int idx=0; idx< sel.length; idx++ ) {
								if( touched.equals(sel[idx])) {
									all = true;
									break;
								}
							}
						}
						if( !all ) {
							setSelectionPath(touched);
						}
						actionTarget = getSelectedFromTree();
						if( actionTarget.files.size() > 0 ) {
							popup.removeAll();
							checkForClipboardContent();

							for(FileSource file : actionTarget.files) {
								if(itemPaste.isEnabled() && file.canWrite() ) {
									popup.add(itemPaste);								 
								} 
								try {
									buildPopupFor(file);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							popupComponent = evt.getComponent();
							popup.show(evt.getComponent(), evt.getX(), evt.getY());
						}
					} 

				} catch (Exception e) {
					showError("", e);
				}
			}
		});
		tree.setSelectionRow(0);
	}

	private void setRowSize() {
		JTable table = getTable();
		Font font = table.getFont();
		FontMetrics m = table.getFontMetrics(font);
		int h = (int)(m.getHeight());
		table.setRowHeight(h);
		JTree tree = getTree();
		tree.setRowHeight(h);
		getCopyProgressPanel().setRowSize(h);
	}

	protected void buildPopupFor(FileSource file) throws IOException {
		// assume removeAll and set paste is done ahead of time
		List<JMenuItem> tmp = new ArrayList<>();
		if( file.isFile()) {
			if( file.canRead()) {
				tmp.addAll(fileReadMenus);
			}
			if( file.canWrite()) {
				tmp.addAll(fileWriteMenus);
			}
		} else {
			// every dir gets this because I don't know how to tell 
			// if the user has mkdir rights
			tmp.add(itemNewFolder);
			if( file.canRead()) {
				tmp.addAll(dirReadMenus);
			}
			if( file.canWrite()) {
				tmp.addAll(dirWriteMenus);
			} 
		}
		for(JMenuItem mi : tmp) {
			// just in case it's already there so we don't have two of a menu
			popup.remove(mi);
			popup.add(mi);
		}
	}

	protected boolean isRoot(FileSource file) throws IOException {
		String path = file.getAbsolutePath();
		for(FileSource root : factory.listRoots()) {
			if( root.getAbsolutePath().equals(path)) {
				return true;
			}
		}
		return false;
	}

	protected boolean isReadable(FileSource dir) {
		boolean ret = true;
		try {
			ret = dir.canRead();
			if( ret) {
				FileSource link = dir.getLinkedTo();
				if( link != null) {
					ret = !link.isChildOfMine(dir);				
				}
			}
		} catch (Throwable e) {
		}

		return ret;
	}

	protected void setStatus( FileSource dir) {
		JPanel status = getStatusPanel();
		status.removeAll();
		setTitle(dir.getAbsolutePath());
		status.add(new FileComboBoxPanel(dir,new ActionListener() {


			public void actionPerformed(ActionEvent e) {
				Object obj = e.getSource();
				if (obj != null && obj instanceof FileSource) {
					FileSource f = (FileSource) obj;
					try {
						TreePath path = buildTreePath(f);
						FileSourceTreeNode lastNode = (FileSourceTreeNode) path.getLastPathComponent();

						FileSourceTreeLoadingWorking worker = new FileSourceTreeLoadingWorking(( FileSourceTreeNode )lastNode, path, getTree());
						worker.execute();

					} catch (IOException e1) {
						showError("", e1);
					} 


				}				
			}
		}));
		status.updateUI();

	}

	public static TreePath getPath(TreeNode treeNode) {
		List<Object> nodes = new ArrayList<Object>();
		if (treeNode != null) {
			nodes.add(treeNode);
			treeNode = treeNode.getParent();
			while (treeNode != null) {
				nodes.add(0, treeNode);
				treeNode = treeNode.getParent();
			}
		}

		return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
	}

	public FileSourceTreeNode findNode(FileSource file) throws IOException {
		FileSource parent = file.getParentFile();
		if( parent == null ) {
			FileSourceTreeNode tmp = (FileSourceTreeNode) getTree().getModel().getRoot();
			if( tmp.dir.equals(file)) {
				return tmp;
			}
		}
		//  this is my parent node so my node MUST be a child of it
		FileSourceTreeNode pnode = findNode(parent);
		if( pnode !=null ) {
			Vector<TreeNode> e = pnode.getChildren();
			if( e != null ) {
				for(TreeNode node : e) {
					if (node instanceof FileSourceTreeNode) {
						FileSourceTreeNode tn = (FileSourceTreeNode) node;
						if( tn.dir.equals(file)) {
							return tn;
						}		
					}
				}
			}
		}


		return null;
	}

	public TreePath  buildTreePath(FileSource f) throws IOException {
		FileSourceTreeNode node= findNode(f);
		if( node !=null ) {
			return getPath(node);
		}

		return null;
	}

	private void setupMenu() {

		getTestButton().addActionListener((e)->{
			if( test !=null ) {
				TreePath path = test;
				Object last = path.getLastPathComponent();
				System.out.println("cls="+last.getClass());
				setSelectionPath(test);
			}
		});
		getTestButton().setVisible(false);

		getShowHidden().setSelected(showHidden);
		getShowHidden().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBoxMenuItem src = (JCheckBoxMenuItem) e.getSource();
				showHidden = src.isSelected();

				refreshAll();
			}
		});

		getShowExtention().setSelected(showExtention);
		getShowExtention().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JCheckBoxMenuItem src = (JCheckBoxMenuItem) e.getSource();
				showExtention = src.isSelected();
				refreshAll();
			}
		});

		getMntmLocalBrowser().addActionListener(new ActionListener() {


			public void actionPerformed(ActionEvent arg0) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							FileSourceViewer window = new FileSourceViewer();
							window.setup(FileSourceFactory.getDefaultFactory(),true);
							window.show();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});				
			}
		});

		if( FileSourceFactory.getRegisterdFactories().length > 1 ) {
			getMntmRemoteBrowser().addActionListener(new ActionListener() {


				public void actionPerformed(ActionEvent arg0) {
					FactoryPropertiesDialog dialog = new FactoryPropertiesDialog();
					dialog.showDialog(new MemoryFileSourceFactory());
					if( !dialog.isCancel() ) {
						FileSourceFactory f = dialog.getFactory();
						FileSourceViewer b = new FileSourceViewer();
						try {
							b.setup(f,true);
							b.show();				
						}catch(Exception ex) {
							MessageDialog.showErrorDialog( ex.getMessage(), f.getTitle());
							try {
								f.disConnect();
							} catch (Exception e) {
							}
						}
					}
				}
			});
		} else {
			// only 1 factory means there are no remote factories
			getMntmRemoteBrowser().setEnabled(false);
		}

		getMntmExit().addActionListener(new ActionListener() {


			public void actionPerformed(ActionEvent arg0) {
				MessageDialog dialog = new MessageDialog();
				Response respone = dialog.showWarningDialog("Are you sure you want to close all windows and quit?");

				if( respone == Response.Yes) {
					System.exit(0);
				}
			}
		});

		// Help > About (the Desktop about handler exists only on macOS)
		getMntmAbout().addActionListener((e)->{
			AboutFrame frame = new AboutFrame();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});

		JMenuItem terminal = getMntmTerminal();
		//getMnNew().remove(terminal);
		terminal.addActionListener((e)->{
			actionOpenTerminal(null);
		});

		getMntmClose().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				FileSourceViewer.disposeFrame(id);
			}
		});

		// Options > Appearance...: the font and colors (on macOS, Preferences opens it too)
		getMntmFont().addActionListener((e)->actionAppearance(getFrame()));

		getMntmSearch().addActionListener(new ActionListener() {


			public void actionPerformed(ActionEvent arg0) {
				if (currentNode == null || currentNode.getDir() instanceof RootFile ) {
					MessageDialog.showMessageDialog("Please select a directory to search", "");
					return;
				}
				FileSource dir = currentNode.getDir();
				SearchFrame ff = new SearchFrame(dir, FileSourceViewer.this);
				ff.setVisible(true);
			}
		});


		getMntmStartLocation().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {				
				actionSetStartLocation();
			}
		});


	}

	protected void actionSetStartLocation() {
		try {
			FileSource dir = currentNode.getDir();
			String prefId = PREF_START_LOCATION+"_"+factory.getTypeId();
			String tmp = pref.get(prefId, null);
			if( tmp != null ) {
				FileSource tmpDir = factory.createFileSource(tmp);
				if( tmpDir.exists() && dir.isDirectory()) {
					dir = tmpDir;
				}
			}


			FileSourceChooserDialog fc = new FileSourceChooserDialog();

			if( dir !=null) {
				fc.setSelectedFile(dir);
			}

			fc.setFileSelectionMode(FileSourceChooserDialog.DIRECTORIES_ONLY);
			JFrame frame = getFrame();
			fc.setLocationRelativeTo(frame);
			if( fc.showDialog(frame, "Select") == FileSourceChooserDialog.APPROVE_OPTION) {

				dir = fc.getSelectedFile();
				if( dir != null ) {
					if( dir.exists()) {
						pref.put(prefId, dir.getAbsolutePath());					
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
			showError("", e);
		}
	}

	private void actionOpenTerminal(FileSource cwd) {
		// "exit" in the terminal must close the terminal, not end the viewer's JVM
		Console.exitJvm = false;
		Console console = new Console();

		try {
			if( cwd == null ) {
				cwd = factory.getCurrentDirectory();
			}
			
			console.setCurrentDirectory(cwd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		console.start();


	}

	protected void refreshAll()  {
		new Thread(()->{
			String id = PREF_SHOW_HIDDEN+"."+factory.getTypeId();
			pref.putBoolean(id, showHidden);
			try {
				pref.flush();
			} catch (BackingStoreException e1) {
			}

		}).start();
		try {
			new RootFileSourceTreeLoadingWorking().execute();
		} catch (IOException e) {
			showError("", e);
		}
	}



	private void setKeys(JTree tree, JTable table) {
		clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

		KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C,ActionEvent.CTRL_MASK,false);
		KeyStroke copy2 = KeyStroke.getKeyStroke(KeyEvent.VK_C,ActionEvent.META_MASK,false);
		// Identifying the copy KeyStroke user can modify this
		// to copy on some other Key combination.
		KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V,ActionEvent.CTRL_MASK,false);
		KeyStroke paste2 = KeyStroke.getKeyStroke(KeyEvent.VK_V,ActionEvent.META_MASK,false);
		// Identifying the Paste KeyStroke user can modify this
		//to copy on some other Key combination.
		table.registerKeyboardAction(new ActionListener() {


			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Table copy");
				copyFromTable();
			}
		},"Copy",copy,JComponent.WHEN_FOCUSED);

		table.registerKeyboardAction(new ActionListener() {


			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Table copy2");
				copyFromTable();
			}
		},"Copy2",copy2,JComponent.WHEN_FOCUSED);

		table.registerKeyboardAction(new ActionListener() {


			public void actionPerformed(ActionEvent arg0) {
				pasteToTable();
			}
		},"Paste",paste,JComponent.WHEN_FOCUSED);
		table.registerKeyboardAction(new ActionListener() {


			public void actionPerformed(ActionEvent arg0) {
				pasteToTable();
			}
		},"Paste2",paste2,JComponent.WHEN_FOCUSED);


		tree.registerKeyboardAction(new ActionListener() {


			public void actionPerformed(ActionEvent arg0) {
				copyFromTree();
			}
		},"Copy",copy,JComponent.WHEN_FOCUSED);

		tree.registerKeyboardAction(new ActionListener() {


			public void actionPerformed(ActionEvent arg0) {
				copyFromTree();
			}
		},"Copy2",copy2,JComponent.WHEN_FOCUSED);

		tree.registerKeyboardAction(new ActionListener() {


			public void actionPerformed(ActionEvent arg0) {
				pasteToTree();
			}
		},"Paste",paste,JComponent.WHEN_FOCUSED);

		tree.registerKeyboardAction(new ActionListener() {


			public void actionPerformed(ActionEvent arg0) {
				pasteToTree();
			}
		},"Paste2",paste2,JComponent.WHEN_FOCUSED);


		KeyStroke del = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0,false);

		table.registerKeyboardAction(new ActionListener() {


			public void actionPerformed(ActionEvent arg0) {
				actionTarget = getSelectedFromTable();
				deletActionSet();
			}
		},"Delete",del,JComponent.WHEN_FOCUSED);

		tree.registerKeyboardAction(new ActionListener() {


			public void actionPerformed(ActionEvent arg0) {
				actionTarget = getSelectedFromTree();
				deletActionSet();
			}
		},"Delete",del,JComponent.WHEN_FOCUSED);



	}

	protected void checkForClipboardContent() {
		Transferable t = clipboard.getContents(this);

		if( t != null ) {
			List<FileSource> list = getTransferData(t);
			if( list != null && list.size() > 0) {
				itemPaste.setEnabled(true);
			} else {
				itemPaste.setEnabled(false);
			}

		}
	}



	protected void pasteToActionSet() {
		System.out.println("pasteToActionSet");
		if( actionTarget == null || actionTarget.files.size() == 0 ) {
			System.out.println("no place to paste");
		} else if( actionTarget.files.size() > 1 ) {
			System.out.println("too many paste destination files sz="+actionTarget.files.size());
		} else {
			Transferable t = clipboard.getContents(this);
			if( t != null ) {
				List<FileSource> list = getTransferData(t);
				if( list != null && list.size() > 0) {
					FileSource dst = actionTarget.files.get(0);
					getCopyProgressPanel().setVisible(true);
					getCopyProgressPanel().addCopy(list, dst);

					clipboard.setContents(new Transferable() {
						public DataFlavor[] getTransferDataFlavors() {
							return new DataFlavor[0];
						}

						public boolean isDataFlavorSupported(DataFlavor flavor) {
							return false;
						}

						public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
							throw new UnsupportedFlavorException(flavor);
						}
					},this);

				} else {
					System.out.println("pasteToActionSet No data to paste");
				}
			}
		}
		actionTarget = null;
	}

	@SuppressWarnings("unchecked")
	public List<FileSource> getTransferData(Transferable t) {
		java.util.List<FileSource> files = null;
		try {
			files = (java.util.List<FileSource>)t.getTransferData(FileSourceTransferable.fileSourceFlavor);
		} catch (UnsupportedFlavorException | IOException e) {
		}
		if( files == null) {
			try {
				List<File> l = (java.util.List<File>)t.getTransferData(DataFlavor.javaFileListFlavor);
				if( l != null && l.size() > 0 ) {
					files = new ArrayList<FileSource>();
					for (File f : l) {						
						files.add(FileProxyFactory.fileProxyFactory.createFileSource(f.getCanonicalPath()));
					}
				}
			} catch (UnsupportedFlavorException | IOException e1) {
			}			
		}


		return files;
	}

	protected void copyActionSet() {

		if( actionTarget != null ) {
			if( actionTarget.files.size() > 0 ) {
				clipboard.setContents(createTransferable(actionTarget.files),this);
			}
		}

		actionTarget = null;
	}

	protected void backupActionSet() {
		if( actionTarget != null ) {
			if( actionTarget.files.size() > 0 ) {
				clipboard.setContents(createTransferable(actionTarget.files),this);
			}
		}

		actionTarget = null;
	}

	protected void pasteToTree() {
		System.out.println("pasteToTree");
		actionTarget = getSelectedFromTree();
		pasteToActionSet();
	}

	protected void copyFromTree() {
		System.out.println("copyToTree");
		actionTarget = getSelectedFromTree();
		copyActionSet();
	}

	protected void pasteToTable() {
		System.out.println("pasteToTable");
		actionTarget = getSelectedFromTable();
		pasteToActionSet();

	}

	protected void copyFromTable() {
		actionTarget = getSelectedFromTable();
		copyActionSet();
	}

	protected void deletActionSet() {


		if( actionTarget != null && actionTarget.files.size() >0 ) {
			try {
				MessageDialog dialog = new MessageDialog();

				boolean doAll = false;
				boolean yes = false;
				boolean multi = actionTarget.files.size()>1;
				// only directories effect the tree
				List<FileSource> directories = new ArrayList<>();

				for(FileSource f : actionTarget.files) {
					if( !doAll ) {
						String msg = null;
						if( f.isDirectory()) {
							msg = "Are you sure you want to delete this directory\nand all of it's children\n\n"+f.getAbsolutePath();
						} else {
							msg = "Are you sure you want to delete this file?\n\n"+f.getAbsolutePath();
						}
						dialog.setShowAppliesToAll(multi);
						yes = dialog.showWarningDialog(msg)==MessageDialog.Response.Yes;	
						doAll = dialog.applyToAll();
					}
					if( yes) {
						//delete a file or dir. 
						//if dir has kids they are deleted as well
						if( f.isDirectory()) {
							directories.add(f);
						}
						deleteAll(f);
					}
				}

				DefaultTreeModel model = (DefaultTreeModel) getTree().getModel();
				FileSourceTreeNode root = (FileSourceTreeNode) model.getRoot();

				for(FileSource f : directories) {
					if( !f.exists()) {
						FileSourceTreeNode node = searchNode(root, f.toString());
						model.removeNodeFromParent(node);
					}
				}

				FileTableModel tableModel = (FileTableModel) getTable().getModel();
				tableModel.resetIFile();

			} catch (IOException e) {
				showError( "Error in delete",e);
			}

		} else {
			//System.out.println("No action set");
		}
		actionTarget = null;
	}

	private void deleteAll(FileSource file) throws IOException {
		if( file.isDirectory() ) {
			FileSource[] list = file.listFiles();
			for (FileSource f : list) {
				deleteAll(f);
			}
		} 
		if(!file.delete()) {
			throw new IOException("Delete failed");
		}
	}



	public void forceRefresh() {


		JTree tree = getTree();
		TreePath [] path = tree.getSelectionPaths();
		if( path != null) {
			for (TreePath tp : path) {
				Object node = tp.getLastPathComponent();
				if (node instanceof FileSourceTreeNode) {
					FileSourceTreeNode tn = (FileSourceTreeNode) node;

					try {
						if( !tn.getDir().exists()) {
							DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
							model.removeNodeFromParent(tn);
						} else {
							tn.reset();
						}
					} catch (IOException e) {
						showError("Error in refresh",e);
					}
				}
			}
		}
		if( path != null && path.length>0) {			
			setSelectionPath(path[0]);
			if( path.length > 1 ) {
				tree.setSelectionPaths(path);
			}
		}
		TreeModel treeModel = tree.getModel();
		if (treeModel instanceof DefaultTreeModel) {
			DefaultTreeModel dtm = (DefaultTreeModel) treeModel;
			dtm.reload();
		}
		FileTableModel tableModel = (FileTableModel) getTable().getModel();
		setTableModel(tableModel.getDir());


	}

	public void setSelectionPath(TreePath treePath ) {
		if( treePath != null ) {
			JTree tree = getTree();
			tree.setSelectionPath(treePath);
			tree.expandPath(treePath);
			tree.scrollPathToVisible(treePath);			
		}
	}

	protected Transferable createTransferable(List<FileSource> files) {

		Transferable ret = new FileSourceTransferable(files);

		return ret;
	}

	public ActionContext getSelectedFromTable() {
		ArrayList<FileSource> files = new ArrayList<FileSource>();
		JTable table = getTable();
		TableModel m1 = table.getModel();
		if( m1 instanceof FileTableModel) {
			FileTableModel m = (FileTableModel)m1;
			for (int row : table.getSelectedRows()) {
				files.add(m.getFile(table.convertRowIndexToModel(row)));
			}
		}
		return new ActionContext(table,files);
	}

	public ActionContext getSelectedFromTree() {
		ArrayList<FileSource> files = new ArrayList<FileSource>();
		JTree tree = getTree();
		TreePath [] path = tree.getSelectionPaths();
		if( path != null) {
			for (TreePath tp : path) {
				Object node = tp.getLastPathComponent();
				if (node instanceof FileSourceTreeNode) {
					FileSourceTreeNode tn = (FileSourceTreeNode) node;
					files.add(tn.getDir());
				}
			}
		}
		return new ActionContext(tree,files);
	}



	public void showError(final String title, final Throwable e) {
		if( SwingUtilities.isEventDispatchThread()) {

			String msg = "";
			if( e != null ) {
				msg = e.getMessage();
			}
			String t2 = title;
			if( t2 == null ) {
				t2 = "";
			}
			MessageDialog.showErrorDialog(msg, t2);
		} else {
			SwingUtilities.invokeLater(()->showError(title, e));
		}
	}


	public void lostOwnership(Clipboard arg0, Transferable arg1) {
		//Nothing to do...

	}

	public void refreshTableIfNeeded(FileSource file) {
		FileSource target = file;
		try {
			if(target.isFile()) {
				target = file.getParentFile();
			}
			FileTableModel model =  (FileTableModel) getTable().getModel();
			if( model.getDir().equals(target)) {
				final FileSource t2 = target;
				SwingUtilities.invokeLater(()->{
					getTable().setModel(new FileTableModel(t2, showHidden, showExtention));
				});
			}
		} catch (IOException e) {
			showError("", e);
		}
	}

	List<FileSource> getDirList(FileSource dir) throws IOException {
		List<FileSource> ret = new ArrayList<>();
		for(FileSource kid: dir.listFiles()) {
			if( kid.isDirectory()) {
				ret.add(kid);
			}
		}
		return ret;
	}

	/**
	 *   
	 *   
	 * @param dir
	 */
	public FileSourceTreeNode addTreeNode(FileSource dir) {

		try {
			// check if it already exists
			DefaultTreeModel model = (DefaultTreeModel) getTree().getModel();
			FileSourceTreeNode ret = findNode(dir);
			if( ret == null) {
				// not find or create the parent node
				FileSource pdir = dir.getParentFile();
				FileSourceTreeNode pnode = findNode(pdir);

				if( pnode == null ) {
					pnode = addTreeNode(pdir);
				}


				ret = new FileSourceTreeNode(dir);

				FileSource [] kids = dir.listFiles();
				if( kids != null) {
					for(FileSource kid : dir.listFiles()) {
						if( kid.isDirectory()) {
							ret.add(new FileSourceTreeNode(kid));
						}
					}
				}
				final FileSourceTreeNode parent = pnode;
				final FileSourceTreeNode newChild = ret;
				SwingUtilities.invokeAndWait(()->{
					model.insertNodeInto(newChild, parent, parent.getChildCount());
					model.reload(parent);
				});
				// if the parent is the current directory, update it
				FileTableModel m = (FileTableModel) getTable().getModel();
				FileSource cwd = m.getDir();
				if( cwd.equals(pdir)) {
					SwingUtilities.invokeAndWait(()->{
						getTable().setModel(new FileTableModel(pdir, showHidden, showExtention));
					});					
				}				
			} else {
				model.reload(ret.getParent());
			}

			return ret;
		} catch (IOException e) {
			showError("addTreeNode", e);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}


		return null;
	}

	public void updateModelFor(FileSource file)  {
		if( SwingUtilities.isEventDispatchThread()) {
			try {
				if( file.isDirectory()) {
					addTreeNode(file);
				}
				FileTableModel m = (FileTableModel) getTable().getModel();
				FileSource cwd = m.getDir();
				if( cwd.equals(cwd) || cwd.equals(file.getParentFile())) {
					SwingUtilities.invokeLater(()->{
						m.fireTableDataChanged();
					});					
				} 
			} catch (Exception e) {
				showError("", e);
			}
		} else {
			SwingUtilities.invokeLater(()->{
				updateModelFor(file);
			});
		}
	}

	public void open(FileSource file) {
		if (file instanceof FileProxy) {
			File local = ((FileProxy) file).getTarget();								
			try {
				Desktop.getDesktop().open(local);
			} catch (IOException e) {
				MessageDialog.showErrorDialog(e.getMessage(),"Can't open "+local+"\n"+e);
			}
		} else {
			IRegistry reg = IRegistry.getRegistry();
			List<RegData> list = reg.getRegisteredHandler(file.getAbsolutePath(),CommandType.Viewer);
			if( list == null || list.size()==0) {
				MessageDialog.showMessageDialog("There are no applications registered for "+file, "");
			} else {

				RegData editor = list.get(0); 
				File local1 = new File(getTempDir(),""+System.currentTimeMillis()+file.getName());
				FileSource local;
				try {
					local = FileSourceFactory.getDefaultFactory().createFileSource(local1.getAbsolutePath());					
					if( confirmLargeDownload(file, true) ) { new EditMonitorThread(this, file,local, editor).start(); }					
				} catch (IOException e) {
					showError("", e);
				}

			}
		}

	}



}

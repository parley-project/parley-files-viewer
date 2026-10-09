package us.bringardner.parley.files.viewer.test;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;

public class TestTreeModle extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTable table;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TestTreeModle frame = new TestTreeModle();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	boolean showHidden = false;
	boolean showExtention = false;

	class FileSourceTreeNode extends DefaultMutableTreeNode {
		private static final long serialVersionUID = 1L;

		boolean queried = false;
		FileSource dir;
		String shortName;
		String displayName;

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
		}

		@Override
		public String toString() {				
			return showExtention?displayName:shortName;
		}

		@Override
		public boolean getAllowsChildren() {
			return !isTraversable();
		}

	
		@Override
		public boolean isLeaf() {
			if(!queried) {
				query();
			}
			return super.isLeaf();
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

		void query() {

			if(isTraversable()) {				
				try {

					FileSource[] list = dir.listFiles();
					if( list != null ) {
						for(FileSource f :  list ) {
							if( f.isDirectory() ) {
								boolean hide = f.isHidden() || f.getName().startsWith(".");
								if( showHidden || !hide) {
									FileSourceTreeNode node = new FileSourceTreeNode(f);
									add(node);
								}
							}
						}
					}			
				} catch(IOException ex) {
					ex.printStackTrace();
				}
			}
			queried = true;
		}
	}

	/**
	 * Create the frame.
	 * @throws IOException 
	 */
	public TestTreeModle() throws IOException {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));

		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.WEST);
		FileSourceTreeNode root = new FileSourceTreeNode(FileSourceFactory.getDefaultFactory().createFileSource("/"));
		
		JTree tree = new JTree();
		tree.setModel(new DefaultTreeModel(root));
		scrollPane.setViewportView(tree);

		JScrollPane scrollPane_1 = new JScrollPane();
		contentPane.add(scrollPane_1, BorderLayout.CENTER);

		table = new JTable();
		scrollPane_1.setViewportView(table);
	}

}

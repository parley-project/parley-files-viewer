package us.bringardner.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;

import us.bringardner.core.BaseThread;
import us.bringardner.io.filesource.FileSource;
import us.bringardner.io.filesource.viewer.FileSourceViewer;
import us.bringardner.swing.MessageDialog.Response;
import us.bringardner.io.IoUtils;

public class CopyProgressPanel extends JPanel implements FileSourceViewer.CopyProgressListner {
	private static int idTracker = 0;
	private synchronized static int nextId() {
		return ++idTracker;
	}

	private int bufferSize=1024;

	private  class CopyThread extends BaseThread {


		private Transaction transaction;

		public CopyThread(Transaction tx) {
			this.transaction = tx;
		}

		public void run() {
			running = started = true;
			InputStream in = null;
			OutputStream out = null;

			byte [] buffer = new byte[bufferSize];
			try {

				FileSource target = transaction.dest;
				if( transaction.dest.isDirectory()) {
					target = transaction.dest.getChild(transaction.source.getName());
				}
				in = transaction.source.getInputStream();
				out = target.getOutputStream();
				long bytesCopied = 0;
				copyStarted(transaction.source.length());
				while(!stopping && running ) {
					if(isCanceled() || transaction.canceled) {
						stop();
						return;
					}
					while(isPaused()) {
						Thread.sleep(10);
						if(isCanceled() || transaction.canceled) {
							stop();
							return;
						}
					}
					int got = in.read(buffer);
					if( got < 0 ) {
						stop();	
					} else if( got > 0) {
						if(isCanceled()) {
							stop();
							continue;
						}
						while(isPaused()) {
							Thread.sleep(10);
							if(isCanceled() || transaction.canceled) {
								stop();
								return;
							}
						}


						out.write(buffer, 0, got);
						bytesCopied+=got;
						updateProgress(bytesCopied);

						if(isCanceled()) {
							stop();
						}
						while(isPaused()) {
							Thread.sleep(10);
							if(isCanceled() || transaction.canceled) {
								stop();
								return;
							}
						}										
					}
				}

			} catch (Exception e) {
				error(e);				
			} finally {
				if( out!=null ) {
					IoUtils.closeQuietly(out);
				}
				if( in!=null ) {
					IoUtils.closeQuietly(in);
				}
				copyComplete();
				running = false;												
			}
		}
	}


	private class Transaction {
		@SuppressWarnings("unused")
		long id=nextId();
		long startTime;
		long endTime;
		long expected;
		FileSource source;
		FileSource dest;
		boolean canceled;
		Exception error;
		long bytesCopied;


	}

	private List<FileSource> nodesToAdd = new ArrayList<>();
	private static String [] colNames = {"Destination","StartTime","EndTime","Status"};
	public class CopyFileTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		@Override
		public int getRowCount() {
			return transactions.size();
		}

		@Override
		public int getColumnCount() {
			return colNames.length;
		}

		@Override
		public String getColumnName(int column) {
			return colNames[column];
		}
		private SimpleDateFormat fmt = new SimpleDateFormat("hh:mm:ss a");

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Transaction tx = transactions.get(rowIndex);
			switch (columnIndex) {
			case 0: return tx.dest;
			case 1: return tx.startTime==0?"":fmt.format(new Date(tx.startTime));
			case 2: return tx.endTime==0?"":fmt.format(new Date(tx.endTime));
			case 3: 
				if( tx.canceled) {
					return "Cancled";
				} else if( tx.error!=null) {
					return tx.error.getLocalizedMessage();
				} 
				return tx.expected==0?"":""+((int)((tx.bytesCopied/tx.expected)*100.0))+"%";

			default:
				break;
			}
			return null;
		}

	}

	class MonitorThread extends BaseThread {
		CopyThread copyThread;


		@Override
		public void run() {
			started = running = true;
			long last = System.currentTimeMillis();

			while(!stopping) {
				try {
					if(copyThread == null || !copyThread.isRunning()) {
						if( currentTransaction !=null ) {
							if( viewer !=null) {
								viewer.refreshTableIfNeeded(currentTransaction.dest);
								currentTransaction=null;
							}
						}
						if( isCanceled()) {
							for(Transaction tx : waitingTransaction) {
								tx.canceled=true;
							}
							waitingTransaction.clear();
							SwingUtilities.invokeLater(()->{
								progressBar.setValue(progressBar.getMaximum());
								progressBarFiles.setValue(progressBarFiles.getMaximum());
								hideOrShowCancelButton(false);
								if( pauseCheckBox.isSelected()) {
									pauseCheckBox.setSelected(false);
								}
							});

						}

						if( waitingTransaction.size()>0) {

							Transaction tx = waitingTransaction.remove(0);
							if( tx !=null ) {
								last = System.currentTimeMillis();
								currentTransaction = tx;
								SwingUtilities.invokeLater(()->{
									descriptionLabel.setText(tx.dest.getAbsolutePath());
									progressBarFiles.setValue(progressBarFiles.getValue()+1);
									cancelCurrentButton.setToolTipText("Cancel "+tx.dest.getName());
									hideOrShowCancelButton(true);
								});

								copyThread = new CopyThread(currentTransaction);
								copyThread.start();
								long start = System.currentTimeMillis();

								while (!copyThread.hasStarted()) {
									Thread.sleep(5);
									if( System.currentTimeMillis()-start>200) {
										System.out.println("Slow start copy thread");
									}
								}

							}
						} else {							
							SwingUtilities.invokeLater(()->{
								hideOrShowCancelButton(false);
								if( pauseCheckBox.isSelected()) {
									pauseCheckBox.setSelected(false);
								}
							});


							//  no TX to process. maybe stop???
							long time = System.currentTimeMillis()-last;
							if( time > maxIdelTime) {
								stop();
							}
						}
					}

					Thread.sleep(100);
				} catch (Throwable e) {					
				}

			} // while

			running = false;
		}

	}

	private long maxIdelTime=300000;// 5 minutes
	private MonitorThread monitorThread = new MonitorThread();

	private List<Transaction> transactions= new ArrayList<>();
	private List<Transaction> waitingTransaction= new ArrayList<>();
	private static final long serialVersionUID = 1L;
	private JLabel descriptionLabel;
	private JProgressBar progressBar;
	private JButton expandCollapseButton;
	private boolean canceled;
	private long expected;
	private JPanel centerPanel;
	private FileSourceViewer viewer;
	private boolean expanded = true;
	private JCheckBox pauseCheckBox;
	private boolean paused=false;
	private Transaction currentTransaction;
	private JProgressBar progressBarFiles;
	private JTable table;
	private JPanel cancelOnePanel;
	private JButton cancelCurrentButton;
	private JButton cancelButton;
	private JButton clearButton;
	private JPanel panel_1;
	private JButton hideButton;






	/**
	 * Create the panel.
	 */
	public CopyProgressPanel() {
		setLayout(new BorderLayout(0, 0));

		JPanel controlPanel = new JPanel();
		add(controlPanel, BorderLayout.SOUTH);
		controlPanel.setLayout(new BorderLayout(0, 0));

		expandCollapseButton = new JButton("");
		expandCollapseButton.setToolTipText("Show Details");
		expandCollapseButton.setBorderPainted(false);
		controlPanel.add(expandCollapseButton, BorderLayout.WEST);
		expandCollapseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionExpandCollapse();
			}
		});
		expandCollapseButton.setIcon(new ImageIcon(CopyProgressPanel.class.getResource("/us/bringardner/io/filesource/viewer/icons/UpArrow20x20.png")));

		JPanel panel = new JPanel();
		controlPanel.add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout(0, 0));

		progressBar = new JProgressBar();
		panel.add(progressBar, BorderLayout.SOUTH);
		progressBar.setStringPainted(true);

		progressBarFiles = new JProgressBar();
		progressBarFiles.setStringPainted(true);
		panel.add(progressBarFiles, BorderLayout.NORTH);

		panel_1 = new JPanel();
		controlPanel.add(panel_1, BorderLayout.EAST);

		cancelButton = new JButton("");
		panel_1.add(cancelButton);
		cancelButton.setToolTipText("Cancel all");
		cancelButton.setBorderPainted(false);
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionCancel();
			}
		});
		cancelButton.setIcon(new ImageIcon(CopyProgressPanel.class.getResource("/us/bringardner/io/filesource/viewer/icons/Cancel30x30.png")));

		hideButton = new JButton("");
		hideButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionHide();
			}
		});
		hideButton.setVisible(false);
		hideButton.setBorderPainted(false);
		hideButton.setToolTipText("Close History");
		hideButton.setIcon(new ImageIcon(CopyProgressPanel.class.getResource("/us/bringardner/io/filesource/viewer/icons/eye_closed30x30.png")));
		panel_1.add(hideButton);

		JPanel topPanel = new JPanel();
		add(topPanel, BorderLayout.NORTH);
		topPanel.setLayout(new BorderLayout(0, 0));

		descriptionLabel = new JLabel("/asdadsad/dfsgdf/jghhj/hkkjk");
		topPanel.add(descriptionLabel);

		JPanel pausePanel = new JPanel();
		topPanel.add(pausePanel, BorderLayout.WEST);

		JLabel lblNewLabel = new JLabel("Pause");
		pausePanel.add(lblNewLabel);

		pauseCheckBox = new JCheckBox("");
		pauseCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				paused = pauseCheckBox.isSelected();
			}
		});
		pauseCheckBox.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				paused = pauseCheckBox.isSelected();
			}
		});
		pausePanel.add(pauseCheckBox);

		cancelOnePanel = new JPanel();
		topPanel.add(cancelOnePanel, BorderLayout.EAST);

		cancelCurrentButton = new JButton("");
		cancelCurrentButton.setEnabled(false);
		cancelCurrentButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionCancelOne();
			}
		});
		cancelCurrentButton.setBorderPainted(false);
		cancelCurrentButton.setToolTipText("Cancel current copy");
		cancelCurrentButton.setIcon(new ImageIcon(CopyProgressPanel.class.getResource("/us/bringardner/io/filesource/viewer/icons/Cancel30x30.png")));
		cancelOnePanel.add(cancelCurrentButton);

		clearButton = new JButton("");
		clearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				actionClear();
			}
		});
		clearButton.setVisible(false);
		clearButton.setBorderPainted(false);
		clearButton.setToolTipText("Clear History");
		clearButton.setIcon(new ImageIcon(CopyProgressPanel.class.getResource("/us/bringardner/io/filesource/viewer/icons/sweep_clear30x30.png")));
		cancelOnePanel.add(clearButton);

		centerPanel = new JPanel();
		add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BorderLayout(0, 0));

		JScrollPane scrollPane = new JScrollPane();
		centerPanel.add(scrollPane, BorderLayout.CENTER);

		table = new JTable(new CopyFileTableModel());		
		scrollPane.setViewportView(table);
		scrollPane.setOpaque(false);
		scrollPane.getViewport().setOpaque(false);
		table.setBackground(FileSourceViewer.TRANSPARENT);
		table.setOpaque(false);
		table.setBackground(new Color(255, 255, 255, 0));

	}

	public void hideOrShowCancelButton(boolean showCancel) {
		cancelCurrentButton.setVisible(showCancel);
		cancelButton.setVisible(showCancel);
		hideButton.setVisible(!showCancel);
		clearButton.setVisible(!showCancel);

	}

	protected void actionClear() {
		transactions.clear();
		((CopyFileTableModel)table.getModel()).fireTableDataChanged();
	}

	protected void actionHide() {
		setVisible(false);		
	}

	protected void actionCancelOne() {
		if(currentTransaction!=null) {
			currentTransaction.canceled=true;
		}		
	}

	public FileSourceViewer getViewer() {
		return viewer;
	}

	public void setViewer(FileSourceViewer viewer) {
		this.viewer = viewer;
	}

	protected void actionCancel() {
		canceled = true;		
	}

	protected void actionExpandCollapse() {		
		setExpanded(!expanded);
	}

	@Override
	public void copyStarted(long expected) {
		if( SwingUtilities.isEventDispatchThread()) {
			this.expected = expected;
			if(currentTransaction!=null ) {
				currentTransaction.expected = expected;
				currentTransaction.startTime = System.currentTimeMillis();
			}
			//System.out.println("Set expected="+expected);
			updateProgress(0);
			setVisible(true);
			updateUI();
		} else {
			SwingUtilities.invokeLater(()->copyStarted(expected));
		}
	}

	@Override
	public void updateProgress(long bytesCopied) {
		if( SwingUtilities.isEventDispatchThread()) {
			int per = (int)(((double)bytesCopied)/((double)expected)*100.0);
			progressBar.setValue(per);
			if(currentTransaction!=null ) {
				currentTransaction.bytesCopied = bytesCopied;
			}
		} else {
			SwingUtilities.invokeLater(()->updateProgress(bytesCopied));
		}

	}

	public boolean close() {
		monitorThread.stop();

		return true;	
	}

	@Override
	public void copyComplete() {
		if( SwingUtilities.isEventDispatchThread()) {
			if(currentTransaction!=null ) {
				currentTransaction.endTime = System.currentTimeMillis();
				if( isCanceled()) {
					currentTransaction.canceled = true;
				}
				((CopyFileTableModel)table.getModel()).fireTableDataChanged();				
				viewer.updateModelFor(currentTransaction.dest);
			}

		} else {
			SwingUtilities.invokeLater(()->copyComplete());
		}


	}

	@Override
	public void error(Exception e) {
		if(currentTransaction!=null ) {
			currentTransaction.error = e;
		}
		if( viewer == null ) {
			e.printStackTrace();
		} else {
			viewer.showError("Error in copy", e);
		}

	}

	@Override
	public boolean isCanceled() {
		return canceled;
	}

	public void setExpanded(boolean b) {
		if( expanded != b ) {
			if( SwingUtilities.isEventDispatchThread()) {
				expanded = b;
				if( b ) {
					add(centerPanel, BorderLayout.CENTER);
				} else {
					remove(centerPanel);
				}
				expandCollapseButton.setIcon(expanded?FileSourceViewer.dnArrow:FileSourceViewer.upArrow);
				updateUI();
			} else {
				SwingUtilities.invokeLater(()->setExpanded(b));
			}			
		}
	}

	@Override
	public boolean isPaused() {
		return paused;
	}

	@Override
	public void setDescription(String description) {


		if( SwingUtilities.isEventDispatchThread()) {
			descriptionLabel.setText(description);
		} else {
			SwingUtilities.invokeLater(()->setDescription(description));
		}

	}


	private void addCopy(FileSource source, FileSource dst,MessageDialog dialog) throws IOException {

		if( source.isFile()) {

			Transaction tx = new Transaction();
			tx.source = source;			
			tx.dest = dst;	
			if( dst.isDirectory()) {
				tx.dest = dst.getChild(source.getName());
			}
			if( tx.dest.exists()) {
				if(!dialog.applyToAll()) {
					dialog.setMessage(tx.dest.getAbsolutePath()+" already exists. Overwrite it?");

					Response ok = dialog.showDialogThreadSafe();
					if( ok == Response.No) {
						return;
					}
				}
			}
			transactions.add(tx);
			waitingTransaction.add(tx);
			progressBarFiles.setMaximum(waitingTransaction.size());
			((CopyFileTableModel)table.getModel()).fireTableDataChanged();
		} else if(source.isDirectory()){
			if( dst.isFile()) {
				System.out.println("Source="+source);
				System.out.println("dest="+dst);
				throw new IOException("Can't copy a directory to a file");
			}
			FileSource dstDir = dst.getChild(source.getName());
			if( !dstDir.exists()) {
				if( !dstDir.mkdirs()) {
					throw new IOException(dstDir+" in not a valid directory");
				} else {
					nodesToAdd.add(dstDir);
				}
			}

			FileSource kids[] = source.listFiles();
			if( kids != null ) {
				for(FileSource kid : kids) {
					addCopy(kid,dstDir,dialog);
				}
			}
		} else {
			throw new IOException(source+" is not a valid file source");
		}		
	}


	public void addCopy(List<FileSource> list, FileSource dst) {
		if( viewer == null) {
			System.out.println("Null viewer");
			IOException e = new IOException();
			e.printStackTrace();
		}
		if( SwingUtilities.isEventDispatchThread() ) {
			// don't process in dispatch thread
			new Thread(()->addCopy1(list, dst)).start();
		} else {
			addCopy1(list, dst);
		}
	}

	/**
	 * This should never be in he dispatch thread
	 * @param list
	 * @param dst
	 */
	private void addCopy1(List<FileSource> list, FileSource dst) {
		nodesToAdd.clear();
		boolean p = paused;
		paused = true;
		MessageDialog dialog = new MessageDialog();
		dialog.setShowAppliesToAll(list.size()>1);
		if( waitingTransaction.size()==0) {
			transactions.clear();
		}

		for(FileSource file : list) {
			try {
				if(file.isDirectory()) {
					dialog.setShowAppliesToAll(true);
				}
				addCopy(file, dst,dialog);
			} catch (IOException e) {
				if( viewer!=null ) {
					viewer.showError("", e);
				} else {
					e.printStackTrace();
				}
			}							
		}	

		if( nodesToAdd.size()>0) {
			for(FileSource file: nodesToAdd) {
				viewer.addTreeNode(file);
			}
		}

		paused = p;
		if( !monitorThread.isRunning()) {
			monitorThread.start();
		}		

	}

	public void setRowSize(int h) {
		table.setRowHeight(h);		
	}

}

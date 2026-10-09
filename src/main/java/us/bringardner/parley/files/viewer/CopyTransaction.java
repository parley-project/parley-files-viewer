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
 * ~version~V000.01.39-V000.01.08-V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.SwingUtilities;

import us.bringardner.parley.files.FileSource;

public class CopyTransaction implements FileSourceViewer.CopyProgressListner {
	private long startTime;
	private long endTime;
	private long expected;
	private FileSource source;
	private FileSource dest;
	private Exception error;
	private long bytesCopied;
	private String startLabel = "";
	private String endLabel = "";
	private String statusLabel;
	private CopyFileTableModel model;
	
	private SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	private int row;
	private FileSourceViewer browser;
	
	public CopyTransaction(FileSource source, FileSource dest, CopyFileTableModel model, FileSourceViewer browser) throws IOException {
		this.source = source;
		this.dest = dest;
		this.expected = source.length();
		this.model = model;
		//this.row = model.getRowCount();	
		this.browser = browser;
		System.out.println("source="+source);
		System.out.println("dest="+dest);
	}
	


	public double percentComplete() {
		double ret = 100.0;
		if( expected > 0 ) {
			ret = (((double)bytesCopied/(double)expected)*100.0);
		}
		return ret;
	}

	public String getStatusLabel() {
		if( statusLabel == null ) {
			statusLabel = String.format("%3.2f%%", percentComplete());
		}
		return statusLabel;
	}
	
	
	public String getStartLabel() {
		return startLabel;
	}




	public String getEndLabel() {
		return endLabel;
	}




	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public long getExpected() {
		return expected;
	}

	public FileSource getSource() {
		return source;
	}

	public FileSource getDest() {
		return dest;
	}

	public Exception getError() {
		return error;
	}

	public long getBytesCopied() {
		return bytesCopied;
	}

	public void setBytesCopied(long bytesCopied) {
		this.bytesCopied = bytesCopied;
	}

	
	public void copyStarted() {
		statusLabel = null;
		startTime = System.currentTimeMillis();		
		startLabel = fmt.format(new Date(startTime));
		SwingUtilities.invokeLater(new Runnable() {
			
			public void run() {
				CopyManagerFrame.singleton.getStatusLabelWest().setText(source.getAbsolutePath());
				CopyManagerFrame.singleton.getStatusLabelEast().setText(dest.getAbsolutePath());
				CopyManagerFrame.singleton.getProgressBar().setValue(0);
			}
		});
		updateModel();
	}

	private void updateModel() {
		model.fireTableRowsUpdated(row, row);
		//model.fireTableDataChanged();
		SwingUtilities.invokeLater(new Runnable() {
			
			public void run() {
				CopyManagerFrame.singleton.getProgressBar().setValue((int)percentComplete());
			}
		});
		
	}



	
	public void updateProgress(long bytesCopied) {
		this.bytesCopied = bytesCopied;	
		statusLabel = null;
		updateModel();
	}

	
	public void copyComplete() {
		endTime = System.currentTimeMillis();	
		endLabel = fmt.format(new Date(endTime));
		model.fireTableRowsUpdated(row, row);
		browser.forceRefresh();
		System.out.println("Copy completed");
	}

	
	public void error(Exception e) {
		error= e;
		model.fireTableRowsUpdated(row, row);
	}



	
	public boolean isCanceled() {
		return false;
	}



	@Override
	public void copyStarted(long bytesExpected) {
		expected = bytesExpected;
		
	}



	@Override
	public boolean isPaused() {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public void setDescription(String description) {
		// TODO Auto-generated method stub
		
	}



	

}

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

import java.awt.EventQueue;
import java.io.IOException;

import us.bringardner.parley.files.FileSource;

public class CopyManagerFrame extends CopyManagerFrameBase {
	private static final long serialVersionUID = 1L;

	private static CopyThread thread = null;// TODO: new CopyThread();
	public static final CopyManagerFrame singleton = new CopyManagerFrame();
	
	public static void addCopy(FileSource source, FileSource dest, FileSourceViewer browser) throws IOException {
		CopyTransaction tx = new CopyTransaction(source, dest,singleton.getModel(),browser);
		singleton.add(tx);	
	}
	
	private CopyFileTableModel model = new CopyFileTableModel();
	
	public CopyManagerFrame() {
		super();
		getProgressBar().setValue(0);
		getStatusLabelWest().setText("");
		getStatusLabelEast().setText("");
		getTable().setModel(model);
		thread.start();
	}
	
	public void add(CopyTransaction tx) {
		model.addCopy(tx);
		if( !isShowing() ) {
			setVisible(true);
		}
	}
	
	public CopyFileTableModel getModel() {
		return model;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					CopyManagerFrame frame = new CopyManagerFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

}

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
 * ~version~V000.01.22-V000.01.10-V000.01.08-V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import us.bringardner.parley.core.BaseThread;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.sftp.SftpFileSourceFactory;
import us.bringardner.parley.files.viewer.FileSourceViewer.CopyProgressListner;
import us.bringardner.parley.files.viewer.IRegistry.CommandType;
import us.bringardner.parley.files.viewer.IRegistry.RegData;
import us.bringardner.swing.MessageDialog;

/**
 * If the file is remote we'll download a copy and monitor
 * it for changes.
 * 
 * @author Tony Bringardner
 *
 */
public class EditMonitorThread extends BaseThread implements CopyProgressListner {

	private  class OpenInstance  {

		FileSource local;
		FileSource remote;
		FileSourceViewer browser;
		@SuppressWarnings("unused")
		RegData editor;
		long modTime;
		@SuppressWarnings("unused")
		long lastUserResponse;
		Process process;
		Throwable error;
		AtomicBoolean inProsecc = new AtomicBoolean(false);

		public OpenInstance(FileSourceViewer browser, FileSource remote, FileSource local, RegData editor,Process process,long modDate) {
			this.browser = browser;
			this.remote = remote;
			this.local  = local;
			this.editor = editor;
			this.process = process;			
			this.modTime = modDate;
		}

	}

	/**
	 * We must use the queue method because the MacOS the process.waitFor method does not return because the open command will not terminate until the application closes , not the session  
	 */
	private static List<OpenInstance> queue = new ArrayList<>();
	private static class QueueThread extends BaseThread {
		int adminFreq = 2000;

		@Override
		public void run() {
			started = running = true;
			List<Integer> complete = new ArrayList<>();
			while(running) {
				complete.clear(); 
				synchronized (queue) {


					for(int idx=0,sz = queue.size(); idx < sz; idx++) {
						OpenInstance inst = queue.get(idx);
						if( inst.error != null ) {
							complete.add(idx);
							continue;
						}
						if( inst.inProsecc.get()) {
							continue;
						}
						try {
							if(inst.local.lastModified() != inst.modTime) {								
								uploadIfWanted(inst);
							}
							if(!inst.process.isAlive()) {
								if(inst.local.lastModified() > inst.remote.lastModified()) {								
									uploadIfWanted(inst);
								}	
								complete.add(idx);
							}
						} catch (IOException e) {
							complete.add(idx);
						}
					}
					for(Integer idx : complete) {
						queue.remove(idx.intValue());
					}
				}
				try {
					Thread.sleep(adminFreq);
				} catch (InterruptedException e) {
				}
			}

		}

	}
	private static QueueThread queueThread = new QueueThread();

	public static void main(String [] args) throws IOException {
		String src = "/data/services/home/bringardner.us/Tony/MySql/mysql-5.5.8-win32.zip";
		src = "/data/services/home/bringardner.us/TheAnswer.txt";
		src = "/data/services/home/bringardner.us/Test.txt";
		String dst = "/Volumes/Data/home/Test.txt";
		IRegistry reg = IRegistry.getRegistry();
		List<RegData> list = reg.getRegisteredHandler(dst,CommandType.Editor);

		SftpFileSourceFactory factory = new SftpFileSourceFactory();
		Properties prop = factory.getConnectProperties();
		prop.setProperty(SftpFileSourceFactory.PROP_HOST, "test.bringardner.us");
		prop.setProperty(SftpFileSourceFactory.PROP_USER, "tony");
		prop.setProperty(SftpFileSourceFactory.PROP_PASSWORD, "0000");
		if( !factory.connect(prop)) {
			System.out.println("Can't connect");			
		} else {
			FileSource fs = factory.createFileSource(src);
			FileSource fd = FileSourceFactory.getDefaultFactory().createFileSource(dst);
			FileSourceViewer viewer = new FileSourceViewer();
			EditMonitorThread mt = new EditMonitorThread(viewer, fs, fd,list.get(0));
			mt.setDaemon(true);
			mt.start();
			while(!mt.hasStarted()) {
				try {
					Thread.sleep(10);				
				} catch (InterruptedException e) {
				}
			}
			while( mt.isRunning()) {
				try {
					Thread.sleep(10);				
				} catch (InterruptedException e) {
				}
			}
			//System.exit(0);
		}
	}

	public static void uploadIfWanted(OpenInstance inst) throws IOException {
		int res = JOptionPane.showConfirmDialog(inst.browser.getFrame(), "The local copy of "+inst.remote.getName()+" has changed.\nWould you like to replace the original file?");
		inst.lastUserResponse = System.currentTimeMillis();
		if( res == JOptionPane.OK_OPTION) {
			final CopySingleDialog dialog = new CopySingleDialog();
			dialog.setTitle("Upload");
			
			final CopyThread copythread = new CopyThread(inst.local,inst.remote, dialog);
			inst.inProsecc.set(true);
			new Thread(()->{
				download(copythread,dialog);
				inst.inProsecc.set(false);
				try {
					inst.modTime = inst.local.lastModified();
				} catch (IOException e) {
					inst.error = e;
				}
			}).start();
		} else {
				inst.modTime = inst.local.lastModified();
			
		}
	}

	private FileSource local;
	private FileSource remote;
	private FileSourceViewer browser;
	private RegData editor;
	private AtomicBoolean canceled = new AtomicBoolean(false);
	private AtomicBoolean paused   = new AtomicBoolean(false);
	private AtomicBoolean copyComplete   = new AtomicBoolean(false);


	public EditMonitorThread(FileSourceViewer browser,FileSource remote, FileSource local,RegData editor) {
		this.local = local;
		this.remote = remote;
		this.browser = browser;
		this.editor = editor;
	}



	/**
	 *  
	 */
	public void run() {
		started = running = true;
		//long maxWait = 30000;
		try {
			// 1) Start download in a separate thread
			// 2) If download takes longer than xx open dialog for user interaction
			// 3) once download is complete, start the edit process
			// 4) monitor the process an wait for it to terminate
			// 5) if local file has changed after the edit process competes, upload to remote site
			CopySingleDialog dialog = new CopySingleDialog();
			dialog.setTitle("Download ");
			CopyThread copythread = new CopyThread(remote, local, dialog);
			download(copythread,dialog);
			if( !dialog.isCanceled()) {
				long lastMod = local.lastModified();

				Process process = startEditor();

				if( process == null ) {
					stop();				
					MessageDialog.showMessageDialog( "Could not start editor "+editor.name+" for "+remote.getName()+"\ntry 'open' instead.","");
				} else {

					OpenInstance ins = new OpenInstance(browser,remote,local,editor,process,lastMod);
					synchronized (queue) {
						queue.add(ins);
					}
					if( !queueThread.isRunning()) {
						queueThread.setName("QueueThread");
						queueThread.start();
					}
				}
			}
		} catch (IOException e) {			
			MessageDialog.showErrorDialog(e.getMessage(), "Uploading file");
		}	
		running = false;
	}


	private static void download(CopyThread copythread, CopySingleDialog dialog) {
		long start = System.currentTimeMillis();
		copythread.start();
		try {
			while(!copythread.hasStarted()) {
				Thread.sleep(10);
			}
			while( copythread.isRunning()) {
				Thread.sleep(10);				
				if( !dialog.isVisible()) {
					long time = System.currentTimeMillis()-start;
					if( time > 2000) {
						SwingUtilities.invokeLater(()->{dialog.setVisible(true);});						
					}
				}
			}
		} catch (InterruptedException e) {
			System.out.println(e);
		}

		SwingUtilities.invokeLater(()->{dialog.dispose();});
	}


	private Process startEditor() {
		Process ret = null;
		String path = local.getAbsolutePath();
		try {
			ProcessBuilder b = new ProcessBuilder().command(editor.getCommand()+" "+path);
			ret = b.start();
		} catch (IOException e) {
			logDebug("Error executing "+editor.name, e);
		}



		return ret;
	}


	@Override
	public void setDescription(String description) {
		// TODO Auto-generated method stub

	}


	@Override
	public void copyStarted(long bytesExpected) {
		// TODO Auto-generated method stub

	}


	@Override
	public void updateProgress(long bytesCopied) {
		// TODO Auto-generated method stub

	}


	@Override
	public void copyComplete() {
		copyComplete.set(true);		
	}


	@Override
	public void error(Exception e) {
		// TODO Auto-generated method stub

	}


	@Override
	public boolean isCanceled() {
		return canceled.get();
	}


	@Override
	public boolean isPaused() {
		return paused.get();
	}


}

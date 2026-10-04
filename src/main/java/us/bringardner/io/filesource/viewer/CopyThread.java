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
package us.bringardner.io.filesource.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import us.bringardner.core.BaseThread;
import us.bringardner.io.filesource.FileSource;
import us.bringardner.io.filesource.FileSourceFactory;
import us.bringardner.io.filesource.fileproxy.FileProxy;
import us.bringardner.io.filesource.sftp.SftpFileSourceFactory;
import us.bringardner.io.filesource.viewer.FileSourceViewer.CopyProgressListner;
import us.bringardner.io.IoUtils;

public class CopyThread extends BaseThread {

	public static void main(String [] args) throws IOException {
		String src = "/data/services/home/bringardner.us/Tony/MySql/mysql-5.5.8-win32.zip";
		String dst = "/Volumes/Data/home/mysql-5.5.8-win32.zip";
		
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
			
			CopyProgressListner l = new CopyProgressListner() {
				private boolean canceled = false;
				private boolean paused = false;
				
				long startTime;
				long lastTime;
				long size;
				long copied;
				String desc;
				//String msg = "%d%   55MB   8.4MB/s  mysql-5.5.8-win32.zip";
				String msg = "%2.2f%% %dMB %2.2fMB/s %s";
				
				
				@Override
				public void updateProgress(long bytesCopied) {
					copied = bytesCopied;
					lastTime = System.currentTimeMillis();
					
					double perc = ((double)copied/(double)size)*100.0;
					
					
					long time = lastTime-startTime;
					double speedMbits = (copied) / (time / 1000.0)/ 1_000_000;
				        
					int mb = (int)(copied/(1024*1024));
					
					System.out.println(String.format(msg, perc,mb,speedMbits,desc));
					
					
				}
				
				@Override
				public void setDescription(String description) {
					desc = description;
					
				}
				
				@Override
				public boolean isPaused() {
					return paused;
				}
				
				@Override
				public boolean isCanceled() {
					return canceled;
				}
				
				@Override
				public void error(Exception e) {
					System.out.println(e);					
				}
				
				@Override
				public void copyStarted(long bytesExpected) {
					size = bytesExpected;
					startTime = System.currentTimeMillis();
				}
				
				@Override
				public void copyComplete() {
					System.out.println("Complete");
					System.exit(0);
				}
			};
			CopyThread ct = new CopyThread(fs, fd, l);
			ct.setDaemon(true);
			ct.start();
			
			/*
			byte [] data = new byte[1024*10];
			try(InputStream in = fs.getInputStream()) {
				try( OutputStream out = fd.getOutputStream()) {
					int got = in.read(data);
					while( got >= 0 ) {
						if( got > 0 ) {
							out.write(data, 0, got);
						}
						got = in.read(data);
					}
				}
			}
			*/
		}
		
	}
	
	private FileSource source;
	private FileSource dest;
	private FileSourceViewer.CopyProgressListner listener;
	private int bufferSize=1024*100;

	
	public CopyThread(FileSource source, FileSource dest, FileSourceViewer.CopyProgressListner listener) {
		this.source = source;
		this.dest  = dest;
		this.listener = listener;
		if( listener !=null) {
			if (source instanceof FileProxy) {
				// the FileProxy is local and has an ugly name
				listener.setDescription(dest.getName());
			} else {
				listener.setDescription(source.getName());
			}
		}
	}

	public void run() {
		running = started = true;
		InputStream in = null;
		OutputStream out = null;
		
		byte [] buffer = new byte[bufferSize];
		try {
			
			FileSource target = dest;
			if( dest.isDirectory()) {
				target = dest.getChild(source.getName());
			}
			in = source.getInputStream();
			out = target.getOutputStream();
			long bytesCopied = 0;
			
			if( listener != null ) {
				listener.copyStarted(source.length());
			}
			while(!stopping && running ) {
				if( listener !=null ) {
					if(listener.isCanceled()) {
						stop();
						continue;
					}
					while(listener.isPaused() && !listener.isCanceled()) {
						Thread.sleep(10);
					}
				}
				int got = in.read(buffer);
				if( got < 0 ) {
					stop();	
				} else if( got > 0) {
					if( listener !=null ) {
						if(listener.isCanceled()) {
							stop();
							continue;
						}
						while(listener.isPaused() && !listener.isCanceled()) {
							Thread.sleep(10);
						}
					}
					
					out.write(buffer, 0, got);
					bytesCopied+=got;
					if( listener != null ) {
						listener.updateProgress(bytesCopied);
					}										
				}
			}

		} catch (Exception e) {
			if( listener != null ) {
				listener.error(e);
			} else {
				e.printStackTrace();
			}
		} finally {
			if( out!=null ) {
				IoUtils.closeQuietly(out);
			}
			if( in!=null ) {
				IoUtils.closeQuietly(in);
			}
			running = false;
			if( listener != null ) {
				listener.copyComplete();
			}
		}				
	}

}

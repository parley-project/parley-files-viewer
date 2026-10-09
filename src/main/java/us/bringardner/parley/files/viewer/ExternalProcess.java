package us.bringardner.parley.files.viewer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import us.bringardner.parley.core.BaseThread;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.fileproxy.FileProxy;


public class ExternalProcess extends BaseThread{

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public static class StreamCopier extends BaseThread{
		
		InputStream in;
		OutputStream out;
		Throwable error;
		byte [] buffer = new byte[1024*10];
		StringBuilder debug = new StringBuilder();
		
		public StreamCopier(InputStream in, OutputStream out,String name) {
			this.in = in;
			this.out = out;
			setName(name);
			
		}

		@Override
		public void run() {
			started = running = true;
			try {
				int cnt = 0;
				while(running && !stopping && (cnt = in.read(buffer))>=0) {
					if( cnt > 0 ) {
						debug.append(new String(buffer,0,cnt));
						out.write(buffer, 0, cnt);						
					}
				}
			} catch (Throwable e) {
				error = e;
				stop();
			} finally {
				try {
						in.close();
						thread.interrupt();
				} catch (Exception e2) {}
			}

			running = false;

		}

		@Override
		public void stop() {
			super.stop();
			try {
				thread.interrupt();

			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


	}

	
	
	List<String> cmd;
	int exitCode;
	Throwable error;
	FileSourceFactory factory;
	
	public ExternalProcess(List<String> cmd) {
		this.cmd  = cmd;
		
		
	}

	@Override
	public void run() {
		running = started = true;
		StreamCopier sc2 = null;
		StreamCopier sc3 = null;

		try {
			String name = cmd.get(0);
			ProcessBuilder builder = new ProcessBuilder(cmd);

			FileSource dir = factory.getCurrentDirectory();
			if (dir instanceof FileProxy) {
				FileProxy cwd = (FileProxy) dir;
				builder.directory(cwd.getTarget());
			}

			Process p = builder.start();
			
			ByteArrayOutputStream stdout = new ByteArrayOutputStream();
			sc2 = new StreamCopier(p.getInputStream(),stdout,name+" stdout");
			ByteArrayOutputStream stderr = new ByteArrayOutputStream();
			sc3 = new StreamCopier(p.getErrorStream(),stderr,name+" stderr");
			
			sc2.start();
			sc3.start();
			
			@SuppressWarnings("unused")
			int time = 0;
			while(p.isAlive()) {
				try {
					p.waitFor(1000, TimeUnit.MILLISECONDS);
					if( ++time > 3) { 
						//Console.System_out.println("ExternalProcess Waiting for "+(time*1000));
					}
				} catch (InterruptedException e) {
				}
			}
			exitCode = p.exitValue();


		} catch (Throwable e) {
			exitCode = 1;
			error = e;
		} finally {
			try {sc2.stop();}catch (Throwable e) {}
			try {sc3.stop();}catch (Throwable e) {}
		}

		running = false;

	}



}


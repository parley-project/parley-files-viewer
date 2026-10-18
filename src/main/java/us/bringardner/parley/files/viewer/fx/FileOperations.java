/**
 * Copyright Tony Bringardner 1998, 2026 <a href="http://bringardner.com/tony">Tony Bringardner</a>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package us.bringardner.parley.files.viewer.fx;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CancellationException;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceCopy;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.fileproxy.FileProxy;

/**
 * What the viewer does to files, with no user interface: delete, rename, new folder, and a
 * copy of a file on this machine (copying files into a folder is the {@link CopyQueue}'s). Long operations report to a {@link Progress}, which may cancel
 * them; they are meant to run off the JavaFX thread.
 */
final class FileOperations {

	/** Told about a long operation as it goes; also asked whether to stop. */
	interface Progress {
		/** The step the operation is on (a file name). */
		void step(String text, int done, int total);

		boolean isCanceled();

		Progress NONE = new Progress() {
			@Override public void step(String text, int done, int total) { }
			@Override public boolean isCanceled() { return false; }
		};
	}

	private FileOperations() {
	}

	/** Deletes file, and everything in it if it's a folder. */
	static void deleteAll(FileSource file, Progress progress) throws IOException {
		if( progress.isCanceled()) {
			throw new CancellationException("Delete canceled");
		}
		if( file.isDirectory()) {
			FileSource[] kids = file.listFiles();
			if( kids != null ) {
				for(FileSource kid : kids) {
					deleteAll(kid, progress);
				}
			}
		}
		progress.step(file.getName(), 0, 0);
		if( !file.delete()) {
			throw new IOException("Can't delete "+file.getAbsolutePath());
		}
	}

	/** Renames file within its own folder. */
	static FileSource rename(FileSource file, String newName) throws IOException {
		String name = newName.trim();
		if( name.isEmpty() || name.indexOf('/') >= 0 || name.indexOf(file.getFileSourceFactory().getSeparatorChar()) >= 0 ) {
			throw new IOException("\""+newName+"\" isn't a name");
		}
		FileSource parent = file.getParentFile();
		if( parent == null ) {
			throw new IOException("Can't rename "+file.getAbsolutePath());
		}
		FileSource dest = parent.getChild(name);
		if( dest.exists()) {
			throw new IOException(name+" already exists in "+parent.getAbsolutePath());
		}
		if( !file.renameTo(dest)) {
			throw new IOException("Can't rename "+file.getName()+" to "+name);
		}
		return dest;
	}

	/** Makes the folder name in dir. */
	static FileSource newFolder(FileSource dir, String name) throws IOException {
		String n = name.trim();
		if( n.isEmpty()) {
			throw new IOException("A folder needs a name");
		}
		FileSource folder = dir.getChild(n);
		if( folder.exists()) {
			throw new IOException(n+" already exists in "+dir.getAbsolutePath());
		}
		if( !folder.mkdirs()) {
			throw new IOException("Can't create "+n+" in "+dir.getAbsolutePath());
		}
		return folder;
	}

	/** True if file is a file on this machine's own file system. */
	static boolean isLocal(FileSource file) {
		return file instanceof FileProxy;
	}

	/**
	 * A file on this machine to give another application: a local file is itself; any other is
	 * copied under its own name to a new temporary folder, so the application chooses by the
	 * extension and shows the right title. The copy is removed when the viewer exits.
	 */
	static File localCopy(FileSource file) throws IOException {
		if( file instanceof FileProxy ) {
			return ((FileProxy) file).getTarget();
		}
		File dir = File.createTempFile("parley-open-", "");
		if( !dir.delete() || !dir.mkdirs()) {
			throw new IOException("Can't create a folder in the temporary directory");
		}
		File temp = new File(dir, file.getName());
		dir.deleteOnExit();
		temp.deleteOnExit();
		FileSource local = FileSourceFactory.getDefaultFactory().createFileSource(temp.getAbsolutePath());
		FileSourceCopy.copy(file, local);
		return temp;
	}
}

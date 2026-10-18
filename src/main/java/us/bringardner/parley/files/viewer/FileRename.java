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
package us.bringardner.parley.files.viewer;

import java.io.IOException;

import us.bringardner.parley.files.FileSource;

/** Renaming a file or folder in its own folder: the same in both viewers. */
public final class FileRename {

	private FileRename() {
	}

	/**
	 * Renames a file or folder, keeping it in the folder it's in.
	 *
	 * @param file the file or folder
	 * @param newName its new name (a name, not a path)
	 * @return the renamed file
	 * @throws IOException if the name isn't a name, is taken, or the rename fails
	 */
	public static FileSource rename(FileSource file, String newName) throws IOException {
		String name = newName == null ? "" : newName.trim();
		if( name.isEmpty() || name.indexOf('/') >= 0 || name.indexOf(file.getFileSourceFactory().getSeparatorChar()) >= 0 ) {
			throw new IOException("\""+newName+"\" isn't a name");
		}
		// in the file's own folder: a bare name given to the factory would be relative to its current
		// folder (the Swing viewer's rename moved files there)
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
}

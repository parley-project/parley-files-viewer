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

import java.io.File;
import java.io.IOException;

/**
 * Print, the same in both viewers: java.awt.Desktop's print on macOS and Windows; the lp command on
 * Linux, where Desktop doesn't print (and where starting AWT beside JavaFX would leave JavaFX unable
 * to show windows).
 */
public final class Printing {

	private Printing() {
	}

	/** True if files can be printed here. */
	public static boolean isAvailable() {
		if( isLinux() ) {
			return lp() != null;
		}
		try {
			return java.awt.Desktop.isDesktopSupported()
					&& java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.PRINT);
		} catch (RuntimeException | LinkageError e) {
			return false;
		}
	}

	/**
	 * Prints a file (of this computer) with the application the system has for it; waits for lp.
	 *
	 * @param file the file
	 * @throws IOException if it can't be printed
	 */
	public static void print(File file) throws IOException {
		if( isLinux() ) {
			File lp = lp();
			if( lp == null ) {
				throw new IOException("There's no lp command to print with");
			}
			Process p = new ProcessBuilder(lp.getAbsolutePath(), file.getAbsolutePath()).redirectErrorStream(true).start();
			String out = new String(p.getInputStream().readAllBytes());
			try {
				if( p.waitFor() != 0 ) {
					throw new IOException(out.trim().isEmpty() ? "lp failed" : out.trim());
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interrupted while printing", e);
			}
		} else {
			java.awt.Desktop.getDesktop().print(file);
		}
	}

	static boolean isLinux() {
		return System.getProperty("os.name", "").toLowerCase().startsWith("linux");
	}

	/** The lp command, or null where there isn't one. */
	static File lp() {
		for(String dir : new String[] {"/usr/bin", "/bin", "/usr/local/bin"}) {
			File f = new File(dir, "lp");
			if( f.canExecute() ) {
				return f;
			}
		}
		return null;
	}
}

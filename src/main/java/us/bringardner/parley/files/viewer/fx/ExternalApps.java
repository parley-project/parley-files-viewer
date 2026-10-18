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
import java.util.ArrayList;
import java.util.List;

/**
 * Starts the applications of this machine for a file, with its own commands rather than
 * java.awt.Desktop so no AWT is loaded beside JavaFX.
 */
final class ExternalApps {

	private ExternalApps() {
	}

	private static String os() {
		return System.getProperty("os.name", "").toLowerCase();
	}

	/** Opens a file with the application registered for it; the process ends when it has been handed over. */
	static Process open(File file) throws IOException {
		List<String> cmd = new ArrayList<>();
		String os = os();
		if( os.startsWith("mac")) {
			cmd.add("open");
		} else if( os.startsWith("windows")) {
			cmd.add("cmd");
			cmd.add("/c");
			cmd.add("start");
			cmd.add("\"\"");
		} else {
			cmd.add("xdg-open");
		}
		cmd.add(file.getAbsolutePath());
		return new ProcessBuilder(cmd).redirectErrorStream(true).start();
	}

	/**
	 * Opens a file in a text editor, and (where the system can) waits until the editor is closed,
	 * so the returned process lives as long as the editing does.
	 */
	static Process edit(File file) throws IOException {
		List<String> cmd = new ArrayList<>();
		String os = os();
		if( os.startsWith("mac")) {
			cmd.add("open");
			cmd.add("-W");
			cmd.add("-t");
		} else if( os.startsWith("windows")) {
			cmd.add("cmd");
			cmd.add("/c");
			cmd.add("start");
			cmd.add("/wait");
			cmd.add("\"\"");
		} else {
			cmd.add("xdg-open");
		}
		cmd.add(file.getAbsolutePath());
		return new ProcessBuilder(cmd).redirectErrorStream(true).start();
	}
}

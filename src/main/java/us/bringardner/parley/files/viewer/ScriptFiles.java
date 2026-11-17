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
 * ~version~V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.Icon;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.fileproxy.FileProxy;
import us.bringardner.parley.files.viewer.IRegistry.RegData;

/**
 * Files that look like shell scripts, and the two Parley programs offered for them: fsh, to run
 * one (Open With...), and Fsh Studio, to edit one (Edit With...).
 * <p>
 * A file looks like a script by its name (.fsh, .sh, .bash, .zsh, .ksh) or, for a file on this
 * computer, by a first line that starts #! and names a shell.
 */
public final class ScriptFiles {

	private static final Set<String> EXTENSIONS = Set.of(".fsh", ".sh", ".bash", ".zsh", ".ksh");
	private static final Set<String> SHELLS = Set.of("sh", "bash", "zsh", "ksh", "dash", "fsh");
	/** How much of a file is read to see its first line. */
	private static final int FIRST_LINE_BYTES = 256;

	private ScriptFiles() {
	}

	/** @return true if a file name ends as a shell script's does */
	public static boolean isScriptName(String name) {
		String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
		for(String ext : EXTENSIONS) {
			if( lower.endsWith(ext) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param line a file's first line
	 * @return true if it is a #! line that names a shell, directly (#!/bin/bash) or through env
	 *         (#!/usr/bin/env bash)
	 */
	public static boolean isShellShebang(String line) {
		if( line == null || !line.startsWith("#!") ) {
			return false;
		}
		String[] words = line.substring(2).trim().split("\\s+");
		if( words.length == 0 || words[0].isEmpty() ) {
			return false;
		}
		String program = baseName(words[0]);
		if( program.equals("env") ) {
			// env's own options (-S, -i, NAME=value) come before the program
			for(int idx = 1; idx < words.length; idx++) {
				if( !words[idx].startsWith("-") && words[idx].indexOf('=') < 0 ) {
					program = baseName(words[idx]);
					break;
				}
			}
		}
		return SHELLS.contains(program);
	}

	private static String baseName(String path) {
		return path.substring(path.lastIndexOf('/') + 1);
	}

	/**
	 * @return true if the file looks like a shell script: by its name, or (only for a file on this
	 *         computer, so nothing is downloaded) by its first line
	 */
	public static boolean looksLikeScript(FileSource file) {
		if( file == null ) {
			return false;
		}
		if( isScriptName(file.getName()) ) {
			return true;
		}
		if( !(file instanceof FileProxy) ) {
			return false;
		}
		try {
			if( !file.isFile() ) {
				return false;
			}
			try (InputStream in = file.getInputStream()) {
				byte[] buf = in.readNBytes(FIRST_LINE_BYTES);
				String text = new String(buf, StandardCharsets.ISO_8859_1);
				int end = text.indexOf('\n');
				String line = end < 0 ? text : text.substring(0, end);
				return isShellShebang(line.trim());
			}
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Finds the parley command (the launcher): on the PATH, else where its installer puts it (a
	 * program started from the desktop often has no /usr/local/bin on its PATH).
	 *
	 * @return its full path, or null if it isn't installed
	 */
	public static String findParley() {
		return findParley(System.getenv("PATH"), System.getProperty("user.home"));
	}

	public static String findParley(String path, String home) {
		List<String> dirs = new ArrayList<>();
		if( path != null ) {
			for(String dir : path.split(File.pathSeparator)) {
				if( !dir.isEmpty() ) {
					dirs.add(dir);
				}
			}
		}
		dirs.add("/usr/local/bin");
		dirs.add("/opt/homebrew/bin");
		if( home != null ) {
			dirs.add(new File(home, ".local/bin").getPath());
		}
		boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
		String[] names = windows ? new String[] {"parley.cmd", "parley.exe", "parley.bat"} : new String[] {"parley"};
		for(String dir : dirs) {
			for(String name : names) {
				File f = new File(dir, name);
				if( f.isFile() && f.canExecute() ) {
					return f.getAbsolutePath();
				}
			}
		}
		return null;
	}

	/** A program that starts nothing outside Parley: no icon and no command of its own. */
	private abstract static class ParleyApp extends RegData {
		ParleyApp(String name, String id) {
			super(name, id);
		}

		@Override
		public List<Icon> getIcons() throws IOException {
			return new ArrayList<>();
		}

		@Override
		public Icon getIcon(int width, int height) throws IOException {
			return null;
		}
	}

	/** fsh, offered by Open With... for a script: runs it in a terminal window (a file on this computer). */
	public static final class FshApp extends ParleyApp {
		public FshApp() {
			super("fsh", "parley:fsh");
		}

		@Override
		public String getCommand() {
			return "fsh";
		}
	}

	/** Fsh Studio, offered by Edit With... for a script: started by the parley command. */
	public static final class StudioApp extends ParleyApp {
		private final String parley;

		/** @param parley the parley command's path (see {@link ScriptFiles#findParley()}) */
		public StudioApp(String parley) {
			super("Fsh Studio", "parley:fshstudio");
			this.parley = parley;
		}

		@Override
		public String getCommand() {
			return parley + " fshstudio";
		}

		@Override
		public List<String> getCommandLine(String file) {
			List<String> ret = new ArrayList<>();
			ret.add(parley);
			ret.add("fshstudio");
			ret.add(file);
			return ret;
		}
	}

	/** A path as one word of an fsh command line: in single quotes. */
	public static String shellQuote(String text) {
		return "'" + text.replace("'", "'\\''") + "'";
	}
}

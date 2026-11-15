/**
 * <PRE>
 * 
 * Copyright Tony Bringarder 1998, 2026 <A href="http://bringardner.com/tony">Tony Bringardner</A>
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
 *	@author Tony Bringardner   
 */
package us.bringardner.parley.files.viewer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import us.bringardner.fsh.Console;
import us.bringardner.fsh.syntax.Parser;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.archive.format.ArchiveFormat;
import us.bringardner.parley.files.archive.format.ArchiveFormats;

/**
 * The right-click menu's Compress and Expand. The work is done by fsh's own archive commands
 * ({@code zip}, {@code unzip}, {@code tar}), run in a console of their own started in the folder
 * the files are in, so the viewer makes and opens archives exactly as the shell does, on any file
 * system (local or remote).
 * <p>
 * Those commands are fsh "utilities": where the system has a program of the same name (macOS,
 * Linux), the program runs unless fsh's are turned on, and a program can't see a remote folder
 * (it failed over SFTP with zip's "name not matched", status 12). So the console turns them on
 * first ({@link #USE_FSH_COMMANDS}).
 * <p>
 * As the Finder does: Compress puts {@code name.zip} (or {@code Archive.zip} for several items)
 * beside what was compressed; Expand puts an archive's files in a new folder beside it, named after
 * it without its extension ({@code photos.tar.gz} → {@code photos}). Neither replaces anything: a
 * name that is taken gets a number ({@code photos 2}).
 */
class ArchiveActions {

	/** Run first in the console: fsh's own zip, unzip, tar (and the rest), never the system's programs. */
	static final String USE_FSH_COMMANDS = "shopt -s fshutils";

	/** The name of the archive Compress makes from more than one item. */
	static final String SEVERAL_ITEMS_NAME = "Archive";

	private ArchiveActions() {
	}

	/** @return the format of the archive file's name (its extension), or null if it isn't one we expand */
	static ArchiveFormat formatOf(FileSource file) throws IOException {
		if( file == null || !file.isFile()) {
			return null;
		}
		return ArchiveFormats.forFileName(file.getName());
	}

	/** @return true if the file is an archive Expand can open (zip and its kin, tar, tar.gz, tgz) */
	static boolean isArchive(FileSource file) throws IOException {
		return formatOf(file) != null;
	}

	/**
	 * Zips the files (which are all in folder) into a new zip archive in folder.
	 *
	 * @return the archive made
	 * @throws IOException with zip's own message when it fails
	 */
	static FileSource compress(FileSource folder, List<FileSource> files) throws IOException {
		String base = files.size() == 1 ? files.get(0).getName() : SEVERAL_ITEMS_NAME;
		String zipName = unique(folder, base, ".zip");
		StringBuilder script = new StringBuilder("zip -rq ").append(quote(zipName));
		for(FileSource f : files) {
			script.append(' ').append(quote(f.getName()));
		}
		run(folder, script.toString());
		return folder.getChild(zipName);
	}

	/**
	 * Expands the archive into a new folder beside it.
	 *
	 * @return the folder made
	 * @throws IOException with the command's own message when it fails
	 */
	static FileSource expand(FileSource archive) throws IOException {
		ArchiveFormat format = formatOf(archive);
		if( format == null ) {
			throw new IOException(archive.getName()+" is not an archive this can expand");
		}
		FileSource folder = archive.getParentFile();
		String dir = unique(folder, folderName(archive.getName(), format), "");
		String extract = "zip".equals(format.getName())
				? "unzip -q -d "+quote(dir)+" "+quote(archive.getName())
				// (tar recognizes a gzipped archive itself)
				: "tar -xf "+quote(archive.getName())+" -C "+quote(dir);
		run(folder, "mkdir "+quote(dir)+" && "+extract);
		return folder.getChild(dir);
	}

	/** The archive's name without the extension its format knows it by: photos.tar.gz is photos. */
	static String folderName(String archiveName, ArchiveFormat format) {
		String lower = archiveName.toLowerCase(Locale.ROOT);
		String longest = "";
		for(String ext : format.getExtensions()) {
			if( lower.endsWith("."+ext) && ext.length() > longest.length()) {
				longest = ext;
			}
		}
		String name = longest.isEmpty() ? archiveName : archiveName.substring(0, archiveName.length()-longest.length()-1);
		return name.isEmpty() ? archiveName+" contents" : name;
	}

	/** base+ext if nothing in folder has that name, else "base 2"+ext, "base 3"+ext ... */
	static String unique(FileSource folder, String base, String ext) throws IOException {
		String name = base+ext;
		for(int n = 2; folder.getChild(name).exists(); n++) {
			name = base+" "+n+ext;
		}
		return name;
	}

	/** A name in the folder as one shell word, never read as an option: './it'\''s here' */
	static String quote(String name) {
		return "'./"+name.replace("'", "'\\''")+"'";
	}

	/**
	 * Runs script in a console of its own, started in folder.
	 *
	 * @throws IOException with what the commands wrote to standard error when the script fails
	 */
	static void run(FileSource folder, String script) throws IOException {
		// exit in a script must end the script, not the viewer
		Console.exitJvm = false;
		// a new console takes the parser's aliases and posix mode: given back after, for any open terminal
		Function<String,String> aliases = Parser.aliases;
		BooleanSupplier posix = Parser.posixMode;
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		try (PrintStream errStream = new PrintStream(err, true, StandardCharsets.UTF_8);
				PrintStream outStream = new PrintStream(OutputStream.nullOutputStream())) {
			Console console = new Console();
			console.setStdIn(InputStream.nullInputStream());
			console.setStdOut(outStream);
			console.setStdErr(errStream);
			console.startIn(folder);
			int status = console.executeScript(USE_FSH_COMMANDS+"\n"+script);
			errStream.flush();
			if( status != 0 ) {
				String message = err.toString(StandardCharsets.UTF_8).trim();
				throw new IOException(message.isEmpty() ? "failed (status "+status+")" : message);
			}
		} finally {
			Parser.aliases = aliases;
			Parser.posixMode = posix;
		}
	}
}

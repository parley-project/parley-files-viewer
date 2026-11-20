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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import us.bringardner.fsh.Console;
import us.bringardner.fsh.syntax.Parser;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.archive.FileSourceSources;
import us.bringardner.parley.files.archive.format.ArchiveFormat;
import us.bringardner.parley.files.archive.format.ArchiveFormats;
import us.bringardner.parley.files.archive.format.ArchiveReader;
import us.bringardner.parley.files.archive.format.SeekableSource;

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
 * beside what was compressed. Expand puts what the archive holds beside it: when that is one folder
 * or file ({@code docs.zip} holding {@code docs/}), that folder or file itself; otherwise a new
 * folder named after the archive without its extension ({@code photos.tar.gz} → {@code photos})
 * holding it all. Neither replaces anything: a name that is taken gets a number ({@code docs 2}).
 */
class ArchiveActions {

	/** Run first in the console: fsh's own zip, unzip, tar (and the rest), never the system's programs. */
	static final String USE_FSH_COMMANDS = "shopt -s fshutils";

	/**
	 * What Compress and Expand report as they go, for the progress window. Called on the thread
	 * doing the work.
	 */
	interface Progress {
		/** How many items there are, when that is known first (Compress counts them; a zip's index lists them). */
		default void total(int count) {
		}

		/** One more item (file or folder) done: how many are done so far, and its name. */
		default void item(int done, String name) {
		}

		/**
		 * How much of the archive has been read, in bytes, of its size: how far a tar's
		 * expansion has got (a tar has no index, so the number of items isn't known first).
		 */
		default void bytes(long done, long total) {
		}

		/** Reports nothing. */
		Progress NONE = new Progress() {
		};
	}

	/** zip's line for each item it adds: "  adding: docs/a.txt (deflated 40%)" */
	static final String ZIP_ADDING = "  adding: ";
	/** unzip's lines for each item it extracts: "   creating: docs/" and "  inflating: docs/a.txt" */
	static final String UNZIP_CREATING = "   creating: ", UNZIP_INFLATING = "  inflating: ";

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
		return compress(folder, files, Progress.NONE);
	}

	/**
	 * As {@link #compress(FileSource, List)}, reporting to progress: the files and folders are
	 * counted first (over a remote file system that is a listing of each folder), then each is
	 * reported as zip adds it.
	 */
	static FileSource compress(FileSource folder, List<FileSource> files, Progress progress) throws IOException {
		String base = files.size() == 1 ? files.get(0).getName() : SEVERAL_ITEMS_NAME;
		String zipName = unique(folder, base, ".zip");
		if( progress != Progress.NONE ) {
			int count = 0;
			for(FileSource f : files) {
				count += countItems(f);
			}
			progress.total(count);
		}
		// (not -q: zip writes a line for each item it adds, which is the progress)
		StringBuilder script = new StringBuilder("zip -r ").append(quote(zipName));
		for(FileSource f : files) {
			script.append(' ').append(quote(f.getName()));
		}
		AtomicInteger done = new AtomicInteger();
		run(folder, script.toString(), line->{
			if( line.startsWith(ZIP_ADDING) ) {
				String name = line.substring(ZIP_ADDING.length());
				int how = name.lastIndexOf(" (");
				progress.item(done.incrementAndGet(), how > 0 ? name.substring(0, how) : name);
			}
		});
		return folder.getChild(zipName);
	}

	/** The items zip -r makes of f: f, and everything in it when it is a folder (as zip, following a link to one). */
	static int countItems(FileSource f) throws IOException {
		int count = 1;
		if( f.isDirectory() ) {
			FileSource[] kids = f.listFiles();
			if( kids != null ) {
				for(FileSource k : kids) {
					count += countItems(k);
				}
			}
		}
		return count;
	}

	/** The start of the hidden folder an archive is expanded into first, beside it. */
	static final String EXPANDING_PREFIX = ".expanding-";

	/**
	 * Expands the archive beside it. It is expanded into a hidden folder first; then, if it held
	 * one folder or file, that is moved out beside the archive (so docs.zip holding docs/ gives
	 * docs, not docs/docs); otherwise the hidden folder is given the archive's name.
	 *
	 * @return the folder or file made
	 * @throws IOException with the command's own message when it fails
	 */
	static FileSource expand(FileSource archive) throws IOException {
		return expand(archive, Progress.NONE);
	}

	/**
	 * As {@link #expand(FileSource)}, reporting to progress each item as it is extracted; for a
	 * zip the total comes first, from its index. A tar has no index: it is given to tar on its
	 * standard input, and how much of it has been read is reported (its size is known).
	 */
	static FileSource expand(FileSource archive, Progress progress) throws IOException {
		ArchiveFormat format = formatOf(archive);
		if( format == null ) {
			throw new IOException(archive.getName()+" is not an archive this can expand");
		}
		FileSource folder = archive.getParentFile();
		String name = folderName(archive.getName(), format);
		String tmp = unique(folder, EXPANDING_PREFIX+name, "");
		boolean zip = "zip".equals(format.getName());
		if( zip && progress != Progress.NONE ) {
			// the index is read from the end: a few reads, however big the archive
			try (SeekableSource src = FileSourceSources.open(archive); ArchiveReader reader = format.openReader(src)) {
				progress.total(reader.getEntries().size());
			}
		}
		// (not quiet: unzip, and tar with -v, write a line for each item, which is the progress)
		String extract = zip
				? "unzip -d "+quote(tmp)+" "+quote(archive.getName())
				// (from standard input: the bytes read are counted; tar recognizes a gzipped archive itself)
				: "tar -xvf - -C "+quote(tmp);
		AtomicInteger done = new AtomicInteger();
		Consumer<String> lines = line->{
			String item = null;
			if( !zip ) {
				item = line;   // tar -v: the member's name
			} else if( line.startsWith(UNZIP_INFLATING) ) {
				item = line.substring(UNZIP_INFLATING.length());
			} else if( line.startsWith(UNZIP_CREATING) ) {
				item = line.substring(UNZIP_CREATING.length());
			}
			if( item != null && !item.isEmpty() ) {
				// (unzip names it with the hidden folder in front)
				String prefix = "./"+tmp+"/";
				progress.item(done.incrementAndGet(), item.startsWith(prefix) ? item.substring(prefix.length()) : item);
			}
		};
		FileSource tmpDir = folder.getChild(tmp);
		try (InputStream stdin = zip ? InputStream.nullInputStream() : new CountingInputStream(archive.getInputStream(), archive.contentLength(), progress)) {
			run(folder, "mkdir "+quote(tmp)+" && "+extract, lines, stdin);
		} catch (IOException e) {
			// what was expanded before it failed stays, where it can be seen
			if( tmpDir.exists() ) {
				moveTo(tmpDir, folder, unique(folder, name, ""));
			}
			throw e;
		}
		tmpDir.refresh();
		FileSource[] kids = tmpDir.listFiles();
		if( kids != null && kids.length == 1 ) {
			FileSource only = kids[0];
			String[] baseExt = only.isDirectory() ? new String[] {only.getName(), ""} : splitExtension(only.getName());
			FileSource made = moveTo(only, folder, unique(folder, baseExt[0], baseExt[1]));
			if( !tmpDir.delete() ) {
				throw new IOException("Expanded "+archive.getName()+" but could not remove "+tmp);
			}
			return made;
		}
		return moveTo(tmpDir, folder, unique(folder, name, ""));
	}

	/** Renames file into folder as name. */
	private static FileSource moveTo(FileSource file, FileSource folder, String name) throws IOException {
		FileSource to = folder.getChild(name);
		if( !file.renameTo(to) ) {
			throw new IOException("Could not rename "+file.getName()+" to "+name);
		}
		return to;
	}

	/** "report.pdf" is {"report", ".pdf"}; "README" and ".profile" have no extension. */
	static String[] splitExtension(String name) {
		int dot = name.lastIndexOf('.');
		return dot > 0 ? new String[] {name.substring(0, dot), name.substring(dot)} : new String[] {name, ""};
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
		run(folder, script, line->{});
	}

	/**
	 * As {@link #run(FileSource, String)}, giving each line the commands write to standard output
	 * to lines, as it is written.
	 */
	static void run(FileSource folder, String script, Consumer<String> lines) throws IOException {
		run(folder, script, lines, InputStream.nullInputStream());
	}

	/** As {@link #run(FileSource, String, Consumer)}, with stdin as the commands' standard input (the caller closes it). */
	static void run(FileSource folder, String script, Consumer<String> lines, InputStream stdin) throws IOException {
		// exit in a script must end the script, not the viewer
		Console.exitJvm = false;
		// a new console takes the parser's aliases and posix mode: given back after, for any open terminal
		Function<String,String> aliases = Parser.aliases;
		BooleanSupplier posix = Parser.posixMode;
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		try (PrintStream errStream = new PrintStream(err, true, StandardCharsets.UTF_8);
				PrintStream outStream = new PrintStream(new LineOutputStream(lines), true, StandardCharsets.UTF_8)) {
			Console console = new Console();
			console.setStdIn(stdin);
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

	/** Gives each line written to it (UTF-8, without its end) to a consumer. */
	static class LineOutputStream extends OutputStream {
		private final Consumer<String> lines;
		private final ByteArrayOutputStream line = new ByteArrayOutputStream();

		LineOutputStream(Consumer<String> lines) {
			this.lines = lines;
		}

		@Override
		public void write(int b) {
			if( b == '\n' ) {
				String text = line.toString(StandardCharsets.UTF_8);
				line.reset();
				lines.accept(text.endsWith("\r") ? text.substring(0, text.length()-1) : text);
			} else {
				line.write(b);
			}
		}

		@Override
		public void close() {
			if( line.size() > 0 ) {
				write('\n');
			}
		}
	}

	/** Reports to progress how many of a stream's bytes have been read, of its size. */
	static class CountingInputStream extends java.io.FilterInputStream {
		private final long total;
		private final Progress progress;
		private long done;

		CountingInputStream(InputStream in, long total, Progress progress) {
			super(in);
			this.total = total;
			this.progress = progress;
			progress.bytes(0, total);
		}

		@Override
		public int read() throws IOException {
			int b = super.read();
			if( b >= 0 ) {
				progress.bytes(++done, total);
			}
			return b;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			int n = super.read(b, off, len);
			if( n > 0 ) {
				done += n;
				progress.bytes(done, total);
			}
			return n;
		}

		@Override
		public long skip(long n) throws IOException {
			long skipped = super.skip(n);
			if( skipped > 0 ) {
				done += skipped;
				progress.bytes(done, total);
			}
			return skipped;
		}
	}
}

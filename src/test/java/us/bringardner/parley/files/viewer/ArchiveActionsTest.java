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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.archive.format.ArchiveFormats;
import us.bringardner.parley.files.archive.tar.TarArchiveEntry;
import us.bringardner.parley.files.archive.tar.TarWriter;
import us.bringardner.parley.files.memory.MemoryFileSourceFactory;

/**
 * The right-click menu's Compress and Expand (ArchiveActions), on local files and on a file system
 * that isn't local (memory, standing in for SFTP and the other remote ones: a program run by the
 * system can't see it, so only fsh's own commands can do the work there).
 */
public class ArchiveActionsTest {

	@TempDir
	Path dir;

	private FileSource fs(Path p) throws IOException {
		return FileSourceFactory.getDefaultFactory().createFileSource(p.toString());
	}

	private Path write(String rel, String text) throws IOException {
		Path p = dir.resolve(rel);
		Files.createDirectories(p.getParent());
		Files.writeString(p, text);
		return p;
	}

	/** every file under top, as path=text */
	private Set<String> contents(Path top) throws IOException {
		Set<String> ret = new TreeSet<>();
		try (Stream<Path> s = Files.walk(top)) {
			for(Path p : (Iterable<Path>) s::iterator) {
				if( Files.isRegularFile(p)) {
					ret.add(top.relativize(p).toString().replace('\\', '/')+"="+Files.readString(p));
				}
			}
		}
		return ret;
	}

	@Test
	public void compressAFolderThenExpandIt() throws Exception {
		write("src/a.txt", "A");
		write("src/sub/it's here.txt", "B");
		FileSource zip = ArchiveActions.compress(fs(dir), List.of(fs(dir.resolve("src"))));
		assertEquals("src.zip", zip.getName());
		assertTrue(Files.isRegularFile(dir.resolve("src.zip")));
		assertTrue(ArchiveActions.isArchive(zip));

		FileSource out = ArchiveActions.expand(zip);
		// the archive holds one folder, src: that is what comes out (not src/src), numbered as src is there
		assertEquals("src 2", out.getName());
		assertEquals(Set.of("a.txt=A", "sub/it's here.txt=B"), contents(dir.resolve("src 2")));
		assertNoneExpanding();
	}

	@Test
	public void severalItemsGoInArchiveZip() throws Exception {
		write("one.txt", "1");
		write("-two.txt", "2");
		FileSource zip = ArchiveActions.compress(fs(dir), List.of(fs(dir.resolve("one.txt")), fs(dir.resolve("-two.txt"))));
		assertEquals("Archive.zip", zip.getName());
		// and again: nothing is replaced
		FileSource zip2 = ArchiveActions.compress(fs(dir), List.of(fs(dir.resolve("one.txt"))));
		assertEquals("one.txt.zip", zip2.getName());
		FileSource zip3 = ArchiveActions.compress(fs(dir), List.of(fs(dir.resolve("one.txt")), fs(dir.resolve("-two.txt"))));
		assertEquals("Archive 2.zip", zip3.getName());

		FileSource out = ArchiveActions.expand(zip);
		// two things in it: they go in a folder named after the archive
		assertEquals("Archive", out.getName());
		assertEquals(Set.of("one.txt=1", "-two.txt=2"), contents(dir.resolve("Archive")));
	}

	@Test
	public void expandAGzippedTar() throws Exception {
		Path tgz = dir.resolve("photos.tar.gz");
		try (OutputStream file = Files.newOutputStream(tgz);
				OutputStream gz = new java.util.zip.GZIPOutputStream(file);
				TarWriter tar = new TarWriter(gz)) {
			byte [] bytes = "pixels".getBytes();
			try (OutputStream e = tar.putEntry(TarArchiveEntry.builder("trip/day1.jpg").size(bytes.length).build())) {
				e.write(bytes);
			}
		}
		FileSource f = fs(tgz);
		assertTrue(ArchiveActions.isArchive(f));
		FileSource out = ArchiveActions.expand(f);
		// one folder, trip, in it: trip comes out
		assertEquals("trip", out.getName());
		assertEquals(Set.of("day1.jpg=pixels"), contents(dir.resolve("trip")));
		assertNoneExpanding();
	}

	@Test
	public void oneFileComesOutBesideTheArchive() throws Exception {
		write("report.pdf", "R");
		FileSource zip = ArchiveActions.compress(fs(dir), List.of(fs(dir.resolve("report.pdf"))));
		FileSource out = ArchiveActions.expand(zip);
		// report.pdf is there: the number goes before the extension
		assertEquals("report 2.pdf", out.getName());
		assertEquals("R", Files.readString(dir.resolve("report 2.pdf")));
		assertNoneExpanding();
	}

	/** no hidden folder of an expansion is left behind */
	private void assertNoneExpanding() throws IOException {
		try (Stream<Path> s = Files.list(dir)) {
			assertFalse(s.anyMatch(p->p.getFileName().toString().startsWith(ArchiveActions.EXPANDING_PREFIX)));
		}
	}

	@Test
	public void extensions() {
		assertEquals(List.of("report", ".pdf"), List.of(ArchiveActions.splitExtension("report.pdf")));
		assertEquals(List.of("README", ""), List.of(ArchiveActions.splitExtension("README")));
		assertEquals(List.of(".profile", ""), List.of(ArchiveActions.splitExtension(".profile")));
	}

	@Test
	public void folderNames() {
		assertEquals("photos", ArchiveActions.folderName("photos.tar.gz", ArchiveFormats.forFileName("x.tar.gz")));
		assertEquals("photos", ArchiveActions.folderName("photos.TGZ", ArchiveFormats.forFileName("x.tgz")));
		assertEquals("app", ArchiveActions.folderName("app.jar", ArchiveFormats.forFileName("x.jar")));
		assertEquals(".zip contents", ArchiveActions.folderName(".zip", ArchiveFormats.forFileName("x.zip")));
	}

	@Test
	public void notAnArchive() throws Exception {
		FileSource txt = fs(write("notes.txt", "n"));
		assertFalse(ArchiveActions.isArchive(txt));
		assertFalse(ArchiveActions.isArchive(fs(dir)));
		assertThrows(IOException.class, ()->ArchiveActions.expand(txt));
	}

	@Test
	public void aDamagedArchiveSaysWhy() throws Exception {
		FileSource bad = fs(write("bad.zip", "not a zip at all"));
		IOException e = assertThrows(IOException.class, ()->ArchiveActions.expand(bad));
		assertFalse(e.getMessage().isEmpty());
	}

	/** a file of the memory file system, with text */
	private static FileSource write(FileSource file, String text) throws IOException {
		file.getParentFile().mkdirs();
		try (OutputStream out = file.getOutputStream()) {
			out.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
		}
		return file;
	}

	private static String read(FileSource file) throws IOException {
		try (java.io.InputStream in = file.getInputStream()) {
			return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
	}

	@Test
	public void onAFileSystemThatIsNotLocal() throws Exception {
		MemoryFileSourceFactory memory = new MemoryFileSourceFactory();
		memory.connect();
		try {
			FileSource home = memory.createFileSource("/home/tony");
			home.mkdirs();
			write(home.getChild("docs/a.txt"), "A");
			write(home.getChild("docs/sub/b.txt"), "B");

			// (over SFTP this failed with zip's status 12: the system's zip ran, and can't see the folder)
			FileSource zip = ArchiveActions.compress(home, List.of(home.getChild("docs")));
			assertEquals("docs.zip", zip.getName());
			assertTrue(zip.exists());

			FileSource out = ArchiveActions.expand(zip);
			assertEquals("docs 2", out.getName());
			assertEquals("A", read(out.getChild("a.txt")));
			assertEquals("B", read(out.getChild("sub/b.txt")));

			// a gzipped tar
			FileSource tgz = home.getChild("photos.tgz");
			try (OutputStream file = tgz.getOutputStream();
					OutputStream gz = new java.util.zip.GZIPOutputStream(file);
					TarWriter tar = new TarWriter(gz)) {
				byte [] bytes = "pixels".getBytes();
				try (OutputStream e = tar.putEntry(TarArchiveEntry.builder("trip/day1.jpg").size(bytes.length).build())) {
					e.write(bytes);
				}
			}
			FileSource photos = ArchiveActions.expand(tgz);
			assertEquals("trip", photos.getName());
			assertEquals("pixels", read(photos.getChild("day1.jpg")));
			for(String n : home.list()) {
				assertFalse(n.startsWith(ArchiveActions.EXPANDING_PREFIX), n);
			}

			// a failure says what the command said
			FileSource bad = write(home.getChild("bad.zip"), "not a zip at all");
			IOException e = assertThrows(IOException.class, ()->ArchiveActions.expand(bad));
			assertFalse(e.getMessage().startsWith("failed (status"), e.getMessage());
		} finally {
			memory.disConnect();
		}
	}

	/** What a Compress or Expand reported. */
	static class Recorded implements ArchiveActions.Progress {
		Integer total;
		long bytesDone = -1, bytesTotal = -1;
		final List<String> names = new java.util.ArrayList<>();
		final List<Integer> counts = new java.util.ArrayList<>();

		@Override
		public void total(int count) {
			total = count;
		}

		@Override
		public void item(int done, String name) {
			counts.add(done);
			names.add(name);
		}

		@Override
		public void bytes(long done, long total) {
			assertTrue(done >= bytesDone, "bytes read never go back");
			bytesDone = done;
			bytesTotal = total;
		}

		/** the counts went 1, 2, 3 ... */
		void assertCounted() {
			for(int i = 0; i < counts.size(); i++) {
				assertEquals(i+1, counts.get(i));
			}
		}
	}

	@Test
	public void progressOfAZip() throws Exception {
		write("src/a.txt", "A");
		write("src/sub/b.txt", "B");
		Recorded compressed = new Recorded();
		FileSource zip = ArchiveActions.compress(fs(dir), List.of(fs(dir.resolve("src"))), compressed);
		// counted first: src, a.txt, sub and b.txt; then each as it was added
		assertEquals(4, compressed.total);
		assertEquals(Set.of("src/", "src/a.txt", "src/sub/", "src/sub/b.txt"), new TreeSet<>(compressed.names));
		compressed.assertCounted();

		Recorded expanded = new Recorded();
		ArchiveActions.expand(zip, expanded);
		// the total from the zip's index; the names as they are in the archive (not the hidden folder's)
		assertEquals(4, expanded.total);
		assertEquals(Set.of("src/", "src/a.txt", "src/sub/", "src/sub/b.txt"), new TreeSet<>(expanded.names));
		expanded.assertCounted();
	}

	@Test
	public void progressOfATar() throws Exception {
		Path tgz = dir.resolve("photos.tgz");
		try (OutputStream file = Files.newOutputStream(tgz);
				OutputStream gz = new java.util.zip.GZIPOutputStream(file);
				TarWriter tar = new TarWriter(gz)) {
			for(String n : List.of("trip/day1.jpg", "trip/day2.jpg")) {
				byte [] bytes = n.getBytes();
				try (OutputStream e = tar.putEntry(TarArchiveEntry.builder(n).size(bytes.length).build())) {
					e.write(bytes);
				}
			}
		}
		Recorded expanded = new Recorded();
		ArchiveActions.expand(fs(tgz), expanded);
		// a tar has no index: no total, but how much of it has been read, of its size
		assertEquals(null, expanded.total);
		assertEquals(Files.size(tgz), expanded.bytesTotal);
		assertTrue(expanded.bytesDone > 0 && expanded.bytesDone <= expanded.bytesTotal, expanded.bytesDone+" of "+expanded.bytesTotal);
		assertEquals(List.of("trip/day1.jpg", "trip/day2.jpg"), expanded.names);
		expanded.assertCounted();
		assertEquals("trip/day1.jpg", read(fs(dir.resolve("trip/day1.jpg"))));
	}

	@Test
	public void progressOnAFileSystemThatIsNotLocal() throws Exception {
		MemoryFileSourceFactory memory = new MemoryFileSourceFactory();
		memory.connect();
		try {
			FileSource home = memory.createFileSource("/home/tony");
			home.mkdirs();
			write(home.getChild("docs/a.txt"), "A");
			write(home.getChild("docs/b.txt"), "B");
			Recorded compressed = new Recorded();
			FileSource zip = ArchiveActions.compress(home, List.of(home.getChild("docs")), compressed);
			assertEquals(3, compressed.total);
			assertEquals(3, compressed.names.size(), compressed.names.toString());
			Recorded expanded = new Recorded();
			ArchiveActions.expand(zip, expanded);
			assertEquals(3, expanded.total);
			assertEquals(Set.of("docs/", "docs/a.txt", "docs/b.txt"), new TreeSet<>(expanded.names));
		} finally {
			memory.disConnect();
		}
	}

	@Test
	public void whatTheProgressWindowSays() {
		assertEquals("Getting ready\u2026", ArchiveProgressWindow.text(-1, 0, "", -1));
		assertEquals("0 of 401 (0%)", ArchiveProgressWindow.text(401, 0, "", 0));
		assertEquals("152 of 401 (37%): docs/f234.bin", ArchiveProgressWindow.text(401, 152, "docs/f234.bin", 37));
		assertEquals("12%", ArchiveProgressWindow.text(-1, 0, "", 12));
		assertEquals("1 item (3%): trip/a.jpg", ArchiveProgressWindow.text(-1, 1, "trip/a.jpg", 3));
		assertEquals("37 items (45%): trip/b.jpg", ArchiveProgressWindow.text(-1, 37, "trip/b.jpg", 45));
	}

	@Test
	public void linesAsTheyAreWritten() throws Exception {
		List<String> got = new java.util.ArrayList<>();
		try (ArchiveActions.LineOutputStream out = new ArchiveActions.LineOutputStream(got::add)) {
			out.write("one\ntwo\r\nthr".getBytes(java.nio.charset.StandardCharsets.UTF_8));
			assertEquals(List.of("one", "two"), got);
			out.write("ee \u00e9".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		}
		// what is left at the end is a line too
		assertEquals(List.of("one", "two", "three \u00e9"), got);
	}
}

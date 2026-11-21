package us.bringardner.parley.files.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.viewer.IRegistry.CommandType;
import us.bringardner.parley.files.viewer.IRegistry.RegData;

/**
 * Which directories are shown as applications. The registry is replaced by one that answers the
 * way Launch Services does (a valid bundle has a handler, a folder that is only named X.app has
 * none), so this runs on any system; what Launch Services itself says is checked on a Mac in
 * registry/MacRegistryApplicationTest.
 */
public class AppBundlesTest {

	/** says yes for the paths it was told about, and keeps every path it was asked about */
	private static class FakeRegistry implements IRegistry {
		final List<String> valid = new ArrayList<>();
		final List<String> asked = new ArrayList<>();

		@Override
		public List<RegData> getRegisteredHandler(String path, CommandType type) {
			return new ArrayList<>();
		}

		@Override
		public boolean isApplication(String path) {
			asked.add(path);
			return valid.contains(path);
		}
	}

	@TempDir
	Path dir;

	private Supplier<IRegistry> saved;
	private FakeRegistry fake;

	@BeforeEach
	void useAFakeRegistry() {
		saved = AppBundles.registry;
		fake = new FakeRegistry();
		AppBundles.registry = () -> fake;
		AppBundles.clear();
	}

	@AfterEach
	void putTheRegistryBack() {
		AppBundles.registry = saved;
		AppBundles.clear();
	}

	private FileSource source(Path p) throws Exception {
		return FileSourceFactory.getDefaultFactory().createFileSource(p.toString());
	}

	private Path folder(String name) throws Exception {
		return Files.createDirectory(dir.resolve(name));
	}

	@Test
	void aNameEndsAsABundleDoes() {
		assertTrue(AppBundles.hasAppName("Calculator.app"));
		assertTrue(AppBundles.hasAppName("Calculator.APP"));
		assertTrue(AppBundles.hasAppName(".app"));
		assertFalse(AppBundles.hasAppName("Calculator.application"));
		assertFalse(AppBundles.hasAppName("app"));
		assertFalse(AppBundles.hasAppName("Calculator.app.zip"));
		assertFalse(AppBundles.hasAppName(""));
		assertFalse(AppBundles.hasAppName(null));
	}

	@Test
	void aFolderTheRegistryKnowsIsAnApplication() throws Exception {
		Path app = folder("Real.app");
		fake.valid.add(app.toFile().getAbsolutePath());
		assertTrue(AppBundles.isAppBundle(source(app)));
	}

	@Test
	void aFolderOnlyNamedAppIsAFolder() throws Exception {
		assertFalse(AppBundles.isAppBundle(source(folder("Fake.app"))));
	}

	@Test
	void aFileNamedAppIsNotAnApplication() throws Exception {
		Path file = Files.write(dir.resolve("File.app"), new byte[1]);
		fake.valid.add(file.toFile().getAbsolutePath());
		assertFalse(AppBundles.isAppBundle(source(file)));
		assertTrue(fake.asked.isEmpty(), "a file is not asked about");
	}

	@Test
	void aFolderWithAnotherNameIsNeverAskedAbout() throws Exception {
		Path plain = folder("plain");
		fake.valid.add(plain.toFile().getAbsolutePath());
		assertFalse(AppBundles.isAppBundle(source(plain)));
		assertFalse(AppBundles.isAppBundle(null));
		assertTrue(fake.asked.isEmpty(), "only a name ending .app is worth asking about");
	}

	@Test
	void theRegistryIsAskedOnceForEachBundle() throws Exception {
		Path app = folder("Real.app");
		fake.valid.add(app.toFile().getAbsolutePath());
		FileSource f = source(app);
		for(int idx = 0; idx < 5; idx++) {
			assertTrue(AppBundles.isAppBundle(f));
		}
		assertEquals(1, fake.asked.size());
	}

	@Test
	void aBundleThatChangedIsAskedAboutAgain() throws Exception {
		Path app = folder("Real.app");
		File file = app.toFile();
		file.setLastModified(1_000_000_000_000L);
		assertFalse(AppBundles.isAppBundle(source(app)));
		// (it is made a valid application: what is in it changes the bundle's time)
		fake.valid.add(file.getAbsolutePath());
		file.setLastModified(1_100_000_000_000L);
		assertTrue(AppBundles.isAppBundle(source(app)));
		assertEquals(2, fake.asked.size());
	}

	@Test
	void theTableShowsAnApplicationAsAFileOfItsOwnKind() throws Exception {
		Path app = folder("Real.app");
		fake.valid.add(app.toFile().getAbsolutePath());
		folder("Fake.app");
		folder("plain");
		Files.write(dir.resolve("b.txt"), new byte[5]);
		FileSource d = source(dir);
		FileTableModel m = new FileTableModel(d, FileTableModel.load(d, null, true), true, true);
		List<Object> names = new ArrayList<>();
		List<Object> types = new ArrayList<>();
		for(int row = 0; row < m.getRowCount(); row++) {
			names.add(m.getValueAt(row, 1));
			types.add(m.getValueAt(row, 3));
		}
		assertEquals(List.of("b.txt", "Fake.app", "plain", "Real.app"), names);
		int real = names.indexOf("Real.app");
		int fakeApp = names.indexOf("Fake.app");
		assertEquals("App", types.get(real));
		assertEquals("Dir", types.get(fakeApp));
		assertTrue(m.isApplication(real));
		assertFalse(m.isDirectory(real), "an application is not shown as a folder");
		assertEquals(-1L, m.getValueAt(real, 4), "a bundle has no size of its own to show");
		assertFalse(m.isApplication(fakeApp));
		assertTrue(m.isDirectory(fakeApp));
	}
}

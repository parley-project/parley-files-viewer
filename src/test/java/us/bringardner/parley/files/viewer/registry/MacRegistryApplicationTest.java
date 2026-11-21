package us.bringardner.parley.files.viewer.registry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * IRegistry.isApplication: what is an application is for Launch Services to say, so the checks
 * of its answers run only on a Mac; the rest runs everywhere.
 */
class MacRegistryApplicationTest {

	private static void mac() {
		assumeTrue(LaunchServices.isAvailable(), "Launch Services is only on a Mac");
	}

	private static File system(String path) {
		File ret = new File(path);
		assumeTrue(ret.isDirectory(), path+" is not installed here");
		return ret;
	}

	@Test
	void whereThereIsNoLaunchServicesNothingIsAnApplication() {
		assumeFalse(LaunchServices.isAvailable());
		assertFalse(new MacRegistry().isApplication("/System/Applications/Calculator.app"));
	}

	@Test
	void aRegistryThatKnowsNoApplicationsSaysNo() {
		assertFalse(new WindowsRegistry().isApplication("C:\\Program Files\\x.app"));
	}

	@Test
	void aValidApplicationIsOne() {
		mac();
		assertTrue(new MacRegistry().isApplication(system("/System/Applications/Calculator.app").getPath()));
	}

	@Test
	void anApplicationThatTheListOfHandlersLeavesOutIsOneToo() {
		mac();
		// (MacRegistry.getInstance skips it, as it is a poor viewer of anything: it is still an application)
		assertTrue(new MacRegistry().isApplication(system("/System/Applications/Utilities/System Information.app").getPath()));
	}

	@Test
	void aFolderOnlyNamedAppIsNot(@TempDir File dir) {
		mac();
		File fake = new File(dir, "testdir.app");
		assertTrue(fake.mkdir());
		assertFalse(new MacRegistry().isApplication(fake.getPath()));
	}

	@Test
	void aFileNamedAppIsNot(@TempDir File dir) throws IOException {
		mac();
		File file = new File(dir, "test.app");
		Files.writeString(file.toPath(), "x\n");
		assertFalse(new MacRegistry().isApplication(file.getPath()));
	}

	@Test
	void aFolderWithAnotherNameAndAPathThatIsNotThereAreNot(@TempDir File dir) {
		mac();
		assertFalse(new MacRegistry().isApplication(dir.getPath()));
		assertFalse(new MacRegistry().isApplication(new File(dir, "gone.app").getPath()));
		assertFalse(new MacRegistry().isApplication(null));
	}
}

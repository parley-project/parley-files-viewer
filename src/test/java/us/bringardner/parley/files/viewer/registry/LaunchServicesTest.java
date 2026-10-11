package us.bringardner.parley.files.viewer.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import us.bringardner.parley.files.viewer.registry.LaunchServices.App;
import us.bringardner.parley.files.viewer.registry.LaunchServices.Result;
import us.bringardner.parley.files.viewer.registry.LaunchServices.Role;

/**
 * Which copy of an application is listed (LaunchServices.dedupe) is checked everywhere; what
 * Launch Services answers only on a Mac.
 */
class LaunchServicesTest {

	private static App app(String name, String path, String bundleId, String version, boolean isDefault) {
		App a = new App(name, path, "");
		a.bundleId = bundleId;
		a.version = version;
		a.isDefault = isDefault;
		return a;
	}

	private static Result result(App ... apps) {
		Result r = new Result("public.plain-text");
		for(App a : apps) {
			r.apps.add(a);
		}
		return r;
	}

	// ---- versions

	@Test
	void versionsAreComparedByTheirNumbers() {
		assertTrue(LaunchServices.compareVersions("10.4.14", "10.4.8") > 0);
		assertTrue(LaunchServices.compareVersions("2.31226.1", "1.40609.0") > 0);
		assertTrue(LaunchServices.compareVersions("9", "10") < 0);
		// (a missing number is 0)
		assertEquals(0, LaunchServices.compareVersions("1.0", "1"));
		assertEquals(0, LaunchServices.compareVersions("", ""));
		assertTrue(LaunchServices.compareVersions("", "0.1") < 0);
		// (letters are not numbers: 1.2b3 is 1, 2, 3)
		assertTrue(LaunchServices.compareVersions("1.2b4", "1.2b3") > 0);
	}

	// ---- the policy: the default, then the newest, then the better place, then the shallower path

	@Test
	void anApplicationInstalledOnceIsLeftAlone() {
		Result in = result(app("Pages", "/Applications/Pages.app", "com.apple.iWork.Pages", "14.0", false));
		Result out = LaunchServices.dedupe(in);
		assertEquals(1, out.apps.size());
		assertTrue(out.apps.get(0).alternates.isEmpty());
	}

	@Test
	void theDefaultCopyWinsOverANewerOne() {
		Result out = LaunchServices.dedupe(result(
				app("Claude", "/Volumes/Data/Applications/Claude.app", "id.claude", "9.0", false),
				app("Claude", "/Applications/Claude.app", "id.claude", "1.0", true)));
		assertEquals(1, out.apps.size());
		assertEquals("/Applications/Claude.app", out.apps.get(0).path);
		assertEquals(1, out.apps.get(0).alternates.size());
		assertEquals("/Volumes/Data/Applications/Claude.app", out.apps.get(0).alternates.get(0).path);
	}

	@Test
	void theNewestCopyWinsWhenNoneIsTheDefault() {
		// (GarageBand: the "copy" is the newer build, and a filename is not evidence of anything)
		Result out = LaunchServices.dedupe(result(
				app("GarageBand", "/Applications/GarageBand.app", "id.gb", "10.4.8", false),
				app("GarageBand", "/Applications/GarageBand copy.app", "id.gb", "10.4.14", false)));
		assertEquals("/Applications/GarageBand copy.app", out.apps.get(0).path);
		assertEquals("/Applications/GarageBand.app", out.apps.get(0).alternates.get(0).path);
	}

	@Test
	void theBetterPlaceWinsOnTheSameVersion() {
		Result out = LaunchServices.dedupe(result(
				app("X", "/Volumes/Data/Applications/X.app", "id.x", "1.0", false),
				app("X", "/System/Applications/X.app", "id.x", "1.0", false),
				app("X", "/Applications/X.app", "id.x", "1.0", false)));
		assertEquals("/Applications/X.app", out.apps.get(0).path);
		assertEquals(2, out.apps.get(0).alternates.size());
	}

	@Test
	void theShallowerPathWinsInTheSamePlace() {
		// (OpenSCAD: the same version in a folder and in a folder in it)
		Result out = LaunchServices.dedupe(result(
				app("OpenSCAD", "/Volumes/Data/Applications/ExecutableJars/OpenSCAD.app", "id.os", "2021.01", false),
				app("OpenSCAD", "/Volumes/Data/Applications/OpenSCAD.app", "id.os", "2021.01", false)));
		assertEquals("/Volumes/Data/Applications/OpenSCAD.app", out.apps.get(0).path);
	}

	@Test
	void theChoiceDoesNotDependOnTheOrderOfTheInput() {
		App a = app("X", "/Applications/X.app", "id.x", "2.0", false);
		App b = app("X", "/Volumes/V/X.app", "id.x", "1.0", false);
		App c = app("X", "/Applications/Old/X.app", "id.x", "2.0", false);
		String one = LaunchServices.dedupe(result(a, b, c)).apps.get(0).path;
		String two = LaunchServices.dedupe(result(copy(c), copy(b), copy(a))).apps.get(0).path;
		String three = LaunchServices.dedupe(result(copy(b), copy(a), copy(c))).apps.get(0).path;
		assertEquals(one, two);
		assertEquals(one, three);
		assertEquals("/Applications/X.app", one);
	}

	private static App copy(App a) {
		return app(a.name, a.path, a.bundleId, a.version, a.isDefault);
	}

	@Test
	void nothingIsLostAndTheOrderOfTheApplicationsIsKept() {
		Result out = LaunchServices.dedupe(result(
				app("A", "/Applications/A.app", "id.a", "1", true),
				app("B", "/Applications/B.app", "id.b", "1", false),
				app("A", "/Applications/A2.app", "id.a", "2", false),
				app("C", "/Applications/C.app", "id.c", "1", false)));
		List<String> names = new ArrayList<>();
		int all = 0;
		for(App a : out.apps) {
			names.add(a.name);
			all += 1 + a.alternates.size();
		}
		assertEquals(List.of("A", "B", "C"), names);
		assertEquals(4, all);
		// (the default stays first)
		assertTrue(out.apps.get(0).isDefault);
	}

	@Test
	void applicationsWithoutABundleIdentifierAreNeverMerged() {
		Result out = LaunchServices.dedupe(result(
				app("Tool", "/opt/a/Tool.app", "", "1", false),
				app("Tool", "/opt/b/Tool.app", "", "1", false)));
		assertEquals(2, out.apps.size());
	}

	@Test
	void theRouteAndTheProblemsSurviveDedupe() {
		Result in = result(app("A", "/Applications/A.app", "id.a", "1", false));
		in.route = "file";
		in.problems.add("one handler had no name");
		Result out = LaunchServices.dedupe(in);
		assertEquals("file", out.route);
		assertEquals(in.problems, out.problems);
		assertEquals(in.uti, out.uti);
	}

	// ---- Launch Services itself (a Mac)

	private static void mac() {
		assumeTrue(LaunchServices.isAvailable(), "Launch Services is only on a Mac");
	}

	@Test
	void anExtensionIsAPlainTextTypeWithApplications() {
		mac();
		Result r = LaunchServices.handlersFor("txt", Role.ALL);
		assertEquals("public.plain-text", r.uti);
		assertFalse(r.apps.isEmpty());
		for(App a : r.apps) {
			assertTrue(new File(a.path).exists(), a.path);
			assertTrue(a.path.endsWith(".app"), a.path);
			assertFalse(a.name.isEmpty());
		}
	}

	@Test
	void anExtensionNobodyKnowsHasNoApplications() {
		mac();
		Result r = LaunchServices.handlersFor("zzqqxx", Role.ALL);
		assertTrue(r.apps.isEmpty());
		assertFalse(r.problems.isEmpty());
	}

	@Test
	void aFileIsAskedAboutItselfAndTheDefaultIsFirst(@TempDir File dir) throws IOException {
		mac();
		File f = new File(dir, "a.txt");
		Files.writeString(f.toPath(), "hello\n");
		Result r = LaunchServices.handlersForPath(f.getPath(), Role.ALL);
		assertEquals("file", r.route);
		assertFalse(r.apps.isEmpty());
		assertTrue(r.apps.get(0).isDefault, "the default application is first");
		long defaults = r.apps.stream().filter(a -> a.isDefault).count();
		assertEquals(1, defaults);
	}

	@Test
	void aFileWithNoExtensionIsAnsweredByItsContent(@TempDir File dir) throws IOException {
		mac();
		File f = new File(dir, "README");
		Files.writeString(f.toPath(), "plain text with no extension\n");
		Result r = LaunchServices.handlersForPath(f.getPath(), Role.ALL);
		assertEquals("file", r.route);
		// (asked by extension, there is nothing to ask about)
		assertFalse(r.apps.isEmpty());
	}

	@Test
	void aFolderIsAFileToo(@TempDir File dir) {
		mac();
		Result r = LaunchServices.handlersForPath(dir.getPath(), Role.ALL);
		assertEquals("file", r.route);
		assertFalse(r.apps.isEmpty());
	}

	@Test
	void aPathThatIsNotThereIsAskedAboutByItsExtension() {
		mac();
		Result r = LaunchServices.handlersForPath("/no/such/folder/file.txt", Role.ALL);
		assertEquals("extension", r.route);
		assertEquals("public.plain-text", r.uti);
		assertFalse(r.apps.isEmpty());
	}

	@Test
	void rolesNarrowTheList(@TempDir File dir) throws IOException {
		mac();
		File f = new File(dir, "a.txt");
		Files.writeString(f.toPath(), "hello\n");
		int all = LaunchServices.handlersForPath(f.getPath(), Role.ALL).apps.size();
		int viewers = LaunchServices.handlersForPath(f.getPath(), Role.VIEWER).apps.size();
		int editors = LaunchServices.handlersForPath(f.getPath(), Role.EDITOR).apps.size();
		assertTrue(viewers <= all && editors <= all);
		assertNotEquals(0, viewers + editors);
	}

	@Test
	void aDedupedListHasEachApplicationOnce(@TempDir File dir) throws IOException {
		mac();
		File f = new File(dir, "a.txt");
		Files.writeString(f.toPath(), "hello\n");
		Result r = LaunchServices.dedupe(LaunchServices.handlersForPath(f.getPath(), Role.ALL));
		Set<String> ids = new HashSet<>();
		for(App a : r.apps) {
			if( !a.bundleId.isEmpty() ) {
				assertTrue(ids.add(a.bundleId), "listed twice: "+a.bundleId);
			}
		}
	}
}

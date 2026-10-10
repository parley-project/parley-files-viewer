package us.bringardner.parley.files.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import us.bringardner.parley.files.memory.MemoryFileSourceFactory;

/** The root of a file system is one thing, however many times the viewer builds it. */
public class RootFileEqualityTest {

	@Test
	void twoRootsOfOneFileSystemAreEqual() throws Exception {
		MemoryFileSourceFactory f = new MemoryFileSourceFactory();
		RootFile a = new RootFile(f);
		RootFile b = new RootFile(f);
		assertEquals(a, b);
		assertEquals(b, a, "symmetric");
		assertEquals(a, a, "reflexive");
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void theRootsOfTwoFileSystemsAreNot() throws Exception {
		RootFile a = new RootFile(new MemoryFileSourceFactory());
		RootFile b = new RootFile(new MemoryFileSourceFactory());
		assertNotEquals(a, b);
	}

	@Test
	void notEqualToNullOrAnotherKindOfFile() throws Exception {
		MemoryFileSourceFactory f = new MemoryFileSourceFactory();
		RootFile a = new RootFile(f);
		assertFalse(a.equals(null));
		assertFalse(a.equals(f.createFileSource("/")));
		assertFalse(a.equals("/"));
	}

	@Test
	void ordersByPathAndIsNotEqualToEverything() throws Exception {
		MemoryFileSourceFactory f = new MemoryFileSourceFactory();
		RootFile root = new RootFile(f);
		assertEquals(0, root.compareTo(new RootFile(f)));
		// "/" sorts before anything below it, and a child sorts after it
		assertTrue(root.compareTo(f.createFileSource("/a")) < 0);
		assertTrue(f.createFileSource("/a").compareTo(root) > 0);
		assertTrue(root.compareTo("/zzz") < 0, "it used to answer 0 to everything");
	}
}

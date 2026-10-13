package us.bringardner.parley.files.viewer.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.memory.MemoryFileSourceFactory;
import us.bringardner.parley.files.viewer.FileSourceViewer;

/**
 * An application can only open a file on this machine, so a file from another file system (here
 * a memory file system, which is not on a disk) is copied to the temporary folder first.
 * Needs a display for the progress dialog, so it's skipped in a headless build.
 */
public class TestDownloadForExternalApplication {

	private FileSourceViewer viewer;
	private FileSource remote;
	private final byte [] data = "the remote file's data".getBytes(StandardCharsets.UTF_8);

	@BeforeEach
	void setup() throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		viewer = new FileSourceViewer();
		MemoryFileSourceFactory factory = new MemoryFileSourceFactory();
		remote = factory.createFileSource("/docs/Report.txt");
		remote.getParentFile().mkdirs();
		try (OutputStream out = remote.getOutputStream()) {
			out.write(data);
		}
	}

	@AfterEach
	void cleanup() {
		if( viewer != null && viewer.getFrame() != null ) {
			viewer.getFrame().dispose();
		}
	}

	@Test
	public void aFileOnAnotherSystemIsCopiedUnderItsOwnName() throws Exception {
		File local = viewer.getLocalFileDownloadIfNeeded(remote);
		assertNotNull(local, "the copy was not made");
		assertTrue(local.isFile());
		assertEquals("Report.txt", local.getName(), "the application picks by the name's extension");
		assertArrayEquals(data, Files.readAllBytes(local.toPath()));
	}

	@Test
	public void theSameVersionIsNotCopiedTwice() throws Exception {
		File first = viewer.getLocalFileDownloadIfNeeded(remote);
		File second = viewer.getLocalFileDownloadIfNeeded(remote);
		assertNotNull(first);
		assertEquals(first, second);
	}

	@Test
	public void aLocalFileIsItself() throws Exception {
		File file = File.createTempFile("local", ".txt");
		file.deleteOnExit();
		FileSource fs = FileSourceFactory.fileProxyFactory.createFileSource(file.getAbsolutePath());
		assertEquals(file.getCanonicalFile(), viewer.getLocalFileDownloadIfNeeded(fs).getCanonicalFile());
		assertFalse(file.getParentFile().getName().startsWith("open-"));
	}

	@Test
	public void theEventThreadIsNotMadeToWait() throws Exception {
		AtomicReference<Throwable> thrown = new AtomicReference<>();
		SwingUtilities.invokeAndWait(()->{
			try {
				viewer.getLocalFileDownloadIfNeeded(remote);
			} catch (Throwable e) {
				thrown.set(e);
			}
		});
		assertNotNull(thrown.get());
		assertSame(IllegalStateException.class, thrown.get().getClass());
	}
}

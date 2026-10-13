package us.bringardner.parley.files.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.memory.MemoryFileSourceFactory;

/**
 * A large file from another machine is asked about before it is downloaded for an application to
 * open. The question is answered by the test (it overrides askLargeDownload), so no dialog is shown.
 * Needs a display for the progress dialog of a download that goes ahead, so it's skipped headless.
 */
public class TestLargeDownloadWarning {

	private static final int TWO_MB = 2 * 1024 * 1024;

	private final Preferences pref = Preferences.userNodeForPackage(FileSourceViewer.class);
	private int savedLimit;
	private boolean hadLimit;

	private final List<String> asked = new ArrayList<>();
	private boolean answer;
	private FileSourceViewer viewer;
	private FileSource big;

	@BeforeEach
	void setup() throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		hadLimit = pref.get(FileSourceViewer.PREF_LARGE_FILE_MB, null) != null;
		savedLimit = pref.getInt(FileSourceViewer.PREF_LARGE_FILE_MB, 0);
		pref.putInt(FileSourceViewer.PREF_LARGE_FILE_MB, 1);

		viewer = new FileSourceViewer() {
			@Override
			protected boolean askLargeDownload(String message) {
				asked.add(message);
				return answer;
			}
		};
		MemoryFileSourceFactory factory = new MemoryFileSourceFactory();
		big = factory.createFileSource("/data/Big.bin");
		big.getParentFile().mkdirs();
		try (OutputStream out = big.getOutputStream()) {
			out.write(new byte[TWO_MB]);
		}
	}

	@AfterEach
	void cleanup() {
		if( hadLimit ) {
			pref.putInt(FileSourceViewer.PREF_LARGE_FILE_MB, savedLimit);
		} else {
			pref.remove(FileSourceViewer.PREF_LARGE_FILE_MB);
		}
		if( viewer != null && viewer.getFrame() != null ) {
			viewer.getFrame().dispose();
		}
	}

	@Test
	public void aLargeFileIsAskedAboutAndNotDownloadedIfRefused() throws Exception {
		answer = false;
		assertNull(viewer.getLocalFileDownloadIfNeeded(big));
		assertEquals(1, asked.size());
		assertTrue(asked.get(0).contains("Big.bin"), asked.get(0));
		assertTrue(asked.get(0).contains("2.0 MB"), asked.get(0));
	}

	@Test
	public void aLargeFileIsDownloadedIfAccepted() throws Exception {
		answer = true;
		File local = viewer.getLocalFileDownloadIfNeeded(big);
		assertNotNull(local);
		assertEquals(TWO_MB, local.length());
		assertEquals(1, asked.size());
	}

	@Test
	public void aCopyAlreadyMadeIsNotAskedAboutAgain() throws Exception {
		answer = true;
		File first = viewer.getLocalFileDownloadIfNeeded(big);
		File second = viewer.getLocalFileDownloadIfNeeded(big);
		assertNotNull(first);
		assertEquals(first, second);
		assertEquals(1, asked.size(), "asked again for a file that was already downloaded");
	}

	@Test
	public void aSmallFileIsNotAskedAbout() throws Exception {
		pref.putInt(FileSourceViewer.PREF_LARGE_FILE_MB, 10);
		answer = false;
		assertNotNull(viewer.getLocalFileDownloadIfNeeded(big));
		assertTrue(asked.isEmpty());
	}

	@Test
	public void zeroTurnsTheQuestionOff() throws Exception {
		pref.putInt(FileSourceViewer.PREF_LARGE_FILE_MB, 0);
		answer = false;
		assertNotNull(viewer.getLocalFileDownloadIfNeeded(big));
		assertTrue(asked.isEmpty());
	}

	@Test
	public void aLocalFileIsNeverAskedAbout() throws Exception {
		File file = File.createTempFile("big", ".bin");
		file.deleteOnExit();
		try (OutputStream out = new java.io.FileOutputStream(file)) {
			out.write(new byte[TWO_MB]);
		}
		FileSource local = FileSourceFactory.fileProxyFactory.createFileSource(file.getAbsolutePath());
		answer = false;
		assertNotNull(viewer.getLocalFileDownloadIfNeeded(local));
		assertTrue(asked.isEmpty());
		assertTrue(viewer.confirmLargeDownload(local, true));
	}

	@Test
	public void anEditSaysTheWholeFileIsSentBack() throws Exception {
		answer = false;
		assertFalse(viewer.confirmLargeDownload(big, true));
		assertFalse(viewer.confirmLargeDownload(big, false));
		assertTrue(asked.get(0).contains("sent back"), asked.get(0));
		assertFalse(asked.get(1).contains("sent back"), asked.get(1));
	}

	@Test
	public void sizesAreReadable() {
		assertEquals("512 bytes", FileSourceViewer.formatSize(512));
		assertEquals("1.0 KB", FileSourceViewer.formatSize(1024));
		assertEquals("2.0 MB", FileSourceViewer.formatSize(TWO_MB));
		assertEquals("3.2 GB", FileSourceViewer.formatSize((long)(3.2 * 1024 * 1024 * 1024)));
	}
}

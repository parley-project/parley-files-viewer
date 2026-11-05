package us.bringardner.parley.files.viewer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;

/**
 * The table's rows are read once, when the folder is loaded; painting reads
 * them back without asking the files again (for SFTP each question is a
 * round trip to the server).
 */
public class FileTableModelTest {

	@TempDir
	Path dir;

	private FileTableModel model(boolean showHidden) throws Exception {
		Files.write(dir.resolve("b.txt"), new byte[5]);
		Files.createDirectory(dir.resolve("sub"));
		Files.write(dir.resolve(".hidden"), new byte[1]);
		Files.createSymbolicLink(dir.resolve("linksub"), dir.resolve("sub"));
		FileSource d = FileSourceFactory.getDefaultFactory().createFileSource(dir.toString());
		return new FileTableModel(d, FileTableModel.load(d, null, showHidden), showHidden, true);
	}

	private static List<Object> column(FileTableModel m, int col) {
		List<Object> ret = new ArrayList<>();
		for (int row = 0; row < m.getRowCount(); row++) {
			ret.add(m.getValueAt(row, col));
		}
		return ret;
	}

	@Test
	public void rowsSortedByNameWithoutHidden() throws Exception {
		FileTableModel m = model(false);
		assertEquals(List.of("b.txt", "linksub", "sub"), column(m, 1));
		assertEquals(List.of("File", "Link", "Dir"), column(m, 3));
		assertEquals(List.of(5L, -1L, -1L), column(m, 4));
		assertEquals(FileTableModel.dirIcon, m.getValueAt(2, 0));
	}

	@Test
	public void hiddenShownWhenAsked() throws Exception {
		assertEquals(List.of(".hidden", "b.txt", "linksub", "sub"), column(model(true), 1));
	}

	@Test
	public void paintingDoesNotAskTheFilesAgain() throws Exception {
		FileTableModel m = model(false);
		// changed after loading: the rows still show what was loaded, until the folder is loaded again
		Files.write(dir.resolve("b.txt"), new byte[500]);
		assertEquals(5L, m.getValueAt(0, 4));
		m.setDir(m.getDir());
		assertEquals(500L, m.getValueAt(0, 4));
	}

	@Test
	public void toolTipIsTheCanonicalPath() throws Exception {
		FileTableModel m = model(false);
		assertEquals(dir.resolve("b.txt").toRealPath().toString(), m.getToolTip(0));
		assertEquals(dir.resolve("sub").toRealPath().toString(), m.getToolTip(1), "a link shows its target");
		assertNull(m.getToolTip(-1));
		assertNull(m.getToolTip(3));
		assertTrue(m.getToolTip(0) == m.getToolTip(0), "worked out once");
	}
}

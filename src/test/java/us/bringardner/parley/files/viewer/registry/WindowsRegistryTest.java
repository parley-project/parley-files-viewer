package us.bringardner.parley.files.viewer.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import us.bringardner.parley.files.viewer.IRegistry;
import us.bringardner.parley.files.viewer.IRegistry.CommandType;
import us.bringardner.parley.files.viewer.IRegistry.RegData;

class WindowsRegistryTest {

	@Test
	void extensionIsTheLastDotOfTheName() {
		assertEquals(".txt", WindowsRegistry.getExt("a.txt"));
		assertEquals(".xml", WindowsRegistry.getExt("Test.x.y.xml"));
		assertEquals(".test", WindowsRegistry.getExt(".test"));
		assertEquals(".txt", WindowsRegistry.getExt("C:\\dir.d\\a.txt"));
		assertEquals(".txt", WindowsRegistry.getExt("/dir.d/a.txt"));
		assertNull(WindowsRegistry.getExt("Test"));
		assertNull(WindowsRegistry.getExt("C:\\dir.d\\Test"));
		assertNull(WindowsRegistry.getExt("a."));
		assertNull(WindowsRegistry.getExt(null));
	}

	@Test
	void commandIsSplitAtSpacesOutsideQuotes() {
		assertEquals(List.of("C:\\Program Files\\App\\a.exe", "-x", "%1"),
				RegData.splitCommand("\"C:\\Program Files\\App\\a.exe\" -x \"%1\""));
		assertEquals(List.of("notepad.exe", "%1"), RegData.splitCommand("  notepad.exe   %1 "));
		assertEquals(List.of(), RegData.splitCommand(""));
	}

	@Test
	void theFileTakesThePlaceOfTheVerbsPlaceholder() {
		String file = "C:\\my dir\\a b.docx";
		assertEquals(List.of("C:\\Office\\WINWORD.EXE", "/n", file),
				WindowsRegistry.commandLine("\"C:\\Office\\WINWORD.EXE\" /n \"%1\" /o \"%u\"", file));
		assertEquals(List.of("C:\\Windows\\Explorer.exe", file),
				WindowsRegistry.commandLine("C:\\Windows\\Explorer.exe /idlist,%I,%L", file));
		assertEquals(List.of("msedge.exe", "--single-argument", file),
				WindowsRegistry.commandLine("msedge.exe --single-argument %1", file));
		assertEquals(List.of("notepad.exe", file),
				WindowsRegistry.commandLine("notepad.exe", file));
	}

	@Test
	void environmentVariablesAreExpanded() {
		String root = System.getenv("SystemRoot");
		if( root != null ) {
			assertEquals(root+"\\x.exe", WindowsRegistry.expand("%SystemRoot%\\x.exe"));
		}
		assertEquals("%NO_SUCH_VARIABLE_ANYWHERE%\\x", WindowsRegistry.expand("%NO_SUCH_VARIABLE_ANYWHERE%\\x"));
		assertEquals("100%", WindowsRegistry.expand("100%"));
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void aTextFileHasAnApplicationThatExists() throws Exception {
		assertTrue(WindowsRegistry.isAvailable());
		List<RegData> list = new WindowsRegistry().getRegisteredHandler("C:\\some dir\\Test.txt", CommandType.Any);
		assertFalse(list.isEmpty(), "no application for .txt");
		RegData first = list.get(0);
		assertTrue(new File(first.path).isFile(), "not a file: "+first.path);
		assertNotNull(first.name);
		assertFalse(first.name.isEmpty());
		for(RegData d : list) {
			assertTrue(new File(d.path).isFile(), "not a file: "+d.path);
		}
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void theFilesPathIsOneArgumentOfTheCommand() {
		String file = "C:\\some dir\\a file.txt";
		List<RegData> list = new WindowsRegistry().getRegisteredHandler(file, CommandType.Viewer);
		assertFalse(list.isEmpty());
		for(RegData d : list) {
			List<String> cmd = d.getCommandLine(file);
			assertTrue(cmd.contains(file), d+" "+cmd);
			assertTrue(cmd.get(0).toLowerCase().endsWith(".exe"), cmd.toString());
		}
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void aNameWithoutAnExtensionHasNoApplications() {
		assertTrue(new WindowsRegistry().getRegisteredHandler("Test", CommandType.Any).isEmpty());
		assertTrue(new WindowsRegistry().getRegisteredHandler(null, CommandType.Any).isEmpty());
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void aSecondAskGivesTheSameAnswer() {
		IRegistry reg = new WindowsRegistry();
		List<RegData> a = reg.getRegisteredHandler("x.html", CommandType.Editor);
		List<RegData> b = reg.getRegisteredHandler("y.HTML", CommandType.Editor);
		assertEquals(a.size(), b.size());
		for(int idx = 0; idx < a.size(); idx++) {
			assertEquals(a.get(idx).path, b.get(idx).path);
		}
	}
}

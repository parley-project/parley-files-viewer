/**
 * Copyright Tony Bringardner 1998, 2026 <a href="http://bringardner.com/tony">Tony Bringardner</a>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package us.bringardner.parley.files.viewer.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.viewer.ScriptFiles;

/**
 * Which files look like shell scripts, where the parley command is found, and what Fsh Studio is
 * started with.
 */
public class TestScriptFiles {

	@TempDir
	File tmp;

	private FileSource local(String name, String text) throws Exception {
		File f = new File(tmp, name);
		Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
		return FileSourceFactory.fileProxyFactory.createFileSource(f.getAbsolutePath());
	}

	@Test
	public void namesOfScripts() {
		assertTrue(ScriptFiles.isScriptName("build.fsh"));
		assertTrue(ScriptFiles.isScriptName("run.sh"));
		assertTrue(ScriptFiles.isScriptName("Setup.BASH"));
		assertTrue(ScriptFiles.isScriptName("x.zsh"));
		assertFalse(ScriptFiles.isScriptName("notes.txt"));
		assertFalse(ScriptFiles.isScriptName("sh"));
		assertFalse(ScriptFiles.isScriptName(null));
	}

	@Test
	public void shebangLines() {
		assertTrue(ScriptFiles.isShellShebang("#!/bin/sh"));
		assertTrue(ScriptFiles.isShellShebang("#!/usr/bin/env bash"));
		assertTrue(ScriptFiles.isShellShebang("#! /bin/bash -e"));
		assertTrue(ScriptFiles.isShellShebang("#!/usr/bin/env -S fsh -x"));
		assertTrue(ScriptFiles.isShellShebang("#!/usr/local/bin/fsh"));
		assertFalse(ScriptFiles.isShellShebang("#!/usr/bin/python3"));
		assertFalse(ScriptFiles.isShellShebang("#!/usr/bin/env python"));
		assertFalse(ScriptFiles.isShellShebang("echo hi"));
		assertFalse(ScriptFiles.isShellShebang("#!"));
		assertFalse(ScriptFiles.isShellShebang(null));
	}

	@Test
	public void aLocalFileIsReadForItsFirstLine() throws Exception {
		assertTrue(ScriptFiles.looksLikeScript(local("deploy", "#!/bin/bash\necho hi\n")));
		assertTrue(ScriptFiles.looksLikeScript(local("job", "#!/usr/bin/env sh\r\necho hi\r\n")));
		assertFalse(ScriptFiles.looksLikeScript(local("tool.py", "#!/usr/bin/python3\nprint(1)\n")));
		assertFalse(ScriptFiles.looksLikeScript(local("readme", "echo not a script\n")));
		assertTrue(ScriptFiles.looksLikeScript(local("plain.fsh", "no first line needed")));
		assertFalse(ScriptFiles.looksLikeScript(null));
	}

	@Test
	public void aFolderIsNotAScript() throws Exception {
		File dir = new File(tmp, "dir");
		assertTrue(dir.mkdirs());
		assertFalse(ScriptFiles.looksLikeScript(FileSourceFactory.fileProxyFactory.createFileSource(dir.getAbsolutePath())));
	}

	@Test
	public void quotingAPath() {
		assertEquals("'/a b/c.fsh'", ScriptFiles.shellQuote("/a b/c.fsh"));
		assertEquals("'it'\\''s'", ScriptFiles.shellQuote("it's"));
	}

	@Test
	public void studioIsStartedByTheParleyCommand() {
		List<String> command = new ScriptFiles.StudioApp("/usr/local/bin/parley").getCommandLine("/work/a b.fsh");
		assertEquals(List.of("/usr/local/bin/parley", "fshstudio", "/work/a b.fsh"), command);
	}

	@Test
	public void parleyIsFoundOnThePath() throws Exception {
		if( System.getProperty("os.name", "").toLowerCase().startsWith("windows") ) {
			return;
		}
		File bin = new File(tmp, "bin");
		assertTrue(bin.mkdirs());
		File parley = new File(bin, "parley");
		Files.write(parley.toPath(), "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
		assertTrue(parley.setExecutable(true));
		assertEquals(parley.getAbsolutePath(), ScriptFiles.findParley(bin.getAbsolutePath(), tmp.getAbsolutePath()));
	}
}

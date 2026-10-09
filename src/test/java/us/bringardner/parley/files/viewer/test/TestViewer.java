/**
 * <PRE>
 * 
 * Copyright Tony Bringarder 1998, 2025 <A href="http://bringardner.com/tony">Tony Bringardner</A>
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
 *   
 *	@author Tony Bringardner   
 *
 *
 * ~version~V000.01.17-V000.01.08-V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer.test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.Test;

import us.bringardner.parley.files.FileSourceFactory;
import us.bringardner.parley.files.viewer.FileSourceViewer;

/**
 * Opens the viewer on the local file system and closes it again. Needs a display,
 * so it's skipped in a headless build.
 * <p>
 * It was a jfcunit test (JFCTestCase), but jfcunit is neither on the classpath nor in
 * Maven Central, so it never compiled; this does what it did (BJL-27).
 */
public class TestViewer {

	@Test
	public void theViewerOpens() throws Exception {
		assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
		FileSourceViewer viewer = new FileSourceViewer();
		try {
			viewer.setup(FileSourceFactory.fileProxyFactory, true);
			viewer.show();
			assertTrue(viewer.getFrame().isShowing());
		} finally {
			if( viewer.getFrame() != null ) {
				viewer.getFrame().dispose();
			}
		}
	}
}

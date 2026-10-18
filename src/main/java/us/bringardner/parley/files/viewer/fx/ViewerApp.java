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
package us.bringardner.parley.files.viewer.fx;

import java.util.List;

import javafx.application.Application;
import javafx.stage.Stage;
import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.FileSourceFactory;

/**
 * The JavaFX file viewer: a window onto the default file system, with more windows for
 * other file systems (File, Connect to...). Start it with {@link Launcher}.
 */
public class ViewerApp extends Application {

	@Override
	public void start(Stage stage) throws Exception {
		// the theme and font chosen in Settings (the Swing viewer's too), and the Parley icon
		us.bringardner.fx.dialog.SettingsDialog.load(FxLook.prefs());
		FxLook.dockIcon();
		// connections to servers whose certificate nothing trusts ask here
		FxCertificateValidator certificates = FxCertificateValidator.install();
		certificates.setOwner(stage);
		List<String> args = getParameters().getUnnamed();
		FileSourceFactory factory = FileSourceFactory.getDefaultFactory();
		FileSource start = null;
		if( !args.isEmpty()) {
			// a folder to start in
			FileSource dir = factory.createFileSource(args.get(0));
			if( dir.isDirectory()) {
				start = dir;
			}
		}
		ViewerWindow window = new ViewerWindow(factory, start, stage);
		window.show();
	}

	public static void main(String[] args) {
		launch(args);
	}

}

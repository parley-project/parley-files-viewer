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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;
import us.bringardner.fx.gradient.GradientLook;
import us.bringardner.parley.brand.ParleyBrand;

/**
 * The JavaFX viewer's look, as the Swing viewer's: fx-widgets' gradient look in the theme and font
 * chosen in Settings (shared with the Swing viewer), and the Parley app icon with the folder badge
 * on every window.
 */
final class FxLook {

	/** The application's name: the macOS application menu and About show it. */
	static final String APP_NAME = "Parley Files Viewer";

	/** The icon's badge, as on the Swing viewer. */
	static final String BADGE = "files";

	private static List<Image> icons;

	private FxLook() {
	}

	/**
	 * Where the appearance settings are kept: the Swing viewer's preferences, so both viewers have
	 * the same theme and font.
	 */
	static Preferences prefs() {
		return Preferences.userRoot().node("/us/bringardner/parley/files/viewer");
	}

	/** The app icon with the folder badge in every size; the OS picks the one it needs. */
	static synchronized List<Image> appIcons() {
		if( icons == null ) {
			List<Image> list = new ArrayList<>();
			for(int size : ParleyBrand.ICON_SIZES) {
				URL url = ParleyBrand.resource("parley-icon-" + BADGE + "-" + size + ".png");
				if( url != null ) {
					list.add(new Image(url.toExternalForm()));
				}
			}
			icons = List.copyOf(list);
		}
		return icons;
	}

	/** The look on a window's scene, and the icons on the window. */
	static Scene apply(Stage stage, Scene scene) {
		GradientLook.apply(scene);
		icons(stage);
		return scene;
	}

	/** The look and the icons on a dialog (a window of its own, which takes neither from its owner). */
	static <T extends Dialog<?>> T apply(T dialog) {
		GradientLook.apply(dialog);
		icons(dialog.getDialogPane().getScene().getWindow());
		return dialog;
	}

	private static void icons(Window w) {
		if( w instanceof Stage && ((Stage) w).getIcons().isEmpty() ) {
			((Stage) w).getIcons().setAll(appIcons());
		}
	}

	/**
	 * The Dock / taskbar icon. JavaFX sets it from the window icons on Windows and Linux but not on
	 * macOS, where it would stay the generic Java icon. Best effort.
	 */
	static void dockIcon() {
		try {
			if( java.awt.Taskbar.isTaskbarSupported()
					&& java.awt.Taskbar.getTaskbar().isSupported(java.awt.Taskbar.Feature.ICON_IMAGE) ) {
				java.awt.Taskbar.getTaskbar().setIconImage(ParleyBrand.icon(512, BADGE));
			}
		} catch (Throwable e) {
			// headless, or no taskbar: the window icons are enough
		}
	}
}

# Changelog

## parley-files-viewer 1.0.0 (unreleased)

First release under the Parley name. The viewer was `com.bringardner:bjl_file_system_viewer`
(BjlFileSystemViewer) in the Bringardner Java Library.

### Changed (needs a code change)

- Package: `us.bringardner.io.filesource.viewer` is now `us.bringardner.parley.files.viewer`.
- Built on the Parley modules parley-core, parley-io, parley-net, parley-files,
  parley-files-swing, parley-files-sftp and parley-files-jdbc, and on
  [swing-widgets](https://github.com/parley-project/swing-widgets). The gradient buttons,
  dialogs and fields that were the viewer's own `us.bringardner.swing` package now come from
  swing-widgets.

### Changed (no code change needed)

- The JavaFX viewer (`us.bringardner.parley.files.viewer.fx`, started by `Launcher`) is part of this
  project, as the JavaFX IDE is part of fsh-ide. It was the separate parley-files-viewer-fx
  project; it now needs JavaFX 17 LTS and parley-files-fx 1.1.0.
- The menus follow the standard Parley layout (swing-widgets' `StandardMenus`), the same as
  fsh-ide: File (New Window ⌘N, New Remote Session… ⇧⌘N, New Terminal ⌥⌘T, Close ⌘W, Set Start
  Directory…), Edit (Find… ⌘F), View (Show Hidden Files, Show File Extensions, Refresh ⌘R),
  Window and Help. Appearance… is now Settings… (⌘,). Each window has its own menu bar, at the top
  of the screen on macOS, so the shortcuts work in the focused window.
- Options > Font is now Options > Appearance... (also the Preferences item on macOS): the font and a
  color scheme, from named schemes or any one color, with a preview. It is saved and comes back at
  the next start, and changes the open windows at once. The look and the settings are shared with
  the other Parley applications through swing-widgets.
- Themes: Gold (as before), Dark, Light and the color themes, or one made from any color, in
  Options > Appearance... The table, tree, selection, header and search window colors come from the
  theme, so a dark theme reads as well as a light one; selected rows use the theme's selection color.
- The window and Dock icons are the Parley app icon with a folder badge, from
  [parley-brand](https://github.com/parley-project/parley-brand), in every size from 16 to 512 px.
- The viewer is an example application built on Parley, not part of the library, so it is no longer
  listed in parley-bom.
- "Open terminal" uses [fsh](https://github.com/parley-project/parley-fsh).
- On macOS the applications that can open a file come from Launch Services through JNA, so there
  is no native library to build or to put on `java.library.path`.

### Requirements

- Java 11 or later, like the other Parley modules.

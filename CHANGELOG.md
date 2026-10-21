# Changelog

## parley-files-viewer 1.1.0 (unreleased)

Needs parley-files-swing, parley-files-fx, swing-widgets, fx-widgets and fsh 1.1.0.

### Changed

- The JavaFX viewer (`us.bringardner.parley.files.viewer.fx`, started by `Launcher`) is part of this
  project, as the JavaFX IDE is part of fsh-ide. It was the separate parley-files-viewer-fx
  project; it now needs JavaFX 17 LTS and parley-files-fx 1.1.0.
- The JavaFX viewer looks like the Swing viewer: the gradient look in the theme and font chosen in
  Settings… (fx-widgets' SettingsDialog, shared with the Swing viewer), the Parley icon, and the
  standard menus. Connect to... is now File > New Remote Session..., Search... is Edit > Find...,
  and there are About, Settings…, Window and Quit; Quit asks while files are being copied.
- The JavaFX viewer has the Swing viewer's Open With... (the applications registered for a file,
  with their icons, or any other) and terminal (File > New Terminal, ⌥⌘T, and New Terminal Here:
  an fsh shell in a folder of any file system). The pom gains RichTextFX for the terminal.
- The Swing and JavaFX viewers have the same menus. The Swing viewer gains File > New Folder... (⇧⌘N,
  so New Remote Session... no longer has a shortcut), Open, Open With..., Edit, Edit With..., Print,
  Recent Files and Start Here Next Time, and Edit > Copy, Paste, Rename... and Delete; its file
  menu's items are named as the JavaFX viewer's (New Terminal Here, Rename..., New Folder...,
  Edit With...) and it has Find.... The JavaFX viewer gains Edit With..., Print and Set Start
  Directory..., New Folder... moves to File, and its file menu's Search... is Find....
- Edit With... works: it chose nothing before. Backup, which did what Copy does, is gone.
- In a text field, Edit > Copy, Paste and Delete act on the text, not the files.
- The Swing viewer's globe button, and its menu of Show Hidden Files and Show File Extensions, is
  gone: the View menu has them.
- Folders are in the theme's colors in both viewers (the theme's light color with its accent
  around), not the fixed yellow (Swing) and blue (JavaFX) ones; they change with the theme.
- Both viewers start in the same folder: Start Here Next Time and Set Start Directory... in either
  set it for both (the JavaFX viewer's own setting, if any, is moved over once).

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

### Fixed

- Rename in the Swing viewer moved the file to the folder the viewer started in (the name was
  taken as relative to the file system's current folder, not the file's): the file now stays in
  its own folder, as in the JavaFX viewer, and a name with a separator or one already taken is
  refused.

## parley-files-viewer 1.0.0 (2026-10-21)

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

- The window and Dock icons are the Parley app icon with a folder badge, from
  [parley-brand](https://github.com/parley-project/parley-brand), in every size from 16 to 512 px.
- The viewer is an example application built on Parley, not part of the library, so it is no longer
  listed in parley-bom.
- "Open terminal" uses [fsh](https://github.com/parley-project/parley-fsh).
- On macOS the applications that can open a file come from Launch Services through JNA, so there
  is no native library to build or to put on `java.library.path`.

### Requirements

- Java 11 or later, like the other Parley modules.

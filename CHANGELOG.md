# Changelog

## parley-files-viewer 1.0.0 (unreleased)

First release under the Parley name. The viewer was `com.bringardner:bjl_file_system_viewer`
(BjlFileSystemViewer) in the Bringardner Java Library.

### Changed (needs a code change)

- Package: `us.bringardner.io.filesource.viewer` is now `us.bringardner.parley.files.viewer`.
- Built on the Parley modules parley-core, parley-io, parley-net, parley-files,
  parley-files-swing, parley-files-sftp and parley-files-jdbc, and on
  [swing-widgets](https://github.com/tony-bringardner/swing-widgets). The gradient buttons,
  dialogs and fields that were the viewer's own `us.bringardner.swing` package now come from
  swing-widgets.

### Changed (no code change needed)

- The window and Dock icons are the Parley app icon with a folder badge, from
  [parley-brand](https://github.com/tony-bringardner/parley-brand), in every size from 16 to 512 px.
- The viewer is an example application built on Parley, not part of the library, so it is no longer
  listed in parley-bom.
- "Open terminal" uses [fsh](https://github.com/tony-bringardner/fsh).
- On macOS the applications that can open a file come from Launch Services through JNA, so there
  is no native library to build or to put on `java.library.path`.

### Requirements

- Java 11 or later, like the other Parley modules.

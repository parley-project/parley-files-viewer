# parley-files-viewer

A Swing application for browsing, copying, searching and editing files on any
[parley-files](https://github.com/tony-bringardner/parley-files) file system: local, memory, FTP,
SFTP and JDBC. Part of **Parley**, formerly the Bringardner Java Library.

Formerly `com.bringardner:bjl_file_system_viewer` (BjlFileSystemViewer), with packages under
`us.bringardner.io.filesource.viewer`; they are now `us.bringardner.parley.files.viewer`.

- **Java 21** or later (the "Open terminal" action uses [fsh](https://github.com/tony-bringardner/fsh))
- Built on parley-core, parley-io, parley-net, parley-files, parley-files-swing, parley-files-sftp, parley-files-jdbc
  and [swing-widgets](https://github.com/tony-bringardner/swing-widgets), which now holds the
  gradient buttons, dialogs and fields that used to be the viewer's `us.bringardner.swing` package

## macOS native library

`macos/libGetLaunchData.dylib` (source in `macos/GetLaunchData`) finds the applications that can
open a file type. Start the viewer with `-Djava.library.path=<path to macos>` so it can be loaded.
Its JNI function is named after `us.bringardner.parley.files.viewer.registry.MacRegistry`, so the
library must be rebuilt if that class moves.

## Building

```bash
mvn package
```

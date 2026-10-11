# parley-files-viewer

A Swing application for browsing, copying, searching and editing files on any
[parley-files](https://github.com/tony-bringardner/parley-files) file system: local, memory, FTP,
SFTP and JDBC. Part of **Parley**, formerly the Bringardner Java Library.

Formerly `com.bringardner:bjl_file_system_viewer` (BjlFileSystemViewer), with packages under
`us.bringardner.io.filesource.viewer`; they are now `us.bringardner.parley.files.viewer`.

- **Java 11** or later, like the other Parley modules (the "Open terminal" action uses
  [fsh](https://github.com/tony-bringardner/fsh), which runs on Java 11 too)
- Built on parley-core, parley-io, parley-net, parley-files, parley-files-swing, parley-files-sftp, parley-files-jdbc
  and [swing-widgets](https://github.com/tony-bringardner/swing-widgets), which now holds the
  gradient buttons, dialogs and fields that used to be the viewer's `us.bringardner.swing` package

## Opening files with other applications (macOS)

The list of applications that can open a file comes from Launch Services, through
[JNA](https://github.com/java-native-access/jna) (`registry.LaunchServices`): there is no native
library to build or to put on `java.library.path`, and nothing macOS-specific to ship (JNA is
only used on a Mac).

- **The file itself is asked about** where it exists, as Finder's *Open With* does, so a file
  with no extension and a package (`.photoslibrary`, `.app`) get the applications Finder would
  list. A path that does not exist is asked about by its extension.
- **The default application is first**, and the list can be limited to the viewer or the editor
  role.
- **An application installed more than once is listed once**, as its best copy: the one Launch
  Services opens the file with, else the newest version, else the one in the better place
  (`/Applications`, then `~/Applications`, then `/System/Applications`, then elsewhere, then the
  shallower path). The other copies are in its `getAlternates()`.
- What Launch Services lists are the applications that **declare** they open that kind of file;
  it cannot know that one is good at it.

## Building

```bash
mvn package
```

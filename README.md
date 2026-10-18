# parley-files-viewer

An application for browsing, copying, searching and editing files on any
[parley-files](https://github.com/parley-project/parley-files) file system: local, memory, FTP,
SFTP and JDBC. There are two: the Swing viewer (`FileSourceViewer`) and the JavaFX viewer
(`us.bringardner.parley.files.viewer.fx.Launcher`), in one project, as fsh-ide keeps its Swing and
JavaFX IDEs.

**An example of what Parley can help you build.** The viewer is an application, not part of the
Parley library: it uses the library the same way your own application would, so it is not in
[parley-bom](https://github.com/parley-project/parley-bom). Its window and Dock icons (the Parley tower with a folder badge) come from
[parley-brand](https://github.com/parley-project/parley-brand). Other examples are
[fsh](https://github.com/parley-project/parley-fsh), a shell for any parley-files file system, and
[fsh-ide](https://github.com/parley-project/parley-fsh-ide), an IDE for fsh scripts.

Formerly `com.bringardner:bjl_file_system_viewer` (BjlFileSystemViewer), with packages under
`us.bringardner.io.filesource.viewer`; they are now `us.bringardner.parley.files.viewer`.

- **Java 11** or later, like the other Parley modules (the "Open terminal" action uses
  [fsh](https://github.com/parley-project/parley-fsh), which runs on Java 11 too)
- The JavaFX viewer needs JavaFX 17 LTS and [parley-files-fx](https://github.com/parley-project/parley-files-fx)
- Built on parley-core, parley-io, parley-net, parley-files, parley-files-swing, parley-files-sftp, parley-files-jdbc
  and [swing-widgets](https://github.com/parley-project/swing-widgets), which now holds the
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

## The JavaFX viewer

The JavaFX twin of the Swing viewer, built from the views in parley-files-fx (`FolderTree`,
`FileTable`, `PathBar`, `ConnectionDialog`, `RecentFileMenu`). It was the separate
parley-files-viewer-fx project.

```
mvn -o compile exec:java -Dexec.mainClass=us.bringardner.parley.files.viewer.fx.Launcher
```

An optional argument is the folder to start in.

### What it does

- Folder tree, path bar, back / forward / up, and the files of the folder (name, date, size).
  The tree follows the folder shown: it expands and selects it as you navigate by any route.
- File > Connect to... opens a window on another file system; Recent Files reopens remote files.
- Open (a remote file is downloaded to a temporary copy first), Edit (a remote file is edited as a
  copy and, when it changes, you're asked whether to replace the original).
- Copy / Paste (between windows and servers, and with Finder or Explorer for local files), drag
  and drop onto the file list or a folder in it, New Folder, Rename, Delete.
- Show hidden files and extensions, per file system type; Start Here Next Time.
- File > Search... (Cmd/Ctrl+F) looks in the folder shown and below it: by name (`*` and `?`,
  whole name, case-insensitive unless asked), and under "More choices" by size, date modified and
  type. Results appear as they're found and the search can be canceled; double-click opens one,
  "Show in Folder" goes to its folder. Folders that can't be read are skipped and counted.

Delete and downloads run in the background with a progress window that can cancel them.

### Copy manager

Copy, paste and drag and drop add files to one queue, shared by every window, that copies them one
at a time in the background. A pane along the bottom of each window shows the file being copied and
how many of the batch are done; it stays hidden until something is copied.

- Pause, Stop This File, Stop All (a stopped file's partial copy is removed).
- Details lists every file with its start and finish times and its result; Clear forgets the
  finished ones. Right-click a row to stop it.
- A file that's already there asks Replace / Replace All / Skip / Skip All / Cancel before anything
  is copied; the folders are made first, so the tree and file list update when the batch ends.
- A file that fails is recorded and the rest go on (the Swing viewer showed a dialog for each).

### Server certificates

Connections to a server whose certificate the JVM's trust store doesn't trust (a self-signed SFTP
or FTPS server, say) ask first, in a JavaFX dialog, instead of failing:

- It shows the server, who the certificate was issued to and by, when it's valid, and its SHA-256
  and SHA-1 fingerprints (selectable, to compare with what the server's administrator gave you).
- It warns about what's visibly wrong: expired, not yet valid, issued for a different server,
  signed by the server itself.
- Reject is the default button and what closing the window means. "Accept This Time" lasts for the
  session; "Always Accept" is remembered for that server and certificate only (parley-net's
  `DynamicTrustManager` keeps it in `~/.bjlTructed`, shared with the Swing viewer).
- Several connections to one server ask once.

`FxCertificateValidator.install()` does the wiring; `ViewerApp` calls it at start.

### Not yet ported from the Swing viewer

The external application chooser (registry), the terminal,
font and theme settings.

## Building

```bash
mvn package
```

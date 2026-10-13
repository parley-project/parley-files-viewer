#!/bin/sh
# Records the reachability metadata (reflection, JNI, resources) the native image of the Parley Files
# Viewer needs, by running the real viewer on a JVM under GraalVM's tracing agent. Run it on each
# platform the native image is built for (the AWT/Swing metadata is platform specific: the Windows
# entries are already in the file; Linux and macOS add theirs), then commit the updated file.
#
#   tools/record-native-config.sh
#
# Needs a GraalVM JDK (java -version says GraalVM) as the JAVA_HOME or on PATH, and Maven.
# The agent merges into the existing file, so it only ever adds. Use the viewer the way you would:
# browse local and remote file systems, open a file, copy, search, the connection dialog, settings,
# a terminal ("exit" in it), Help > About. Whatever is not used is not recorded, and fails in the
# native image (reflection/JNI "not found"). Close the viewer normally when done: the file is
# written on exit.

set -e
project=$(cd "$(dirname "$0")/.." && pwd)
cd "$project"

out=$project/src/main/resources/META-INF/native-image/us.bringardner.parley/parley-files-viewer
mkdir -p "$out"

mvn -B -q compile
mvn -B -q dependency:build-classpath -Dmdep.outputFile=target/record-classpath.txt
cp=$(cat target/record-classpath.txt)
case $(uname -s) in
MINGW*|MSYS*|CYGWIN*) sep=';' ;;
*) sep=':' ;;
esac

java -agentlib:native-image-agent=config-merge-dir="$out" \
	-cp "target/classes$sep$cp" us.bringardner.parley.files.viewer.FileSourceViewer

echo "Recorded into $out"

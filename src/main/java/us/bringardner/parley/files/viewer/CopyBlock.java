package us.bringardner.parley.files.viewer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.StreamOptions;
import us.bringardner.parley.io.IoUtils;

/**
 * How much a copy moves at a time. Each file source knows what suits it (an SFTP connection
 * its chunk, a database its row size, an FTP connection its transfer buffer), so a copy asks
 * both ends and uses the larger, and opens its streams with that size so their buffers match
 * the loop. It used to be a number written into each copy loop (1 KB in one, 100 KB in the other).
 */
final class CopyBlock {

	/** Not less than this, however small a source says it wants. */
	static final int MIN = 8 * 1024;
	/** Not more than this: a bigger block makes progress and cancel coarse, and holds memory. */
	static final int MAX = 1024 * 1024;
	/** When neither end says. */
	static final int FALLBACK = IoUtils.BUFFER_SIZE;

	private CopyBlock() {
	}

	/**
	 * @return the block to copy from one source to the other: the larger of what the two say
	 * they want, within MIN and MAX, or FALLBACK if neither says
	 */
	static int sizeFor(FileSource from, FileSource to) {
		int size = Math.max(wanted(from), wanted(to));
		if( size <= 0 ) {
			size = FALLBACK;
		}
		return Math.max(MIN, Math.min(MAX, size));
	}

	private static int wanted(FileSource f) {
		try {
			return f == null ? 0 : f.getStreamDefaults().bufferSize();
		} catch (RuntimeException e) {
			// a source that can't say is the same as one that doesn't
			return 0;
		}
	}

	/** The options to open both streams of a copy with. */
	static StreamOptions optionsFor(int block) {
		return StreamOptions.buffer(block);
	}

	/** Copies from to to, with the block size they agree on; both streams are closed. */
	static long copy(FileSource from, FileSource to) throws IOException {
		int block = sizeFor(from, to);
		StreamOptions options = optionsFor(block);
		long total = 0;
		try (InputStream in = from.getInputStream(options); OutputStream out = to.getOutputStream(false, options)) {
			byte[] buffer = new byte[block];
			int n;
			while( (n = in.read(buffer)) >= 0 ) {
				out.write(buffer, 0, n);
				total += n;
			}
		}
		return total;
	}
}

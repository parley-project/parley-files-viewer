package us.bringardner.parley.files.viewer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import us.bringardner.parley.files.FileSource;
import us.bringardner.parley.files.StreamOptions;
import us.bringardner.parley.files.memory.MemoryFileSourceFactory;

/**
 * A copy asks both ends what suits them, moves that much at a time, and opens its streams with
 * the same size.
 */
public class CopyBlockTest {

	/** A file source that says what it wants and records how it was opened. */
	private static final class Spy implements InvocationHandler {
		final FileSource delegate;
		final int wants;
		final List<StreamOptions> inputs = new ArrayList<>();
		final List<StreamOptions> outputs = new ArrayList<>();
		boolean failsToSayWhatItWants;

		Spy(FileSource delegate, int wants) {
			this.delegate = delegate;
			this.wants = wants;
		}

		FileSource source() {
			return (FileSource) Proxy.newProxyInstance(FileSource.class.getClassLoader(),
					new Class<?>[] { FileSource.class }, this);
		}

		@Override
		public Object invoke(Object proxy, java.lang.reflect.Method m, Object[] args) throws Throwable {
			try {
				switch (m.getName()) {
				case "getStreamDefaults":
					if( failsToSayWhatItWants ) {
						throw new IllegalStateException("not connected");
					}
					return wants > 0 ? StreamOptions.buffer(wants) : StreamOptions.NONE;
				case "getInputStream":
					if( args != null && args.length == 1 && args[0] instanceof StreamOptions ) {
						inputs.add((StreamOptions) args[0]);
						return delegate.getInputStream();
					}
					break;
				case "getOutputStream":
					if( args != null && args.length == 2 && args[1] instanceof StreamOptions ) {
						outputs.add((StreamOptions) args[1]);
						return delegate.getOutputStream((Boolean) args[0]);
					}
					break;
				default:
					break;
				}
				return m.invoke(delegate, args);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}
	}

	private static FileSource memory(String name) throws Exception {
		return new MemoryFileSourceFactory().createFileSource("/" + name);
	}

	private static byte[] put(FileSource f, int size) throws Exception {
		byte[] data = new byte[size];
		new Random(size).nextBytes(data);
		try (OutputStream out = f.getOutputStream()) {
			out.write(data);
		}
		return data;
	}

	// ------------------------------------------------------------ the size

	@Test
	void usesTheLargerOfWhatTheTwoEndsSay() throws Exception {
		FileSource a = new Spy(memory("a1"), 32 * 1024).source();
		FileSource b = new Spy(memory("b1"), 128 * 1024).source();
		assertEquals(128 * 1024, CopyBlock.sizeFor(a, b));
		assertEquals(128 * 1024, CopyBlock.sizeFor(b, a));
	}

	@Test
	void fallsBackWhenNeitherSaysAnything() throws Exception {
		assertEquals(CopyBlock.FALLBACK, CopyBlock.sizeFor(memory("a2"), memory("b2")));
		FileSource one = new Spy(memory("a3"), 0).source();
		assertEquals(CopyBlock.FALLBACK, CopyBlock.sizeFor(one, null));
	}

	@Test
	void keptWithinTheLimits() throws Exception {
		FileSource tiny = new Spy(memory("a4"), 100).source();
		FileSource huge = new Spy(memory("b4"), 64 * 1024 * 1024).source();
		assertEquals(CopyBlock.MIN, CopyBlock.sizeFor(tiny, tiny));
		assertEquals(CopyBlock.MAX, CopyBlock.sizeFor(huge, tiny));
	}

	@Test
	void aSourceThatCantSayIsTheSameAsOneThatDoesntWant() throws Exception {
		Spy broken = new Spy(memory("a5"), 256 * 1024);
		broken.failsToSayWhatItWants = true;
		FileSource ok = new Spy(memory("b5"), 16 * 1024).source();
		assertEquals(16 * 1024, CopyBlock.sizeFor(broken.source(), ok));
	}

	// ------------------------------------------------------------ the copy

	@Test
	void copyOpensBothStreamsWithTheBlockAndMovesEveryByte() throws Exception {
		FileSource plainSource = memory("from6");
		byte[] data = put(plainSource, 300_000);
		Spy from = new Spy(plainSource, 64 * 1024);
		Spy to = new Spy(memory("to6"), 200 * 1024);

		long copied = CopyBlock.copy(from.source(), to.source());
		assertEquals(data.length, copied);

		StreamOptions expected = StreamOptions.buffer(200 * 1024);
		assertEquals(List.of(expected), from.inputs, "the source was opened with the agreed size");
		assertEquals(List.of(expected), to.outputs, "so was the destination");
		try (InputStream in = to.delegate.getInputStream()) {
			assertArrayEquals(data, in.readAllBytes());
		}
	}

	// ------------------------------------------------------------ the copy thread

	@Test
	void theCopyThreadUsesItToo() throws Exception {
		FileSource plainSource = memory("from7");
		byte[] data = put(plainSource, 150_000);
		Spy from = new Spy(plainSource, 96 * 1024);
		Spy to = new Spy(memory("to7"), 0);

		CopyThread t = new CopyThread(from.source(), to.source(), null);
		t.start();
		t.join(20_000);
		assertFalse(t.isRunning(), "the copy should be done");

		StreamOptions expected = StreamOptions.buffer(96 * 1024);
		assertEquals(List.of(expected), from.inputs);
		assertEquals(List.of(expected), to.outputs);
		try (InputStream in = to.delegate.getInputStream()) {
			assertArrayEquals(data, in.readAllBytes());
		}
		assertTrue(to.delegate.length() == data.length);
	}
}

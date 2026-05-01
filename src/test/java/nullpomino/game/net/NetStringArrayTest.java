package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link NetStringArray}'s reader and writer that the netplay
 * packet codecs use to serialise and deserialise tab-delimited strings.
 * Lives under the same package as the type so the package-private
 * Reader / Writer constructors are callable.
 */
class NetStringArrayTest {

	@Test
	void writerIndexesStartAtZeroAndAdvancePerWrite() {
		NetStringArray.Writer writer = new NetStringArray.Writer(4);

		writer.write("alpha");
		writer.write(7);
		writer.write(true);
		writer.write("omega");

		assertArrayEquals(
				new String[] {"alpha", "7", "true", "omega"},
				writer.values());
	}

	@Test
	void writeEncodedRoundTripsThroughUrlEncoder() {
		NetStringArray.Writer writer = new NetStringArray.Writer(2);
		writer.writeEncoded("hello world");
		writer.writeEncoded("a/b+c");

		String[] values = writer.values();

		// urlEncode replaces space with '+' and slashes with %2F.
		assertEquals("hello+world", values[0]);
		assertEquals("a%2Fb%2Bc", values[1]);

		// Round-trip through Reader.readEncoded recovers the inputs.
		NetStringArray.Reader reader = new NetStringArray.Reader(values);
		assertEquals("hello world", reader.readEncoded());
		assertEquals("a/b+c", reader.readEncoded());
	}

	@Test
	void readerHasNextTracksWhetherTheNextIndexIsInBounds() {
		NetStringArray.Reader reader = new NetStringArray.Reader(
				new String[] {"x", "y"});

		assertTrue(reader.hasNext());
		reader.read();
		assertTrue(reader.hasNext());
		reader.read();
		assertFalse(reader.hasNext());
	}

	@Test
	void readerReadAdvancesTheCursorOnEachCall() {
		NetStringArray.Reader reader = new NetStringArray.Reader(
				new String[] {"first", "second", "third"});

		assertEquals("first", reader.read());
		assertEquals("second", reader.read());
		assertEquals("third", reader.read());
	}

	@Test
	void readerReadIntParsesIntegerToken() {
		NetStringArray.Reader reader = new NetStringArray.Reader(
				new String[] {"42", "-7"});

		assertEquals(42, reader.readInt());
		assertEquals(-7, reader.readInt());
	}

	@Test
	void readerReadBooleanParsesBooleanToken() {
		NetStringArray.Reader reader = new NetStringArray.Reader(
				new String[] {"true", "false", "TRUE", "junk"});

		assertTrue(reader.readBoolean());
		assertFalse(reader.readBoolean());
		// Boolean.parseBoolean is case-insensitive for the literal 'true'.
		assertTrue(reader.readBoolean());
		// Anything else parses to false.
		assertFalse(reader.readBoolean());
	}

	@Test
	void readerOverloadsReturnDefaultsWhenCursorPastEnd() {
		NetStringArray.Reader reader = new NetStringArray.Reader(
				new String[] {"only-value"});

		// First call consumes the only entry; the rest fall through to
		// the documented default-value branch.
		assertEquals("only-value", reader.read("fallback"));
		assertEquals("fallback", reader.read("fallback"));
		assertEquals(99, reader.readInt(99));
		assertTrue(reader.readBoolean(true));
	}

	@Test
	void readerReadIntDefaultPathDoesNotConsumeWhenAvailable() {
		// When the cursor still has values, readInt(default) reads the
		// real value rather than the default.
		NetStringArray.Reader reader = new NetStringArray.Reader(
				new String[] {"123"});

		assertEquals(123, reader.readInt(-1));
	}

	@Test
	void readerReadIntThrowsForNonNumericPayload() {
		NetStringArray.Reader reader = new NetStringArray.Reader(
				new String[] {"not-an-int"});

		assertThrows(NumberFormatException.class, reader::readInt);
	}

	@Test
	void readerReadPastEndThrowsArrayIndexOutOfBounds() {
		NetStringArray.Reader reader = new NetStringArray.Reader(
				new String[] {"only-value"});

		reader.read();
		// The non-default read() does not guard against overflow; pin
		// that contract so callers know to use the default-value
		// overloads when the wire shape may be short.
		assertThrows(ArrayIndexOutOfBoundsException.class, reader::read);
	}

	@Test
	void writerToReaderRoundTripPreservesEveryValue() {
		NetStringArray.Writer writer = new NetStringArray.Writer(5);
		writer.write("ascii");
		writer.write(2024);
		writer.write(false);
		writer.writeEncoded("a b/c");
		writer.write("trailing");

		NetStringArray.Reader reader = new NetStringArray.Reader(writer.values());
		assertEquals("ascii", reader.read());
		assertEquals(2024, reader.readInt());
		assertFalse(reader.readBoolean());
		assertEquals("a b/c", reader.readEncoded());
		assertEquals("trailing", reader.read());
		assertFalse(reader.hasNext());
	}
}

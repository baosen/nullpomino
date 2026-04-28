package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pins the CustomProperties overloads the codebase actually exercises.
 * Guards the dead-overload cleanup that removes byte / short / char /
 * double get/set pairs — grep turned up zero callers for those, but
 * if someone reaches for them in a future commit this test keeps the
 * kept surface (int/long/float/boolean/String) visible.
 */
class CustomPropertiesRoundTripTest {

	@TempDir
	Path tempDir;

	@Test
	void intRoundTrip() {
		CustomProperties p = new CustomProperties();
		p.setProperty("i", 42);
		assertEquals(42, p.getProperty("i", -1));
		assertEquals(-1, p.getProperty("missing", -1));
	}

	@Test
	void longRoundTrip() {
		CustomProperties p = new CustomProperties();
		p.setProperty("l", 1234567890123L);
		assertEquals(1234567890123L, p.getProperty("l", 0L));
		assertEquals(0L, p.getProperty("missing", 0L));
	}

	@Test
	void floatRoundTrip() {
		CustomProperties p = new CustomProperties();
		p.setProperty("f", 6.5f);
		assertEquals(6.5f, p.getProperty("f", 0f));
		assertEquals(0f, p.getProperty("missing", 0f));
	}

	@Test
	void doubleRoundTrip() {
		CustomProperties p = new CustomProperties();
		p.setProperty("d", 3.141592653589793);
		assertEquals(3.141592653589793, p.getProperty("d", 0.0));
		assertEquals(0.0, p.getProperty("missing", 0.0));
	}

	@Test
	void booleanRoundTrip() {
		CustomProperties p = new CustomProperties();
		p.setProperty("b", true);
		assertEquals(true, p.getProperty("b", false));
		p.setProperty("b", false);
		assertEquals(false, p.getProperty("b", true));
		assertEquals(true, p.getProperty("missing", true));
		p.setProperty("b", "not-a-boolean");
		assertEquals(false, p.getProperty("b", true));
	}

	@Test
	void numericGettersFallBackToDefaultOnMalformed() {
		CustomProperties p = new CustomProperties();
		p.setProperty("bad", "not-a-number");
		p.setProperty("empty", "");
		assertEquals(7, p.getProperty("bad", 7));
		assertEquals(7L, p.getProperty("bad", 7L));
		assertEquals(7f, p.getProperty("bad", 7f));
		assertEquals(7.0, p.getProperty("bad", 7.0));
		assertEquals(7, p.getProperty("empty", 7));
	}

	@Test
	void encodeDecodeRoundTripsStoredProperties() {
		CustomProperties source = new CustomProperties();
		source.setProperty("name", "Nullpo Mino");
		source.setProperty("unicode", "テスト");
		source.setProperty("enabled", true);

		CustomProperties decoded = new CustomProperties();
		assertTrue(decoded.decode(source.encode("roundtrip")));

		assertEquals("Nullpo Mino", decoded.getProperty("name"));
		assertEquals("テスト", decoded.getProperty("unicode"));
		assertEquals(true, decoded.getProperty("enabled", false));
	}

	@Test
	void decodeReturnsFalseForMalformedUrlEncoding() {
		CustomProperties decoded = new CustomProperties();

		assertFalse(decoded.decode("%"));
	}

	@Test
	void loadFromFileReadsProperties() throws IOException {
		Path file = tempDir.resolve("sample.properties");
		Files.write(file, "name=Nullpo\nvalue=42\n".getBytes(StandardCharsets.UTF_8));

		CustomProperties properties = CustomProperties.loadFromFile(file.toString());

		assertEquals("Nullpo", properties.getProperty("name"));
		assertEquals(42, properties.getProperty("value", 0));
	}

	@Test
	void storeToFileWritesLoadableProperties() throws IOException {
		Path file = tempDir.resolve("stored.properties");
		CustomProperties source = new CustomProperties();
		source.setProperty("name", "Nullpo");
		source.setProperty("value", 42);

		source.storeToFile(file.toString(), "test");
		CustomProperties loaded = CustomProperties.loadFromFile(file.toString());

		assertEquals("Nullpo", loaded.getProperty("name"));
		assertEquals(42, loaded.getProperty("value", 0));
	}
}

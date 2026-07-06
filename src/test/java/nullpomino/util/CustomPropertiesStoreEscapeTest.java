package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * Covers the remaining escape branches of the hand-rolled
 * {@link CustomProperties#store} serializer: every control-character
 * case arm, leading/inner spaces in keys and values, the null-comment
 * and null-value paths, and the unicode-escape arm.
 */
public class CustomPropertiesStoreEscapeTest {
	private static String store(CustomProperties props, String comments) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		props.store(out, comments);
		return out.toString(StandardCharsets.ISO_8859_1);
	}

	@Test
	public void escapesEveryControlCharacterArm() throws Exception {
		CustomProperties props = new CustomProperties();
		props.setProperty("k", "a\\b\tc\nd\re\ff g=h:i#j!k");
		String text = store(props, null);
		assertTrue(text.contains("\\\\"));
		assertTrue(text.contains("\\t"));
		assertTrue(text.contains("\\n"));
		assertTrue(text.contains("\\r"));
		assertTrue(text.contains("\\f"));
		assertTrue(text.contains("\\="));
		assertTrue(text.contains("\\:"));
		assertTrue(text.contains("\\#"));
		assertTrue(text.contains("\\!"));

		Properties reloaded = new Properties();
		reloaded.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.ISO_8859_1)));
		assertEquals(props.getProperty("k"), reloaded.getProperty("k"));
	}

	@Test
	public void escapesSpacesInKeysAndLeadingSpacesInValues() throws Exception {
		CustomProperties props = new CustomProperties();
		props.setProperty("a b", " leading and inner");
		String text = store(props, "with comments");
		assertTrue(text.startsWith("#with comments\n"));
		assertTrue(text.contains("a\\ b="));
		assertTrue(text.contains("=\\ leading and inner"));
		assertFalse(text.contains("inner\\ "));

		Properties reloaded = new Properties();
		reloaded.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.ISO_8859_1)));
		assertEquals(" leading and inner", reloaded.getProperty("a b"));
	}

	@Test
	public void emitsUnicodeEscapesOutsidePrintableAscii() throws Exception {
		CustomProperties props = new CustomProperties();
		props.setProperty("jp", "あ");
		String text = store(props, null);
		assertTrue(text.contains("\\u3042"));
		assertTrue(text.contains("\\u0001"));
	}

	@Test
	public void nullValueFromOverriddenGetterStoresEmptyString() throws Exception {
		CustomProperties props = new CustomProperties() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getProperty(String key) {
				return null;
			}
		};
		props.setProperty("k", "v");
		assertEquals("k=\n", store(props, null));
	}

	@Test
	public void commentNewlinesAreFlattened() throws Exception {
		CustomProperties props = new CustomProperties();
		String text = store(props, "line1\r\nline2");
		assertEquals("#line1  line2\n", text);
	}
}

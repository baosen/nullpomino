// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * StringSet of properties that can be stored in non-
 */
public class CustomProperties extends Properties {
	/**
	 * Serial version
	 */
	private static final long serialVersionUID = 2L;

	/**
	 * Invoked with the logical filename after every successful
	 * {@link #storeToFile}. The browser build installs a listener that mirrors
	 * the written file into persistent browser storage; unset on desktop.
	 */
	public static volatile Consumer<String> storeListener;

	public static CustomProperties loadFromFile(String filename) throws IOException {
		CustomProperties properties = new CustomProperties();
		try (FileInputStream in = new FileInputStream(DataDir.path(filename))) {
			properties.load(in);
		}
		return properties;
	}

	public static CustomProperties loadFromFileOrEmpty(String filename) {
		try {
			return loadFromFile(filename);
		} catch(IOException e) {
			return new CustomProperties();
		}
	}

	public void storeToFile(String filename, String comments) throws IOException {
		try (FileOutputStream out = new FileOutputStream(DataDir.path(filename))) {
			store(out, comments);
		}
		Consumer<String> listener = storeListener;
		if (listener != null) {
			listener.accept(filename);
		}
	}

	/**
	 * Writes the properties in {@code java.util.Properties} format, but without
	 * delegating to {@link Properties#store(OutputStream, String)}: TeaVM's
	 * classlib implements that via an {@code "ISO8859_1"} charset alias its
	 * charset registry does not recognize, so it throws there. This
	 * reimplements the same escaping (all output is 7-bit ASCII, with
	 * non-Latin-1 characters emitted as {@code \\uXXXX}), so files written here
	 * are byte-compatible with — and re-loadable by — a stock JDK. The date
	 * comment the JDK adds is omitted; it is purely informational.
	 */
	@Override
	public void store(OutputStream out, String comments) throws IOException {
		StringBuilder sb = new StringBuilder();
		if (comments != null) {
			sb.append('#').append(comments.replace('\r', ' ').replace('\n', ' ')).append('\n');
		}
		for (String key : stringPropertyNames()) {
			String value = getProperty(key);
			sb.append(saveConvert(key, true))
					.append('=')
					.append(saveConvert(value == null ? "" : value, false))
					.append('\n');
		}
		out.write(sb.toString().getBytes(StandardCharsets.ISO_8859_1));
		out.flush();
	}

	/** Escapes a key or value exactly as {@code java.util.Properties} does. */
	private static String saveConvert(String text, boolean escapeSpace) {
		StringBuilder out = new StringBuilder(text.length() * 2);
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			switch (c) {
				case '\\': out.append("\\\\"); break;
				case '\t': out.append("\\t"); break;
				case '\n': out.append("\\n"); break;
				case '\r': out.append("\\r"); break;
				case '\f': out.append("\\f"); break;
				case ' ':
					if (i == 0 || escapeSpace) out.append('\\');
					out.append(' ');
					break;
				case '=':
				case ':':
				case '#':
				case '!':
					out.append('\\').append(c);
					break;
				default:
					if (c < 0x20 || c > 0x7e) {
						out.append("\\u")
								.append(HEX[(c >> 12) & 0xF])
								.append(HEX[(c >> 8) & 0xF])
								.append(HEX[(c >> 4) & 0xF])
								.append(HEX[c & 0xF]);
					} else {
						out.append(c);
					}
			}
		}
		return out.toString();
	}

	private static final char[] HEX = "0123456789abcdef".toCharArray();

	public synchronized Object setProperty(String key, int value) {
		return setProperty(key, String.valueOf(value));
	}

	public synchronized Object setProperty(String key, long value) {
		return setProperty(key, String.valueOf(value));
	}

	public synchronized Object setProperty(String key, float value) {
		return setProperty(key, String.valueOf(value));
	}

	public synchronized Object setProperty(String key, double value) {
		return setProperty(key, String.valueOf(value));
	}

	public synchronized Object setProperty(String key, boolean value) {
		return setProperty(key, String.valueOf(value));
	}

	public int getProperty(String key, int defaultValue) {
		return parseInt(getProperty(key), defaultValue);
	}

	/**
	 * longGets a property of type
	 * @param key Key
	 * @param defaultValue keyStrange that I return if it can not find thecount
	 * @return Integer that corresponds to the specified keycount (Not founddefaultValue)
	 */
	public long getProperty(String key, long defaultValue) {
		return parseLong(getProperty(key), defaultValue);
	}

	/**
	 * floatGets a property of type
	 * @param key Key
	 * @param defaultValue keyStrange that I return if it can not find thecount
	 * @return Integer that corresponds to the specified keycount (Not founddefaultValue)
	 */
	public float getProperty(String key, float defaultValue) {
		return parseFloat(getProperty(key), defaultValue);
	}

	public double getProperty(String key, double defaultValue) {
		return parseDouble(getProperty(key), defaultValue);
	}

	public boolean getProperty(String key, boolean defaultValue) {
		String str = getProperty(key, Boolean.toString(defaultValue));
		return Boolean.parseBoolean(str);
	}

	/**
	 * Converted to a string this property set(URLEncoderEncoded)
	 * @param comments Identifying comment
	 * @return URLEncoderProperty string sets that are encoded in
	 */
	public String encode(String comments) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			store(out, comments);
			String properties = new String(out.toByteArray(), StandardCharsets.UTF_8);
			return URLEncoder.encode(properties, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to encode properties", e);
		}
	}

	/**
	 * encode(String)I encodedStringRestore the property set from
	 * @param source encode(String)I encodedString
	 * @return The successtrue
	 */
	public boolean decode(String source) {
		try {
			String decodedString = URLDecoder.decode(source, StandardCharsets.UTF_8);
			ByteArrayInputStream in = new ByteArrayInputStream(decodedString.getBytes(StandardCharsets.UTF_8));
			load(in);
		} catch (IllegalArgumentException | IOException e) {
			return false;
		}

		return true;
	}

	private static int parseInt(String value, int defaultValue) {
		return parseNumber(value, defaultValue, Integer::parseInt);
	}

	private static long parseLong(String value, long defaultValue) {
		return parseNumber(value, defaultValue, Long::parseLong);
	}

	private static float parseFloat(String value, float defaultValue) {
		return parseNumber(value, defaultValue, Float::parseFloat);
	}

	private static double parseDouble(String value, double defaultValue) {
		return parseNumber(value, defaultValue, Double::parseDouble);
	}

	private static <T> T parseNumber(String value, T defaultValue, NumberParser<T> parser) {
		if(value == null) return defaultValue;

		try {
			return parser.parse(value);
		} catch(NumberFormatException e) {
			return defaultValue;
		}
	}

	private interface NumberParser<T> {
		T parse(String value);
	}
}

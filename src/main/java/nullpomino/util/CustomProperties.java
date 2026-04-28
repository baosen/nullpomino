// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * StringSet of properties that can be stored in non-
 */
public class CustomProperties extends Properties {
	/**
	 * Serial version
	 */
	private static final long serialVersionUID = 2L;

	public static CustomProperties loadFromFile(String filename) throws IOException {
		CustomProperties properties = new CustomProperties();
		try (FileInputStream in = new FileInputStream(filename)) {
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
		try (FileOutputStream out = new FileOutputStream(filename)) {
			store(out, comments);
		}
	}

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

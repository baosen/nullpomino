// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
		if(value == null) return defaultValue;

		try {
			return Integer.parseInt(value);
		} catch(NumberFormatException e) {
			return defaultValue;
		}
	}

	private static long parseLong(String value, long defaultValue) {
		if(value == null) return defaultValue;

		try {
			return Long.parseLong(value);
		} catch(NumberFormatException e) {
			return defaultValue;
		}
	}

	private static float parseFloat(String value, float defaultValue) {
		if(value == null) return defaultValue;

		try {
			return Float.parseFloat(value);
		} catch(NumberFormatException e) {
			return defaultValue;
		}
	}

	private static double parseDouble(String value, double defaultValue) {
		if(value == null) return defaultValue;

		try {
			return Double.parseDouble(value);
		} catch(NumberFormatException e) {
			return defaultValue;
		}
	}
}

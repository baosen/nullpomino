// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
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
		String str = getProperty(key, String.valueOf(defaultValue));

		int result;
		try {
			result = Integer.parseInt(str);
		} catch(NumberFormatException e) {
			result = defaultValue;
		}

		return result;
	}

	/**
	 * longGets a property of type
	 * @param key Key
	 * @param defaultValue keyStrange that I return if it can not find thecount
	 * @return Integer that corresponds to the specified keycount (Not founddefaultValue)
	 */
	public long getProperty(String key, long defaultValue) {
		String str = getProperty(key, String.valueOf(defaultValue));

		long result;
		try {
			result = Long.parseLong(str);
		} catch(NumberFormatException e) {
			result = defaultValue;
		}

		return result;
	}

	/**
	 * floatGets a property of type
	 * @param key Key
	 * @param defaultValue keyStrange that I return if it can not find thecount
	 * @return Integer that corresponds to the specified keycount (Not founddefaultValue)
	 */
	public float getProperty(String key, float defaultValue) {
		String str = getProperty(key, String.valueOf(defaultValue));

		float result;
		try {
			result = Float.parseFloat(str);
		} catch(NumberFormatException e) {
			result = defaultValue;
		}

		return result;
	}

	public double getProperty(String key, double defaultValue) {
		String str = getProperty(key, String.valueOf(defaultValue));

		double result;
		try {
			result = Double.parseDouble(str);
		} catch(NumberFormatException e) {
			result = defaultValue;
		}

		return result;
	}

	public boolean getProperty(String key, boolean defaultValue) {
		String str = getProperty(key, Boolean.toString(defaultValue));
		return Boolean.valueOf(str);
	}

	/**
	 * Converted to a string this property set(URLEncoderEncoded)
	 * @param comments Identifying comment
	 * @return URLEncoderProperty string sets that are encoded in
	 */
	public String encode(String comments) {
		String result = null;

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			store(out, comments);
			result = URLEncoder.encode(out.toString("UTF-8"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new Error("UTF-8 not supported", e);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * encode(String)I encodedStringRestore the property set from
	 * @param source encode(String)I encodedString
	 * @return The successtrue
	 */
	public boolean decode(String source) {
		try {
			String decodedString = URLDecoder.decode(source, "UTF-8");
			ByteArrayInputStream in = new ByteArrayInputStream(decodedString.getBytes("UTF-8"));
			load(in);
		} catch (UnsupportedEncodingException e) {
			throw new Error("UTF-8 not supported", e);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
}

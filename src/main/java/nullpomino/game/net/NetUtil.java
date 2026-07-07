// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;


/**
 * Network utils
 */
public class NetUtil {
	private static final int ZIP_BUFFER_SIZE = 1024;

	public interface PacketHandler {
		void handle(String packet) throws IOException;
	}

	/**
	 * Convert byte[] to String (with UTF-8 encoding)
	 * @param bytes Byte array (byte[])
	 * @return String
	 */
	public static String bytesToString(byte[] bytes) {
		return new String(bytes, StandardCharsets.UTF_8);
	}

	/**
	 * Convert String to byte[] (with UTF-8 encoding)
	 * @param str String
	 * @return Byte array (byte[])
	 */
	public static byte[] stringToBytes(String str) {
		return str.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Encode non-URL-safe characters with using URLEncoder
	 * @param str String
	 * @return URLEncoder-encoded String
	 */
	public static String urlEncode(String str) {
		return URLEncoder.encode(str, StandardCharsets.UTF_8);
	}

	/**
	 * Decode URL-safe characters with using URLDecoder
	 * @param str URLEncoder-encoded String
	 * @return Decoded String
	 */
	public static String urlDecode(String str) {
		return URLDecoder.decode(str, StandardCharsets.UTF_8);
	}

	public static StringBuilder processPacketBuffer(StringBuilder partialPacket,
			String message, PacketHandler handler) throws IOException {
		StringBuilder packetBuffer = new StringBuilder();
		if(partialPacket != null) packetBuffer.append(partialPacket);
		packetBuffer.append(message);

		int index;
		while((index = packetBuffer.indexOf("\n")) != -1) {
			handler.handle(packetBuffer.substring(0, index));
			packetBuffer.delete(0, index + 1);
		}

		return packetBuffer.length() > 0 ? packetBuffer : null;
	}


	/**
	 * Compress a byte array (byte[]). The compression level is 9.<br>
	 * <a href="http://www.exampledepot.com/egs/java.util.zip/CompArray.html">Source</a>
	 * @param input Raw byte array (byte[])
	 * @return Compressed byte array (byte[])
	 */
	public static byte[] compressByteArray(byte[] input) {
		return compressByteArray(input, Deflater.BEST_COMPRESSION);
	}

	/**
	 * Compress a byte array (byte[]).<br>
	 * <a href="http://www.exampledepot.com/egs/java.util.zip/CompArray.html">Source</a>
	 * @param input Raw byte array (byte[])
	 * @param level Compression level (0-9)
	 * @return Compressed byte array (byte[])
	 */
	public static byte[] compressByteArray(byte[] input, int level) {
		Deflater compressor = new Deflater(level);
		try {
			compressor.setInput(input);
			compressor.finish();

			ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
			byte[] buf = new byte[ZIP_BUFFER_SIZE];
			while(!compressor.finished()) {
				int count = compressor.deflate(buf);
				bos.write(buf, 0, count);
			}

			return bos.toByteArray();
		} finally {
			compressor.end();
		}
	}

	/**
	 * Decompress a byte array (byte[])
	 * @param compressedData Compressed byte array (byte[])
	 * @return Raw byte array (byte[])
	 */
	public static byte[] decompressByteArray(byte[] compressedData) {
		Inflater decompressor = new Inflater();
		try {
			decompressor.setInput(compressedData);

			ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length);
			byte[] buf = new byte[ZIP_BUFFER_SIZE];
			while(!decompressor.finished()) {
				int count = decompressor.inflate(buf);
				if((count == 0) && decompressor.needsInput() && !decompressor.finished()) {
					throw new RuntimeException("This byte array is not a valid compressed data");
				}
				bos.write(buf, 0, count);
			}

			return bos.toByteArray();
		} catch (DataFormatException e) {
			throw new RuntimeException("This byte array is not a valid compressed data", e);
		} finally {
			decompressor.end();
		}
	}

	/**
	 * Compress a String then encode with Base64. The compression level is 9.
	 * @param input String you want to compress
	 * @return Compressed + Base64 encoded String
	 */
	public static String compressString(String input) {
		return compressString(input, Deflater.BEST_COMPRESSION);
	}

	/**
	 * Compress a String then encode with Base64.
	 * @param input String you want to compress
	 * @param level Compression level (0-9)
	 * @return Compressed + Base64 encoded String
	 */
	public static String compressString(String input, int level) {
		byte[] bCompressed = compressByteArray(stringToBytes(input), level);
		return Base64.getEncoder().encodeToString(bCompressed);
	}

	/**
	 * Decompress a Base64 encoded String
	 * @param input Compressed + Base64 encoded String
	 * @return Raw String
	 */
	public static String decompressString(String input) {
		byte[] bCompressed = Base64.getDecoder().decode(input);
		byte[] bDecompressed = decompressByteArray(bCompressed);
		return bytesToString(bDecompressed);
	}
}

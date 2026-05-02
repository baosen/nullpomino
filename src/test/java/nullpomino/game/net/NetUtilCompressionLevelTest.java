package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

import org.junit.jupiter.api.Test;

/**
 * Pins the explicit-level overloads of {@link NetUtil#compressByteArray}
 * and {@link NetUtil#compressString}. The no-arg overloads default to
 * {@link Deflater#BEST_COMPRESSION} (level 9); the int-level overloads
 * accept any 0..9 and produce decompressible output via the standard
 * decompressByteArray / decompressString inverses.
 */
class NetUtilCompressionLevelTest {

	@Test
	void compressByteArrayDefaultDelegatesToBestCompressionLevel() {
		// Default overload must produce identical bytes to the
		// explicit BEST_COMPRESSION (9) call.
		byte[] payload = "ababababababababababababababab".getBytes(StandardCharsets.UTF_8);

		assertArrayEquals(
				NetUtil.compressByteArray(payload, Deflater.BEST_COMPRESSION),
				NetUtil.compressByteArray(payload),
				"no-arg overload must match level=9 explicit overload byte-for-byte");
	}

	@Test
	void compressByteArrayWithEachLevelRoundTrips() {
		byte[] payload = "Hello, NullpoMino!".getBytes(StandardCharsets.UTF_8);

		// Levels 0..9 are valid; 0 = no compression, 9 = best.
		for(int level = 0; level <= 9; level++) {
			byte[] compressed = NetUtil.compressByteArray(payload, level);
			assertNotNull(compressed);
			assertArrayEquals(payload, NetUtil.decompressByteArray(compressed),
					"level " + level + " round-trip");
		}
	}

	@Test
	void compressByteArrayLevelZeroProducesLargerOutputThanBestForRepetitivePayload() {
		// Highly repetitive payload: level 0 (no compression) is
		// strictly larger than level 9 (best compression). Pin that
		// the level argument actually flows through.
		byte[] repetitive = new byte[2048];
		for(int i = 0; i < repetitive.length; i++) repetitive[i] = (byte) ('A' + (i % 4));

		byte[] noComp = NetUtil.compressByteArray(repetitive, 0);
		byte[] bestComp = NetUtil.compressByteArray(repetitive, 9);

		assert(noComp.length > bestComp.length)
				: "level 0 should produce larger output than level 9 on a repetitive payload "
						+ "(noComp=" + noComp.length + ", bestComp=" + bestComp.length + ")";
	}

	@Test
	void compressByteArrayHandlesEmptyInputAtAnyLevel() {
		// Empty input compresses to a non-empty deflate trailer at
		// every level; round-trip must still recover an empty array.
		for(int level = 0; level <= 9; level++) {
			byte[] compressed = NetUtil.compressByteArray(new byte[0], level);
			assertNotNull(compressed);
			assertArrayEquals(new byte[0],
					NetUtil.decompressByteArray(compressed),
					"empty round-trip at level " + level);
		}
	}

	@Test
	void compressStringDefaultDelegatesToBestCompressionLevel() {
		String payload = "the quick brown fox jumps over the lazy dog";

		assertEquals(
				NetUtil.compressString(payload, Deflater.BEST_COMPRESSION),
				NetUtil.compressString(payload),
				"no-arg compressString must match level=9 overload");
	}

	@Test
	void compressStringWithEachLevelRoundTrips() {
		String payload = "Test payload with spaces, punctuation; and digits 0123456789.";

		for(int level = 0; level <= 9; level++) {
			String compressed = NetUtil.compressString(payload, level);
			assertNotNull(compressed);
			assertEquals(payload, NetUtil.decompressString(compressed),
					"level " + level + " string round-trip");
		}
	}

	@Test
	void compressStringLevelZeroProducesLargerOutputThanBestForRepetitivePayload() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < 200; i++) sb.append("alpha-beta-gamma");

		String noComp = NetUtil.compressString(sb.toString(), 0);
		String bestComp = NetUtil.compressString(sb.toString(), 9);

		assert(noComp.length() > bestComp.length())
				: "level 0 string output should be larger than level 9 "
						+ "(noComp=" + noComp.length() + ", bestComp=" + bestComp.length() + ")";
	}

	@Test
	void compressStringRoundTripsUnicodePayloadAcrossLevels() {
		// UTF-8 stringToBytes -> compress -> decompress -> bytesToString
		// must preserve multi-byte characters at every level.
		String unicode = "テスト→日本語→Compression";

		for(int level : new int[] {0, 1, 5, 9}) {
			assertEquals(unicode, NetUtil.decompressString(
					NetUtil.compressString(unicode, level)),
					"unicode round-trip at level " + level);
		}
	}
}

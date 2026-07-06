package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Closes the remaining branch gap in {@link NetUtil#decompressByteArray}
 * (L148): a zero-output inflate step whose input is not exhausted, i.e.
 * {@code needsInput()} is false. An empty payload followed by trailing
 * garbage finishes the stream on a zero-count step while a byte is still
 * pending in the input buffer.
 */
class NetUtilBranchGapTest {

	@Test
	void decompressEmptyPayloadWithTrailingGarbageReturnsEmptyArray() {
		byte[] compressed = NetUtil.compressByteArray(new byte[0]);
		byte[] padded = Arrays.copyOf(compressed, compressed.length + 1);
		padded[compressed.length] = 0x42; // unread trailing byte

		byte[] result = NetUtil.decompressByteArray(padded);

		assertEquals(0, result.length);
	}

	@Test
	void decompressEmptyPayloadWithoutTrailingBytesReturnsEmptyArray() {
		byte[] compressed = NetUtil.compressByteArray(new byte[0]);
		assertArrayEquals(new byte[0], NetUtil.decompressByteArray(compressed));
	}
}

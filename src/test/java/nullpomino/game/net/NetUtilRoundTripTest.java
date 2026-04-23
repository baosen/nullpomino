package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pins NetUtil's compress-then-Base64 wire helpers. Both paths are
 * used to serialise compressed field state and rule data onto the
 * protocol stream. Must remain byte-identical across the Phase 1c
 * Base64Coder → java.util.Base64 swap.
 */
class NetUtilRoundTripTest {

	@Test
	void compressRoundTripsAsciiPayload() {
		String payload = "player=HAL\tscore=9001\tfield=..##...####....####....####....####....####.\n";
		String compressed = NetUtil.compressString(payload);
		assertEquals(payload, NetUtil.decompressString(compressed));
	}

	@Test
	void compressRoundTripsEmptyString() {
		assertEquals("", NetUtil.decompressString(NetUtil.compressString("")));
	}

	@Test
	void compressRoundTripsHighlyRepetitivePayload() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 1024; i++) sb.append("abcdefgh");
		String payload = sb.toString();
		assertEquals(payload, NetUtil.decompressString(NetUtil.compressString(payload)));
	}
}

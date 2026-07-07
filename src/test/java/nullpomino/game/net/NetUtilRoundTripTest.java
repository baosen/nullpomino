package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Pins NetUtil's compress-then-Base64 wire helpers. Both paths are
 * used to serialise compressed field state and rule data onto the
 * protocol stream. Must remain byte-identical across the Phase 1c
 * Base64Coder → java.util.Base64 swap.
 */
class NetUtilRoundTripTest {

	@Test
	void utf8HelpersRoundTripUnicode() {
		String payload = "name=Nullpo Mino; note=テスト";
		assertEquals(payload, NetUtil.bytesToString(NetUtil.stringToBytes(payload)));
		assertArrayEquals(payload.getBytes(StandardCharsets.UTF_8), NetUtil.stringToBytes(payload));
	}

	@Test
	void urlHelpersRoundTripReservedCharacters() {
		String payload = "room=Alpha Beta&message=a+b=c?";
		String encoded = NetUtil.urlEncode(payload);
		assertEquals("room%3DAlpha+Beta%26message%3Da%2Bb%3Dc%3F", encoded);
		assertEquals(payload, NetUtil.urlDecode(encoded));
	}

	@Test
	void packetBufferProcessesCompletePacketsAndReturnsTail() throws Exception {
		List<String> packets = new ArrayList<String>();

		StringBuilder tail = NetUtil.processPacketBuffer(null, "one\ntwo\nthr",
				packet -> packets.add(packet));
		assertEquals(Arrays.asList("one", "two"), packets);
		assertEquals("thr", tail.toString());

		tail = NetUtil.processPacketBuffer(tail, "ee\n", packet -> packets.add(packet));
		assertEquals(Arrays.asList("one", "two", "three"), packets);
		assertNull(tail);
	}

	@Test
	void packetBufferPreservesEmptyPackets() throws Exception {
		List<String> packets = new ArrayList<String>();

		StringBuilder tail = NetUtil.processPacketBuffer(null, "alpha\n\nomega",
				packet -> packets.add(packet));

		assertEquals(Arrays.asList("alpha", ""), packets);
		assertEquals("omega", tail.toString());
	}

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
	void byteArrayCompressionRoundTripsEmptyInput() {
		assertArrayEquals(new byte[0], NetUtil.decompressByteArray(NetUtil.compressByteArray(new byte[0])));
	}

	@Test
	void decompressRejectsInvalidCompressedBytes() {
		assertThrows(RuntimeException.class, () -> NetUtil.decompressByteArray(new byte[0]));
		assertThrows(RuntimeException.class, () -> NetUtil.decompressByteArray(new byte[] {1, 2, 3}));
	}

	@Test
	void compressRoundTripsHighlyRepetitivePayload() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 1024; i++) sb.append("abcdefgh");
		String payload = sb.toString();
		assertEquals(payload, NetUtil.decompressString(NetUtil.compressString(payload)));
	}
}

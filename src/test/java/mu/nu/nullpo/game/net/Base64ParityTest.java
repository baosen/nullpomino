package mu.nu.nullpo.game.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Base64;
import java.util.Random;

import biz.source_code.base64Coder.Base64Coder;

import org.junit.jupiter.api.Test;

/**
 * Pins that the vendored Base64Coder and java.util.Base64 produce
 * byte-identical output on every input we actually feed into the
 * wire. The Phase 1c refactor swaps every Base64Coder call for the
 * stdlib; this test confirms the swap is a no-op for the protocol.
 *
 * When Base64Coder itself is removed the assertions below that
 * invoke it must be dropped, but the round-trip assertions keep
 * guarding NetUtil's behaviour.
 */
class Base64ParityTest {

	@Test
	void emptyInput() {
		byte[] in = new byte[0];
		assertEquals(Base64.getEncoder().encodeToString(in), new String(Base64Coder.encode(in)));
		assertArrayEquals(Base64.getDecoder().decode(""), Base64Coder.decode(""));
	}

	@Test
	void singleByte() {
		byte[] in = {0x5a};
		assertEquals(Base64.getEncoder().encodeToString(in), new String(Base64Coder.encode(in)));
	}

	@Test
	void asciiString() {
		byte[] in = "NullpoMino netplay admin password check".getBytes();
		String viaStdlib = Base64.getEncoder().encodeToString(in);
		String viaVendor = new String(Base64Coder.encode(in));
		assertEquals(viaStdlib, viaVendor);
		assertArrayEquals(in, Base64.getDecoder().decode(viaVendor));
		assertArrayEquals(in, Base64Coder.decode(viaStdlib));
	}

	@Test
	void randomBuffer() {
		byte[] in = new byte[4096];
		new Random(0x5eed).nextBytes(in);
		String viaStdlib = Base64.getEncoder().encodeToString(in);
		String viaVendor = new String(Base64Coder.encode(in));
		assertEquals(viaStdlib, viaVendor);
		assertArrayEquals(in, Base64.getDecoder().decode(viaVendor));
		assertArrayEquals(in, Base64Coder.decode(viaStdlib));
	}

	@Test
	void everyTrailingLengthFromOneByteTo100() {
		byte[] base = new byte[100];
		new Random(0xc0ffee).nextBytes(base);
		for (int len = 1; len <= base.length; len++) {
			byte[] chunk = new byte[len];
			System.arraycopy(base, 0, chunk, 0, len);
			assertEquals(
					Base64.getEncoder().encodeToString(chunk),
					new String(Base64Coder.encode(chunk)),
					"encode parity fails at length " + len);
		}
	}
}

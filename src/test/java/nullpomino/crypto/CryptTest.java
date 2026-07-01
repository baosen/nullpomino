package nullpomino.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class CryptTest {

	@Test
	void cryptStringOverloadMatchesByteArrayOverload() {
		String salt = "ab";
		String original = "secret";

		String fromString = Crypt.crypt(salt, original);
		String fromBytes = Crypt.crypt(
				salt.getBytes(StandardCharsets.US_ASCII),
				original.getBytes(StandardCharsets.US_ASCII));

		assertEquals(13, fromString.length());
		assertEquals(fromBytes, fromString);
	}

	@Test
	void cryptStringPadsShortSaltsToTwoCharactersWithA() {
		String shortSalt = "z";
		String paddedSalt = "zA";

		assertEquals(Crypt.crypt(paddedSalt, "pwd"), Crypt.crypt(shortSalt, "pwd"));
	}

	@Test
	void cryptByteArrayPadsShortSaltsWithASCIIA() {
		byte[] empty = new byte[0];
		byte[] paddedSalt = new byte[] { 'A', 'A' };

		String fromEmpty = Crypt.crypt(empty, "pwd".getBytes(StandardCharsets.US_ASCII));
		String fromPadded = Crypt.crypt(paddedSalt, "pwd".getBytes(StandardCharsets.US_ASCII));

		assertEquals(fromPadded, fromEmpty);
	}

	@Test
	void cryptByteBufferDelegatesToByteArray() {
		byte[] saltBytes = "Xy".getBytes(StandardCharsets.US_ASCII);
		byte[] originalBytes = "password".getBytes(StandardCharsets.US_ASCII);

		String fromBytes = Crypt.crypt(saltBytes, originalBytes);
		String fromBuffer = Crypt.crypt(ByteBuffer.wrap(saltBytes), ByteBuffer.wrap(originalBytes));

		assertEquals(fromBytes, fromBuffer);
	}
}

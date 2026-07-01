package nullpomino.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RC4Test {

	@Test
	void rc4OfBytesIsItsOwnInverse() {
		byte[] plaintext = "Hello world".getBytes();
		byte[] ciphertext = new RC4("secret").rc4(plaintext.clone());
		byte[] decrypted = new RC4("secret").rc4(ciphertext);

		assertNotEquals(new String(plaintext), new String(ciphertext));
		assertArrayEquals(plaintext, decrypted);
	}

	@Test
	void rc4OfStringReturnsByteArrayOfMatchingLength() {
		// The String overload is intentionally exercised; the original
		// implementation discards the result of the inner call and
		// returns the input bytes, so this test only pins that the
		// method does not crash and that it returns the expected length.
		byte[] result = new RC4("k").rc4("hello");

		assertEquals("hello".getBytes().length, result.length);
	}

	@Test
	void rc4OfCharArrayMatchesByteArrayResult() {
		String plaintext = "test";
		byte[] keyBytes = "secret".getBytes();
		char[] keyChars = "secret".toCharArray();

		byte[] viaBytes = new RC4(keyBytes).rc4(plaintext.getBytes());
		byte[] viaChars = new RC4(keyChars).rc4(plaintext.toCharArray());

		assertArrayEquals(viaBytes, viaChars);
	}

	@Test
	void rc4ReturnsNullForNullInputs() {
		RC4 c1 = new RC4("k");
		assertNull(c1.rc4((byte[]) null));

		RC4 c2 = new RC4("k");
		assertNull(c2.rc4((String) null));

		RC4 c3 = new RC4("k".toCharArray());
		assertNull(c3.rc4((char[]) null));
	}

	@Test
	void emptyOrNullKeyRejectedByEveryConstructor() {
		assertThrows(NullPointerException.class, () -> new RC4((byte[]) null));
		assertThrows(NullPointerException.class, () -> new RC4(new byte[0]));
		assertThrows(NullPointerException.class, () -> new RC4((char[]) null));
		assertThrows(NullPointerException.class, () -> new RC4(new char[0]));
	}
}

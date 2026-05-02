package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link NetUtil#processPacketBuffer} edge cases that the
 * happy-path test in NetUtilRoundTripTest doesn't cover: a null
 * partial buffer with no terminator (returns null since nothing was
 * accumulated), a non-null partial buffer with no terminator
 * (returns the buffer unchanged), an exact-terminator-only payload,
 * and a packet with multiple newlines emitting multiple handler
 * calls.
 */
class NetUtilProcessPacketBufferEdgeCasesTest {

	@Test
	void nullPartialAndEmptyMessageReturnsNull() throws Exception {
		List<String> packets = new ArrayList<>();

		StringBuilder tail = NetUtil.processPacketBuffer(null, "",
				packets::add);

		assertNull(tail,
				"empty input with no partial -> nothing to keep -> null tail");
		assertEquals(0, packets.size(),
				"no terminator -> no handler call");
	}

	@Test
	void nullPartialAndPartialMessageReturnsCarriedTail() throws Exception {
		// "data without newline" should accumulate into the tail.
		List<String> packets = new ArrayList<>();

		StringBuilder tail = NetUtil.processPacketBuffer(null, "abc",
				packets::add);

		assertEquals("abc", tail.toString());
		assertEquals(0, packets.size());
	}

	@Test
	void existingPartialContinuesAccumulationWhenNoNewline() throws Exception {
		// partial="abc", message="def" -> tail="abcdef".
		List<String> packets = new ArrayList<>();
		StringBuilder partial = new StringBuilder("abc");

		StringBuilder tail = NetUtil.processPacketBuffer(partial, "def",
				packets::add);

		assertEquals("abcdef", tail.toString(),
				"existing partial concatenates with new message");
		assertEquals(0, packets.size());
	}

	@Test
	void exactTerminatorEmitsEmptyPacketAndReturnsNullTail() throws Exception {
		// "\n" alone -> emit one empty packet, no tail.
		List<String> packets = new ArrayList<>();

		StringBuilder tail = NetUtil.processPacketBuffer(null, "\n",
				packets::add);

		assertEquals(1, packets.size());
		assertEquals("", packets.get(0));
		assertNull(tail);
	}

	@Test
	void multipleNewlinesInOneCallEmitMultipleHandlerInvocations() throws Exception {
		// "alpha\nbeta\ngamma\n" -> three handler calls, no tail.
		List<String> packets = new ArrayList<>();

		StringBuilder tail = NetUtil.processPacketBuffer(null,
				"alpha\nbeta\ngamma\n", packets::add);

		assertEquals(3, packets.size());
		assertEquals("alpha", packets.get(0));
		assertEquals("beta", packets.get(1));
		assertEquals("gamma", packets.get(2));
		assertNull(tail);
	}

	@Test
	void partialAndMessageWithMidBufferTerminatorCombinesIntoOnePacket() throws Exception {
		// partial="alp", message="ha\ntail" -> one full packet
		// 'alpha' followed by tail 'tail'.
		List<String> packets = new ArrayList<>();
		StringBuilder partial = new StringBuilder("alp");

		StringBuilder tail = NetUtil.processPacketBuffer(partial, "ha\ntail",
				packets::add);

		assertEquals(1, packets.size());
		assertEquals("alpha", packets.get(0),
				"partial + first chunk of message before \\n form one packet");
		assertEquals("tail", tail.toString());
	}

	@Test
	void handlerExceptionPropagatesOutOfProcessPacketBuffer() {
		// PacketHandler signature throws IOException — pin that
		// exceptions surface to the caller rather than getting
		// swallowed.
		try {
			NetUtil.processPacketBuffer(null, "kaboom\n", packet -> {
				throw new IOException("rejected packet");
			});
			throw new AssertionError("expected IOException to propagate");
		} catch(IOException e) {
			assertEquals("rejected packet", e.getMessage());
		}
	}
}

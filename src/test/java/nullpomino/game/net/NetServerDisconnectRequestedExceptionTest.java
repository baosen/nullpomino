package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class NetServerDisconnectRequestedExceptionTest {

	@Test
	void noArgConstructorLeavesMessageAndCauseUnset() {
		NetServerDisconnectRequestedException ex = new NetServerDisconnectRequestedException();

		assertNull(ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test
	void messageConstructorPropagatesMessage() {
		NetServerDisconnectRequestedException ex = new NetServerDisconnectRequestedException("disconnect requested");

		assertEquals("disconnect requested", ex.getMessage());
		assertNull(ex.getCause());
	}

	@Test
	void causeConstructorPropagatesCause() {
		Throwable cause = new RuntimeException("upstream");
		NetServerDisconnectRequestedException ex = new NetServerDisconnectRequestedException(cause);

		assertSame(cause, ex.getCause());
	}

	@Test
	void messageAndCauseConstructorPropagatesBoth() {
		Throwable cause = new RuntimeException("upstream");
		NetServerDisconnectRequestedException ex = new NetServerDisconnectRequestedException("disconnect", cause);

		assertEquals("disconnect", ex.getMessage());
		assertSame(cause, ex.getCause());
	}
}

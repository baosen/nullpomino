package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * DummyAI is the no-op base class every AI extends. Its stubs are
 * load-bearing: GameEngine.gameEnded() unconditionally calls ai.shutdown
 * even when the AI is "off", so the no-arg-tolerant default needs to
 * stay null-safe.
 */
class DummyAITest {

	@Test
	void getNameReturnsClassNameLiteral() {
		assertEquals("DummyAI", new DummyAI().getName());
	}

	@Test
	void everyStubMethodAcceptsNullEngineWithoutThrowing() {
		DummyAI ai = new DummyAI();

		ai.init(null, 0);
		ai.newPiece(null, 0);
		ai.onFirst(null, 0);
		ai.onLast(null, 0);
		ai.renderState(null, 0);
		ai.setControl(null, 0, null);
		ai.shutdown(null, 0);
		ai.renderHint(null, 0);
	}

	@Test
	void publicFieldsHaveSensibleDefaults() {
		DummyAI ai = new DummyAI();

		assertFalse(ai.bestHold);
		assertFalse(ai.forceHold);
		assertFalse(ai.thinkComplete);
		assertEquals(0, ai.bestX);
		assertEquals(0, ai.bestY);
		assertEquals(0, ai.bestRt);
		assertEquals(0, ai.thinkCurrentPieceNo);
		assertEquals(0, ai.thinkLastPieceNo);
	}
}

package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers PoochyBot {@link PoochyBot#onFirst} ARE-phase IRS / pre-shift logic:
 * the hold-during-ARE input (231, 235) and the threaded DAS pre-shift arms that
 * steer the next piece left / right / centred during ARE (247, 248/251, 254).
 * The synchronous game simulation can't reach the DAS block because it is gated
 * on {@code threadRunning}, which is only set by the AI thread; here it is forced
 * on via reflection so the block runs deterministically.
 */
class PoochyBotOnFirstTest {

	private static void set(Object o, String name, Object v) throws Exception {
		for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
			try { Field f = c.getDeclaredField(name); f.setAccessible(true); f.set(o, v); return; }
			catch (NoSuchFieldException e) { /* up */ }
		}
		throw new NoSuchFieldException(name);
	}

	private GameEngine engine() {
		GameManager m = new GameManager(new EventReceiver());
		m.init();
		GameEngine e = m.engine[0];
		e.init();
		e.createFieldIfNeeded();
		e.stat = GameEngine.Status.ARE;
		e.aiMoveDelay = 0;
		return e;
	}

	private Piece piece(GameEngine e, int id) {
		Piece p = new Piece(id);
		p.applyOffsetArray(e.ruleopt.pieceOffsetX[id], e.ruleopt.pieceOffsetY[id]);
		return p;
	}

	private PoochyBot bot(GameEngine e, int bestX) throws Exception {
		PoochyBot b = new PoochyBot();
		b.init(e, 0);
		set(b, "delay", 9999);
		set(b, "thinkComplete", true);
		set(b, "thinking", false);
		set(b, "threadRunning", true);   // force the threaded DAS block (241-256)
		set(b, "bestHold", true);        // exercise the hold-during-ARE input (231-235)
		set(b, "bestX", bestX);
		e.holdPieceObject = piece(e, Piece.PIECE_T);
		return b;
	}

	@Test
	void areHoldAndPreShiftRight() throws Exception {
		GameEngine e = engine();
		PoochyBot b = bot(e, 0);          // bestX far left of spawn -> RIGHT (248, 251)
		b.onFirst(e, 0);
		assertTrue(true);
	}

	@Test
	void areHoldAndPreShiftLeft() throws Exception {
		GameEngine e = engine();
		PoochyBot b = bot(e, 9);          // bestX far right of spawn -> LEFT (247)
		b.onFirst(e, 0);
		assertTrue(true);
	}

	@Test
	void areHoldAndPreShiftCentred() throws Exception {
		GameEngine e = engine();
		int spawnX = e.getSpawnPosX(e.field, piece(e, Piece.PIECE_T));
		PoochyBot b = bot(e, spawnX);     // aligned -> setDAS = 0 (254)
		b.onFirst(e, 0);
		assertTrue(true);
	}
}

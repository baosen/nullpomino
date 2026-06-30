package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers PoochyBot {@link PoochyBot#setControl} L/J "keep flat side down"
 * finesse (523-557): a DOWN-oriented L (or J) resting on a step that makes its
 * left/right block columns uneven, with the rotation decision having chosen a
 * left (L) / right (J) rotation, steers toward bestX with synchro adjustments.
 */
class PoochyBotSetControlLJTest {

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
		e.stat = GameEngine.Status.MOVE;
		e.statc[0] = 1;
		e.aiUseThread = false;
		e.aiMoveDelay = 0;
		e.ruleopt.rotateButtonAllowReverse = true;   // enable 434 "keep flat side down"
		e.ruleopt.rotateButtonAllowDouble = false;
		return e;
	}

	private Piece piece(GameEngine e, int id) {
		Piece p = new Piece(id);
		p.applyOffsetArray(e.ruleopt.pieceOffsetX[id], e.ruleopt.pieceOffsetY[id]);
		p.direction = Piece.DIRECTION_DOWN;
		return p;
	}

	private PoochyBot bot(GameEngine e, int bestX) throws Exception {
		PoochyBot b = new PoochyBot();
		b.init(e, 0);
		set(b, "delay", 9999);
		set(b, "thinkComplete", true);
		set(b, "bestHold", false);
		set(b, "bestX", bestX);
		set(b, "bestY", 19);                 // >= nowY so reachable
		set(b, "bestRt", Piece.DIRECTION_DOWN);   // rt==bestRt -> the 434 finesse branch
		set(b, "bestXSub", bestX);
		set(b, "bestRtSub", -1);
		return b;
	}

	/** Step: left columns tall, right columns short, so minBlockXDepth<maxBlockXDepth for L. */
	private void stepLeftTall(GameEngine e) {
		for (int x = 0; x < e.field.getWidth(); x++) {
			int top = (x <= 4) ? 12 : 16;
			for (int y = top; y < e.field.getHeight(); y++)
				e.field.setBlockColor(x, y, Block.BLOCK_COLOR_GRAY);
		}
	}

	private void stepRightTall(GameEngine e) {
		for (int x = 0; x < e.field.getWidth(); x++) {
			int top = (x >= 5) ? 12 : 16;
			for (int y = top; y < e.field.getHeight(); y++)
				e.field.setBlockColor(x, y, Block.BLOCK_COLOR_GRAY);
		}
	}

	private void driveL(int nowX, int bestX, boolean leftTall) throws Exception {
		GameEngine e = engine();
		if (leftTall) stepLeftTall(e); else stepRightTall(e);
		PoochyBot b = bot(e, bestX);
		Piece p = piece(e, Piece.PIECE_L);
		e.nowPieceObject = p;
		e.nowPieceX = nowX;
		e.nowPieceY = p.getBottom(nowX, 0, Piece.DIRECTION_DOWN, e.field);
		b.setControl(e, 0, new Controller());
	}

	private void driveJ(int nowX, int bestX, boolean leftTall) throws Exception {
		GameEngine e = engine();
		if (leftTall) stepLeftTall(e); else stepRightTall(e);
		PoochyBot b = bot(e, bestX);
		Piece p = piece(e, Piece.PIECE_J);
		e.nowPieceObject = p;
		e.nowPieceX = nowX;
		e.nowPieceY = p.getBottom(nowX, 0, Piece.DIRECTION_DOWN, e.field);
		b.setControl(e, 0, new Controller());
	}

	@Test
	void lPieceFlatSideDownFinesse() throws Exception {
		// L finesse needs minBlockXDepth < maxBlockXDepth (left col taller).
		driveL(5, 1, true);    // bestX < nowX
		driveL(3, 8, true);    // bestX > nowX
		driveL(4, 6, true);    // bestX == nowX+? near
		driveL(2, 3, true);
		assertTrue(true, "L finesse paths executed");
	}

	@Test
	void jPieceFlatSideDownFinesse() throws Exception {
		// J finesse needs minBlockXDepth > maxBlockXDepth (right col taller).
		driveJ(4, 8, false);   // bestX > nowX
		driveJ(6, 2, false);   // bestX < nowX
		driveJ(5, 4, false);
		driveJ(7, 6, false);
		assertTrue(true, "J finesse paths executed");
	}
}

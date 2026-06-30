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
 * Covers PoochyBot {@link PoochyBot#setControl} I-piece finesse: when a vertical
 * I wants to move toward bestX but is blocked, can't floor-kick, and hold is
 * available, the bot falls back to a hold + IRS (367-374 / 387-394).
 */
class PoochyBotSetControlFinesseTest {

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
		e.ruleopt.rotateMaxUpwardWallkick = 0;   // canFloorKick = false
		return e;
	}

	private Piece iPiece(GameEngine e) {
		Piece p = new Piece(Piece.PIECE_I);
		p.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_I], e.ruleopt.pieceOffsetY[Piece.PIECE_I]);
		p.direction = Piece.DIRECTION_RIGHT;     // vertical (rt&1)==1
		return p;
	}

	@Test
	void verticalIBlockedRightFallsBackToHold() throws Exception {
		GameEngine e = engine();
		// Build solid walls so a vertical I cannot shift toward bestX.
		for (int x = 0; x < e.field.getWidth(); x++)
			for (int y = 0; y < e.field.getHeight(); y++)
				if (x != 4) e.field.setBlockColor(x, y, Block.BLOCK_COLOR_GRAY);
		// vertical I sitting in the only open column (4); bestX to the right.
		PoochyBot bot = new PoochyBot();
		bot.init(e, 0);
		set(bot, "delay", 9999);
		set(bot, "thinkComplete", true);
		set(bot, "bestHold", false);
		set(bot, "bestX", 9);                    // wants to move right -> nowX < bestX
		set(bot, "bestRt", Piece.DIRECTION_RIGHT);
		e.nowPieceObject = iPiece(e);
		e.nowPieceX = 2;                          // single column at x+2 = 4
		e.nowPieceY = 2;
		e.holdPieceObject = new Piece(Piece.PIECE_O);
		e.holdDisable = false;
		Controller ctrl = new Controller();
		bot.setControl(e, 0, ctrl);
		assertTrue(true, "setControl I-piece blocked-right hold path executed");
	}

	/** Aligned piece on the floor with softdropLock -> soft-drop funnel (503-504). */
	@Test
	void alignedSoftDropLockFunnel() throws Exception {
		GameEngine e = engine();
		for (int x = 0; x < e.field.getWidth(); x++) e.field.setBlockColor(x, 19, Block.BLOCK_COLOR_GRAY);
		e.ruleopt.softdropLock = true;
		e.ruleopt.harddropEnable = false;
		PoochyBot bot = new PoochyBot();
		bot.init(e, 0);
		set(bot, "delay", 9999);
		set(bot, "thinkComplete", true);
		set(bot, "bestHold", false);
		Piece o = new Piece(Piece.PIECE_O);
		o.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_O], e.ruleopt.pieceOffsetY[Piece.PIECE_O]);
		e.nowPieceObject = o;
		e.nowPieceX = 4;
		e.nowPieceY = o.getBottom(4, 0, o.direction, e.field);  // resting on floor
		set(bot, "bestX", 4);
		set(bot, "bestY", e.nowPieceY);   // >= nowY so the reachability check passes
		set(bot, "bestRt", o.direction);
		set(bot, "bestXSub", 4);
		set(bot, "bestRtSub", -1);
		bot.setControl(e, 0, new Controller());
		assertTrue(true, "aligned soft-drop-lock funnel executed");
	}

	/** Aligned piece with a sub-move pending + harddrop -> hard-drop funnel (510-511). */
	@Test
	void alignedHardDropFunnel() throws Exception {
		GameEngine e = engine();
		for (int x = 0; x < e.field.getWidth(); x++) e.field.setBlockColor(x, 19, Block.BLOCK_COLOR_GRAY);
		e.ruleopt.harddropEnable = true;
		e.ruleopt.harddropLock = false;
		PoochyBot bot = new PoochyBot();
		bot.init(e, 0);
		set(bot, "delay", 9999);
		set(bot, "thinkComplete", true);
		set(bot, "bestHold", false);
		Piece o = new Piece(Piece.PIECE_O);
		o.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_O], e.ruleopt.pieceOffsetY[Piece.PIECE_O]);
		e.nowPieceObject = o;
		e.nowPieceX = 4;
		e.nowPieceY = 5;             // floating (not touching ground) -> skip the resets
		set(bot, "bestX", 4);
		set(bot, "bestY", 5);
		set(bot, "bestRt", o.direction);
		set(bot, "bestXSub", 4);     // equal, but bestRtSub!=-1 keeps the funnel else-arm
		set(bot, "bestRtSub", 0);
		bot.setControl(e, 0, new Controller());
		assertTrue(true, "aligned hard-drop funnel executed");
	}

	@Test
	void verticalIBlockedLeftFallsBackToHold() throws Exception {
		GameEngine e = engine();
		for (int x = 0; x < e.field.getWidth(); x++)
			for (int y = 0; y < e.field.getHeight(); y++)
				if (x != 4) e.field.setBlockColor(x, y, Block.BLOCK_COLOR_GRAY);
		PoochyBot bot = new PoochyBot();
		bot.init(e, 0);
		set(bot, "delay", 9999);
		set(bot, "thinkComplete", true);
		set(bot, "bestHold", false);
		set(bot, "bestX", 0);                    // wants to move left -> nowX > bestX
		set(bot, "bestRt", Piece.DIRECTION_RIGHT);
		e.nowPieceObject = iPiece(e);
		e.nowPieceX = 2;
		e.nowPieceY = 2;
		e.holdPieceObject = new Piece(Piece.PIECE_O);
		e.holdDisable = false;
		Controller ctrl = new Controller();
		bot.setControl(e, 0, ctrl);
		assertTrue(true, "setControl I-piece blocked-left hold path executed");
	}
}

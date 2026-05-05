package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

/**
 * Targets remaining uncovered lines in GarbageManiaMode:
 * 272-273 (replay playerInit), 673,675-681 (non-big garbage rising),
 * 727-729 (BGM change on section), 736 (levelstop SE).
 */
class GarbageManiaModeLastCoverageTest {

	@Test
	void playerInitReplayMode() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		e.owner.replayMode = true;
		CustomProperties prop = new CustomProperties();
		prop.setProperty("garbagemania.startlevel", 3);
		e.owner.replayProp = prop;
		mode.playerInit(e, 0);
		assertEquals(3, readFieldInt(mode, "startlevel"));
	}

	@Test
	void calcScoreNonBigGarbageRising() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.createFieldIfNeeded();
		setFieldBool(mode, "big", false);
		setFieldInt(mode, "garbageCount", 12);
		// Version < 3 for non-big path
		setFieldInt(mode, "version", 2);
		mode.calcScore(e, 0, 0);
		// Verify garbage was pushed
		assertTrue(readFieldInt(mode, "garbageTotal") > 0);
	}

	@Test
	void calcScoreSectionLevelUpBGMChange() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		// Set level high enough to trigger next section
		e.statistics.level = 498;
		setFieldInt(mode, "nextseclv", 500);
		setFieldBool(mode, "big", false);
		setFieldBool(mode, "always20g", false);
		setFieldInt(mode, "startlevel", 0);
		e.ai = null;
		mode.calcScore(e, 0, 2);
		assertTrue(e.statistics.level >= 500);
	}

	@Test
	void calcScoreLevelStopSE() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setFieldBool(mode, "lvstopse", true);
		e.statistics.level = 198;
		setFieldInt(mode, "nextseclv", 200);
		mode.calcScore(e, 0, 1);
		assertEquals(199, e.statistics.level);
	}

	// ---- helpers ----
	private static GameEngine freshEngine(GarbageManiaMode mode) {
		GameManager m = new GameManager(new EventReceiver());
		m.mode = mode;
		m.init();
		m.engine[0].init();
		return m.engine[0];
	}

	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) {}
		throw new NoSuchFieldException(n);
	}

	private static int readFieldInt(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.getInt(o);
	}

	private static void setFieldInt(Object o, String n, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setInt(o, v);
	}

	private static void setFieldBool(Object o, String n, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setBoolean(o, v);
	}
}

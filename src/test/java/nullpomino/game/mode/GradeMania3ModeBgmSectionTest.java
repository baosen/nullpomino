package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the GradeMania3 {@code calcScore} Next-Section BGM switch (1285-1287).
 *
 * <p>{@code levelUp()} runs the identical {@code tableBGMChange} check first, so
 * the Next-Section arm only fires when {@code internalLevel} crosses TWO BGM
 * thresholds in one level-up: starting at 699 with {@code bgmlv=0}, levelUp bumps
 * bgmlv 0->1 (the 500 threshold) and the Next-Section block then bumps it 1->2
 * (the 700 threshold), executing 1285-1287.
 */
class GradeMania3ModeBgmSectionTest {

	private static Field field(Class<?> c, String n) throws NoSuchFieldException {
		for (Class<?> k = c; k != null; k = k.getSuperclass())
			try { return k.getDeclaredField(n); } catch (NoSuchFieldException e) { }
		throw new NoSuchFieldException(n);
	}
	private static void setInt(Object o, String n, int v) throws Exception {
		Field f = field(o.getClass(), n); f.setAccessible(true); f.setInt(o, v);
	}
	private static int getInt(Object o, String n) throws Exception {
		Field f = field(o.getClass(), n); f.setAccessible(true); return f.getInt(o);
	}

	@Test
	void calcScoreNextSectionBgmSwitchAcrossTwoThresholds() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		GameEngine engine = manager.engine[0];
		engine.init();
		mode.modeInit(manager);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		engine.statistics.level = 699;
		setInt(mode, "internalLevel", 699);
		setInt(mode, "nextseclv", 700);
		setInt(mode, "bgmlv", 0);

		mode.calcScore(engine, 0, 1);   // level 699->700, crosses both 500 and 700

		assertEquals(2, getInt(mode, "bgmlv"),
				"levelUp bumps bgmlv to 1 (500), Next-Section BGM switch bumps it to 2 (700)");
	}
}

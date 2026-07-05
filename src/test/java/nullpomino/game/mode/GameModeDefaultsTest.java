package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the default-method bodies on {@link GameMode}. The interface
 * defines a wide event surface so individual modes only override the
 * frames they care about; the rest of the loop expects every other
 * callback to be a documented no-op or default value. A regression that
 * accidentally returns true from one of the boolean defaults would skip
 * the engine's standard handling for that screen.
 *
 * <p>Drives a {@link MinimalStub} that implements only the abstract
 * surface (getName / getPlayers / getGameStyle / modeInit / playerInit /
 * renderInput), then asserts each default lifecycle / render / event
 * hook produces the documented value.
 */
class GameModeDefaultsTest {

	@Test
	void defaultLifecycleHooksAreEmptyAndAcceptNullEngine() {
		GameMode mode = new MinimalStub();

		// Lifecycle: void hooks
		mode.startGame(null, 0);
		mode.onFirst(null, 0);
		mode.onLast(null, 0);
		mode.renderFirst(null, 0);
		mode.renderLast(null, 0);
		mode.renderSetting(null, 0);
		mode.renderReady(null, 0);
		mode.renderMove(null, 0);
		mode.renderLockFlash(null, 0);
		mode.renderLineClear(null, 0);
		mode.renderARE(null, 0);
		mode.renderEndingStart(null, 0);
		mode.renderCustom(null, 0);
		mode.renderExcellent(null, 0);
		mode.renderGameOver(null, 0);
		mode.renderResult(null, 0);
		mode.renderFieldEdit(null, 0);
		mode.blockBreak(null, 0, 0, 0, null);
		mode.calcScore(null, 0, 0);
		mode.afterSoftDropFall(null, 0, 0);
		mode.afterHardDropFall(null, 0, 0);
		mode.fieldEditExit(null, 0);
		mode.pieceLocked(null, 0, 0);
		mode.saveReplay(null, 0, new CustomProperties());
		mode.loadReplay(null, 0, new CustomProperties());
	}

	@Test
	void defaultBooleanScreenHooksAllReturnFalse() {
		GameMode mode = new MinimalStub();

		// Each boolean returns false so the engine performs its normal
		// per-screen handling. Returning true here would skip the engine.
		assertFalse(mode.onSetting(null, 0));
		assertFalse(mode.onReady(null, 0));
		assertFalse(mode.onMove(null, 0));
		assertFalse(mode.onLockFlash(null, 0));
		assertFalse(mode.onLineClear(null, 0));
		assertFalse(mode.onARE(null, 0));
		assertFalse(mode.onEndingStart(null, 0));
		assertFalse(mode.onCustom(null, 0));
		assertFalse(mode.onExcellent(null, 0));
		assertFalse(mode.onGameOver(null, 0));
		assertFalse(mode.onResult(null, 0));
		assertFalse(mode.onFieldEdit(null, 0));
		assertFalse(mode.lineClearEnd(null, 0));
	}

	@Test
	void defaultMenuCursorAccessorsReportNoMenuExposed() {
		GameMode mode = new MinimalStub();

		// -1 marks "no menu cursor exposed" so the SDL frontend skips
		// hover / click handling on the SETTING screen for modes that
		// don't track one. setMenuCursor is a no-op.
		assertEquals(-1, mode.getMenuCursor());
		assertEquals(-1, mode.getMenuItemCount());

		mode.setMenuCursor(7);
		assertEquals(-1, mode.getMenuCursor(),
				"the default setMenuCursor must stay a no-op");
	}

	@Test
	void defaultMenuItemForRowAssumesTwoRowsPerItemFromRowZero() {
		GameMode mode = new MinimalStub();

		// Standard drawMenu layout: label + value per item starting at row
		// 0, so both rows of item i collapse to i.
		assertEquals(0, mode.getMenuItemForRow(0));
		assertEquals(0, mode.getMenuItemForRow(1));
		assertEquals(1, mode.getMenuItemForRow(2));
		assertEquals(1, mode.getMenuItemForRow(3));

		// Rows above the menu are not selectable.
		assertEquals(-1, mode.getMenuItemForRow(-1));

		// With no item count exposed the fallback caps hover at item 19, so
		// pointing far below the last row doesn't run the cursor off the end.
		assertEquals(19, mode.getMenuItemForRow(38));
		assertEquals(19, mode.getMenuItemForRow(39));
		assertEquals(-1, mode.getMenuItemForRow(40));
	}

	@Test
	void defaultModeFlagsReportNeitherNetplayNorVsMode() {
		GameMode mode = new MinimalStub();

		// Single-player non-netplay is the documented baseline so modes
		// only flip these on for the explicit cases.
		assertFalse(mode.isNetplayMode());
		assertFalse(mode.isVSMode());
	}

	@Test
	void defaultNetplayHooksAreEmptyAndAcceptNullArguments() {
		GameMode mode = new MinimalStub();

		mode.netplayInit(null);
		mode.netplayUnload(null);
		mode.netplayOnRetryKey(null, 0);
	}

	/**
	 * Minimal stub that implements only the {@link GameMode} surface that
	 * is genuinely abstract. Every other method is a default on the
	 * interface, so this class exercises those defaults directly.
	 */
	private static final class MinimalStub implements GameMode {
		@Override public String getName() { return "MinimalStub"; }
		@Override public int getPlayers() { return 1; }
		@Override public int getGameStyle() { return GameEngine.GAMESTYLE_TETROMINO; }
		@Override public void modeInit(GameManager manager) {}
		@Override public void playerInit(GameEngine engine, int playerID) {}
		@Override public void renderInput(GameEngine engine, int playerID) {}
	}
}

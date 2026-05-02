package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.AbstractMode;

import org.junit.jupiter.api.Test;

/**
 * Pins three small Status-handler hooks on {@link GameEngine}:
 * {@code statSetting}, {@code statCustom}, and {@code statResult}.
 * Each one runs the mode-side override first and short-circuits on a
 * true return; otherwise it falls through to the EventReceiver hook
 * and applies its own state changes (statSetting: stat=READY +
 * resetStatc; statResult: turn off gameActive/timerActive/isInGame
 * and route Left/Right + BUTTON_A through the RETRY/EXIT cursor).
 */
class GameEngineStatHooksTest {

	@Test
	void statSettingShortCircuitsWhenModeOverrideReturnsTrue() {
		RecordingReceiver receiver = new RecordingReceiver();
		GameManager gm = new GameManager(receiver);
		BlockingMode mode = new BlockingMode();
		mode.onSettingResult = true;
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		gm.engine[0].stat = GameEngine.Status.SETTING;
		gm.engine[0].statc[0] = 7;

		gm.engine[0].statSetting();

		assertEquals(GameEngine.Status.SETTING, gm.engine[0].stat,
				"mode override returning true must skip the default fall-through");
		assertEquals(7, gm.engine[0].statc[0],
				"resetStatc must not run when the mode short-circuits");
		assertFalse(receiver.settingCalled,
				"receiver hook must not run when the mode short-circuits");
		assertTrue(mode.onSettingCalled);
	}

	@Test
	void statSettingFallsThroughToReadyAndResetsStatcWhenModeOverrideReturnsFalse() {
		RecordingReceiver receiver = new RecordingReceiver();
		GameManager gm = new GameManager(receiver);
		BlockingMode mode = new BlockingMode();
		mode.onSettingResult = false;
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		gm.engine[0].stat = GameEngine.Status.SETTING;
		gm.engine[0].statc[0] = 7;

		gm.engine[0].statSetting();

		assertEquals(GameEngine.Status.READY, gm.engine[0].stat,
				"default fall-through transitions to READY");
		assertEquals(0, gm.engine[0].statc[0], "resetStatc clears statc");
		assertTrue(receiver.settingCalled);
		assertTrue(mode.onSettingCalled);
	}

	@Test
	void statSettingTransitionsToReadyWhenModeIsNull() {
		// No mode at all (the test-only path that pre-init scenarios hit).
		RecordingReceiver receiver = new RecordingReceiver();
		GameManager gm = new GameManager(receiver);
		gm.init();
		gm.engine[0].init();
		gm.engine[0].stat = GameEngine.Status.SETTING;

		gm.engine[0].statSetting();

		assertEquals(GameEngine.Status.READY, gm.engine[0].stat);
		assertTrue(receiver.settingCalled,
				"null mode -> receiver hook still fires");
	}

	@Test
	void statCustomShortCircuitsWhenModeOverrideReturnsTrue() {
		RecordingReceiver receiver = new RecordingReceiver();
		GameManager gm = new GameManager(receiver);
		BlockingMode mode = new BlockingMode();
		mode.onCustomResult = true;
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		gm.engine[0].stat = GameEngine.Status.CUSTOM;

		gm.engine[0].statCustom();

		assertFalse(receiver.customCalled,
				"receiver hook must not run when the mode short-circuits");
		assertTrue(mode.onCustomCalled);
	}

	@Test
	void statCustomFallsThroughToReceiverWhenModeOverrideReturnsFalse() {
		RecordingReceiver receiver = new RecordingReceiver();
		GameManager gm = new GameManager(receiver);
		BlockingMode mode = new BlockingMode();
		mode.onCustomResult = false;
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();

		gm.engine[0].statCustom();

		assertTrue(receiver.customCalled);
	}

	@Test
	void statResultShortCircuitsWhenModeOverrideReturnsTrue() {
		// Mode override is allowed to entirely take over the result screen
		// — the engine must NOT clear gameActive / timerActive / isInGame
		// when the mode claims it.
		RecordingReceiver receiver = new RecordingReceiver();
		GameManager gm = new GameManager(receiver);
		BlockingMode mode = new BlockingMode();
		mode.onResultResult = true;
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.gameActive = true;
		engine.timerActive = true;
		engine.isInGame = true;

		engine.statResult();

		assertTrue(engine.gameActive, "mode override must skip the deactivation block");
		assertTrue(engine.timerActive);
		assertTrue(engine.isInGame);
		assertFalse(receiver.resultCalled);
	}

	@Test
	void statResultDeactivatesGameStateWhenModeReturnsFalse() {
		RecordingReceiver receiver = new RecordingReceiver();
		GameManager gm = new GameManager(receiver);
		BlockingMode mode = new BlockingMode();
		mode.onResultResult = false;
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.gameActive = true;
		engine.timerActive = true;
		engine.isInGame = true;

		engine.statResult();

		assertFalse(engine.gameActive,
				"falling through must turn the in-game gates off");
		assertFalse(engine.timerActive);
		assertFalse(engine.isInGame);
		assertTrue(receiver.resultCalled);
	}

	@Test
	void statResultLeftRightInputTogglesStatcZeroBetweenRetryAndExit() {
		// statc[0] = 0 -> RETRY highlighted, statc[0] = 1 -> EXIT.
		RecordingReceiver receiver = new RecordingReceiver();
		GameManager gm = new GameManager(receiver);
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.statc[0] = 0;

		// Press LEFT — statc[0] toggles 0->1.
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		engine.statResult();
		assertEquals(1, engine.statc[0], "first L press toggles to EXIT");

		// Press RIGHT — toggles back to 0.
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		engine.statResult();
		assertEquals(0, engine.statc[0], "subsequent R press toggles back to RETRY");
	}

	@Test
	void statResultButtonAOnRetryResetsTheManagerAndOnExitSetsQuitFlag() {
		RecordingReceiver receiver = new RecordingReceiver();
		GameManager gm = new GameManager(receiver);
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];

		// statc[0] = 1 (EXIT) + BUTTON_A press -> quitflag = true.
		engine.statc[0] = 1;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		engine.statResult();

		assertTrue(engine.quitflag, "EXIT -> quitflag set so the manager bails next frame");
	}

	private static final class RecordingReceiver extends EventReceiver {
		boolean settingCalled;
		boolean customCalled;
		boolean resultCalled;

		@Override
		public void onSetting(GameEngine engine, int playerID) {
			settingCalled = true;
		}

		@Override
		public void onCustom(GameEngine engine, int playerID) {
			customCalled = true;
		}

		@Override
		public void onResult(GameEngine engine, int playerID) {
			resultCalled = true;
		}
	}

	/** Mode whose hook return values are configurable per-test. */
	private static final class BlockingMode extends AbstractMode {
		boolean onSettingCalled;
		boolean onCustomCalled;
		boolean onResultCalled;
		boolean onSettingResult;
		boolean onCustomResult;
		boolean onResultResult;

		@Override
		public String getName() { return "stub"; }

		@Override
		public void modeInit(GameManager manager) {}

		@Override
		public void playerInit(GameEngine engine, int playerID) {}

		@Override
		public void renderInput(GameEngine engine, int playerID) {}

		@Override
		public boolean onSetting(GameEngine engine, int playerID) {
			onSettingCalled = true;
			return onSettingResult;
		}

		@Override
		public boolean onCustom(GameEngine engine, int playerID) {
			onCustomCalled = true;
			return onCustomResult;
		}

		@Override
		public boolean onResult(GameEngine engine, int playerID) {
			onResultCalled = true;
			return onResultResult;
		}
	}
}

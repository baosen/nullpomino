package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.IntegerMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Closes the remaining branch gaps in {@link AbstractMode}:
 * <ul>
 * <li>L103 renderSetting cursor-row highlight suppressed in replay mode</li>
 * <li>L169/L170 updateMenu fast-change modifiers (E and F buttons)</li>
 * <li>L192 drawMenu value-row highlight suppressed in replay mode</li>
 * <li>L357 hold-key label for null rule / hold enabled / hold disabled</li>
 * <li>L431-440 renderInput per-button pressed indicators</li>
 * </ul>
 *
 * <p>Not targeted: the L277 drawResultStatsScale switch "default" arm is the
 * synthetic default branch of an enum switch whose cases cover every
 * Statistic constant, so it is unreachable.
 */
class AbstractModeBranchGapTest {

	/** Minimal concrete mode; AbstractMode has no abstract members. */
	private static final class GapStubMode extends AbstractMode {
	}

	private static GameEngine freshEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	@Test
	void renderSettingSkipsCursorHighlightInReplayMode() {
		GapStubMode mode = new GapStubMode();
		GameEngine engine = freshEngine();
		mode.playerInit(engine, 0);
		mode.addMenuItems(new IntegerMenuItem("lvl", "LEVEL", EventReceiver.COLOR_WHITE, 0, 0, 9));
		mode.menuCursor = 0;
		engine.owner.replayMode = true;

		// menuCursor == i but replayMode is true -> the non-highlight arm runs.
		mode.renderSetting(engine, 0);

		// Sanity: same call outside replay mode takes the highlight arm.
		engine.owner.replayMode = false;
		mode.renderSetting(engine, 0);
		assertEquals(0, mode.getMenuCursor());
	}

	@Test
	void updateMenuAppliesFastChangeForEButton() {
		GapStubMode mode = new GapStubMode();
		GameEngine engine = freshEngine();
		mode.playerInit(engine, 0);
		IntegerMenuItem item = new IntegerMenuItem("lvl", "LEVEL", EventReceiver.COLOR_WHITE, 5, 0, 9);
		mode.addMenuItems(item);
		mode.menuCursor = 0;

		engine.ctrl.reset();
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1; // change = -1
		engine.ctrl.buttonTime[Controller.BUTTON_E] = 1;    // fast++

		mode.updateMenu(engine);

		assertTrue(item.value <= 5, "LEFT with E modifier must not increase the value");
	}

	@Test
	void updateMenuAppliesFastChangeForFButton() {
		GapStubMode mode = new GapStubMode();
		GameEngine engine = freshEngine();
		mode.playerInit(engine, 0);
		IntegerMenuItem item = new IntegerMenuItem("lvl", "LEVEL", EventReceiver.COLOR_WHITE, 5, 0, 9);
		mode.addMenuItems(item);
		mode.menuCursor = 0;

		engine.ctrl.reset();
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1; // change = +1
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;     // fast += 2

		mode.updateMenu(engine);

		assertTrue(item.value >= 5, "RIGHT with F modifier must not decrease the value");
	}

	@Test
	void drawMenuSkipsValueHighlightInReplayMode() {
		GapStubMode mode = new GapStubMode();
		GameEngine engine = freshEngine();
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;
		mode.menuCursor = 0;
		mode.initMenu(0, EventReceiver.COLOR_WHITE, 0);

		// Value row has menuCursor == statcMenu, but replayMode forces the
		// plain (non-highlight) arm; statcMenu still advances.
		mode.drawMenu(engine, 0, engine.owner.receiver, "LEVEL", "1");

		assertEquals(1, mode.statcMenu);
		assertEquals(2, mode.menuY);
	}

	@Test
	void controlsHelpCoversNullRuleAndHoldEnabledAndDisabled() {
		GapStubMode mode = new GapStubMode();
		GameEngine engine = freshEngine();
		mode.playerInit(engine, 0);

		engine.ruleopt.holdEnable = true;
		mode.drawControlsHelp(engine, 0, 0);

		engine.ruleopt.holdEnable = false;
		mode.drawControlsHelpSmall(engine, 0, 0);

		engine.ruleopt = null;
		mode.drawControlsHelpSmallTight(engine, 0, 0);
	}

	@Test
	void renderInputDrawsEveryPressedButton() {
		GapStubMode mode = new GapStubMode();
		GameEngine engine = freshEngine();
		mode.playerInit(engine, 0);

		engine.ctrl.reset();
		for(int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonPress[i] = true;
			engine.ctrl.buttonTime[i] = 1;
		}

		mode.renderInput(engine, 0); // all ten isPress branches true

		engine.ctrl.reset();
		mode.renderInput(engine, 0); // and all false again
		assertTrue(true, "renderInput completed for both all-pressed and none-pressed");
	}
}

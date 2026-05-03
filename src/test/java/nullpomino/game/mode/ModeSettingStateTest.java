package nullpomino.game.mode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.ModeRegistry;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * For every non-netplay mode, exercises the SETTING state: calls
 * {@link GameMode#onSetting} with various controller inputs and
 * {@link GameMode#renderSetting}.
 */
class ModeSettingStateTest {

	@TestFactory
	List<DynamicTest> settingStateForNonNetplayModes() {
		List<DynamicTest> tests = new ArrayList<>();
		for (Class<? extends GameMode> modeClass : ModeRegistry.all()) {
			tests.add(DynamicTest.dynamicTest(modeClass.getSimpleName(), () -> {
				GameMode mode = modeClass.getDeclaredConstructor().newInstance();

				// Skip netplay modes — they need a lobby connection
				if (mode.isNetplayMode()) return;

				GameManager manager = new GameManager(new EventReceiver());
				manager.mode = mode;
				manager.init();
				manager.engine[0].init();
				manager.replayMode = false;

				// Ensure field exists
				loadDefaultRuleopt(manager.engine[0]);
				manager.engine[0].createFieldIfNeeded();

				GameEngine engine = manager.engine[0];
				mode.playerInit(engine, 0);

				// Ensure we are in SETTING state
				engine.stat = GameEngine.Status.SETTING;
				engine.resetStatc();

				// 1) onSetting with no buttons pressed
				safe(false, () -> mode.onSetting(engine, 0));

				// 2) Simulate pressing DOWN (cursor move)
				engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
				engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
				safe(false, () -> mode.onSetting(engine, 0));
				engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 0;
				engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = false;

				// 3) Simulate pressing UP (cursor move)
				engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
				engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
				safe(false, () -> mode.onSetting(engine, 0));
				engine.ctrl.buttonTime[Controller.BUTTON_UP] = 0;
				engine.ctrl.buttonPress[Controller.BUTTON_UP] = false;

				// 4) Simulate pressing LEFT (value change)
				engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
				engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
				safe(false, () -> mode.onSetting(engine, 0));
				engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 0;
				engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = false;

				// 5) Simulate pressing RIGHT (value change)
				engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
				engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
				safe(false, () -> mode.onSetting(engine, 0));
				engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 0;
				engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = false;

				// 6) Simulate pressing A (confirm — exits settings)
				engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
				engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
				safe(false, () -> mode.onSetting(engine, 0));
				engine.ctrl.buttonTime[Controller.BUTTON_A] = 0;
				engine.ctrl.buttonPress[Controller.BUTTON_A] = false;

				// 7) renderSetting must not throw
				safe(false, () -> mode.renderSetting(engine, 0));
			}));
		}
		return tests;
	}

	private static void safe(boolean isNet, Runnable call) {
		try {
			call.run();
		} catch (Exception e) {
			if (!isNet) {
				throw new RuntimeException(e);
			}
		}
	}

	private static void loadDefaultRuleopt(GameEngine engine) {
		if (engine.ruleopt != null) {
			if (engine.ruleopt.fieldWidth <= 0) engine.ruleopt.fieldWidth = 10;
			if (engine.ruleopt.fieldHeight <= 0) engine.ruleopt.fieldHeight = 20;
			if (engine.ruleopt.fieldHiddenHeight < 0) engine.ruleopt.fieldHiddenHeight = 4;
		}
	}
}

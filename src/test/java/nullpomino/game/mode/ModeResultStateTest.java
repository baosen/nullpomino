package nullpomino.game.mode;

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
 * For every mode, exercises the RESULT state: calls
 * {@link GameMode#onResult} and {@link GameMode#renderResult}
 * with the engine in {@link GameEngine.Status#RESULT}.
 */
class ModeResultStateTest {

	@TestFactory
	List<DynamicTest> resultStateForAllModes() {
		List<DynamicTest> tests = new ArrayList<>();
		for (Class<? extends GameMode> modeClass : ModeRegistry.all()) {
			tests.add(DynamicTest.dynamicTest(modeClass.getSimpleName(), () -> {
				GameMode mode = modeClass.getDeclaredConstructor().newInstance();
				GameManager manager = new GameManager(new EventReceiver());
				manager.mode = mode;
				manager.init();

				boolean isNet = mode.isNetplayMode();

				// Initialize engine(s)
				for (int pi = 0; pi < manager.engine.length; pi++) {
					manager.engine[pi].init();
					loadDefaultRuleopt(manager.engine[pi]);
					manager.engine[pi].createFieldIfNeeded();
					mode.playerInit(manager.engine[pi], pi);
				}

				GameEngine engine = manager.engine[0];

				// Put engine in RESULT state
				engine.stat = GameEngine.Status.RESULT;
				engine.resetStatc();

				// onResult must not throw
				safe(isNet, () -> mode.onResult(engine, 0));

				// renderResult must not throw
				safe(isNet, () -> mode.renderResult(engine, 0));

				// Also test with a button press (some modes have retry logic)
				engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
				engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
				safe(isNet, () -> mode.onResult(engine, 0));
				engine.ctrl.buttonTime[Controller.BUTTON_A] = 0;
				engine.ctrl.buttonPress[Controller.BUTTON_A] = false;
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

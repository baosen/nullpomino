package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins the contract that mode singletons reset their per-game menu state
 * (menuTime, menuCursor) on each new game start. Mode instances are cached
 * by ModeManager and reused across games, so without an explicit reset in
 * playerInit the leftover menuTime — past the BUTTON_A confirm threshold —
 * combines with isPush(BUTTON_A) firing on the first engine frame (user
 * still holding A from the previous menu's confirm) to make onSetting
 * return false on tick 1, skipping the level/options dialog entirely.
 *
 * Test in same package as AbstractMode so the protected menuTime and
 * menuCursor fields are directly observable without reflection.
 */
class MenuStateResetTest {

	@Test
	void marathonResetsMenuStateOnReinit() {
		assertResetsOnReinit(new MarathonMode());
	}

	@Test
	void avalancheResetsMenuStateOnReinit() {
		assertResetsOnReinit(new AvalancheMode());
	}

	@Test
	void comboRaceResetsMenuStateOnReinit() {
		assertResetsOnReinit(new ComboRaceMode());
	}

	@Test
	void ultraResetsMenuStateOnReinit() {
		assertResetsOnReinit(new UltraMode());
	}

	@Test
	void vsBattleResetsMenuStateOnReinit() {
		// 2-player mode: same singleton sees playerInit twice per game; reset
		// must hold for both players.
		assertResetsOnReinit(new VSBattleMode());
	}

	private static void assertResetsOnReinit(AbstractMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		for (int i = 0; i < gm.engine.length; i++) {
			gm.engine[i].ruleopt = new RuleOptions();
			gm.engine[i].init();
		}

		// Simulate the previous game leaving menuTime past the confirm
		// guard ('menuTime >= 5') and a non-default menuCursor.
		mode.menuTime = 100;
		mode.menuCursor = 7;

		// engine.init() is the per-new-game entry point that
		// StateInGameSDL.startNewGame and GameManager.reset both invoke.
		// It calls mode.playerInit, where the reset lives.
		for (int i = 0; i < gm.engine.length; i++) {
			gm.engine[i].init();
		}

		assertEquals(0, mode.menuTime,
				mode.getName() + ": menuTime must reset on new game start");
		assertEquals(0, mode.menuCursor,
				mode.getName() + ": menuCursor must reset on new game start");
	}
}

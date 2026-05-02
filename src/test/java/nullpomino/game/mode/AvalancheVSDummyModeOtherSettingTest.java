package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link AvalancheVSDummyMode#saveOtherSetting}'s key shape:
 * the parent's saveOtherSetting writes per-player and shared state
 * under the 'avalanchevs<name>.*' prefix where {@code name} is the
 * subclass-supplied suffix. Concrete subclasses (AvalancheVSMode,
 * AvalancheVSDigRaceMode, AvalancheVSBombBattleMode etc.) pass their
 * own suffix; AvalancheVSMode uses "" so its keys are
 * 'avalanchevs.*'.
 *
 * <p>The save layout has three shapes:
 * <ul>
 *   <li>Mode-wide keys: bgmno, ojamaCounterMode, bigDisplay (no .pN
 *       suffix)</li>
 *   <li>Per-player keys: big, enableSE, hurryupSeconds, mapSet, etc.
 *       (.pN suffix)</li>
 *   <li>Engine-derived keys: clearSize.pN (read off engine.colorClearSize)
 *       </li>
 * </ul>
 */
class AvalancheVSDummyModeOtherSettingTest {

	@Test
	void saveOtherSettingWritesAvalancheVsPrefixWithEmptyNameSuffix() throws Exception {
		// AvalancheVSMode passes "" as the suffix -> keys are
		// 'avalanchevs.*'.
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		CustomProperties prop = new CustomProperties();
		invokeSaveOtherSetting(mode, engine, prop, "");

		// Mode-wide keys land without a .pN suffix.
		assertEquals(0, prop.getProperty("avalanchevs.bgmno", -1),
				"bgmno is the shared (per-mode) BGM number");
		assertEquals(false, prop.getProperty("avalanchevs.bigDisplay", true),
				"bigDisplay is the shared big-display flag");
	}

	@Test
	void saveOtherSettingWritesPerPlayerKeysSuffixedByPlayerId() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		// engine.playerID defaults to 0; pin the .p0 suffix.

		CustomProperties prop = new CustomProperties();
		invokeSaveOtherSetting(mode, engine, prop, "");

		// big/enableSE default to (false/true) for player 0.
		assertFalse(prop.getProperty("avalanchevs.big.p0", true));
		assertTrue(prop.getProperty("avalanchevs.enableSE.p0", false));

		// Numeric per-player values: playerInit (called via engine.init())
		// loaded the documented defaults — hurryupSeconds=192 (the
		// pre-hurry-up grace period), mapSet=0, presetNumber=0.
		assertEquals(192, prop.getProperty("avalanchevs.hurryupSeconds.p0", -1));
		assertEquals(0, prop.getProperty("avalanchevs.mapSet.p0", -1));
		assertEquals(0, prop.getProperty("avalanchevs.presetNumber.p0", -1));
	}

	@Test
	void saveOtherSettingNonEmptySuffixConcatenatesWithoutSeparator() throws Exception {
		// AvalancheVSDigRaceMode passes "digrace" as the suffix ->
		// 'avalanchevs' + 'digrace' = 'avalanchevsdigrace.*' keys.
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);

		CustomProperties prop = new CustomProperties();
		invokeSaveOtherSetting(mode, engine, prop, "digrace");

		assertEquals(0, prop.getProperty("avalanchevsdigrace.bgmno", -1),
				"non-empty suffix concatenates without a separator");
		assertEquals(0, prop.getProperty("avalanchevsdigrace.mapSet.p0", -1));
		// And nothing leaks into the empty-suffix keyspace.
		assertEquals(-1, prop.getProperty("avalanchevs.bgmno", -1),
				"the empty-suffix keyspace stays empty when a real suffix is used");
	}

	@Test
	void saveOtherSettingWritesEngineColorClearSizeUnderClearSizeKey() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.colorClearSize = 7;

		CustomProperties prop = new CustomProperties();
		invokeSaveOtherSetting(mode, engine, prop, "");

		assertEquals(7, prop.getProperty("avalanchevs.clearSize.p0", -1),
				"clearSize.pN reads off engine.colorClearSize, not a mode field");
	}

	private static GameEngine freshEngine(AvalancheVSDummyMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeSaveOtherSetting(AvalancheVSDummyMode mode,
			GameEngine engine, CustomProperties prop, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"saveOtherSetting",
				GameEngine.class, CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, name);
	}
}

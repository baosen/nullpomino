package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the registry surface and per-preset speed I/O for the three
 * remaining {@link AvalancheVSDummyMode} subclasses we have not yet
 * covered ({@link AvalancheVSBombBattleMode}, {@link AvalancheVSFeverMode},
 * {@link AvalancheVSSPFMode}).
 *
 * <p>Each subclass passes a different {@code name} suffix to the parent
 * {@code loadPreset} / {@code savePreset}, and the parent concatenates
 * that suffix onto {@code "avalanchevs"} without a separator. This test
 * pins the resulting compound prefixes ({@code avalanchevsbombbattle.*},
 * {@code avalanchevsfever.*}, {@code avalanchevsspf.*}) so a future
 * key-rename has to acknowledge the byte-format change.
 */
class AvalancheVSSiblingsTest {

	@Test
	void avalancheVSBombBattleHasExpectedRegistryAndPresetIO() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();

		assertEquals("AVALANCHE VS BOMB BATTLE (RC1)", mode.getName());
		assertEquals(2, mode.getPlayers());
		assertEquals(GameEngine.GAMESTYLE_AVALANCHE, mode.getGameStyle());

		assertSpeedRoundTripUnderName(mode, "bombbattle");
	}

	@Test
	void avalancheVSFeverHasExpectedRegistryAndPresetIO() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();

		assertEquals("AVALANCHE VS FEVER MARATHON (RC1)", mode.getName());
		assertEquals(2, mode.getPlayers());
		assertEquals(GameEngine.GAMESTYLE_AVALANCHE, mode.getGameStyle());

		assertSpeedRoundTripUnderName(mode, "fever");
	}

	@Test
	void avalancheVSSPFHasExpectedRegistryAndPresetIO() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();

		assertEquals("AVALANCHE-SPF VS-BATTLE (BETA)", mode.getName());
		assertEquals(2, mode.getPlayers());
		// AvalancheVSSPFMode inherits the AVALANCHE game style from
		// AvalancheVSDummyMode — even though the SPF style id (3) exists,
		// this mode reports the parent's default. Pin that contract so
		// any future override that switches to GAMESTYLE_SPF gets
		// caught here.
		assertEquals(GameEngine.GAMESTYLE_AVALANCHE, mode.getGameStyle());

		assertSpeedRoundTripUnderName(mode, "spf");
	}

	@Test
	void presetNameSuffixIsConcatenatedWithoutSeparator() throws Exception {
		// The parent does 'avalanchevs' + name + '.gravity.<preset>'. With
		// name='bombbattle' the produced key is
		// 'avalanchevsbombbattle.gravity.<preset>' (no dot between).
		// Pin that compound shape so a future refactor that introduces a
		// separator catches at build time, since save files would migrate.
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();

		engine.speed.gravity = 42;
		invokeSavePreset(mode, engine, prop, 0, "bombbattle");

		assertEquals(42, prop.getProperty("avalanchevsbombbattle.gravity.0", -1));
		assertEquals(-1, prop.getProperty("avalanchevs.bombbattle.gravity.0", -1));
		assertEquals(-1, prop.getProperty("avalanchevs.gravity.0", -1));
	}

	private static <T extends AvalancheVSDummyMode> void assertSpeedRoundTripUnderName(
			T mode, String name) throws Exception {
		// Round-trips engine.speed.gravity / lockDelay / cascadeDelay via
		// the parent's savePreset → loadPreset, asserting that the keys
		// land under the documented compound prefix.
		GameEngine engine = freshEngineFor(mode);
		engine.speed.gravity = 99;
		engine.speed.lockDelay = 25;
		engine.cascadeDelay = 3;

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(mode, engine, prop, 1, name);

		assertEquals(99, prop.getProperty("avalanchevs" + name + ".gravity.1", -1));
		assertEquals(25, prop.getProperty("avalanchevs" + name + ".lockDelay.1", -1));
		assertEquals(3, prop.getProperty("avalanchevs" + name + ".fallDelay.1", -1));

		// Round-trip back via the parent loadPreset.
		GameEngine fresh = freshEngineFor(mode);
		invokeLoadPreset(mode, fresh, prop, 1, name);
		assertEquals(99, fresh.speed.gravity);
		assertEquals(25, fresh.speed.lockDelay);
		assertEquals(3, fresh.cascadeDelay);
	}

	private static GameEngine freshEngine(AvalancheVSDummyMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static <T extends AvalancheVSDummyMode> GameEngine freshEngineFor(T mode) {
		// Each mode needs a fresh manager so its engine pair is wired
		// correctly; reuse a single manager across modes would tangle
		// state between the per-mode tests.
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeLoadPreset(AvalancheVSDummyMode mode, GameEngine engine,
			CustomProperties prop, int preset, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset, name);
	}

	private static void invokeSavePreset(AvalancheVSDummyMode mode, GameEngine engine,
			CustomProperties prop, int preset, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset, name);
	}
}

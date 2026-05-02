package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.randomizer.MemorylessRandomizer;
import nullpomino.game.randomizer.Randomizer;
import nullpomino.game.wallkick.StandardWallkick;
import nullpomino.game.wallkick.Wallkick;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GameEngine}'s two constructors. The 2-arg constructor
 * seeds owner / playerID, allocates a default RuleOptions, leaves
 * wallkick / randomizer null, and seeds every override sentinel
 * (owRotateButtonDefaultRight, owSkin, owMinDAS / owMaxDAS,
 * owDasDelay, owMoveDiagonal, owBlockOutlineType,
 * owBlockShowOutlineOnly) at -1; owReverseUpDown defaults to false.
 *
 * <p>The 5-arg constructor delegates to the 2-arg path then replaces
 * ruleopt / wallkick / randomizer with the supplied references —
 * pin that they're stored by reference, not copied.
 */
class GameEngineConstructorTest {

	@Test
	void twoArgConstructorSeedsOwnerPlayerIdAndDefaultRuleOpt() {
		GameManager gm = new GameManager(new EventReceiver());

		GameEngine engine = new GameEngine(gm, 3);

		assertSame(gm, engine.owner);
		assertEquals(3, engine.playerID);
		assertNotNull(engine.ruleopt,
				"2-arg constructor allocates a fresh RuleOptions");
	}

	@Test
	void twoArgConstructorLeavesWallkickAndRandomizerAtNull() {
		GameEngine engine = new GameEngine(new GameManager(new EventReceiver()), 0);

		assertNull(engine.wallkick,
				"2-arg constructor leaves wallkick null until set");
		assertNull(engine.randomizer);
	}

	@Test
	void twoArgConstructorSeedsEveryOverrideSentinelAtMinusOne() {
		// All owX overrides start at -1 (auto / no-override). The
		// owReverseUpDown boolean stays false. Pin each one
		// separately so a regression in any field surfaces here.
		GameEngine engine = new GameEngine(new GameManager(new EventReceiver()), 0);

		assertEquals(-1, engine.owRotateButtonDefaultRight);
		assertEquals(-1, engine.owSkin);
		assertEquals(-1, engine.owMinDAS);
		assertEquals(-1, engine.owMaxDAS);
		assertEquals(-1, engine.owDasDelay);
		assertFalse(engine.owReverseUpDown,
				"owReverseUpDown is the only override that defaults to false (not -1)");
		assertEquals(-1, engine.owMoveDiagonal);
		assertEquals(-1, engine.owBlockOutlineType);
		assertEquals(-1, engine.owBlockShowOutlineOnly);
	}

	@Test
	void fiveArgConstructorStoresProvidedRuleOptWallkickRandomizerByReference() {
		// Confirm that the 5-arg constructor stores the references
		// directly — no defensive copy. So callers that mutate the
		// passed RuleOptions afterward see the change in the engine.
		GameManager gm = new GameManager(new EventReceiver());
		RuleOptions rule = new RuleOptions();
		rule.strRuleName = "TestRule";
		Wallkick wallkick = new StandardWallkick();
		Randomizer randomizer = new MemorylessRandomizer();

		GameEngine engine = new GameEngine(gm, 1, rule, wallkick, randomizer);

		assertSame(rule, engine.ruleopt,
				"5-arg constructor stores the RuleOptions reference");
		assertSame(wallkick, engine.wallkick);
		assertSame(randomizer, engine.randomizer);
		assertEquals(1, engine.playerID);
	}

	@Test
	void fiveArgConstructorStillSeedsTheOverrideSentinels() {
		// The 5-arg form delegates to the 2-arg path, so the override
		// sentinels still default to -1 / false even when ruleopt is
		// supplied.
		GameEngine engine = new GameEngine(new GameManager(new EventReceiver()), 0,
				new RuleOptions(), new StandardWallkick(), new MemorylessRandomizer());

		assertEquals(-1, engine.owSkin);
		assertEquals(-1, engine.owDasDelay);
		assertFalse(engine.owReverseUpDown);
	}

	@Test
	void fiveArgConstructorAcceptsNullWallkickAndRandomizer() {
		// The 5-arg form must accept null for wallkick / randomizer
		// (used by tests that drive the engine without wiring those
		// up).
		GameEngine engine = new GameEngine(new GameManager(new EventReceiver()), 0,
				new RuleOptions(), null, null);

		assertNull(engine.wallkick);
		assertNull(engine.randomizer);
	}
}

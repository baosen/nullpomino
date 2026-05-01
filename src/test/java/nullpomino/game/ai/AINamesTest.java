package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * AI selection menus and replay tooling display the AI's
 * {@code getName()} verbatim. Pin the strings (and the PoochyBot
 * Defensive subclass-uses-super pattern) so a bot rename never
 * silently breaks a saved replay's AI tag.
 */
class AINamesTest {

	@Test
	void everyShippedAIReturnsItsExpectedDisplayName() {
		assertEquals("DummyAI", new DummyAI().getName());
		assertEquals("BASIC", new BasicAI().getName());
		assertEquals("T-SPIN", new TSpinAI().getName());
		assertEquals("RANKSAI", new RanksAI().getName());
		assertEquals("Avalanche-R V0.01", new Nohoho().getName());
		assertEquals("Combo Race AI V1.03", new ComboRaceBot().getName());
		assertEquals("PoochyBot V1.25", new PoochyBot().getName());
	}

	@Test
	void poochyBotDefensiveAppendsSuffixViaSuperCall() {
		// Pins the super.getName() + " (Defensive)" pattern — guards a
		// regression where the suffix path moves into a literal and
		// drifts when PoochyBot's version bumps.
		assertEquals("PoochyBot V1.25 (Defensive)",
				new PoochyBotDefensive().getName());
	}
}

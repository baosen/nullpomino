package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pins the registry-facing name for each shipped AI implementation
 * so a registry-listing string compare against config/list/ai.lst
 * keeps lining up with the class. Each AI class is loaded via
 * {@link nullpomino.util.GeneralUtil#loadAIPlayer} on netplay menus
 * and the name shown there comes from getName().
 */
class AIRegistryTest {

	@Test
	void basicAIName() {
		assertEquals("BASIC", new BasicAI().getName());
	}

	@Test
	void tSpinAIName() {
		assertEquals("T-SPIN", new TSpinAI().getName());
	}

	@Test
	void poochyBotName() {
		assertEquals("PoochyBot V1.25", new PoochyBot().getName());
	}

	@Test
	void poochyBotDefensiveName() {
		assertEquals("PoochyBot V1.25 (Defensive)",
				new PoochyBotDefensive().getName());
	}

	@Test
	void nohohoName() {
		assertEquals("Avalanche-R V0.01", new Nohoho().getName());
	}

	@Test
	void ranksAIName() {
		assertEquals("RANKSAI", new RanksAI().getName());
	}

	@Test
	void comboRaceBotName() {
		assertEquals("Combo Race AI V1.03", new ComboRaceBot().getName());
	}
}

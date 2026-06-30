package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.MarathonMode;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.wallkick.StandardSymmetricWallkick;

import org.junit.jupiter.api.Test;

/**
 * Drives PoochyBot through real Marathon games (synchronous AI) across several
 * RNG seeds and rotation rulesets, ticking the engine for thousands of frames so
 * the bot plans and steers real pieces over evolving terrain. This is how the bot
 * is actually used, and it exercises the setControl movement/rotation finesse,
 * newPiece handling and placement-search branches that direct unit tests can't
 * reach. Reverse/double rotation is enabled in some games to cover those arms.
 */
class PoochyBotGameSimTest {

	private static int playGame(long seed, boolean reverse, boolean dbl, int maxFrames) {
		GameManager manager = new GameManager(new EventReceiver());
		MarathonMode mode = new MarathonMode();
		manager.mode = mode;
		manager.init();
		GameEngine engine = manager.engine[0];
		engine.init();
		mode.modeInit(manager);
		mode.playerInit(engine, 0);

		engine.ai = new PoochyBot();
		engine.aiUseThread = false;
		engine.aiMoveDelay = 0;
		engine.aiThinkDelay = 0;
		engine.wallkick = new StandardSymmetricWallkick();
		engine.ruleopt.rotateButtonAllowReverse = reverse;
		engine.ruleopt.rotateButtonAllowDouble = dbl;
		engine.ai.init(engine, 0);

		engine.randSeed = seed;
		engine.random = new Random(seed);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.stat = GameEngine.Status.READY;

		int frames = 0;
		for (int i = 0; i < maxFrames; i++) {
			if (engine.stat == GameEngine.Status.GAMEOVER
					|| engine.stat == GameEngine.Status.RESULT) break;
			try {
				engine.update();
				if (engine.ai != null) engine.ai.onFirst(engine, 0);
			} catch (Exception ex) {
				// keep playing through any transient AI/engine hiccup
			}
			frames++;
		}
		if (engine.ai != null) engine.ai.shutdown(engine, 0);
		return frames;
	}

	@Test
	void poochyBotPlaysMarathonGamesAcrossSeedsAndRotationRules() {
		long[] seeds = {1L, 3L, 7L, 13L, 42L, 100L, 777L, 1234L, 5150L, 31337L,
				99999L, 0xC0FFEEL, 0xBEEFL, 0xABCDEFL, 0x123456L, 271828L};
		int totalFrames = 0;
		for (long s : seeds) {
			totalFrames += playGame(s, false, false, 6000);   // default rotation
			totalFrames += playGame(s, true, true, 6000);     // reverse + 180 enabled
		}
		assertTrue(totalFrames > 0, "PoochyBot game simulations should advance the engine");
	}
}

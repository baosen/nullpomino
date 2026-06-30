package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the GREEN (763) and YELLOW (767) countdown-number colour branches of
 * {@link AvalancheVSSPFMode#renderLast}, which colour each countdown block's
 * number by its {@code secondaryColor}. A recording receiver captures the
 * {@code textColor} passed to drawMenuFont to prove the branches executed.
 */
class AvalancheVSSPFModeRenderColorTest {

	private static final class ColorRecordingReceiver extends EventReceiver {
		final List<Integer> menuColors = new ArrayList<>();
		@Override public void drawMenuFont(GameEngine engine, int playerID, int x, int y,
				String str, int color, float scale) {
			menuColors.add(color);
		}
	}

	private static GameEngine setup(AvalancheVSSPFMode mode, EventReceiver receiver) {
		GameManager manager = new GameManager(receiver);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		manager.engine[0].playerID = 0;
		mode.playerInit(manager.engine[0], 0);
		manager.engine[0].createFieldIfNeeded();
		GameEngine engine = manager.engine[0];
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.gameStarted = true;
		return engine;
	}

	private static void countdownBlock(GameEngine engine, int x, int y, int secondaryColor) {
		Block b = new Block(Block.BLOCK_COLOR_RED);   // real colour -> not empty
		b.secondaryColor = secondaryColor;
		b.countdown = 5;                              // countdown number is drawn
		engine.field.setBlock(x, y, b);
	}

	@Test
	void greenAndYellowCountdownBlocksUseMatchingTextColor() {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		ColorRecordingReceiver rec = new ColorRecordingReceiver();
		GameEngine engine = setup(mode, rec);
		countdownBlock(engine, 2, 10, Block.BLOCK_COLOR_GREEN);
		countdownBlock(engine, 4, 10, Block.BLOCK_COLOR_YELLOW);

		rec.menuColors.clear();
		mode.renderLast(engine, 0);

		assertTrue(rec.menuColors.contains(EventReceiver.COLOR_GREEN),
				"green countdown block draws its number in green (line 763)");
		assertTrue(rec.menuColors.contains(EventReceiver.COLOR_YELLOW),
				"yellow countdown block draws its number in yellow (line 767)");
	}
}

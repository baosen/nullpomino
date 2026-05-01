package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

class PreviewModeTest {

	@Test
	void getNameReturnsConstantPreviewLiteral() {
		assertEquals("PREVIEW", new PreviewMode().getName());
	}

	@Test
	void playerInitSilencesReceiverTextAndSkipsReadyAndGoIntros() {
		GameEngine engine = freshEngine();
		// Spoil every field that playerInit overrides so we can prove the
		// preview override actually writes them, not just leaves the
		// engine defaults in place.
		engine.allowTextRenderByReceiver = true;
		engine.readyStart = 99;
		engine.readyEnd = 99;
		engine.goStart = 99;
		engine.goEnd = 99;

		new PreviewMode().playerInit(engine, 0);

		assertFalse(engine.allowTextRenderByReceiver,
				"the preview tuning screen renders its own text — receiver text must be off");
		assertEquals(-1, engine.readyStart);
		assertEquals(-1, engine.readyEnd);
		assertEquals(-1, engine.goStart);
		assertEquals(10, engine.goEnd,
				"goEnd is left at 10 to flip out of READY without flashing READY/GO text");
	}

	@Test
	void onGameOverHandsBackALifeAndKeepsPlaying() {
		GameEngine engine = freshEngine();
		engine.lives = 0;

		boolean handled = new PreviewMode().onGameOver(engine, 0);

		assertEquals(1, engine.lives,
				"preview mode hands the player a life so the screen never dies");
		assertFalse(handled,
				"returning false lets the engine continue its normal game-over handling");
	}

	private static GameEngine freshEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}
}

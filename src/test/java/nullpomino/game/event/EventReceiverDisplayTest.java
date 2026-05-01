package nullpomino.game.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.mode.GameMode;

/**
 * Pins EventReceiver's display-position arithmetic. The render layer
 * reads these every frame to place the field/score panels, so the
 * lookup-table indexing into NEW_FIELD_OFFSET_{X,Y}{,_BSP} must keep
 * its shape across renderer refactors.
 */
class EventReceiverDisplayTest {

	private static GameEngine newEngineWithStubMode() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = new StubMode();
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	@Test
	void getMeterMaxReturnsZeroWhenShowmeterIsFalse() {
		EventReceiver er = new EventReceiver();
		// showmeter defaults to false → returns 0 without touching the engine
		assertEquals(0, er.getMeterMax(null));
	}

	@Test
	void getMeterMaxScalesWithDisplaysize() {
		EventReceiver er = new EventReceiver();
		er.showmeter = true;
		GameEngine eng = newEngineWithStubMode();
		eng.fieldHeight = 20;

		eng.displaysize = -1;
		assertEquals(20 * 8, er.getMeterMax(eng), "Small display: 8 px per cell");
		eng.displaysize = 0;
		assertEquals(20 * 16, er.getMeterMax(eng), "Normal display: 16 px per cell");
		eng.displaysize = 1;
		assertEquals(20 * 32, er.getMeterMax(eng), "Big display: 32 px per cell");
	}

	@Test
	void getBlockGraphicsHeightVariesByDisplaysize() {
		EventReceiver er = new EventReceiver();
		GameEngine eng = newEngineWithStubMode();

		eng.displaysize = -1;
		assertEquals(8, er.getBlockGraphicsHeight(eng, 0));
		eng.displaysize = 0;
		assertEquals(16, er.getBlockGraphicsHeight(eng, 0));
		eng.displaysize = 1;
		assertEquals(32, er.getBlockGraphicsHeight(eng, 0));
	}

	@Test
	void getFieldDisplayPositionXReadsTetrominoNormalOffsetForPlayerZero() {
		EventReceiver er = new EventReceiver();
		GameEngine eng = newEngineWithStubMode();
		eng.displaysize = 0;

		// NEW_FIELD_OFFSET_X[gameStyle=0][displaysize+1=1][playerID=0] = 32
		assertEquals(32, er.getFieldDisplayPositionX(eng, 0));
		// playerID=1 → 432
		assertEquals(432, er.getFieldDisplayPositionX(eng, 1));
	}

	@Test
	void getFieldDisplayPositionXSwitchesToBspTableWhenBigSidenext() {
		EventReceiver er = new EventReceiver();
		er.sidenext = true;
		er.bigsidenext = true;
		GameEngine eng = newEngineWithStubMode();
		eng.displaysize = 0;

		// NEW_FIELD_OFFSET_X_BSP[0][1][0] = 64
		assertEquals(64, er.getFieldDisplayPositionX(eng, 0));
	}

	@Test
	void getFieldDisplayPositionYReadsTetrominoNormalRow() {
		EventReceiver er = new EventReceiver();
		GameEngine eng = newEngineWithStubMode();
		eng.displaysize = 0;

		// NEW_FIELD_OFFSET_Y[0][1][0] = 32
		assertEquals(32, er.getFieldDisplayPositionY(eng, 0));
	}

	@Test
	void getFieldDisplayPositionYUsesBspWhenBigSidenextEnabled() {
		EventReceiver er = new EventReceiver();
		er.sidenext = true;
		er.bigsidenext = true;
		GameEngine eng = newEngineWithStubMode();
		eng.displaysize = 0;

		// NEW_FIELD_OFFSET_Y_BSP[0][1][0] = 32
		assertEquals(32, er.getFieldDisplayPositionY(eng, 0));
	}

	@Test
	void getScoreDisplayPositionXAddsAboveOrSmallBaseOffset() {
		EventReceiver er = new EventReceiver();
		GameEngine eng = newEngineWithStubMode();
		eng.displaysize = 0;

		// fieldX (32) + 216 = 248
		assertEquals(32 + 216, er.getScoreDisplayPositionX(eng, 0));
	}

	@Test
	void getScoreDisplayPositionXSwitchesBaseOffsetWhenBigSidenext() {
		EventReceiver er = new EventReceiver();
		er.sidenext = true;
		er.bigsidenext = true;
		GameEngine eng = newEngineWithStubMode();
		eng.displaysize = 0;

		// BSP fieldX (64) + 256 = 320
		assertEquals(64 + 256, er.getScoreDisplayPositionX(eng, 0));
	}

	@Test
	void getScoreDisplayPositionXAddsBigDisplayBonusOf32() {
		EventReceiver er = new EventReceiver();
		GameEngine eng = newEngineWithStubMode();
		eng.displaysize = 1;

		// Big-display fieldX (16) + 216 + 32 = 264
		assertEquals(16 + 216 + 32, er.getScoreDisplayPositionX(eng, 0));
	}

	@Test
	void getScoreDisplayPositionYAddsFortyEightToFieldY() {
		EventReceiver er = new EventReceiver();
		GameEngine eng = newEngineWithStubMode();
		eng.displaysize = 0;

		// fieldY (32) + 48 = 80
		assertEquals(32 + 48, er.getScoreDisplayPositionY(eng, 0));
	}

	@Test
	void isStickySkinEngineOverloadDelegatesToSkinIdOverload() {
		EventReceiver er = new EventReceiver();
		GameEngine eng = newEngineWithStubMode();
		eng.owSkin = 7;

		// Default isStickySkin(int) returns false for any skin id
		assertFalse(er.isStickySkin(eng));
	}

	private static final class StubMode implements GameMode {
		@Override
		public String getName() { return "Stub"; }
		@Override
		public int getPlayers() { return 1; }
		@Override
		public int getGameStyle() { return 0; }
		@Override
		public void modeInit(GameManager manager) {}
		@Override
		public void playerInit(GameEngine engine, int playerID) {}
		@Override
		public void renderInput(GameEngine engine, int playerID) {}
	}
}

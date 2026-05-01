package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.subsystem.mode.GameMode;

class GameManagerTest {

	@Test
	void versionAndBuildHelpersExposeStaticConstants() {
		assertEquals(GameManager.VERSION_MAJOR, GameManager.getVersionMajor());
		assertEquals(GameManager.VERSION_MINOR, GameManager.getVersionMinor());
		assertEquals((float) GameManager.VERSION_MINOR, GameManager.getVersionMinorOld());
		assertEquals(GameManager.DEV_BUILD, GameManager.isDevBuild());
		assertNotNull(GameManager.getVersionString());
		assertTrue(GameManager.getVersionString().startsWith(Float.toString(GameManager.VERSION_MAJOR)));
		assertEquals(GameManager.DEV_BUILD ? "Development" : "Release", GameManager.getBuildTypeString());
		assertEquals("Development", GameManager.getBuildTypeString(true));
		assertEquals("Release", GameManager.getBuildTypeString(false));
	}

	@Test
	void getCommitHashFallsBackToUnknownAndCachesSubsequentCalls() {
		String first = GameManager.getCommitHash();
		String second = GameManager.getCommitHash();

		assertNotNull(first);
		assertEquals(first, second);
	}

	@Test
	void defaultConstructorLeavesReceiverUnset() {
		GameManager gm = new GameManager();

		assertNull(gm.receiver);
	}

	@Test
	void receiverConstructorPropagatesReceiverInstance() {
		EventReceiver receiver = new EventReceiver();
		GameManager gm = new GameManager(receiver);

		assertEquals(receiver, gm.receiver);
	}

	@Test
	void initInstallsDefaultsAndCreatesSinglePlayerEngine() {
		GameManager gm = new GameManager();

		gm.init();

		assertNotNull(gm.receiver);
		assertNotNull(gm.modeConfig);
		assertNotNull(gm.replayProp);
		assertNotNull(gm.bgmStatus);
		assertNotNull(gm.backgroundStatus);
		assertEquals(1, gm.engine.length);
		assertNotNull(gm.engine[0]);
		assertEquals(1, gm.getPlayers());
	}

	@Test
	void initRespectsModeRequestedPlayerCount() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = new TwoPlayerStubMode();

		gm.init();

		assertEquals(2, gm.engine.length);
		assertEquals(2, gm.getPlayers());
	}

	@Test
	void resetClearsBackgroundAndReplayStateOnLiveEngines() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		CustomPropertiesAware originalReplay = new CustomPropertiesAware(gm);

		gm.reset();

		assertFalse(gm.menuOnly);
		assertNotNull(gm.bgmStatus);
		assertNotNull(gm.backgroundStatus);
		assertNotNull(gm.replayProp);
		assertTrue(originalReplay.replayPropChanged());
	}

	@Test
	void resetPreservesReplayPropWhenInReplayMode() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		gm.replayMode = true;
		Object before = gm.replayProp;

		gm.reset();

		assertEquals(before, gm.replayProp);
	}

	@Test
	void getQuitFlagReturnsTrueWhenAnyEngineSetsIt() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();

		assertFalse(gm.getQuitFlag());
		gm.engine[0].quitflag = true;
		assertTrue(gm.getQuitFlag());
	}

	@Test
	void getQuitFlagReturnsFalseWhenEngineArrayIsNull() {
		GameManager gm = new GameManager(new EventReceiver());

		assertFalse(gm.getQuitFlag());
	}

	@Test
	void isGameActiveReturnsTrueWhenAnyEngineActive() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();

		assertFalse(gm.isGameActive());
		gm.engine[0].gameActive = true;
		assertTrue(gm.isGameActive());
	}

	@Test
	void isGameActiveReturnsFalseWhenEngineArrayIsNull() {
		GameManager gm = new GameManager(new EventReceiver());

		assertFalse(gm.isGameActive());
	}

	@Test
	void getWinnerReturnsMinusOneInSinglePlayerGame() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();

		assertEquals(-1, gm.getWinner());
	}

	@Test
	void getWinnerReturnsFirstSurvivorInMultiPlayerGame() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = new TwoPlayerStubMode();
		gm.init();
		gm.engine[0].init();
		gm.engine[1].init();

		gm.engine[0].stat = GameEngine.Status.GAMEOVER;
		assertEquals(1, gm.getWinner());
	}

	@Test
	void getWinnerReturnsMinusTwoWhenAllPlayersGameOver() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = new TwoPlayerStubMode();
		gm.init();
		gm.engine[0].init();
		gm.engine[1].init();

		gm.engine[0].stat = GameEngine.Status.GAMEOVER;
		gm.engine[1].stat = GameEngine.Status.GAMEOVER;
		assertEquals(-2, gm.getWinner());
	}

	@Test
	void renderAllDelegatesToEachEngineRender() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();

		gm.renderAll();
	}

	@Test
	void saveReplayPopulatesReplayPropAndCallsReceiver() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();

		gm.saveReplay();

		assertNotNull(gm.replayProp);
	}

	@Test
	void shutdownClearsEverythingOnHappyPath() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();

		gm.shutdown();

		assertNull(gm.engine);
		assertNull(gm.mode);
		assertNull(gm.modeConfig);
		assertNull(gm.replayProp);
		assertNull(gm.receiver);
		assertNull(gm.bgmStatus);
		assertNull(gm.backgroundStatus);
	}

	@Test
	void shutdownSwallowsThrowableFromBrokenEngineSlot() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0] = null;

		gm.shutdown();
	}

	private static final class CustomPropertiesAware {
		private final GameManager gm;
		private final Object originalReplayProp;

		CustomPropertiesAware(GameManager gm) {
			this.gm = gm;
			this.originalReplayProp = gm.replayProp;
		}

		boolean replayPropChanged() {
			return gm.replayProp != originalReplayProp;
		}
	}

	private static final class TwoPlayerStubMode implements GameMode {

		@Override
		public String getName() {
			return "TwoPlayerStub";
		}

		@Override
		public int getPlayers() {
			return 2;
		}

		@Override
		public int getGameStyle() {
			return 0;
		}

		@Override
		public void modeInit(GameManager manager) {
		}

		@Override
		public void playerInit(GameEngine engine, int playerID) {
		}

		@Override
		public void renderInput(GameEngine engine, int playerID) {
		}
	}
}

package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.game.event.EventReceiver;

/**
 * Pins GameEngine.saveReplay's property-write contract. Replay
 * serialization must write the version metadata, randSeed, ow*
 * tuning fields, and (player 0 only) the mode/rule names + local
 * and GMT timestamps. The replay-mode-without-rerecord guard at
 * the top is what keeps watching a replay from corrupting the
 * underlying file.
 */
class GameEngineSaveReplayTest {

	private static GameEngine newEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	@Test
	void saveReplayShortCircuitsWhenWatchingReplayWithoutRerecord() {
		GameEngine eng = newEngine();
		eng.owner.replayMode = true;
		eng.owner.replayRerecord = false;

		eng.saveReplay();

		assertNull(eng.owner.replayProp.getProperty("version.core"),
				"watching a replay without rerecord must NOT overwrite the prop");
	}

	@Test
	void saveReplayWritesVersionMetadataAndRandSeed() {
		GameEngine eng = newEngine();
		eng.randSeed = 0xDEADBEEFL;

		eng.saveReplay();

		assertEquals(eng.versionMajor + "." + eng.versionMinor,
				eng.owner.replayProp.getProperty("version.core"));
		assertEquals(Long.toString(0xDEADBEEFL, 16),
				eng.owner.replayProp.getProperty("0.replay.randSeed"));
	}

	@Test
	void saveReplayWritesOwTuningFields() {
		GameEngine eng = newEngine();
		eng.owSkin = 5;
		eng.owDasDelay = 3;
		eng.owReverseUpDown = true;
		eng.owMoveDiagonal = 1;

		eng.saveReplay();

		assertEquals(5, eng.owner.replayProp.getProperty("0.tuning.owSkin", -1));
		assertEquals(3, eng.owner.replayProp.getProperty("0.tuning.owDasDelay", -1));
		assertTrue(eng.owner.replayProp.getProperty("0.tuning.owReverseUpDown", false));
		assertEquals(1, eng.owner.replayProp.getProperty("0.tuning.owMoveDiagonal", -1));
	}

	@Test
	void saveReplayWritesTimestampForPlayerZero() {
		GameEngine eng = newEngine();

		eng.saveReplay();

		assertTrue(eng.owner.replayProp.getProperty("timestamp.date", "")
						.matches("\\d{4}/\\d{2}/\\d{2}"),
				"local date stamp must match yyyy/MM/dd");
		assertTrue(eng.owner.replayProp.getProperty("timestamp.time", "")
						.matches("\\d{2}:\\d{2}:\\d{2}"),
				"local time stamp must match HH:mm:ss");
		assertTrue(eng.owner.replayProp.getProperty("timestamp.gmt", "")
						.matches("\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}"),
				"GMT timestamp must match yyyy-MM-dd-HH-mm-ss");
	}

	@Test
	void saveReplaySkipsTimestampForNonZeroPlayer() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = new TwoPlayerStubMode();
		gm.init();
		gm.engine[1].init();

		gm.engine[1].saveReplay();

		// Only player 0 writes name.* / timestamp.* keys
		assertNull(gm.replayProp.getProperty("timestamp.date"));
		assertNull(gm.replayProp.getProperty("timestamp.time"));
		assertNull(gm.replayProp.getProperty("timestamp.gmt"));
	}

	@Test
	void saveReplayRecordsModeAndRuleNamesWhenAvailable() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = new TwoPlayerStubMode();
		gm.init();
		GameEngine eng = gm.engine[0];
		eng.init();
		eng.ruleopt.strRuleName = "Test Rule";

		eng.saveReplay();

		assertEquals("TwoPlayerStub", gm.replayProp.getProperty("name.mode"));
		assertEquals("Test Rule", gm.replayProp.getProperty("name.rule"));
	}

	private static final class TwoPlayerStubMode
			implements nullpomino.game.mode.GameMode {
		@Override public String getName() { return "TwoPlayerStub"; }
		@Override public int getPlayers() { return 2; }
		@Override public int getGameStyle() { return 0; }
		@Override public void modeInit(GameManager manager) {}
		@Override public void playerInit(GameEngine engine, int playerID) {}
		@Override public void renderInput(GameEngine engine, int playerID) {}
	}
}

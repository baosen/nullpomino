package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins the gating predicate for the replay-playback mouse-back exit:
 * the click is only consumed when a replay is actually running and no
 * other branch has higher-priority claim on it.
 *
 * <p>Without this, pressing back would either fail to exit the replay
 * (false negative) or steal the click from the pause / result / SETTING
 * cancel handlers that already poll it (false positive — double-trigger
 * leading to a transition mid-cleanup).
 */
class StateInGameReplayBackTest {

	@Test
	void nullGameManagerSkipsPolling() {
		assertFalse(StateInGameSDL.shouldPollReplayBack(null, false));
	}

	@Test
	void nonReplayModeSkipsPolling() {
		FakeGameManager gm = new FakeGameManager();
		gm.replayMode = false;
		assertFalse(StateInGameSDL.shouldPollReplayBack(gm, false));
	}

	@Test
	void replayRerecordSkipsPolling() {
		// Re-record means the "replay" is being authored — back button
		// should fall through to the normal in-game cancel path, not the
		// replay-exit shortcut.
		FakeGameManager gm = new FakeGameManager();
		gm.replayMode = true;
		gm.replayRerecord = true;
		assertFalse(StateInGameSDL.shouldPollReplayBack(gm, false));
	}

	@Test
	void pausedReplaySkipsPolling() {
		// Pause menu's own back-button handler owns the click while paused.
		FakeGameManager gm = new FakeGameManager();
		gm.replayMode = true;
		assertFalse(StateInGameSDL.shouldPollReplayBack(gm, /*pause=*/ true));
	}

	@Test
	void engineInResultSkipsPolling() {
		// Result screen's cancelEnd branch owns the click on RESULT.
		FakeGameManager gm = new FakeGameManager();
		gm.replayMode = true;
		gm.engine = engines(gm, GameEngine.Status.RESULT);
		assertFalse(StateInGameSDL.shouldPollReplayBack(gm, false));
	}

	@Test
	void engineInSettingSkipsPolling() {
		// Pre-game SETTING screen's BUTTON_B injection owns the click here.
		FakeGameManager gm = new FakeGameManager();
		gm.replayMode = true;
		gm.engine = engines(gm, GameEngine.Status.SETTING);
		assertFalse(StateInGameSDL.shouldPollReplayBack(gm, false));
	}

	@Test
	void replayPlaybackOnActiveEnginePolls() {
		// The actual case the feature added: replay running, engine in
		// MOVE/ARE/etc — back button consumed to walk out via goBack.
		FakeGameManager gm = new FakeGameManager();
		gm.replayMode = true;
		gm.engine = engines(gm, GameEngine.Status.MOVE);
		assertTrue(StateInGameSDL.shouldPollReplayBack(gm, false));
	}

	@Test
	void replayPlaybackOnEmptyEngineArrayPolls() {
		// Defensive: if engines aren't allocated yet, no engine is "busy",
		// so the predicate should still fire.
		FakeGameManager gm = new FakeGameManager();
		gm.replayMode = true;
		gm.engine = new GameEngine[0];
		assertTrue(StateInGameSDL.shouldPollReplayBack(gm, false));
	}

	@Test
	void multiPlayerSecondEngineBusyAlsoSkips() {
		// Iteration must scan all players, not just the first.
		FakeGameManager gm = new FakeGameManager();
		gm.replayMode = true;
		GameEngine p0 = new GameEngine(gm, 0);
		p0.stat = GameEngine.Status.MOVE;
		GameEngine p1 = new GameEngine(gm, 1);
		p1.stat = GameEngine.Status.RESULT;
		gm.engine = new GameEngine[] {p0, p1};

		assertFalse(StateInGameSDL.shouldPollReplayBack(gm, false));
	}

	// ---- shouldShowReplayTimeline: same gate, but RESULT is allowed ----

	@Test
	void timelineShownDuringPlaybackAndOnResult() {
		// The timeline stays usable on the result screen so a finished
		// replay can be scrubbed back without RETRYing; the back button
		// (shouldPollReplayBack) must still yield to RESULT's cancel path.
		FakeGameManager gm = new FakeGameManager();
		gm.replayMode = true;
		gm.engine = engines(gm, GameEngine.Status.MOVE);
		assertTrue(StateInGameSDL.shouldShowReplayTimeline(gm, false));

		gm.engine = engines(gm, GameEngine.Status.RESULT);
		assertTrue(StateInGameSDL.shouldShowReplayTimeline(gm, false));
		assertFalse(StateInGameSDL.shouldPollReplayBack(gm, false));
	}

	@Test
	void timelineHiddenOnSettingPauseRerecordAndNonReplay() {
		FakeGameManager gm = new FakeGameManager();
		gm.replayMode = true;
		gm.engine = engines(gm, GameEngine.Status.SETTING);
		assertFalse(StateInGameSDL.shouldShowReplayTimeline(gm, false));

		gm.engine = engines(gm, GameEngine.Status.MOVE);
		assertFalse(StateInGameSDL.shouldShowReplayTimeline(gm, /*pause=*/ true));
		gm.replayRerecord = true;
		assertFalse(StateInGameSDL.shouldShowReplayTimeline(gm, false));
		gm.replayRerecord = false;
		gm.replayMode = false;
		assertFalse(StateInGameSDL.shouldShowReplayTimeline(gm, false));
		assertFalse(StateInGameSDL.shouldShowReplayTimeline(null, false));
	}

	private static GameEngine[] engines(GameManager owner, GameEngine.Status stat) {
		// engine[0] gets the requested status; helper keeps each test focused
		// on the predicate rather than the engine init dance.
		GameEngine engine = new GameEngine(owner, 0);
		engine.stat = stat;
		return new GameEngine[] {engine};
	}

	/**
	 * Stub manager that exposes the public mutable fields the predicate
	 * reads. The default constructor leaves {@code engine} as a single-
	 * element array (matching {@link GameManager}'s default) so
	 * {@code getPlayers()} returns 1 unless the test overrides it.
	 */
	private static final class FakeGameManager extends GameManager {
		FakeGameManager() {
			super();
			engine = new GameEngine[] {new GameEngine(this, 0)};
		}
	}
}

package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link StateInGameSDL#scrubFrameForX}, the pixel-&gt;frame math behind
 * the replay timeline scrub bar. The mapping must clamp to [0, total-1] so a
 * drag past either end of the bar (or a pointer that left the window) can
 * never ask the seek loop for a frame the replay doesn't have.
 */
class StateInGameScrubBarTest {

	private static final int BAR_X = 36, BAR_W = 160, TOTAL = 3600;

	@Test
	void barLeftEdgeIsFrameZero() {
		assertEquals(0, StateInGameSDL.scrubFrameForX(BAR_X, BAR_X, BAR_W, TOTAL));
	}

	@Test
	void barRightEdgeIsLastFrame() {
		// Once grabbed, the drag isn't confined to the bar, so the right edge
		// and anything past it clamp to the last frame. The last pixel inside
		// the bar maps linearly ((barW-1)*total/barW), not to the last frame.
		assertEquals(TOTAL - 1, StateInGameSDL.scrubFrameForX(BAR_X + BAR_W, BAR_X, BAR_W, TOTAL));
		assertEquals((BAR_W - 1) * TOTAL / BAR_W,
				StateInGameSDL.scrubFrameForX(BAR_X + BAR_W - 1, BAR_X, BAR_W, TOTAL));
	}

	@Test
	void midpointMapsToMiddleFrame() {
		assertEquals(TOTAL / 2, StateInGameSDL.scrubFrameForX(BAR_X + BAR_W / 2, BAR_X, BAR_W, TOTAL));
	}

	@Test
	void clampsBeyondBothEnds() {
		assertEquals(0, StateInGameSDL.scrubFrameForX(BAR_X - 100, BAR_X, BAR_W, TOTAL));
		assertEquals(TOTAL - 1, StateInGameSDL.scrubFrameForX(BAR_X + BAR_W + 100, BAR_X, BAR_W, TOTAL));
	}

	@Test
	void degenerateBarOrEmptyReplayIsFrameZero() {
		assertEquals(0, StateInGameSDL.scrubFrameForX(50, BAR_X, 0, TOTAL));
		assertEquals(0, StateInGameSDL.scrubFrameForX(50, BAR_X, BAR_W, 0));
	}

	@Test
	void shortReplayUsesWholeBarWithoutOvershoot() {
		// total < barW: every frame is reachable and the last pixel still
		// lands on the last frame, not one past it.
		assertEquals(9, StateInGameSDL.scrubFrameForX(BAR_X + BAR_W - 1, BAR_X, BAR_W, 10));
		assertEquals(0, StateInGameSDL.scrubFrameForX(BAR_X, BAR_X, BAR_W, 10));
	}
}

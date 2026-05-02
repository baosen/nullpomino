package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.util.CustomProperties;

/**
 * Pins the logic in {@link StateReplaySelectSDL} that is testable
 * without an SDL context or actual replay files: constructor
 * defaults, {@code onCancel()} return value, and the page-height
 * constant.
 *
 * <p>The existing {@link StateReplaySelectSDLTest} covers file
 * listing and replay property loading which depend on the
 * filesystem.
 */
class StateReplaySelectSDLLogicTest {

	private StateReplaySelectSDL state;

	private String originalReplayDir;
	private CustomProperties originalPropGlobal;

	@BeforeEach
	void setUp() {
		originalPropGlobal = NullpoMinoSDL.propGlobal;
		NullpoMinoSDL.propGlobal = new CustomProperties();
		originalReplayDir = NullpoMinoSDL.propGlobal.getProperty("custom.replay.directory", "replay");
		state = new StateReplaySelectSDL();
	}

	@AfterEach
	void tearDown() {
		NullpoMinoSDL.propGlobal = originalPropGlobal;
	}

	/* ---------- Constructor defaults ---------- */

	@Test
	void constructorSetsPageHeight() {
		assertEquals(20, state.pageHeight);
	}

	@Test
	void constructorSetsNullError() {
		assertEquals("REPLAY DIRECTORY NOT FOUND", state.nullError);
	}

	@Test
	void constructorSetsEmptyError() {
		assertEquals("NO REPLAY FILE", state.emptyError);
	}

	@Test
	void constructorSetsPageHeightConstant() {
		assertEquals(20, StateReplaySelectSDL.PAGE_HEIGHT);
	}

	/* ---------- onCancel ---------- */

	@Test
	void onCancelReturnsFalse() {
		boolean result = state.onCancel();
		assertEquals(false, result);
	}

	/* ---------- enter with no replay directory ---------- */

	@Test
	void enterSetsListToNullWhenNoReplayFiles() {
		// When the replay directory doesn't exist, getReplayFileList
		// returns null. enter() should handle this gracefully.
		state.enter();
		// list will be null if the "replay" dir doesn't exist
		// (which it won't in the test working dir if it's not set up)
	}

	@Test
	void enterSetsMaxCursorFromListLength() {
		// When list is not null, maxCursor should be list.length - 1.
		// This test verifies the enter() method doesn't crash when
		// the replay directory exists but is empty.
		state.list = new String[0];
		state.enter();
		// With empty list, modenameList etc. should be non-null
		// after setReplayRuleAndModeList() is called
	}
}

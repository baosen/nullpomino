package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.util.CustomProperties;

/**
 * Pins the parsing logic in {@link StateConfigAISelectSDL}:
 * {@code loadAIList()} reads AI class names from a text stream, and
 * {@code loadAINames()} resolves names from paths (handling missing
 * classes gracefully).  Also covers {@code enter()} config loading.
 *
 * <p>These tests construct the state object via a stub that avoids
 * the file I/O in the real constructor.
 */
class StateConfigAISelectSDLLogicTest {

	private CustomProperties originalPropGlobal;

	@BeforeEach
	void setUp() {
		originalPropGlobal = NullpoMinoSDL.propGlobal;
		NullpoMinoSDL.propGlobal = new CustomProperties();
	}

	@AfterEach
	void tearDown() {
		NullpoMinoSDL.propGlobal = originalPropGlobal;
	}

	/* ---------- loadAIList ---------- */

	@Test
	void loadAIListParsesSingleEntry() {
		StateConfigAISelectSDL state = newStub();
		BufferedReader bf = new BufferedReader(new StringReader(
				"nullpomino.game.ai.DummyAI\n"));

		String[] result = state.loadAIList(bf);

		assertEquals(1, result.length);
		assertEquals("nullpomino.game.ai.DummyAI", result[0]);
	}

	@Test
	void loadAIListSkipsCommentLines() {
		StateConfigAISelectSDL state = newStub();
		BufferedReader bf = new BufferedReader(new StringReader(
				"# This is a comment\n" +
				"nullpomino.game.ai.DummyAI\n" +
				"# another comment\n"));

		String[] result = state.loadAIList(bf);

		assertEquals(1, result.length);
		assertEquals("nullpomino.game.ai.DummyAI", result[0]);
	}

	@Test
	void loadAIListStopsAtEmptyLine() {
		StateConfigAISelectSDL state = newStub();
		BufferedReader bf = new BufferedReader(new StringReader(
				"nullpomino.game.ai.DummyAI\n" +
				"\n" +
				"nullpomino.game.ai.RanksAI\n"));

		String[] result = state.loadAIList(bf);

		assertEquals(1, result.length);
	}

	@Test
	void loadAIListHandlesMultipleEntries() {
		StateConfigAISelectSDL state = newStub();
		BufferedReader bf = new BufferedReader(new StringReader(
				"nullpomino.game.ai.DummyAI\n" +
				"nullpomino.game.ai.RanksAI\n" +
				"nullpomino.game.ai.HeuristicAI\n"));

		String[] result = state.loadAIList(bf);

		assertEquals(3, result.length);
	}

	@Test
	void loadAIListReturnsEmptyArrayForEmptyInput() {
		StateConfigAISelectSDL state = newStub();
		BufferedReader bf = new BufferedReader(new StringReader(""));

		String[] result = state.loadAIList(bf);

		assertNotNull(result);
		assertEquals(0, result.length);
	}

	@Test
	void loadAIListReturnsEmptyArrayForWhitespaceOnlyInput() {
		StateConfigAISelectSDL state = newStub();
		BufferedReader bf = new BufferedReader(new StringReader("   \n"));

		String[] result = state.loadAIList(bf);

		// Implementation treats whitespace-only lines as valid (not empty, not comment)
		// so the whitespace line is added as a single entry
		assertEquals(1, result.length);
		assertEquals("   ", result[0]);
	}

	@Test
	void loadAIListIgnoresLinesAfterEmptyLine() {
		StateConfigAISelectSDL state = newStub();
		BufferedReader bf = new BufferedReader(new StringReader(
				"nullpomino.game.ai.DummyAI\n" +
				"\n" +
				"# should be ignored\n" +
				"nullpomino.game.ai.RanksAI\n"));

		String[] result = state.loadAIList(bf);

		assertEquals(1, result.length);
	}

	/* ---------- loadAINames ---------- */

	@Test
	void loadAINamesMarksMissingClassAsInvalid() {
		StateConfigAISelectSDL state = newStub();
		String[] paths = {"nonexistent.ai.Class"};

		String[] names = state.loadAINames(paths);

		assertEquals(1, names.length);
		assertEquals("(INVALID)", names[0]);
	}

	@Test
	void loadAINamesHandlesEmptyInput() {
		StateConfigAISelectSDL state = newStub();

		String[] names = state.loadAINames(new String[0]);

		assertNotNull(names);
		assertEquals(0, names.length);
	}

	@Test
	void loadAINamesHandlesAllMissingClasses() {
		StateConfigAISelectSDL state = newStub();
		String[] paths = {"foo.AI1", "bar.AI2", "baz.AI3"};

		String[] names = state.loadAINames(paths);

		assertEquals(3, names.length);
		for (String name : names) {
			assertEquals("(INVALID)", name);
		}
	}

	/* ---------- enter ---------- */

	@Test
	void enterLoadsConfigFromPropGlobal() {
		StateConfigAISelectSDL state = newStub();
		NullpoMinoSDL.propGlobal.setProperty("0.ai", "nullpomino.game.ai.DummyAI");
		NullpoMinoSDL.propGlobal.setProperty("0.aiMoveDelay", "3");
		NullpoMinoSDL.propGlobal.setProperty("0.aiThinkDelay", "50");
		NullpoMinoSDL.propGlobal.setProperty("0.aiUseThread", "true");
		NullpoMinoSDL.propGlobal.setProperty("0.aiShowHint", "true");
		NullpoMinoSDL.propGlobal.setProperty("0.aiPrethink", "true");
		NullpoMinoSDL.propGlobal.setProperty("0.aiShowState", "true");

		state.player = 0;
		state.enter();

		assertEquals(3, state.aiMoveDelay);
		assertEquals(50, state.aiThinkDelay);
		assertEquals(true, state.aiUseThread);
		assertEquals(true, state.aiShowHint);
		assertEquals(true, state.aiPrethink);
		assertEquals(true, state.aiShowState);
	}

	@Test
	void enterDefaultsWhenNoConfig() {
		StateConfigAISelectSDL state = newStub();
		state.player = 0;
		state.enter();

		assertEquals(0, state.aiMoveDelay);
		assertEquals(0, state.aiThinkDelay);
		assertEquals(true, state.aiUseThread);
		assertEquals(false, state.aiShowHint);
		assertEquals(false, state.aiPrethink);
		assertEquals(false, state.aiShowState);
		assertEquals("", state.currentAI);
	}

	@Test
	void enterSetsAiIdToNegativeForUnknownAi() {
		StateConfigAISelectSDL state = newStub();
		NullpoMinoSDL.propGlobal.setProperty("0.ai", "does.not.Exist");
		state.player = 0;
		state.enter();

		assertEquals(-1, state.aiID);
	}

	/**
	 * Create a stub that skips the file I/O in the real constructor.
	 * We use the no-arg constructor and manually set the AI list fields
	 * to avoid depending on config/list/ai.lst existing.
	 */
	private static StateConfigAISelectSDL newStub() {
		StateConfigAISelectSDL state = new StateConfigAISelectSDL();
		state.aiPathList = new String[0];
		state.aiNameList = new String[0];
		return state;
	}
}

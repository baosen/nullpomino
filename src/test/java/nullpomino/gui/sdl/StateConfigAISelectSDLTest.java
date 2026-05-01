package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link StateConfigAISelectSDL}: the
 * constructor's ai.lst load, the menu list and name parsers, and the
 * enter() routine that reads the player's saved AI selection back into
 * the screen state. The render and update methods need an SDL3 context
 * and are out of scope here.
 */
class StateConfigAISelectSDLTest {

	private CustomProperties originalPropGlobal;

	@BeforeEach
	void snapshotPropGlobal() {
		originalPropGlobal = NullpoMinoSDL.propGlobal;
		NullpoMinoSDL.propGlobal = new CustomProperties();
	}

	@AfterEach
	void restorePropGlobal() {
		NullpoMinoSDL.propGlobal = originalPropGlobal;
	}

	@Test
	void constructorLoadsBundledAIListIntoPathAndNameArrays() {
		StateConfigAISelectSDL state = new StateConfigAISelectSDL();

		// config/list/ai.lst is on the runfile path → constructor populates
		// aiPathList and aiNameList in lockstep.
		assertNotNull(state.aiPathList,
				"the constructor should populate aiPathList from config/list/ai.lst");
		assertNotNull(state.aiNameList);
		assertEquals(state.aiPathList.length, state.aiNameList.length,
				"name array must stay the same length as the path array");
		assertTrue(state.aiPathList.length > 0,
				"the bundled ai.lst ships with at least one AI entry");
	}

	@Test
	void loadAIListSkipsCommentsBreaksOnBlankLineAndKeepsHashFreeEntriesInOrder() {
		StateConfigAISelectSDL state = new StateConfigAISelectSDL();
		String input = String.join("\n",
				"# header comment",
				"foo.bar.AlphaAI",
				"# inline comment",
				"foo.bar.BetaAI",
				"",
				"foo.bar.IgnoredAfterBlank",
				"# trailing comment");

		String[] result = state.loadAIList(new BufferedReader(new StringReader(input)));

		assertEquals(2, result.length,
				"a blank line is the documented end-of-list marker");
		assertEquals("foo.bar.AlphaAI", result[0]);
		assertEquals("foo.bar.BetaAI", result[1]);
	}

	@Test
	void loadAIListReturnsEmptyArrayForBlankInput() {
		StateConfigAISelectSDL state = new StateConfigAISelectSDL();

		String[] result = state.loadAIList(new BufferedReader(new StringReader("")));

		assertEquals(0, result.length);
	}

	@Test
	void loadAINamesUsesGetNameForResolvableClassesAndFallsBackForBrokenOnes() {
		StateConfigAISelectSDL state = new StateConfigAISelectSDL();
		// DummyAI is a real AIPlayer in the test classpath, the second is not.
		String[] paths = {
				"nullpomino.game.ai.DummyAI",
				"nullpomino.does.not.exist.MissingAI"
		};

		String[] names = state.loadAINames(paths);

		assertEquals(2, names.length);
		assertEquals("DummyAI", names[0],
				"resolved AI class must surface its getName() literal");
		assertEquals("(INVALID)", names[1],
				"a missing AI class must surface the (INVALID) sentinel");
	}

	@Test
	void enterReadsBackTheSavedAIClassAndAppliesDocumentedDefaultsForOptions() {
		StateConfigAISelectSDL state = new StateConfigAISelectSDL();
		state.player = 0;

		state.enter();

		// Empty propGlobal → currentAI defaults to "" so aiID becomes -1.
		assertEquals("", state.currentAI);
		assertEquals(-1, state.aiID,
				"unmatched AI class must drop aiID to -1 (the DISABLE sentinel)");
		assertEquals(0, state.aiMoveDelay);
		assertEquals(0, state.aiThinkDelay);
		assertTrue(state.aiUseThread,
				"aiUseThread must default to true to match the legacy behaviour");
	}

	@Test
	void enterMatchesSavedAIPathAgainstAiPathListThroughLegacyClassNamesTranslate() {
		StateConfigAISelectSDL state = new StateConfigAISelectSDL();
		state.player = 0;
		// Save a known AI from the bundled list so enter has a target to
		// match against.
		String knownAI = state.aiPathList[0];
		NullpoMinoSDL.propGlobal.setProperty("0.ai", knownAI);
		NullpoMinoSDL.propGlobal.setProperty("0.aiMoveDelay", 12);
		NullpoMinoSDL.propGlobal.setProperty("0.aiThinkDelay", 250);
		NullpoMinoSDL.propGlobal.setProperty("0.aiUseThread", false);
		NullpoMinoSDL.propGlobal.setProperty("0.aiShowHint", true);
		NullpoMinoSDL.propGlobal.setProperty("0.aiPrethink", true);
		NullpoMinoSDL.propGlobal.setProperty("0.aiShowState", true);

		state.enter();

		assertEquals(knownAI, state.currentAI);
		assertEquals(0, state.aiID,
				"a saved AI that maps to aiPathList[0] must reproduce on aiID");
		assertEquals(12, state.aiMoveDelay);
		assertEquals(250, state.aiThinkDelay);
		assertEquals(false, state.aiUseThread);
		assertEquals(true, state.aiShowHint);
		assertEquals(true, state.aiPrethink);
		assertEquals(true, state.aiShowState);
	}

	@Test
	void enterUsesPerPlayerKeyNamespaceSoTwoPlayersDoNotCollide() {
		StateConfigAISelectSDL p0 = new StateConfigAISelectSDL();
		StateConfigAISelectSDL p1 = new StateConfigAISelectSDL();
		p0.player = 0;
		p1.player = 1;
		String knownAI = p0.aiPathList[0];
		NullpoMinoSDL.propGlobal.setProperty("0.ai", knownAI);
		NullpoMinoSDL.propGlobal.setProperty("0.aiMoveDelay", 5);
		NullpoMinoSDL.propGlobal.setProperty("1.ai", "");
		NullpoMinoSDL.propGlobal.setProperty("1.aiMoveDelay", 99);

		p0.enter();
		p1.enter();

		assertEquals(0, p0.aiID);
		assertEquals(5, p0.aiMoveDelay);
		assertEquals(-1, p1.aiID, "player 1 has no AI saved, so aiID stays -1");
		assertEquals(99, p1.aiMoveDelay);
	}

	@Test
	void maxAiInOnePagePinsTheLegacyTwentyEntryLimit() {
		// Pin the constant so a future refactor that changes the menu
		// page height has to acknowledge the bump.
		assertEquals(20, StateConfigAISelectSDL.MAX_AI_IN_ONE_PAGE);
	}
}

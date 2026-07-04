package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.io.TempDir;

import nullpomino.game.component.Piece;
import nullpomino.game.component.RuleOptions;
import nullpomino.game.ai.DummyAI;
import nullpomino.game.wallkick.StandardWallkick;
import nullpomino.game.randomizer.MemorylessRandomizer;

import org.junit.jupiter.api.Test;

/**
 * Pins the GeneralUtil helpers that are actually called from
 * elsewhere in the codebase. Guards the dead-helper cleanup that
 * removes getCalendarStringDate, getCalendarStringTime, and
 * getNumberOfPiecesCanAppear — all zero-caller per grep.
 */
class GeneralUtilApiTest {

	@TempDir
	Path tempDir;

	@Test
	void getTimeFormatsFramesAsMinutesAndSeconds() {
		assertEquals("00:00.00", GeneralUtil.getTime(0));
		// 60 fps: one second == 60 frames.
		String s = GeneralUtil.getTime(60);
		assertTrue(s.startsWith("00:01"), "expected 00:01... got " + s);
	}

	@Test
	void getONorOFFMapsBooleanToString() {
		assertEquals("ON", GeneralUtil.getONorOFF(true));
		assertEquals("OFF", GeneralUtil.getONorOFF(false));
	}

	@Test
	void getOorXMapsBooleanToGlyph() {
		assertNotNull(GeneralUtil.getOorX(true));
		assertNotNull(GeneralUtil.getOorX(false));
	}

	@Test
	void calendarStringRoundTrip() {
		Calendar fixed = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		fixed.set(2026, Calendar.APRIL, 23, 12, 34, 56);
		fixed.set(Calendar.MILLISECOND, 0);

		String exported = GeneralUtil.exportCalendarString(fixed);
		Calendar imported = GeneralUtil.importCalendarString(exported);
		assertNotNull(imported);
		assertEquals(fixed.getTimeInMillis(), imported.getTimeInMillis());
	}

	@Test
	void getCalendarStringRendersHumanReadable() {
		Calendar fixed = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		fixed.set(2026, Calendar.APRIL, 23, 12, 34, 56);
		String s = GeneralUtil.getCalendarString(fixed, TimeZone.getTimeZone("GMT"));
		assertNotNull(s);
		assertTrue(s.contains("2026"), "expected year in rendered string: " + s);
	}

	@Test
	void isPieceSZOOnlyRecognisesSZO() {
		boolean[] onlyS = new boolean[8];
		onlyS[Piece.PIECE_S] = true;
		onlyS[Piece.PIECE_Z] = true;
		onlyS[Piece.PIECE_O] = true;
		assertTrue(GeneralUtil.isPieceSZOOnly(onlyS));

		onlyS[Piece.PIECE_T] = true;
		assertTrue(!GeneralUtil.isPieceSZOOnly(onlyS));
		assertTrue(!GeneralUtil.isPieceSZOOnly(null));
	}

	@Test
	void createNextPieceArrayParsesSingleDigitsAndDefaultsInvalidCharactersToI() {
		assertArrayEquals(new int[] {
				Piece.PIECE_I,
				Piece.PIECE_L,
				Piece.PIECE_O,
				Piece.PIECE_I,
				9
		}, GeneralUtil.createNextPieceArrayFromNumberString("012x9"));
		assertNull(GeneralUtil.createNextPieceArrayFromNumberString(""));
	}

	@Test
	void stringCombineJoinsFromStartIndex() {
		assertEquals("user for reason", GeneralUtil.StringCombine(
				new String[] {"ban", "user", "for", "reason"}, " ", 1));
		assertEquals("", GeneralUtil.StringCombine(new String[] {"only"}, " ", 1));
	}

	@Test
	void pluginLoadersCreateExpectedTypes() {
		assertTrue(GeneralUtil.loadRandomizer(
				"net.omegaboshi.nullpomino.game.subsystem.randomizer.MemorylessRandomizer")
				instanceof MemorylessRandomizer);
		assertTrue(GeneralUtil.loadWallkick(
				"mu.nu.nullpo.game.subsystem.wallkick.StandardWallkick")
				instanceof StandardWallkick);
		assertTrue(GeneralUtil.loadAIPlayer("nullpomino.game.ai.DummyAI") instanceof DummyAI);
		assertTrue(GeneralUtil.loadAIPlayer("nullpomino.game.subsystem.ai.DummyAI")
				instanceof DummyAI);
		assertTrue(GeneralUtil.loadAIPlayer("mu.nu.nullpo.game.subsystem.ai.DummyAI")
				instanceof DummyAI);
	}

	@Test
	void pluginLoadersReturnNullWhenClassCannotLoad() {
		assertNull(GeneralUtil.loadRandomizer("example.DoesNotExist"));
	}

	@Test
	void getReplayFilenameProducesTimestampedRepFile() {
		String name = GeneralUtil.getReplayFilename();
		assertNotNull(name);
		assertTrue(name.endsWith(".rep"), "expected .rep suffix: " + name);
		assertTrue(name.matches("\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}_\\d{2}\\.rep"),
				"expected yyyy_MM_dd_HH_mm_ss.rep, got " + name);
	}

	@Test
	void getCalendarStringWithoutTimezoneRendersUsingDefaultZone() {
		Calendar fixed = Calendar.getInstance();
		fixed.set(2026, Calendar.APRIL, 23, 12, 34, 56);

		String s = GeneralUtil.getCalendarString(fixed);

		assertNotNull(s);
		assertTrue(s.contains("2026"), "expected year in rendered string: " + s);
	}

	@Test
	void exportCalendarStringNoArgRendersCurrentInstantInGmt() {
		String s = GeneralUtil.exportCalendarString();

		assertNotNull(s);
		assertTrue(s.matches("\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}"),
				"expected yyyy-MM-dd-HH-mm-ss, got " + s);
	}

	@Test
	void importCalendarStringReturnsNullForUnparseableInput() {
		assertNull(GeneralUtil.importCalendarString("not-a-date"));
	}

	@Test
	void loadRuleReturnsDefaultRuleOptionsWhenFileMissing() {
		RuleOptions r = GeneralUtil.loadRule("nope/does/not/exist.rul");

		assertNotNull(r);
		assertEquals("", r.strRuleName);
	}

	@Test
	void loadRuleReadsRuleNameFromExistingFile() throws IOException {
		Path file = tempDir.resolve("test.rul");
		Files.write(file, "0.ruleopt.strRuleName=My Rule\n".getBytes(StandardCharsets.UTF_8));

		RuleOptions r = GeneralUtil.loadRule(file.toString());

		assertEquals("My Rule", r.strRuleName);
	}

	@Test
	void constructorCanBeCalled() {
		// Cover the default constructor (line 23)
		GeneralUtil util = new GeneralUtil();
		assertNotNull(util);
	}

	@Test
	void pieceIdFromDigitHandlesAllDigitPaths() {
		// Test digit '0' (edge of range, value >= '0' and value <= '9')
		assertArrayEquals(new int[] {Piece.PIECE_I},
			GeneralUtil.createNextPieceArrayFromNumberString("0"));

		// Test digit '9' (edge of range)
		int[] result = GeneralUtil.createNextPieceArrayFromNumberString("9");
		assertNotNull(result);
		assertEquals(1, result.length);
	}

	@Test
	void pieceIdFromDigitWithNonDigitReturnsI() {
		// Test with non-digit char that triggers value > '9'
		assertArrayEquals(new int[] {Piece.PIECE_I, Piece.PIECE_I},
			GeneralUtil.createNextPieceArrayFromNumberString("ab"));
	}

	@Test
	void pieceIdFromDigitWithCharBelowZeroReturnsI() {
		// '/' (0x2F) and ' ' are below '0' (0x30), exercising the value < '0' branch
		assertArrayEquals(new int[] {Piece.PIECE_I, Piece.PIECE_I},
			GeneralUtil.createNextPieceArrayFromNumberString("/ "));
	}

	@Test
	void loadRuleReturnsDefaultOnIOException() {
		RuleOptions r = GeneralUtil.loadRule(tempDir.resolve("nonexistent.rul").toString());
		assertNotNull(r);
		assertEquals("", r.strRuleName);
	}

	@Test
	void formatGMatchesJdkPercentGForGameStatRanges() {
		// formatG replaces String.format("%g",...) for the browser build (TeaVM
		// lacks %g). It must produce identical output to the JDK's %g for the
		// value ranges game statistics occupy (fixed-notation range).
		double[] values = {0.0, 6.5, 1.0, 12.3456, 123.456, 0.0123, 1000.0, 99.9, 42.0, 250.75};
		int[] precisions = {4, 5, 6};
		for(int p : precisions) {
			for(double v : values) {
				assertEquals(String.format("%." + p + "g", v), GeneralUtil.formatG(v, p),
						"formatG mismatch for value " + v + " precision " + p);
			}
		}
	}
}

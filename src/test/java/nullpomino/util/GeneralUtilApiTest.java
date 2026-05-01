package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.TimeZone;

import nullpomino.game.component.Piece;
import nullpomino.game.ai.DummyAI;
import nullpomino.game.subsystem.wallkick.StandardWallkick;
import nullpomino.game.randomizer.MemorylessRandomizer;

import org.junit.jupiter.api.Test;

/**
 * Pins the GeneralUtil helpers that are actually called from
 * elsewhere in the codebase. Guards the dead-helper cleanup that
 * removes getCalendarStringDate, getCalendarStringTime, and
 * getNumberOfPiecesCanAppear — all zero-caller per grep.
 */
class GeneralUtilApiTest {

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
}

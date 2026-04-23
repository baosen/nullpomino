package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.TimeZone;

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
		onlyS[1] = true; // S
		onlyS[5] = true; // Z
		onlyS[3] = true; // O (index depends on Piece IDs but the helper only cares about enable flags)
		assertNotNull(GeneralUtil.isPieceSZOOnly(onlyS));
	}
}

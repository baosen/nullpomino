package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Pins the NetServerBan export/import string contract. The persisted
 * format is "addr;banLength;GMT<calendar>" — the only path
 * exportString() writes. Guards the Phase 1d deletion of the
 * ObjectStream-based legacy path (exportStartDate + the non-GMT
 * branch of importStartDate), which has been dead since
 * exportString() was changed to always emit the GMT format.
 */
class NetServerBanRoundTripTest {

	@Test
	void roundTripsAddressBanLengthAndStartDate() {
		NetServerBan original = new NetServerBan("192.0.2.17", NetServerBan.BANLENGTH_1WEEK);
		// freeze start date to a known instant so equality is deterministic
		Calendar fixed = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		fixed.set(2026, Calendar.APRIL, 23, 12, 34, 56);
		fixed.set(Calendar.MILLISECOND, 0);
		original.startDate = fixed;

		String exported = original.exportString();
		assertNotNull(exported);
		assertTrue(exported.contains("GMT"), "exported form must carry the GMT marker: " + exported);

		NetServerBan imported = new NetServerBan();
		imported.importString(exported);

		assertEquals(original.addr, imported.addr);
		assertEquals(original.banLength, imported.banLength);
		assertNotNull(imported.startDate);
		assertEquals(original.startDate.getTimeInMillis(), imported.startDate.getTimeInMillis());
	}

	@Test
	void permanentBanHasNullEndDate() {
		NetServerBan ban = new NetServerBan("10.0.0.1");
		assertEquals(NetServerBan.BANLENGTH_PERMANENT, ban.banLength);
		assertNull(ban.getEndDate());
		assertTrue(!ban.isExpired());
	}

	@Test
	void temporaryBanLengthsMapToExpectedCalendarOffsets() {
		assertEndDateOffset(NetServerBan.BANLENGTH_1HOUR, Calendar.HOUR, 1);
		assertEndDateOffset(NetServerBan.BANLENGTH_6HOURS, Calendar.HOUR, 6);
		assertEndDateOffset(NetServerBan.BANLENGTH_24HOURS, Calendar.HOUR, 24);
		assertEndDateOffset(NetServerBan.BANLENGTH_1WEEK, Calendar.WEEK_OF_MONTH, 1);
		assertEndDateOffset(NetServerBan.BANLENGTH_1MONTH, Calendar.MONTH, 1);
		assertEndDateOffset(NetServerBan.BANLENGTH_1YEAR, Calendar.YEAR, 1);
	}

	private static void assertEndDateOffset(int banLength, int calendarField, int amount) {
		Calendar fixed = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		fixed.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
		fixed.set(Calendar.MILLISECOND, 0);

		NetServerBan ban = new NetServerBan("203.0.113.1", banLength);
		ban.startDate = fixed;

		Calendar expected = (Calendar) fixed.clone();
		expected.add(calendarField, amount);
		assertEquals(expected.getTimeInMillis(), ban.getEndDate().getTimeInMillis());
	}
}

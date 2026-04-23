package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
		assertEquals(null, ban.getEndDate());
		assertTrue(!ban.isExpired());
	}
}

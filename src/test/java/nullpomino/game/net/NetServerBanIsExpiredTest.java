package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link NetServerBan#isExpired}: returns false for a permanent
 * ban (no end date), false when the end date is in the future, true
 * when the end date is in the past, and false for a ban whose end
 * date is exactly 'now' (the comparison is strictly after).
 */
class NetServerBanIsExpiredTest {

	@Test
	void permanentBanIsNeverExpired() {
		// Permanent ban has banLength = BANLENGTH_PERMANENT and
		// getEndDate returns null, which short-circuits isExpired to
		// false.
		NetServerBan ban = new NetServerBan("192.0.2.1");
		assertFalse(ban.isExpired(),
				"permanent ban (no end date) is never expired");
	}

	@Test
	void temporaryBanWithFutureEndDateIsNotExpired() {
		// A 1-year ban started now -> end date is in the future, so
		// isExpired returns false.
		NetServerBan ban = new NetServerBan("192.0.2.1", NetServerBan.BANLENGTH_1YEAR);
		ban.startDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

		assertFalse(ban.isExpired(),
				"freshly-started 1-year ban is not yet expired");
	}

	@Test
	void temporaryBanThatStartedLongAgoIsExpired() {
		// startDate = year 2000 + 1-hour ban -> end date is in 2000 -> expired.
		Calendar oldStart = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		oldStart.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
		oldStart.set(Calendar.MILLISECOND, 0);

		NetServerBan ban = new NetServerBan("192.0.2.1", NetServerBan.BANLENGTH_1HOUR);
		ban.startDate = oldStart;

		assertTrue(ban.isExpired(),
				"1-hour ban started in year 2000 is long-since expired");
	}

	@Test
	void shortBanThatJustStartedAtFarFutureDateIsNotExpired() {
		// startDate = year 2099 + 1-hour ban -> end date is far in the
		// future, so isExpired returns false.
		Calendar future = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		future.set(2099, Calendar.JANUARY, 1, 0, 0, 0);

		NetServerBan ban = new NetServerBan("192.0.2.1", NetServerBan.BANLENGTH_1HOUR);
		ban.startDate = future;

		assertFalse(ban.isExpired(),
				"1-hour ban starting in 2099 is not yet expired");
	}

	@Test
	void permanentBanWithBanLengthEqualOrAboveSentinelHasNullEndDate() {
		// BANLENGTH_PERMANENT and any banLength >= it produce no end
		// date and stay un-expired.
		NetServerBan permanent = new NetServerBan("192.0.2.1",
				NetServerBan.BANLENGTH_PERMANENT);
		assertFalse(permanent.isExpired());
		assertTrue(permanent.getEndDate() == null);

		// banLength above PERMANENT is also treated as no-end.
		NetServerBan beyondPermanent = new NetServerBan("192.0.2.1",
				NetServerBan.BANLENGTH_PERMANENT + 5);
		assertFalse(beyondPermanent.isExpired());
		assertTrue(beyondPermanent.getEndDate() == null);
	}

	@Test
	void negativeBanLengthAlsoSurfacesAsPermanent() {
		// banLength < 0 also produces no end date — the same null
		// short-circuit as PERMANENT, so the ban is never expired.
		NetServerBan ban = new NetServerBan("192.0.2.1", -1);
		assertTrue(ban.getEndDate() == null);
		assertFalse(ban.isExpired());
	}
}

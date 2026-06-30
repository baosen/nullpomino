package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

/**
 * Covers the remaining branches in NetPlayerClient.processPacket /
 * getPlayerInfoByUID:
 * <ul>
 *   <li>L110 welcome {@code (message.length > 6)} ternary true outcome (a
 *       welcome packet carrying a ping-interval field);</li>
 *   <li>L215 changestatus {@code joinseat} else-if true outcome;</li>
 *   <li>L250 getPlayerInfoByUID {@code pInfo.uid == uid} false outcome (the loop
 *       skips a non-matching player before finding/missing the target).</li>
 * </ul>
 *
 * NetPlayerClient is driven without a live socket: send() swallows the
 * resulting NPE, so processPacket completes for parsing-only paths.
 */
class NetPlayerClientBranchCoverageTest {

	private static String playerWire(int uid, String name) {
		NetPlayerInfo p = new NetPlayerInfo();
		p.uid = uid;
		p.strName = name;
		return p.exportString();
	}

	@Test
	void welcomeWithPingIntervalFieldTakesLengthGreaterThanSixBranch() throws IOException {
		NetPlayerClient client = new NetPlayerClient("example.invalid", 5000, "Nullpo");
		try {
			// 7 tab fields → message.length == 7 > 6 → ternary true arm; the
			// interval differs from PING_INTERVAL so a ping task is scheduled.
			client.processPacket(
					"welcome\t1.0\t8\t2\t0\t1.0a\t" + (NetBaseClient.PING_INTERVAL * 3));

			assertEquals(8, client.getPlayerCount());
			assertEquals(2, client.getObserverCount());
			assertNotNull(client.timerPing);
		} finally {
			if (client.timerPing != null) {
				client.timerPing.cancel();
			}
		}
	}

	@Test
	void changeStatusJoinSeatTakesJoinSeatElseIfBranch() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		client.processPacket("playernew\t" + playerWire(11, "Seater"));

		// changestatus\tjoinseat\t[uid]\t[unused]\t[seatID]
		client.processPacket("changestatus\tjoinseat\t11\t\t5");

		NetPlayerInfo p = client.getPlayerInfoByUID(11);
		assertNotNull(p);
		assertEquals(5, p.seatID);
		assertEquals(-1, p.queueID);
	}

	@Test
	void getPlayerInfoByUidSkipsNonMatchingPlayerThenMatches() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		// Two players: the lookup for uid 22 must skip the uid-21 entry first
		// (pInfo.uid == uid false) before matching the uid-22 entry.
		client.processPacket("playerlist\t2\t"
				+ playerWire(21, "First") + "\t" + playerWire(22, "Second"));

		NetPlayerInfo found = client.getPlayerInfoByUID(22);
		assertNotNull(found);
		assertEquals(22, found.uid);

		// A lookup that matches nothing scans both entries (both uid == uid false).
		assertNull(client.getPlayerInfoByUID(999));
	}
}

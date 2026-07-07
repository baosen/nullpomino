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
 *   <li>L215 changestatus {@code joinseat} else-if true outcome;</li>
 *   <li>L250 getPlayerInfoByUID {@code pInfo.uid == uid} false outcome (the loop
 *       skips a non-matching player before finding/missing the target).</li>
 * </ul>
 *
 * NetPlayerClient is driven without a transport: the base send() is a
 * no-op, so processPacket completes for parsing-only paths.
 */
class NetPlayerClientBranchCoverageTest {

	private static String playerWire(int uid, String name) {
		NetPlayerInfo p = new NetPlayerInfo();
		p.uid = uid;
		p.strName = name;
		return p.exportString();
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

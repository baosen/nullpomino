package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NetPlayerInfoRoundTripTest {

	@Test
	void exportImportPreservesPlayerWireFields() {
		NetPlayerInfo original = new NetPlayerInfo();
		original.strName = "Nullpo; Player";
		original.strCountry = "JP";
		original.strHost = "host+name";
		original.strTeam = "Team A";
		original.roomID = 8;
		original.uid = 99;
		original.seatID = 2;
		original.queueID = 1;
		original.ready = true;
		original.playing = true;
		original.connected = true;
		original.isTripUse = true;
		original.rating = new int[] {1000, 1100, 1200, 1300};
		original.playCount = new int[] {10, 20, 30, 40};
		original.winCount = new int[] {1, 2, 3, 4};
		original.playCountNow = 5;
		original.winCountNow = 6;

		NetPlayerInfo imported = new NetPlayerInfo(original.exportString());

		assertEquals(original.strName, imported.strName);
		assertEquals(original.strCountry, imported.strCountry);
		assertEquals(original.strHost, imported.strHost);
		assertEquals(original.strTeam, imported.strTeam);
		assertEquals(original.roomID, imported.roomID);
		assertEquals(original.uid, imported.uid);
		assertEquals(original.seatID, imported.seatID);
		assertEquals(original.queueID, imported.queueID);
		assertEquals(original.ready, imported.ready);
		assertEquals(original.playing, imported.playing);
		assertEquals(original.connected, imported.connected);
		assertEquals(original.isTripUse, imported.isTripUse);
		assertArrayEquals(original.rating, imported.rating);
		assertArrayEquals(original.playCount, imported.playCount);
		assertArrayEquals(original.winCount, imported.winCount);
		assertEquals(original.playCountNow, imported.playCountNow);
		assertEquals(original.winCountNow, imported.winCountNow);
		assertEquals(original.spPersonalBest.exportString(), imported.spPersonalBest.exportString());
	}
}

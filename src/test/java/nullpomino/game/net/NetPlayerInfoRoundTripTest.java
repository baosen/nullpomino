package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.RuleOptions;

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
		assertArrayEquals(original.rating, imported.rating);
		assertArrayEquals(original.playCount, imported.playCount);
		assertArrayEquals(original.winCount, imported.winCount);
		assertEquals(original.playCountNow, imported.playCountNow);
		assertEquals(original.winCountNow, imported.winCountNow);
		assertEquals(original.spPersonalBest.exportString(), imported.spPersonalBest.exportString());
	}

	@Test
	void exportStringArrayKeepsWireFieldCount() {
		// 26 fields since the tripcode flag was removed from the blob
		assertEquals(26, new NetPlayerInfo().exportStringArray().length);
	}

	@Test
	void importStringArrayAcceptsLegacyDataWithoutCurrentRoomCounts() {
		NetPlayerInfo original = new NetPlayerInfo();
		original.playCountNow = 5;
		original.winCountNow = 6;
		// The full blob is 26 fields; drop the trailing playCountNow/winCountNow
		String[] legacyFields = Arrays.copyOf(original.exportStringArray(), 24);

		NetPlayerInfo imported = new NetPlayerInfo();
		imported.playCountNow = 7;
		imported.winCountNow = 8;
		imported.importStringArray(legacyFields);

		assertEquals(7, imported.playCountNow);
		assertEquals(8, imported.winCountNow);
	}

	@Test
	void stringArrayConstructorImportsAllPlayerWireFields() {
		NetPlayerInfo source = new NetPlayerInfo();
		source.strName = "Player";
		source.uid = 7;

		NetPlayerInfo imported = new NetPlayerInfo(source.exportStringArray());

		assertEquals("Player", imported.strName);
		assertEquals(7, imported.uid);
	}

	@Test
	void copyConstructorClonesRuleOptionsWithoutSharing() {
		NetPlayerInfo source = new NetPlayerInfo();
		source.ruleOpt = new RuleOptions();
		source.ruleOpt.strRuleName = "Original";

		NetPlayerInfo copy = new NetPlayerInfo(source);

		assertNotSame(source.ruleOpt, copy.ruleOpt);
		assertEquals("Original", copy.ruleOpt.strRuleName);
		source.ruleOpt.strRuleName = "Mutated";
		assertEquals("Original", copy.ruleOpt.strRuleName);
	}

	@Test
	void resetPlayStateClearsReadyAndPlayingFlags() {
		NetPlayerInfo player = new NetPlayerInfo();
		player.ready = true;
		player.playing = true;

		player.resetPlayState();

		assertFalse(player.ready);
		assertFalse(player.playing);
	}

	@Test
	void deleteClearsRuleOptionReferenceWithoutTouchingPrimitives() {
		NetPlayerInfo player = new NetPlayerInfo();
		player.ruleOpt = new RuleOptions();
		player.uid = 5;

		player.delete();

		assertNull(player.ruleOpt);
		assertEquals(5, player.uid);
	}

	@Test
	void copyConstructorCopiesArraysWithoutSharing() {
		NetPlayerInfo original = new NetPlayerInfo();
		original.rating = new int[] {1000, 1100, 1200, 1300};
		original.ratingBefore = new int[] {900, 1000, 1100, 1200};
		original.playCount = new int[] {10, 20, 30, 40};
		original.winCount = new int[] {1, 2, 3, 4};

		NetPlayerInfo copy = new NetPlayerInfo(original);
		original.rating[0] = 1;
		original.ratingBefore[0] = 2;
		original.playCount[0] = 3;
		original.winCount[0] = 4;

		assertArrayEquals(new int[] {1000, 1100, 1200, 1300}, copy.rating);
		assertArrayEquals(new int[] {900, 1000, 1100, 1200}, copy.ratingBefore);
		assertArrayEquals(new int[] {10, 20, 30, 40}, copy.playCount);
		assertArrayEquals(new int[] {1, 2, 3, 4}, copy.winCount);
	}
}

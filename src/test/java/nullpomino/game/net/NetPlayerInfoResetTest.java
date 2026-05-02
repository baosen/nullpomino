package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.play.GameEngine;

class NetPlayerInfoResetTest {

	@Test
	void resetPlayStateSetsReadyAndPlayingToFalseWhenTheyWereTrue() {
		NetPlayerInfo player = new NetPlayerInfo();
		player.ready = true;
		player.playing = true;

		player.resetPlayState();

		assertFalse(player.ready);
		assertFalse(player.playing);
	}

	@Test
	void resetPlayStateLeavesReadyAndPlayingFalseWhenTheyAlreadyWereFalse() {
		NetPlayerInfo player = new NetPlayerInfo();
		player.ready = false;
		player.playing = false;

		player.resetPlayState();

		assertFalse(player.ready);
		assertFalse(player.playing);
	}

	@Test
	void resetPlayStateDoesNotAffectOtherFields() {
		NetPlayerInfo player = new NetPlayerInfo();
		player.strName = "Test";
		player.uid = 42;
		player.connected = true;
		player.ready = true;
		player.playing = true;

		player.resetPlayState();

		assertEquals("Test", player.strName);
		assertEquals(42, player.uid);
		assertTrue(player.connected);
	}

	@Test
	void copyMethodCopiesAllFieldsIndependently() {
		NetPlayerInfo source = new NetPlayerInfo();
		source.strName = "Source";
		source.strCountry = "US";
		source.strHost = "host";
		source.strTeam = "Team S";
		source.ruleOpt = new RuleOptions();
		source.ruleOpt.strRuleName = "SourceRule";
		source.rating = new int[] {1000, 1100, 1200, 1300};
		source.ratingBefore = new int[] {900, 1000, 1100, 1200};
		source.playCount = new int[] {10, 20, 30, 40};
		source.winCount = new int[] {1, 2, 3, 4};
		source.playCountNow = 5;
		source.winCountNow = 6;
		source.uid = 99;
		source.roomID = 8;
		source.seatID = 2;
		source.queueID = 1;
		source.ready = true;
		source.playing = true;
		source.connected = true;
		source.isTripUse = false;
		source.strRealHost = "realHost";
		source.strRealIP = "1.2.3.4";

		NetPlayerInfo dest = new NetPlayerInfo();
		dest.copy(source);

		// Verify all scalar fields
		assertEquals("Source", dest.strName);
		assertEquals("US", dest.strCountry);
		assertEquals("host", dest.strHost);
		assertEquals("Team S", dest.strTeam);
		assertEquals("SourceRule", dest.ruleOpt.strRuleName);
		assertNotSame(source.ruleOpt, dest.ruleOpt);
		assertEquals(5, dest.playCountNow);
		assertEquals(6, dest.winCountNow);
		assertEquals(99, dest.uid);
		assertEquals(8, dest.roomID);
		assertEquals(2, dest.seatID);
		assertEquals(1, dest.queueID);
		assertTrue(dest.ready);
		assertTrue(dest.playing);
		assertTrue(dest.connected);
		assertFalse(dest.isTripUse);
		assertEquals("realHost", dest.strRealHost);
		assertEquals("1.2.3.4", dest.strRealIP);

		// Verify arrays are independent copies
		assertArrayEquals(new int[] {1000, 1100, 1200, 1300}, dest.rating);
		assertArrayEquals(new int[] {900, 1000, 1100, 1200}, dest.ratingBefore);
		assertArrayEquals(new int[] {10, 20, 30, 40}, dest.playCount);
		assertArrayEquals(new int[] {1, 2, 3, 4}, dest.winCount);

		// Mutate source arrays to confirm independence
		source.rating[0] = 999;
		source.ratingBefore[0] = 888;
		source.playCount[0] = 777;
		source.winCount[0] = 666;

		assertArrayEquals(new int[] {1000, 1100, 1200, 1300}, dest.rating);
		assertArrayEquals(new int[] {900, 1000, 1100, 1200}, dest.ratingBefore);
		assertArrayEquals(new int[] {10, 20, 30, 40}, dest.playCount);
		assertArrayEquals(new int[] {1, 2, 3, 4}, dest.winCount);
	}

	@Test
	void copyWithNullRuleOptLeavesDestRuleOptNull() {
		NetPlayerInfo source = new NetPlayerInfo();
		source.ruleOpt = null;

		NetPlayerInfo dest = new NetPlayerInfo();
		dest.ruleOpt = new RuleOptions();
		dest.copy(source);

		assertNull(dest.ruleOpt);
	}
}

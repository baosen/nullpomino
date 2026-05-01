package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;

import nullpomino.game.component.Statistics;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

class NetSPRankingTest {

	@Test
	void removeRecordRemovesDuplicatePlayersWithoutIndexShift() {
		NetSPRanking ranking = rankingWithMax(10);
		NetSPRecord alice1 = record("alice", 100);
		NetSPRecord bob = record("bob", 90);
		NetSPRecord alice2 = record("alice", 80);
		ranking.listRecord.add(alice1);
		ranking.listRecord.add(bob);
		ranking.listRecord.add(alice2);

		assertEquals(2, ranking.removeRecord("alice"));

		assertEquals(1, ranking.listRecord.size());
		assertSame(bob, ranking.listRecord.get(0));
	}

	@Test
	void registerRecordKeepsExactlyMaxRecordsAndRejectsLowerOverflow() {
		NetSPRanking ranking = rankingWithMax(2);
		NetSPRecord alice = record("alice", 100);
		NetSPRecord bob = record("bob", 90);
		NetSPRecord carol = record("carol", 80);

		assertEquals(0, ranking.registerRecord(alice));
		assertEquals(1, ranking.registerRecord(bob));
		assertEquals(-1, ranking.registerRecord(carol));

		assertEquals(2, ranking.listRecord.size());
		assertSame(alice, ranking.listRecord.get(0));
		assertSame(bob, ranking.listRecord.get(1));
	}

	@Test
	void registerRecordAllowsUnlimitedRankings() {
		NetSPRanking ranking = rankingWithMax(-1);

		assertEquals(0, ranking.registerRecord(record("alice", 100)));
		assertEquals(1, ranking.registerRecord(record("bob", 90)));
		assertEquals(2, ranking.registerRecord(record("carol", 80)));

		assertEquals(3, ranking.listRecord.size());
	}

	@Test
	void betterReplacementKeepsRankingFullAndReturnsInsertedRank() {
		NetSPRanking ranking = rankingWithMax(2);
		NetSPRecord alice = record("alice", 100);
		NetSPRecord bob = record("bob", 90);
		NetSPRecord dana = record("dana", 110);
		ranking.registerRecord(alice);
		ranking.registerRecord(bob);

		assertEquals(0, ranking.registerRecord(dana));

		assertEquals(2, ranking.listRecord.size());
		assertSame(dana, ranking.listRecord.get(0));
		assertSame(alice, ranking.listRecord.get(1));
	}

	@Test
	void propertyRoundTripUsesLegacyKeysAndHonorsMaxRecords() {
		NetSPRanking original = rankingWithMax(10);
		NetSPRecord alice = record("alice", 100);
		NetSPRecord bob = record("bob", 90);
		original.listRecord.add(alice);
		original.listRecord.add(bob);
		CustomProperties props = new CustomProperties();

		original.writeProperty(props);
		assertEquals(2, props.getProperty("spranking.rule.mode.0.numRecords", -1));

		NetSPRanking imported = rankingWithMax(1);
		imported.readProperty(props);

		assertEquals(1, imported.listRecord.size());
		assertEquals("alice", imported.listRecord.get(0).strPlayerName);
		assertEquals(100, imported.listRecord.get(0).stats.score);
	}

	@Test
	void defaultConstructorAppliesResetValues() {
		NetSPRanking r = new NetSPRanking();

		assertEquals("", r.strModeName);
		assertEquals("", r.strRuleName);
		assertEquals(0, r.gameType);
		assertEquals(0, r.style);
		assertEquals(0, r.rankingType);
		assertEquals(100, r.maxRecords);
		assertEquals(0, r.listRecord.size());
	}

	@Test
	void copyConstructorAndCopyMethodCloneRecordListIndependently() {
		NetSPRanking source = rankingWithMax(10);
		source.listRecord.add(record("alice", 100));

		NetSPRanking ctorCopy = new NetSPRanking(source);
		assertNotSame(source.listRecord, ctorCopy.listRecord);
		assertEquals(1, ctorCopy.listRecord.size());
		source.listRecord.get(0).stats.score = 9999;
		assertEquals(100, ctorCopy.listRecord.get(0).stats.score);

		NetSPRanking copyMethod = new NetSPRanking();
		copyMethod.copy(source);
		assertNotSame(source.listRecord, copyMethod.listRecord);
		assertEquals(source.strModeName, copyMethod.strModeName);
		assertEquals(source.maxRecords, copyMethod.maxRecords);
	}

	@Test
	void getRecordAndIndexOfFindByNameOrPlayerInfo() {
		NetSPRanking ranking = rankingWithMax(10);
		ranking.listRecord.add(record("alice", 100));
		ranking.listRecord.add(record("bob", 90));

		assertEquals(1, ranking.indexOf("bob"));
		assertEquals(-1, ranking.indexOf("dana"));
		assertEquals("alice", ranking.getRecord("alice").strPlayerName);
		assertNull(ranking.getRecord("dana"));

		NetPlayerInfo player = new NetPlayerInfo();
		player.strName = "bob";
		assertEquals(1, ranking.indexOf(player));
		assertEquals("bob", ranking.getRecord(player).strPlayerName);
	}

	@Test
	void removeRecordByPlayerInfoDelegatesToNameLookup() {
		NetSPRanking ranking = rankingWithMax(10);
		ranking.listRecord.add(record("alice", 100));
		ranking.listRecord.add(record("bob", 90));
		NetPlayerInfo player = new NetPlayerInfo();
		player.strName = "alice";

		assertEquals(1, ranking.removeRecord(player));
		assertEquals(1, ranking.listRecord.size());
		assertSame("bob", ranking.listRecord.get(0).strPlayerName);
	}

	@Test
	void isNewRecordReturnsTrueOnFirstSubmissionOrBetter() {
		NetSPRanking ranking = rankingWithMax(10);
		NetSPRecord alice = record("alice", 100);

		assertTrue(ranking.isNewRecord(alice));
		ranking.listRecord.add(alice);

		NetSPRecord better = record("alice", 200);
		assertTrue(ranking.isNewRecord(better));

		NetSPRecord worse = record("alice", 50);
		assertFalse(ranking.isNewRecord(worse));
	}

	@Test
	void mergeRankingsCombinesRecordsAcrossSourceRankings() {
		NetSPRanking r1 = rankingWithMax(10);
		r1.listRecord.add(record("alice", 100));
		r1.listRecord.add(record("bob", 90));
		NetSPRanking r2 = rankingWithMax(10);
		r2.listRecord.add(record("carol", 80));
		LinkedList<NetSPRanking> rankings = new LinkedList<>();
		rankings.add(r1);
		rankings.add(r2);

		NetSPRanking merged = NetSPRanking.mergeRankings(rankings);

		assertEquals(3, merged.listRecord.size());
		assertNotNullElement(merged, "alice");
		assertNotNullElement(merged, "bob");
		assertNotNullElement(merged, "carol");
	}

	@Test
	void mergeRankingsReturnsNullForNullOrEmptyInput() {
		assertNull(NetSPRanking.mergeRankings(null));
		assertNull(NetSPRanking.mergeRankings(new LinkedList<>()));
	}

	private static void assertNotNullElement(NetSPRanking ranking, String name) {
		assertSame(name, ranking.getRecord(name).strPlayerName);
	}

	private static NetSPRanking rankingWithMax(int maxRecords) {
		return new NetSPRanking("mode", "rule", 0, 0,
				NetSPRecord.RANKINGTYPE_GENERIC_SCORE, maxRecords);
	}

	private static NetSPRecord record(String playerName, int score) {
		NetSPRecord record = new NetSPRecord();
		record.strPlayerName = playerName;
		record.strModeName = "mode";
		record.strRuleName = "rule";
		record.stats = new Statistics();
		record.stats.score = score;
		return record;
	}
}

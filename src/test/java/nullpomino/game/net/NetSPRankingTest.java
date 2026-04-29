package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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

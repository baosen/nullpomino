package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link NetSPRanking#writeProperty} /
 * {@link NetSPRanking#readProperty} edge cases that the
 * round-trip-the-list test in {@code NetSPRankingTest} doesn't reach:
 * the empty-list case (numRecords=0 written) and the
 * unset-maxRecords-during-read case (every record imported regardless
 * of count).
 *
 * <p>The propertyKey shape is
 * {@code spranking.<rule>.<mode>.<gameType>.}, so different
 * (rule, mode, gameType) tuples land in disjoint key namespaces.
 */
class NetSPRankingPropertyEdgeCasesTest {

	@Test
	void writePropertyOnEmptyListWritesZeroNumRecords() {
		NetSPRanking ranking = new NetSPRanking("MARATHON", "Standard",
				0, 0, NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 10);
		CustomProperties props = new CustomProperties();

		ranking.writeProperty(props);

		assertEquals(0, props.getProperty(
				"spranking.Standard.MARATHON.0.numRecords", -1),
				"empty list -> numRecords=0 under the rule.mode.gametype prefix");
	}

	@Test
	void writePropertyKeyShapeIncludesRuleModeAndGameType() {
		// Pin the byte shape: two rankings with the same gameType but
		// different rule/mode names land in distinct keyspaces.
		NetSPRanking r1 = new NetSPRanking("MARATHON", "Standard",
				5, 0, NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 10);
		NetSPRanking r2 = new NetSPRanking("ULTRA", "Classic",
				5, 0, NetSPRecord.RANKINGTYPE_ULTRA, 10);

		CustomProperties props = new CustomProperties();
		r1.writeProperty(props);
		r2.writeProperty(props);

		assertEquals(0, props.getProperty(
				"spranking.Standard.MARATHON.5.numRecords", -1),
				"r1 lands at spranking.Standard.MARATHON.5.*");
		assertEquals(0, props.getProperty(
				"spranking.Classic.ULTRA.5.numRecords", -1),
				"r2 lands at spranking.Classic.ULTRA.5.*");
	}

	@Test
	void readPropertyOnAbsentKeyspaceLeavesEmptyList() {
		// CustomProperties has no keys -> readProperty's
		// numRecords=0 default leaves listRecord empty.
		NetSPRanking ranking = new NetSPRanking("MARATHON", "Standard",
				0, 0, NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 10);
		// Spoil the list so we can confirm clear-and-load.
		ranking.listRecord.add(new NetSPRecord());

		ranking.readProperty(new CustomProperties());

		assertEquals(0, ranking.listRecord.size(),
				"empty property store -> empty list");
	}

	@Test
	void readPropertyHonorsMaxRecordsClampWhenStoredCountExceedsIt() {
		// Write 5 records under maxRecords=10, then load with
		// maxRecords=2 -> only the first two records are imported.
		NetSPRanking source = new NetSPRanking("MARATHON", "Standard",
				0, 0, NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 10);
		for(int i = 0; i < 5; i++) {
			NetSPRecord r = new NetSPRecord();
			r.strPlayerName = "p" + i;
			source.listRecord.add(r);
		}

		CustomProperties props = new CustomProperties();
		source.writeProperty(props);

		NetSPRanking dest = new NetSPRanking("MARATHON", "Standard",
				0, 0, NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 2);
		dest.readProperty(props);

		assertEquals(2, dest.listRecord.size(),
				"maxRecords=2 clamps the load to the first two records");
		assertEquals("p0", dest.listRecord.get(0).strPlayerName);
		assertEquals("p1", dest.listRecord.get(1).strPlayerName);
	}

	@Test
	void readPropertyImportsAllRecordsWhenMaxRecordsIsUnlimited() {
		// maxRecords=-1 means "unlimited"; the load reads every
		// record regardless of count.
		NetSPRanking source = new NetSPRanking("MARATHON", "Standard",
				0, 0, NetSPRecord.RANKINGTYPE_GENERIC_SCORE, -1);
		for(int i = 0; i < 5; i++) {
			NetSPRecord r = new NetSPRecord();
			r.strPlayerName = "p" + i;
			source.listRecord.add(r);
		}

		CustomProperties props = new CustomProperties();
		source.writeProperty(props);

		NetSPRanking dest = new NetSPRanking("MARATHON", "Standard",
				0, 0, NetSPRecord.RANKINGTYPE_GENERIC_SCORE, -1);
		dest.readProperty(props);

		assertEquals(5, dest.listRecord.size(),
				"maxRecords=-1 imports every record");
	}

	@Test
	void writePropertyAndReadPropertyRoundTripPreservesPlayerNames() {
		NetSPRanking source = new NetSPRanking("MARATHON", "Standard",
				0, 0, NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 10);
		String[] names = {"alice", "bob", "carol"};
		for(String n : names) {
			NetSPRecord r = new NetSPRecord();
			r.strPlayerName = n;
			source.listRecord.add(r);
		}

		CustomProperties props = new CustomProperties();
		source.writeProperty(props);

		NetSPRanking dest = new NetSPRanking("MARATHON", "Standard",
				0, 0, NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 10);
		dest.readProperty(props);

		assertEquals(3, dest.listRecord.size());
		for(int i = 0; i < names.length; i++) {
			assertEquals(names[i], dest.listRecord.get(i).strPlayerName);
		}
	}
}

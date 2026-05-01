package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Statistics;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

class NetSPPersonalBestTest {

	@Test
	void importsEmptyRecordListWithoutNullingList() {
		NetSPPersonalBest imported = new NetSPPersonalBest("Player;");

		assertEquals("Player", imported.strPlayerName);
		assertNotNull(imported.listRecord);
		assertEquals(0, imported.listRecord.size());
	}

	@Test
	void exportImportPreservesCompressedRecords() {
		NetSPPersonalBest original = new NetSPPersonalBest();
		original.strPlayerName = "Player; Name";
		NetSPRecord record = new NetSPRecord();
		record.strPlayerName = original.strPlayerName;
		record.strRuleName = "Rule";
		record.strModeName = "Mode";
		record.gameType = 2;
		record.strTimeStamp = "2026-04-26";
		record.listCustomStats.add("score;1000");
		original.listRecord.add(record);

		NetSPPersonalBest imported = new NetSPPersonalBest(original.exportString());
		NetSPRecord importedRecord = imported.getRecord("Rule", "Mode", 2);

		assertEquals(original.strPlayerName, imported.strPlayerName);
		assertEquals(1, imported.listRecord.size());
		assertNotNull(importedRecord);
		assertEquals("1000", importedRecord.getCustomStat("score"));
		assertNull(imported.getRecord("missing", "Mode", 2));
	}

	@Test
	void propertyRoundTripUsesLegacyKeys() {
		NetSPPersonalBest original = new NetSPPersonalBest();
		original.strPlayerName = "Player";
		NetSPRecord record = record("Rule", "Mode", 2);
		original.listRecord.add(record);
		CustomProperties props = new CustomProperties();

		original.writeProperty(props);
		assertEquals(1, props.getProperty("sppersonal.Player.numRecords", -1));

		NetSPPersonalBest imported = new NetSPPersonalBest();
		imported.strPlayerName = "Player";
		imported.readProperty(props);

		assertEquals(1, imported.listRecord.size());
		assertNotNull(imported.getRecord("Rule", "Mode", 2));
		assertEquals("2026-04-26", imported.getRecord("Rule", "Mode", 2).strTimeStamp);
	}

	@Test
	void stringArrayConstructorPopulatesNameAndRecords() {
		NetSPRecord seed = record("Rule", "Mode", 2);
		seed.stats = new Statistics();
		seed.stats.score = 100;
		NetSPPersonalBest source = new NetSPPersonalBest();
		source.strPlayerName = "Player";
		source.listRecord.add(seed);

		NetSPPersonalBest imported = new NetSPPersonalBest(source.exportStringArray());

		assertEquals("Player", imported.strPlayerName);
		assertEquals(1, imported.listRecord.size());
	}

	@Test
	void importStringArrayWithSinglePlayerNameElementClearsList() {
		NetSPPersonalBest pb = new NetSPPersonalBest();
		pb.listRecord.add(record("Rule", "Mode", 0));
		pb.strPlayerName = "Stale";

		pb.importStringArray(new String[] { "Fresh" });

		assertEquals("Fresh", pb.strPlayerName);
		assertEquals(0, pb.listRecord.size());
	}

	@Test
	void copyConstructorClonesRecordListIndependently() {
		NetSPPersonalBest source = new NetSPPersonalBest();
		source.strPlayerName = "alice";
		NetSPRecord r = record("Rule", "Mode", 0);
		r.stats = new Statistics();
		r.stats.score = 100;
		source.listRecord.add(r);

		NetSPPersonalBest copy = new NetSPPersonalBest(source);

		assertEquals("alice", copy.strPlayerName);
		assertNotSame(source.listRecord, copy.listRecord);
		source.listRecord.get(0).stats.score = 9999;
		assertEquals(100, copy.listRecord.get(0).stats.score);
	}

	@Test
	void isNewRecordReturnsTrueWhenNoPriorAndCompareDecidesOtherwise() {
		NetSPPersonalBest pb = new NetSPPersonalBest();
		NetSPRecord first = scored("Rule", "Mode", 100);
		assertTrue(pb.isNewRecord(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, first));

		pb.listRecord.add(first);
		NetSPRecord better = scored("Rule", "Mode", 200);
		assertTrue(pb.isNewRecord(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, better));

		NetSPRecord worse = scored("Rule", "Mode", 50);
		assertFalse(pb.isNewRecord(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, worse));
	}

	@Test
	void registerRecordAddsAppendsAndReplacesAccordingToCompare() {
		NetSPPersonalBest pb = new NetSPPersonalBest();

		NetSPRecord first = scored("Rule", "Mode", 100);
		assertTrue(pb.registerRecord(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, first));
		assertEquals(1, pb.listRecord.size());

		NetSPRecord worse = scored("Rule", "Mode", 50);
		assertFalse(pb.registerRecord(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, worse));
		assertEquals(100, pb.listRecord.get(0).stats.score);

		NetSPRecord better = scored("Rule", "Mode", 200);
		assertTrue(pb.registerRecord(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, better));
		assertEquals(200, pb.listRecord.get(0).stats.score);

		NetSPRecord differentRule = scored("Other", "Mode", 100);
		assertTrue(pb.registerRecord(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, differentRule));
		assertEquals(2, pb.listRecord.size());
	}

	private static NetSPRecord scored(String rule, String mode, int score) {
		NetSPRecord r = record(rule, mode, 0);
		r.stats = new Statistics();
		r.stats.score = score;
		return r;
	}

	private static NetSPRecord record(String rule, String mode, int gameType) {
		NetSPRecord record = new NetSPRecord();
		record.strRuleName = rule;
		record.strModeName = mode;
		record.gameType = gameType;
		record.strTimeStamp = "2026-04-26";
		return record;
	}
}

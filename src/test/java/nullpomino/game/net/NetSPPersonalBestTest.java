package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

	private static NetSPRecord record(String rule, String mode, int gameType) {
		NetSPRecord record = new NetSPRecord();
		record.strRuleName = rule;
		record.strModeName = mode;
		record.gameType = gameType;
		record.strTimeStamp = "2026-04-26";
		return record;
	}
}

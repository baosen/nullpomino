package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}

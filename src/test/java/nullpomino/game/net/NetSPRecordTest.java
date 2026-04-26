package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class NetSPRecordTest {

	@Test
	void customStatsExportImportAndLookupRoundTrip() {
		NetSPRecord record = new NetSPRecord();
		record.listCustomStats.add("combo;12");
		record.listCustomStats.add("note;value;with;semicolons");

		NetSPRecord imported = new NetSPRecord();
		imported.importCustomStats(record.exportCustomStats());

		assertEquals("12", imported.getCustomStat("combo"));
		assertEquals("value;with;semicolons", imported.getCustomStat("note"));
		assertEquals("fallback", imported.getCustomStat("missing", "fallback"));
		assertNull(imported.getCustomStat("malformed"));
	}

	@Test
	void exportImportPreservesRecordFieldsWithEmptyTimestamp() {
		NetSPRecord original = new NetSPRecord();
		original.strPlayerName = "Player; Name";
		original.strModeName = "Mode+Name";
		original.strRuleName = "Rule Name";
		original.strReplayProp = "replay";
		original.gameType = 2;
		original.style = 1;
		original.strTimeStamp = "";
		original.listCustomStats.add("score;1000");

		NetSPRecord imported = new NetSPRecord(original.exportString());

		assertEquals(original.strPlayerName, imported.strPlayerName);
		assertEquals(original.strModeName, imported.strModeName);
		assertEquals(original.strRuleName, imported.strRuleName);
		assertNull(imported.stats);
		assertEquals("1000", imported.getCustomStat("score"));
		assertEquals(original.strReplayProp, imported.strReplayProp);
		assertEquals(original.gameType, imported.gameType);
		assertEquals(original.style, imported.style);
		assertEquals("", imported.strTimeStamp);
	}
}

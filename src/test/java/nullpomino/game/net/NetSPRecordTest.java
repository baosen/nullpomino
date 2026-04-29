package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

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

	@Test
	void exportStringArrayKeepsLegacyFieldCount() {
		assertEquals(9, new NetSPRecord().exportStringArray().length);
	}

	@Test
	void importStringArrayAcceptsLegacyDataWithoutTimestamp() {
		NetSPRecord original = new NetSPRecord();
		original.strTimeStamp = "2026-04-29";
		String[] legacyFields = Arrays.copyOf(original.exportStringArray(), 8);

		NetSPRecord imported = new NetSPRecord();
		imported.strTimeStamp = "old";
		imported.importStringArray(legacyFields);

		assertEquals("", imported.strTimeStamp);
	}

	@Test
	void statRowsMatchRankingTypeColumns() {
		NetSPRecord record = new NetSPRecord();
		record.stats = new nullpomino.game.component.Statistics();
		record.stats.score = 100;
		record.stats.lines = 20;
		record.stats.time = 300;
		record.stats.totalPieceLocked = 40;
		record.stats.pps = 1.5f;
		record.stats.spl = 2.25;
		record.stats.maxCombo = 12;
		record.stats.rollclear = 1;

		assertEquals("100,20,300", record.getStatRow(NetSPRecord.RANKINGTYPE_GENERIC_SCORE));
		assertEquals("300,40,1.5", record.getStatRow(NetSPRecord.RANKINGTYPE_GENERIC_TIME));
		assertEquals("300,20,2.25", record.getStatRow(NetSPRecord.RANKINGTYPE_SCORERACE));
		assertEquals("300,20,40", record.getStatRow(NetSPRecord.RANKINGTYPE_DIGRACE));
		assertEquals("100,20,40", record.getStatRow(NetSPRecord.RANKINGTYPE_ULTRA));
		assertEquals("12,300,1.5", record.getStatRow(NetSPRecord.RANKINGTYPE_COMBORACE));
		assertEquals("100,20,300", record.getStatRow(NetSPRecord.RANKINGTYPE_DIGCHALLENGE));
		assertEquals("20,300,1.5,1", record.getStatRow(NetSPRecord.RANKINGTYPE_TIMEATTACK));
		assertEquals("", record.getStatRow(999));
	}
}

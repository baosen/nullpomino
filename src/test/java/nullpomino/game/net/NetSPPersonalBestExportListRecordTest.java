package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.LinkedList;

import nullpomino.game.component.Statistics;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link NetSPPersonalBest#exportListRecord} /
 * {@link NetSPPersonalBest#importListRecord}: the compressed-list
 * codec used to ship a player's record set across the netplay wire,
 * delegating to {@link NetSPRecordList#exportCompressedList} /
 * {@link NetSPRecordList#importCompressedList}.
 */
class NetSPPersonalBestExportListRecordTest {

	@Test
	void exportListRecordOnEmptyListReturnsEmptyString() {
		NetSPPersonalBest pb = new NetSPPersonalBest();

		assertEquals("", pb.exportListRecord(),
				"empty record list -> empty exported string");
	}

	@Test
	void exportImportRoundTripsRecordsThroughCompressedString() {
		NetSPPersonalBest source = new NetSPPersonalBest();
		source.strPlayerName = "Player";
		source.listRecord.add(buildRecord("MARATHON", "Standard", 100, 5));
		source.listRecord.add(buildRecord("LINE RACE", "Classic", 200, 10));

		String exported = source.exportListRecord();
		assertNotNull(exported);
		assertEquals(2, exported.split(";", -1).length,
				"two records -> two semicolon-separated entries");

		NetSPPersonalBest dest = new NetSPPersonalBest();
		dest.importListRecord(exported);

		assertEquals(2, dest.listRecord.size());
		assertEquals("MARATHON", dest.listRecord.get(0).strModeName);
		assertEquals("LINE RACE", dest.listRecord.get(1).strModeName);
		assertEquals(100, dest.listRecord.get(0).stats.score);
		assertEquals(200, dest.listRecord.get(1).stats.score);
	}

	@Test
	void importListRecordOnNullSourceClearsTheList() {
		NetSPPersonalBest pb = new NetSPPersonalBest();
		pb.listRecord.add(buildRecord("STAYS", "Rule", 0, 0));

		pb.importListRecord(null);

		assertEquals(0, pb.listRecord.size(),
				"null source -> list cleared (importCompressedList no-ops past clear)");
	}

	@Test
	void importListRecordOnEmptySourceClearsTheList() {
		NetSPPersonalBest pb = new NetSPPersonalBest();
		pb.listRecord.add(buildRecord("STAYS", "Rule", 0, 0));

		pb.importListRecord("");

		assertEquals(0, pb.listRecord.size(),
				"empty source -> list cleared");
	}

	@Test
	void importListRecordReplacesExistingContents() {
		NetSPPersonalBest pb = new NetSPPersonalBest();
		pb.listRecord.add(buildRecord("STALE", "Rule", 0, 0));

		NetSPPersonalBest source = new NetSPPersonalBest();
		source.listRecord.add(buildRecord("FRESH", "Rule", 50, 1));
		String exported = source.exportListRecord();

		pb.importListRecord(exported);

		assertEquals(1, pb.listRecord.size());
		assertEquals("FRESH", pb.listRecord.get(0).strModeName,
				"importListRecord replaces — does not append");
	}

	@Test
	void importListRecordAllocatesListWhenNullBeforeImport() {
		// Import on a record with listRecord=null must allocate a fresh
		// list rather than NPE.
		NetSPPersonalBest pb = new NetSPPersonalBest();
		pb.listRecord = null;

		pb.importListRecord("");

		assertNotNull(pb.listRecord,
				"importListRecord must allocate listRecord when null");
		assertEquals(0, pb.listRecord.size());
	}

	@Test
	void exportListRecordOfMultipleRecordsProducesParallelOrder() {
		// Three records exported in insertion order produce a
		// three-entry semicolon-separated string; reimporting preserves
		// the order.
		NetSPPersonalBest source = new NetSPPersonalBest();
		LinkedList<NetSPRecord> records = source.listRecord;
		records.add(buildRecord("A", "R", 10, 1));
		records.add(buildRecord("B", "R", 20, 2));
		records.add(buildRecord("C", "R", 30, 3));

		String exported = source.exportListRecord();
		NetSPPersonalBest dest = new NetSPPersonalBest();
		dest.importListRecord(exported);

		assertEquals(3, dest.listRecord.size());
		assertEquals("A", dest.listRecord.get(0).strModeName);
		assertEquals("B", dest.listRecord.get(1).strModeName);
		assertEquals("C", dest.listRecord.get(2).strModeName);
	}

	private static NetSPRecord buildRecord(String modeName, String ruleName,
			int score, int lines) {
		NetSPRecord record = new NetSPRecord();
		record.strModeName = modeName;
		record.strRuleName = ruleName;
		record.stats = new Statistics();
		record.stats.score = score;
		record.stats.lines = lines;
		return record;
	}
}

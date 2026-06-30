package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.LinkedList;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Statistics;
import nullpomino.util.CustomProperties;

class NetSPRecordListTest {

	@Test
	void deepCopyClonesEachRecordIndependently() {
		LinkedList<NetSPRecord> source = new LinkedList<>();
		source.add(record("alice", 100));
		source.add(record("bob", 200));

		LinkedList<NetSPRecord> copy = NetSPRecordList.deepCopy(source);

		assertEquals(2, copy.size());
		assertEquals("alice", copy.get(0).strPlayerName);
		assertEquals("bob", copy.get(1).strPlayerName);
		assertNotSame(source.get(0), copy.get(0));

		source.get(0).stats.score = 9999;
		assertEquals(100, copy.get(0).stats.score);
	}

	@Test
	void deepCopyOfEmptyListReturnsEmptyList() {
		LinkedList<NetSPRecord> copy = NetSPRecordList.deepCopy(new LinkedList<>());

		assertEquals(0, copy.size());
	}

	@Test
	void writeAndReadPropertyRoundTripRecords() {
		LinkedList<NetSPRecord> source = new LinkedList<>();
		source.add(record("alice", 100));
		source.add(record("bob", 200));

		CustomProperties prop = new CustomProperties();
		NetSPRecordList.writeProperty(prop, "test.", source);

		LinkedList<NetSPRecord> read = new LinkedList<>();
		NetSPRecordList.readProperty(prop, "test.", read, -1);

		assertEquals(2, read.size());
		assertEquals(100, read.get(0).stats.score);
		assertEquals(200, read.get(1).stats.score);
	}

	@Test
	void readPropertyCapsRecordsAtMaxRecords() {
		LinkedList<NetSPRecord> source = new LinkedList<>();
		for(int i = 0; i < 5; i++) source.add(record("p" + i, 100 + i));

		CustomProperties prop = new CustomProperties();
		NetSPRecordList.writeProperty(prop, "test.", source);

		LinkedList<NetSPRecord> read = new LinkedList<>();
		NetSPRecordList.readProperty(prop, "test.", read, 2);

		assertEquals(2, read.size());
	}

	@Test
	void readPropertyClearsExistingRecordsBeforeReading() {
		LinkedList<NetSPRecord> read = new LinkedList<>();
		read.add(record("preexisting", 0));

		NetSPRecordList.readProperty(new CustomProperties(), "test.", read, -1);

		assertEquals(0, read.size());
	}

	@Test
	void exportImportCompressedRoundTripRecords() {
		LinkedList<NetSPRecord> source = new LinkedList<>();
		source.add(record("alice", 100));
		source.add(record("bob", 200));

		String compressed = NetSPRecordList.exportCompressedList(source);

		LinkedList<NetSPRecord> imported = new LinkedList<>();
		NetSPRecordList.importCompressedList(compressed, imported);

		assertEquals(2, imported.size());
		assertEquals(100, imported.get(0).stats.score);
		assertEquals(200, imported.get(1).stats.score);
	}

	@Test
	void importCompressedClearsListWhenSourceIsNullOrEmpty() {
		LinkedList<NetSPRecord> records = new LinkedList<>();
		records.add(record("preexisting", 0));

		NetSPRecordList.importCompressedList(null, records);
		assertEquals(0, records.size());

		records.add(record("preexisting", 0));
		NetSPRecordList.importCompressedList("", records);
		assertEquals(0, records.size());
	}

	@Test
	void readPropertySkipsMissingRecordSlots() {
		// Write a single record, then claim two exist so slot 1 reads back null
		// and the (compressedRecord != null) guard takes its false branch.
		LinkedList<NetSPRecord> source = new LinkedList<>();
		source.add(record("alice", 100));
		CustomProperties prop = new CustomProperties();
		NetSPRecordList.writeProperty(prop, "test.", source);
		prop.setProperty("test.numRecords", 2);

		LinkedList<NetSPRecord> read = new LinkedList<>();
		NetSPRecordList.readProperty(prop, "test.", read, -1);

		assertEquals(1, read.size());
	}

	@Test
	void importCompressedSkipsEmptySegments() {
		// A trailing ';' yields an empty split segment, exercising the
		// (compressedRecord.length() > 0) false branch while the valid
		// segments still import.
		LinkedList<NetSPRecord> source = new LinkedList<>();
		source.add(record("alice", 100));
		source.add(record("bob", 200));
		String compressed = NetSPRecordList.exportCompressedList(source);

		LinkedList<NetSPRecord> imported = new LinkedList<>();
		NetSPRecordList.importCompressedList(compressed + ";", imported);

		assertEquals(2, imported.size());
	}

	private static NetSPRecord record(String playerName, int score) {
		NetSPRecord r = new NetSPRecord();
		r.strPlayerName = playerName;
		r.strModeName = "TestMode";
		r.strRuleName = "TestRule";
		r.strReplayProp = "";
		r.strTimeStamp = "2026-05-01-00-00-00";
		r.stats = new Statistics();
		r.stats.score = score;
		return r;
	}
}

package nullpomino.game.net;

import java.util.LinkedList;

import nullpomino.util.CustomProperties;

final class NetSPRecordList {
	private NetSPRecordList() {
	}

	static LinkedList<NetSPRecord> deepCopy(LinkedList<NetSPRecord> records) {
		LinkedList<NetSPRecord> copy = new LinkedList<NetSPRecord>();
		for(NetSPRecord record : records) {
			copy.add(new NetSPRecord(record));
		}
		return copy;
	}

	static void writeProperty(CustomProperties prop, String keyPrefix,
			LinkedList<NetSPRecord> records) {
		prop.setProperty(keyPrefix + "numRecords", records.size());
		for(int i = 0; i < records.size(); i++) {
			prop.setProperty(keyPrefix + i, compress(records.get(i)));
		}
	}

	static void readProperty(CustomProperties prop, String keyPrefix,
			LinkedList<NetSPRecord> records, int maxRecords) {
		int numRecords = prop.getProperty(keyPrefix + "numRecords", 0);
		if((maxRecords >= 0) && (numRecords > maxRecords)) numRecords = maxRecords;

		records.clear();
		for(int i = 0; i < numRecords; i++) {
			String compressedRecord = prop.getProperty(keyPrefix + i);
			if(compressedRecord != null) {
				records.add(decompress(compressedRecord));
			}
		}
	}

	static String exportCompressedList(LinkedList<NetSPRecord> records) {
		LinkedList<String> compressedRecords = new LinkedList<String>();
		for(NetSPRecord record : records) {
			compressedRecords.add(compress(record));
		}
		return String.join(";", compressedRecords);
	}

	static void importCompressedList(String source,
			LinkedList<NetSPRecord> records) {
		records.clear();
		if((source == null) || (source.length() <= 0)) return;

		String[] compressedRecords = source.split(";", -1);
		for(String compressedRecord : compressedRecords) {
			if(compressedRecord.length() > 0) {
				records.add(decompress(compressedRecord));
			}
		}
	}

	private static String compress(NetSPRecord record) {
		return NetUtil.compressString(record.exportString());
	}

	private static NetSPRecord decompress(String compressedRecord) {
		return new NetSPRecord(NetUtil.decompressString(compressedRecord));
	}
}

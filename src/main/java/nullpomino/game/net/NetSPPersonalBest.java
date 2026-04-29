package nullpomino.game.net;

import java.io.Serializable;
import java.util.LinkedList;

import nullpomino.util.CustomProperties;

/**
 * Single player personal record manager
 */
public class NetSPPersonalBest implements Serializable {
	/** serialVersionUID for Serialize */
	private static final long serialVersionUID = 1L;

	/** Player Name */
	public String strPlayerName;

	/** Records */
	public LinkedList<NetSPRecord> listRecord;

	/**
	 * Constructor
	 */
	public NetSPPersonalBest() {
		reset();
	}

	/**
	 * Copy Constructor
	 * @param s Source
	 */
	public NetSPPersonalBest(NetSPPersonalBest s) {
		copy(s);
	}

	/**
	 * Constructor that imports data from a String Array
	 * @param s String Array (String[2])
	 */
	public NetSPPersonalBest(String[] s) {
		importStringArray(s);
	}

	/**
	 * Constructor that imports data from a String
	 * @param s String (Split by ;)
	 */
	public NetSPPersonalBest(String s) {
		importString(s);
	}

	/**
	 * Initialization
	 */
	public void reset() {
		strPlayerName = "";
		listRecord = new LinkedList<NetSPRecord>();
	}

	/**
	 * Copy from other NetSPPersonalBest
	 * @param s Source
	 */
	public void copy(NetSPPersonalBest s) {
		strPlayerName = s.strPlayerName;
		listRecord = NetSPRecordList.deepCopy(s.listRecord);
	}

	/**
	 * Get specific NetSPRecord
	 * @param rule Rule Name
	 * @param mode Mode Name
	 * @param gtype Game Type
	 * @return NetSPRecord (null if not found)
	 */
	public NetSPRecord getRecord(String rule, String mode, int gtype) {
		for(NetSPRecord r : listRecord) {
			if(r.strRuleName.equals(rule) && r.strModeName.equals(mode) && r.gameType == gtype) {
				return r;
			}
		}
		return null;
	}

	/**
	 * Checks if r1 is a new record.
	 * @param rtype Ranking Type
	 * @param r1 Newer Record
	 * @return Returns <code>true</code> if there are no previous record of this player, or if the newer record (r1) is better than old one.
	 */
	public boolean isNewRecord(int rtype, NetSPRecord r1) {
		NetSPRecord r2 = getRecord(r1.strRuleName, r1.strModeName, r1.gameType);
		if(r2 == null) return true;
		return r1.compare(rtype, r2);
	}

	/**
	 * Register a record.
	 * @param rtype Ranking Type
	 * @param r1 Newer Record
	 * @return Returns <code>true</code> if the newer record (r1) is registered.
	 */
	public boolean registerRecord(int rtype, NetSPRecord r1) {
		NetSPRecord r2 = getRecord(r1.strRuleName, r1.strModeName, r1.gameType);

		if(r2 != null) {
			if(r1.compare(rtype, r2)) {
				// Replace with a new record
				r2.copy(r1);
			} else {
				return false;
			}
		} else {
			// Register a new record
			listRecord.add(r1);
		}

		return true;
	}

	/**
	 * Write to a CustomProperties
	 * @param prop CustomProperties
	 */
	public void writeProperty(CustomProperties prop) {
		NetSPRecordList.writeProperty(prop, propertyKey(), listRecord);
	}

	/**
	 * Read from a CustomProperties
	 * @param prop CustomProperties
	 */
	public void readProperty(CustomProperties prop) {
		NetSPRecordList.readProperty(prop, propertyKey(), listRecord, -1);
	}

	/**
	 * Export the records to a String
	 * @return String (Split by ;)
	 */
	public String exportListRecord() {
		return NetSPRecordList.exportCompressedList(listRecord);
	}

	/**
	 * Import the record from a String
	 * @param s String (Split by ;)
	 */
	public void importListRecord(String s) {
		if(listRecord == null) listRecord = new LinkedList<NetSPRecord>();
		NetSPRecordList.importCompressedList(s, listRecord);
	}

	private String propertyKey() {
		return "sppersonal." + strPlayerName + ".";
	}

	/**
	 * Export to a String Array
	 * @return String Array (String[2])
	 */
	public String[] exportStringArray() {
		String[] s = new String[2];
		s[0] = NetUtil.urlEncode(strPlayerName);
		s[1] = exportListRecord();
		return s;
	}

	/**
	 * Export to a String
	 * @return String (Split by ;)
	 */
	public String exportString() {
		return String.join(";", exportStringArray());
	}


	/**
	 * Import from a String Array
	 * @param s String Array (String[8])
	 */
	public void importStringArray(String[] s) {
		if(listRecord == null) listRecord = new LinkedList<NetSPRecord>();
		if(s.length > 0) strPlayerName = NetUtil.urlDecode(s[0]);
		if(s.length > 1) importListRecord(s[1]);
		else listRecord.clear();
	}

	/**
	 * Import from a String
	 * @param s String (Split by ;)
	 */
	public void importString(String s) {
		importStringArray(s.split(";", -1));
	}
}

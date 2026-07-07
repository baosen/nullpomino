package nullpomino.game.net;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;

import nullpomino.util.CustomProperties;

/**
 * Single player mode ranking
 */
public class NetSPRanking implements Serializable {
	/** serialVersionUID for Serialize */
	private static final long serialVersionUID = 1L;

	/** Game Mode Name */
	public String strModeName;

	/** Rule Name */
	public String strRuleName;

	/** Game Type ID */
	public int gameType;

	/** Game Style ID */
	public int style;

	/** Ranking Type */
	public int rankingType;

	/** Max number of records (-1:Unlimited) */
	public int maxRecords;

	/** Records */
	public LinkedList<NetSPRecord> listRecord;

	/**
	 * Default Constructor
	 */
	public NetSPRanking() {
		reset();
	}

	/**
	 * Copy Constructor
	 * @param s Source
	 */
	public NetSPRanking(NetSPRanking s) {
		copy(s);
	}

	/**
	 * Constructor
	 * @param modename Game Mode Name
	 * @param rulename Rule Name
	 * @param gtype Game Type ID
	 * @param style Game Style ID
	 * @param rtype Ranking Type
	 * @param max Max number of records
	 */
	public NetSPRanking(String modename, String rulename, int gtype, int style, int rtype, int max) {
		reset();
		this.strModeName = modename;
		this.strRuleName = rulename;
		this.gameType = gtype;
		this.style = style;
		this.rankingType = rtype;
		this.maxRecords = max;
	}

	/**
	 * Initialization
	 */
	public void reset() {
		strModeName = "";
		strRuleName = "";
		gameType = 0;
		style = 0;
		rankingType = 0;
		maxRecords = 100;
		listRecord = new LinkedList<NetSPRecord>();
	}

	/**
	 * Copy from other NetSPRankingData
	 * @param s Source
	 */
	public void copy(NetSPRanking s) {
		strModeName = s.strModeName;
		strRuleName = s.strRuleName;
		gameType = s.gameType;
		style = s.style;
		rankingType = s.rankingType;
		maxRecords = s.maxRecords;
		listRecord = NetSPRecordList.deepCopy(s.listRecord);
	}

	/**
	 * Get specific player's record
	 * @param strPlayerName Player Name
	 * @return NetSPRecord (null if not found)
	 */
	public NetSPRecord getRecord(String strPlayerName) {
		int index = indexOf(strPlayerName);
		return (index == -1) ? null : listRecord.get(index);
	}

	/**
	 * Get specific player's record
	 * @param pInfo NetPlayerInfo
	 * @return NetSPRecord (null if not found)
	 */
	public NetSPRecord getRecord(NetPlayerInfo pInfo) {
		return getRecord(pInfo.strName);
	}

	/**
	 * Get specific player's index
	 * @param strPlayerName Player Name
	 * @return Index (-1 if not found)
	 */
	public int indexOf(String strPlayerName) {
		for(int i = 0; i < listRecord.size(); i++) {
			NetSPRecord r = listRecord.get(i);
			if(r.strPlayerName.equals(strPlayerName)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Get specific player's index
	 * @param pInfo NetPlayerInfo
	 * @return Index (-1 if not found)
	 */
	public int indexOf(NetPlayerInfo pInfo) {
		return indexOf(pInfo.strName);
	}

	/**
	 * Remove specific player's record
	 * @param strPlayerName Player Name
	 * @return Number of records removed (0 if not found)
	 */
	public int removeRecord(String strPlayerName) {
		int originalSize = listRecord.size();
		for(Iterator<NetSPRecord> it = listRecord.iterator(); it.hasNext();) {
			if(it.next().strPlayerName.equals(strPlayerName)) {
				it.remove();
			}
		}

		return originalSize - listRecord.size();
	}

	/**
	 * Remove specific player's record
	 * @param pInfo NetPlayerInfo
	 * @return Number of records removed (0 if not found)
	 */
	public int removeRecord(NetPlayerInfo pInfo) {
		return removeRecord(pInfo.strName);
	}

	/**
	 * Checks if r1 is a new record.
	 * @param r1 Newer Record
	 * @return Returns <code>true</code> if there are no previous record of this player, or if the newer record (r1) is better than old one.
	 */
	public boolean isNewRecord(NetSPRecord r1) {
		NetSPRecord r2 = getRecord(r1.strPlayerName);
		if(r2 == null) return true;
		return r1.compare(rankingType, r2);
	}

	/**
	 * Register a new record
	 * @param r1 Record
	 * @return Rank (-1 if out of rank)
	 */
	public int registerRecord(NetSPRecord r1) {
		if(!isNewRecord(r1)) return -1;

		// Remove older records
		removeRecord(r1.strPlayerName);

		// Insert new record
		int rank = listRecord.size();

		for(int i = 0; i < listRecord.size(); i++) {
			if(r1.compare(rankingType, listRecord.get(i))) {
				rank = i;
				break;
			}
		}
		listRecord.add(rank, r1);

		// Remove anything after maxRecords
		if(maxRecords >= 0) {
			while(listRecord.size() > maxRecords) listRecord.removeLast();
		}

		// Done
		return ((maxRecords >= 0) && (rank >= maxRecords)) ? -1 : rank;
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
		NetSPRecordList.readProperty(prop, propertyKey(), listRecord, maxRecords);
	}

	private String propertyKey() {
		return "spranking." + strRuleName + "." + strModeName + "." + gameType + ".";
	}

}

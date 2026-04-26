package nullpomino.game.net;

import java.io.Serializable;
import java.util.LinkedList;

import nullpomino.game.component.Statistics;
import nullpomino.util.CustomProperties;

/**
 * Single player mode record
 */
public class NetSPRecord implements Serializable {
	/** serialVersionUID for Serialize */
	private static final long serialVersionUID = 1L;

	/** Ranking type constants */
	public static final int RANKINGTYPE_GENERIC_SCORE = 0,
							RANKINGTYPE_GENERIC_TIME = 1,
							RANKINGTYPE_SCORERACE = 2,
							RANKINGTYPE_DIGRACE = 3,
							RANKINGTYPE_ULTRA = 4,
							RANKINGTYPE_COMBORACE = 5,
							RANKINGTYPE_DIGCHALLENGE = 6,
							RANKINGTYPE_TIMEATTACK = 7;

	/** Player Name */
	public String strPlayerName;

	/** Game Mode Name */
	public String strModeName;

	/** Rule Name */
	public String strRuleName;

	/** Main Stats */
	public Statistics stats;

	/** List of custom stats (Each String is NAME;VALUE format) */
	public LinkedList<String> listCustomStats;

	/** Replay data (Compressed) */
	public String strReplayProp;

	/** Time stamp (GMT) */
	public String strTimeStamp;

	/** Game Type ID */
	public int gameType;

	/** Game Style ID */
	public int style;

	/**
	 * Compare 2 records
	 * @param type Ranking Type
	 * @param r1 Record 1
	 * @param r2 Record 2
	 * @return <code>true</code> if r1 is better than r2
	 */
	public static boolean compareRecords(int type, NetSPRecord r1, NetSPRecord r2) {
		Statistics s1 = r1.stats;
		Statistics s2 = r2.stats;

		switch(type) {
		case RANKINGTYPE_GENERIC_SCORE:
			return isBetter(
					higher(s1.score, s2.score),
					higher(s1.lines, s2.lines),
					lower(s1.time, s2.time));
		case RANKINGTYPE_GENERIC_TIME:
			return isBetter(
					lower(s1.time, s2.time),
					lower(s1.totalPieceLocked, s2.totalPieceLocked),
					higher(s1.pps, s2.pps));
		case RANKINGTYPE_SCORERACE:
			return isBetter(
					lower(s1.time, s2.time),
					lower(s1.lines, s2.lines),
					higher(s1.spl, s2.spl));
		case RANKINGTYPE_DIGRACE:
			return isBetter(
					lower(s1.time, s2.time),
					lower(s1.lines, s2.lines),
					lower(s1.totalPieceLocked, s2.totalPieceLocked));
		case RANKINGTYPE_ULTRA:
			return isBetter(
					higher(s1.score, s2.score),
					higher(s1.lines, s2.lines),
					lower(s1.totalPieceLocked, s2.totalPieceLocked));
		case RANKINGTYPE_COMBORACE:
			return isBetter(
					higher(s1.maxCombo, s2.maxCombo),
					lower(s1.time, s2.time),
					higher(s1.pps, s2.pps));
		case RANKINGTYPE_DIGCHALLENGE:
			return isBetter(
					higher(s1.score, s2.score),
					higher(s1.lines, s2.lines),
					higher(s1.time, s2.time));
		case RANKINGTYPE_TIMEATTACK:
			int maxLines = (r1.gameType >= 5) ? 200 : 150;
			int l1 = Math.min(s1.lines, maxLines);
			int l2 = Math.min(s2.lines, maxLines);
			return isBetter(
					higher(s1.rollclear, s2.rollclear),
					higher(l1, l2),
					lower(s1.time, s2.time),
					higher(s1.pps, s2.pps));
		default:
			return false;
		}
	}

	private static boolean isBetter(int... comparisons) {
		for(int comparison : comparisons) {
			if(comparison > 0) return true;
			if(comparison < 0) return false;
		}
		return false;
	}

	private static int higher(int a, int b) {
		return Integer.compare(a, b);
	}

	private static int lower(int a, int b) {
		return Integer.compare(b, a);
	}

	private static int higher(float a, float b) {
		return Float.compare(a, b);
	}

	private static int higher(double a, double b) {
		return Double.compare(a, b);
	}

	/**
	 * Default Constructor
	 */
	public NetSPRecord() {
		reset();
	}

	/**
	 * Copy Constructor
	 * @param s Source
	 */
	public NetSPRecord(NetSPRecord s) {
		copy(s);
	}

	/**
	 * Constructor that imports data from a String Array
	 * @param s String Array (String[6])
	 */
	public NetSPRecord(String[] s) {
		importStringArray(s);
	}

	/**
	 * Constructor that imports data from a String
	 * @param s String (Split by ;)
	 */
	public NetSPRecord(String s) {
		importString(s);
	}

	/**
	 * Initialization
	 */
	public void reset() {
		strPlayerName = "";
		strModeName = "";
		strRuleName = "";
		stats = null;
		listCustomStats = new LinkedList<String>();
		strReplayProp = "";
		strTimeStamp = "";
		gameType = 0;
		style = 0;
	}

	/**
	 * Copy from other NetSPRecord
	 * @param s Source
	 */
	public void copy(NetSPRecord s) {
		strPlayerName = s.strPlayerName;
		strModeName = s.strModeName;
		strRuleName = s.strRuleName;

		if(s.stats == null) stats = null;
		else stats = new Statistics(s.stats);

		listCustomStats = new LinkedList<String>(s.listCustomStats);

		strReplayProp = s.strReplayProp;
		strTimeStamp = s.strTimeStamp;
		gameType = s.gameType;
		style = s.style;
	}

	/**
	 * Export custom stats to a String
	 * @return String (Split by ,)
	 */
	public String exportCustomStats() {
		if((listCustomStats != null) && (listCustomStats.size() > 0)) {
			return String.join(",", listCustomStats);
		}
		return "";
	}

	/**
	 * Import custom stats from a String
	 * @param s String (Split by ,)
	 */
	public void importCustomStats(String s) {
		if(listCustomStats == null) listCustomStats = new LinkedList<String>();
		else listCustomStats.clear();
		if((s == null) || (s.length() <= 0)) return;

		String[] array = s.split(",");
		for(String customStat : array) {
			listCustomStats.add(customStat);
		}
	}


	/**
	 * Set replay data from CustomProperties
	 * @param p CustomProperties that contains replay data
	 */
	public void setReplayProp(CustomProperties p) {
		String strEncode = p.encode("NullpoMino Net Single Player Replay (" + strPlayerName + ")");
		strReplayProp = NetUtil.compressString(strEncode);
	}

	/**
	 * Export to a String Array
	 * @return String Array (String[9])
	 */
	public String[] exportStringArray() {
		String[] s = new String[9];
		s[0] = NetUtil.urlEncode(strPlayerName);
		s[1] = NetUtil.urlEncode(strModeName);
		s[2] = NetUtil.urlEncode(strRuleName);
		s[3] = (stats == null) ? "" : NetUtil.compressString(stats.exportString());
		s[4] = ((listCustomStats == null) || (listCustomStats.size() <= 0)) ? "" : NetUtil.compressString(exportCustomStats());
		s[5] = strReplayProp;
		s[6] = Integer.toString(gameType);
		s[7] = Integer.toString(style);
		s[8] = strTimeStamp;
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
	 * @param s String Array (String[9])
	 */
	public void importStringArray(String[] s) {
		strPlayerName = NetUtil.urlDecode(s[0]);
		strModeName = NetUtil.urlDecode(s[1]);
		strRuleName = NetUtil.urlDecode(s[2]);
		if(s[3].length() <= 0) stats = null;
		else stats = new Statistics(NetUtil.decompressString(s[3]));
		if(s[4].length() <= 0) listCustomStats = new LinkedList<String>();
		else importCustomStats(NetUtil.decompressString(s[4]));
		strReplayProp = s[5];
		gameType = Integer.parseInt(s[6]);
		style = Integer.parseInt(s[7]);
		strTimeStamp = (s.length > 8) ? s[8] : "";
	}

	/**
	 * Import from a String
	 * @param s String (Split by ;)
	 */
	public void importString(String s) {
		importStringArray(s.split(";", -1));
	}

	/**
	 * Compare to other NetSPRecord
	 * @param type Ranking Type
	 * @param r2 The other NetSPRecord
	 * @return <code>true</code> if this this record is better than r2
	 */
	public boolean compare(int type, NetSPRecord r2) {
		return compareRecords(type, this, r2);
	}

	/**
	 * Get String value of specific custom stat
	 * @param name Custom stat name
	 * @return Value (null if not found)
	 */
	public String getCustomStat(String name) {
		for(String strTemp : listCustomStats) {
			String[] strArray = strTemp.split(";", 2);

			if((strArray.length > 1) && strArray[0].equals(name)) {
				return strArray[1];
			}
		}
		return null;
	}

	/**
	 * Get String value of specific custom stat
	 * @param name Custom stat name
	 * @param strDefault Default value (used when the name is not found)
	 * @return Value (strDefault if not found)
	 */
	public String getCustomStat(String name, String strDefault) {
		String strResult = getCustomStat(name);
		return (strResult == null) ? strDefault : strResult;
	}

	/**
	 * Get a short String of stats of the record (used by NetServer)
	 * @param type Ranking Type
	 * @return Short String of stats of the record
	 */
	public String getStatRow(int type) {
		switch(type) {
		case RANKINGTYPE_GENERIC_SCORE:
			return statRow(stats.score, stats.lines, stats.time);
		case RANKINGTYPE_GENERIC_TIME:
			return statRow(stats.time, stats.totalPieceLocked, stats.pps);
		case RANKINGTYPE_SCORERACE:
			return statRow(stats.time, stats.lines, stats.spl);
		case RANKINGTYPE_DIGRACE:
			return statRow(stats.time, stats.lines, stats.totalPieceLocked);
		case RANKINGTYPE_ULTRA:
			return statRow(stats.score, stats.lines, stats.totalPieceLocked);
		case RANKINGTYPE_COMBORACE:
			return statRow(stats.maxCombo, stats.time, stats.pps);
		case RANKINGTYPE_DIGCHALLENGE:
			return statRow(stats.score, stats.lines, stats.time);
		case RANKINGTYPE_TIMEATTACK:
			return statRow(stats.lines, stats.time, stats.pps, stats.rollclear);
		default:
			return "";
		}
	}

	private static String statRow(Object... values) {
		String[] strings = new String[values.length];
		for(int i = 0; i < values.length; i++) {
			strings[i] = String.valueOf(values[i]);
		}
		return String.join(",", strings);
	}
}

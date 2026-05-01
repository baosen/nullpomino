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

	private static final int EXPORT_FIELD_COUNT = 9;

	/** Ranking type constants */
	public static final int RANKINGTYPE_GENERIC_SCORE = 0,
							RANKINGTYPE_GENERIC_TIME = 1,
							RANKINGTYPE_SCORERACE = 2,
							RANKINGTYPE_DIGRACE = 3,
							RANKINGTYPE_ULTRA = 4,
							RANKINGTYPE_COMBORACE = 5,
							RANKINGTYPE_DIGCHALLENGE = 6,
							RANKINGTYPE_TIMEATTACK = 7;

	private static final Ranking[] RANKINGS = {
			new Ranking((r1, r2) -> isBetter(
					higher(r1.stats.score, r2.stats.score),
					higher(r1.stats.lines, r2.stats.lines),
					lower(r1.stats.time, r2.stats.time)),
					s -> statRow(s.score, s.lines, s.time)),
			new Ranking((r1, r2) -> isBetter(
					lower(r1.stats.time, r2.stats.time),
					lower(r1.stats.totalPieceLocked, r2.stats.totalPieceLocked),
					higher(r1.stats.pps, r2.stats.pps)),
					s -> statRow(s.time, s.totalPieceLocked, s.pps)),
			new Ranking((r1, r2) -> isBetter(
					lower(r1.stats.time, r2.stats.time),
					lower(r1.stats.lines, r2.stats.lines),
					higher(r1.stats.spl, r2.stats.spl)),
					s -> statRow(s.time, s.lines, s.spl)),
			new Ranking((r1, r2) -> isBetter(
					lower(r1.stats.time, r2.stats.time),
					lower(r1.stats.lines, r2.stats.lines),
					lower(r1.stats.totalPieceLocked, r2.stats.totalPieceLocked)),
					s -> statRow(s.time, s.lines, s.totalPieceLocked)),
			new Ranking((r1, r2) -> isBetter(
					higher(r1.stats.score, r2.stats.score),
					higher(r1.stats.lines, r2.stats.lines),
					lower(r1.stats.totalPieceLocked, r2.stats.totalPieceLocked)),
					s -> statRow(s.score, s.lines, s.totalPieceLocked)),
			new Ranking((r1, r2) -> isBetter(
					higher(r1.stats.maxCombo, r2.stats.maxCombo),
					lower(r1.stats.time, r2.stats.time),
					higher(r1.stats.pps, r2.stats.pps)),
					s -> statRow(s.maxCombo, s.time, s.pps)),
			new Ranking((r1, r2) -> isBetter(
					higher(r1.stats.score, r2.stats.score),
					higher(r1.stats.lines, r2.stats.lines),
					higher(r1.stats.time, r2.stats.time)),
					s -> statRow(s.score, s.lines, s.time)),
			new Ranking(NetSPRecord::timeAttackIsBetter,
					s -> statRow(s.lines, s.time, s.pps, s.rollclear)),
	};

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
		Ranking ranking = ranking(type);
		return (ranking != null) && ranking.isBetter(r1, r2);
	}

	private static Ranking ranking(int type) {
		return ((type < 0) || (type >= RANKINGS.length)) ? null : RANKINGS[type];
	}

	private static boolean timeAttackIsBetter(NetSPRecord r1, NetSPRecord r2) {
		int maxLines = (r1.gameType >= 5) ? 200 : 150;
		return isBetter(
				higher(r1.stats.rollclear, r2.stats.rollclear),
				higher(Math.min(r1.stats.lines, maxLines), Math.min(r2.stats.lines, maxLines)),
				lower(r1.stats.time, r2.stats.time),
				higher(r1.stats.pps, r2.stats.pps));
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

	private record Ranking(RecordRanker ranker, StatRowFormatter rowFormatter) {
		boolean isBetter(NetSPRecord r1, NetSPRecord r2) {
			return ranker.isBetter(r1, r2);
		}

		String statRow(Statistics stats) {
			return rowFormatter.format(stats);
		}
	}

	@FunctionalInterface
	private interface RecordRanker {
		boolean isBetter(NetSPRecord r1, NetSPRecord r2);
	}

	@FunctionalInterface
	private interface StatRowFormatter {
		String format(Statistics stats);
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
		NetStringArray.Writer writer = new NetStringArray.Writer(EXPORT_FIELD_COUNT);
		writer.writeEncoded(strPlayerName);
		writer.writeEncoded(strModeName);
		writer.writeEncoded(strRuleName);
		writer.write((stats == null) ? "" : NetUtil.compressString(stats.exportString()));
		writer.write(hasCustomStats() ? NetUtil.compressString(exportCustomStats()) : "");
		writer.write(strReplayProp);
		writer.write(gameType);
		writer.write(style);
		writer.write(strTimeStamp);
		return writer.values();
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
		NetStringArray.Reader reader = new NetStringArray.Reader(s);
		strPlayerName = reader.readEncoded();
		strModeName = reader.readEncoded();
		strRuleName = reader.readEncoded();
		stats = importStats(reader.read());
		importCompressedCustomStats(reader.read());
		strReplayProp = reader.read();
		gameType = reader.readInt();
		style = reader.readInt();
		strTimeStamp = reader.hasNext() ? reader.read() : "";
	}

	private boolean hasCustomStats() {
		return (listCustomStats != null) && (listCustomStats.size() > 0);
	}

	private static Statistics importStats(String compressedStats) {
		if(compressedStats.length() <= 0) return null;
		return new Statistics(NetUtil.decompressString(compressedStats));
	}

	private void importCompressedCustomStats(String compressedCustomStats) {
		if(compressedCustomStats.length() <= 0) {
			listCustomStats = new LinkedList<String>();
		} else {
			importCustomStats(NetUtil.decompressString(compressedCustomStats));
		}
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
		Ranking ranking = ranking(type);
		return (ranking == null) ? "" : ranking.statRow(stats);
	}

	private static String statRow(Object... values) {
		String[] strings = new String[values.length];
		for(int i = 0; i < values.length; i++) {
			strings[i] = String.valueOf(values[i]);
		}
		return String.join(",", strings);
	}
}

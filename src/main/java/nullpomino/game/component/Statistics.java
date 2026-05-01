// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.component;

import java.io.Serializable;

import nullpomino.util.CustomProperties;

/**
 * ScoreInformation such as the
 */
public class Statistics implements Serializable {
	/** Serial version ID */
	private static final long serialVersionUID = -499640168205398295L;

	private static final int EXPORT_FIELD_COUNT = 38;

	/** Total score */
	public int score;

	/** Line clear score */
	public int scoreFromLineClear;

	/** Soft drop score */
	public int scoreFromSoftDrop;

	/** Hard drop score */
	public int scoreFromHardDrop;

	/** I got in some other wayScore */
	public int scoreFromOtherBonus;

	/** Total line count */
	public int lines;

	/** Course time */
	public int time;

	/** Level */
	public int level;

	/** LevelAdded to the display ofcount (Display levelUse if it is different from the value of the internal) */
	public int levelDispAdd;

	/** I put the piececount */
	public int totalPieceLocked;

	/** The total operating pieces are time */
	public int totalPieceActiveTime;

	/** The total move the piece count */
	public int totalPieceMove;

	/** A piecerotationTotal to count */
	public int totalPieceRotate;

	/** 1-line clear count */
	public int totalSingle;

	/** 2-line clear count */
	public int totalDouble;

	/** 3-line clear count */
	public int totalTriple;

	/** 4-line clear count */
	public int totalFour;

	/** T-Spin 0 lines (with wallkick) count */
	public int totalTSpinZeroMini;

	/** T-Spin 0 lines (without wallkick) count */
	public int totalTSpinZero;

	/** T-Spin 1 line (with wallkick) count */
	public int totalTSpinSingleMini;

	/** T-Spin 1 line (without wallkick) count */
	public int totalTSpinSingle;

	/** T-Spin 2 line (with wallkick) count */
	public int totalTSpinDoubleMini;

	/** T-Spin 2 line (without wallkick) count */
	public int totalTSpinDouble;

	/** T-Spin 3 line count */
	public int totalTSpinTriple;

	/** Back to Back 4-line clear count */
	public int totalB2BFour;

	/** Back to Back T-Spin clear count */
	public int totalB2BTSpin;

	/** Hold use count */
	public int totalHoldUsed;

	/** Largest combo */
	public int maxCombo;

	/** 1LinesScore per (Score Per Line) */
	public double spl;

	/** 1Score per minute (Score Per Minute) */
	public double spm;

	/** 1Scores per second (Score Per Second) */
	public double sps;

	/** 1Per minuteLinescount (Lines Per Minute) */
	public float lpm;

	/** 1Per secondLinescount (Lines Per Second) */
	public float lps;

	/** 1Pieces per minutecount (Pieces Per Minute) */
	public float ppm;

	/** 1Per second piece ofcount (Pieces Per Second) */
	public float pps;

	/** TAS detection: slowdown rate */
	public float gamerate;

	/** Max chain */
	public int maxChain;

	/** Roll cleared flag (0=Died 1=Reached 2=Fully Survived) */
	public int rollclear;

	/**
	 * Constructor
	 */
	public Statistics() {
		reset();
	}

	/**
	 * Copy constructor
	 * @param s Copy source
	 */
	public Statistics(Statistics s) {
		copy(s);
	}

	/**
	 * Constructor that imports data from a String Array
	 * @param s String Array (String[37])
	 */
	public Statistics(String[] s) {
		importStringArray(s);
	}

	/**
	 * Constructor that imports data from a String
	 * @param s String (Split by ;)
	 */
	public Statistics(String s) {
		importString(s);
	}

	/**
	 * Reset to defaults
	 */
	public void reset() {
		score = 0;
		scoreFromLineClear = 0;
		scoreFromSoftDrop = 0;
		scoreFromHardDrop = 0;
		scoreFromOtherBonus = 0;
		lines = 0;
		time = 0;
		level = 0;
		levelDispAdd = 0;
		totalPieceLocked = 0;
		totalPieceActiveTime = 0;
		totalPieceMove = 0;
		totalPieceRotate = 0;
		totalSingle = 0;
		totalDouble = 0;
		totalTriple = 0;
		totalFour = 0;
		totalTSpinZeroMini = 0;
		totalTSpinZero = 0;
		totalTSpinSingleMini = 0;
		totalTSpinSingle = 0;
		totalTSpinDoubleMini = 0;
		totalTSpinDouble = 0;
		totalTSpinTriple = 0;
		totalB2BFour = 0;
		totalB2BTSpin = 0;
		totalHoldUsed = 0;
		maxCombo = 0;
		spl = 0.0;
		spm = 0.0;
		sps = 0.0;
		lpm = 0f;
		lps = 0f;
		ppm = 0f;
		pps = 0f;
		gamerate = 0f;
		maxChain = 0;
		rollclear = 0;
	}

	/**
	 * OtherStatisticsCopy the value of the
	 * @param s Copy source
	 */
	public void copy(Statistics s) {
		score = s.score;
		scoreFromLineClear = s.scoreFromLineClear;
		scoreFromSoftDrop = s.scoreFromSoftDrop;
		scoreFromHardDrop = s.scoreFromHardDrop;
		scoreFromOtherBonus = s.scoreFromOtherBonus;
		lines = s.lines;
		time = s.time;
		level = s.level;
		levelDispAdd = s.levelDispAdd;
		totalPieceLocked = s.totalPieceLocked;
		totalPieceActiveTime = s.totalPieceActiveTime;
		totalPieceMove = s.totalPieceMove;
		totalPieceRotate = s.totalPieceRotate;
		totalSingle = s.totalSingle;
		totalDouble = s.totalDouble;
		totalTriple = s.totalTriple;
		totalFour = s.totalFour;
		totalTSpinZeroMini = s.totalTSpinZeroMini;
		totalTSpinZero = s.totalTSpinZero;
		totalTSpinSingleMini = s.totalTSpinSingleMini;
		totalTSpinSingle = s.totalTSpinSingle;
		totalTSpinDoubleMini = s.totalTSpinDoubleMini;
		totalTSpinDouble = s.totalTSpinDouble;
		totalTSpinTriple = s.totalTSpinTriple;
		totalB2BFour = s.totalB2BFour;
		totalB2BTSpin = s.totalB2BTSpin;
		maxCombo = s.maxCombo;
		spl = s.spl;
		spm = s.spm;
		sps = s.sps;
		lpm = s.lpm;
		lps = s.lps;
		ppm = s.ppm;
		pps = s.pps;
		gamerate = s.gamerate;
		totalHoldUsed = s.totalHoldUsed;
		maxChain = s.maxChain;
		rollclear = s.rollclear;
	}

	/**
	 * SPMYaLPMUpdates
	 */
	public void update() {
		if(lines > 0) {
			spl = (double)(score) / (double)(lines);
		}
		if(time > 0) {
			spm = (double)(score * 3600.0) / (double)(time);
			sps = (double)(score * 60.0) / (double)(time);
			lpm = (float)(lines * 3600f) / (float)(time);
			lps = (float)(lines * 60f) / (float)(time);
			ppm = (float)(totalPieceLocked * 3600f) / (float)(time);
			pps = (float)(totalPieceLocked * 60f) / (float)(time);
		}
	}

	/**
	 * Stored in the property set
	 * @param p Property Set
	 * @param id AnyID (Player IDEtc.)
	 */
	public void writeProperty(CustomProperties p, int id) {
		p.setProperty(id + ".statistics.score", score);
		p.setProperty(id + ".statistics.scoreFromLineClear", scoreFromLineClear);
		p.setProperty(id + ".statistics.scoreFromSoftDrop", scoreFromSoftDrop);
		p.setProperty(id + ".statistics.scoreFromHardDrop", scoreFromHardDrop);
		p.setProperty(id + ".statistics.scoreFromOtherBonus", scoreFromOtherBonus);
		p.setProperty(id + ".statistics.lines", lines);
		p.setProperty(id + ".statistics.time", time);
		p.setProperty(id + ".statistics.level", level);
		p.setProperty(id + ".statistics.levelDispAdd", levelDispAdd);
		p.setProperty(id + ".statistics.totalPieceLocked", totalPieceLocked);
		p.setProperty(id + ".statistics.totalPieceActiveTime", totalPieceActiveTime);
		p.setProperty(id + ".statistics.totalPieceMove", totalPieceMove);
		p.setProperty(id + ".statistics.totalPieceRotate", totalPieceRotate);
		p.setProperty(id + ".statistics.totalSingle", totalSingle);
		p.setProperty(id + ".statistics.totalDouble", totalDouble);
		p.setProperty(id + ".statistics.totalTriple", totalTriple);
		p.setProperty(id + ".statistics.totalFour", totalFour);
		p.setProperty(id + ".statistics.totalTSpinZeroMini", totalTSpinZeroMini);
		p.setProperty(id + ".statistics.totalTSpinZero", totalTSpinZero);
		p.setProperty(id + ".statistics.totalTSpinSingleMini", totalTSpinSingleMini);
		p.setProperty(id + ".statistics.totalTSpinSingle", totalTSpinSingle);
		p.setProperty(id + ".statistics.totalTSpinDoubleMini", totalTSpinDoubleMini);
		p.setProperty(id + ".statistics.totalTSpinDouble", totalTSpinDouble);
		p.setProperty(id + ".statistics.totalTSpinTriple", totalTSpinTriple);
		p.setProperty(id + ".statistics.totalB2BFour", totalB2BFour);
		p.setProperty(id + ".statistics.totalB2BTSpin", totalB2BTSpin);
		p.setProperty(id + ".statistics.totalHoldUsed", totalHoldUsed);
		p.setProperty(id + ".statistics.maxCombo", maxCombo);
		p.setProperty(id + ".statistics.spl", spl);
		p.setProperty(id + ".statistics.spm", spm);
		p.setProperty(id + ".statistics.sps", sps);
		p.setProperty(id + ".statistics.lpm", lpm);
		p.setProperty(id + ".statistics.lps", lps);
		p.setProperty(id + ".statistics.ppm", ppm);
		p.setProperty(id + ".statistics.pps", pps);
		p.setProperty(id + ".statistics.gamerate", gamerate);
		p.setProperty(id + ".statistics.maxChain", maxChain);
		p.setProperty(id + ".statistics.rollclear", rollclear);

		// OldVersionFor compatibility with
		if(id == 0) {
			p.setProperty("result.score", score);
			p.setProperty("result.totallines", lines);
			p.setProperty("result.level", level);
			p.setProperty("result.time", time);
		}
	}

	/**
	 * Read from the property set
	 * @param p Property Set
	 * @param id AnyID (Player IDEtc.)
	 */
	public void readProperty(CustomProperties p, int id) {
		score = p.getProperty(id + ".statistics.score", 0);
		scoreFromLineClear = p.getProperty(id + ".statistics.scoreFromLineClear", 0);
		scoreFromSoftDrop = p.getProperty(id + ".statistics.scoreFromSoftDrop", 0);
		scoreFromHardDrop = p.getProperty(id + ".statistics.scoreFromHardDrop", 0);
		scoreFromOtherBonus = p.getProperty(id + ".statistics.scoreFromOtherBonus", 0);
		lines = p.getProperty(id + ".statistics.lines", 0);
		time = p.getProperty(id + ".statistics.time", 0);
		level = p.getProperty(id + ".statistics.level", 0);
		levelDispAdd = p.getProperty(id + ".statistics.levelDispAdd", 0);
		totalPieceLocked = p.getProperty(id + ".statistics.totalPieceLocked", 0);
		totalPieceActiveTime = p.getProperty(id + ".statistics.totalPieceActiveTime", 0);
		totalPieceMove = p.getProperty(id + ".statistics.totalPieceMove", 0);
		totalPieceRotate = p.getProperty(id + ".statistics.totalPieceRotate", 0);
		totalSingle = p.getProperty(id + ".statistics.totalSingle", 0);
		totalDouble = p.getProperty(id + ".statistics.totalDouble", 0);
		totalTriple = p.getProperty(id + ".statistics.totalTriple", 0);
		totalFour = p.getProperty(id + ".statistics.totalFour", 0);
		totalTSpinZeroMini = p.getProperty(id + ".statistics.totalTSpinZeroMini", 0);
		totalTSpinZero = p.getProperty(id + ".statistics.totalTSpinZero", 0);
		totalTSpinSingleMini = p.getProperty(id + ".statistics.totalTSpinSingleMini", 0);
		totalTSpinSingle = p.getProperty(id + ".statistics.totalTSpinSingle", 0);
		totalTSpinDoubleMini = p.getProperty(id + ".statistics.totalTSpinDoubleMini", 0);
		totalTSpinDouble = p.getProperty(id + ".statistics.totalTSpinDouble", 0);
		totalTSpinTriple = p.getProperty(id + ".statistics.totalTSpinTriple", 0);
		totalB2BFour = p.getProperty(id + ".statistics.totalB2BFour", 0);
		totalB2BTSpin = p.getProperty(id + ".statistics.totalB2BTSpin", 0);
		totalHoldUsed = p.getProperty(id + ".statistics.totalHoldUsed", 0);
		maxCombo = p.getProperty(id + ".statistics.maxCombo", 0);
		spl = p.getProperty(id + ".statistics.spl", 0f);
		spm = p.getProperty(id + ".statistics.spm", 0f);
		sps = p.getProperty(id + ".statistics.sps", 0f);
		lpm = p.getProperty(id + ".statistics.lpm", 0f);
		lps = p.getProperty(id + ".statistics.lps", 0f);
		ppm = p.getProperty(id + ".statistics.ppm", 0f);
		pps = p.getProperty(id + ".statistics.pps", 0f);
		gamerate = p.getProperty(id + ".statistics.gamerate", 0f);
		maxChain = p.getProperty(id + ".statistics.maxChain", 0);
		rollclear = p.getProperty(id + ".statistics.rollclear", 0);
	}

	/**
	 * Import from String Array
	 * @param s String Array (String[38])
	 */
	public void importStringArray(String[] s) {
		FieldReader reader = new FieldReader(s);
		score = reader.readInt();
		scoreFromLineClear = reader.readInt();
		scoreFromSoftDrop = reader.readInt();
		scoreFromHardDrop = reader.readInt();
		scoreFromOtherBonus = reader.readInt();
		lines = reader.readInt();
		time = reader.readInt();
		level = reader.readInt();
		levelDispAdd = reader.readInt();
		totalPieceLocked = reader.readInt();
		totalPieceActiveTime = reader.readInt();
		totalPieceMove = reader.readInt();
		totalPieceRotate = reader.readInt();
		totalSingle = reader.readInt();
		totalDouble = reader.readInt();
		totalTriple = reader.readInt();
		totalFour = reader.readInt();
		totalTSpinZeroMini = reader.readInt();
		totalTSpinZero = reader.readInt();
		totalTSpinSingleMini = reader.readInt();
		totalTSpinSingle = reader.readInt();
		totalTSpinDoubleMini = reader.readInt();
		totalTSpinDouble = reader.readInt();
		totalTSpinTriple = reader.readInt();
		totalB2BFour = reader.readInt();
		totalB2BTSpin = reader.readInt();
		totalHoldUsed = reader.readInt();
		maxCombo = reader.readInt();
		spl = reader.readDouble();
		spm = reader.readDouble();
		sps = reader.readDouble();
		lpm = reader.readFloat();
		lps = reader.readFloat();
		ppm = reader.readFloat();
		pps = reader.readFloat();
		gamerate = reader.readFloat();
		maxChain = reader.readInt();
		rollclear = reader.readInt(rollclear);
	}

	/**
	 * Import from String
	 * @param s String (Split by ;)
	 */
	public void importString(String s) {
		importStringArray(s.split(";", -1));
	}

	/**
	 * Export to String Array
	 * @return String Array (String[38])
	 */
	public String[] exportStringArray() {
		FieldWriter writer = new FieldWriter(EXPORT_FIELD_COUNT);
		writer.write(score);
		writer.write(scoreFromLineClear);
		writer.write(scoreFromSoftDrop);
		writer.write(scoreFromHardDrop);
		writer.write(scoreFromOtherBonus);
		writer.write(lines);
		writer.write(time);
		writer.write(level);
		writer.write(levelDispAdd);
		writer.write(totalPieceLocked);
		writer.write(totalPieceActiveTime);
		writer.write(totalPieceMove);
		writer.write(totalPieceRotate);
		writer.write(totalSingle);
		writer.write(totalDouble);
		writer.write(totalTriple);
		writer.write(totalFour);
		writer.write(totalTSpinZeroMini);
		writer.write(totalTSpinZero);
		writer.write(totalTSpinSingleMini);
		writer.write(totalTSpinSingle);
		writer.write(totalTSpinDoubleMini);
		writer.write(totalTSpinDouble);
		writer.write(totalTSpinTriple);
		writer.write(totalB2BFour);
		writer.write(totalB2BTSpin);
		writer.write(totalHoldUsed);
		writer.write(maxCombo);
		writer.write(spl);
		writer.write(spm);
		writer.write(sps);
		writer.write(lpm);
		writer.write(lps);
		writer.write(ppm);
		writer.write(pps);
		writer.write(gamerate);
		writer.write(maxChain);
		writer.write(rollclear);
		return writer.values();
	}

	/**
	 * Export to String
	 * @return String (Split by ;)
	 */
	public String exportString() {
		return String.join(";", exportStringArray());
	}

	private static final class FieldReader {
		private final String[] values;
		private int index;

		FieldReader(String[] values) {
			this.values = values;
		}

		boolean hasNext() {
			return index < values.length;
		}

		int readInt() {
			return Integer.parseInt(read());
		}

		int readInt(int defaultValue) {
			return hasNext() ? readInt() : defaultValue;
		}

		double readDouble() {
			return Double.parseDouble(read());
		}

		float readFloat() {
			return Float.parseFloat(read());
		}

		private String read() {
			return values[index++];
		}
	}

	private static final class FieldWriter {
		private final String[] values;
		private int index;

		FieldWriter(int size) {
			values = new String[size];
		}

		void write(int value) {
			write(Integer.toString(value));
		}

		void write(double value) {
			write(Double.toString(value));
		}

		void write(float value) {
			write(Float.toString(value));
		}

		String[] values() {
			return values;
		}

		private void write(String value) {
			values[index++] = value;
		}
	}
}

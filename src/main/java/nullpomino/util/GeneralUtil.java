// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import nullpomino.game.component.Piece;
import nullpomino.game.component.RuleOptions;
import nullpomino.game.ai.DummyAI;
import nullpomino.game.wallkick.Wallkick;
import nullpomino.game.randomizer.Randomizer;

/**
 * Generic static utils
 */
public class GeneralUtil {
	/** Log */
	static Logger log = Logger.getLogger(GeneralUtil.class);

	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

	/** Replay filename timestamp format (thread-safe). */
	private static final DateTimeFormatter REPLAY_FILENAME_FORMAT = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
	/** Human-readable date/time format (thread-safe). */
	private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	/** Wire/on-disk calendar format, GMT (thread-safe). */
	private static final DateTimeFormatter EXPORT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

	/**
	 * Converts play time into a String
	 * @param t Play time
	 * @return String for play time
	 */
	public static String getTime(int t) {
		if(t < 0) return "--:--.--";

		return String.format("%02d:%02d.%02d", t / 3600, (t / 60) % 60, (t % 60) * 5 / 3);
	}

	/**
	 * Returns ON if b is true, OFF if b is false
	 * @param b Boolean variable to be checked
	 * @return ON if b is true, OFF if b is false
	 */
	public static String getONorOFF(boolean b) {
		return b ? "ON" : "OFF";
	}

	/**
	 * Returns ○ if b is true, × if b is false
	 * @param b Boolean variable to be checked
	 * @return ○ if b is true, × if b is false
	 */
	public static String getOorX(boolean b) {
		return b ? "c" : "e";
	}

	/**
	 * Fetches the filename for a replay
	 * @return Replay's filename
	 */
	public static String getReplayFilename() {
		return LocalDateTime.now().format(REPLAY_FILENAME_FORMAT) + ".rep";
	}

	/**
	 * Get date and time from a Calendar
	 * @param c Calendar
	 * @return Date and Time String
	 */
	public static String getCalendarString(Calendar c) {
		return c.toInstant().atZone(ZoneId.systemDefault()).format(DISPLAY_FORMAT);
	}

	/**
	 * Get date and time from a Calendar with specific TimeZone
	 * @param c Calendar
	 * @param z TimeZone
	 * @return Date and Time String
	 */
	public static String getCalendarString(Calendar c, TimeZone z) {
		return c.toInstant().atZone(z.toZoneId()).format(DISPLAY_FORMAT);
	}

	/**
	 * Export a Calendar to a String for saving/sending. TimeZone is always GMT. Time is based on current time.
	 * @return Calendar String (Each field is separated with a hyphen '-')
	 */
	public static String exportCalendarString() {
		Calendar c = Calendar.getInstance(GMT);
		return exportCalendarString(c);
	}

	/**
	 * Export a Calendar to a String for saving/sending. TimeZone is always GMT.
	 * @param c Calendar
	 * @return Calendar String (Each field is separated with a hyphen '-')
	 */
	public static String exportCalendarString(Calendar c) {
		return c.toInstant().atZone(ZoneOffset.UTC).format(EXPORT_FORMAT);
	}

	/**
	 * Create a Calendar by using a String that came from exportCalendarString. TimeZone is always GMT.
	 * @param s String (Each field is separated with a hyphen '-')
	 * @return Calendar (null if fails)
	 */
	public static Calendar importCalendarString(String s) {
		try {
			LocalDateTime ldt = LocalDateTime.parse(s, EXPORT_FORMAT);
			Calendar c = Calendar.getInstance(GMT);
			c.setTimeInMillis(ldt.toInstant(ZoneOffset.UTC).toEpochMilli());
			return c;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Returns true if enabled piece types are S,Z,O only.
	 * @param pieceEnable Piece enable flags
	 * @return <code>true</code> if enabled piece types are S,Z,O only.
	 */
	public static boolean isPieceSZOOnly(boolean[] pieceEnable) {
		if(pieceEnable == null) return false;

		for(int i = 0; i < pieceEnable.length; i++) {
			if(pieceEnable[i] && !isSZOPiece(i))
				return false;
		}

		return true;
	}

	/**
	 * Create piece ID array from a String
	 * @param strSrc String
	 * @return Piece ID array
	 */
	public static int[] createNextPieceArrayFromNumberString(String strSrc) {
		int len = strSrc.length();
		if(len < 1) return null;

		int[] nextArray = new int[len];
		for(int i = 0; i < len; i++) {
			nextArray[i] = pieceIdFromDigit(strSrc.charAt(i));
		}

		return nextArray;
	}

	/**
	 * Load rule file
	 * @param filename Filename
	 * @return RuleOptions
	 */
	public static RuleOptions loadRule(String filename) {
		CustomProperties prop = new CustomProperties();

		try {
			prop = CustomProperties.loadFromFile(filename);
		} catch (Exception e) {
			log.warn("Failed to load rule from " + filename, e);
		}

		RuleOptions ruleopt = new RuleOptions();
		ruleopt.readProperty(prop, 0);

		return ruleopt;
	}

	/**
	 * Load Randomizer
	 * @param filename Classpath of the randomizer
	 * @return Randomizer (null if something fails)
	 */
	public static Randomizer loadRandomizer(String filename) {
		return loadClass(filename, Randomizer.class, "Randomizer");
	}

	/**
	 * Load Wallkick
	 * @param filename Classpath of the wallkick
	 * @return Wallkick (null if something fails)
	 */
	public static Wallkick loadWallkick(String filename) {
		return loadClass(filename, Wallkick.class, "Wallkick");
	}

	/**
	 * Load AI
	 * @param filename Classpath of the AI
	 * @return The instance of AI (null if something fails)
	 */
	public static DummyAI loadAIPlayer(String filename) {
		return loadClass(filename, DummyAI.class, "AIPlayer");
	}

	private static <T> T loadClass(String filename, Class<T> expectedType, String description) {
		try {
			return ClassFactory.create(filename, expectedType);
		} catch (Exception e) {
			log.warn("Failed to load " + description + " from " + filename, e);
			return null;
		}
	}
	
	/**
	 * Combine array of strings
	 * @param strings Array of strings
	 * @param separator Separator used for combine
	 * @param startIndex First element which will be combined
	 * @return Combined string
	 */
	public static String StringCombine(String[] strings, String separator,
			int startIndex)
	{
		if(startIndex >= strings.length) return "";
		return String.join(separator, Arrays.copyOfRange(strings, startIndex, strings.length));
	}

	private static boolean isSZOPiece(int pieceID) {
		return (pieceID == Piece.PIECE_S) || (pieceID == Piece.PIECE_Z) || (pieceID == Piece.PIECE_O);
	}

	private static int pieceIdFromDigit(char value) {
		if((value < '0') || (value > '9')) return Piece.PIECE_I;

		int pieceID = value - '0';
		if(pieceID >= Piece.PIECE_COUNT) return Piece.PIECE_I;
		return pieceID;
	}
}

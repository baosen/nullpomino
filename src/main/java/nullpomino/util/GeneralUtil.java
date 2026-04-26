// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.io.FileInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import nullpomino.game.component.Piece;
import nullpomino.game.component.RuleOptions;
import nullpomino.game.subsystem.ai.DummyAI;
import nullpomino.game.subsystem.wallkick.Wallkick;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer;

/**
 * Generic static utils
 */
public class GeneralUtil {
	/** Log */
	static Logger log = Logger.getLogger(GeneralUtil.class);

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
		if(b == true) return "ON";
		return "OFF";
	}

	/**
	 * Returns ○ if b is true, × if b is false
	 * @param b Boolean variable to be checked
	 * @return ○ if b is true, × if b is false
	 */
	public static String getOorX(boolean b) {
		if(b == true) return "c";
		return "e";
	}

	/**
	 * Fetches the filename for a replay
	 * @return Replay's filename
	 */
	public static String getReplayFilename() {
		Calendar c = Calendar.getInstance();
		DateFormat dfm = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String filename = dfm.format(c.getTime()) + ".rep";
		return filename;
	}

	/**
	 * Get date and time from a Calendar
	 * @param c Calendar
	 * @return Date and Time String
	 */
	public static String getCalendarString(Calendar c) {
		DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dfm.format(c.getTime());
	}

	/**
	 * Get date and time from a Calendar with specific TimeZone
	 * @param c Calendar
	 * @param z TimeZone
	 * @return Date and Time String
	 */
	public static String getCalendarString(Calendar c, TimeZone z) {
		DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dfm.setTimeZone(z);
		return dfm.format(c.getTime());
	}

	/**
	 * Export a Calendar to a String for saving/sending. TimeZone is always GMT. Time is based on current time.
	 * @return Calendar String (Each field is separated with a hyphen '-')
	 */
	public static String exportCalendarString() {
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		return exportCalendarString(c);
	}

	/**
	 * Export a Calendar to a String for saving/sending. TimeZone is always GMT.
	 * @param c Calendar
	 * @return Calendar String (Each field is separated with a hyphen '-')
	 */
	public static String exportCalendarString(Calendar c) {
		DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		dfm.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dfm.format(c.getTime());
	}

	/**
	 * Create a Calendar by using a String that came from exportCalendarString. TimeZone is always GMT.
	 * @param s String (Each field is separated with a hyphen '-')
	 * @return Calendar (null if fails)
	 */
	public static Calendar importCalendarString(String s) {
		DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		dfm.setTimeZone(TimeZone.getTimeZone("GMT"));

		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

		try {
			Date date = dfm.parse(s);
			c.setTime(date);
		} catch (Exception e) {
			return null;
		}

		return c;
	}

	/**
	 * Returns true if enabled piece types are S,Z,O only.
	 * @param pieceEnable Piece enable flags
	 * @return <code>true</code> if enabled piece types are S,Z,O only.
	 */
	public static boolean isPieceSZOOnly(boolean[] pieceEnable) {
		if(pieceEnable == null) return false;

		for(int i = 0; i < pieceEnable.length; i++) {
			if((pieceEnable[i] == true) && (i != Piece.PIECE_S) && (i != Piece.PIECE_Z) && (i != Piece.PIECE_O))
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
			int pieceID = Piece.PIECE_I;

			try {
				pieceID = Integer.parseInt(strSrc.substring(i, i + 1));
			} catch (NumberFormatException e) {}

			if((pieceID < 0) || (pieceID >= Piece.PIECE_COUNT)) pieceID = Piece.PIECE_I;

			nextArray[i] = pieceID;
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
			FileInputStream in = new FileInputStream(filename);
			prop.load(in);
			in.close();
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
		String res = "";
		for (int i = startIndex; i<strings.length; i++) {
			res+= strings[i];
			if (i != strings.length-1)
				res+= separator;
		}
		
		return res;
	}
}

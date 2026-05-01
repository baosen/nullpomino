// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.play.GameEngine;

/**
 * Player information
 */
public class NetPlayerInfo implements Serializable {
	/** Serial version */
	private static final long serialVersionUID = 1L;

	private static final int EXPORT_FIELD_COUNT = 27;

	/** Default rating for multiplayer games */
	public static final int DEFAULT_MULTIPLAYER_RATING = 1500;

	/** Name */
	public String strName = "";

	/** Country code */
	public String strCountry = "";

	/** Host */
	public String strHost = "";

	/** Team name */
	public String strTeam = "";

	/** Rules in use */
	public RuleOptions ruleOpt = null;

	/** Multiplayer rating */
	public int[] rating = new int[GameEngine.MAX_GAMESTYLE];

	/** Rating backup (internal use) */
	public int[] ratingBefore = new int[GameEngine.MAX_GAMESTYLE];

	/** Number of rated multiplayer games played */
	public int[] playCount = new int[GameEngine.MAX_GAMESTYLE];

	/** Number of games played in current room */
	public int playCountNow = 0;

	/** Number of rated multiplayer games win */
	public int[] winCount = new int[GameEngine.MAX_GAMESTYLE];

	/** Number of wins in current room */
	public int winCountNow = 0;

	/** Single player personal records */
	public NetSPPersonalBest spPersonalBest = new NetSPPersonalBest();

	/** User ID */
	public int uid = -1;

	/** Current room ID */
	public int roomID = -1;

	/** Game seat number (-1 if spectator) */
	public int seatID = -1;

	/** Join queue number (-1 if not in queue) */
	public int queueID = -1;

	/** true if "Ready" sign */
	public boolean ready = false;

	/** true if playing now */
	public boolean playing = false;

	/** true if connected */
	public boolean connected = false;

	/** true if this player is using tripcode */
	public boolean isTripUse = false;

	/** Real host name (for internal use) */
	public String strRealHost = "";

	/** Real IP (for internal use) */
	public String strRealIP = "";

	/** SocketChannel of this player (for internal use) */
	public SocketChannel channel = null;

	/**
	 * Constructor
	 */
	public NetPlayerInfo() {
	}

	/**
	 * Copy constructor
	 * @param n Copy source
	 */
	public NetPlayerInfo(NetPlayerInfo n) {
		copy(n);
	}

	/**
	 * String array constructor (Uses importStringArray)
	 * @param pdata String array (String[12])
	 */
	public NetPlayerInfo(String[] pdata) {
		importStringArray(pdata);
	}

	/**
	 * String constructor (Uses importString)
	 * @param str String(Divided by ;)
	 */
	public NetPlayerInfo(String str) {
		importString(str);
	}

	/**
	 * Copy from other NetPlayerInfo
	 * @param n Copy source
	 */
	public void copy(NetPlayerInfo n) {
		strName = n.strName;
		strCountry = n.strCountry;
		strHost = n.strHost;
		strTeam = n.strTeam;

		if(n.ruleOpt != null) {
			ruleOpt = new RuleOptions(n.ruleOpt);
		} else {
			ruleOpt = null;
		}

		rating = Arrays.copyOf(n.rating, GameEngine.MAX_GAMESTYLE);
		ratingBefore = Arrays.copyOf(n.ratingBefore, GameEngine.MAX_GAMESTYLE);
		playCount = Arrays.copyOf(n.playCount, GameEngine.MAX_GAMESTYLE);
		winCount = Arrays.copyOf(n.winCount, GameEngine.MAX_GAMESTYLE);
		spPersonalBest = new NetSPPersonalBest(n.spPersonalBest);

		playCountNow = n.playCountNow;
		winCountNow = n.winCountNow;

		uid = n.uid;
		roomID = n.roomID;
		seatID = n.seatID;
		queueID = n.queueID;
		ready = n.ready;
		playing = n.playing;
		connected = n.connected;
		isTripUse = n.isTripUse;
		strRealHost = n.strRealHost;
		strRealIP = n.strRealIP;
		channel = n.channel;
	}

	/**
	 * Import from String array
	 * @param pdata String array (String[27])
	 */
	public void importStringArray(String[] pdata) {
		NetStringArray.Reader reader = new NetStringArray.Reader(pdata);
		strName = reader.readEncoded();
		strCountry = reader.readEncoded();
		strHost = reader.readEncoded();
		strTeam = reader.readEncoded();
		roomID = reader.readInt();
		uid = reader.readInt();
		seatID = reader.readInt();
		queueID = reader.readInt();
		ready = reader.readBoolean();
		playing = reader.readBoolean();
		connected = reader.readBoolean();
		isTripUse = reader.readBoolean();
		readIntArray(reader, rating);
		readIntArray(reader, playCount);
		readIntArray(reader, winCount);
		String compressedPersonalBest = reader.read(null);
		if(compressedPersonalBest != null) {
			spPersonalBest.importString(NetUtil.decompressString(compressedPersonalBest));
		}
		playCountNow = reader.readInt(playCountNow);
		winCountNow = reader.readInt(winCountNow);
	}

	/**
	 * Import from String (Divided by ;)
	 * @param str String
	 */
	public void importString(String str) {
		importStringArray(str.split(";", -1));
	}

	/**
	 * Export to String array
	 * @return String array (String[27])
	 */
	public String[] exportStringArray() {
		NetStringArray.Writer writer = new NetStringArray.Writer(EXPORT_FIELD_COUNT);
		writer.writeEncoded(strName);
		writer.writeEncoded(strCountry);
		writer.writeEncoded(strHost);
		writer.writeEncoded(strTeam);
		writer.write(roomID);
		writer.write(uid);
		writer.write(seatID);
		writer.write(queueID);
		writer.write(ready);
		writer.write(playing);
		writer.write(connected);
		writer.write(isTripUse);
		writeIntArray(writer, rating);
		writeIntArray(writer, playCount);
		writeIntArray(writer, winCount);
		writer.write(NetUtil.compressString(spPersonalBest.exportString()));
		writer.write(playCountNow);
		writer.write(winCountNow);
		return writer.values();
	}

	/**
	 * Export to String (Divided by ;)
	 * @return String
	 */
	public String exportString() {
		return String.join(";", exportStringArray());
	}

	private static void readIntArray(NetStringArray.Reader reader, int[] target) {
		for(int i = 0; i < target.length; i++) {
			target[i] = reader.readInt();
		}
	}

	private static void writeIntArray(NetStringArray.Writer writer, int[] source) {
		for(int value : source) {
			writer.write(value);
		}
	}

	/**
	 * Reset play flags
	 */
	public void resetPlayState() {
		ready = false;
		playing = false;
	}

	/**
	 * Delete this player
	 */
	public void delete() {
		ruleOpt = null;
	}
}

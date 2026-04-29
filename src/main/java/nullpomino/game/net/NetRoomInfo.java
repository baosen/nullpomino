// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import nullpomino.game.component.RuleOptions;

/**
 * Room Information
 */
public class NetRoomInfo implements Serializable {
	/** Serial version */
	private static final long serialVersionUID = 1L;

	/** Identification number */
	public int roomID = -1;

	/** Room name */
	public String strName = "";

	/** Can participateMaximumPeoplecount */
	public int maxPlayers = 6;

	/** 自動開始までの待機 time */
	public int autoStartSeconds = 0;

	/** Fall velocity(Molecule) */
	public int gravity = 1;

	/** Fall velocity(Denominator) */
	public int denominator = 60;

	/** ARE */
	public int are = 30;

	/** ARE after line clear */
	public int areLine = 30;

	/** Line clear time */
	public int lineDelay = 40;

	/** Fixation time */
	public int lockDelay = 30;

	/** DAS */
	public int das = 14;

	/** Flag for types of T-Spins allowed (0=none, 1=normal, 2=all spin) */
	public int tspinEnableType = 1;

	/** Spin detection type */
	public static final int SPINTYPE_4POINT = 0, SPINTYPE_IMMOBILE = 1;

	public int spinCheckType = SPINTYPE_4POINT;

	/** Allow EZ-spins in spinCheckType 2 */
	public boolean tspinEnableEZ = false;

	/** Flag for enabling B2B */
	public boolean b2b = true;

	/** b2b adds as a separate garbage chunk */
	public boolean b2bChunk;

	/** Flag for enabling combos */
	public boolean combo = true;

	/** Allow Rensa/Combo Block */
	public boolean rensaBlock = true;

	/** Allow garbage countering */
	public boolean counter = true;

	/** Enable bravo bonus */
	public boolean bravo = true;

	/** Fixed rules flag */
	public boolean ruleLock = false;

	/** Rule name */
	public String ruleName = "";

	/** Rule */
	public RuleOptions ruleOpt = null;

	/** Have joinedNumber of players */
	public int playerSeatedCount = 0;

	/** Of people in the spectatorcount */
	public int spectatorCount = 0;

	/** Count of all the people in the room(During the war+While watching) */
	public int playerListCount = 0;

	/** Game flag */
	public boolean playing = false;

	/** Start gameRight afterNumber of players */
	public int startPlayers = 0;

	/** Death count */
	public int deadCount = 0;

	/** Automatically start timerWhen you are running thetrue */
	public boolean autoStartActive = false;

	/** SomeoneOKAfter I gave a displayCancelWastrue */
	public boolean isSomeoneCancelled = false;

	/** 3If I live more than Attack Reduce the force */
	public boolean reduceLineSend = false;

	/** Rate of change of garbage holes */
	public int garbagePercent = 100;

	/** Hole change style (false=line true=attack) */
	public boolean garbageChangePerAttack = true;

	/** Divide change rate by number of live players/teams to mimic feel of 1v1 */
	public boolean divideChangeRateByPlayers = false;

	/** Garbage send type (false=Send to all, true=Target) */
	public boolean isTarget = false;

	/** Targeting time */
	public int targetTimer = 60;

	/** HurryupSeconds before the startcount(-1InHurryupNo) */
	public int hurryupSeconds = -1;

	/** HurryupTimes afterBlockDo you run up the floor every time you put the */
	public int hurryupInterval = 5;

	/** Automatically start timer type(false=NullpoMino true=TNET2) */
	public boolean autoStartTNET2 = false;

	/** SomeoneOKAfter I gave a displayCancelWasTimerInvalidation */
	public boolean disableTimerAfterSomeoneCancelled = false;

	/** Map is enabled */
	public boolean useMap = false;

	/** LastMap */
	public int mapPrevious = -1;

	/** New fragmentsgarbage blockUsing the system */
	public boolean useFractionalGarbage = false;

	/** Mode name */
	public String strMode = "";

	/** Single player flag */
	public boolean singleplayer = false;

	/** Rated-game flag */
	public boolean rated = false;

	/** Custom rated-game flag */
	public boolean customRated = false;

	/** Game style */
	public int style = 0;

	/** Map list */
	public LinkedList<String> mapList = new LinkedList<String>();

	/** List of people in the room */
	public LinkedList<NetPlayerInfo> playerList = new LinkedList<NetPlayerInfo>();

	/** Game seat */
	public LinkedList<NetPlayerInfo> playerSeat = new LinkedList<NetPlayerInfo>();

	/** Game seat(Start gameI updated and new people will not change, even if someone or go out or come in only when) */
	public LinkedList<NetPlayerInfo> playerSeatNowPlaying = new LinkedList<NetPlayerInfo>();

	/** Queue */
	public LinkedList<NetPlayerInfo> playerQueue = new LinkedList<NetPlayerInfo>();

	/** Dead player list (Pushed from front, winner will be the first entry) */
	public LinkedList<NetPlayerInfo> playerSeatDead = new LinkedList<NetPlayerInfo>();

	/** Chat messages */
	public LinkedList<NetChatMessage> chatList = new LinkedList<NetChatMessage>();

	private static final int EXPORT_FIELD_COUNT = 43;

	/**
	 * Constructor
	 */
	public NetRoomInfo() {
	}

	/**
	 * Copy constructor
	 *
	 * @param n
	 *            Copy source
	 */
	public NetRoomInfo(NetRoomInfo n) {
		copy(n);
	}

	/**
	 * StringFrom an array of dataSubstituteConstructor
	 *
	 * @param rdata
	 *            StringAn array of(String[7])
	 */
	public NetRoomInfo(String[] rdata) {
		importStringArray(rdata);
	}

	/**
	 * StringFrom dataSubstituteConstructor
	 *
	 * @param str
	 *            String
	 */
	public NetRoomInfo(String str) {
		importString(str);
	}

	/**
	 * OtherNetRoomInfoCopied from the
	 *
	 * @param n
	 *            Copy source
	 */
	public void copy(NetRoomInfo n) {
		roomID = n.roomID;
		strName = n.strName;
		maxPlayers = n.maxPlayers;
		autoStartSeconds = n.autoStartSeconds;
		gravity = n.gravity;
		denominator = n.denominator;
		are = n.are;
		areLine = n.areLine;
		lineDelay = n.lineDelay;
		lockDelay = n.lockDelay;
		das = n.das;
		tspinEnableType = n.tspinEnableType;
		spinCheckType = n.spinCheckType;
		tspinEnableEZ = n.tspinEnableEZ;
		b2b = n.b2b;
		b2bChunk = n.b2bChunk;
		combo = n.combo;
		rensaBlock = n.rensaBlock;
		counter = n.counter;
		bravo = n.bravo;

		ruleLock = n.ruleLock;
		ruleName = n.ruleName;
		if (n.ruleOpt != null) {
			ruleOpt = new RuleOptions(n.ruleOpt);
		} else {
			ruleOpt = null;
		}

		playerSeatedCount = n.playerSeatedCount;
		spectatorCount = n.spectatorCount;
		playerListCount = n.playerListCount;
		playing = n.playing;
		startPlayers = n.startPlayers;
		deadCount = n.deadCount;
		autoStartActive = n.autoStartActive;
		isSomeoneCancelled = n.isSomeoneCancelled;
		reduceLineSend = n.reduceLineSend;
		hurryupSeconds = n.hurryupSeconds;
		hurryupInterval = n.hurryupInterval;
		autoStartTNET2 = n.autoStartTNET2;
		disableTimerAfterSomeoneCancelled = n.disableTimerAfterSomeoneCancelled;
		useMap = n.useMap;
		mapPrevious = n.mapPrevious;
		useFractionalGarbage = n.useFractionalGarbage;
		garbageChangePerAttack = n.garbageChangePerAttack;
		garbagePercent = n.garbagePercent;
		divideChangeRateByPlayers = n.divideChangeRateByPlayers;
		isTarget = n.isTarget;
		targetTimer = n.targetTimer;
		strMode = n.strMode;
		singleplayer = n.singleplayer;
		rated = n.rated;
		customRated = n.customRated;
		style = n.style;

		copyList(mapList, n.mapList);
		copyList(playerList, n.playerList);
		copyList(playerSeat, n.playerSeat);
		copyList(playerSeatNowPlaying, n.playerSeatNowPlaying);
		copyList(playerQueue, n.playerQueue);
		copyList(playerSeatDead, n.playerSeatDead);
		copyList(chatList, n.chatList);
	}

	private static <T> void copyList(LinkedList<T> target, LinkedList<T> source) {
		target.clear();
		target.addAll(source);
	}

	/**
	 * StringFrom an array of dataAssignment(PlayerExcept list)
	 *
	 * @param rdata
	 *            StringAn array of(String[43])
	 */
	public void importStringArray(String[] rdata) {
		NetStringArray.Reader reader = new NetStringArray.Reader(rdata);
		roomID = reader.readInt();
		strName = reader.readEncoded();
		maxPlayers = reader.readInt();
		playerSeatedCount = reader.readInt();
		spectatorCount = reader.readInt();
		playerListCount = reader.readInt();
		playing = reader.readBoolean();
		ruleLock = reader.readBoolean();
		ruleName = reader.readEncoded();
		autoStartSeconds = reader.readInt();
		gravity = reader.readInt();
		denominator = reader.readInt();
		are = reader.readInt();
		areLine = reader.readInt();
		lineDelay = reader.readInt();
		lockDelay = reader.readInt();
		das = reader.readInt();
		tspinEnableType = reader.readInt();
		b2b = reader.readBoolean();
		combo = reader.readBoolean();
		rensaBlock = reader.readBoolean();
		counter = reader.readBoolean();
		bravo = reader.readBoolean();
		reduceLineSend = reader.readBoolean();
		hurryupSeconds = reader.readInt();
		hurryupInterval = reader.readInt();
		autoStartTNET2 = reader.readBoolean();
		disableTimerAfterSomeoneCancelled = reader.readBoolean();
		useMap = reader.readBoolean();
		useFractionalGarbage = reader.readBoolean();
		garbageChangePerAttack = reader.readBoolean();
		garbagePercent = reader.readInt();
		spinCheckType = reader.readInt();
		tspinEnableEZ = reader.readBoolean();
		b2bChunk = reader.readBoolean();
		strMode = reader.readEncoded();
		singleplayer = reader.readBoolean();
		rated = reader.readBoolean();
		customRated = reader.readBoolean();
		style = reader.readInt();
		divideChangeRateByPlayers = reader.readBoolean();
		if(reader.hasNext()) isTarget = reader.readBoolean();
		if(reader.hasNext()) targetTimer = reader.readInt();
	}

	/**
	 * String(;Separated in)From dataAssignment(PlayerExcept list)
	 *
	 * @param str
	 *            String
	 */
	public void importString(String str) {
		importStringArray(str.split(";", -1));
	}

	/**
	 * StringConverts an array of(PlayerExcept list)
	 *
	 * @return StringAn array of(String[43])
	 */
	public String[] exportStringArray() {
		NetStringArray.Writer writer = new NetStringArray.Writer(EXPORT_FIELD_COUNT);
		writer.write(roomID);
		writer.writeEncoded(strName);
		writer.write(maxPlayers);
		writer.write(playerSeatedCount);
		writer.write(spectatorCount);
		writer.write(playerListCount);
		writer.write(playing);
		writer.write(ruleLock);
		writer.writeEncoded(ruleName);
		writer.write(autoStartSeconds);
		writer.write(gravity);
		writer.write(denominator);
		writer.write(are);
		writer.write(areLine);
		writer.write(lineDelay);
		writer.write(lockDelay);
		writer.write(das);
		writer.write(tspinEnableType);
		writer.write(b2b);
		writer.write(combo);
		writer.write(rensaBlock);
		writer.write(counter);
		writer.write(bravo);
		writer.write(reduceLineSend);
		writer.write(hurryupSeconds);
		writer.write(hurryupInterval);
		writer.write(autoStartTNET2);
		writer.write(disableTimerAfterSomeoneCancelled);
		writer.write(useMap);
		writer.write(useFractionalGarbage);
		writer.write(garbageChangePerAttack);
		writer.write(garbagePercent);
		writer.write(spinCheckType);
		writer.write(tspinEnableEZ);
		writer.write(b2bChunk);
		writer.writeEncoded(strMode);
		writer.write(singleplayer);
		writer.write(rated);
		writer.write(customRated);
		writer.write(style);
		writer.write(divideChangeRateByPlayers);
		writer.write(isTarget);
		writer.write(targetTimer);
		return writer.values();
	}

	/**
	 * StringConverted to(;Separated in)(PlayerExcept list)
	 *
	 * @return String
	 */
	public String exportString() {
		return String.join(";", exportStringArray());
	}

	/**
	 * Number of playersUpdate the count
	 */
	public void updatePlayerCount() {
		playerSeatedCount = getNumberOfPlayerSeated();
		playerListCount = playerList.size();
		spectatorCount = playerListCount - playerSeatedCount;
	}

	/**
	 * Those who are in the game now seatcountAcountObtained(nullSeat does not count)
	 *
	 * @return Those who are in the game now seatcount
	 */
	public int getNumberOfPlayerSeated() {
		return countPlayers(playerSeat, pInfo -> pInfo != null);
	}

	/**
	 * SpecifiedPlayerWhat is numberI look at the game you are in the seat of
	 *
	 * @param pInfo
	 *            Player
	 * @return Game seat number(If you do not have-1)
	 */
	public int getPlayerSeatNumber(NetPlayerInfo pInfo) {
		for (int i = 0; i < playerSeat.size(); i++) {
			if (playerSeat.get(i) == pInfo) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * @return If you put the seat immediately without waiting gametrue
	 */
	public boolean canJoinSeat() {
		return (getNumberOfPlayerSeated() < maxPlayers);
	}

	/**
	 * Entered the game seat
	 *
	 * @param pInfo
	 *            Player
	 * @return Game seat number(I were packed-1)
	 */
	public int joinSeat(NetPlayerInfo pInfo) {
		if (!canJoinSeat()) return -1;

		exitQueue(pInfo);
		for (int i = 0; i < playerSeat.size(); i++) {
			if (playerSeat.get(i) == null) {
				playerSeat.set(i, pInfo);
				return i;
			}
		}

		playerSeat.add(pInfo);
		return playerSeat.size() - 1;
	}

	/**
	 * SpecifiedPlayerRemove the seat from the game
	 *
	 * @param pInfo
	 *            Player
	 */
	public void exitSeat(NetPlayerInfo pInfo) {
		for (int i = 0; i < playerSeat.size(); i++) {
			if (playerSeat.get(i) == pInfo) {
				playerSeat.set(i, null);
			}
		}
	}

	/**
	 * Waiting to enter the
	 *
	 * @param pInfo
	 *            Player
	 * @return Waiting number
	 */
	public int joinQueue(NetPlayerInfo pInfo) {
		if (playerQueue.contains(pInfo)) {
			return playerQueue.indexOf(pInfo);
		}
		playerQueue.add(pInfo);
		return playerQueue.size() - 1;
	}

	/**
	 * SpecifiedPlayerRemoved from the waiting list
	 *
	 * @param pInfo
	 *            Player
	 */
	public void exitQueue(NetPlayerInfo pInfo) {
		playerQueue.remove(pInfo);
	}

	/**
	 * How manyPlayerDid you complete the preparationcountObtained
	 *
	 * @return Was readyNumber of players
	 */
	public int getHowManyPlayersReady() {
		return countPlayers(playerSeat, pInfo -> (pInfo != null) && pInfo.ready);
	}

	/**
	 * How manyPlayerOr is playingcountObtained(People have just come to the room and still does not include dead man)
	 *
	 * @return In playNumber of players
	 */
	public int getHowManyPlayersPlaying() {
		return countPlayers(playerSeatNowPlaying, this::isActiveSeatedPlayer);
	}

	/**
	 * I survived the lastPlayerGet information
	 *
	 * @return I survived the lastPlayerInformation(Yet2Or if you live more than, If I do not start the game in the first place isnull)
	 */
	public NetPlayerInfo getWinner() {
		if (isStartedMultiplayerGame() && (getHowManyPlayersPlaying() < 2)) {
			for (NetPlayerInfo pInfo : playerSeatNowPlaying) {
				if (isConnectedActiveSeatedPlayer(pInfo)) return pInfo;
			}
		}
		return null;
	}

	/**
	 * I survived the lastTeam nameGet the
	 *
	 * @return I survived the lastTeam name
	 */
	public String getWinnerTeam() {
		if (isStartedMultiplayerGame() && (getHowManyPlayersPlaying() >= 2)) {
			for (NetPlayerInfo pInfo : playerSeatNowPlaying) {
				if (isConnectedActiveSeatedPlayer(pInfo)) {
					if (pInfo.strTeam.length() <= 0) {
						return null;
					} else {
						return pInfo.strTeam;
					}
				}
			}
		}

		return null;
	}

	/**
	 * @return 1If only one team has survivedtrue
	 */
	public boolean isTeamWin() {
		String teamname = null;

		if (isStartedMultiplayerGame() && (getHowManyPlayersPlaying() >= 2)) {
			for (NetPlayerInfo pInfo : playerSeatNowPlaying) {
				if (isConnectedActiveSeatedPlayer(pInfo)) {
					if (pInfo.strTeam.length() <= 0) {
						return false;
					} else if (teamname == null) {
						teamname = pInfo.strTeam;
					} else if (!teamname.equals(pInfo.strTeam)) {
						return false;
					}
				}
			}
		}

		return (teamname != null);
	}

	/**
	 * @return true if it's a team game
	 */
	public boolean isTeamGame() {
		return hasDuplicateStartedPlayerValue(pInfo -> pInfo.strTeam);
	}

	/**
	 * @return true if 2 or more people have same IP
	 */
	public boolean hasSameIPPlayers() {
		return hasDuplicateStartedPlayerValue(pInfo -> pInfo.strRealIP);
	}

	/**
	 * Start gameCall processing at
	 */
	public void gameStart() {
		updatePlayerCount();
		playerSeatNowPlaying.clear();
		playerSeatNowPlaying.addAll(playerSeat);
		playerSeatDead.clear();
		chatList.clear();
		startPlayers = playerSeatedCount;
		deadCount = 0;
		autoStartActive = false;
		isSomeoneCancelled = false;
	}

	/**
	 * What Happens When erasing Room
	 */
	public void delete() {
		ruleOpt = null;
		mapList.clear();
		playerList.clear();
		playerSeat.clear();
		playerSeatNowPlaying.clear();
		playerQueue.clear();
		playerSeatDead.clear();
		chatList.clear();
	}

	private boolean isActiveSeatedPlayer(NetPlayerInfo pInfo) {
		return (pInfo != null) && pInfo.playing && playerSeat.contains(pInfo);
	}

	private boolean isConnectedActiveSeatedPlayer(NetPlayerInfo pInfo) {
		return isActiveSeatedPlayer(pInfo) && pInfo.connected;
	}

	private boolean isStartedMultiplayerGame() {
		return (startPlayers >= 2) && playing;
	}

	private static int countPlayers(Iterable<NetPlayerInfo> players,
			Predicate<NetPlayerInfo> predicate) {
		int count = 0;
		for(NetPlayerInfo pInfo : players) {
			if(predicate.test(pInfo)) count++;
		}
		return count;
	}

	private boolean hasDuplicateStartedPlayerValue(Function<NetPlayerInfo, String> valueFn) {
		if(startPlayers < 2) return false;

		Set<String> values = new HashSet<String>();
		for(NetPlayerInfo pInfo : playerSeatNowPlaying) {
			if(pInfo != null) {
				String value = valueFn.apply(pInfo);
				if((value.length() > 0) && !values.add(value)) return true;
			}
		}
		return false;
	}
}

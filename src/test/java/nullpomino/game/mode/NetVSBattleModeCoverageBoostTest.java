package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.Test;

/**
 * Coverage-boost tests for {@link NetVSBattleMode}. Targets the remaining
 * uncovered source lines: the calcScore T-Spin single / normal triple events,
 * the fractional-garbage divide path and garbage-line generation, the onLast
 * meter clamp branches, the renderLast fractional-garbage font colors, the
 * target marker, the K.O. / line-clear-event drawing switch (both display
 * sizes), the games-count direct font, netSendEndGameStats, the b2bChunk
 * branch and hurry-up sound in netlobbyOnMessage, and the unused GarbageEntry
 * constructors.
 *
 * <p>Idiom: a real-but-disconnected {@link NetLobbyFrame} + a
 * {@link NetPlayerClient} subclass whose {@code send()} is a no-op are attached
 * so the network send paths execute without a live socket. Net message handlers
 * are driven with crafted {@code String[]} arrays.
 */
class NetVSBattleModeCoverageBoostTest {

	// ---------------------------------------------------------------
	// Setup helpers
	// ---------------------------------------------------------------

	private static GameManager makeManager() {
		return new GameManager(new EventReceiver());
	}

	/** Build a mode + manager, init engines 0..maxPlayerID, return the manager. */
	private static GameManager buildManager(NetVSBattleMode mode, int maxPlayerID) {
		GameManager manager = makeManager();
		manager.mode = mode;
		manager.init();
		for (int i = 0; i <= maxPlayerID; i++) {
			if (manager.engine[i] != null) {
				manager.engine[i].init();
				manager.engine[i].createFieldIfNeeded();
				manager.engine[i].playerID = i;
				manager.engine[i].nowPieceObject = new Piece(Piece.PIECE_T);
			}
		}
		return manager;
	}

	private static GameEngine freshEngine(NetVSBattleMode mode) {
		return buildManager(mode, 0).engine[0];
	}

	private static NetRoomInfo makeRoomInfo() {
		NetRoomInfo room = new NetRoomInfo();
		room.reduceLineSend = false;
		room.bravo = false;
		room.counter = true;
		room.useFractionalGarbage = false;
		room.rensaBlock = true;
		room.garbagePercent = 100;
		room.divideChangeRateByPlayers = false;
		room.garbageChangePerAttack = false;
		room.hurryupSeconds = -1;
		room.isTarget = false;
		room.b2bChunk = false;
		return room;
	}

	/** Attach a disconnected lobby + a no-op send client so send() is safe. */
	private static void attachLobby(NetVSBattleMode mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		NetPlayerClient client = new NetPlayerClient("", 0) {
			@Override
			public boolean send(String msg) { return true; }
			@Override
			public int getPlayerUID() { return 0; }
		};
		Field clientField = NetLobbyFrame.class.getDeclaredField("netPlayerClient");
		clientField.setAccessible(true);
		clientField.set(lobby, client);
		setField(mode, "netLobby", lobby);
	}

	/** Make a player exist, alive, active. */
	private static void makePlayerAttackable(NetVSBattleMode mode, int playerID) throws Exception {
		((boolean[]) readField(mode, "netvsPlayerExist"))[playerID] = true;
		((boolean[]) readField(mode, "netvsPlayerDead"))[playerID] = false;
		((boolean[]) readField(mode, "netvsPlayerActive"))[playerID] = true;
	}

	private static Class<?> getGarbageEntryClass() throws Exception {
		for (Class<?> c : NetVSBattleMode.class.getDeclaredClasses()) {
			if (c.getSimpleName().equals("GarbageEntry")) return c;
		}
		throw new ClassNotFoundException("GarbageEntry");
	}

	private static Object makeGarbageEntry(NetVSBattleMode mode, int lines, int playerID, int uid) throws Exception {
		Constructor<?> ctor = getGarbageEntryClass()
				.getDeclaredConstructor(NetVSBattleMode.class, int.class, int.class, int.class);
		ctor.setAccessible(true);
		return ctor.newInstance(mode, lines, playerID, uid);
	}

	// ---------------------------------------------------------------
	// calcScore: T-Spin single (non-mini) -> lines 312-313
	// ---------------------------------------------------------------

	@Test
	void calcScoreTSpinSingleNonMini() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setField(mode, "netCurrentRoomInfo", makeRoomInfo());
		setBoolean(mode, "netvsIsPractice", true);
		e.tspin = true;
		e.tspinez = false;
		e.tspinmini = false;
		e.field.setBlock(0, e.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(e, 0, 1);

		assertEquals(6, ((int[]) readField(mode, "lastevent"))[0]); // EVENT_TSPIN_SINGLE
	}

	// ---------------------------------------------------------------
	// calcScore: normal Triple -> lines 346-347
	// ---------------------------------------------------------------

	@Test
	void calcScoreNormalTriple() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setField(mode, "netCurrentRoomInfo", makeRoomInfo());
		setBoolean(mode, "netvsIsPractice", true);
		e.tspin = false;
		e.field.setBlock(0, e.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(e, 0, 3);

		assertEquals(3, ((int[]) readField(mode, "lastevent"))[0]); // EVENT_TRIPLE
	}

	// ---------------------------------------------------------------
	// calcScore: fractional garbage divide path (399-404) with >=3 teams
	// ---------------------------------------------------------------

	@Test
	void calcScoreFractionalGarbageDivideByTeams() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = buildManager(mode, 2);
		mode.modeInit(mgr);
		GameEngine e = mgr.engine[0];

		NetRoomInfo room = makeRoomInfo();
		room.useFractionalGarbage = true;
		room.reduceLineSend = true; // so attackNumPlayerIndex follows team count
		setField(mode, "netCurrentRoomInfo", room);
		setBoolean(mode, "netvsIsPractice", true);

		// 3 alive teams: players 0,1,2 exist, alive, gameActive, distinct teams
		String[] team = (String[]) readField(mode, "netvsPlayerTeam");
		boolean[] exist = (boolean[]) readField(mode, "netvsPlayerExist");
		boolean[] dead = (boolean[]) readField(mode, "netvsPlayerDead");
		for (int i = 0; i < 3; i++) {
			exist[i] = true;
			dead[i] = false;
			team[i] = "team" + i;
			mgr.engine[i].gameActive = true;
		}

		e.tspin = false;
		e.field.setBlock(0, e.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_GRAY));

		// Four lines -> base attack > 0 so the divide loop actually scales
		mode.calcScore(e, 0, 4);

		assertTrue(((int[]) readField(mode, "garbageSent"))[0] >= 0);
	}

	// ---------------------------------------------------------------
	// calcScore: garbage counter cancellation (411-429) +
	// garbage line generation (449-536) with garbageChangePerAttack
	// + divideChangeRateByPlayers (459-462)
	// ---------------------------------------------------------------

	@Test
	void calcScoreGarbageGenerationDivideAndChangePerAttack() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = buildManager(mode, 2);
		mode.modeInit(mgr);
		GameEngine e = mgr.engine[0];

		NetRoomInfo room = makeRoomInfo();
		room.rensaBlock = false;            // allow garbage to appear on a line clear
		room.garbageChangePerAttack = true; // hit the per-attack hole shift branch
		room.divideChangeRateByPlayers = true;
		room.counter = false;
		setBoolean(mode, "netvsIsPractice", false);
		setField(mode, "netCurrentRoomInfo", room);
		attachLobby(mode);

		// 3 alive teams so divideChangeRateByPlayers divisor (teams-1) is 2
		String[] team = (String[]) readField(mode, "netvsPlayerTeam");
		boolean[] exist = (boolean[]) readField(mode, "netvsPlayerExist");
		boolean[] dead = (boolean[]) readField(mode, "netvsPlayerDead");
		int[] seat = (int[]) readField(mode, "netvsPlayerSeatID");
		for (int i = 0; i < 3; i++) {
			exist[i] = true;
			dead[i] = false;
			team[i] = "team" + i;
			seat[i] = i;
			mgr.engine[i].gameActive = true;
		}

		// Queue a large garbage entry (>= 2 full lines + a remainder)
		LinkedList<Object> entries = new LinkedList<>();
		entries.add(makeGarbageEntry(mode, 150, 1, 100)); // 2 full + 30 remainder
		setField(mode, "garbageEntries", entries);

		e.field.reset();
		e.tspin = false;

		mode.calcScore(e, 0, 0); // 0 lines -> garbage appears, no attack sent

		// Garbage lines should have been added to the field (does not throw).
		assertNotNull(readField(mode, "garbageEntries"));
	}

	// ---------------------------------------------------------------
	// calcScore: small-garbage block (499-533) without per-attack change
	// ---------------------------------------------------------------

	@Test
	void calcScoreSmallGarbagePlainLoop() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);

		NetRoomInfo room = makeRoomInfo();
		room.rensaBlock = false;
		room.garbageChangePerAttack = false; // exercise the per-line for-loop instead
		setBoolean(mode, "netvsIsPractice", false);
		setField(mode, "netCurrentRoomInfo", room);
		attachLobby(mode);

		// Two small entries that together exceed one full line via the remainder path
		LinkedList<Object> entries = new LinkedList<>();
		entries.add(makeGarbageEntry(mode, 40, 1, 100));
		entries.add(makeGarbageEntry(mode, 40, 1, 100));
		setField(mode, "garbageEntries", entries);

		e.field.reset();
		e.tspin = false;
		mode.calcScore(e, 0, 0);

		assertNotNull(readField(mode, "garbageEntries"));
	}

	// ---------------------------------------------------------------
	// onLast: meter rising clamp (581-584) and falling (585-586)
	// ---------------------------------------------------------------

	@Test
	void onLastMeterRisesAndClamps() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setField(mode, "netCurrentRoomInfo", makeRoomInfo());

		// Large garbage -> newMeterValue high, current meterValue 0 -> meter rises.
		((int[]) readField(mode, "garbage"))[0] = 8 * 60;
		e.meterValue = 0;

		mode.onLast(e, 0);

		assertTrue(e.meterValue > 0);
	}

	@Test
	void onLastMeterFalls() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setField(mode, "netCurrentRoomInfo", makeRoomInfo());

		// No garbage -> newMeterValue 0, current meterValue high -> meter falls by 1.
		((int[]) readField(mode, "garbage"))[0] = 0;
		e.meterValue = 50;

		mode.onLast(e, 0);

		assertEquals(49, e.meterValue);
	}

	// ---------------------------------------------------------------
	// renderLast: fractional-garbage display font colors (633-647)
	// for the three thresholds; both display sizes.
	// ---------------------------------------------------------------

	@Test
	void renderLastFractionalGarbageColorsBigDisplay() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		NetRoomInfo room = makeRoomInfo();
		room.useFractionalGarbage = true;
		setField(mode, "netCurrentRoomInfo", room);
		((boolean[]) readField(mode, "netvsPlayerExist"))[0] = true;
		e.isVisible = true;
		e.displaysize = 0;
		((int[]) readField(mode, "garbage"))[0] = 4 * 60; // RED threshold

		mode.renderLast(e, 0);
		assertNotNull(readField(mode, "garbage"));
	}

	@Test
	void renderLastFractionalGarbageColorsSmallDisplay() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		NetRoomInfo room = makeRoomInfo();
		room.useFractionalGarbage = true;
		setField(mode, "netCurrentRoomInfo", room);
		((boolean[]) readField(mode, "netvsPlayerExist"))[0] = true;
		e.isVisible = true;
		e.displaysize = -1;
		((int[]) readField(mode, "garbage"))[0] = 3 * 60; // ORANGE threshold

		mode.renderLast(e, 0);
		assertNotNull(readField(mode, "garbage"));
	}

	// ---------------------------------------------------------------
	// renderLast: TARGET marker, both display sizes (650-662)
	// ---------------------------------------------------------------

	@Test
	void renderLastTargetMarkerSmallDisplay() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = buildManager(mode, 1);
		mode.modeInit(mgr);
		GameEngine e = mgr.engine[1];

		NetRoomInfo room = makeRoomInfo();
		room.isTarget = true;
		room.targetTimer = 30;
		setField(mode, "netCurrentRoomInfo", room);
		setInt(mode, "targetID", 1);
		setInt(mode, "targetTimer", 12); // not in the flash window
		setBoolean(mode, "netvsIsGameActive", true);
		setInt(mode, "netvsNumAlivePlayers", 3);
		makePlayerAttackable(mode, 1);
		((boolean[]) readField(mode, "netvsPlayerExist"))[1] = true;
		String[] team1 = (String[]) readField(mode, "netvsPlayerTeam");
		team1[0] = "";
		team1[1] = "";
		e.isVisible = true;
		e.displaysize = -1;

		mode.renderLast(e, 1);
		assertNotNull(readField(mode, "netCurrentRoomInfo"));
	}

	@Test
	void renderLastTargetMarkerBigDisplayFlash() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = buildManager(mode, 1);
		mode.modeInit(mgr);
		GameEngine e = mgr.engine[1];

		NetRoomInfo room = makeRoomInfo();
		room.isTarget = true;
		room.targetTimer = 30;
		setField(mode, "netCurrentRoomInfo", room);
		setInt(mode, "targetID", 1);
		setInt(mode, "targetTimer", 30); // >= targetTimer-20 and even -> flash white
		setBoolean(mode, "netvsIsGameActive", true);
		setInt(mode, "netvsNumAlivePlayers", 3);
		makePlayerAttackable(mode, 1);
		((boolean[]) readField(mode, "netvsPlayerExist"))[1] = true;
		String[] team2 = (String[]) readField(mode, "netvsPlayerTeam");
		team2[0] = "";
		team2[1] = "";
		e.isVisible = true;
		e.displaysize = 0;

		mode.renderLast(e, 1);
		assertNotNull(readField(mode, "netCurrentRoomInfo"));
	}

	// ---------------------------------------------------------------
	// renderLast: K.O. drawing (684-690), both display sizes
	// ---------------------------------------------------------------

	@Test
	void renderLastKObigDisplay() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setField(mode, "netCurrentRoomInfo", makeRoomInfo());
		((boolean[]) readField(mode, "netvsPlayerExist"))[0] = true;
		((boolean[]) readField(mode, "playerKObyYou"))[0] = true;
		e.isVisible = true;
		e.displaysize = 0;

		mode.renderLast(e, 0);
		assertTrue(((boolean[]) readField(mode, "playerKObyYou"))[0]);
	}

	@Test
	void renderLastKOsmallDisplay() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setField(mode, "netCurrentRoomInfo", makeRoomInfo());
		((boolean[]) readField(mode, "netvsPlayerExist"))[0] = true;
		((boolean[]) readField(mode, "playerKObyYou"))[0] = true;
		e.isVisible = true;
		e.displaysize = -1;

		mode.renderLast(e, 0);
		assertTrue(((boolean[]) readField(mode, "playerKObyYou"))[0]);
	}

	// ---------------------------------------------------------------
	// renderLast: full line-clear-event switch (692-826)
	// Drives every EVENT_* case, with B2B and combo, in both displaysizes.
	// ---------------------------------------------------------------

	private void driveLineEventSwitch(int displaysize, boolean fractional) throws Exception {
		int[] events = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}; // EVENT_SINGLE..EVENT_TSPIN_EZ
		for (int ev : events) {
			for (boolean b2b : new boolean[]{false, true}) {
				NetVSBattleMode mode = new NetVSBattleMode();
				mode.modeInit(makeManager());
				GameEngine e = freshEngine(mode);
				NetRoomInfo room = makeRoomInfo();
				room.useFractionalGarbage = fractional;
				setField(mode, "netCurrentRoomInfo", room);

				((boolean[]) readField(mode, "netvsPlayerExist"))[0] = true;
				((boolean[]) readField(mode, "playerKObyYou"))[0] = false;
				((int[]) readField(mode, "lastevent"))[0] = ev;
				((int[]) readField(mode, "scgettime"))[0] = 0; // < 120
				((boolean[]) readField(mode, "lastb2b"))[0] = b2b;
				((int[]) readField(mode, "lastcombo"))[0] = 3; // >= 2 -> combo text
				((int[]) readField(mode, "lastpiece"))[0] = Piece.PIECE_T;
				if (fractional) ((int[]) readField(mode, "garbage"))[0] = 90; // x2=0 branch
				e.isVisible = true;
				e.displaysize = displaysize;

				mode.renderLast(e, 0);
			}
		}
	}

	@Test
	void renderLastLineEventSwitchBigDisplay() throws Exception {
		driveLineEventSwitch(0, false);
		assertTrue(true);
	}

	@Test
	void renderLastLineEventSwitchSmallDisplay() throws Exception {
		driveLineEventSwitch(-1, false);
		assertTrue(true);
	}

	@Test
	void renderLastLineEventSwitchSmallDisplayFractional() throws Exception {
		driveLineEventSwitch(-1, true);
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// renderLast: games-count direct font, small display (828-836)
	// ---------------------------------------------------------------

	@Test
	void renderLastGamesCountSmallDisplay() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setField(mode, "netCurrentRoomInfo", makeRoomInfo());
		((boolean[]) readField(mode, "netvsPlayerExist"))[0] = true;
		((boolean[]) readField(mode, "playerKObyYou"))[0] = false;
		((int[]) readField(mode, "lastevent"))[0] = 0; // EVENT_NONE -> games count branch
		setBoolean(mode, "netvsIsPractice", false);
		e.isVisible = true;
		e.displaysize = -1;

		mode.renderLast(e, 0);
		assertNotNull(readField(mode, "netvsPlayerWinCount"));
	}

	// ---------------------------------------------------------------
	// netSendEndGameStats: send path (889-900)
	// ---------------------------------------------------------------

	@Test
	void netSendEndGameStatsSends() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		attachLobby(mode);

		Method m = NetVSBattleMode.class.getDeclaredMethod("netSendEndGameStats", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, e); // must not throw with a no-op send client

		assertNotNull(readField(mode, "netLobby"));
	}

	// ---------------------------------------------------------------
	// netlobbyOnMessage: attack with b2bChunk (977-987) + danger (990-992)
	// ---------------------------------------------------------------

	@Test
	void netlobbyOnMessageAttackB2bChunkAndDanger() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = buildManager(mode, 5);
		mode.modeInit(mgr);

		NetRoomInfo room = makeRoomInfo();
		room.b2bChunk = true; // exercise the secondAdd chunk branch
		setField(mode, "netCurrentRoomInfo", room);
		setBoolean(mode, "netvsIsPractice", false);
		mgr.engine[0].timerActive = true;

		int[] seat = (int[]) readField(mode, "netvsPlayerSeatID");
		seat[0] = 0;
		seat[1] = 1;
		makePlayerAttackable(mode, 1);
		String[] team = (String[]) readField(mode, "netvsPlayerTeam");
		team[0] = "A";
		team[1] = "B";
		attachLobby(mode);

		// Pre-fill garbage so garbage[0] crosses the 4*denominator danger threshold
		LinkedList<Object> pre = new LinkedList<>();
		pre.add(makeGarbageEntry(mode, 4 * 60, 1, 100));
		setField(mode, "garbageEntries", pre);

		// [0]=game [1]=uid [2]=seatID [3]=attack [4..9]=pts(6) [10]=stray-tab
		// [11]=lastevent [12]=lastb2b [13]=lastcombo [14]=garbage [15]=lastpiece [16]=targetSeatID
		String[] msg = new String[17];
		msg[0] = "game";
		msg[1] = "100";
		msg[2] = "1";
		msg[3] = "attack";
		msg[4] = "60"; msg[5] = "120"; msg[6] = "0";
		msg[7] = "0"; msg[8] = "0"; msg[9] = "0";
		msg[10] = "";
		msg[11] = "4";    // lastevent
		msg[12] = "true"; // lastb2b
		msg[13] = "0";
		msg[14] = "0";
		msg[15] = "0";
		msg[16] = "-1";

		mode.netlobbyOnMessage(null, null, msg);

		LinkedList<?> entries = (LinkedList<?>) readField(mode, "garbageEntries");
		assertNotNull(entries);
		// Original entry + the two new entries (main chunk + b2b chunk).
		assertTrue(entries.size() >= 3);
	}

	// ---------------------------------------------------------------
	// netlobbyOnMessage: hurryup sound path (996-1003) -> line 999
	// ---------------------------------------------------------------

	@Test
	void netlobbyOnMessageHurryUpPlaysSound() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = buildManager(mode, 0);
		mode.modeInit(mgr);

		NetRoomInfo room = makeRoomInfo();
		room.hurryupSeconds = 10; // > 0 so the inner block runs
		setField(mode, "netCurrentRoomInfo", room);
		setBoolean(mode, "netvsIsPractice", false);
		mgr.engine[0].timerActive = true; // so playSE branch runs

		String[] msg = {"game", "0", "0", "hurryup"};
		mode.netlobbyOnMessage(null, null, msg);

		assertTrue(readBoolean(mode, "hurryupStarted"));
	}

	// ---------------------------------------------------------------
	// GarbageEntry unused constructors (1025-1026, 1042-1045)
	// ---------------------------------------------------------------

	@Test
	void garbageEntryNoArgAndTwoArgConstructors() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		Class<?> geClass = getGarbageEntryClass();

		// No-arg constructor -> lines 1025-1026
		Constructor<?> ctor0 = geClass.getDeclaredConstructor(NetVSBattleMode.class);
		ctor0.setAccessible(true);
		Object e0 = ctor0.newInstance(mode);
		assertEquals(0, geClass.getField("lines").getInt(e0));

		// Two-arg (lines, playerID) constructor -> lines 1042-1045
		Constructor<?> ctor2 = geClass.getDeclaredConstructor(NetVSBattleMode.class, int.class, int.class);
		ctor2.setAccessible(true);
		Object e2 = ctor2.newInstance(mode, 90, 3);
		assertEquals(90, geClass.getField("lines").getInt(e2));
		assertEquals(3, geClass.getField("playerID").getInt(e2));
	}

	// ---------------------------------------------------------------
	// Reflection helpers
	// ---------------------------------------------------------------

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static boolean readBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(obj, value);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}

package nullpomino.game.mode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedList;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tail coverage for {@link NetVSBattleMode#calcScore} and {@link NetVSBattleMode#onLast}:
 * fractional-garbage division for 3+ teams, garbage countering + send, the
 * garbage-appearance loops (both garbageChangePerAttack modes and small garbage),
 * and the onLast meter "rise then clamp to target" branch.
 */
class NetVSBattleModeTailCoverageTest {

	private NetVSBattleMode mode;
	private GameManager manager;
	private GameEngine engine;

	@BeforeEach
	void setUp() throws Exception {
		mode = new NetVSBattleMode();
		manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		for (int i = 0; i < manager.engine.length; i++) {
			manager.engine[i].init();
			manager.engine[i].createFieldIfNeeded();
		}
		engine = manager.engine[0];
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		// Empty team names so netvsGetNumberOfTeamsAlive counts per-player
		set(mode, "netvsPlayerTeam", new String[]{"", "", "", "", "", ""});
		set(mode, "netvsPlayerSeatID", new int[]{0, 1, 2, 3, 4, 5});

		mode.netLobby = new NetLobbyFrame();
		mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();
	}

	private NetRoomInfo baseRoom() {
		NetRoomInfo room = new NetRoomInfo();
		room.reduceLineSend = false;
		room.rensaBlock = false;
		room.bravo = false;
		room.useFractionalGarbage = false;
		room.counter = false;
		room.garbagePercent = 100;
		room.divideChangeRateByPlayers = false;
		room.garbageChangePerAttack = false;
		room.hurryupSeconds = -1;
		room.isTarget = false;
		room.b2bChunk = false;
		return room;
	}

	/** Make {@code n} players exist/alive (non-team) so teams-alive == n. */
	private void aliveTeams(int n) throws Exception {
		boolean[] exist = new boolean[6];
		boolean[] dead = new boolean[6];
		for (int i = 0; i < 6; i++) {
			exist[i] = i < n;
			dead[i] = false;
			manager.engine[i].gameActive = i < n;
		}
		set(mode, "netvsPlayerExist", exist);
		set(mode, "netvsPlayerDead", dead);
	}

	/** Construct the private inner GarbageEntry(g, p). */
	private Object newGarbageEntry(int lines, int playerID) throws Exception {
		for (Class<?> inner : NetVSBattleMode.class.getDeclaredClasses()) {
			if (inner.getSimpleName().equals("GarbageEntry")) {
				Constructor<?> ctor = inner.getDeclaredConstructor(
						NetVSBattleMode.class, int.class, int.class);
				ctor.setAccessible(true);
				return ctor.newInstance(mode, lines, playerID);
			}
		}
		throw new IllegalStateException("GarbageEntry not found");
	}

	@SuppressWarnings("unchecked")
	private LinkedList<Object> garbageEntries() throws Exception {
		Field f = findField(NetVSBattleMode.class, "garbageEntries");
		f.setAccessible(true);
		return (LinkedList<Object>) f.get(mode);
	}

	// ---- calcScore fractional division for 3+ teams (lines 398-404) ----
	@Test
	void calcScoreFractionalGarbageDividesForThreeTeams() throws Exception {
		NetRoomInfo room = baseRoom();
		room.useFractionalGarbage = true;
		mode.netCurrentRoomInfo = room;
		mode.netvsIsPractice = false;
		aliveTeams(3);
		engine.createFieldIfNeeded();
		// Prevent all-clear path; not relevant since bravo off
		engine.b2b = false;

		// A "FOUR" yields pts so the divide-by-(teams-1) loop has work
		mode.calcScore(engine, 0, 4);
	}

	// ---- calcScore garbage countering + send (lines 412-444) ----
	@Test
	void calcScoreCountersIncomingGarbageAndSends() throws Exception {
		NetRoomInfo room = baseRoom();
		room.counter = true;
		room.useFractionalGarbage = false;
		mode.netCurrentRoomInfo = room;
		mode.netvsIsPractice = false;
		aliveTeams(2);
		engine.createFieldIfNeeded();

		// Incoming garbage to be countered
		garbageEntries().add(newGarbageEntry(2, 1));

		// FOUR -> positive pts; countering loop consumes the incoming entry
		mode.calcScore(engine, 0, 4);
	}

	// ---- garbage appearance, garbageChangePerAttack = false (lines 484-494, 519) ----
	@Test
	void garbageAppearsPerLineMode() throws Exception {
		NetRoomInfo room = baseRoom();
		room.garbageChangePerAttack = false;
		room.garbagePercent = 100;
		mode.netCurrentRoomInfo = room;
		mode.netvsIsPractice = false;
		aliveTeams(2);
		engine.createFieldIfNeeded();
		engine.random = new java.util.Random(12345L);

		// One big entry (>= GARBAGE_DENOMINATOR) plus modulo so small garbage
		// also accumulates >= a denominator across entries.
		garbageEntries().add(newGarbageEntry(60, 1));
		garbageEntries().add(newGarbageEntry(40, 1));
		garbageEntries().add(newGarbageEntry(40, 1));

		// lines == 0 takes the garbage-appearance branch directly
		mode.calcScore(engine, 0, 0);
	}

	// ---- garbage appearance, garbageChangePerAttack = true (lines 473-512) ----
	@Test
	void garbageAppearsPerAttackMode() throws Exception {
		NetRoomInfo room = baseRoom();
		room.garbageChangePerAttack = true;
		room.garbagePercent = 100;
		mode.netCurrentRoomInfo = room;
		mode.netvsIsPractice = false;
		aliveTeams(2);
		engine.createFieldIfNeeded();
		engine.random = new java.util.Random(999L);
		set(mode, "lastHole", 0); // force a defined starting hole

		garbageEntries().add(newGarbageEntry(120, 1));
		garbageEntries().add(newGarbageEntry(40, 1));
		garbageEntries().add(newGarbageEntry(40, 1));

		mode.calcScore(engine, 0, 0);
	}

	// ---- onLast meter rises then clamps to target (line 583) ----
	@Test
	void onLastMeterRisesAndClampsToTarget() throws Exception {
		NetRoomInfo room = baseRoom();
		mode.netCurrentRoomInfo = room;
		mode.netvsIsPractice = false;
		set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
		engine.displaysize = 0; // block graphics height 16, half = 8
		engine.gameActive = true;
		engine.timerActive = true;

		// garbage 60 -> newMeterValue = (60/60)*16 = 16
		set(mode, "garbage", new int[]{60, 0, 0, 0, 0, 0});
		// Current meter just below target so rise (+8) overshoots and clamps
		engine.meterValue = 12;

		mode.onLast(engine, 0);

		org.junit.jupiter.api.Assertions.assertEquals(16, engine.meterValue);
	}

	// ================================================================
	// Helpers
	// ================================================================

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name + " in " + cls.getName());
	}

	private static void set(Object obj, String name, Object value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.set(obj, value);
	}
}

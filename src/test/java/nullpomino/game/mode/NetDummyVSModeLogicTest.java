package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.LinkedList;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the logic methods on {@link NetDummyVSMode} and {@link NetDummyMode}
 * that do not require a live netplay session: watch detection, seat-ID
 * resolution, team/alive counting, attackability, goal-type default,
 * and ranking-view/send guards.
 */
class NetDummyVSModeLogicTest {

	private TestableNetDummyVSMode mode;
	private GameManager manager;

	/**
	 * Testable subclass that exposes protected methods as public so tests
	 * can invoke them directly without reflection.
	 */
	static class TestableNetDummyVSMode extends NetDummyVSMode {
		@Override
		public boolean netvsIsWatch() { return super.netvsIsWatch(); }

		public int netvsGetPlayerIDbySeatID(int seat, int myseat) {
			return super.netvsGetPlayerIDbySeatID(seat, myseat);
		}

		public int netvsGetNumberOfTeamsAlive() { return super.netvsGetNumberOfTeamsAlive(); }

		public boolean netvsIsAttackable(int playerID) { return super.netvsIsAttackable(playerID); }

		public int netGetGoalType() { return super.netGetGoalType(); }

		public boolean netIsNetRankingViewOK(GameEngine engine) { return super.netIsNetRankingViewOK(engine); }

		public boolean netIsNetRankingSendOK(GameEngine engine) { return super.netIsNetRankingSendOK(engine); }
	}

	@BeforeEach
	void setUp() throws Exception {
		mode = new TestableNetDummyVSMode();
		manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		for (int i = 0; i < manager.engine.length; i++) {
			manager.engine[i].init();
		}
		// modeInit() allocates netvsPlayerTeam / netvsPlayerName arrays
		// but elements default to null.  The real netUpdatePlayerExist()
		// fills them with "" — do the same here so every test gets a
		// consistent baseline.
		String[] team = (String[]) getField(mode, "netvsPlayerTeam");
		String[] name = (String[]) getField(mode, "netvsPlayerName");
		for (int i = 0; i < team.length; i++) {
			team[i] = "";
			name[i] = "";
		}
	}

	// ========================
	// netvsIsWatch
	// ========================

	@Test
	void netvsIsWatchReturnsFalseWhenNetLobbyIsNull() {
		assertFalse(mode.netvsIsWatch());
	}

	@Test
	void netvsIsWatchReturnsTrueWhenSeatIdIsMinusOne() throws Exception {
		setupNetLobbyForSeatID(-1);
		assertTrue(mode.netvsIsWatch());
	}

	@Test
	void netvsIsWatchReturnsFalseWhenSeatIdIsNotMinusOne() throws Exception {
		setupNetLobbyForSeatID(0);
		assertFalse(mode.netvsIsWatch());
	}

	/**
	 * Wire up a minimal netLobby graph so that
	 * {@code netLobby.netPlayerClient.getYourPlayerInfo().seatID} resolves
	 * to the desired value.
	 */
	private void setupNetLobbyForSeatID(int seatID) throws Exception {
		NetPlayerInfo pInfo = new NetPlayerInfo();
		pInfo.seatID = seatID;
		pInfo.uid = 42;

		NetPlayerClient client = new NetPlayerClient();
		LinkedList<NetPlayerInfo> list = new LinkedList<>();
		list.add(pInfo);
		setField(client, "playerInfoList", list);
		setIntField(client, "playerUID", 42);

		NetLobbyFrame lobby = new NetLobbyFrame();
		lobby.netPlayerClient = client;

		setField(mode, "netLobby", lobby);
	}

	// ========================
	// netvsGetPlayerIDbySeatID
	// ========================

	@Test
	void netvsGetPlayerIDbySeatIDReturnsIdentityWhenMyseatIsZero() {
		// NETVS_GAME_SEAT_NUMBERS[0] = {0,1,2,3,4,5}
		assertEquals(0, mode.netvsGetPlayerIDbySeatID(0, 0));
		assertEquals(3, mode.netvsGetPlayerIDbySeatID(3, 0));
		assertEquals(5, mode.netvsGetPlayerIDbySeatID(5, 0));
	}

	@Test
	void netvsGetPlayerIDbySeatIDNormalizesNegativeMyseatToZero() {
		// Spectator (myseat == -1) is clamped to 0
		assertEquals(0, mode.netvsGetPlayerIDbySeatID(0, -1));
		assertEquals(5, mode.netvsGetPlayerIDbySeatID(5, -1));
	}

	@Test
	void netvsGetPlayerIDbySeatIDLooksUpCorrectRowForMyseatOfOne() {
		// NETVS_GAME_SEAT_NUMBERS[1] = {1,0,2,3,4,5}
		assertEquals(1, mode.netvsGetPlayerIDbySeatID(0, 1));
		assertEquals(0, mode.netvsGetPlayerIDbySeatID(1, 1));
		assertEquals(5, mode.netvsGetPlayerIDbySeatID(5, 1));
	}

	@Test
	void netvsGetPlayerIDbySeatIDLooksUpCorrectRowForMyseatOfTwo() {
		// NETVS_GAME_SEAT_NUMBERS[2] = {1,2,0,3,4,5}
		assertEquals(1, mode.netvsGetPlayerIDbySeatID(0, 2));
		assertEquals(0, mode.netvsGetPlayerIDbySeatID(2, 2));
		assertEquals(3, mode.netvsGetPlayerIDbySeatID(3, 2));
	}

	@Test
	void netvsGetPlayerIDbySeatIDHandlesMyseatAtLastRow() {
		// NETVS_GAME_SEAT_NUMBERS[5] = {1,2,3,4,5,0}
		assertEquals(1, mode.netvsGetPlayerIDbySeatID(0, 5));
		assertEquals(0, mode.netvsGetPlayerIDbySeatID(5, 5));
	}

	// ========================
	// netvsGetNumberOfTeamsAlive
	// ========================

	@Test
	void netvsGetNumberOfTeamsAliveReturnsZeroWhenNoPlayersExist() {
		assertEquals(0, mode.netvsGetNumberOfTeamsAlive());
	}

	@Test
	void netvsGetNumberOfTeamsAliveCountsSoloPlayers() throws Exception {
		setPlayerExist(0, true);
		setPlayerExist(1, true);
		manager.engine[0].gameActive = true;
		manager.engine[1].gameActive = true;

		assertEquals(2, mode.netvsGetNumberOfTeamsAlive());
	}

	@Test
	void netvsGetNumberOfTeamsAliveDoesNotCountDeadPlayers() throws Exception {
		setPlayerExist(0, true);
		setPlayerExist(1, true);
		setPlayerDead(1, true);
		manager.engine[0].gameActive = true;
		manager.engine[1].gameActive = true;

		assertEquals(1, mode.netvsGetNumberOfTeamsAlive());
	}

	@Test
	void netvsGetNumberOfTeamsAliveDoesNotCountInactivePlayers() throws Exception {
		setPlayerExist(0, true);
		setPlayerExist(1, true);
		manager.engine[0].gameActive = true;
		manager.engine[1].gameActive = false;

		assertEquals(1, mode.netvsGetNumberOfTeamsAlive());
	}

	@Test
	void netvsGetNumberOfTeamsAliveDoesNotCountNonExistentPlayers() throws Exception {
		setPlayerExist(0, true);
		// Player 1 defaults to not existing
		manager.engine[0].gameActive = true;

		assertEquals(1, mode.netvsGetNumberOfTeamsAlive());
	}

	@Test
	void netvsGetNumberOfTeamsAliveCountsTeamAsOneUnit() throws Exception {
		setPlayerExist(0, true);
		setPlayerExist(1, true);
		setPlayerTeam(0, "Alpha");
		setPlayerTeam(1, "Alpha");
		manager.engine[0].gameActive = true;
		manager.engine[1].gameActive = true;

		assertEquals(1, mode.netvsGetNumberOfTeamsAlive());
	}

	@Test
	void netvsGetNumberOfTeamsAliveCountsMultipleTeamsAndSoloPlayers() throws Exception {
		setPlayerExist(0, true);
		setPlayerExist(1, true);
		setPlayerExist(2, true);
		setPlayerExist(3, true);
		setPlayerTeam(0, "Alpha");
		setPlayerTeam(1, "Alpha");
		setPlayerTeam(2, "Beta");
		// Player 3 is solo (no team)
		manager.engine[0].gameActive = true;
		manager.engine[1].gameActive = true;
		manager.engine[2].gameActive = true;
		manager.engine[3].gameActive = true;

		assertEquals(3, mode.netvsGetNumberOfTeamsAlive());
	}

	// ========================
	// netvsIsAttackable
	// ========================

	@Test
	void netvsIsAttackableReturnsFalseForSelf() {
		assertFalse(mode.netvsIsAttackable(0));
	}

	@Test
	void netvsIsAttackableReturnsFalseForNegativePlayerId() {
		assertFalse(mode.netvsIsAttackable(-1));
	}

	@Test
	void netvsIsAttackableReturnsFalseForNonExistentPlayer() throws Exception {
		setPlayerExist(1, false);
		assertFalse(mode.netvsIsAttackable(1));
	}

	@Test
	void netvsIsAttackableReturnsFalseForDeadPlayer() throws Exception {
		setPlayerExist(1, true);
		setPlayerDead(1, true);
		assertFalse(mode.netvsIsAttackable(1));
	}

	@Test
	void netvsIsAttackableReturnsFalseForInactivePlayer() throws Exception {
		setPlayerExist(1, true);
		setPlayerActive(1, false);
		assertFalse(mode.netvsIsAttackable(1));
	}

	@Test
	void netvsIsAttackableReturnsFalseForTeammate() throws Exception {
		setPlayerExist(1, true);
		setPlayerActive(1, true);
		setPlayerTeam(0, "Alpha");
		setPlayerTeam(1, "Alpha");
		assertFalse(mode.netvsIsAttackable(1));
	}

	@Test
	void netvsIsAttackableReturnsTrueForValidTarget() throws Exception {
		setPlayerExist(1, true);
		setPlayerActive(1, true);
		// No team, not dead, not self
		assertTrue(mode.netvsIsAttackable(1));
	}

	@Test
	void netvsIsAttackableReturnsTrueForDifferentTeam() throws Exception {
		setPlayerExist(1, true);
		setPlayerActive(1, true);
		setPlayerTeam(0, "Alpha");
		setPlayerTeam(1, "Beta");
		assertTrue(mode.netvsIsAttackable(1));
	}

	@Test
	void netvsIsAttackableReturnsTrueWhenTeammateHasNoTeamName() throws Exception {
		// Player 0 has no team, player 1 has a team; not teammates
		setPlayerExist(1, true);
		setPlayerActive(1, true);
		setPlayerTeam(0, "");
		setPlayerTeam(1, "Alpha");
		assertTrue(mode.netvsIsAttackable(1));
	}

	@Test
	void netvsIsAttackableReturnsTrueWhenTargetHasNoTeamName() throws Exception {
		// Player 0 has a team, player 1 has no team; not teammates
		setPlayerExist(1, true);
		setPlayerActive(1, true);
		setPlayerTeam(0, "Alpha");
		setPlayerTeam(1, "");
		assertTrue(mode.netvsIsAttackable(1));
	}

	// ========================
	// netGetGoalType
	// ========================

	@Test
	void netGetGoalTypeReturnsZero() {
		assertEquals(0, mode.netGetGoalType());
	}

	// ========================
	// netIsNetRankingViewOK
	// ========================

	@Test
	void netIsNetRankingViewOKReturnsFalseForAnyEngine() {
		assertFalse(mode.netIsNetRankingViewOK(manager.engine[0]));
	}

	// ========================
	// netIsNetRankingSendOK
	// ========================

	@Test
	void netIsNetRankingSendOKDelegatesToNetIsNetRankingViewOK() {
		assertFalse(mode.netIsNetRankingSendOK(manager.engine[0]));
	}

	// ========================
	// Reflection helpers
	// ========================

	private void setPlayerExist(int pid, boolean v) throws Exception {
		boolean[] arr = (boolean[]) getField(mode, "netvsPlayerExist");
		arr[pid] = v;
	}

	private void setPlayerDead(int pid, boolean v) throws Exception {
		boolean[] arr = (boolean[]) getField(mode, "netvsPlayerDead");
		arr[pid] = v;
	}

	private void setPlayerActive(int pid, boolean v) throws Exception {
		boolean[] arr = (boolean[]) getField(mode, "netvsPlayerActive");
		arr[pid] = v;
	}

	private void setPlayerTeam(int pid, String team) throws Exception {
		String[] arr = (String[]) getField(mode, "netvsPlayerTeam");
		arr[pid] = team;
	}

	private static void setField(Object instance, String name, Object value) throws Exception {
		Field f = resolveField(instance.getClass(), name);
		f.setAccessible(true);
		f.set(instance, value);
	}

	private static void setIntField(Object instance, String name, int value) throws Exception {
		Field f = resolveField(instance.getClass(), name);
		f.setAccessible(true);
		f.setInt(instance, value);
	}

	private static Object getField(Object instance, String name) throws Exception {
		Field f = resolveField(instance.getClass(), name);
		f.setAccessible(true);
		return f.get(instance);
	}

	/**
	 * Walk the class hierarchy to find a declared field.
	 */
	private static Field resolveField(Class<?> clazz, String name) throws NoSuchFieldException {
		Class<?> c = clazz;
		while (c != null) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}

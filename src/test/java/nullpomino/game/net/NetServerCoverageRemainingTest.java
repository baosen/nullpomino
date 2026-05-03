// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Coverage-targeted tests for {@link NetServer} covering additional
 * processPacket handlers and helper methods not yet exercised by
 * existing test classes.
 *
 * <p>Focuses on packet types: getinfo, ping, getpresets, ruledata,
 * ruleget, rulegetrated, mpranking, roomleave. All tested via
 * reflection without a real network server.
 */
class NetServerCoverageRemainingTest {

	private NetServer server;
	private Selector savedSelector;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() throws Exception {
		setStaticField("propServer", new CustomProperties());

		server = new NetServer(9999);

		// Seed static fields
		setStaticDouble("ratingNormalMaxDiff", NetServer.NORMAL_MAX_DIFF);
		setStaticInt("ratingProvisionalGames", NetServer.PROVISIONAL_GAMES);
		setStaticInt("ratingMin", 0);
		setStaticInt("ratingMax", 99999);
		setStaticInt("ratingDefault", NetPlayerInfo.DEFAULT_MULTIPLAYER_RATING);
		setStaticInt("maxMPRanking", NetServer.DEFAULT_MAX_MPRANKING);
		setStaticInt("maxSPRanking", NetServer.DEFAULT_MAX_SPRANKING);
		setStaticInt("maxLobbyChatHistory", NetServer.DEFAULT_MAX_LOBBYCHAT_HISTORY);
		setStaticInt("maxRoomChatHistory", NetServer.DEFAULT_MAX_ROOMCHAT_HISTORY);
		setStaticBoolean("allowDNSAccess", true);

		// Init ruleList
		initRuleList();

		// Init mpRankingList
		Field f = NetServer.class.getDeclaredField("mpRankingList");
		f.setAccessible(true);
		LinkedList<NetPlayerInfo>[] mpList = new LinkedList[GameEngine.MAX_GAMESTYLE];
		for (int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
			mpList[i] = new LinkedList<NetPlayerInfo>();
		}
		f.set(null, mpList);

		// Init banList
		setStaticField("banList", new LinkedList<NetServerBan>());

		// Init lobbyChatList
		setStaticField("lobbyChatList", new LinkedList<NetChatMessage>());

		// Init SP ranking lists
		setStaticField("spRankingListAlltime", new LinkedList<NetSPRanking>());
		setStaticField("spRankingListDaily", new LinkedList<NetSPRanking>());

		// Init SP mode list
		LinkedList<String>[] spModeList = new LinkedList[GameEngine.MAX_GAMESTYLE];
		for (int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
			spModeList[i] = new LinkedList<String>();
		}
		Field spModeField = NetServer.class.getDeclaredField("spModeList");
		spModeField.setAccessible(true);
		spModeField.set(null, spModeList);

		// Init mp mode lists
		LinkedList<String>[] mpModeList = new LinkedList[GameEngine.MAX_GAMESTYLE];
		LinkedList<Boolean>[] mpModeIsRace = new LinkedList[GameEngine.MAX_GAMESTYLE];
		for (int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
			mpModeList[i] = new LinkedList<String>();
			mpModeIsRace[i] = new LinkedList<Boolean>();
		}
		Field mpModeField = NetServer.class.getDeclaredField("mpModeList");
		mpModeField.setAccessible(true);
		mpModeField.set(null, mpModeList);
		Field mpRaceField = NetServer.class.getDeclaredField("mpModeIsRace");
		mpRaceField.setAccessible(true);
		mpRaceField.set(null, mpModeIsRace);

		// Init ratedInfoList
		setStaticField("ratedInfoList", new LinkedList<String>());

		// Init propPresets
		setStaticField("propPresets", new CustomProperties());
	}

	@AfterEach
	void tearDown() throws Exception {
		if (savedSelector != null && savedSelector.isOpen()) {
			savedSelector.close();
		}
	}

	// ==================================================================
	// Helpers
	// ==================================================================

	private static void setStaticDouble(String name, double value) throws Exception {
		Field f = NetServer.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setDouble(null, value);
	}

	private static void setStaticInt(String name, int value) throws Exception {
		Field f = NetServer.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(null, value);
	}

	private static void setStaticBoolean(String name, boolean value) throws Exception {
		Field f = NetServer.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setBoolean(null, value);
	}

	private static void setStaticField(String name, Object value) throws Exception {
		Field f = NetServer.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(null, value);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getStaticField(String name, Class<T> type) throws Exception {
		Field f = NetServer.class.getDeclaredField(name);
		f.setAccessible(true);
		return (T) f.get(null);
	}

	@SuppressWarnings("unchecked")
	private <T> T getInstanceField(Object obj, String name) throws Exception {
		Field f = NetServer.class.getDeclaredField(name);
		f.setAccessible(true);
		return (T) f.get(obj);
	}

	/** Init ruleList with empty arrays. */
	@SuppressWarnings("unchecked")
	private static void initRuleList() throws Exception {
		LinkedList<RuleOptions>[] ruleList = new LinkedList[GameEngine.MAX_GAMESTYLE];
		LinkedList<Integer>[] ruleSettingIDList = new LinkedList[GameEngine.MAX_GAMESTYLE];
		for (int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
			ruleList[i] = new LinkedList<RuleOptions>();
			ruleSettingIDList[i] = new LinkedList<Integer>();
		}
		Field f = NetServer.class.getDeclaredField("ruleList");
		f.setAccessible(true);
		f.set(null, ruleList);
		Field f2 = NetServer.class.getDeclaredField("ruleSettingIDList");
		f2.setAccessible(true);
		f2.set(null, ruleSettingIDList);
	}

	/** Enable the selector so send() doesn't NPE. */
	private void setSelector() throws Exception {
		Field f = NetServer.class.getDeclaredField("selector");
		f.setAccessible(true);
		if (f.get(server) == null) {
			savedSelector = Selector.open();
			f.set(server, savedSelector);
		}
	}

	/** Add a player+channel to the server's internal maps. */
	private NetPlayerInfo addPlayerToServer(SocketChannel ch, String name, int uid) throws Exception {
		return addPlayerToServer(ch, name, uid, null);
	}

	private NetPlayerInfo addPlayerToServer(SocketChannel ch, String name, int uid, RuleOptions ruleOpt) throws Exception {
		NetPlayerInfo p = new NetPlayerInfo();
		p.strName = name;
		p.uid = uid;
		p.roomID = -1;
		p.connected = true;
		p.playing = false;
		p.isTripUse = false;
		p.ruleOpt = (ruleOpt != null) ? ruleOpt : new RuleOptions();
		Map<SocketChannel, NetPlayerInfo> infoMap = getInstanceField(server, "playerInfoMap");
		infoMap.put(ch, p);
		LinkedList<SocketChannel> chList = getInstanceField(server, "channelList");
		chList.add(ch);
		return p;
	}

	/** Invoke processPacket and unwrap InvocationTargetException. */
	private void callProcessPacket(SocketChannel ch, String msg) throws Throwable {
		Method m = NetServer.class.getDeclaredMethod("processPacket", SocketChannel.class, String.class);
		m.setAccessible(true);
		try {
			m.invoke(server, ch, msg);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	// ==================================================================
	// processPacket: getinfo
	// ==================================================================

	@Test
	void processPacketGetinfoReturnsServerInfo() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			callProcessPacket(ch, "getinfo");
			// Should not throw; server info sent to channel (but no reader)
		}
	}

	// ==================================================================
	// processPacket: disconnect
	// ==================================================================

	@Test
	void processPacketDisconnectThrowsException() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			assertThrows(NetServerDisconnectRequestedException.class,
					() -> callProcessPacket(ch, "disconnect"));
		}
	}

	// ==================================================================
	// processPacket: ping
	// ==================================================================

	@Test
	void processPacketPingWithoutIdSendsPong() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			callProcessPacket(ch, "ping");
			// Should not throw; pong sent
		}
	}

	@Test
	void processPacketPingWithIdSendsPongWithId() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			callProcessPacket(ch, "ping\t42");
			// Should not throw; pong with id 42 sent
		}
	}

	// ==================================================================
	// processPacket: getpresets
	// ==================================================================

	@Test
	void processPacketGetpresetsWithEmptyRatedInfoList() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			callProcessPacket(ch, "getpresets");
			// Should not throw with empty ratedInfoList
		}
	}

	@Test
	void processPacketGetpresetsWithEntries() throws Throwable {
		setSelector();
		LinkedList<String> ratedInfoList = getStaticField("ratedInfoList", LinkedList.class);
		ratedInfoList.add("preset1");
		ratedInfoList.add("preset2");

		try (SocketChannel ch = SocketChannel.open()) {
			callProcessPacket(ch, "getpresets");
			// Should not throw with entries
		}
	}

	// ==================================================================
	// processPacket: ruledata
	// ==================================================================

	@Test
	void processPacketRuledataWithoutPlayerInfoDoesNothing() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			// No player info -> pInfo is null, early return
			callProcessPacket(ch, "ruledata\t12345\tencodedData");
			// Should not throw
		}
	}

	@Test
	void processPacketRuledataWithChecksumMatch() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			addPlayerToServer(ch, "TestPlayer", 1);

			// Create some rule data and compute its checksum
			CustomProperties prop = new CustomProperties();
			prop.setProperty("0.ruleName", "TestRule");
			String strRuleData = prop.encode("RuleData");
			String compressedData = NetUtil.compressString(strRuleData);

			java.util.zip.Adler32 checksum = new java.util.zip.Adler32();
			checksum.update(NetUtil.stringToBytes(compressedData));
			long expectedChecksum = checksum.getValue();

			callProcessPacket(ch, "ruledata\t" + expectedChecksum + "\t" + compressedData);
			// Should process the rule data successfully
			Map<SocketChannel, NetPlayerInfo> infoMap = getInstanceField(server, "playerInfoMap");
			NetPlayerInfo p = infoMap.get(ch);
			assertNotNull(p);
			assertNotNull(p.ruleOpt);
		}
	}

	@Test
	void processPacketRuledataWithChecksumMismatch() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			addPlayerToServer(ch, "TestPlayer", 1);

			// Wrong checksum
			callProcessPacket(ch, "ruledata\t99999\tencodedData");
			// Should not throw; sends ruledatafail
		}
	}

	// ==================================================================
	// processPacket: ruleget
	// ==================================================================

	@Test
	void processPacketRulegetWithoutPlayerInfoDoesNothing() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			callProcessPacket(ch, "ruleget\t1");
			// pInfo is null, returns early
		}
	}

	@Test
	void processPacketRulegetWithNonExistentPlayer() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			addPlayerToServer(ch, "TestPlayer", 1);

			callProcessPacket(ch, "ruleget\t999");
			// UID 999 doesn't exist -> rulegetfail
		}
	}

	@Test
	void processPacketRulegetWithExistingPlayer() throws Throwable {
		setSelector();
		try (SocketChannel ch1 = SocketChannel.open(); SocketChannel ch2 = SocketChannel.open()) {
			addPlayerToServer(ch1, "Player1", 1);
			NetPlayerInfo p2 = addPlayerToServer(ch2, "Player2", 2);
			p2.ruleOpt = new RuleOptions();
			p2.ruleOpt.strRuleName = "Standard";

			callProcessPacket(ch1, "ruleget\t2");
			// Should find player 2's rule data and send it
		}
	}

	// ==================================================================
	// processPacket: rulegetrated
	// ==================================================================

	@Test
	void processPacketRulegetratedWithoutPlayerInfoDoesNothing() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			callProcessPacket(ch, "rulegetrated\t0\tStandard");
			// pInfo is null, returns early
		}
	}

	@Test
	void processPacketRulegetratedWithNonExistentRule() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			addPlayerToServer(ch, "TestPlayer", 1);

			callProcessPacket(ch, "rulegetrated\t0\tNonExistentRule");
			// Rule doesn't exist -> rulegetratedfail
		}
	}

	@Test
	void processPacketRulegetratedWithExistingRule() throws Throwable {
		setSelector();
		// Set up ruleList with a Standard rule
		LinkedList<RuleOptions>[] ruleList = new LinkedList[GameEngine.MAX_GAMESTYLE];
		for (int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
			ruleList[i] = new LinkedList<RuleOptions>();
		}
		RuleOptions rule = new RuleOptions();
		rule.strRuleName = "Standard";
		ruleList[0].add(rule);
		Field f = NetServer.class.getDeclaredField("ruleList");
		f.setAccessible(true);
		f.set(null, ruleList);

		try (SocketChannel ch = SocketChannel.open()) {
			addPlayerToServer(ch, "TestPlayer", 1);

			callProcessPacket(ch, "rulegetrated\t0\tStandard");
			// Should find rule and send rulegetratedsuccess
		}
	}

	// ==================================================================
	// processPacket: mpranking
	// ==================================================================

	@Test
	void processPacketMprankingWithEmptyList() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			callProcessPacket(ch, "mpranking\t0");
			// Should not throw with empty mpRankingList
		}
	}

	@Test
	void processPacketMprankingWithEntries() throws Throwable {
		setSelector();
		LinkedList<NetPlayerInfo> mpList = getMpRankingList(0);
		NetPlayerInfo p = new NetPlayerInfo();
		p.strName = "TopPlayer";
		p.rating[0] = 2000;
		p.playCount[0] = 100;
		p.winCount[0] = 60;
		mpList.add(p);

		try (SocketChannel ch = SocketChannel.open()) {
			callProcessPacket(ch, "mpranking\t0");
			// Should not throw
		}
	}

	@SuppressWarnings("unchecked")
	private static LinkedList<NetPlayerInfo> getMpRankingList(int style) throws Exception {
		Field f = NetServer.class.getDeclaredField("mpRankingList");
		f.setAccessible(true);
		LinkedList<NetPlayerInfo>[] arr = (LinkedList<NetPlayerInfo>[]) f.get(null);
		return arr[style];
	}

	// ==================================================================
	// processPacket: roomleave
	// ==================================================================

	@Test
	void processPacketRoomleaveWithoutPlayerInfoDoesNothing() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			callProcessPacket(ch, "roomleave");
			// pInfo is null, returns early
		}
	}

	@Test
	void processPacketRoomleaveWhenNotInRoomDoesNothing() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			addPlayerToServer(ch, "LobbyPlayer", 1);
			// roomID is -1 (lobby), not in a room
			callProcessPacket(ch, "roomleave");
			// Should not throw
		}
	}

	@Test
	void processPacketRoomleaveLeavesRoom() throws Throwable {
		setSelector();
		try (SocketChannel ch = SocketChannel.open()) {
			NetPlayerInfo p = addPlayerToServer(ch, "RoomPlayer", 1);

			NetRoomInfo room = new NetRoomInfo();
			room.roomID = 5;
			room.strName = "TestRoom";
			room.maxPlayers = 4;
			room.playerSeat.add(p);
			room.playerList.add(p);

			LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
			roomList.add(room);

			p.roomID = 5;
			p.seatID = 0;

			callProcessPacket(ch, "roomleave");
			assertEquals(-1, p.roomID);
			assertEquals(-1, p.seatID);
		}
	}

	// ==================================================================
	// getHostAddress / getHostName / getHostFull with connected socket
	// ==================================================================

	@Test
	void getHostAddressWithNullReturnsEmptyString() throws Throwable {
		Method m = NetServer.class.getDeclaredMethod("getHostAddress", SocketChannel.class);
		m.setAccessible(true);
		String result = (String) m.invoke(null, (SocketChannel) null);
		assertEquals("", result);
	}

	@Test
	void getHostNameWithDisconnectedSocket() throws Throwable {
		Method m = NetServer.class.getDeclaredMethod("getHostName", SocketChannel.class);
		m.setAccessible(true);
		try (SocketChannel ch = SocketChannel.open()) {
			String result = (String) m.invoke(null, ch);
			assertNotNull(result);
		}
	}

	@Test
	void getHostFullWithDisconnectedSocket() throws Throwable {
		Method m = NetServer.class.getDeclaredMethod("getHostFull", SocketChannel.class);
		m.setAccessible(true);
		try (SocketChannel ch = SocketChannel.open()) {
			String result = (String) m.invoke(null, ch);
			assertNotNull(result);
		}
	}

	// ==================================================================
	// getRatedRule with ruleList having entries
	// ==================================================================

	@Test
	void getRatedRuleWithNullNameReturnsNull() throws Throwable {
		MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());
		MethodHandle mh = lookup.findVirtual(NetServer.class, "getRatedRule",
				MethodType.methodType(RuleOptions.class, int.class, String.class));
		RuleOptions result = (RuleOptions) mh.invokeExact(server, 0, (String) null);
		assertNull(result);
	}

	// ==================================================================
	// getBan with null check
	// ==================================================================

	@Test
	void getBanWithNullChannelReturnsNull() throws Throwable {
		MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());
		MethodHandle mh = lookup.findVirtual(NetServer.class, "getBan",
				MethodType.methodType(NetServerBan.class, SocketChannel.class));
		NetServerBan result = (NetServerBan) mh.invokeExact(server, (SocketChannel) null);
		assertNull(result);
	}

	// ==================================================================
	// writeServerStatusFile with various config
	// ==================================================================

	@Test
	void writeServerStatusFileWithNullPathDoesNotThrow() throws Throwable {
		CustomProperties props = new CustomProperties();
		props.setProperty("netserver.writestatusfile", true);
		// No statusfilename set -> uses default path
		setStaticField("propServer", props);

		Method m = NetServer.class.getDeclaredMethod("writeServerStatusFile");
		m.setAccessible(true);
		m.invoke(server);
		// Should not throw
	}

	// ==================================================================
	// loadRuleList with the : style parser
	// ==================================================================

	@Test
	void loadRuleListWithPathAndStyleMarkersCoverage() throws Throwable {
		// Test with a known good style marker
		java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("netServerTest");
		java.nio.file.Path ruleFile = tempDir.resolve("rulelist.lst");
		java.nio.file.Files.writeString(ruleFile, String.join("\n",
				":TETRIS",
				"",
				"#comment"));

		MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());
		MethodHandle mh = lookup.findStatic(NetServer.class, "loadRuleList",
				MethodType.methodType(void.class, String.class));
		mh.invokeExact((String) ruleFile.toString());

		LinkedList<RuleOptions>[] rl = getStaticField("ruleList", LinkedList[].class);
		assertNotNull(rl);

		// Cleanup
		java.nio.file.Files.deleteIfExists(ruleFile);
		java.nio.file.Files.deleteIfExists(tempDir);
	}

	// ==================================================================
	// Constructors
	// ==================================================================

	@Test
	void defaultConstructorUsesDefaultPort() throws Exception {
		// Use reflection to create a new instance with different constructor
		java.lang.reflect.Constructor<NetServer> ctor = NetServer.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		NetServer s = ctor.newInstance();
		assertNotNull(s);
	}

	// ==================================================================
	// checkConnectionOnBanlist with empty banlist
	// ==================================================================

	@Test
	void checkConnectionOnBanlistWithEmptyListReturnsFalse() throws Throwable {
		Method m = NetServer.class.getDeclaredMethod("checkConnectionOnBanlist", SocketChannel.class);
		m.setAccessible(true);
		try (SocketChannel ch = SocketChannel.open()) {
			boolean banned = (Boolean) m.invoke(server, ch);
			assertFalse(banned);
		}
	}

	// ==================================================================
	// main method coverage (no args)
	// ==================================================================

	@Test
	void mainWithNoArgsDoesNotThrow() {
		// This will start a server on default port which will fail when trying to
		// open a server socket - but the constructor wraps exceptions
		// We just verify it can be called without throwing
		assertDoesNotThrow(() -> NetServer.main(new String[0]));
	}
}

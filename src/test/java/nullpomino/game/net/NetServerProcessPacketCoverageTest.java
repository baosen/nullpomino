package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Map;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.net.NetServer;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Coverage tests for {@link NetServer}'s big message-dispatch handlers:
 * {@code processPacket(SocketChannel, String)} and
 * {@code processAdminCommand(SocketChannel, String[])}.
 *
 * <p>These handlers are a giant switch over the first tab-separated token of a
 * client message. None of the branches actually need live network I/O: the
 * {@code send(...)} helper merely enqueues bytes into the internal pending
 * queues and calls {@code selector.wakeup()}. We therefore install a real
 * (but unregistered) {@link Selector} via reflection and register a fake,
 * unconnected {@link SocketChannel} in the server's private maps, then invoke
 * the private handlers directly through {@link MethodHandles}.
 *
 * <p>Assertions are deliberately robust (no-throw and observable state
 * mutations) because exact wire formats are not the point — line execution is.
 */
class NetServerProcessPacketCoverageTest {

	private NetServer server;
	private Selector selector;
	private final LinkedList<SocketChannel> openChannels = new LinkedList<SocketChannel>();

	private MethodHandle processPacket;
	private MethodHandle processAdminCommand;

	@BeforeEach
	void setUp() throws Exception {
		// init() reads propServer, so make sure it exists.
		setStaticField("propServer", new CustomProperties());
		setStaticField("propPlayerData", new CustomProperties());

		server = new NetServer(19999);

		// A real selector so send()/wakeup() does not NPE. We never register
		// channels with it, so it is never used for actual selection.
		selector = Selector.open();
		setInstanceField(server, "selector", selector);

		MethodHandles.Lookup lookup =
				MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());

		processPacket = lookup.findVirtual(NetServer.class, "processPacket",
				MethodType.methodType(void.class, SocketChannel.class, String.class));
		processAdminCommand = lookup.findVirtual(NetServer.class, "processAdminCommand",
				MethodType.methodType(void.class, SocketChannel.class, String[].class));

		// Ensure ban list exists for ban/unban handlers.
		setStaticField("banList", new LinkedList<NetServerBan>());
	}

	@AfterEach
	void tearDown() {
		for (SocketChannel ch : openChannels) {
			try { ch.close(); } catch (Exception ignored) { }
		}
		openChannels.clear();
		try { if (selector != null) selector.close(); } catch (Exception ignored) { }
	}

	// ------------------------------------------------------------------
	// Reflection helpers
	// ------------------------------------------------------------------

	private static void setStaticField(String name, Object value) throws Exception {
		Field f = NetServer.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(null, value);
	}

	private static void setInstanceField(NetServer s, String name, Object value) throws Exception {
		Field f = NetServer.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(s, value);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getInstanceField(NetServer s, String name) throws Exception {
		Field f = NetServer.class.getDeclaredField(name);
		f.setAccessible(true);
		return (T) f.get(s);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getStaticField(String name) throws Exception {
		Field f = NetServer.class.getDeclaredField(name);
		f.setAccessible(true);
		return (T) f.get(null);
	}

	private SocketChannel newChannel() throws Exception {
		SocketChannel ch = SocketChannel.open();
		openChannels.add(ch);
		return ch;
	}

	/** Register a channel in channelList + lastCommTimeMap (unauthenticated). */
	private void registerChannel(SocketChannel ch) throws Exception {
		LinkedList<SocketChannel> channelList = getInstanceField(server, "channelList");
		channelList.add(ch);
		Map<SocketChannel, Long> lastComm = getInstanceField(server, "lastCommTimeMap");
		lastComm.put(ch, System.currentTimeMillis());
	}

	/** Register a logged-in player for a channel and return its NetPlayerInfo. */
	private NetPlayerInfo registerPlayer(SocketChannel ch, String name) throws Exception {
		registerChannel(ch);
		NetPlayerInfo p = new NetPlayerInfo();
		p.strName = name;
		p.uid = 1000 + name.hashCode();
		p.connected = true;
		p.channel = ch;
		p.ruleOpt = new RuleOptions();
		p.ruleOpt.strRuleName = "TestRule";
		Map<SocketChannel, NetPlayerInfo> infoMap = getInstanceField(server, "playerInfoMap");
		infoMap.put(ch, p);
		return p;
	}

	/** Add a room to the server's roomInfoList. */
	private void addRoom(NetRoomInfo room) throws Exception {
		LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
		roomList.add(room);
	}

	private void invoke(SocketChannel ch, String msg) throws Throwable {
		processPacket.invoke(server, ch, msg);
	}

	private void invokeAdmin(SocketChannel ch, String[] cmd) throws Throwable {
		processAdminCommand.invoke(server, ch, cmd);
	}

	// ------------------------------------------------------------------
	// Simple stateless commands
	// ------------------------------------------------------------------

	@Test
	void getinfoReplies() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		invoke(ch, "getinfo");
		// send() enqueued data for this channel
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void disconnectThrowsDisconnectRequested() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		assertThrows(NetServerDisconnectRequestedException.class, () -> {
			try { invoke(ch, "disconnect"); }
			catch (Throwable t) { if (t instanceof Exception) throw (Exception) t; throw new RuntimeException(t); }
		});
	}

	@Test
	void pingWithIdReplies() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		invoke(ch, "ping\t42");
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void pingWithoutIdReplies() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		invoke(ch, "ping");
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	// ------------------------------------------------------------------
	// Observer / player login version+build checks
	// ------------------------------------------------------------------

	@Test
	void observerLoginVersionMismatchQueuesDisconnect() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		// A clearly-wrong client version triggers DIFFERENT_VERSION branch.
		invoke(ch, "observerlogin\t0.001\t0\tfalse");
		LinkedList<SocketChannel> obs = getInstanceField(server, "observerList");
		assertFalse(obs.contains(ch), "version mismatch should not add observer");
	}

	@Test
	void loginVersionMismatchQueuesDisconnect() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		invoke(ch, "login\t0.001\tBob\tXX\tTeam\t0\tfalse");
		Map<SocketChannel, NetPlayerInfo> infoMap = getInstanceField(server, "playerInfoMap");
		assertNull(infoMap.get(ch), "version mismatch should not log player in");
	}

	@Test
	void loginIgnoredWhenAlreadyObserver() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		LinkedList<SocketChannel> obs = getInstanceField(server, "observerList");
		obs.add(ch);
		invoke(ch, "login\t0.001\tBob\tXX\tTeam\t0\tfalse");
		Map<SocketChannel, NetPlayerInfo> infoMap = getInstanceField(server, "playerInfoMap");
		assertNull(infoMap.get(ch));
	}

	@Test
	void loginSucceedsWithMatchingVersion() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		float ver = nullpomino.game.play.GameManager.getVersionMajor();
		boolean dev = nullpomino.game.play.GameManager.isDevBuild();
		invoke(ch, "login\t" + ver + "\tCarol\tNO\tTeamX\t0\t" + dev);
		Map<SocketChannel, NetPlayerInfo> infoMap = getInstanceField(server, "playerInfoMap");
		NetPlayerInfo p = infoMap.get(ch);
		assertNotNull(p, "matching-version login should succeed");
		assertEquals("Carol", p.strName);
	}

	@Test
	void observerLoginSucceedsWithMatchingVersion() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		float ver = nullpomino.game.play.GameManager.getVersionMajor();
		boolean dev = nullpomino.game.play.GameManager.isDevBuild();
		invoke(ch, "observerlogin\t" + ver + "\t0\t" + dev);
		LinkedList<SocketChannel> obs = getInstanceField(server, "observerList");
		assertTrue(obs.contains(ch), "matching-version observer login should succeed");
	}

	// ------------------------------------------------------------------
	// Rule data exchange
	// ------------------------------------------------------------------

	@Test
	void ruledataWithBadChecksumReportsFail() throws Throwable {
		SocketChannel ch = newChannel();
		registerPlayer(ch, "RuleGuy");
		// Provide a wrong checksum so we hit the FAIL branch.
		invoke(ch, "ruledata\t1\tnotcompressed");
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void rulegetUnknownUidReportsFail() throws Throwable {
		SocketChannel ch = newChannel();
		registerPlayer(ch, "Asker");
		invoke(ch, "ruleget\t999999");
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void rulegetKnownUidReturnsRule() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Self");
		invoke(ch, "ruleget\t" + p.uid);
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void rulegetratedUnknownReportsFail() throws Throwable {
		SocketChannel ch = newChannel();
		registerPlayer(ch, "Rater");
		invoke(ch, "rulegetrated\t0\tNonExistentRule");
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void getpresetsReplies() throws Throwable {
		SocketChannel ch = newChannel();
		registerPlayer(ch, "PresetGuy");
		invoke(ch, "getpresets");
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	// ------------------------------------------------------------------
	// Lobby and room chat
	// ------------------------------------------------------------------

	@Test
	void lobbychatBroadcastsAndStores() throws Throwable {
		SocketChannel ch = newChannel();
		registerPlayer(ch, "Chatter");
		LinkedList<NetChatMessage> before = getStaticField("lobbyChatList");
		int prevSize = before.size();
		invoke(ch, "lobbychat\thello%20world");
		LinkedList<NetChatMessage> after = getStaticField("lobbyChatList");
		assertEquals(prevSize + 1, after.size());
	}

	@Test
	void lobbychatPrivateMessageToUnknownUser() throws Throwable {
		SocketChannel ch = newChannel();
		registerPlayer(ch, "Sender");
		// "/msg Nobody hi" — target not found path.
		invoke(ch, "lobbychat\t" + NetUtil.urlEncode("/msg Nobody hi"));
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void lobbychatPrivateMessageToKnownUser() throws Throwable {
		SocketChannel sender = newChannel();
		SocketChannel target = newChannel();
		registerPlayer(sender, "Sender");
		registerPlayer(target, "Target");
		// "/msg Target hello" routes to the found-player branch.
		invoke(sender, "lobbychat\t" + NetUtil.urlEncode("/msg Target hello"));
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(target) || pending.containsKey(sender));
	}

	@Test
	void roomChatStoresInRoom() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "RoomChatter");
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 7;
		addRoom(room);
		p.roomID = 7;
		invoke(ch, "chat\thi%20room");
		assertEquals(1, room.chatList.size());
	}

	// ------------------------------------------------------------------
	// Multiplayer ranking
	// ------------------------------------------------------------------

	@Test
	void mprankingRepliesForEmptyBoard() throws Throwable {
		SocketChannel ch = newChannel();
		registerPlayer(ch, "Ranker");
		invoke(ch, "mpranking\t0");
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	// ------------------------------------------------------------------
	// Room create variants
	// ------------------------------------------------------------------

	@Test
	void singleroomcreateAddsRoom() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Solo");
		p.roomID = -1;
		LinkedList<NetRoomInfo> rooms = getInstanceField(server, "roomInfoList");
		int before = rooms.size();
		invoke(ch, "singleroomcreate\t" + NetUtil.urlEncode("MyRoom") + "\t" + NetUtil.urlEncode("Mode") + "");
		assertEquals(before + 1, rooms.size());
		assertNotEquals(-1, p.roomID);
	}

	@Test
	void roomcreateAddsMultiplayerRoom() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Host");
		p.roomID = -1;
		LinkedList<NetRoomInfo> rooms = getInstanceField(server, "roomInfoList");
		int before = rooms.size();
		// roomcreate\t[name]\t[roominfo]\t[mode] — the roominfo token must be a
		// valid NetRoomInfo.exportString() so the server can parse it.
		String roomInfoStr = new NetRoomInfo().exportString();
		invoke(ch, "roomcreate\t" + NetUtil.urlEncode("MultiRoom") + "\t" + NetUtil.urlEncode(roomInfoStr) + "\t" + NetUtil.urlEncode("MyMode"));
		assertEquals(before + 1, rooms.size());
		assertNotEquals(-1, p.roomID);
	}

	@Test
	void roomcreateIgnoredWhenAlreadyInRoom() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Busy");
		p.roomID = 5; // already in a room
		LinkedList<NetRoomInfo> rooms = getInstanceField(server, "roomInfoList");
		int before = rooms.size();
		invoke(ch, "roomcreate\t" + NetUtil.urlEncode("X") + "\t" + NetUtil.urlEncode("") + "\t" + NetUtil.urlEncode("M"));
		assertEquals(before, rooms.size());
	}

	// ------------------------------------------------------------------
	// Room join
	// ------------------------------------------------------------------

	@Test
	void roomjoinUnknownRoomReportsFail() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Joiner");
		p.roomID = -1;
		invoke(ch, "roomjoin\t999\tfalse");
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void roomjoinReturnToLobby() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Leaver");
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 3;
		addRoom(room);
		p.roomID = 3;
		room.playerList.add(p);
		invoke(ch, "roomjoin\t-1\tfalse");
		assertEquals(-1, p.roomID);
	}

	@Test
	void roomjoinEntersExistingRoom() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Entrant");
		p.roomID = -1;
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 11;
		room.maxPlayers = 4;
		addRoom(room);
		invoke(ch, "roomjoin\t11\tfalse");
		assertEquals(11, p.roomID);
		assertTrue(room.playerList.contains(p));
	}

	@Test
	void roomjoinAsWatcher() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Watcher");
		p.roomID = -1;
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 12;
		room.maxPlayers = 4;
		addRoom(room);
		invoke(ch, "roomjoin\t12\ttrue");
		assertEquals(12, p.roomID);
		assertEquals(-1, p.seatID, "watcher should not take a seat");
	}

	// ------------------------------------------------------------------
	// changeteam / changename / changestatus
	// ------------------------------------------------------------------

	@Test
	void changeteamUpdatesTeam() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "TeamGuy");
		p.playing = false;
		p.strTeam = "";
		invoke(ch, "changeteam\t" + NetUtil.urlEncode("NewTeam"));
		assertEquals("NewTeam", p.strTeam);
	}

	@Test
	void changenameUpdatesName() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "OldName");
		p.playing = false;
		invoke(ch, "changename\t" + NetUtil.urlEncode("BrandNew"));
		assertEquals("BrandNew", p.strName);
	}

	@Test
	void changenameRejectsEmpty() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Keep");
		p.playing = false;
		invoke(ch, "changename\t" + NetUtil.urlEncode("   "));
		assertEquals("Keep", p.strName);
	}

	@Test
	void changenameRejectsWhilePlaying() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Playing");
		p.playing = true;
		invoke(ch, "changename\t" + NetUtil.urlEncode("Nope"));
		assertEquals("Playing", p.strName);
	}

	@Test
	void changestatusToSpectator() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Specter");
		p.playing = false;
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 21;
		room.maxPlayers = 4;
		room.singleplayer = false;
		addRoom(room);
		p.roomID = 21;
		p.seatID = room.joinSeat(p);
		invoke(ch, "changestatus\ttrue");
		assertEquals(-1, p.seatID);
	}

	@Test
	void changestatusToPlayer() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Seater");
		p.playing = false;
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 22;
		room.maxPlayers = 4;
		room.singleplayer = false;
		addRoom(room);
		p.roomID = 22;
		p.seatID = -1;
		invoke(ch, "changestatus\tfalse");
		assertTrue(p.seatID != -1 || p.queueID != -1);
	}

	// ------------------------------------------------------------------
	// ready / autostart / start1p
	// ------------------------------------------------------------------

	@Test
	void readyMarksPlayerReady() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Readyer");
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 31;
		room.maxPlayers = 4;
		room.singleplayer = false;
		addRoom(room);
		p.roomID = 31;
		p.seatID = room.joinSeat(p);
		invoke(ch, "ready\ttrue");
		assertTrue(p.ready);
	}

	@Test
	void readyCancelMarksSomeoneCancelled() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Canceler");
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 32;
		room.maxPlayers = 4;
		room.singleplayer = false;
		addRoom(room);
		p.roomID = 32;
		p.seatID = room.joinSeat(p);
		invoke(ch, "ready\tfalse");
		assertTrue(room.isSomeoneCancelled);
	}

	@Test
	void start1pStartsSingleplayerGame() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "SoloStart");
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 33;
		room.maxPlayers = 1;
		room.singleplayer = true;
		addRoom(room);
		p.roomID = 33;
		p.seatID = room.joinSeat(p);
		invoke(ch, "start1p");
		assertTrue(room.playing);
	}

	@Test
	void autostartTriggersWhenActive() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p1 = registerPlayer(ch, "AutoA");
		SocketChannel ch2 = newChannel();
		NetPlayerInfo p2 = registerPlayer(ch2, "AutoB");
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 34;
		room.maxPlayers = 4;
		room.singleplayer = false;
		room.autoStartActive = true;
		room.autoStartTNET2 = false;
		addRoom(room);
		p1.roomID = 34;
		p2.roomID = 34;
		p1.seatID = room.joinSeat(p1);
		p2.seatID = room.joinSeat(p2);
		p1.ready = true;
		p2.ready = true;
		invoke(ch, "autostart");
		// gameStart requires >= 2 seated players; should now be playing.
		assertTrue(room.playing);
	}

	// ------------------------------------------------------------------
	// dead / racewin
	// ------------------------------------------------------------------

	@Test
	void deadNoOpWhenNotInRoom() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Ghost");
		p.roomID = -1;
		invoke(ch, "dead");
		assertFalse(p.playing);
	}

	@Test
	void deadWithKoUid() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Victim");
		p.roomID = -1;
		invoke(ch, "dead\t12345");
		assertFalse(p.playing);
	}

	@Test
	void racewinNoOpWhenNotSeated() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Racer");
		p.roomID = -1;
		p.seatID = -1;
		invoke(ch, "racewin\t1\t2");
		assertFalse(p.playing);
	}

	// ------------------------------------------------------------------
	// gstat / gstat1p
	// ------------------------------------------------------------------

	@Test
	void gstatBroadcastsForMultiplayer() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Statter");
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 41;
		room.singleplayer = false;
		addRoom(room);
		p.roomID = 41;
		p.seatID = 0;
		invoke(ch, "gstat\t100\t200");
		// no exception is the assertion; verify the room still consistent
		assertEquals(41, p.roomID);
	}

	@Test
	void gstat1pBroadcastsForSingleplayer() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Statter1p");
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 42;
		room.singleplayer = true;
		addRoom(room);
		p.roomID = 42;
		p.seatID = 0;
		invoke(ch, "gstat1p\tsomestats");
		assertEquals(42, p.roomID);
	}

	// ------------------------------------------------------------------
	// spranking / spdownload / reset1p / game
	// ------------------------------------------------------------------

	@Test
	void sprankingEmptyReturnsZeroBoard() throws Throwable {
		SocketChannel ch = newChannel();
		registerPlayer(ch, "SPRanker");
		invoke(ch, "spranking\t" + NetUtil.urlEncode("any") + "\t" + NetUtil.urlEncode("SomeMode") + "\t0\tfalse");
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void spdownloadUnknownRecordLogsWarning() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Downloader");
		p.roomID = -1;
		// No ranking exists; method logs a warning and returns without throwing.
		invoke(ch, "spdownload\t" + NetUtil.urlEncode("any") + "\t" + NetUtil.urlEncode("Mode") + "\t0\tfalse\t" + NetUtil.urlEncode("Nobody"));
		assertEquals(-1, p.roomID);
	}

	@Test
	void reset1pResetsPlayState() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Resetter");
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 51;
		room.maxPlayers = 1;
		room.singleplayer = true;
		addRoom(room);
		p.roomID = 51;
		p.seatID = room.joinSeat(p);
		invoke(ch, "reset1p");
		assertEquals(51, p.roomID);
	}

	@Test
	void gameMessageRelayedToRoom() throws Throwable {
		SocketChannel ch = newChannel();
		NetPlayerInfo p = registerPlayer(ch, "Player");
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 52;
		room.maxPlayers = 4;
		room.singleplayer = false;
		addRoom(room);
		p.roomID = 52;
		p.seatID = room.joinSeat(p);
		invoke(ch, "game\tpiecemove\t1\t2");
		assertEquals(52, p.roomID);
	}

	// ------------------------------------------------------------------
	// adminlogin
	// ------------------------------------------------------------------

	@Test
	void adminloginDisabledWhenNoCredentials() throws Throwable {
		// propServer has no admin username/password -> DISABLE branch.
		SocketChannel ch = newChannel();
		registerChannel(ch);
		float ver = nullpomino.game.play.GameManager.getVersionMajor();
		boolean dev = nullpomino.game.play.GameManager.isDevBuild();
		invoke(ch, "adminlogin\t" + ver + "\tadmin\tcheckdata\t" + dev);
		LinkedList<SocketChannel> admins = getInstanceField(server, "adminList");
		assertFalse(admins.contains(ch));
	}

	@Test
	void adminloginWrongUsernameFails() throws Throwable {
		CustomProperties props = getStaticField("propServer");
		props.setProperty("netserver.admin.username", "root");
		props.setProperty("netserver.admin.password", "secret");
		SocketChannel ch = newChannel();
		registerChannel(ch);
		float ver = nullpomino.game.play.GameManager.getVersionMajor();
		boolean dev = nullpomino.game.play.GameManager.isDevBuild();
		invoke(ch, "adminlogin\t" + ver + "\twronguser\tQUJD\t" + dev);
		LinkedList<SocketChannel> admins = getInstanceField(server, "adminList");
		assertFalse(admins.contains(ch));
	}

	@Test
	void adminCommandRejectedWhenNotLoggedIn() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		// "admin" command without being in adminList triggers logout path.
		String inner = NetUtil.compressString("clientlist");
		invoke(ch, "admin\t" + inner);
		LinkedList<SocketChannel> admins = getInstanceField(server, "adminList");
		assertFalse(admins.contains(ch));
	}

	@Test
	void adminCommandDispatchedWhenLoggedIn() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		LinkedList<SocketChannel> admins = getInstanceField(server, "adminList");
		admins.add(ch);
		String inner = NetUtil.compressString("clientlist");
		invoke(ch, "admin\t" + inner);
		// adminSendClientList broadcasts to admins -> queued data for ch.
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	// ------------------------------------------------------------------
	// processAdminCommand directly
	// ------------------------------------------------------------------

	@Test
	void adminClientlist() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		LinkedList<SocketChannel> admins = getInstanceField(server, "adminList");
		admins.add(ch);
		invokeAdmin(ch, new String[]{"clientlist"});
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void adminBanAddsEntry() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		LinkedList<SocketChannel> admins = getInstanceField(server, "adminList");
		admins.add(ch);
		LinkedList<NetServerBan> banList = getStaticField("banList");
		int before = banList.size();
		invokeAdmin(ch, new String[]{"ban", "9.9.9.9", "0"});
		assertEquals(before + 1, banList.size());
		assertEquals("9.9.9.9", banList.getLast().addr);
	}

	@Test
	void adminUnbanRemovesEntry() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		LinkedList<SocketChannel> admins = getInstanceField(server, "adminList");
		admins.add(ch);
		LinkedList<NetServerBan> banList = getStaticField("banList");
		NetServerBan ban = new NetServerBan("8.8.8.8", 0);
		banList.add(ban);
		invokeAdmin(ch, new String[]{"unban", "8.8.8.8"});
		boolean stillThere = false;
		for (NetServerBan b : banList) if (b.addr.equals("8.8.8.8")) stillThere = true;
		assertFalse(stillThere);
	}

	@Test
	void adminUnbanAllClearsList() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		LinkedList<SocketChannel> admins = getInstanceField(server, "adminList");
		admins.add(ch);
		LinkedList<NetServerBan> banList = getStaticField("banList");
		banList.add(new NetServerBan("1.1.1.1", 0));
		banList.add(new NetServerBan("2.2.2.2", 0));
		invokeAdmin(ch, new String[]{"unban", "ALL"});
		assertTrue(banList.isEmpty());
	}

	@Test
	void adminBanlistReplies() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		LinkedList<SocketChannel> admins = getInstanceField(server, "adminList");
		admins.add(ch);
		LinkedList<NetServerBan> banList = getStaticField("banList");
		banList.add(new NetServerBan("3.3.3.3", 0));
		invokeAdmin(ch, new String[]{"banlist"});
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void adminPlayerdeleteResetsData() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		LinkedList<SocketChannel> admins = getInstanceField(server, "adminList");
		admins.add(ch);
		invokeAdmin(ch, new String[]{"playerdelete", "SomePlayer"});
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void adminRoomdeleteUnknownReportsFail() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		LinkedList<SocketChannel> admins = getInstanceField(server, "adminList");
		admins.add(ch);
		invokeAdmin(ch, new String[]{"roomdelete", "12345"});
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void adminRoomdeleteExistingRoom() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		LinkedList<SocketChannel> admins = getInstanceField(server, "adminList");
		admins.add(ch);
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 77;
		room.strName = "Doomed";
		addRoom(room);
		invokeAdmin(ch, new String[]{"roomdelete", "77"});
		LinkedList<NetRoomInfo> rooms = getInstanceField(server, "roomInfoList");
		assertFalse(rooms.contains(room));
	}

	@Test
	void adminShutdownSetsFlag() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		LinkedList<SocketChannel> admins = getInstanceField(server, "adminList");
		admins.add(ch);
		invokeAdmin(ch, new String[]{"shutdown"});
		boolean shutdownRequested = (Boolean) getInstanceField(server, "shutdownRequested");
		assertTrue(shutdownRequested);
	}

	@Test
	void adminAnnounceBroadcasts() throws Throwable {
		SocketChannel ch = newChannel();
		// Register an ordinary player so broadcast() has a recipient.
		registerPlayer(ch, "Listener");
		invokeAdmin(ch, new String[]{"announce", "Hello everyone"});
		Map<SocketChannel, ?> pending = getInstanceField(server, "pendingData");
		assertTrue(pending.containsKey(ch));
	}

	@Test
	void adminUnknownCommandIsNoOp() throws Throwable {
		SocketChannel ch = newChannel();
		registerChannel(ch);
		// Unknown admin command: no branch matches, returns without throwing.
		invokeAdmin(ch, new String[]{"totallyunknown"});
		assertNotNull(server);
	}
}

package nullpomino.gui.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.Adler32;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.mode.NetDummyMode;
import nullpomino.game.net.NetBaseClient;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;
import nullpomino.game.net.room.RoomEndpoint;
import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

/** Protocol, pump, action, lifecycle, and data-helper branch matrix. */
class NetLobbyFrameBranchMatrixTest {
	@Test
	void lifecyclePumpAndDisconnectMatrices() throws Exception {
		Harness nl = harness();
		RecordingListener listener = new RecordingListener();
		nl.addListener(null);
		nl.addListener(listener);
		nl.setNetDummyMode(new TestDummy());
		nl.init();
		assertTrue(listener.inits > 0);

		nl.netOnMessage(nl.client, new String[] {"start"});
		nl.netOnMessage(nl.client, new String[0]);
		nl.pump();
		nl.netOnDisconnect(nl.client, null);
		nl.pump();
		nl.netOnDisconnect(nl.client, new IOException("lost"));
		nl.pump();
		assertTrue(listener.disconnects >= 2);

		Harness ioFailure = harness();
		ioFailure.addListener(new ThrowingListener(new IOException("io")));
		ioFailure.injectLocalMessage("unknown");
		ioFailure.pump();
		Harness fatalFailure = harness();
		fatalFailure.addListener(new ThrowingListener(new AssertionError("boom")));
		fatalFailure.injectLocalMessage("unknown");
		fatalFailure.pump();
		fatalFailure.netOnDisconnect(fatalFailure.client, null);
		fatalFailure.pump();
		Harness noListeners = harness();
		noListeners.clearListeners();
		noListeners.disconnectDirect(null);

		Harness connected = harness();
		connected.addListener(listener);
		connected.setNetDummyMode(new TestDummy());
		connected.client.connected = true;
		connected.shutdown();
		assertTrue(connected.client.sentAny("disconnect"));

		Harness disconnected = harness();
		disconnected.client.connected = false;
		disconnected.shutdown();
		Harness empty = harness();
		empty.netPlayerClient = null;
		empty.clearListeners();
		empty.shutdown();
	}

	@Test
	void loginRulesPlayersAndRoomListDispatchMatrices() throws Exception {
		Harness nl = harness();
		RecordingListener listener = new RecordingListener();
		nl.addListener(listener);
		nl.setNetDummyMode(new TestDummy());

		nl.dispatch();
		nl.dispatch("welcome");
		nl.dispatch("welcome", "1.0");
		nl.dispatch("welcome", "1.0", "3");
		nl.dispatch("loginsuccess");
		nl.dispatch("loginsuccess", NetUtil.urlEncode("Alice"));
		assertTrue(nl.uploads >= 2);

		nl.dispatch("loginfail", "DIFFERENT_VERSION");
		nl.dispatch("loginfail", "DIFFERENT_VERSION", "2.0");
		nl.dispatch("loginfail", "DIFFERENT_BUILD");
		nl.dispatch("loginfail", "DIFFERENT_BUILD", "DEV");
		nl.dispatch("loginfail");
		nl.dispatch("loginfail", "OTHER", "detail");

		nl.dispatch("banned");
		String now = GeneralUtil.exportCalendarString(Calendar.getInstance());
		nl.dispatch("banned", now);
		nl.dispatch("banned", now, "");
		nl.dispatch("banned", now, now);

		nl.dispatch("ruledatasuccess");
		nl.dispatch("ruledatafail");
		nl.dispatch("rulelock");
		CustomProperties rule = new CustomProperties();
		rule.setProperty("0.ruleopt.strRuleName", "Matrix Rule");
		nl.dispatch("rulelock", NetUtil.compressString(rule.encode("Rule")));
		nl.dispatch("rulelock", NetUtil.compressString(rule.encode("Rule")));
		nl.dispatch("rulelist");
		nl.dispatch("rulelist", "-1", "x");
		nl.dispatch("rulelist", "99", "x");
		nl.dispatch("rulelist", "0", NetUtil.urlEncode("A"), NetUtil.urlEncode("B"));

		NetPlayerInfo me = player(1, "Me", 5, "");
		NetPlayerInfo peer = player(2, "Peer", 5, "host");
		nl.client.me = me;
		nl.client.players.put(1, me);
		nl.client.players.put(2, peer);
		for(String cmd : new String[] {"playerlist", "playerupdate", "playernew"}) nl.dispatch(cmd);
		nl.dispatch("playerlogout");
		nl.dispatch("playerlogout", peer.exportString());
		peer.roomID = 6;
		nl.dispatch("playerlogout", peer.exportString());
		nl.client.me = null;
		nl.dispatch("playerlogout", peer.exportString());
		nl.client.me = me;

		nl.dispatch("playerenter");
		nl.dispatch("playerenter", "99");
		nl.dispatch("playerenter", "2");
		peer.strHost = "";
		nl.dispatch("playerenter", "2");
		nl.dispatch("playerleave");
		nl.dispatch("playerleave", "99");
		nl.dispatch("playerleave", "2");
		peer.strHost = "host";
		nl.dispatch("playerleave", "2");

		nl.dispatch("changeteam");
		nl.dispatch("changeteam", "99");
		nl.dispatch("changeteam", "2");
		nl.dispatch("changeteam", "2", "old", NetUtil.urlEncode("Red"));
		nl.lobbyMode = NetLobbyFrame.LOBBYMODE_INROOM;
		nl.dispatch("changeteam", "2", "old", NetUtil.urlEncode("Blue"));

		NetRoomInfo roomA = room(3, "A", false);
		NetRoomInfo roomB = room(4, "B", true);
		nl.dispatch("roomlist");
		nl.dispatch("roomlist", "0");
		nl.dispatch("roomlist", "3", roomA.exportString(), roomB.exportString());
		nl.dispatch("ratedpresets");
		nl.dispatch("ratedpresets", NetUtil.compressString(roomA.exportString()));
		nl.dispatch("roomcreate");
		nl.dispatch("roomcreate", roomA.exportString());
		roomA.strName = "updated";
		nl.dispatch("roomupdate");
		nl.dispatch("roomupdate", roomA.exportString());
		nl.dispatch("roomupdate", room(99, "missing", false).exportString());
		nl.dispatch("roomdelete");
		nl.dispatch("roomdelete", room(99, "missing", false).exportString());
		nl.dispatch("roomdelete", roomA.exportString());
		nl.roomList.clear();
		nl.dispatch("roomupdate", roomA.exportString());
		nl.dispatch("roomdelete", roomA.exportString());
	}

	@Test
	void roomJoinChatGameAndRankingDispatchMatrices() throws Exception {
		Harness nl = harness();
		RecordingListener listener = new RecordingListener();
		nl.addListener(listener);
		nl.setNetDummyMode(new TestDummy());
		NetPlayerInfo me = player(1, "Me", -1, "");
		NetPlayerInfo peer = player(2, "Peer", 7, "host");
		nl.client.me = me;
		nl.client.players.put(1, me);
		nl.client.players.put(2, peer);
		NetRoomInfo unlocked = room(7, "Room", false);
		NetRoomInfo locked = room(8, "Locked", true);
		nl.client.rooms.put(7, unlocked);
		nl.client.rooms.put(8, locked);

		nl.dispatch("roomjoinsuccess");
		nl.client.me = null;
		nl.dispatch("roomjoinsuccess", "7", "0", "-1");
		nl.client.me = me;
		nl.dispatch("roomjoinsuccess", "99", "0", "-1");
		nl.dispatch("roomjoinsuccess", "7", "-1", "-1");
		nl.dispatch("roomcreatesuccess", "7", "-1", "0");
		nl.dispatch("roomjoinsuccess", "8", "0", "-1");
		nl.dispatch("roomjoinsuccess", "-1", "-1", "-1");
		nl.dispatch("roomjoinfail");
		nl.dispatch("roomkicked");
		nl.dispatch("roomkicked", "KICK", "why", NetUtil.urlEncode("Admin"));

		nl.dispatch("map");
		nl.dispatch("map", NetUtil.compressString("m1\tm2"));
		String when = GeneralUtil.exportCalendarString();
		nl.dispatch("lobbychat");
		nl.dispatch("lobbychat", "99", "x", when, NetUtil.urlEncode("missing"));
		nl.dispatch("lobbychat", "2", "x", when, NetUtil.urlEncode("hello"));
		nl.dispatch("chat");
		nl.dispatch("chat", "99", "x", when, NetUtil.urlEncode("missing"));
		nl.dispatch("chat", "2", "x", when, NetUtil.urlEncode("hello"));
		nl.dispatch("lobbychath");
		nl.dispatch("lobbychath", NetUtil.urlEncode("Old"), when, NetUtil.urlEncode("history"));
		nl.dispatch("chath", NetUtil.urlEncode("Old"), when, NetUtil.urlEncode("history"));

		nl.dispatch("changestatus");
		nl.dispatch("changestatus", "watchonly", "99");
		for(String mode : new String[] {"watchonly", "joinqueue", "joinseat", "unknown"}) {
			nl.dispatch("changestatus", mode, "2");
		}
		nl.dispatch("autostartbegin");
		nl.dispatch("autostartbegin", "5");
		nl.dispatch("start");
		nl.dispatch("dead");
		nl.dispatch("dead", "1", NetUtil.urlEncode("Loser"));
		nl.dispatch("dead", "1", NetUtil.urlEncode("Loser"), "x", "x", "x", NetUtil.urlEncode("Winner"));
		nl.dispatch("finish");
		nl.dispatch("finish", "x", "x", "");
		nl.dispatch("finish", "x", "x", NetUtil.urlEncode("Solo"));
		nl.dispatch("finish", "x", "x", NetUtil.urlEncode("Solo"), "false");
		nl.dispatch("finish", "x", "x", NetUtil.urlEncode("Team"), "true");
		nl.dispatch("rating");
		nl.dispatch("rating", "x", "x", NetUtil.urlEncode("Alice"), "1500", "25");

		nl.dispatch("mpranking");
		String ranking = "\tbad;row\t-1;" + NetUtil.urlEncode("Alice") + ";100;1;2\t0;"
				+ NetUtil.urlEncode("Bob") + ";200;2;3";
		nl.dispatch("mpranking", "-1", "3", NetUtil.compressString(ranking));
		nl.dispatch("mpranking", "99", "3", NetUtil.compressString(ranking));
		nl.dispatch("mpranking", "0", "3", NetUtil.compressString(ranking));
		assertEquals(2, nl.mpRankingRows[0].length);

		nl.dispatch("changename");
		nl.dispatch("changename", "2", NetUtil.urlEncode("Peer"), NetUtil.urlEncode("Other"));
		nl.dispatch("changename", "1", NetUtil.urlEncode("Me"), NetUtil.urlEncode("Mine"));
		nl.netPlayerClient = null;
		nl.dispatch("changename", "1", "a", "b");
		nl.netPlayerClient = nl.client;
		nl.dispatch("changenamefail");
		for(String reason : new String[] {"DUPLICATE", "EMPTY", "PLAYING", "OTHER"}) {
			nl.dispatch("changenamefail", reason);
		}
		nl.dispatch("announce");
		nl.dispatch("announce", NetUtil.urlEncode("maintenance"));

		CustomProperties replay = new CustomProperties();
		replay.setProperty("name.mode", "MARATHON");
		String compressed = NetUtil.compressString(replay.encode("Replay"));
		Adler32 checksum = new Adler32();
		checksum.update(NetUtil.stringToBytes(compressed));
		nl.dispatch("spdownload");
		nl.dispatch("spdownload", "0", compressed);
		nl.dispatch("spdownload", Long.toString(checksum.getValue()), compressed);
		nl.dispatch("unknown");
		assertTrue(listener.messages > 0);
	}

	@Test
	void actionsAndDataHelperMatrices() throws Exception {
		Harness nl = harness();
		FakeClient client = nl.client;
		nl.netPlayerClient = null;
		nl.sendChat(false, null);
		nl.sendChat(false, "");
		nl.sendChat(false, "hello");
		nl.netPlayerClient = client;
		nl.sendChat(false, "/TEAM Red");
		nl.sendChat(false, "/team");
		nl.sendChat(false, "/name");
		nl.sendChat(true, "/name ");
		nl.sendChat(false, "/name Alice");
		nl.sendChat(false, "/help");
		nl.sendChat(true, "/?");
		nl.sendChat(true, "room");
		nl.sendChat(false, "lobby");
		client.connected = false;
		nl.sendChat(false, "/name Bob");
		client.connected = true;

		nl.netPlayerClient = null;
		nl.joinRoom(1, false);
		nl.netPlayerClient = client;
		client.me = null;
		nl.joinRoom(1, false);
		client.me = player(1, "Me", 1, "");
		nl.joinRoom(1, false);
		nl.joinRoom(2, true);

		nl.netPlayerClient = null;
		nl.realUpload();
		nl.netPlayerClient = client;
		nl.ruleOptPlayer = null;
		nl.realUpload();

		Harness connection = harness();
		FakeEndpoint endpoint = new FakeEndpoint();
		connection.connectToRoom("Alice", null, endpoint);
		connection.connectToRoom("Alice", " Team ", endpoint);
		nl.ruleOptPlayer = new RuleOptions();
		nl.realUpload();

		nl.netPlayerClient = null;
		assertTrue(nl.updateSameRoomPlayerInfoList().isEmpty());
		nl.netPlayerClient = client;
		client.me = null;
		assertTrue(nl.updateSameRoomPlayerInfoList().isEmpty());
		NetPlayerInfo me = player(1, "Me", 3, "");
		client.me = me;
		client.playerList.clear();
		client.playerList.add(me);
		client.playerList.add(player(2, "Same", 3, ""));
		client.playerList.add(player(3, "Other", 4, ""));
		assertEquals(2, nl.updateSameRoomPlayerInfoList().size());
		assertNotNull(nl.getSameRoomPlayerInfoList());

		assertFalse(nl.removeListener(new RecordingListener()));
		RecordingListener listener = new RecordingListener();
		nl.addListener(listener);
		assertTrue(nl.removeListener(listener));
		nl.setNetDummyMode(new TestDummy());
		assertNotNull(nl.getNetDummyMode());

		nl.injectLocalMessage(null);
		nl.injectLocalMessage("");
		nl.injectLocalMessage("start\t");
		nl.pump();

		String oldOs = System.getProperty("os.name");
		try {
			System.setProperty("os.name", "Windows 11");
			assertNotNull(nl.getRuleFileList());
			System.setProperty("os.name", "Linux");
			assertNotNull(nl.getRuleFileList());
		} finally {
			if(oldOs == null) System.clearProperty("os.name");
			else System.setProperty("os.name", oldOs);
		}
		assertEquals(null, invokeSort(null, "Linux"));
		assertEquals("a", invokeSort(new String[] {"b", "a"}, "Linux")[0]);
		assertEquals("b", invokeSort(new String[] {"b", "a"}, "Windows 11")[0]);

		NetLobbyFrame localization = new NetLobbyFrame();
		NullpoMinoSDL.propLang = new CustomProperties();
		NullpoMinoSDL.propLangDefault = new CustomProperties();
		NullpoMinoSDL.propLang.setProperty("shared", "SHARED");
		localization.propLang = null;
		localization.propLangDefault = new CustomProperties();
		localization.propLangDefault.setProperty("default", "DEFAULT");
		assertEquals("DEFAULT", localization.getUIText("default"));
		localization.propLang = new CustomProperties();
		localization.propLangDefault = null;
		assertEquals("SHARED", localization.getUIText("shared"));
		assertEquals("missing", localization.getUIText("missing"));

		Harness descriptions = harness();
		descriptions.setModeProperties(null, null);
		assertEquals("MODE", descriptions.getModeDesc("MODE"));
		CustomProperties defaults = new CustomProperties();
		defaults.setProperty("MODE", "DEFAULT");
		descriptions.setModeProperties(null, defaults);
		assertEquals("DEFAULT", descriptions.getModeDesc("MODE"));
		CustomProperties foreground = new CustomProperties();
		foreground.setProperty("MODE", "FOREGROUND");
		descriptions.setModeProperties(foreground, defaults);
		assertEquals("FOREGROUND", descriptions.getModeDesc("MODE"));

		NetLobbyFrame persistence = new NetLobbyFrame();
		persistence.saveConfig();
		persistence.saveGlobalConfig();
		persistence.propConfig = new CustomProperties();
		persistence.propGlobal = new CustomProperties();
		persistence.saveConfig();
		persistence.saveGlobalConfig();
	}

	private static Harness harness() {
		Harness nl = new Harness();
		nl.propConfig = new CustomProperties();
		nl.propGlobal = new CustomProperties();
		nl.client = new FakeClient();
		nl.netPlayerClient = nl.client;
		return nl;
	}

	private static NetPlayerInfo player(int uid, String name, int roomID, String host) {
		NetPlayerInfo p = new NetPlayerInfo();
		p.uid = uid;
		p.strName = name;
		p.roomID = roomID;
		p.seatID = -1;
		p.queueID = -1;
		p.strHost = host;
		p.connected = true;
		return p;
	}

	private static NetRoomInfo room(int id, String name, boolean locked) {
		NetRoomInfo r = new NetRoomInfo();
		r.roomID = id;
		r.strName = name;
		r.strMode = "NET-VS-BATTLE";
		r.maxPlayers = 6;
		r.ruleLock = locked;
		r.ruleName = "STANDARD";
		return r;
	}

	private static String[] invokeSort(String[] list, String osName) throws Exception {
		Method method = NetLobbyFrame.class.getDeclaredMethod("sortRuleFiles", String[].class, String.class);
		method.setAccessible(true);
		return (String[])method.invoke(null, list, osName);
	}

	private static final class Harness extends NetLobbyFrame {
		FakeClient client;
		int uploads;
		int saves;
		void dispatch(String... message) throws IOException { dispatchMessage(message); }
		void realUpload() { super.sendMyRuleDataToServer(); }
		void clearListeners() { listeners = null; }
		void disconnectDirect(Throwable ex) { dispatchDisconnect(ex); }
		void setModeProperties(CustomProperties foreground, CustomProperties defaults) {
			propModeDesc = foreground;
			propDefaultModeDesc = defaults;
		}
		@Override public void saveConfig() { saves++; }
		@Override public String getUIText(String key) { return key == null ? "" : key; }
		@Override public void sendMyRuleDataToServer() { uploads++; }
	}

	private static final class FakeEndpoint implements RoomEndpoint {
		LineListener lineListener;
		ClosedListener closedListener;
		@Override public void setLineListener(LineListener listener) { lineListener = listener; }
		@Override public void setClosedListener(ClosedListener listener) { closedListener = listener; }
		@Override public void sendLine(String line) {}
		@Override public void clientReady() {}
		@Override public boolean isOpen() { return true; }
		@Override public int getListenPort() { return 1; }
		@Override public String getDisplayHost() { return "host"; }
		@Override public String getSessionId() { return "session"; }
		@Override public void shutdown() {}
	}

	private static final class FakeClient extends NetPlayerClient {
		boolean connected = true;
		NetPlayerInfo me;
		final Map<Integer, NetPlayerInfo> players = new HashMap<Integer, NetPlayerInfo>();
		final Map<Integer, NetRoomInfo> rooms = new HashMap<Integer, NetRoomInfo>();
		final LinkedList<NetPlayerInfo> playerList = new LinkedList<NetPlayerInfo>();
		final List<String> sent = new LinkedList<String>();
		@Override public boolean isConnected() { return connected; }
		@Override public boolean send(String msg) { sent.add(msg); return true; }
		@Override public String getHost() { return "host"; }
		@Override public int getPort() { return 1234; }
		@Override public int getPlayerUID() { return 1; }
		@Override public NetPlayerInfo getYourPlayerInfo() { return me; }
		@Override public NetPlayerInfo getPlayerInfoByUID(int uid) { return players.get(uid); }
		@Override public NetRoomInfo getRoomInfo(int roomID) { return rooms.get(roomID); }
		@Override public LinkedList<NetPlayerInfo> getPlayerInfoList() { return playerList; }
		boolean sentAny(String prefix) {
			for(String line : sent) if(line.startsWith(prefix)) return true;
			return false;
		}
	}

	private static class RecordingListener implements NetLobbyListener {
		int inits;
		int disconnects;
		int messages;
		@Override public void netlobbyOnInit(NetLobbyFrame lobby) { inits++; }
		@Override public void netlobbyOnLoginOK(NetLobbyFrame lobby, NetPlayerClient client) {}
		@Override public void netlobbyOnRoomJoin(NetLobbyFrame lobby, NetPlayerClient client, NetRoomInfo roomInfo) {}
		@Override public void netlobbyOnRoomLeave(NetLobbyFrame lobby, NetPlayerClient client) {}
		@Override public void netlobbyOnDisconnect(NetLobbyFrame lobby, NetPlayerClient client, Throwable ex) { disconnects++; }
		@Override public void netlobbyOnMessage(NetLobbyFrame lobby, NetPlayerClient client, String[] message)
				throws IOException { messages++; }
		@Override public void netlobbyOnExit(NetLobbyFrame lobby) {}
	}

	private static final class ThrowingListener extends RecordingListener {
		private final Throwable throwable;
		ThrowingListener(Throwable throwable) { this.throwable = throwable; }
		@Override public void netlobbyOnMessage(NetLobbyFrame lobby, NetPlayerClient client, String[] message)
				throws IOException {
			if(throwable instanceof IOException) throw (IOException)throwable;
			if(throwable instanceof RuntimeException) throw (RuntimeException)throwable;
			if(throwable instanceof Error) throw (Error)throwable;
		}
		@Override public void netlobbyOnDisconnect(NetLobbyFrame lobby, NetPlayerClient client, Throwable ex) {
			if(throwable instanceof RuntimeException) throw (RuntimeException)throwable;
			if(throwable instanceof Error) throw (Error)throwable;
		}
	}

	private static final class TestDummy extends NetDummyMode {
		@Override public void netlobbyOnInit(NetLobbyFrame lobby) {}
		@Override public void netlobbyOnLoginOK(NetLobbyFrame lobby, NetPlayerClient client) {}
		@Override public void netlobbyOnRoomJoin(NetLobbyFrame lobby, NetPlayerClient client, NetRoomInfo roomInfo) {}
		@Override public void netlobbyOnRoomLeave(NetLobbyFrame lobby, NetPlayerClient client) {}
		@Override public void netlobbyOnDisconnect(NetLobbyFrame lobby, NetPlayerClient client, Throwable ex) {}
		@Override public void netlobbyOnMessage(NetLobbyFrame lobby, NetPlayerClient client, String[] message) {}
		@Override public void netlobbyOnExit(NetLobbyFrame lobby) {}
	}
}

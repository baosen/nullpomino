package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.LinkedList;

import nullpomino.game.component.Block;
import nullpomino.game.component.RuleOptions;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tail-coverage tests for {@link NetDummyMode}: targets netplayInit -> netOnJoin
 * (locked-rule path), netlobbyOnMessage playerlogout, netUpdatePlayerExist
 * seat-id branches, netDrawAllPlayersCount (connected), netDrawSpectatorsCount
 * SETTING+ranking branch, netSendField compressed branch, and netSendReplay
 * send-OK branch.
 */
class NetDummyModeTailCoverageTest {

	private GameManager manager;
	private GameEngine engine;
	private EventReceiver receiver;

	@BeforeEach
	void setUp() {
		receiver = new EventReceiver();
		manager = new GameManager(receiver);
		manager.init();
		manager.engine[0].init();
		engine = manager.engine[0];
		engine.ruleopt.fieldWidth = 10;
		engine.ruleopt.fieldHeight = 20;
	}

	/** A NetPlayerClient that pretends to be connected and in a room. */
	static class FakeClient extends NetPlayerClient {
		NetRoomInfo room;
		NetPlayerInfo me = new NetPlayerInfo();
		FakeClient(NetRoomInfo room) { this.room = room; this.me.seatID = 0; }
		@Override public boolean isConnected() { return true; }
		@Override public NetRoomInfo getCurrentRoomInfo() { return room; }
		@Override public NetPlayerInfo getYourPlayerInfo() { return me; }
		@Override public int getObserverCount() { return 1; }
		@Override public int getPlayerCount() { return 2; }
	}

	/** A NetLobbyFrame returning a fixed same-room player list. */
	static class FakeLobby extends NetLobbyFrame {
		LinkedList<NetPlayerInfo> sameRoom = new LinkedList<>();
		@Override public LinkedList<NetPlayerInfo> updateSameRoomPlayerInfoList() {
			return sameRoom;
		}
	}

	/** A NetDummyMode that allows the net-ranking view/send paths. */
	static class RankingOkMode extends NetDummyMode {
		@Override protected boolean netIsNetRankingViewOK(GameEngine e) { return true; }
		@Override protected boolean netIsNetRankingSendOK(GameEngine e) { return true; }
	}

	// --- 160, 642-648, 671-674: netplayInit -> netOnJoin locked-rule + player count ---
	@Test
	void netplayInitJoinsRoomWithLockedRule() {
		NetDummyMode mode = new NetDummyMode();
		mode.owner = manager;

		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 7;
		room.ruleLock = true;

		FakeLobby lobby = new FakeLobby();
		lobby.netPlayerClient = new FakeClient(room);
		lobby.ruleOptLock = new RuleOptions();
		// Same-room players: one with seat 0 (name) and one spectator (seat -1)
		NetPlayerInfo p0 = new NetPlayerInfo();
		p0.roomID = 7; p0.seatID = 0; p0.strName = "host";
		NetPlayerInfo pSpec = new NetPlayerInfo();
		pSpec.roomID = 7; pSpec.seatID = -1;
		lobby.sameRoom.add(p0);
		lobby.sameRoom.add(pSpec);

		mode.netplayInit(lobby);

		assertEquals(mode, lobby.getNetDummyMode());
		assertEquals(1, mode.netNumSpectators);
	}

	// --- 495: netlobbyOnMessage playerlogout for current room ---
	@Test
	void netlobbyOnMessagePlayerLogoutCurrentRoom() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		mode.owner = manager;
		mode.netCurrentRoomInfo = new NetRoomInfo();
		mode.netCurrentRoomInfo.roomID = 5;

		NetPlayerInfo p = new NetPlayerInfo();
		p.roomID = 5;
		String msg1 = p.exportString();

		mode.netlobbyOnMessage(null, null, new String[]{"playerlogout", msg1});

		// netUpdatePlayerExist was invoked; with no lobby it resets to defaults
		assertEquals(0, mode.netNumSpectators);
	}

	// --- 688-693: netDrawAllPlayersCount when connected ---
	@Test
	void netDrawAllPlayersCountConnected() {
		NetDummyMode mode = new NetDummyMode();
		mode.owner = manager;
		mode.netLobby = new FakeLobby();
		mode.netLobby.netPlayerClient = new FakeClient(new NetRoomInfo());

		mode.netDrawAllPlayersCount(engine);
	}

	// --- 735-738: netDrawSpectatorsCount SETTING + net-ranking view OK ---
	@Test
	void netDrawSpectatorsCountSettingRankingOk() {
		RankingOkMode mode = new RankingOkMode();
		mode.owner = manager;
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netNumSpectators = 2;
		engine.stat = GameEngine.Status.SETTING;

		mode.netDrawSpectatorsCount(engine, 0, 0);
	}

	// --- 851-852: netSendField compressed branch (large repetitive field) ---
	@Test
	void netSendFieldCompressed() {
		NetDummyMode mode = new NetDummyMode();
		mode.owner = manager;
		mode.netAlwaysSendFieldAttributes = true;
		mode.netLobby = new NetLobbyFrame();
		mode.netLobby.netPlayerClient = new NetPlayerClient();
		engine.createFieldIfNeeded();
		// Fill the field densely so the serialized string compresses smaller.
		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		for (int y = 0; y < h; y++)
			for (int x = 0; x < w; x++)
				engine.field.setBlock(x, y, new Block(Block.BLOCK_COLOR_RED,
						0, Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_OUTLINE));

		// The dense repetitive field compresses smaller than its raw form,
		// so netSendField takes the "isCompressed = true" branch.
		mode.netSendField(engine);
	}

	// --- 1365-1377: netSendReplay send-OK path ---
	@Test
	void netSendReplaySendOk() throws Exception {
		RankingOkMode mode = new RankingOkMode();
		mode.owner = manager;
		mode.netLobby = new NetLobbyFrame();
		mode.netLobby.netPlayerClient = new NetPlayerClient();
		manager.replayProp = new nullpomino.util.CustomProperties();
		engine.statistics.score = 12345;
		engine.statistics.lines = 50;

		Method m = NetDummyMode.class.getDeclaredMethod("netSendReplay", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);

		// send-OK path leaves status before-send (0), not the failure value (2)
		assertEquals(0, mode.netReplaySendStatus);
	}
}

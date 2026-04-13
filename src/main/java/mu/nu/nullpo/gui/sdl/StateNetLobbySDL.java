package mu.nu.nullpo.gui.sdl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import mu.nu.nullpo.game.component.RuleOptions;
import mu.nu.nullpo.game.net.NetPlayerClient;
import mu.nu.nullpo.game.net.NetPlayerInfo;
import mu.nu.nullpo.game.net.NetRoomInfo;
import mu.nu.nullpo.game.net.NetUtil;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.game.play.GameManager;
import mu.nu.nullpo.gui.net.NetLobby;
import mu.nu.nullpo.gui.net.NetLobbyListener;
import mu.nu.nullpo.gui.sdl.binding.SDL3;
import mu.nu.nullpo.util.CustomProperties;
import mu.nu.nullpo.util.GeneralUtil;
import nuklear.Backend;
import nuklear.Nuklear4j;
import nuklear.swig.nk_button_behavior;
import nuklear.swig.nk_color;
import nuklear.swig.nk_context;
import nuklear.swig.nk_edit_types;
import nuklear.swig.nk_layout_format;
import nuklear.swig.nk_panel;
import nuklear.swig.nk_panel_flags;
import nuklear.swig.nk_rect;
import nuklear.swig.nk_text_alignment;
import nuklear.swig.nuklear;

/**
 * Nuklear-based netplay lobby state for the SDL version.
 *
 * The Swing lobby is the behavioral source of truth. This state mirrors the
 * same room creation/detail/rated/rule flows but in a single-window Nuklear UI.
 */
public class StateNetLobbySDL extends BaseStateSDL implements NetLobbyListener {
	static final Logger log = Logger.getLogger(StateNetLobbySDL.class);

	private static final int SCREEN_SERVER_SELECT = 0;
	private static final int SCREEN_CONNECTING = 1;
	private static final int SCREEN_LOBBY = 2;
	private static final int SCREEN_SERVER_ADD = 3;
	private static final int SCREEN_CREATERATED_WAITING = 4;
	private static final int SCREEN_CREATERATED = 5;
	private static final int SCREEN_CREATEROOM = 6;
	private static final int SCREEN_CREATEROOM1P = 7;
	private static final int SCREEN_MPRANKING = 8;
	private static final int SCREEN_RULECHANGE = 9;

	private static final int TAB_MAIN = 0;
	private static final int TAB_SPEED = 1;
	private static final int TAB_BONUS = 2;
	private static final int TAB_GARBAGE = 3;
	private static final int TAB_MISC = 4;
	private static final int TAB_PRESET = 5;

	private static final int CHAT_LOG_LIMIT = 200;
	private static final int MAX_VISIBLE_ROOMS = 18;
	private static final int MAX_VISIBLE_PLAYERS = 18;

	private static final String[] CREATE_ROOM_TABS = {
		"Main", "Speed", "Bonus", "Garbage", "Misc", "Preset"
	};
	private static final String[] SPIN_BONUS_NAMES = {"Off", "T-Only", "All Spin"};
	private static final String[] SPIN_CHECK_NAMES = {"4-Point", "Immobile"};
	private static final String[] TUNING_GENERIC_NAMES = {"Auto", "Disable", "Enable"};
	private static final String[] TUNING_ROTATE_NAMES = {"Auto", "Left", "Right"};
	private static final String[] TUNING_OUTLINE_NAMES = {"Auto", "None", "Normal", "Connect", "Same Color"};
	private static final String YOUR_RULE_LABEL = "(Your current rule)";

	private nk_context nkCtx;
	private nk_color bgColor;
	private nk_panel serverSelectPanel;
	private nk_panel connectingPanel;
	private nk_panel lobbyPanel;
	private nk_panel serverAddPanel;
	private nk_panel createRatedWaitingPanel;
	private nk_panel createRatedPanel;
	private nk_panel createRoomPanel;
	private nk_panel createRoom1PPanel;
	private nk_panel rankingPanel;
	private nk_panel rulePanel;

	private NetLobbySDL netLobby;
	private CustomProperties propConfig;
	private CustomProperties propGlobal;
	private CustomProperties propModeDesc;
	private CustomProperties propDefaultModeDesc;

	private int currentScreen = SCREEN_SERVER_SELECT;
	private String statusMessage = "";
	private boolean handoffToNetGame;
	private String handoffModeName;

	private final ConcurrentLinkedQueue<Runnable> pendingActions = new ConcurrentLinkedQueue<Runnable>();

	private final LinkedList<String> serverList = new LinkedList<String>();
	private int selectedServer;
	private final byte[] playerNameBuf = new byte[64];
	private final int[] playerNameLen = new int[] {0};
	private final byte[] playerTeamBuf = new byte[64];
	private final int[] playerTeamLen = new int[] {0};
	private final byte[] serverAddBuf = new byte[128];
	private final int[] serverAddLen = new int[] {0};

	private final byte[] chatInputBuf = new byte[256];
	private final int[] chatInputLen = new int[] {0};
	private final LinkedList<String> chatLog = new LinkedList<String>();

	private final byte[] createRatedNameBuf = new byte[64];
	private final int[] createRatedNameLen = new int[] {0};
	private final int[] createRatedPresetIndex = new int[] {0};
	private final int[] createRatedMaxPlayers = new int[] {6};
	private final LinkedList<NetRoomInfo> presets = new LinkedList<NetRoomInfo>();

	private final RoomFormState createRoomState = new RoomFormState();
	private int createRoomTab;
	private NetRoomInfo backupRoomInfo;
	private int currentViewDetailRoomID = -1;

	private final int[] createRoom1PModeIndex = new int[] {0};
	private final int[] createRoom1PRuleIndex = new int[] {0};
	private NetRoomInfo backupRoomInfo1P;

	private final LinkedList<String[]> rankingData = new LinkedList<String[]>();
	private int rankingMyRank = -1;

	private final int[] ruleSelectedIndexByStyle = new int[GameEngine.MAX_GAMESTYLE];
	private final TuningState tuningState = new TuningState();
	private final LinkedList<RuleEntry> ruleEntries = new LinkedList<RuleEntry>();
	private int ruleChangeTab;
	private String[] skinOptionNames = new String[] {"Auto"};

	private String[] multiModeList = new String[0];
	private String[] singleModeList = new String[0];

	static {
		try {
			Nuklear4j.initializeNative();
		} catch(Throwable e) {
			Logger.getLogger(StateNetLobbySDL.class).error("Failed to load nuklear4j native library", e);
		}
	}

	@Override
	public void enter() {
		currentScreen = SCREEN_SERVER_SELECT;
		statusMessage = "";
		handoffToNetGame = false;
		handoffModeName = null;
		currentViewDetailRoomID = -1;
		createRoomTab = TAB_MAIN;
		ruleChangeTab = 0;
		rankingData.clear();
		rankingMyRank = -1;
		presets.clear();
		while(pendingActions.poll() != null) {}

		SDL3.INSTANCE.SDL_StartTextInput(NullpoMinoSDL.window);

		if(NullpoMinoSDL.nkBackend == null && ResourceHolderSDL.ttfFont != null) {
			NullpoMinoSDL.nkBackend = new SDL3Backend(NullpoMinoSDL.renderer, ResourceHolderSDL.ttfFont);
			NullpoMinoSDL.nkBackend.setRenderingSurface(
				NullpoMinoSDL.renderer,
				NullpoMinoSDL.LOGICAL_WIDTH,
				NullpoMinoSDL.LOGICAL_HEIGHT
			);
		}

		nkCtx = new nk_context();
		Backend backend = NullpoMinoSDL.nkBackend;
		Nuklear4j.initializeContext(
			nkCtx,
			NullpoMinoSDL.LOGICAL_WIDTH,
			NullpoMinoSDL.LOGICAL_HEIGHT,
			backend.getMaxCharWidth(),
			backend.getFontHeight()
		);

		bgColor = new nk_color();
		bgColor.setR((short)40);
		bgColor.setG((short)40);
		bgColor.setB((short)50);
		bgColor.setA((short)255);

		serverSelectPanel = new nk_panel();
		connectingPanel = new nk_panel();
		lobbyPanel = new nk_panel();
		serverAddPanel = new nk_panel();
		createRatedWaitingPanel = new nk_panel();
		createRatedPanel = new nk_panel();
		createRoomPanel = new nk_panel();
		createRoom1PPanel = new nk_panel();
		rankingPanel = new nk_panel();
		rulePanel = new nk_panel();

		propConfig = new CustomProperties();
		try {
			FileInputStream in = new FileInputStream("config/setting/netlobby.cfg");
			propConfig.load(in);
			in.close();
		} catch(IOException e) {}

		propGlobal = NullpoMinoSDL.propGlobal;
		propDefaultModeDesc = NullpoMinoSDL.propDefaultModeDesc;
		propModeDesc = new CustomProperties();
		try {
			FileInputStream in = new FileInputStream("config/lang/modedesc_" + Locale.getDefault().getCountry() + ".properties");
			propModeDesc.load(in);
			in.close();
		} catch(IOException e) {}

		multiModeList = loadModeList("config/list/netlobby_multimode.lst");
		singleModeList = loadModeList("config/list/netlobby_singlemode.lst");
		createRuleEntries(getRuleFileList());
		skinOptionNames = buildSkinOptionNames();

		copyStringToBuffer(propConfig.getProperty("serverselect.txtfldPlayerName.text", ""), playerNameBuf, playerNameLen);
		copyStringToBuffer(propConfig.getProperty("serverselect.txtfldPlayerTeam.text", ""), playerTeamBuf, playerTeamLen);
		copyStringToBuffer("", serverAddBuf, serverAddLen);
		clearBuffer(chatInputBuf, chatInputLen);
		chatLog.clear();

		loadServerListFromConfig();
		loadCreateRoomDefaults();
		loadCreateRoom1PDefaults();
		loadRatedDefaults();
		loadRuleChangeDefaults();

		NetLobbySDL transferredLobby = NullpoMinoSDL.transferredNetLobby;
		String transferredStatus = NullpoMinoSDL.transferredNetStatusMessage;
		NullpoMinoSDL.transferredNetLobby = null;
		NullpoMinoSDL.transferredNetMode = null;
		NullpoMinoSDL.transferredNetStatusMessage = "";

		if(transferredLobby != null) {
			netLobby = transferredLobby;
			netLobby.addListener(this);
			serverList.clear();
			serverList.addAll(netLobby.serverList);
			copyStringToBuffer(netLobby.playerName, playerNameBuf, playerNameLen);
			copyStringToBuffer(netLobby.playerTeam, playerTeamBuf, playerTeamLen);
			currentScreen = (netLobby.getNetPlayerClient() != null && netLobby.getNetPlayerClient().isConnected())
				? SCREEN_LOBBY
				: SCREEN_SERVER_SELECT;
		}

		if(transferredStatus != null && transferredStatus.length() > 0) {
			statusMessage = transferredStatus;
		}
	}

	@Override
	public void leave() {
		SDL3.INSTANCE.SDL_StopTextInput(NullpoMinoSDL.window);
		saveNetLobbyConfig();
		shutdownActiveLobby();
	}

	@Override
	public void render() {
		Backend backend = NullpoMinoSDL.nkBackend;
		if(backend == null || nkCtx == null) return;

		Runnable action;
		while((action = pendingActions.poll()) != null) {
			action.run();
		}

		backend.clear(bgColor);
		backend.handleEvent(nkCtx);

		switch(currentScreen) {
			case SCREEN_SERVER_SELECT:
				drawServerSelect();
				break;
			case SCREEN_CONNECTING:
				drawConnecting();
				break;
			case SCREEN_LOBBY:
				drawLobby();
				break;
			case SCREEN_SERVER_ADD:
				drawServerAdd();
				break;
			case SCREEN_CREATERATED_WAITING:
				drawCreateRatedWaiting();
				break;
			case SCREEN_CREATERATED:
				drawCreateRated();
				break;
			case SCREEN_CREATEROOM:
				drawCreateRoom();
				break;
			case SCREEN_CREATEROOM1P:
				drawCreateRoom1P();
				break;
			case SCREEN_MPRANKING:
				drawMPRanking();
				break;
			case SCREEN_RULECHANGE:
				drawRuleChange();
				break;
		}

		backend.render(nkCtx);
	}

	@Override
	public void update() {
		if(handoffToNetGame && netLobby != null) {
			saveNetLobbyConfig();
			netLobby.removeListener(this);
			NullpoMinoSDL.transferredNetLobby = netLobby;
			NullpoMinoSDL.transferredNetMode = handoffModeName;
			NullpoMinoSDL.transferredNetStatusMessage = "";
			netLobby = null;
			handoffToNetGame = false;
			handoffModeName = null;
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NETGAME);
			return;
		}

		if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_B)) {
			handleBackAction();
		}
	}

	private void handleBackAction() {
		switch(currentScreen) {
			case SCREEN_SERVER_SELECT:
				NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_TITLE);
				break;
			case SCREEN_CONNECTING:
				cancelConnection();
				break;
			case SCREEN_SERVER_ADD:
				currentScreen = SCREEN_SERVER_SELECT;
				break;
			case SCREEN_CREATERATED_WAITING:
			case SCREEN_CREATERATED:
				currentViewDetailRoomID = -1;
				currentScreen = SCREEN_LOBBY;
				break;
			case SCREEN_CREATEROOM:
			case SCREEN_CREATEROOM1P:
			case SCREEN_MPRANKING:
			case SCREEN_RULECHANGE:
				currentViewDetailRoomID = -1;
				currentScreen = SCREEN_LOBBY;
				break;
			case SCREEN_LOBBY:
				disconnectToServerSelect();
				break;
			default:
				break;
		}
	}

	private void drawServerSelect() {
		nk_rect bounds = new nk_rect();
		bounds.setX(20);
		bounds.setY(20);
		bounds.setW(600);
		bounds.setH(440);

		long flags = nk_panel_flags.NK_WINDOW_BORDER | nk_panel_flags.NK_WINDOW_TITLE;
		if(nuklear.nk_begin(nkCtx, serverSelectPanel, "NullpoMino Netplay", bounds, flags)) {
			drawLabelValueEdit("Name", playerNameBuf, playerNameLen, 63);
			drawLabelValueEdit("Team", playerTeamBuf, playerTeamLen, 63);

			nuklear.nk_layout_row_dynamic(nkCtx, 28, 1);
			nuklear.nk_label(nkCtx, "Servers", nk_text_alignment.NK_TEXT_LEFT);

			if(serverList.isEmpty()) {
				nuklear.nk_layout_row_dynamic(nkCtx, 26, 1);
				nuklear.nk_label(nkCtx, "(no servers configured)", nk_text_alignment.NK_TEXT_LEFT);
			} else {
				for(int i = 0; i < serverList.size(); i++) {
					nuklear.nk_layout_row_dynamic(nkCtx, 28, 1);
					String label = ((i == selectedServer) ? "> " : "  ") + serverList.get(i);
					if(nuklear.nk_button_label(nkCtx, label, nk_button_behavior.NK_BUTTON_DEFAULT)) {
						selectedServer = i;
					}
				}
			}

			nuklear.nk_layout_row_dynamic(nkCtx, 30, 4);
			if(nuklear.nk_button_label(nkCtx, "Connect", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				connectToServer();
			}
			if(nuklear.nk_button_label(nkCtx, "Add", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				clearBuffer(serverAddBuf, serverAddLen);
				currentScreen = SCREEN_SERVER_ADD;
			}
			if(nuklear.nk_button_label(nkCtx, "Delete", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				deleteSelectedServer();
			}
			if(nuklear.nk_button_label(nkCtx, "Back", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_TITLE);
			}

			drawStatusBlock();
		}
		nuklear.nk_end(nkCtx);
	}

	private void drawConnecting() {
		nk_rect bounds = new nk_rect();
		bounds.setX(120);
		bounds.setY(160);
		bounds.setW(400);
		bounds.setH(150);

		long flags = nk_panel_flags.NK_WINDOW_BORDER | nk_panel_flags.NK_WINDOW_TITLE | nk_panel_flags.NK_WINDOW_NO_SCROLLBAR;
		if(nuklear.nk_begin(nkCtx, connectingPanel, "Connecting", bounds, flags)) {
			nuklear.nk_layout_row_dynamic(nkCtx, 28, 1);
			nuklear.nk_label(nkCtx, statusMessage.length() > 0 ? statusMessage : "Connecting...", nk_text_alignment.NK_TEXT_CENTERED);
			nuklear.nk_layout_row_dynamic(nkCtx, 30, 1);
			if(nuklear.nk_button_label(nkCtx, "Cancel", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				cancelConnection();
			}
		}
		nuklear.nk_end(nkCtx);
	}

	private void drawLobby() {
		nk_rect bounds = new nk_rect();
		bounds.setX(10);
		bounds.setY(10);
		bounds.setW(620);
		bounds.setH(460);

		long flags = nk_panel_flags.NK_WINDOW_BORDER | nk_panel_flags.NK_WINDOW_TITLE;
		if(nuklear.nk_begin(nkCtx, lobbyPanel, "Lobby", bounds, flags)) {
			nuklear.nk_layout_row_dynamic(nkCtx, 26, 1);
			nuklear.nk_label(nkCtx, buildLobbySummary(), nk_text_alignment.NK_TEXT_LEFT);

			drawLabelValueEdit("Team", playerTeamBuf, playerTeamLen, 63);
			nuklear.nk_layout_row_dynamic(nkCtx, 28, 3);
			if(nuklear.nk_button_label(nkCtx, "Apply Team", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				applyLobbyTeamChange();
			}
			if(nuklear.nk_button_label(nkCtx, "Create Room", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				enterCreateRoomFlow();
			}
			if(nuklear.nk_button_label(nkCtx, "Create 1P", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				setCreateRoom1PUIType(false, null);
				currentScreen = SCREEN_CREATEROOM1P;
			}

			nuklear.nk_layout_row_dynamic(nkCtx, 26, 1);
			nuklear.nk_label(nkCtx, "Rooms", nk_text_alignment.NK_TEXT_LEFT);

			List<NetRoomInfo> rooms = getRoomSnapshot();
			if(rooms.isEmpty()) {
				nuklear.nk_layout_row_dynamic(nkCtx, 26, 1);
				nuklear.nk_label(nkCtx, "(no rooms available)", nk_text_alignment.NK_TEXT_LEFT);
			} else {
				int shown = 0;
				for(NetRoomInfo room: rooms) {
					if(shown++ >= MAX_VISIBLE_ROOMS) break;
					drawLobbyRoomRow(room);
				}
			}

			nuklear.nk_layout_row_dynamic(nkCtx, 30, 4);
			if(nuklear.nk_button_label(nkCtx, "Rankings", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				rankingData.clear();
				rankingMyRank = -1;
				currentScreen = SCREEN_MPRANKING;
				if(netLobby != null && netLobby.getNetPlayerClient() != null) {
					netLobby.getNetPlayerClient().send("mpranking\t0\n");
				}
			}
			if(nuklear.nk_button_label(nkCtx, "Change Rules", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				enterRuleChangeScreen();
			}
			if(nuklear.nk_button_label(nkCtx, "Add Server", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				clearBuffer(serverAddBuf, serverAddLen);
				currentScreen = SCREEN_SERVER_ADD;
			}
			if(nuklear.nk_button_label(nkCtx, "Disconnect", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				disconnectToServerSelect();
			}

			nuklear.nk_layout_row_dynamic(nkCtx, 26, 1);
			nuklear.nk_label(nkCtx, "Players", nk_text_alignment.NK_TEXT_LEFT);
			List<NetPlayerInfo> players = getPlayerSnapshot();
			if(players.isEmpty()) {
				nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
				nuklear.nk_label(nkCtx, "(no players)", nk_text_alignment.NK_TEXT_LEFT);
			} else {
				int shown = 0;
				for(NetPlayerInfo p: players) {
					if(shown++ >= MAX_VISIBLE_PLAYERS) break;
					nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
					nuklear.nk_label(nkCtx, formatPlayerSummary(p), nk_text_alignment.NK_TEXT_LEFT);
				}
			}

			nuklear.nk_layout_row_dynamic(nkCtx, 26, 1);
			nuklear.nk_label(nkCtx, "Lobby Chat", nk_text_alignment.NK_TEXT_LEFT);
			int logStart = Math.max(0, chatLog.size() - 10);
			for(int i = logStart; i < chatLog.size(); i++) {
				nuklear.nk_layout_row_dynamic(nkCtx, 22, 1);
				nuklear.nk_label(nkCtx, chatLog.get(i), nk_text_alignment.NK_TEXT_LEFT);
			}

			nuklear.nk_layout_row_begin(nkCtx, nk_layout_format.NK_DYNAMIC, 28, 2);
			nuklear.nk_layout_row_push(nkCtx, 0.8f);
			nuklear.nk_edit_string2(nkCtx, nk_edit_types.NK_EDIT_FIELD, chatInputBuf, chatInputLen, 255);
			nuklear.nk_layout_row_push(nkCtx, 0.2f);
			if(nuklear.nk_button_label(nkCtx, "Send", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				sendLobbyChat();
			}
			nuklear.nk_layout_row_end(nkCtx);

			drawStatusBlock();
		}
		nuklear.nk_end(nkCtx);
	}

	private void drawLobbyRoomRow(NetRoomInfo room) {
		String name = room.strName;
		if(name == null || name.length() <= 0) name = "Room " + room.roomID;

		nuklear.nk_layout_row_dynamic(nkCtx, 22, 1);
		nuklear.nk_label(
			nkCtx,
			"#" + room.roomID + " " + name + " | " + (room.rated ? "Rated" : "Free") + " | Rule: " + getRoomRuleLabel(room),
			nk_text_alignment.NK_TEXT_LEFT
		);

		nuklear.nk_layout_row_begin(nkCtx, nk_layout_format.NK_DYNAMIC, 28, 5);
		nuklear.nk_layout_row_push(nkCtx, 0.38f);
		nuklear.nk_label(nkCtx, getRoomModeLabel(room), nk_text_alignment.NK_TEXT_LEFT);
		nuklear.nk_layout_row_push(nkCtx, 0.18f);
		nuklear.nk_label(nkCtx, room.playing ? "Playing" : "Waiting", nk_text_alignment.NK_TEXT_LEFT);
		nuklear.nk_layout_row_push(nkCtx, 0.14f);
		nuklear.nk_label(nkCtx, room.playerSeatedCount + "/" + room.maxPlayers, nk_text_alignment.NK_TEXT_LEFT);
		nuklear.nk_layout_row_push(nkCtx, 0.12f);
		nuklear.nk_label(nkCtx, "Spec " + room.spectatorCount, nk_text_alignment.NK_TEXT_LEFT);
		nuklear.nk_layout_row_push(nkCtx, 0.18f);
		if(nuklear.nk_button_label(nkCtx, "Detail", nk_button_behavior.NK_BUTTON_DEFAULT)) {
			viewRoomDetail(room.roomID);
		}
		nuklear.nk_layout_row_end(nkCtx);

		nuklear.nk_layout_row_dynamic(nkCtx, 28, 2);
		if(nuklear.nk_button_label(nkCtx, "Join", nk_button_behavior.NK_BUTTON_DEFAULT)) {
			joinRoom(room.roomID, false);
		}
		if(nuklear.nk_button_label(nkCtx, "Watch", nk_button_behavior.NK_BUTTON_DEFAULT)) {
			joinRoom(room.roomID, true);
		}
	}

	private void drawServerAdd() {
		nk_rect bounds = new nk_rect();
		bounds.setX(120);
		bounds.setY(140);
		bounds.setW(400);
		bounds.setH(180);

		long flags = nk_panel_flags.NK_WINDOW_BORDER | nk_panel_flags.NK_WINDOW_TITLE | nk_panel_flags.NK_WINDOW_NO_SCROLLBAR;
		if(nuklear.nk_begin(nkCtx, serverAddPanel, "Add Server", bounds, flags)) {
			nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
			nuklear.nk_label(nkCtx, "Server address (host:port)", nk_text_alignment.NK_TEXT_LEFT);
			nuklear.nk_layout_row_dynamic(nkCtx, 28, 1);
			nuklear.nk_edit_string2(nkCtx, nk_edit_types.NK_EDIT_FIELD, serverAddBuf, serverAddLen, 127);
			nuklear.nk_layout_row_dynamic(nkCtx, 30, 2);
			if(nuklear.nk_button_label(nkCtx, "Add", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				addServerFromField();
			}
			if(nuklear.nk_button_label(nkCtx, "Cancel", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				currentScreen = SCREEN_SERVER_SELECT;
			}
		}
		nuklear.nk_end(nkCtx);
	}

	private void drawCreateRatedWaiting() {
		nk_rect bounds = new nk_rect();
		bounds.setX(120);
		bounds.setY(160);
		bounds.setW(400);
		bounds.setH(150);

		long flags = nk_panel_flags.NK_WINDOW_BORDER | nk_panel_flags.NK_WINDOW_TITLE | nk_panel_flags.NK_WINDOW_NO_SCROLLBAR;
		if(nuklear.nk_begin(nkCtx, createRatedWaitingPanel, "Create Room", bounds, flags)) {
			nuklear.nk_layout_row_dynamic(nkCtx, 28, 1);
			nuklear.nk_label(nkCtx, "Waiting for rated presets from the server...", nk_text_alignment.NK_TEXT_CENTERED);
			drawStatusBlock();
			nuklear.nk_layout_row_dynamic(nkCtx, 30, 1);
			if(nuklear.nk_button_label(nkCtx, "Cancel", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				currentViewDetailRoomID = -1;
				currentScreen = SCREEN_LOBBY;
			}
		}
		nuklear.nk_end(nkCtx);
	}

	private void drawCreateRated() {
		nk_rect bounds = new nk_rect();
		bounds.setX(70);
		bounds.setY(40);
		bounds.setW(500);
		bounds.setH(360);

		long flags = nk_panel_flags.NK_WINDOW_BORDER | nk_panel_flags.NK_WINDOW_TITLE;
		if(nuklear.nk_begin(nkCtx, createRatedPanel, "Create Rated Room", bounds, flags)) {
			drawLabelValueEdit("Room Name", createRatedNameBuf, createRatedNameLen, 63);
			drawCombo("Preset", buildPresetNames(), createRatedPresetIndex);
			nuklear.nk_layout_row_dynamic(nkCtx, 28, 1);
			nuklear.nk_property_int(nkCtx, "Max Players", 2, createRatedMaxPlayers, 6, 1, 1);

			NetRoomInfo preset = getSelectedPreset();
			if(preset != null) {
				nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
				nuklear.nk_label(nkCtx, "Mode: " + getRoomModeLabel(preset), nk_text_alignment.NK_TEXT_LEFT);
				nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
				nuklear.nk_label(nkCtx, "Rule: " + getRoomRuleLabel(preset), nk_text_alignment.NK_TEXT_LEFT);
				nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
				nuklear.nk_label(nkCtx, "Garbage: " + preset.garbagePercent + "%  Hurryup: " + preset.hurryupSeconds, nk_text_alignment.NK_TEXT_LEFT);
			}

			nuklear.nk_layout_row_dynamic(nkCtx, 30, 3);
			if(nuklear.nk_button_label(nkCtx, "Create", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				createRatedRoom();
			}
			if(nuklear.nk_button_label(nkCtx, "Custom", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				openCustomRoomFromPreset();
			}
			if(nuklear.nk_button_label(nkCtx, "Cancel", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				currentViewDetailRoomID = -1;
				currentScreen = SCREEN_LOBBY;
			}

			drawStatusBlock();
		}
		nuklear.nk_end(nkCtx);
	}

	private void drawCreateRoom() {
		boolean detailMode = currentViewDetailRoomID != -1;
		nk_rect bounds = new nk_rect();
		bounds.setX(15);
		bounds.setY(10);
		bounds.setW(610);
		bounds.setH(460);

		String title = detailMode ? "Room Detail" : "Create Room";
		long flags = nk_panel_flags.NK_WINDOW_BORDER | nk_panel_flags.NK_WINDOW_TITLE;
		if(nuklear.nk_begin(nkCtx, createRoomPanel, title, bounds, flags)) {
			nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
			nuklear.nk_label(nkCtx, getCreateRoomHeader(detailMode), nk_text_alignment.NK_TEXT_LEFT);

			nuklear.nk_layout_row_dynamic(nkCtx, 28, CREATE_ROOM_TABS.length);
			for(int i = 0; i < CREATE_ROOM_TABS.length; i++) {
				String label = (createRoomTab == i) ? "[" + CREATE_ROOM_TABS[i] + "]" : CREATE_ROOM_TABS[i];
				if(nuklear.nk_button_label(nkCtx, label, nk_button_behavior.NK_BUTTON_DEFAULT)) {
					createRoomTab = i;
				}
			}

			switch(createRoomTab) {
				case TAB_MAIN:
					drawCreateRoomMainTab();
					break;
				case TAB_SPEED:
					drawCreateRoomSpeedTab();
					break;
				case TAB_BONUS:
					drawCreateRoomBonusTab();
					break;
				case TAB_GARBAGE:
					drawCreateRoomGarbageTab();
					break;
				case TAB_MISC:
					drawCreateRoomMiscTab();
					break;
				case TAB_PRESET:
					drawCreateRoomPresetTab();
					break;
				default:
					break;
			}

			nuklear.nk_layout_row_dynamic(nkCtx, 30, detailMode ? 3 : 2);
			if(detailMode) {
				if(shouldShowJoinButtonsForViewedRoom()) {
					if(nuklear.nk_button_label(nkCtx, "Join", nk_button_behavior.NK_BUTTON_DEFAULT)) {
						joinRoom(currentViewDetailRoomID, false);
					}
					if(nuklear.nk_button_label(nkCtx, "Watch", nk_button_behavior.NK_BUTTON_DEFAULT)) {
						joinRoom(currentViewDetailRoomID, true);
					}
				} else {
					nuklear.nk_label(nkCtx, "(already joined)", nk_text_alignment.NK_TEXT_LEFT);
					nuklear.nk_label(nkCtx, "", nk_text_alignment.NK_TEXT_LEFT);
				}
				if(nuklear.nk_button_label(nkCtx, "Back", nk_button_behavior.NK_BUTTON_DEFAULT)) {
					currentViewDetailRoomID = -1;
					currentScreen = SCREEN_LOBBY;
				}
			} else {
				if(nuklear.nk_button_label(nkCtx, "Create", nk_button_behavior.NK_BUTTON_DEFAULT)) {
					createCustomRoom();
				}
				if(nuklear.nk_button_label(nkCtx, "Back", nk_button_behavior.NK_BUTTON_DEFAULT)) {
					currentViewDetailRoomID = -1;
					currentScreen = SCREEN_LOBBY;
				}
			}

			drawStatusBlock();
		}
		nuklear.nk_end(nkCtx);
	}

	private void drawCreateRoomMainTab() {
		drawLabelValueEdit("Room Name", createRoomState.nameBuf, createRoomState.nameLen, 63);
		drawCombo("Mode", multiModeList, createRoomState.modeIndex);
		nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
		nuklear.nk_label(nkCtx, "Mode description: " + getModeDesc(getSelectedModeName(multiModeList, createRoomState.modeIndex)), nk_text_alignment.NK_TEXT_LEFT);
		nuklear.nk_layout_row_dynamic(nkCtx, 28, 1);
		nuklear.nk_property_int(nkCtx, "Max Players", 2, createRoomState.maxPlayers, 6, 1, 1);
		nuklear.nk_layout_row_dynamic(nkCtx, 28, 1);
		nuklear.nk_property_int(nkCtx, "Hurryup Seconds", -1, createRoomState.hurryupSeconds, 999, 1, 1);
		nuklear.nk_layout_row_dynamic(nkCtx, 28, 1);
		nuklear.nk_property_int(nkCtx, "Hurryup Interval", 1, createRoomState.hurryupInterval, 99, 1, 1);
		nuklear.nk_layout_row_dynamic(nkCtx, 28, 1);
		nuklear.nk_property_int(nkCtx, "Map Set ID", 0, createRoomState.mapSetId, 99, 1, 1);
		nuklear.nk_layout_row_dynamic(nkCtx, 24, 2);
		nuklear.nk_checkbox_label(nkCtx, "Use Map", createRoomState.useMap);
		nuklear.nk_checkbox_label(nkCtx, "Rule Lock", createRoomState.ruleLock);
	}

	private void drawCreateRoomSpeedTab() {
		drawIntProperty("Gravity", -1, createRoomState.gravity, 99999, 1);
		drawIntProperty("Denominator", 0, createRoomState.denominator, 99999, 1);
		drawIntProperty("ARE", 0, createRoomState.are, 99, 1);
		drawIntProperty("ARE Line", 0, createRoomState.areLine, 99, 1);
		drawIntProperty("Line Delay", 0, createRoomState.lineDelay, 99, 1);
		drawIntProperty("Lock Delay", 0, createRoomState.lockDelay, 99, 1);
		drawIntProperty("DAS", 0, createRoomState.das, 99, 1);
	}

	private void drawCreateRoomBonusTab() {
		drawCombo("Spin Bonus", SPIN_BONUS_NAMES, createRoomState.tspinEnableType);
		drawCombo("Spin Check", SPIN_CHECK_NAMES, createRoomState.spinCheckType);
		nuklear.nk_layout_row_dynamic(nkCtx, 24, 3);
		nuklear.nk_checkbox_label(nkCtx, "EZ Spin", createRoomState.tspinEnableEZ);
		nuklear.nk_checkbox_label(nkCtx, "B2B", createRoomState.b2b);
		nuklear.nk_checkbox_label(nkCtx, "Combo", createRoomState.combo);
		nuklear.nk_layout_row_dynamic(nkCtx, 24, 3);
		nuklear.nk_checkbox_label(nkCtx, "Bravo", createRoomState.bravo);
		nuklear.nk_checkbox_label(nkCtx, "Rensa Block", createRoomState.rensaBlock);
		nuklear.nk_checkbox_label(nkCtx, "Counter", createRoomState.counter);
	}

	private void drawCreateRoomGarbageTab() {
		drawIntProperty("Garbage %", 0, createRoomState.garbagePercent, 100, 10);
		drawIntProperty("Target Timer", 0, createRoomState.targetTimer, 3600, 1);
		nuklear.nk_layout_row_dynamic(nkCtx, 24, 2);
		nuklear.nk_checkbox_label(nkCtx, "Garbage Per Attack", createRoomState.garbageChangePerAttack);
		nuklear.nk_checkbox_label(nkCtx, "Divide By Players", createRoomState.divideChangeRateByPlayers);
		nuklear.nk_layout_row_dynamic(nkCtx, 24, 2);
		nuklear.nk_checkbox_label(nkCtx, "B2B Chunk", createRoomState.b2bChunk);
		nuklear.nk_checkbox_label(nkCtx, "Reduce Line Send", createRoomState.reduceLineSend);
		nuklear.nk_layout_row_dynamic(nkCtx, 24, 2);
		nuklear.nk_checkbox_label(nkCtx, "Fractional Garbage", createRoomState.useFractionalGarbage);
		nuklear.nk_checkbox_label(nkCtx, "Target System", createRoomState.isTarget);
	}

	private void drawCreateRoomMiscTab() {
		drawIntProperty("Auto Start Seconds", 0, createRoomState.autoStartSeconds, 999, 1);
		nuklear.nk_layout_row_dynamic(nkCtx, 24, 2);
		nuklear.nk_checkbox_label(nkCtx, "Auto Start TNET2", createRoomState.autoStartTNET2);
		nuklear.nk_checkbox_label(nkCtx, "Disable Timer After Cancel", createRoomState.disableTimerAfterSomeoneCancelled);
	}

	private void drawCreateRoomPresetTab() {
		drawIntProperty("Preset ID", 0, createRoomState.presetId, 999, 1);
		nuklear.nk_layout_row_dynamic(nkCtx, 28, 2);
		if(nuklear.nk_button_label(nkCtx, "Save Preset", nk_button_behavior.NK_BUTTON_DEFAULT)) {
			saveCreateRoomPreset();
		}
		if(nuklear.nk_button_label(nkCtx, "Load Preset", nk_button_behavior.NK_BUTTON_DEFAULT)) {
			loadCreateRoomPreset();
		}
		nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
		nuklear.nk_label(nkCtx, "Preset Code", nk_text_alignment.NK_TEXT_LEFT);
		nuklear.nk_layout_row_dynamic(nkCtx, 28, 1);
		nuklear.nk_edit_string2(
			nkCtx,
			nk_edit_types.NK_EDIT_FIELD,
			createRoomState.presetCodeBuf,
			createRoomState.presetCodeLen,
			createRoomState.presetCodeBuf.length - 1
		);
		nuklear.nk_layout_row_dynamic(nkCtx, 28, 2);
		if(nuklear.nk_button_label(nkCtx, "Export Code", nk_button_behavior.NK_BUTTON_DEFAULT)) {
			exportCreateRoomPresetCode();
		}
		if(nuklear.nk_button_label(nkCtx, "Import Code", nk_button_behavior.NK_BUTTON_DEFAULT)) {
			importCreateRoomPresetCode();
		}
	}

	private void drawCreateRoom1P() {
		boolean detailMode = currentViewDetailRoomID != -1;
		nk_rect bounds = new nk_rect();
		bounds.setX(40);
		bounds.setY(25);
		bounds.setW(560);
		bounds.setH(430);

		String title = detailMode ? "Single Player Room Detail" : "Create Single Player Room";
		long flags = nk_panel_flags.NK_WINDOW_BORDER | nk_panel_flags.NK_WINDOW_TITLE;
		if(nuklear.nk_begin(nkCtx, createRoom1PPanel, title, bounds, flags)) {
			drawCombo("Mode", singleModeList, createRoom1PModeIndex);
			nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
			nuklear.nk_label(nkCtx, "Mode description: " + getModeDesc(getSelectedModeName(singleModeList, createRoom1PModeIndex)), nk_text_alignment.NK_TEXT_LEFT);
			drawCombo("Rule", buildSingleRuleNames(), createRoom1PRuleIndex);

			if(detailMode) {
				NetRoomInfo roomInfo = getViewedRoomInfo();
				if(roomInfo != null) {
					nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
					nuklear.nk_label(nkCtx, "Watching mode: " + roomInfo.strMode, nk_text_alignment.NK_TEXT_LEFT);
					nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
					nuklear.nk_label(nkCtx, "Rule: " + ((roomInfo.ruleName != null && roomInfo.ruleName.length() > 0) ? roomInfo.ruleName : YOUR_RULE_LABEL), nk_text_alignment.NK_TEXT_LEFT);
				}
			}

			nuklear.nk_layout_row_dynamic(nkCtx, 30, 2);
			if(nuklear.nk_button_label(nkCtx, detailMode ? "Watch" : "Create", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				createSinglePlayerRoom();
			}
			if(nuklear.nk_button_label(nkCtx, "Back", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				currentViewDetailRoomID = -1;
				currentScreen = SCREEN_LOBBY;
			}

			drawStatusBlock();
		}
		nuklear.nk_end(nkCtx);
	}

	private void drawMPRanking() {
		nk_rect bounds = new nk_rect();
		bounds.setX(40);
		bounds.setY(20);
		bounds.setW(560);
		bounds.setH(440);

		long flags = nk_panel_flags.NK_WINDOW_BORDER | nk_panel_flags.NK_WINDOW_TITLE;
		if(nuklear.nk_begin(nkCtx, rankingPanel, "Multiplayer Rankings", bounds, flags)) {
			nuklear.nk_layout_row_begin(nkCtx, nk_layout_format.NK_DYNAMIC, 24, 5);
			nuklear.nk_layout_row_push(nkCtx, 0.1f);
			nuklear.nk_label(nkCtx, "Rank", nk_text_alignment.NK_TEXT_LEFT);
			nuklear.nk_layout_row_push(nkCtx, 0.35f);
			nuklear.nk_label(nkCtx, "Name", nk_text_alignment.NK_TEXT_LEFT);
			nuklear.nk_layout_row_push(nkCtx, 0.2f);
			nuklear.nk_label(nkCtx, "Rating", nk_text_alignment.NK_TEXT_LEFT);
			nuklear.nk_layout_row_push(nkCtx, 0.15f);
			nuklear.nk_label(nkCtx, "Played", nk_text_alignment.NK_TEXT_LEFT);
			nuklear.nk_layout_row_push(nkCtx, 0.2f);
			nuklear.nk_label(nkCtx, "Wins", nk_text_alignment.NK_TEXT_LEFT);
			nuklear.nk_layout_row_end(nkCtx);

			if(rankingData.isEmpty()) {
				nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
				nuklear.nk_label(nkCtx, "Loading...", nk_text_alignment.NK_TEXT_LEFT);
			} else {
				for(int i = 0; i < rankingData.size(); i++) {
					String[] row = rankingData.get(i);
					boolean mine = (i == rankingMyRank);
					nuklear.nk_layout_row_begin(nkCtx, nk_layout_format.NK_DYNAMIC, 24, 5);
					nuklear.nk_layout_row_push(nkCtx, 0.1f);
					nuklear.nk_label(nkCtx, row[0], nk_text_alignment.NK_TEXT_LEFT);
					nuklear.nk_layout_row_push(nkCtx, 0.35f);
					nuklear.nk_label(nkCtx, (mine ? "*" : "") + row[1], nk_text_alignment.NK_TEXT_LEFT);
					nuklear.nk_layout_row_push(nkCtx, 0.2f);
					nuklear.nk_label(nkCtx, row[2], nk_text_alignment.NK_TEXT_LEFT);
					nuklear.nk_layout_row_push(nkCtx, 0.15f);
					nuklear.nk_label(nkCtx, row[3], nk_text_alignment.NK_TEXT_LEFT);
					nuklear.nk_layout_row_push(nkCtx, 0.2f);
					nuklear.nk_label(nkCtx, row[4], nk_text_alignment.NK_TEXT_LEFT);
					nuklear.nk_layout_row_end(nkCtx);
				}
			}

			nuklear.nk_layout_row_dynamic(nkCtx, 30, 1);
			if(nuklear.nk_button_label(nkCtx, "Back", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				currentScreen = SCREEN_LOBBY;
			}
		}
		nuklear.nk_end(nkCtx);
	}

	private void drawRuleChange() {
		nk_rect bounds = new nk_rect();
		bounds.setX(20);
		bounds.setY(15);
		bounds.setW(600);
		bounds.setH(450);

		long flags = nk_panel_flags.NK_WINDOW_BORDER | nk_panel_flags.NK_WINDOW_TITLE;
		if(nuklear.nk_begin(nkCtx, rulePanel, "Change Rules", bounds, flags)) {
			int tabCount = GameEngine.MAX_GAMESTYLE + 1;
			nuklear.nk_layout_row_dynamic(nkCtx, 28, tabCount);
			for(int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
				String label = (ruleChangeTab == i) ? "[" + GameEngine.GAMESTYLE_NAMES[i] + "]" : GameEngine.GAMESTYLE_NAMES[i];
				if(nuklear.nk_button_label(nkCtx, label, nk_button_behavior.NK_BUTTON_DEFAULT)) {
					ruleChangeTab = i;
				}
			}
			String tuningLabel = (ruleChangeTab == GameEngine.MAX_GAMESTYLE) ? "[Tuning]" : "Tuning";
			if(nuklear.nk_button_label(nkCtx, tuningLabel, nk_button_behavior.NK_BUTTON_DEFAULT)) {
				ruleChangeTab = GameEngine.MAX_GAMESTYLE;
			}

			if(ruleChangeTab == GameEngine.MAX_GAMESTYLE) {
				drawRuleTuningTab();
			} else {
				drawRuleSelectionTab(ruleChangeTab);
			}

			nuklear.nk_layout_row_dynamic(nkCtx, 30, 2);
			if(nuklear.nk_button_label(nkCtx, "Apply", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				applyRuleChange();
			}
			if(nuklear.nk_button_label(nkCtx, "Cancel", nk_button_behavior.NK_BUTTON_DEFAULT)) {
				currentScreen = SCREEN_LOBBY;
			}
			drawStatusBlock();
		}
		nuklear.nk_end(nkCtx);
	}

	private void drawRuleSelectionTab(int style) {
		List<RuleEntry> subEntries = getSubsetEntries(style);
		if(subEntries.isEmpty()) {
			nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
			nuklear.nk_label(nkCtx, "(no rules for this style)", nk_text_alignment.NK_TEXT_LEFT);
			return;
		}

		int[] selectedHolder = new int[] {clamp(ruleSelectedIndexByStyle[style], 0, subEntries.size() - 1)};
		String[] names = new String[subEntries.size()];
		for(int i = 0; i < subEntries.size(); i++) {
			names[i] = subEntries.get(i).rulename + " (" + subEntries.get(i).filename + ")";
		}
		drawCombo("Rule", names, selectedHolder);
		ruleSelectedIndexByStyle[style] = selectedHolder[0];

		RuleEntry entry = subEntries.get(ruleSelectedIndexByStyle[style]);
		nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
		nuklear.nk_label(nkCtx, "Selected file: " + entry.filepath, nk_text_alignment.NK_TEXT_LEFT);
	}

	private void drawRuleTuningTab() {
		drawCombo("A Button Rotation", TUNING_ROTATE_NAMES, tuningState.rotateButtonDefaultRight);
		drawCombo("Diagonal Move", TUNING_GENERIC_NAMES, tuningState.moveDiagonal);
		drawCombo("Outline Only", TUNING_GENERIC_NAMES, tuningState.blockShowOutlineOnly);
		drawCombo("Skin", skinOptionNames, tuningState.skin);
		drawCombo("Outline Type", TUNING_OUTLINE_NAMES, tuningState.blockOutlineType);
		drawIntProperty("Min DAS", -1, tuningState.minDas, 999, 1);
		drawIntProperty("Max DAS", -1, tuningState.maxDas, 999, 1);
		drawIntProperty("DAS Delay", -1, tuningState.dasDelay, 999, 1);
		nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
		nuklear.nk_checkbox_label(nkCtx, "Reverse Up/Down", tuningState.reverseUpDown);
	}

	private void drawLabelValueEdit(String label, byte[] buffer, int[] lengthHolder, int maxLength) {
		nuklear.nk_layout_row_dynamic(nkCtx, 28, 2);
		nuklear.nk_label(nkCtx, label + ":", nk_text_alignment.NK_TEXT_LEFT);
		nuklear.nk_edit_string2(nkCtx, nk_edit_types.NK_EDIT_FIELD, buffer, lengthHolder, maxLength);
	}

	private void drawCombo(String label, String[] values, int[] selected) {
		nuklear.nk_layout_row_dynamic(nkCtx, 28, 2);
		nuklear.nk_label(nkCtx, label + ":", nk_text_alignment.NK_TEXT_LEFT);
		if(values == null || values.length <= 0) {
			nuklear.nk_label(nkCtx, "(none)", nk_text_alignment.NK_TEXT_LEFT);
			selected[0] = 0;
			return;
		}
		selected[0] = clamp(selected[0], 0, values.length - 1);
		nuklear.nk_combobox(nkCtx, values, values.length, selected, 24);
	}

	private void drawIntProperty(String label, int min, int[] value, int max, int step) {
		nuklear.nk_layout_row_dynamic(nkCtx, 28, 1);
		nuklear.nk_property_int(nkCtx, label, min, value, max, step, 1);
	}

	private void drawStatusBlock() {
		if(statusMessage == null || statusMessage.length() <= 0) return;
		nuklear.nk_layout_row_dynamic(nkCtx, 24, 1);
		nuklear.nk_label(nkCtx, statusMessage, nk_text_alignment.NK_TEXT_LEFT);
	}

	private void connectToServer() {
		String playerName = getBufferString(playerNameBuf, playerNameLen);
		String playerTeam = getBufferString(playerTeamBuf, playerTeamLen);

		if(playerName.length() <= 0) {
			statusMessage = "Please enter a name.";
			return;
		}
		if(serverList.isEmpty() || selectedServer < 0 || selectedServer >= serverList.size()) {
			statusMessage = "Please select a server.";
			return;
		}

		shutdownActiveLobby();
		netLobby = new NetLobbySDL();
		netLobby.playerName = playerName;
		netLobby.playerTeam = playerTeam;
		netLobby.addListener(this);
		netLobby.init();
		netLobby.connectToServer(serverList.get(selectedServer));

		statusMessage = "Connecting to " + serverList.get(selectedServer) + "...";
		currentScreen = SCREEN_CONNECTING;
	}

	private void cancelConnection() {
		shutdownActiveLobby();
		currentScreen = SCREEN_SERVER_SELECT;
		statusMessage = "";
	}

	private void disconnectToServerSelect() {
		shutdownActiveLobby();
		currentScreen = SCREEN_SERVER_SELECT;
		currentViewDetailRoomID = -1;
		statusMessage = "";
	}

	private void deleteSelectedServer() {
		if(serverList.isEmpty()) return;
		if(selectedServer < 0 || selectedServer >= serverList.size()) return;
		serverList.remove(selectedServer);
		if(selectedServer >= serverList.size()) selectedServer = serverList.size() - 1;
		if(selectedServer < 0) selectedServer = 0;
		saveServerList();
		syncLobbyServerList();
	}

	private void addServerFromField() {
		String address = getBufferString(serverAddBuf, serverAddLen);
		if(address.length() <= 0) {
			statusMessage = "Please enter a server address.";
			return;
		}
		serverList.add(address);
		selectedServer = serverList.size() - 1;
		saveServerList();
		syncLobbyServerList();
		currentScreen = SCREEN_SERVER_SELECT;
		statusMessage = "Added server " + address;
	}

	private void enterCreateRoomFlow() {
		currentViewDetailRoomID = -1;
		presets.clear();
		loadRatedDefaults();
		currentScreen = SCREEN_CREATERATED_WAITING;
		statusMessage = "Requesting room presets...";
		if(netLobby != null && netLobby.getNetPlayerClient() != null) {
			netLobby.getNetPlayerClient().send("getpresets\n");
		}
	}

	private void createRatedRoom() {
		NetRoomInfo preset = getSelectedPreset();
		if(preset == null || netLobby == null || netLobby.getNetPlayerClient() == null) return;

		NetRoomInfo roomInfo = new NetRoomInfo(preset);
		roomInfo.strName = getDefaultedRoomName(getBufferString(createRatedNameBuf, createRatedNameLen));
		roomInfo.maxPlayers = createRatedMaxPlayers[0];
		backupRoomInfo = new NetRoomInfo(roomInfo);

		StringBuilder msg = new StringBuilder();
		msg.append("ratedroomcreate\t");
		msg.append(NetUtil.urlEncode(roomInfo.strName));
		msg.append('\t');
		msg.append(createRatedMaxPlayers[0]);
		msg.append('\t');
		msg.append(createRatedPresetIndex[0]);
		msg.append('\t');
		msg.append(NetUtil.urlEncode("NET-VS-BATTLE"));
		msg.append('\n');
		netLobby.getNetPlayerClient().send(msg.toString());

		statusMessage = "Creating rated room...";
		currentScreen = SCREEN_LOBBY;
	}

	private void openCustomRoomFromPreset() {
		NetRoomInfo preset = getSelectedPreset();
		if(preset == null) {
			setCreateRoomUIType(false, null);
		} else {
			setCreateRoomUIType(false, preset);
			copyStringToBuffer(getDefaultedRoomName(getBufferString(createRatedNameBuf, createRatedNameLen)), createRoomState.nameBuf, createRoomState.nameLen);
			createRoomState.maxPlayers[0] = createRatedMaxPlayers[0];
		}
		currentScreen = SCREEN_CREATEROOM;
	}

	private void createCustomRoom() {
		if(netLobby == null || netLobby.getNetPlayerClient() == null) return;

		NetRoomInfo roomInfo = exportRoomInfoFromCreateRoomScreen();
		backupRoomInfo = new NetRoomInfo(roomInfo);

		StringBuilder msg = new StringBuilder();
		msg.append("roomcreate\t");
		msg.append(NetUtil.urlEncode(roomInfo.strName));
		msg.append('\t');
		msg.append(NetUtil.urlEncode(roomInfo.exportString()));
		msg.append('\t');
		msg.append(NetUtil.urlEncode(roomInfo.strMode));
		msg.append('\t');
		appendMapData(msg, roomInfo);
		msg.append('\n');

		netLobby.getNetPlayerClient().send(msg.toString());
		statusMessage = "Creating room...";
		currentScreen = SCREEN_LOBBY;
	}

	private void createSinglePlayerRoom() {
		if(netLobby == null || netLobby.getNetPlayerClient() == null) return;

		if(currentViewDetailRoomID != -1) {
			joinRoom(currentViewDetailRoomID, true);
			return;
		}

		String modeName = getSelectedModeName(singleModeList, createRoom1PModeIndex);
		if(modeName.length() <= 0) {
			statusMessage = "Please select a mode.";
			return;
		}

		String ruleName = "";
		if(createRoom1PRuleIndex[0] > 0) {
			ruleName = getSelectedSingleRuleName();
		}

		backupRoomInfo1P = new NetRoomInfo();
		backupRoomInfo1P.maxPlayers = 1;
		backupRoomInfo1P.singleplayer = true;
		backupRoomInfo1P.strMode = modeName;
		backupRoomInfo1P.ruleName = ruleName;

		StringBuilder msg = new StringBuilder();
		msg.append("singleroomcreate\t\t");
		msg.append(NetUtil.urlEncode(modeName));
		msg.append('\t');
		msg.append(NetUtil.urlEncode(ruleName));
		msg.append('\n');
		netLobby.getNetPlayerClient().send(msg.toString());

		statusMessage = "Creating single-player room...";
		currentScreen = SCREEN_LOBBY;
	}

	private void joinRoom(int roomID, boolean watch) {
		if(netLobby == null || netLobby.getNetPlayerClient() == null) return;
		statusMessage = watch ? "Joining as spectator..." : "Joining room...";
		netLobby.getNetPlayerClient().send("roomjoin\t" + roomID + "\t" + watch + "\n");
		currentScreen = SCREEN_LOBBY;
	}

	private void viewRoomDetail(int roomID) {
		NetPlayerClient client = (netLobby != null) ? netLobby.getNetPlayerClient() : null;
		if(client == null) return;

		NetRoomInfo roomInfo = client.getRoomInfo(roomID);
		if(roomInfo == null) return;

		currentViewDetailRoomID = roomID;
		if(roomInfo.singleplayer) {
			setCreateRoom1PUIType(true, roomInfo);
			currentScreen = SCREEN_CREATEROOM1P;
		} else {
			setCreateRoomUIType(true, roomInfo);
			currentScreen = SCREEN_CREATEROOM;
		}
	}

	private void applyLobbyTeamChange() {
		String teamName = getBufferString(playerTeamBuf, playerTeamLen);
		if(netLobby != null) {
			netLobby.playerTeam = teamName;
			if(netLobby.getNetPlayerClient() != null && netLobby.getNetPlayerClient().isConnected()) {
				netLobby.getNetPlayerClient().send("changeteam\t" + NetUtil.urlEncode(teamName) + "\n");
				statusMessage = "Updated team.";
			}
		}
	}

	private void sendLobbyChat() {
		if(netLobby == null || netLobby.getNetPlayerClient() == null) return;
		String msg = getBufferString(chatInputBuf, chatInputLen);
		if(msg.length() <= 0) return;

		if(msg.startsWith("/team")) {
			String team = msg.replaceFirst("/team", "").trim();
			copyStringToBuffer(team, playerTeamBuf, playerTeamLen);
			applyLobbyTeamChange();
		} else {
			netLobby.getNetPlayerClient().send("lobbychat\t" + NetUtil.urlEncode(msg) + "\n");
		}

		clearBuffer(chatInputBuf, chatInputLen);
	}

	private void setCreateRoomUIType(boolean detailMode, NetRoomInfo roomInfo) {
		createRoomTab = TAB_MAIN;
		if(detailMode && roomInfo != null) {
			importRoomInfoToCreateRoomScreen(roomInfo);
			return;
		}

		NetRoomInfo defaults = (backupRoomInfo != null) ? new NetRoomInfo(backupRoomInfo) : createDefaultRoomInfo();
		importRoomInfoToCreateRoomScreen(defaults);
	}

	private void setCreateRoom1PUIType(boolean detailMode, NetRoomInfo roomInfo) {
		if(detailMode && roomInfo != null) {
			selectModeByName(singleModeList, createRoom1PModeIndex, roomInfo.strMode);
			selectSingleRuleByName(roomInfo.ruleName);
			return;
		}

		String defaultMode = propConfig.getProperty("createroom1p.listboxCreateRoom1PModeList.value", "");
		selectModeByName(singleModeList, createRoom1PModeIndex, defaultMode);
		selectSingleRuleByName(propConfig.getProperty("createroom1p.listboxCreateRoom1PRuleList.value", ""));
		if(backupRoomInfo1P != null) {
			selectModeByName(singleModeList, createRoom1PModeIndex, backupRoomInfo1P.strMode);
			selectSingleRuleByName(backupRoomInfo1P.ruleName);
		}
	}

	private NetRoomInfo exportRoomInfoFromCreateRoomScreen() {
		NetRoomInfo roomInfo = new NetRoomInfo();
		roomInfo.strName = getDefaultedRoomName(getBufferString(createRoomState.nameBuf, createRoomState.nameLen));
		roomInfo.strMode = getSelectedModeName(multiModeList, createRoomState.modeIndex);
		roomInfo.maxPlayers = createRoomState.maxPlayers[0];
		roomInfo.autoStartSeconds = createRoomState.autoStartSeconds[0];
		roomInfo.gravity = createRoomState.gravity[0];
		roomInfo.denominator = createRoomState.denominator[0];
		roomInfo.are = createRoomState.are[0];
		roomInfo.areLine = createRoomState.areLine[0];
		roomInfo.lineDelay = createRoomState.lineDelay[0];
		roomInfo.lockDelay = createRoomState.lockDelay[0];
		roomInfo.das = createRoomState.das[0];
		roomInfo.hurryupSeconds = createRoomState.hurryupSeconds[0];
		roomInfo.hurryupInterval = createRoomState.hurryupInterval[0];
		roomInfo.ruleLock = createRoomState.ruleLock[0] != 0;
		roomInfo.tspinEnableType = createRoomState.tspinEnableType[0];
		roomInfo.spinCheckType = createRoomState.spinCheckType[0];
		roomInfo.tspinEnableEZ = createRoomState.tspinEnableEZ[0] != 0;
		roomInfo.b2b = createRoomState.b2b[0] != 0;
		roomInfo.combo = createRoomState.combo[0] != 0;
		roomInfo.rensaBlock = createRoomState.rensaBlock[0] != 0;
		roomInfo.counter = createRoomState.counter[0] != 0;
		roomInfo.bravo = createRoomState.bravo[0] != 0;
		roomInfo.reduceLineSend = createRoomState.reduceLineSend[0] != 0;
		roomInfo.autoStartTNET2 = createRoomState.autoStartTNET2[0] != 0;
		roomInfo.disableTimerAfterSomeoneCancelled = createRoomState.disableTimerAfterSomeoneCancelled[0] != 0;
		roomInfo.useMap = createRoomState.useMap[0] != 0;
		roomInfo.useFractionalGarbage = createRoomState.useFractionalGarbage[0] != 0;
		roomInfo.garbageChangePerAttack = createRoomState.garbageChangePerAttack[0] != 0;
		roomInfo.divideChangeRateByPlayers = createRoomState.divideChangeRateByPlayers[0] != 0;
		roomInfo.garbagePercent = createRoomState.garbagePercent[0];
		roomInfo.b2bChunk = createRoomState.b2bChunk[0] != 0;
		roomInfo.isTarget = createRoomState.isTarget[0] != 0;
		roomInfo.targetTimer = createRoomState.targetTimer[0];
		return roomInfo;
	}

	private void importRoomInfoToCreateRoomScreen(NetRoomInfo roomInfo) {
		copyStringToBuffer(roomInfo.strName, createRoomState.nameBuf, createRoomState.nameLen);
		selectModeByName(multiModeList, createRoomState.modeIndex, roomInfo.strMode);
		createRoomState.maxPlayers[0] = roomInfo.maxPlayers;
		createRoomState.autoStartSeconds[0] = roomInfo.autoStartSeconds;
		createRoomState.gravity[0] = roomInfo.gravity;
		createRoomState.denominator[0] = roomInfo.denominator;
		createRoomState.are[0] = roomInfo.are;
		createRoomState.areLine[0] = roomInfo.areLine;
		createRoomState.lineDelay[0] = roomInfo.lineDelay;
		createRoomState.lockDelay[0] = roomInfo.lockDelay;
		createRoomState.das[0] = roomInfo.das;
		createRoomState.hurryupSeconds[0] = roomInfo.hurryupSeconds;
		createRoomState.hurryupInterval[0] = roomInfo.hurryupInterval;
		createRoomState.garbagePercent[0] = roomInfo.garbagePercent;
		createRoomState.targetTimer[0] = roomInfo.targetTimer;
		createRoomState.useMap[0] = roomInfo.useMap ? 1 : 0;
		createRoomState.ruleLock[0] = roomInfo.ruleLock ? 1 : 0;
		createRoomState.tspinEnableType[0] = clamp(roomInfo.tspinEnableType, 0, SPIN_BONUS_NAMES.length - 1);
		createRoomState.spinCheckType[0] = clamp(roomInfo.spinCheckType, 0, SPIN_CHECK_NAMES.length - 1);
		createRoomState.tspinEnableEZ[0] = roomInfo.tspinEnableEZ ? 1 : 0;
		createRoomState.b2b[0] = roomInfo.b2b ? 1 : 0;
		createRoomState.combo[0] = roomInfo.combo ? 1 : 0;
		createRoomState.rensaBlock[0] = roomInfo.rensaBlock ? 1 : 0;
		createRoomState.counter[0] = roomInfo.counter ? 1 : 0;
		createRoomState.bravo[0] = roomInfo.bravo ? 1 : 0;
		createRoomState.reduceLineSend[0] = roomInfo.reduceLineSend ? 1 : 0;
		createRoomState.garbageChangePerAttack[0] = roomInfo.garbageChangePerAttack ? 1 : 0;
		createRoomState.divideChangeRateByPlayers[0] = roomInfo.divideChangeRateByPlayers ? 1 : 0;
		createRoomState.b2bChunk[0] = roomInfo.b2bChunk ? 1 : 0;
		createRoomState.useFractionalGarbage[0] = roomInfo.useFractionalGarbage ? 1 : 0;
		createRoomState.isTarget[0] = roomInfo.isTarget ? 1 : 0;
		createRoomState.autoStartTNET2[0] = roomInfo.autoStartTNET2 ? 1 : 0;
		createRoomState.disableTimerAfterSomeoneCancelled[0] = roomInfo.disableTimerAfterSomeoneCancelled ? 1 : 0;
	}

	private void loadCreateRoomDefaults() {
		NetRoomInfo defaults = createDefaultRoomInfo();
		importRoomInfoToCreateRoomScreen(defaults);
		createRoomState.mapSetId[0] = propConfig.getProperty("createroom.defaultMapSetID", 0);
		createRoomState.presetId[0] = propConfig.getProperty("createroom.defaultPresetID", 0);
		clearBuffer(createRoomState.presetCodeBuf, createRoomState.presetCodeLen);
	}

	private void loadCreateRoom1PDefaults() {
		selectModeByName(singleModeList, createRoom1PModeIndex, propConfig.getProperty("createroom1p.listboxCreateRoom1PModeList.value", ""));
		selectSingleRuleByName(propConfig.getProperty("createroom1p.listboxCreateRoom1PRuleList.value", ""));
	}

	private void loadRatedDefaults() {
		copyStringToBuffer("", createRatedNameBuf, createRatedNameLen);
		createRatedPresetIndex[0] = clamp(propConfig.getProperty("createrated.defaultPreset", 0), 0, 999);
		createRatedMaxPlayers[0] = clamp(propConfig.getProperty("createroom.defaultMaxPlayers", 6), 2, 6);
	}

	private void loadRuleChangeDefaults() {
		Arrays.fill(ruleSelectedIndexByStyle, 0);
		for(int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
			String currentFile;
			if(i == 0) {
				currentFile = propGlobal.getProperty("0.rulefile", "");
			} else {
				currentFile = propGlobal.getProperty("0.rulefile." + i, "");
			}
			List<RuleEntry> subEntries = getSubsetEntries(i);
			for(int j = 0; j < subEntries.size(); j++) {
				if(subEntries.get(j).filename.equals(currentFile)) {
					ruleSelectedIndexByStyle[i] = j;
					break;
				}
			}
		}

		tuningState.rotateButtonDefaultRight[0] = clamp(propGlobal.getProperty("0.tuning.owRotateButtonDefaultRight", -1) + 1, 0, TUNING_ROTATE_NAMES.length - 1);
		tuningState.moveDiagonal[0] = clamp(propGlobal.getProperty("0.tuning.owMoveDiagonal", -1) + 1, 0, TUNING_GENERIC_NAMES.length - 1);
		tuningState.blockShowOutlineOnly[0] = clamp(propGlobal.getProperty("0.tuning.owBlockShowOutlineOnly", -1) + 1, 0, TUNING_GENERIC_NAMES.length - 1);
		tuningState.skin[0] = clamp(propGlobal.getProperty("0.tuning.owSkin", -1) + 1, 0, skinOptionNames.length - 1);
		tuningState.blockOutlineType[0] = clamp(propGlobal.getProperty("0.tuning.owBlockOutlineType", -1) + 1, 0, TUNING_OUTLINE_NAMES.length - 1);
		tuningState.minDas[0] = propGlobal.getProperty("0.tuning.owMinDAS", -1);
		tuningState.maxDas[0] = propGlobal.getProperty("0.tuning.owMaxDAS", -1);
		tuningState.dasDelay[0] = propGlobal.getProperty("0.tuning.owDasDelay", -1);
		tuningState.reverseUpDown[0] = propGlobal.getProperty("0.tuning.owReverseUpDown", false) ? 1 : 0;
	}

	private void enterRuleChangeScreen() {
		loadRuleChangeDefaults();
		currentScreen = SCREEN_RULECHANGE;
	}

	private void applyRuleChange() {
		String prevTetrominoRule = propGlobal.getProperty("0.rule", "");
		for(int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
			List<RuleEntry> subEntries = getSubsetEntries(i);
			if(subEntries.isEmpty()) continue;

			int selected = clamp(ruleSelectedIndexByStyle[i], 0, subEntries.size() - 1);
			RuleEntry entry = subEntries.get(selected);
			if(i == 0) {
				propGlobal.setProperty("0.rule", entry.filepath);
				propGlobal.setProperty("0.rulefile", entry.filename);
				propGlobal.setProperty("0.rulename", entry.rulename);
			} else {
				propGlobal.setProperty("0.rule." + i, entry.filepath);
				propGlobal.setProperty("0.rulefile." + i, entry.filename);
				propGlobal.setProperty("0.rulename." + i, entry.rulename);
			}
		}

		propGlobal.setProperty("0.tuning.owRotateButtonDefaultRight", tuningState.rotateButtonDefaultRight[0] - 1);
		propGlobal.setProperty("0.tuning.owMoveDiagonal", tuningState.moveDiagonal[0] - 1);
		propGlobal.setProperty("0.tuning.owBlockShowOutlineOnly", tuningState.blockShowOutlineOnly[0] - 1);
		propGlobal.setProperty("0.tuning.owSkin", tuningState.skin[0] - 1);
		propGlobal.setProperty("0.tuning.owBlockOutlineType", tuningState.blockOutlineType[0] - 1);
		propGlobal.setProperty("0.tuning.owMinDAS", tuningState.minDas[0]);
		propGlobal.setProperty("0.tuning.owMaxDAS", tuningState.maxDas[0]);
		propGlobal.setProperty("0.tuning.owDasDelay", tuningState.dasDelay[0]);
		propGlobal.setProperty("0.tuning.owReverseUpDown", tuningState.reverseUpDown[0] != 0);

		NullpoMinoSDL.saveConfig();

		String newTetrominoRule = propGlobal.getProperty("0.rule", "");
		if(!prevTetrominoRule.equals(newTetrominoRule)) {
			RuleOptions ruleopt = GeneralUtil.loadRule(newTetrominoRule);
			if(ruleopt != null && netLobby != null) {
				netLobby.setRuleOptPlayer(ruleopt);
				if(netLobby.getNetPlayerClient() != null && netLobby.getNetPlayerClient().isConnected()) {
					netLobby.sendMyRuleDataToServer();
				}
			}
		}

		statusMessage = "Updated rules and tuning.";
		currentScreen = SCREEN_LOBBY;
	}

	private void saveCreateRoomPreset() {
		try {
			NetRoomInfo roomInfo = exportRoomInfoFromCreateRoomScreen();
			propConfig.setProperty("0.preset." + createRoomState.presetId[0], NetUtil.compressString(roomInfo.exportString()));
			saveNetLobbyConfig();
			statusMessage = "Saved preset " + createRoomState.presetId[0] + ".";
		} catch (Exception e) {
			log.error("Failed to save room preset", e);
			statusMessage = "Failed to save preset.";
		}
	}

	private void loadCreateRoomPreset() {
		try {
			String presetCode = propConfig.getProperty("0.preset." + createRoomState.presetId[0]);
			if(presetCode == null) {
				statusMessage = "Preset not found.";
				return;
			}
			NetRoomInfo roomInfo = new NetRoomInfo(NetUtil.decompressString(presetCode));
			importRoomInfoToCreateRoomScreen(roomInfo);
			statusMessage = "Loaded preset " + createRoomState.presetId[0] + ".";
		} catch (Exception e) {
			log.error("Failed to load room preset", e);
			statusMessage = "Failed to load preset.";
		}
	}

	private void exportCreateRoomPresetCode() {
		try {
			NetRoomInfo roomInfo = exportRoomInfoFromCreateRoomScreen();
			copyStringToBuffer(NetUtil.compressString(roomInfo.exportString()), createRoomState.presetCodeBuf, createRoomState.presetCodeLen);
		} catch (Exception e) {
			log.error("Failed to export preset code", e);
			statusMessage = "Failed to export preset code.";
		}
	}

	private void importCreateRoomPresetCode() {
		try {
			String presetCode = getBufferString(createRoomState.presetCodeBuf, createRoomState.presetCodeLen).replaceAll("[^a-zA-Z0-9+/=]", "");
			if(presetCode.length() <= 0) {
				statusMessage = "Preset code is empty.";
				return;
			}
			NetRoomInfo roomInfo = new NetRoomInfo(NetUtil.decompressString(presetCode));
			importRoomInfoToCreateRoomScreen(roomInfo);
			statusMessage = "Imported preset code.";
		} catch (Exception e) {
			log.error("Failed to import preset code", e);
			statusMessage = "Failed to import preset code.";
		}
	}

	private void appendMapData(StringBuilder msg, NetRoomInfo roomInfo) {
		if(!roomInfo.useMap) return;
		CustomProperties propMap = new CustomProperties();
		try {
			FileInputStream in = new FileInputStream("config/map/vsbattle/" + createRoomState.mapSetId[0] + ".map");
			propMap.load(in);
			in.close();
		} catch(IOException e) {
			log.warn("Map set " + createRoomState.mapSetId[0] + " not found", e);
		}

		int maxMap = propMap.getProperty("map.maxMapNumber", 0);
		StringBuilder mapBuilder = new StringBuilder();
		if(netLobby != null) netLobby.getMapList().clear();
		for(int i = 0; i < maxMap; i++) {
			String mapData = propMap.getProperty("map." + i, "");
			if(netLobby != null) netLobby.getMapList().add(mapData);
			mapBuilder.append(mapData);
			if(i < maxMap - 1) mapBuilder.append('\t');
		}
		msg.append(NetUtil.compressString(mapBuilder.toString()));
	}

	private void saveNetLobbyConfig() {
		propConfig.setProperty("serverselect.txtfldPlayerName.text", getBufferString(playerNameBuf, playerNameLen));
		propConfig.setProperty("serverselect.txtfldPlayerTeam.text", getBufferString(playerTeamBuf, playerTeamLen));
		propConfig.setProperty("createrated.defaultPreset", createRatedPresetIndex[0]);
		propConfig.setProperty("createroom.defaultPresetID", createRoomState.presetId[0]);

		NetRoomInfo roomInfoToPersist = backupRoomInfo;
		if(currentScreen == SCREEN_CREATEROOM && currentViewDetailRoomID == -1) {
			roomInfoToPersist = exportRoomInfoFromCreateRoomScreen();
		}
		if(roomInfoToPersist != null) {
			propConfig.setProperty("createroom.defaultMaxPlayers", roomInfoToPersist.maxPlayers);
			propConfig.setProperty("createroom.defaultAutoStartSeconds", roomInfoToPersist.autoStartSeconds);
			propConfig.setProperty("createroom.defaultGravity", roomInfoToPersist.gravity);
			propConfig.setProperty("createroom.defaultDenominator", roomInfoToPersist.denominator);
			propConfig.setProperty("createroom.defaultARE", roomInfoToPersist.are);
			propConfig.setProperty("createroom.defaultARELine", roomInfoToPersist.areLine);
			propConfig.setProperty("createroom.defaultLineDelay", roomInfoToPersist.lineDelay);
			propConfig.setProperty("createroom.defaultLockDelay", roomInfoToPersist.lockDelay);
			propConfig.setProperty("createroom.defaultDAS", roomInfoToPersist.das);
			propConfig.setProperty("createroom.defaultGarbagePercent", roomInfoToPersist.garbagePercent);
			propConfig.setProperty("createroom.defaultTargetTimer", roomInfoToPersist.targetTimer);
			propConfig.setProperty("createroom.defaultHurryupSeconds", roomInfoToPersist.hurryupSeconds);
			propConfig.setProperty("createroom.defaultHurryupInterval", roomInfoToPersist.hurryupInterval);
			propConfig.setProperty("createroom.defaultRuleLock", roomInfoToPersist.ruleLock);
			propConfig.setProperty("createroom.defaultTSpinEnableType", roomInfoToPersist.tspinEnableType);
			propConfig.setProperty("createroom.defaultSpinCheckType", roomInfoToPersist.spinCheckType);
			propConfig.setProperty("createroom.defaultTSpinEnableEZ", roomInfoToPersist.tspinEnableEZ);
			propConfig.setProperty("createroom.defaultB2B", roomInfoToPersist.b2b);
			propConfig.setProperty("createroom.defaultCombo", roomInfoToPersist.combo);
			propConfig.setProperty("createroom.defaultRensaBlock", roomInfoToPersist.rensaBlock);
			propConfig.setProperty("createroom.defaultCounter", roomInfoToPersist.counter);
			propConfig.setProperty("createroom.defaultBravo", roomInfoToPersist.bravo);
			propConfig.setProperty("createroom.defaultReduceLineSend", roomInfoToPersist.reduceLineSend);
			propConfig.setProperty("createroom.defaultGarbageChangePerAttack", roomInfoToPersist.garbageChangePerAttack);
			propConfig.setProperty("createroom.defaultDivideChangeRateByPlayers", roomInfoToPersist.divideChangeRateByPlayers);
			propConfig.setProperty("createroom.defaultB2BChunk", roomInfoToPersist.b2bChunk);
			propConfig.setProperty("createroom.defaultUseFractionalGarbage", roomInfoToPersist.useFractionalGarbage);
			propConfig.setProperty("createroom.defaultIsTarget", roomInfoToPersist.isTarget);
			propConfig.setProperty("createroom.defaultAutoStartTNET2", roomInfoToPersist.autoStartTNET2);
			propConfig.setProperty("createroom.defaultDisableTimerAfterSomeoneCancelled", roomInfoToPersist.disableTimerAfterSomeoneCancelled);
			propConfig.setProperty("createroom.defaultUseMap", roomInfoToPersist.useMap);
			propConfig.setProperty("createroom.defaultMapSetID", createRoomState.mapSetId[0]);
		}

		propConfig.setProperty("createroom1p.listboxCreateRoom1PModeList.value", getSelectedModeName(singleModeList, createRoom1PModeIndex));
		propConfig.setProperty("createroom1p.listboxCreateRoom1PRuleList.value", getSelectedSingleRuleName());

		try {
			FileOutputStream out = new FileOutputStream("config/setting/netlobby.cfg");
			propConfig.store(out, "NullpoMino NetLobby Config");
			out.close();
		} catch(IOException e) {
			log.warn("Failed to save netlobby config file", e);
		}

		if(netLobby != null) {
			netLobby.playerName = getBufferString(playerNameBuf, playerNameLen);
			netLobby.playerTeam = getBufferString(playerTeamBuf, playerTeamLen);
		}
	}

	private void shutdownActiveLobby() {
		if(netLobby == null) return;
		netLobby.removeListener(this);
		netLobby.shutdown();
		netLobby = null;
	}

	private void loadServerListFromConfig() {
		serverList.clear();
		String serverListFile = GameManager.isDevBuild()
			? "config/setting/netlobby_serverlist_dev.cfg"
			: "config/setting/netlobby_serverlist.cfg";
		loadServerList(serverListFile);
		if(serverList.isEmpty()) {
			String defaultFile = GameManager.isDevBuild()
				? "config/list/netlobby_serverlist_default_dev.lst"
				: "config/list/netlobby_serverlist_default.lst";
			loadServerList(defaultFile);
		}
		selectedServer = serverList.isEmpty() ? 0 : clamp(selectedServer, 0, serverList.size() - 1);
	}

	private void loadServerList(String filename) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line;
			while((line = in.readLine()) != null) {
				line = line.trim();
				if(line.length() > 0) serverList.add(line);
			}
			in.close();
		} catch(IOException e) {}
	}

	private void saveServerList() {
		String filename = GameManager.isDevBuild()
			? "config/setting/netlobby_serverlist_dev.cfg"
			: "config/setting/netlobby_serverlist.cfg";
		try {
			PrintWriter out = new PrintWriter(filename);
			for(String s: serverList) {
				out.println(s);
			}
			out.flush();
			out.close();
		} catch(IOException e) {
			log.warn("Failed to save server list", e);
		}
	}

	private void syncLobbyServerList() {
		if(netLobby == null) return;
		netLobby.serverList.clear();
		netLobby.serverList.addAll(serverList);
	}

	private String[] loadModeList(String filename) {
		LinkedList<String> list = new LinkedList<String>();
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line;
			while((line = in.readLine()) != null) {
				line = line.trim();
				if(line.length() <= 0 || line.startsWith("#") || line.startsWith(":")) continue;
				int comma = line.indexOf(',');
				if(comma != -1) {
					list.add(line.substring(0, comma));
				}
			}
			in.close();
		} catch(IOException e) {
			log.warn("Failed to load mode list: " + filename, e);
		}
		return list.toArray(new String[list.size()]);
	}

	private String getModeDesc(String modeName) {
		if(modeName == null) return "";
		String key = modeName.replace(' ', '_').replace('(', 'l').replace(')', 'r');
		String result = propModeDesc.getProperty(key);
		if(result == null && propDefaultModeDesc != null) {
			result = propDefaultModeDesc.getProperty(key, modeName);
		}
		return (result != null) ? result : modeName;
	}

	private String[] getRuleFileList() {
		File dir = new File("config/rule");
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir1, String name) {
				return name.endsWith(".rul");
			}
		};
		String[] list = dir.list(filter);
		if(list == null) return new String[0];
		if(!System.getProperty("os.name").startsWith("Windows")) {
			Arrays.sort(list);
		}
		return list;
	}

	private void createRuleEntries(String[] fileList) {
		ruleEntries.clear();
		for(int i = 0; i < fileList.length; i++) {
			RuleEntry entry = new RuleEntry();
			File file = new File("config/rule/" + fileList[i]);
			entry.filename = fileList[i];
			entry.filepath = file.getPath();

			CustomProperties ruleProp = new CustomProperties();
			try {
				FileInputStream in = new FileInputStream(file);
				ruleProp.load(in);
				in.close();
				entry.rulename = ruleProp.getProperty("0.ruleopt.strRuleName", "");
				entry.style = ruleProp.getProperty("0.ruleopt.style", 0);
			} catch(IOException e) {
				entry.rulename = fileList[i];
				entry.style = -1;
			}
			ruleEntries.add(entry);
		}
	}

	private List<RuleEntry> getSubsetEntries(int style) {
		LinkedList<RuleEntry> subEntries = new LinkedList<RuleEntry>();
		for(RuleEntry entry: ruleEntries) {
			if(entry.style == style) {
				subEntries.add(entry);
			}
		}
		return subEntries;
	}

	private List<NetRoomInfo> getRoomSnapshot() {
		NetPlayerClient client = (netLobby != null) ? netLobby.getNetPlayerClient() : null;
		if(client == null) return Collections.emptyList();

		LinkedHashMap<Integer, NetRoomInfo> uniqueRooms = new LinkedHashMap<Integer, NetRoomInfo>();
		for(NetRoomInfo room: client.getRoomInfoList()) {
			if(room != null) uniqueRooms.put(room.roomID, room);
		}

		ArrayList<NetRoomInfo> rooms = new ArrayList<NetRoomInfo>(uniqueRooms.values());
		Collections.sort(rooms, new Comparator<NetRoomInfo>() {
			public int compare(NetRoomInfo o1, NetRoomInfo o2) {
				return o1.roomID - o2.roomID;
			}
		});
		return rooms;
	}

	private List<NetPlayerInfo> getPlayerSnapshot() {
		NetPlayerClient client = (netLobby != null) ? netLobby.getNetPlayerClient() : null;
		if(client == null) return Collections.emptyList();

		LinkedHashMap<Integer, NetPlayerInfo> uniquePlayers = new LinkedHashMap<Integer, NetPlayerInfo>();
		for(NetPlayerInfo player: client.getPlayerInfoList()) {
			if(player != null) uniquePlayers.put(player.uid, player);
		}

		ArrayList<NetPlayerInfo> players = new ArrayList<NetPlayerInfo>(uniquePlayers.values());
		Collections.sort(players, new Comparator<NetPlayerInfo>() {
			public int compare(NetPlayerInfo o1, NetPlayerInfo o2) {
				return o1.strName.compareToIgnoreCase(o2.strName);
			}
		});
		return players;
	}

	private String buildLobbySummary() {
		NetPlayerClient client = (netLobby != null) ? netLobby.getNetPlayerClient() : null;
		if(client == null) return "Disconnected";
		return "Player: " + client.getPlayerName() + " | Server: " + client.getHost() + ":" + client.getPort() +
			" | Online: " + client.getPlayerCount();
	}

	private String formatPlayerSummary(NetPlayerInfo pInfo) {
		StringBuilder sb = new StringBuilder();
		sb.append(pInfo.uid == getCurrentUID() ? "*" : " ");
		sb.append(pInfo.strName);
		if(pInfo.strTeam != null && pInfo.strTeam.length() > 0) {
			sb.append(" - ").append(pInfo.strTeam);
		}
		if(pInfo.roomID >= 0) {
			sb.append(" [room ").append(pInfo.roomID).append(']');
		}
		return sb.toString();
	}

	private String getRoomRuleLabel(NetRoomInfo roomInfo) {
		if(roomInfo.ruleLock) {
			return (roomInfo.ruleName != null && roomInfo.ruleName.length() > 0) ? roomInfo.ruleName.toUpperCase() : "(locked)";
		}
		return "Any";
	}

	private String getRoomModeLabel(NetRoomInfo roomInfo) {
		if(roomInfo == null) return "";
		if(roomInfo.singleplayer) {
			return roomInfo.strMode + " (1P)";
		}
		return roomInfo.strMode;
	}

	private String getCreateRoomHeader(boolean detailMode) {
		if(!detailMode) {
			return "Create a multiplayer room.";
		}
		NetRoomInfo roomInfo = getViewedRoomInfo();
		if(roomInfo == null) return "Room detail";
		return "Room #" + roomInfo.roomID + " | " + (roomInfo.playing ? "Playing" : "Waiting") +
			" | Players " + roomInfo.playerSeatedCount + "/" + roomInfo.maxPlayers +
			" | Spectators " + roomInfo.spectatorCount;
	}

	private boolean shouldShowJoinButtonsForViewedRoom() {
		NetPlayerClient client = (netLobby != null) ? netLobby.getNetPlayerClient() : null;
		if(client == null) return true;
		NetPlayerInfo yourInfo = client.getYourPlayerInfo();
		return yourInfo == null || yourInfo.roomID != currentViewDetailRoomID;
	}

	private NetRoomInfo getViewedRoomInfo() {
		NetPlayerClient client = (netLobby != null) ? netLobby.getNetPlayerClient() : null;
		return (client != null) ? client.getRoomInfo(currentViewDetailRoomID) : null;
	}

	private String[] buildPresetNames() {
		if(presets.isEmpty()) return new String[0];
		String[] result = new String[presets.size()];
		for(int i = 0; i < presets.size(); i++) {
			result[i] = presets.get(i).strName;
		}
		return result;
	}

	private NetRoomInfo getSelectedPreset() {
		if(presets.isEmpty()) return null;
		int selected = clamp(createRatedPresetIndex[0], 0, presets.size() - 1);
		createRatedPresetIndex[0] = selected;
		return presets.get(selected);
	}

	private String[] buildSingleRuleNames() {
		LinkedList<String> list = new LinkedList<String>();
		list.add(YOUR_RULE_LABEL);
		if(netLobby != null && netLobby.listRatedRuleName != null && netLobby.listRatedRuleName.length > 0) {
			list.addAll(netLobby.listRatedRuleName[GameEngine.GAMESTYLE_TETROMINO]);
		}
		return list.toArray(new String[list.size()]);
	}

	private String getSelectedSingleRuleName() {
		String[] names = buildSingleRuleNames();
		if(names.length <= 1) return "";
		if(createRoom1PRuleIndex[0] <= 0 || createRoom1PRuleIndex[0] >= names.length) return "";
		return names[createRoom1PRuleIndex[0]];
	}

	private void selectSingleRuleByName(String ruleName) {
		createRoom1PRuleIndex[0] = 0;
		if(ruleName == null || ruleName.length() <= 0) return;
		String[] ruleNames = buildSingleRuleNames();
		for(int i = 1; i < ruleNames.length; i++) {
			if(ruleName.equals(ruleNames[i])) {
				createRoom1PRuleIndex[0] = i;
				return;
			}
		}
	}

	private void selectModeByName(String[] modes, int[] selectedIndex, String modeName) {
		selectedIndex[0] = 0;
		if(modes == null || modes.length <= 0 || modeName == null || modeName.length() <= 0) return;
		for(int i = 0; i < modes.length; i++) {
			if(modeName.equals(modes[i])) {
				selectedIndex[0] = i;
				return;
			}
		}
	}

	private String getSelectedModeName(String[] modes, int[] selectedIndex) {
		if(modes == null || modes.length <= 0) return "";
		selectedIndex[0] = clamp(selectedIndex[0], 0, modes.length - 1);
		return modes[selectedIndex[0]];
	}

	private String[] buildSkinOptionNames() {
		String skinDir = propGlobal.getProperty("custom.skin.directory", "res");
		int numSkins = 0;
		while(true) {
			File file = new File(skinDir + "/graphics/blockskin/normal/n" + numSkins + ".png");
			if(file.canRead()) {
				numSkins++;
			} else {
				break;
			}
		}
		String[] result = new String[numSkins + 1];
		result[0] = "Auto";
		for(int i = 0; i < numSkins; i++) {
			result[i + 1] = Integer.toString(i);
		}
		return result;
	}

	private NetRoomInfo createDefaultRoomInfo() {
		NetRoomInfo roomInfo = new NetRoomInfo();
		roomInfo.maxPlayers = propConfig.getProperty("createroom.defaultMaxPlayers", 6);
		roomInfo.autoStartSeconds = propConfig.getProperty("createroom.defaultAutoStartSeconds", 15);
		roomInfo.gravity = propConfig.getProperty("createroom.defaultGravity", 1);
		roomInfo.denominator = propConfig.getProperty("createroom.defaultDenominator", 60);
		roomInfo.are = propConfig.getProperty("createroom.defaultARE", 0);
		roomInfo.areLine = propConfig.getProperty("createroom.defaultARELine", 0);
		roomInfo.lineDelay = propConfig.getProperty("createroom.defaultLineDelay", 0);
		roomInfo.lockDelay = propConfig.getProperty("createroom.defaultLockDelay", 30);
		roomInfo.das = propConfig.getProperty("createroom.defaultDAS", 11);
		roomInfo.hurryupSeconds = propConfig.getProperty("createroom.defaultHurryupSeconds", 180);
		roomInfo.hurryupInterval = propConfig.getProperty("createroom.defaultHurryupInterval", 5);
		roomInfo.garbagePercent = propConfig.getProperty("createroom.defaultGarbagePercent", 90);
		roomInfo.targetTimer = propConfig.getProperty("createroom.defaultTargetTimer", 60);
		roomInfo.ruleLock = propConfig.getProperty("createroom.defaultRuleLock", false);
		roomInfo.tspinEnableType = propConfig.getProperty("createroom.defaultTSpinEnableType", 1);
		roomInfo.spinCheckType = propConfig.getProperty("createroom.defaultSpinCheckType", 0);
		roomInfo.tspinEnableEZ = propConfig.getProperty("createroom.defaultTSpinEnableEZ", false);
		roomInfo.b2b = propConfig.getProperty("createroom.defaultB2B", true);
		roomInfo.combo = propConfig.getProperty("createroom.defaultCombo", true);
		roomInfo.rensaBlock = propConfig.getProperty("createroom.defaultRensaBlock", true);
		roomInfo.counter = propConfig.getProperty("createroom.defaultCounter", true);
		roomInfo.bravo = propConfig.getProperty("createroom.defaultBravo", true);
		roomInfo.reduceLineSend = propConfig.getProperty("createroom.defaultReduceLineSend", true);
		roomInfo.garbageChangePerAttack = propConfig.getProperty("createroom.defaultGarbageChangePerAttack", true);
		roomInfo.divideChangeRateByPlayers = propConfig.getProperty("createroom.defaultDivideChangeRateByPlayers", false);
		roomInfo.b2bChunk = propConfig.getProperty("createroom.defaultB2BChunk", false);
		roomInfo.useFractionalGarbage = propConfig.getProperty("createroom.defaultUseFractionalGarbage", false);
		roomInfo.isTarget = propConfig.getProperty("createroom.defaultIsTarget", false);
		roomInfo.autoStartTNET2 = propConfig.getProperty("createroom.defaultAutoStartTNET2", false);
		roomInfo.disableTimerAfterSomeoneCancelled = propConfig.getProperty("createroom.defaultDisableTimerAfterSomeoneCancelled", false);
		roomInfo.useMap = propConfig.getProperty("createroom.defaultUseMap", false);
		if(multiModeList.length > 0) roomInfo.strMode = multiModeList[0];
		return roomInfo;
	}

	private void clearBuffer(byte[] buffer, int[] lengthHolder) {
		Arrays.fill(buffer, (byte)0);
		lengthHolder[0] = 0;
	}

	private void copyStringToBuffer(String value, byte[] buffer, int[] lengthHolder) {
		clearBuffer(buffer, lengthHolder);
		if(value == null) value = "";
		byte[] bytes = value.getBytes();
		int len = Math.min(bytes.length, buffer.length - 1);
		if(len > 0) {
			System.arraycopy(bytes, 0, buffer, 0, len);
		}
		lengthHolder[0] = len;
	}

	private String getBufferString(byte[] buffer, int[] lengthHolder) {
		return new String(buffer, 0, Math.max(0, lengthHolder[0])).trim();
	}

	private String getDefaultedRoomName(String requestedName) {
		if(requestedName != null && requestedName.length() > 0) return requestedName;
		String playerName = getBufferString(playerNameBuf, playerNameLen);
		if(playerName.length() > 0) return playerName + "'s room";
		return "Room";
	}

	private int getCurrentUID() {
		NetPlayerClient client = (netLobby != null) ? netLobby.getNetPlayerClient() : null;
		return (client != null) ? client.getPlayerUID() : -1;
	}

	private int clamp(int value, int min, int max) {
		if(value < min) return min;
		if(value > max) return max;
		return value;
	}

	public void netlobbyOnInit(NetLobby lobby) {}

	public void netlobbyOnLoginOK(NetLobby lobby, NetPlayerClient client) {
		pendingActions.add(new Runnable() {
			public void run() {
				currentScreen = SCREEN_LOBBY;
				appendChatLog("[System] Login successful.");
				statusMessage = "Connected.";
			}
		});
	}

	public void netlobbyOnRoomJoin(NetLobby lobby, NetPlayerClient client, NetRoomInfo roomInfo) {
		final NetRoomInfo info = (roomInfo != null) ? roomInfo : ((client != null) ? client.getCurrentRoomInfo() : null);
		pendingActions.add(new Runnable() {
			public void run() {
				handoffModeName = (info != null) ? info.strMode : null;
				handoffToNetGame = true;
			}
		});
	}

	public void netlobbyOnRoomLeave(NetLobby lobby, NetPlayerClient client) {
		pendingActions.add(new Runnable() {
			public void run() {
				currentScreen = SCREEN_LOBBY;
				currentViewDetailRoomID = -1;
				statusMessage = "Returned to lobby.";
			}
		});
	}

	public void netlobbyOnDisconnect(NetLobby lobby, NetPlayerClient client, Throwable ex) {
		final String message = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Disconnected";
		pendingActions.add(new Runnable() {
			public void run() {
				shutdownActiveLobby();
				currentScreen = SCREEN_SERVER_SELECT;
				currentViewDetailRoomID = -1;
				statusMessage = message;
				appendChatLog("[System] " + message);
			}
		});
	}

	public void netlobbyOnMessage(NetLobby lobby, NetPlayerClient client, String[] message) throws IOException {
		if(message[0].equals("welcome") && message.length >= 3) {
			final String line = "[System] Server version " + message[1] + ", players " + message[2];
			pendingActions.add(new Runnable() { public void run() { appendChatLog(line); }});
		}
		if(message[0].equals("loginsuccess") && message.length >= 2) {
			final String line = "[System] Logged in as " + NetUtil.urlDecode(message[1]);
			pendingActions.add(new Runnable() { public void run() { appendChatLog(line); }});
		}
		if(message[0].equals("loginfail")) {
			final String failText;
			if(message.length > 2 && "DIFFERENT_VERSION".equals(message[1])) {
				failText = "Login failed: client/server versions differ.";
			} else if(message.length > 2 && "DIFFERENT_BUILD".equals(message[1])) {
				failText = "Login failed: client/server build types differ.";
			} else {
				failText = "Login failed.";
			}
			pendingActions.add(new Runnable() {
				public void run() {
					shutdownActiveLobby();
					currentScreen = SCREEN_SERVER_SELECT;
					statusMessage = failText;
				}
			});
		}
		if(message[0].equals("banned")) {
			pendingActions.add(new Runnable() {
				public void run() {
					shutdownActiveLobby();
					currentScreen = SCREEN_SERVER_SELECT;
					statusMessage = "Banned from server.";
				}
			});
		}
		if(message[0].equals("roomjoinfail")) {
			pendingActions.add(new Runnable() {
				public void run() {
					statusMessage = "Failed to join room.";
				}
			});
		}
		if(message[0].equals("roomkicked")) {
			final String reason = (message.length > 1) ? message[1] : "UNKNOWN";
			pendingActions.add(new Runnable() {
				public void run() {
					currentScreen = SCREEN_LOBBY;
					currentViewDetailRoomID = -1;
					statusMessage = "Kicked from room: " + reason;
				}
			});
		}
		if(message[0].equals("lobbychat") && client != null && message.length >= 5) {
			NetPlayerInfo pInfo = client.getPlayerInfoByUID(Integer.parseInt(message[1]));
			if(pInfo != null) {
				final String line = "<" + pInfo.strName + "> " + NetUtil.urlDecode(message[4]);
				pendingActions.add(new Runnable() { public void run() { appendChatLog(line); }});
			}
		}
		if(message[0].equals("lobbychath") && message.length >= 4) {
			final String line = "<" + NetUtil.urlDecode(message[1]) + "> " + NetUtil.urlDecode(message[3]);
			pendingActions.add(new Runnable() { public void run() { appendChatLog(line); }});
		}
		if(message[0].equals("announce") && message.length >= 2) {
			final String line = "<ADMIN> " + NetUtil.urlDecode(message[1]);
			pendingActions.add(new Runnable() { public void run() { appendChatLog(line); }});
		}
		if(message[0].equals("playerenter") && client != null && message.length >= 2) {
			NetPlayerInfo pInfo = client.getPlayerInfoByUID(Integer.parseInt(message[1]));
			if(pInfo != null) {
				final String line = "[System] " + pInfo.strName + " entered a room.";
				pendingActions.add(new Runnable() { public void run() { appendChatLog(line); }});
			}
		}
		if(message[0].equals("playerleave") && client != null && message.length >= 2) {
			NetPlayerInfo pInfo = client.getPlayerInfoByUID(Integer.parseInt(message[1]));
			if(pInfo != null) {
				final String line = "[System] " + pInfo.strName + " left a room.";
				pendingActions.add(new Runnable() { public void run() { appendChatLog(line); }});
			}
		}
		if(message[0].equals("changeteam") && client != null && message.length >= 2) {
			NetPlayerInfo pInfo = client.getPlayerInfoByUID(Integer.parseInt(message[1]));
			if(pInfo != null) {
				final String line = "[System] " + pInfo.strName + " changed team.";
				pendingActions.add(new Runnable() { public void run() { appendChatLog(line); }});
			}
		}
		if(message[0].equals("ratedpresets")) {
			final ArrayList<NetRoomInfo> newPresets = new ArrayList<NetRoomInfo>();
			for(int i = 1; i < message.length; i++) {
				newPresets.add(new NetRoomInfo(NetUtil.decompressString(message[i])));
			}
			pendingActions.add(new Runnable() {
				public void run() {
					if(currentScreen != SCREEN_CREATERATED_WAITING) return;
					presets.clear();
					presets.addAll(newPresets);
					if(presets.isEmpty()) {
						setCreateRoomUIType(false, null);
						currentScreen = SCREEN_CREATEROOM;
					} else {
						createRatedPresetIndex[0] = clamp(createRatedPresetIndex[0], 0, presets.size() - 1);
						currentScreen = SCREEN_CREATERATED;
					}
					statusMessage = "";
				}
			});
		}
		if(message[0].equals("roomlist")) {
			pendingActions.add(new Runnable() {
				public void run() {
					statusMessage = "";
					if(currentViewDetailRoomID != -1 && getViewedRoomInfo() == null) {
						currentViewDetailRoomID = -1;
						if(currentScreen == SCREEN_CREATEROOM || currentScreen == SCREEN_CREATEROOM1P) {
							currentScreen = SCREEN_LOBBY;
						}
					}
				}
			});
		}
		if(message[0].equals("roomupdate") && message.length >= 2) {
			final NetRoomInfo roomInfo = new NetRoomInfo(message[1]);
			pendingActions.add(new Runnable() {
				public void run() {
					if(roomInfo.roomID != currentViewDetailRoomID) return;
					if(currentScreen == SCREEN_CREATEROOM && !roomInfo.singleplayer) {
						importRoomInfoToCreateRoomScreen(roomInfo);
					} else if(currentScreen == SCREEN_CREATEROOM1P && roomInfo.singleplayer) {
						setCreateRoom1PUIType(true, roomInfo);
					}
				}
			});
		}
		if(message[0].equals("roomdelete") && message.length >= 2) {
			final NetRoomInfo roomInfo = new NetRoomInfo(message[1]);
			pendingActions.add(new Runnable() {
				public void run() {
					if(roomInfo.roomID == currentViewDetailRoomID) {
						currentViewDetailRoomID = -1;
						if(currentScreen == SCREEN_CREATEROOM || currentScreen == SCREEN_CREATEROOM1P) {
							currentScreen = SCREEN_LOBBY;
						}
					}
				}
			});
		}
		if(message[0].equals("mpranking") && message.length >= 4) {
			final int myRank = Integer.parseInt(message[2]);
			final String rankingBlob = NetUtil.decompressString(message[3]);
			pendingActions.add(new Runnable() {
				public void run() {
					rankingData.clear();
					rankingMyRank = myRank;
					String[] rows = rankingBlob.split("\t");
					for(int i = 0; i < rows.length; i++) {
						String[] cols = rows[i].split(";");
						if(cols.length < 5) continue;
						String[] row = new String[5];
						int rank = Integer.parseInt(cols[0]);
						row[0] = (rank == -1) ? "N/A" : Integer.toString(rank + 1);
						row[1] = NetUtil.urlDecode(cols[1]);
						row[2] = cols[2];
						row[3] = cols[3];
						row[4] = cols[4];
						rankingData.add(row);
					}
				}
			});
		}
	}

	public void netlobbyOnExit(NetLobby lobby) {}

	private void appendChatLog(String line) {
		chatLog.add(line);
		while(chatLog.size() > CHAT_LOG_LIMIT) {
			chatLog.removeFirst();
		}
	}

	private static final class RuleEntry {
		String filename;
		String filepath;
		String rulename;
		int style;
	}

	private static final class TuningState {
		final int[] rotateButtonDefaultRight = new int[] {0};
		final int[] moveDiagonal = new int[] {0};
		final int[] blockShowOutlineOnly = new int[] {0};
		final int[] skin = new int[] {0};
		final int[] blockOutlineType = new int[] {0};
		final int[] minDas = new int[] {-1};
		final int[] maxDas = new int[] {-1};
		final int[] dasDelay = new int[] {-1};
		final int[] reverseUpDown = new int[] {0};
	}

	private static final class RoomFormState {
		final byte[] nameBuf = new byte[64];
		final int[] nameLen = new int[] {0};
		final int[] modeIndex = new int[] {0};
		final int[] maxPlayers = new int[] {6};
		final int[] autoStartSeconds = new int[] {15};
		final int[] gravity = new int[] {1};
		final int[] denominator = new int[] {60};
		final int[] are = new int[] {0};
		final int[] areLine = new int[] {0};
		final int[] lineDelay = new int[] {0};
		final int[] lockDelay = new int[] {30};
		final int[] das = new int[] {11};
		final int[] hurryupSeconds = new int[] {180};
		final int[] hurryupInterval = new int[] {5};
		final int[] garbagePercent = new int[] {90};
		final int[] targetTimer = new int[] {60};
		final int[] mapSetId = new int[] {0};
		final int[] presetId = new int[] {0};
		final byte[] presetCodeBuf = new byte[1024];
		final int[] presetCodeLen = new int[] {0};
		final int[] ruleLock = new int[] {0};
		final int[] useMap = new int[] {0};
		final int[] tspinEnableType = new int[] {1};
		final int[] spinCheckType = new int[] {0};
		final int[] tspinEnableEZ = new int[] {0};
		final int[] b2b = new int[] {1};
		final int[] combo = new int[] {1};
		final int[] bravo = new int[] {1};
		final int[] rensaBlock = new int[] {1};
		final int[] counter = new int[] {1};
		final int[] reduceLineSend = new int[] {1};
		final int[] garbageChangePerAttack = new int[] {1};
		final int[] divideChangeRateByPlayers = new int[] {0};
		final int[] b2bChunk = new int[] {0};
		final int[] useFractionalGarbage = new int[] {0};
		final int[] isTarget = new int[] {0};
		final int[] autoStartTNET2 = new int[] {0};
		final int[] disableTimerAfterSomeoneCancelled = new int[] {0};
	}
}

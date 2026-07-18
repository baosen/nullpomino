package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.game.net.LoungeService;
import nullpomino.game.net.NetLanDiscovery;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetPlatform;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.gui.sdl.binding.Ref.FloatRef;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SdlBackend;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlTexture;
import nullpomino.gui.sdl.widget.ButtonSDL;
import nullpomino.gui.sdl.widget.TableSDL;
import nullpomino.gui.sdl.widget.TextInputSDL;
import nullpomino.gui.sdl.widget.WidgetSDL;
import nullpomino.util.CustomProperties;

/** Headless lounge lifecycle, discovery, navigation, chat, and handshake matrix. */
class StateNetLobbySDLBranchMatrixTest {
	private static final SdlRenderer RENDERER = new SdlRenderer() {};
	private static final SdlTexture TEXTURE = new SdlTexture() {};
	private static int mouseX;
	private static int mouseY;
	private static int mouseButtons;

	private FakeLounge lounge;
	private TestLobby lobby;
	private TestState state;

	@BeforeAll
	static void installBackend() {
		SdlBackend.set(new SdlBackend.Backend() {
			@Override public SDL3 sdl3() { return sdlStub(); }
			@Override public SDL3Image image() { return stub(SDL3Image.class); }
			@Override public SDL3TTF ttf() { return stub(SDL3TTF.class); }
			@Override public SDL3Mixer mixerOrNull() { return null; }
		});
	}

	@BeforeEach
	void setUp() {
		NullpoMinoSDL.renderer = RENDERER;
		NullpoMinoSDL.propConfig = new CustomProperties();
		NullpoMinoSDL.propGlobal = new CustomProperties();
		NullpoMinoSDL.propLang = new CustomProperties();
		NullpoMinoSDL.propLangDefault = new CustomProperties();
		NullpoMinoSDL.webMode = true;
		NullpoMinoSDL.keyPressedState = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		NullpoMinoSDL.joystickMax = 0;
		NullpoMinoSDL.joyUseNumber = new int[] {-1, -1};
		NullpoMinoSDL.mouseWheelDelta = 0;
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.pendingTextInput.setLength(0);
		NullpoMinoSDL.gameStates = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < NullpoMinoSDL.gameStates.length; i++) {
			NullpoMinoSDL.gameStates[i] = new NoopState();
		}
		NullpoMinoSDL.currentState = -1;
		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_TITLE);
		GameKeySDL.initGlobalGameKeySDL();
		for(GameKeySDL key : GameKeySDL.gamekey) key.loadDefaultKeymap(0);

		MouseInputSDL.initalizeMouseInput();
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();

		ResourceHolderSDL.soundManager = new SoundManagerSDL();
		ResourceHolderSDL.imgFont = TEXTURE;
		ResourceHolderSDL.imgFontSmall = TEXTURE;
		ResourceHolderSDL.imgFontBig = TEXTURE;
		ResourceHolderSDL.imgMenu = TEXTURE;

		lounge = new FakeLounge();
		NetPlatform.install(null, lounge);
		lobby = freshLobby();
		NullpoMinoSDL.netLobby = lobby;
		state = new TestState();
	}

	@Test
	void enterLeaveAndChatConsumerMatrices() {
		NullpoMinoSDL.netLobby = null;
		state.enter();
		assertNotNull(NullpoMinoSDL.netLobby);
		assertNotNull(lounge.consumer);

		lounge.consumer.onChat("alice", "hello");
		NullpoMinoSDL.netLobby = null;
		lounge.consumer.onChat("nobody", "ignored");
		state.leave();

		lobby = freshLobby();
		NullpoMinoSDL.netLobby = lobby;
		lounge.openResult = false;
		state = new TestState();
		state.enter();
		assertFalse((Boolean)get(state, "loungeOpen"));
		state.leave();
		assertTrue(lounge.closes > 0);
		assertTrue(lobby.saves > 0);

		lounge.openResult = true;
		lobby.propConfig.setProperty("serverselect.txtfldPlayerName.text", "player");
		lobby.propConfig.setProperty("serverselect.txtfldPlayerTeam.text", "team");
		state = new TestState();
		state.enter();
		assertEquals(get(state, "chatInput"), get(state, "focused"));

		FakeClient live = new FakeClient();
		lobby.netPlayerClient = live;
		set(state, "pendingAction", 2);
		state.enter();
		assertEquals(2, get(state, "pendingAction"));
		live.connected = false;
		state.enter();

		set(state, "lounge", null);
		NullpoMinoSDL.netLobby = null;
		state.leave();
	}

	@Test
	void discoveryRowsSelectionAndJoinMatrices() {
		state.enter();
		set(state, "loungeOpen", false);
		invoke(state, "refreshRoomTable", noTypes());

		set(state, "loungeOpen", true);
		set(state, "lanKey", "stale");
		lounge.rooms.clear();
		invoke(state, "refreshRoomTable", noTypes());
		invoke(state, "refreshRoomTable", noTypes());

		NetLanDiscovery.Announce one = announce("s1", "One", "", false, 1);
		NetLanDiscovery.Announce two = announce("s2", "Two", "RULE", true, 2);
		lounge.rooms.add(one);
		lounge.rooms.add(two);
		invoke(state, "refreshRoomTable", noTypes());
		TableSDL table = (TableSDL)get(state, "roomTable");
		assertEquals(2, table.getRowCount());

		table.setSelectedIndex(1);
		lounge.rooms = new ArrayList<NetLanDiscovery.Announce>(Arrays.asList(two, one));
		invoke(state, "refreshRoomTable", noTypes());
		assertEquals(0, table.getSelectedIndex());
		setDeclared(table, TableSDL.class, "selected", 99);
		set(state, "lanKey", "invalid-selection");
		invoke(state, "refreshRoomTable", noTypes());
		table.setSelectedIndex(1);
		lounge.rooms = new ArrayList<NetLanDiscovery.Announce>(Arrays.asList(two));
		invoke(state, "refreshRoomTable", noTypes());

		String[] any = (String[])invoke(null, "rowFromAnnounce",
				new Class<?>[] {NetLobbyFrame.class, NetLanDiscovery.Announce.class}, lobby, one);
		String[] rule = (String[])invoke(null, "rowFromAnnounce",
				new Class<?>[] {NetLobbyFrame.class, NetLanDiscovery.Announce.class}, lobby, two);
		assertEquals("ANY", any[1]);
		assertEquals("RULE", rule[1]);

		set(state, "pendingAction", 1);
		invoke(state, "attemptJoinSelected", new Class<?>[] {boolean.class}, false);
		set(state, "pendingAction", 0);
		table.setSelectedIndex(-1);
		invoke(state, "attemptJoinSelected", new Class<?>[] {boolean.class}, false);
		setDeclared(table, TableSDL.class, "selected", 99);
		invoke(state, "attemptJoinSelected", new Class<?>[] {boolean.class}, false);

		setDeclared(table, TableSDL.class, "selected", 0);
		((TextInputSDL)get(state, "nameInput")).setText("");
		invoke(state, "attemptJoinSelected", new Class<?>[] {boolean.class}, false);
		((TextInputSDL)get(state, "nameInput")).setText("player");
		state.joinFails = true;
		invoke(state, "attemptJoinSelected", new Class<?>[] {boolean.class}, false);
		state.joinFails = false;
		invoke(state, "attemptJoinSelected", new Class<?>[] {boolean.class}, true);
		assertEquals(2, get(state, "pendingAction"));
		assertTrue((Boolean)get(state, "pendingWatch"));

		invoke(state, "startJoin", new Class<?>[] {String.class, int.class, boolean.class}, "host", 1, false);
		invoke(state, "cancelPendingJoin", new Class<?>[] {NetLobbyFrame.class}, lobby);
		assertEquals(0, get(state, "pendingAction"));
	}

	@Test
	void createChatCommandAndSessionMatrices() {
		state.enter();
		TextInputSDL name = (TextInputSDL)get(state, "nameInput");

		set(state, "pendingAction", 1);
		invoke(state, "createRoom", noTypes());
		set(state, "pendingAction", 0);
		name.setText(" ");
		invoke(state, "createRoom", noTypes());
		name.setText("player");
		invoke(state, "createRoom", noTypes());
		assertEquals(NullpoMinoSDL.STATE_NET_CREATEROOM, NullpoMinoSDL.currentState);

		state = enteredState();
		send("");
		send("/join");
		send("/join host:bad");
		state.joinFails = true;
		send("/join host");
		state.joinFails = false;
		send("/join host:1234");
		set(state, "pendingAction", 0);

		FakeClient live = new FakeClient();
		lobby.netPlayerClient = live;
		send("server chat");
		assertEquals(1, lobby.chats);
		lobby.netPlayerClient = null;
		FakeClient stale = new FakeClient();
		stale.connected = false;
		lobby.netPlayerClient = stale;
		send("disconnected client");
		lobby.netPlayerClient = null;

		send("/name");
		send("/name Alice");
		send("/team");
		send("/team Red Team");
		send("/help");
		send("/?");
		send("/unknown");
		name.setText("");
		send("hello lounge");
		name.setText("Bob");
		send("second lounge line");
		assertTrue(lounge.chats >= 2);
	}

	@Test
	void updateConnectionPendingMouseAndMirrorMatrices() {
		state.enter();
		((TextInputSDL)get(state, "nameInput")).setText("new name");
		((TextInputSDL)get(state, "teamInput")).setText("new team");
		state.update();
		assertTrue(lobby.pumps > 0);
		set(state, "lounge", null);
		set(state, "loungeOpen", false);
		((TextInputSDL)get(state, "nameInput")).setText("other");
		state.update();
		set(state, "lounge", lounge);
		set(state, "loungeOpen", true);

		FakeClient client = new FakeClient();
		lobby.netPlayerClient = client;
		lobby.lobbyMode = NetLobbyFrame.LOBBYMODE_INROOM;
		state.update();
		assertEquals(NullpoMinoSDL.STATE_NETGAME, NullpoMinoSDL.currentState);

		state = enteredState();
		client = new FakeClient();
		client.connected = false;
		lobby.netPlayerClient = client;
		for(int pending : new int[] {2, 1, 0}) {
			set(state, "pendingAction", pending);
			state.update();
			lobby.netPlayerClient = client;
		}

		state = enteredState();
		client = new FakeClient();
		lobby.netPlayerClient = client;
		lobby.lobbyMode = NetLobbyFrame.LOBBYMODE_LOBBY;
		lobby.roomList.add(new NetRoomInfo());
		lobby.roomList.getFirst().roomID = 42;
		set(state, "pendingAction", 2);
		set(state, "pendingWatch", true);
		set(state, "autoRoomJoinSent", false);
		state.update();
		state.update();
		assertEquals(1, lobby.joins);

		state = enteredState();
		set(state, "pendingAction", 2);
		set(state, "pendingSince", System.currentTimeMillis());
		lobby.netPlayerClient = null;
		state.update();
		FakeClient waitingClient = new FakeClient();
		lobby.netPlayerClient = waitingClient;
		lobby.lobbyMode = NetLobbyFrame.LOBBYMODE_DISCONNECTED;
		state.update();
		lobby.lobbyMode = NetLobbyFrame.LOBBYMODE_LOBBY;
		state.update();
		lobby.roomList.add(new NetRoomInfo());
		set(state, "autoRoomJoinSent", true);
		state.update();
		set(state, "pendingAction", 1);
		state.update();

		for(int pending : new int[] {2, 1}) {
			state = enteredState();
			set(state, "pendingAction", pending);
			set(state, "pendingSince", 0L);
			state.update();
		}

		state = enteredState();
		set(state, "pendingAction", 2);
		set(state, "pendingSince", System.currentTimeMillis());
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X1MASK);
		state.update();
		state = enteredState();
		setMouse(-1, -1, 0);
		state.update();
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X1MASK);
		state.update();
		state = enteredState();
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X2MASK);
		state.update();
		state = enteredState();
		set(state, "pendingAction", 1);
		set(state, "pendingSince", System.currentTimeMillis());
		state.update();

		NullpoMinoSDL.netLobby = null;
		state.update();
	}

	@Test
	void focusNavigationGlobalKeysAndUpdateKeyLoopMatrices() {
		state.enter();
		WidgetSDL name = (WidgetSDL)get(state, "nameInput");
		WidgetSDL team = (WidgetSDL)get(state, "teamInput");
		TableSDL table = (TableSDL)get(state, "roomTable");
		WidgetSDL chat = (WidgetSDL)get(state, "chatInput");
		ButtonSDL[] row = (ButtonSDL[])invoke(state, "buttonRow", noTypes());

		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, name);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, name);
		invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, true);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, name);
		invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, false);
		invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, true);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, team);
		invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, false);
		invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, true);

		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, table);
		invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, true);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, table);
		invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, false);
		table.addRow(new String[] {"a"});
		table.addRow(new String[] {"b"});
		table.setSelectedIndex(1);
		invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, true);
		table.setSelectedIndex(1);
		assertFalse((Boolean)invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, true));
		table.setSelectedIndex(1);
		assertTrue((Boolean)invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, false));
		table.setSelectedIndex(0);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, table);
		assertFalse((Boolean)invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, false));

		for(ButtonSDL button : row) {
			invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, button);
			invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, true);
			invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, button);
			invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, false);
		}
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, chat);
		invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, true);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, chat);
		invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, false);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, new Object[] {null});
		assertFalse((Boolean)invoke(state, "tryWidgetNav", new Class<?>[] {boolean.class}, false));

		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, row[0]);
		invoke(state, "tryButtonRowNav", new Class<?>[] {boolean.class}, true);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, row[row.length - 1]);
		invoke(state, "tryButtonRowNav", new Class<?>[] {boolean.class}, false);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, name);
		assertFalse((Boolean)invoke(state, "tryButtonRowNav", new Class<?>[] {boolean.class}, false));

		handle(SDLConstants.SDL_SCANCODE_F1, 0, true);
		handle(SDLConstants.SDL_SCANCODE_F1, 0, false);
		set(state, "pendingAction", 2);
		handle(SDLConstants.SDL_SCANCODE_ESCAPE, 0, false);
		set(state, "pendingAction", 0);
		handle(SDLConstants.SDL_SCANCODE_ESCAPE, 0, false);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, chat);
		((TextInputSDL)chat).setText("key chat");
		handle(SDLConstants.SDL_SCANCODE_RETURN, 0, false);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, table);
		handle(SDLConstants.SDL_SCANCODE_KP_ENTER, 0, false);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, name);
		handle(SDLConstants.SDL_SCANCODE_RETURN, 0, false);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, team);
		handle(SDLConstants.SDL_SCANCODE_RETURN, 0, false);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, row[0]);
		handle(SDLConstants.SDL_SCANCODE_RETURN, 0, false);
		handle(SDLConstants.SDL_SCANCODE_TAB, 0, false);
		handle(SDLConstants.SDL_SCANCODE_TAB, SDLConstants.SDL_KMOD_SHIFT, false);
		handle(SDLConstants.SDL_SCANCODE_PAGEUP, 0, false);
		handle(SDLConstants.SDL_SCANCODE_PAGEDOWN, 0, false);

		state = enteredState();
		NullpoMinoSDL.pendingTextInput.append("typed");
		ButtonSDL[] currentRow = (ButtonSDL[])invoke(state, "buttonRow", noTypes());
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, currentRow[0]);
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_LEFT, 0, false));
		state.update();
		NullpoMinoSDL.frameKeyEvents.clear();
		state = enteredState();
		NullpoMinoSDL.pendingTextInput.append("typed");
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_HOME, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_END, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_LEFT, 0, true));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RIGHT, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_UP, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, true));
		state.update();
		NullpoMinoSDL.frameKeyEvents.clear();
		TableSDL currentTable = (TableSDL)get(state, "roomTable");
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, currentTable);
		currentTable.addRow(new String[] {"a"});
		currentTable.addRow(new String[] {"b"});
		currentTable.setSelectedIndex(1);
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_UP, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_F1, 0, true));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_HOME, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, true));
		state.update();
		NullpoMinoSDL.frameKeyEvents.clear();
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, new Object[] {null});
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_F1, 0, false));
		state.update();
	}

	@Test
	void mouseWidgetsAndRenderPresenceMatrices() {
		state.enter();
		clickForUpdate(80, 10);
		clickForUpdate(350, 10);
		clickForUpdate(20, 50);
		clickForUpdate(20, 455);
		NullpoMinoSDL.webMode = true;
		state.render();
		NullpoMinoSDL.webMode = false;
		state.render();
		lounge.rooms.add(announce("click", "Clickable", "", false, 1));
		set(state, "lanKey", "click-row");
		invoke(state, "refreshRoomTable", noTypes());
		clickForUpdate(20, 60);
		clickForUpdate(20, 60);

		NullpoMinoSDL.netLobby = null;
		state.render();
		NullpoMinoSDL.netLobby = lobby;
		state = enteredState();
		NullpoMinoSDL.webMode = true;
		state.render();
		NullpoMinoSDL.webMode = false;
		state.render();

		lounge.rooms.add(announce("room", "Room", "RULE", false, 1));
		set(state, "lanKey", "force");
		invoke(state, "refreshRoomTable", noTypes());
		set(state, "presenceName", "A very long local player name");
		String ownId = (String)get(state, "presenceInstanceId");
		lounge.presence.add(new NetLanDiscovery.Presence("self", ownId));
		lounge.presence.add(new NetLanDiscovery.Presence(null, "null"));
		lounge.presence.add(new NetLanDiscovery.Presence("A very long visitor name", "long"));
		for(int i = 0; i < 15; i++) lounge.presence.add(new NetLanDiscovery.Presence("user" + i, "id" + i));
		state.render();
		set(state, "presenceName", "abc");
		state.render();
		set(state, "loungeOpen", false);
		set(state, "presenceName", "");
		state.render();
	}

	private TestState enteredState() {
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.pendingTextInput.setLength(0);
		lobby = freshLobby();
		NullpoMinoSDL.netLobby = lobby;
		state = new TestState();
		state.enter();
		return state;
	}

	private TestLobby freshLobby() {
		TestLobby result = new TestLobby();
		result.propConfig = new CustomProperties();
		result.propGlobal = new CustomProperties();
		return result;
	}

	private void handle(int scancode, int mod, boolean repeat) {
		invoke(state, "handleGlobalKey", new Class<?>[] {NetLobbyFrame.class, NullpoMinoSDL.KeyEvent.class},
				lobby, new NullpoMinoSDL.KeyEvent(scancode, mod, repeat));
	}

	private void send(String text) {
		((TextInputSDL)get(state, "chatInput")).setText(text);
		invoke(state, "sendChat", new Class<?>[] {NetLobbyFrame.class}, lobby);
	}

	private void clickForUpdate(int x, int y) {
		setMouse(x, y, 0);
		state.update();
		setMouse(x, y, SDLConstants.SDL_BUTTON_LMASK);
		state.update();
		setMouse(x, y, 0);
		state.update();
	}

	private static NetLanDiscovery.Announce announce(
			String session, String roomName, String rule, boolean playing, int seated) {
		return new NetLanDiscovery.Announce("127.0.0.1", 9000 + seated, "host", "1",
				session, "lobby", seated, roomName, false, rule, "MODE", playing, seated, 6, 2);
	}

	private static Class<?>[] noTypes() { return new Class<?>[0]; }

	private static Object invoke(Object target, String name, Class<?>[] types, Object... args) {
		try {
			Method method = StateNetLobbySDL.class.getDeclaredMethod(name, types);
			method.setAccessible(true);
			return method.invoke(target, args);
		} catch(Exception e) {
			throw new AssertionError(name, e);
		}
	}

	private static Object get(Object target, String name) {
		try {
			Field field = StateNetLobbySDL.class.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(target);
		} catch(Exception e) {
			throw new AssertionError(name, e);
		}
	}

	private static void set(Object target, String name, Object value) {
		setDeclared(target, StateNetLobbySDL.class, name, value);
	}

	private static void setDeclared(Object target, Class<?> owner, String name, Object value) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch(Exception e) {
			throw new AssertionError(name, e);
		}
	}

	private static void setMouse(int x, int y, int buttons) {
		mouseX = x;
		mouseY = y;
		mouseButtons = buttons;
	}

	private static SDL3 sdlStub() {
		return (SDL3)Proxy.newProxyInstance(SDL3.class.getClassLoader(), new Class<?>[] {SDL3.class},
				(proxy, method, args) -> {
					if(method.getName().equals("SDL_GetMouseState")) {
						((FloatRef)args[0]).value = mouseX;
						((FloatRef)args[1]).value = mouseY;
						return mouseButtons;
					}
					if(method.getName().equals("SDL_RenderCoordinatesFromWindow")) {
						((FloatRef)args[3]).value = (Float)args[1];
						((FloatRef)args[4]).value = (Float)args[2];
						return (byte)1;
					}
					return defaultValue(method.getReturnType());
				});
	}

	private static <T> T stub(Class<T> iface) {
		return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface},
				(proxy, method, args) -> defaultValue(method.getReturnType())));
	}

	private static Object defaultValue(Class<?> type) {
		if(type == byte.class) return (byte)1;
		if(type == short.class) return (short)0;
		if(type == int.class) return 0;
		if(type == long.class) return 0L;
		if(type == float.class) return 0f;
		if(type == double.class) return 0d;
		if(type == boolean.class) return false;
		return null;
	}

	private static final class FakeLounge implements LoungeService {
		boolean openResult = true;
		int closes;
		int chats;
		String presenceName;
		NetLanDiscovery.ChatConsumer consumer;
		List<NetLanDiscovery.Announce> rooms = new ArrayList<NetLanDiscovery.Announce>();
		List<NetLanDiscovery.Presence> presence = new ArrayList<NetLanDiscovery.Presence>();
		@Override public boolean open() { return openResult; }
		@Override public void close() { closes++; }
		@Override public void setChatConsumer(NetLanDiscovery.ChatConsumer consumer) { this.consumer = consumer; }
		@Override public void sendChat(String playerName, String message) { chats++; }
		@Override public void setPresence(String playerName, String instanceId) { presenceName = playerName; }
		@Override public List<NetLanDiscovery.Announce> snapshotRooms() {
			return new ArrayList<NetLanDiscovery.Announce>(rooms);
		}
		@Override public List<NetLanDiscovery.Presence> snapshotPresence() {
			return new ArrayList<NetLanDiscovery.Presence>(presence);
		}
	}

	private static final class TestLobby extends NetLobbyFrame {
		int pumps;
		int saves;
		int joins;
		int chats;
		@Override public void pump() { pumps++; }
		@Override public void saveConfig() { saves++; }
		@Override public void joinRoom(int roomID, boolean watch) { joins++; }
		@Override public void sendChat(boolean roomchat, String strMsg) { chats++; }
		@Override public String getUIText(String key) {
			if(key.endsWith("Any")) return "ANY";
			if(key.endsWith("Playing")) return "PLAYING";
			if(key.endsWith("Waiting")) return "WAITING";
			return key;
		}
	}

	private static final class FakeClient extends NetPlayerClient {
		boolean connected = true;
		@Override public boolean isConnected() { return connected; }
	}

	private static final class TestState extends StateNetLobbySDL {
		boolean joinFails;
		@Override protected void connectJoinedRoom(
				NetLobbyFrame nl, String host, int port, String playerName, String team) throws IOException {
			if(joinFails) throw new IOException("matrix");
			nl.netPlayerClient = new FakeClient();
		}
	}

	private static final class NoopState extends BaseStateSDL {
		@Override public void enter() {}
		@Override public void leave() {}
	}
}

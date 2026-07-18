package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.gui.net.NetLobbyFrame.RoomCreateMode;
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
import nullpomino.gui.sdl.widget.DropdownSDL;
import nullpomino.gui.sdl.widget.SpinnerSDL;
import nullpomino.gui.sdl.widget.TextInputSDL;
import nullpomino.gui.sdl.widget.WidgetSDL;
import nullpomino.util.CustomProperties;

/** Headless lifecycle, form, input, handshake, preset, and render branch matrix. */
class StateNetCreateRoomSDLBranchMatrixTest {
	private static final SdlRenderer RENDERER = new SdlRenderer() {};
	private static final SdlTexture TEXTURE = new SdlTexture() {};
	private static int mouseX;
	private static int mouseY;
	private static int mouseButtons;

	private StateNetCreateRoomSDL state;
	private TestLobby lobby;

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

		lobby = new TestLobby();
		lobby.propConfig = new CustomProperties();
		lobby.propGlobal = new CustomProperties();
		lobby.backupRoomInfo = new NetRoomInfo();
		NullpoMinoSDL.netLobby = lobby;
		state = new TestState();
	}

	@Test
	void enterResolveSourceAndCurrentModeMatrices() {
		NullpoMinoSDL.netLobby = null;
		state.enter();
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
		assertEquals(RoomCreateMode.MULTIPLAYER, invoke(state, "currentMode", noTypes()));

		lobby = freshLobby();
		NullpoMinoSDL.netLobby = lobby;
		lobby.backupRoomInfo = null;
		state.enter();
		assertNotNull(lobby.backupRoomInfo);
		state.leave();
		assertNull(get(state, "focused"));

		lobby.propConfig.setProperty("createroom.lastMode", "RATED");
		state.enter();
		assertEquals(RoomCreateMode.MULTIPLAYER, lobby.createRoomMode);
		assertEquals(RoomCreateMode.MULTIPLAYER, invoke(state, "currentMode", noTypes()));

		lobby.propConfig.setProperty("createroom.lastMode", "SINGLE_PLAYER");
		state.enter();
		assertEquals(RoomCreateMode.SINGLE_PLAYER, lobby.createRoomMode);
		assertFalse(((SpinnerSDL)get(state, "maxPlayers")).enabled);
		lobby.backupRoomInfo.strMode = null;
		state.enter();

		StateNetCreateRoomSDL unbuilt = new StateNetCreateRoomSDL();
		assertEquals(RoomCreateMode.SINGLE_PLAYER, invoke(unbuilt, "currentMode", noTypes()));
		NullpoMinoSDL.netLobby = null;
		assertEquals(RoomCreateMode.MULTIPLAYER, invoke(unbuilt, "currentMode", noTypes()));
		NullpoMinoSDL.netLobby = lobby;

		DropdownSDL selector = (DropdownSDL)get(state, "modeSelector");
		setDeclared(selector, DropdownSDL.class, "selected", -1);
		assertEquals(RoomCreateMode.MULTIPLAYER, invoke(state, "currentMode", noTypes()));
		setDeclared(selector, DropdownSDL.class, "selected", RoomCreateMode.values().length);
		assertEquals(RoomCreateMode.MULTIPLAYER, invoke(state, "currentMode", noTypes()));

		NetRoomInfo detail = populatedRoom("detail", "NET-VS-BATTLE");
		detail.roomID = 7;
		detail.singleplayer = true;
		FakeClient client = new FakeClient();
		client.room = detail;
		lobby.netPlayerClient = client;
		lobby.currentViewDetailRoomID = 7;
		state.enter();
		assertTrue((Boolean)get(state, "detailMode"));
		assertEquals(RoomCreateMode.SINGLE_PLAYER, lobby.createRoomMode);
		assertTrue(((ButtonSDL)get(state, "joinBtn")).visible);

		detail.rated = true;
		detail.customRated = false;
		state.enter();
		assertEquals(RoomCreateMode.MULTIPLAYER, lobby.createRoomMode);

		client.room = null;
		lobby.backupRoomInfo = null;
		state.enter();
		assertNotNull(lobby.backupRoomInfo);
		lobby.netPlayerClient = null;
		state.enter();
	}

	@Test
	void modeFocusTabAndKeyboardNavigationMatrices() {
		state.enter();
		DropdownSDL selector = (DropdownSDL)get(state, "modeSelector");
		SpinnerSDL maxPlayers = (SpinnerSDL)get(state, "maxPlayers");

		NullpoMinoSDL.netLobby = null;
		invoke(state, "onModeChanged", noTypes());
		NullpoMinoSDL.netLobby = lobby;

		selector.setSelectedIndex(RoomCreateMode.SINGLE_PLAYER.ordinal());
		invoke(state, "onModeChanged", noTypes());
		assertEquals(1, maxPlayers.getValue());
		assertFalse(maxPlayers.enabled);
		invoke(state, "onModeChanged", noTypes());

		selector.setSelectedIndex(RoomCreateMode.MULTIPLAYER.ordinal());
		invoke(state, "onModeChanged", noTypes());
		assertTrue(maxPlayers.enabled);
		set(state, "lastModeIndex", -1);
		invoke(state, "onModeChanged", noTypes());
		set(state, "lastModeIndex", RoomCreateMode.values().length);
		invoke(state, "onModeChanged", noTypes());

		set(state, "detailMode", true);
		invoke(state, "applyDetailModeEnabled", noTypes());
		set(state, "focused", null);
		invoke(state, "tryTabFormNav", new Class<?>[] {boolean.class}, false);
		invoke(state, "tryButtonRowNav", new Class<?>[] {boolean.class}, false);
		set(state, "detailMode", false);
		invoke(state, "applyDetailModeEnabled", noTypes());

		WidgetSDL roomName = (WidgetSDL)get(state, "roomName");
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, roomName);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, roomName);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, new Object[] {null});

		invoke(state, "switchTab", new Class<?>[] {int.class}, -1);
		invoke(state, "switchTab", new Class<?>[] {int.class}, 5);
		invoke(state, "switchTab", new Class<?>[] {int.class}, 2);
		set(state, "focused", null);
		assertTrue((Boolean)invoke(state, "tryTabFormNav", new Class<?>[] {boolean.class}, false));
		assertTrue((Boolean)invoke(state, "tryTabFormNav", new Class<?>[] {boolean.class}, true));
		for(int i = 0; i < 12; i++) invoke(state, "tryTabFormNav", new Class<?>[] {boolean.class}, false);

		ButtonSDL cancel = (ButtonSDL)get(state, "cancelBtn");
		ButtonSDL ok = (ButtonSDL)get(state, "okBtn");
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, ok);
		assertTrue((Boolean)invoke(state, "tryButtonRowNav", new Class<?>[] {boolean.class}, true));
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, cancel);
		assertTrue((Boolean)invoke(state, "tryButtonRowNav", new Class<?>[] {boolean.class}, true));
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, cancel);
		assertTrue((Boolean)invoke(state, "tryButtonRowNav", new Class<?>[] {boolean.class}, false));
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, roomName);
		assertFalse((Boolean)invoke(state, "tryButtonRowNav", new Class<?>[] {boolean.class}, false));

		assertFalse(handleKey(SDLConstants.SDL_SCANCODE_ESCAPE, 0, true));
		assertFalse(handleKey(SDLConstants.SDL_SCANCODE_F1, 0, false));
		assertTrue(handleKey(SDLConstants.SDL_SCANCODE_PAGEUP, 0, false));
		assertTrue(handleKey(SDLConstants.SDL_SCANCODE_PAGEDOWN, 0, false));
		assertTrue(handleKey(SDLConstants.SDL_SCANCODE_TAB, 0, false));
		assertTrue(handleKey(SDLConstants.SDL_SCANCODE_TAB, SDLConstants.SDL_KMOD_SHIFT, false));
		assertTrue(handleKey(SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));
	}

	@Test
	void updateMouseTextAndKeyDispatchMatrices() {
		state.enter();
		state.update();
		assertTrue(lobby.pumps > 0);
		((DropdownSDL)get(state, "modeSelector")).setSelectedIndex(RoomCreateMode.SINGLE_PLAYER.ordinal());
		state.update();

		NullpoMinoSDL.pendingTextInput.append("room");
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, get(state, "roomName"));
		state.update();
		assertEquals("room", ((TextInputSDL)get(state, "roomName")).getText());
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, new Object[] {null});
		state.update();

		clickForUpdate(150, 40);
		clickForUpdate(220, 80);
		clickForUpdate(220, 106);
		lobby.netPlayerClient = new FakeClient();
		clickForUpdate(10, 430);

		state = enteredFreshState();
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, get(state, "cancelBtn"));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_LEFT, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_PAGEDOWN, 0, false));
		state.update();
		NullpoMinoSDL.frameKeyEvents.clear();

		state = enteredFreshState();
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, new Object[] {null});
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_F1, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_LEFT, 0, true));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RIGHT, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_UP, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, true));
		state.update();
		NullpoMinoSDL.frameKeyEvents.clear();

		state = enteredDetailState();
		clickForUpdate(150, 430);
		state = enteredDetailState();
		clickForUpdate(280, 430);
		state = enteredDetailState();
		((DropdownSDL)get(state, "modeSelector")).setSelectedIndex(RoomCreateMode.SINGLE_PLAYER.ordinal());
		state.update();

		state = enteredFreshState();
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X2MASK);
		state.update();
		state = enteredFreshState();
		setMouse(-1, -1, 0);
		state.update();
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X1MASK);
		state.update();

		NullpoMinoSDL.netLobby = null;
		state.update();
	}

	@Test
	void submitJoinAndWaitHandshakeMatrices() {
		state.enter();
		NullpoMinoSDL.netLobby = null;
		invoke(state, "submit", new Class<?>[] {boolean.class, boolean.class}, true, false);
		NullpoMinoSDL.netLobby = lobby;
		lobby.netPlayerClient = null;
		invoke(state, "submit", new Class<?>[] {boolean.class, boolean.class}, true, false);

		FakeClient client = new FakeClient();
		lobby.netPlayerClient = client;
		lobby.currentViewDetailRoomID = -1;
		invoke(state, "submit", new Class<?>[] {boolean.class, boolean.class}, true, false);
		lobby.currentViewDetailRoomID = 4;
		invoke(state, "submit", new Class<?>[] {boolean.class, boolean.class}, true, false);
		invoke(state, "submit", new Class<?>[] {boolean.class, boolean.class}, true, true);
		assertEquals(2, lobby.joins);

		state = enteredFreshState();
		client = new FakeClient();
		lobby.netPlayerClient = client;
		TextInputSDL roomName = (TextInputSDL)get(state, "roomName");
		roomName.setText("   ");
		invoke(state, "submit", new Class<?>[] {boolean.class, boolean.class}, false, false);
		assertEquals("ROOM NAME REQUIRED", get(state, "statusLine"));

		DropdownSDL selector = (DropdownSDL)get(state, "modeSelector");
		selector.setSelectedIndex(RoomCreateMode.SINGLE_PLAYER.ordinal());
		invoke(state, "onModeChanged", noTypes());
		roomName.setText("");
		invoke(state, "submit", new Class<?>[] {boolean.class, boolean.class}, false, false);
		roomName.setText("one player");
		setDeclared(get(state, "modeDropdown"), DropdownSDL.class, "selected", -1);
		invoke(state, "submit", new Class<?>[] {boolean.class, boolean.class}, false, false);
		assertTrue((Boolean)get(state, "waitingForRoom"));

		lobby.lobbyMode = NetLobbyFrame.LOBBYMODE_LOBBY;
		invoke(state, "updateWaitForRoom", new Class<?>[] {NetLobbyFrame.class}, lobby);
		assertNotNull(client.sent);
		invoke(state, "updateWaitForRoom", new Class<?>[] {NetLobbyFrame.class}, lobby);
		lobby.lobbyMode = NetLobbyFrame.LOBBYMODE_INROOM;
		invoke(state, "updateWaitForRoom", new Class<?>[] {NetLobbyFrame.class}, lobby);
		assertEquals(NullpoMinoSDL.STATE_NETGAME, NullpoMinoSDL.currentState);

		state = enteredFreshState();
		lobby.backupRoomInfo = null;
		((TextInputSDL)get(state, "roomName")).setText("new transport");
		lobby.netPlayerClient = null;
		invoke(state, "submit", new Class<?>[] {boolean.class, boolean.class}, false, false);
		assertTrue((Boolean)get(state, "waitingForRoom"));

		state = enteredFreshState();
		((TextInputSDL)get(state, "roomName")).setText("failed transport");
		FakeClient stale = new FakeClient();
		stale.connected = false;
		lobby.netPlayerClient = stale;
		((TestState)state).connectionFails = true;
		invoke(state, "submit", new Class<?>[] {boolean.class, boolean.class}, false, false);
		assertTrue(((String)get(state, "statusLine")).startsWith("CREATE FAILED"));

		state = enteredFreshState();
		set(state, "waitingForRoom", true);
		set(state, "pendingSince", System.currentTimeMillis());
		lobby.netPlayerClient = null;
		invoke(state, "updateWaitForRoom", new Class<?>[] {NetLobbyFrame.class}, lobby);

		state = enteredFreshState();
		set(state, "waitingForRoom", true);
		FakeClient disconnected = new FakeClient();
		disconnected.connected = false;
		lobby.netPlayerClient = disconnected;
		invoke(state, "updateWaitForRoom", new Class<?>[] {NetLobbyFrame.class}, lobby);

		state = enteredFreshState();
		set(state, "waitingForRoom", true);
		set(state, "pendingSince", 0L);
		lobby.netPlayerClient = new FakeClient();
		lobby.lobbyMode = NetLobbyFrame.LOBBYMODE_DISCONNECTED;
		invoke(state, "updateWaitForRoom", new Class<?>[] {NetLobbyFrame.class}, lobby);

		state = enteredFreshState();
		set(state, "waitingForRoom", true);
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_F1, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_ESCAPE, 0, true));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));
		invoke(state, "updateWaitForRoom", new Class<?>[] {NetLobbyFrame.class}, lobby);
		NullpoMinoSDL.frameKeyEvents.clear();

		state = enteredFreshState();
		set(state, "waitingForRoom", true);
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X1MASK);
		invoke(state, "updateWaitForRoom", new Class<?>[] {NetLobbyFrame.class}, lobby);
	}

	@Test
	void formMessageMapPresetAndRenderMatrices() {
		state.enter();
		NetRoomInfo room = populatedRoom("matrix", "NET-VS-BATTLE");
		invoke(state, "applyRoomInfoToForm", new Class<?>[] {NetRoomInfo.class}, room);
		NetRoomInfo collected = new NetRoomInfo();
		invoke(state, "collectFormInto", new Class<?>[] {NetRoomInfo.class}, collected);
		assertEquals("matrix", collected.strName);

		room.strName = null;
		invoke(state, "applyRoomInfoToForm", new Class<?>[] {NetRoomInfo.class}, room);
		room.strName = "matrix";
		room.strMode = null;
		assertNotNull(invoke(state, "buildRoomCreateMessage", new Class<?>[] {NetRoomInfo.class, int.class}, room, 0));
		room.strName = null;
		assertNull(invoke(state, "buildRoomCreateMessage", new Class<?>[] {NetRoomInfo.class, int.class}, room, 0));
		room.strName = "  ";
		assertNull(invoke(state, "buildRoomCreateMessage", new Class<?>[] {NetRoomInfo.class, int.class}, room, 0));
		room.strName = "maps";
		room.useMap = true;
		assertNotNull(invoke(state, "buildRoomCreateMessage", new Class<?>[] {NetRoomInfo.class, int.class}, room, 99999));
		assertFalse(room.useMap);
		room.useMap = true;
		assertNotNull(invoke(state, "buildRoomCreateMessage", new Class<?>[] {NetRoomInfo.class, int.class}, room, 0));
		List<String> modes = new ArrayList<String>();
		invoke(null, "ensureModeListFallback", new Class<?>[] {List.class}, modes);
		assertEquals(1, modes.size());
		invoke(null, "ensureModeListFallback", new Class<?>[] {List.class}, modes);

		assertNull(invoke(null, "loadAndCompressMapSet", new Class<?>[] {int.class}, 99999));
		assertNotNull(invoke(null, "loadAndCompressMapSet", new Class<?>[] {int.class}, 0));
		NullpoMinoSDL.netLobby = null;
		assertNotNull(invoke(null, "loadAndCompressMapSet", new Class<?>[] {int.class}, 0));
		NullpoMinoSDL.netLobby = lobby;
		CustomProperties emptyMap = new CustomProperties();
		assertNull(invoke(null, "compressMapSet", new Class<?>[] {CustomProperties.class}, emptyMap));
		emptyMap.setProperty("map.maxMapNumber", 1);
		assertNull(invoke(null, "compressMapSet", new Class<?>[] {CustomProperties.class}, emptyMap));

		NullpoMinoSDL.netLobby = null;
		invoke(state, "saveCurrentAsPreset", noTypes());
		invoke(state, "loadCurrentPreset", noTypes());
		NullpoMinoSDL.netLobby = lobby;
		invoke(state, "saveCurrentAsPreset", noTypes());
		assertTrue(lobby.saves > 0);
		invoke(state, "loadCurrentPreset", noTypes());
		((SpinnerSDL)get(state, "presetID")).setValue(1);
		invoke(state, "loadCurrentPreset", noTypes());
		lobby.propConfig.setProperty("0.preset.1", "");
		invoke(state, "loadCurrentPreset", noTypes());
		lobby.propConfig.setProperty("0.preset.1", "not-base64");
		invoke(state, "loadCurrentPreset", noTypes());
		setDeclared(get(state, "modeDropdown"), DropdownSDL.class, "selected", -1);
		invoke(state, "savePreviousMode", new Class<?>[] {NetLobbyFrame.class}, lobby);

		invoke(state, "exportPresetCode", noTypes());
		String code = ((TextInputSDL)get(state, "presetCodeInput")).getText();
		assertFalse(code.isEmpty());
		invoke(state, "importPresetCode", noTypes());
		((TextInputSDL)get(state, "presetCodeInput")).setText(" --- ");
		invoke(state, "importPresetCode", noTypes());
		((TextInputSDL)get(state, "presetCodeInput")).setText("AAAA");
		invoke(state, "importPresetCode", noTypes());

		NullpoMinoSDL.netLobby = null;
		state.render();
		NullpoMinoSDL.netLobby = lobby;
		state = enteredFreshState();
		for(int tab = 0; tab < 5; tab++) {
			invoke(state, "switchTab", new Class<?>[] {int.class}, tab);
			state.render();
		}
		((SpinnerSDL)get(state, "hurryupInterval")).setValue(1);
		invoke(state, "switchTab", new Class<?>[] {int.class}, 0);
		set(state, "statusLine", "STATUS");
		state.render();
		((SpinnerSDL)get(state, "hurryupInterval")).setValue(2);
		state.render();
		((DropdownSDL)get(state, "modeSelector")).setSelectedIndex(RoomCreateMode.SINGLE_PLAYER.ordinal());
		invoke(state, "onModeChanged", noTypes());
		state.render();
		set(state, "detailMode", true);
		state.render();
		assertDoesNotThrow(state::leave);
	}

	@Test
	void cancelPersistenceAndRoomSessionMatrices() {
		state.enter();
		NullpoMinoSDL.netLobby = null;
		invoke(state, "cancel", noTypes());
		NullpoMinoSDL.netLobby = lobby;

		state = enteredFreshState();
		lobby.backupRoomInfo = null;
		invoke(state, "cancel", noTypes());

		state = enteredFreshState();
		((DropdownSDL)get(state, "modeSelector")).setSelectedIndex(RoomCreateMode.SINGLE_PLAYER.ordinal());
		invoke(state, "onModeChanged", noTypes());
		lobby.roomSession = true;
		invoke(state, "cancel", noTypes());
		assertTrue(lobby.saves > 0);

		state = enteredFreshState();
		set(state, "detailMode", true);
		invoke(state, "cancel", noTypes());
	}

	private boolean handleKey(int scancode, int mod, boolean repeat) {
		return (Boolean)invoke(state, "handleGlobalKey", new Class<?>[] {NullpoMinoSDL.KeyEvent.class},
				new NullpoMinoSDL.KeyEvent(scancode, mod, repeat));
	}

	private StateNetCreateRoomSDL enteredFreshState() {
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.pendingTextInput.setLength(0);
		lobby = freshLobby();
		NullpoMinoSDL.netLobby = lobby;
		StateNetCreateRoomSDL result = new TestState();
		result.enter();
		return result;
	}

	private StateNetCreateRoomSDL enteredDetailState() {
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		lobby = freshLobby();
		NetRoomInfo room = populatedRoom("detail", "NET-VS-BATTLE");
		room.roomID = 7;
		FakeClient client = new FakeClient();
		client.room = room;
		lobby.netPlayerClient = client;
		lobby.currentViewDetailRoomID = 7;
		NullpoMinoSDL.netLobby = lobby;
		StateNetCreateRoomSDL result = new TestState();
		result.enter();
		return result;
	}

	private TestLobby freshLobby() {
		TestLobby result = new TestLobby();
		result.propConfig = new CustomProperties();
		result.propGlobal = new CustomProperties();
		result.backupRoomInfo = new NetRoomInfo();
		return result;
	}

	private void clickForUpdate(int x, int y) {
		setMouse(x, y, 0);
		state.update();
		setMouse(x, y, SDLConstants.SDL_BUTTON_LMASK);
		state.update();
		setMouse(x, y, 0);
		state.update();
	}

	private static NetRoomInfo populatedRoom(String name, String mode) {
		NetRoomInfo r = new NetRoomInfo();
		r.strName = name;
		r.strMode = mode;
		r.maxPlayers = 4;
		r.autoStartSeconds = 9;
		r.gravity = 2;
		r.denominator = 120;
		r.are = 3;
		r.areLine = 4;
		r.lineDelay = 5;
		r.lockDelay = 6;
		r.das = 7;
		r.tspinEnableType = 2;
		r.spinCheckType = 1;
		r.tspinEnableEZ = true;
		r.b2b = false;
		r.combo = false;
		r.rensaBlock = false;
		r.counter = false;
		r.bravo = false;
		r.garbagePercent = 55;
		r.targetTimer = 80;
		r.garbageChangePerAttack = false;
		r.divideChangeRateByPlayers = true;
		r.b2bChunk = true;
		r.useFractionalGarbage = true;
		r.isTarget = true;
		r.reduceLineSend = false;
		r.hurryupSeconds = 60;
		r.hurryupInterval = 2;
		r.autoStartTNET2 = true;
		r.disableTimerAfterSomeoneCancelled = true;
		r.ruleLock = true;
		return r;
	}

	private static Class<?>[] noTypes() { return new Class<?>[0]; }

	private static Object invoke(Object target, String name, Class<?>[] types, Object... args) {
		try {
			Method method = StateNetCreateRoomSDL.class.getDeclaredMethod(name, types);
			method.setAccessible(true);
			return method.invoke(target, args);
		} catch(Exception e) {
			throw new AssertionError(name, e);
		}
	}

	private static Object get(Object target, String name) {
		try {
			Field field = StateNetCreateRoomSDL.class.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(target);
		} catch(Exception e) {
			throw new AssertionError(name, e);
		}
	}

	private static void set(Object target, String name, Object value) {
		setDeclared(target, StateNetCreateRoomSDL.class, name, value);
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

	private static final class TestLobby extends NetLobbyFrame {
		int pumps;
		int saves;
		int joins;
		boolean roomSession;
		@Override public void pump() { pumps++; }
		@Override public void saveConfig() { saves++; }
		@Override public void joinRoom(int roomID, boolean watch) { joins++; }
		@Override public boolean isRoomSession() { return roomSession; }
	}

	private static final class FakeClient extends NetPlayerClient {
		boolean connected = true;
		String sent;
		NetRoomInfo room;
		@Override public boolean isConnected() { return connected; }
		@Override public boolean send(String msg) { sent = msg; return true; }
		@Override public NetRoomInfo getRoomInfo(int roomID) { return room; }
	}

	private static final class TestState extends StateNetCreateRoomSDL {
		boolean connectionFails;
		@Override protected void connectNewRoomSession(NetLobbyFrame nl, String playerName, String team)
				throws IOException {
			if(connectionFails) throw new IOException("matrix");
			nl.netPlayerClient = new FakeClient();
		}
	}

	private static final class NoopState extends BaseStateSDL {
		@Override public void enter() {}
		@Override public void leave() {}
	}
}

package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.game.mode.GameMode;
import nullpomino.game.mode.NetDummyMode;
import nullpomino.game.ai.DummyAI;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
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
import nullpomino.gui.sdl.binding.SdlHandles.MixTrack;
import nullpomino.util.CustomProperties;
import nullpomino.util.ModeManager;

/** Headless net-game lifecycle, input, mode-switch, callback, and render matrix. */
class StateNetGameSDLBranchMatrixTest {
	private static final SdlRenderer RENDERER = new SdlRenderer() {};
	private static final SdlTexture TEXTURE = new SdlTexture() {};
	private static int mouseX;
	private static int mouseY;
	private static int mouseButtons;

	private StateNetGameSDL state;
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
		NullpoMinoSDL.propMusic = new CustomProperties();
		NullpoMinoSDL.modeManager = new ModeManager();
		NullpoMinoSDL.modeManager.addMode(new TestNetMode("TEST-NET", 0));
		NullpoMinoSDL.modeManager.addMode(new TestNetMode("TEST-NET-STYLE", 1));
		NullpoMinoSDL.modeManager.addMode(new TestMode());
		NullpoMinoSDL.webMode = true;
		NullpoMinoSDL.keyPressedState = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		NullpoMinoSDL.joystickMax = 0;
		NullpoMinoSDL.joyUseNumber = new int[] {-1, -1};
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.gameStates = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < NullpoMinoSDL.gameStates.length; i++) NullpoMinoSDL.gameStates[i] = new NoopState();
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
		ResourceHolderSDL.bgmPlaying = -1;
		ResourceHolderSDL.bgmTrack = null;
		NullpoMinoSDL.mixerLib = null;
		NullpoMinoSDL.mixer = null;

		lobby = new TestLobby();
		lobby.propConfig = new CustomProperties();
		lobby.propGlobal = new CustomProperties();
		NullpoMinoSDL.netLobby = lobby;
		state = new StateNetGameSDL();
	}

	@Test
	void enterSourceModeAndLeaveMatrices() {
		NullpoMinoSDL.netLobby = null;
		state.enter();
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);

		NullpoMinoSDL.netLobby = lobby;
		lobby.netPlayerClient = null;
		state = new StateNetGameSDL();
		state.enter();
		assertNotNull(state.gameManager);
		assertTrue(state.gameManager.mode instanceof NetDummyMode);
		state.leave();
		assertNull(state.gameManager);

		FakeClient client = new FakeClient();
		lobby.netPlayerClient = client;
		state = new StateNetGameSDL();
		state.enter();
		state.leave();

		client.me = player(-1);
		state = new StateNetGameSDL();
		state.enter();
		state.leave();

		client.me.roomID = 7;
		client.room = null;
		state = new StateNetGameSDL();
		state.enter();
		state.leave();

		client.room = room(null);
		state = new StateNetGameSDL();
		state.enter();
		state.leave();
		client.room.strMode = "";
		state = new StateNetGameSDL();
		state.enter();
		state.leave();

		client.room.strMode = "TEST-NET";
		state = new StateNetGameSDL();
		state.enter();
		assertEquals("TEST-NET", state.gameManager.mode.getName());
		state.leave();

		state = new StateNetGameSDL();
		set(state, "netLobby", null);
		state.leave();
		StateNetGameSDL.instance = new StateNetGameSDL();
		state.leave();
	}

	@Test
	void renderStructuralAndExceptionMatrices() {
		state.enter();
		state.render();
		state.gameManager = null;
		state.render();

		ThrowingManager manager = manager();
		state.gameManager = manager;
		manager.renderFailure = new NullPointerException("matrix");
		state.render();
		manager.quit = true;
		state.render();
		manager.quit = false;
		manager.renderFailure = new IllegalStateException("matrix");
		state.render();
		manager.quit = true;
		state.render();
		manager.renderFailure = null;
		state.render();
	}

	@Test
	void updateLeaveInputStructureAndModeSwitchMatrices() {
		state.enter();
		state.update();
		assertTrue(lobby.pumps > 0);
		NullpoMinoSDL.netLobby = null;
		state.update();
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));
		state.update();
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.netLobby = lobby;

		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_ESCAPE, 0, true));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_F1, 0, false));
		state.update();
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));
		state.update();
		NullpoMinoSDL.frameKeyEvents.clear();

		state = enteredState();
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X1MASK);
		state.update();
		state = enteredState();
		clickForUpdate(620, 10);

		state = enteredState();
		lobby.roomSession = true;
		lobby.netPlayerClient = null;
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));
		state.update();
		NullpoMinoSDL.frameKeyEvents.clear();
		state = enteredState();
		lobby.roomSession = true;
		FakeClient disconnected = new FakeClient();
		disconnected.connected = false;
		lobby.netPlayerClient = disconnected;
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));
		state.update();
		NullpoMinoSDL.frameKeyEvents.clear();

		state = enteredState();
		lobby.roomSession = true;
		FakeClient connected = new FakeClient();
		lobby.netPlayerClient = connected;
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));
		state.update();
		NullpoMinoSDL.frameKeyEvents.clear();

		state = enteredState();
		GameManager valid = state.gameManager;
		state.gameManager = null;
		state.update();
		state.gameManager = valid;
		GameEngine[] engines = valid.engine;
		valid.engine = null;
		state.update();
		valid.engine = new GameEngine[0];
		state.update();
		valid.engine = new GameEngine[] {null};
		state.update();
		valid.engine = engines;
		engines[0].isInGame = true;
		state.update();
		state.update();
		engines[0].isInGame = false;
		state.update();

		state.gameManager.mode = null;
		state.update();
		state.gameManager.mode = new TestNetMode("TEST-NET", 0);
		set(state, "strModeToEnter", null);
		state.update();
		set(state, "strModeToEnter", "TEST-NET");
		state.update();
		set(state, "strModeToEnter", "");
		state.update();
	}

	@Test
	void updateJoystickBgmQuitRetryAndCatchMatrices() {
		state.enter();
		NullpoMinoSDL.joystickMax = 1;
		NullpoMinoSDL.joyUseNumber[0] = 0;
		NullpoMinoSDL.joyPressedState = new boolean[][] {new boolean[32]};
		NullpoMinoSDL.joyAxisX = new int[] {0};
		NullpoMinoSDL.joyAxisY = new int[] {0};
		NullpoMinoSDL.joyHatState = new int[] {0};
		state.update();
		NullpoMinoSDL.joyUseNumber[0] = -1;
		state.update();
		NullpoMinoSDL.joyUseNumber[0] = 1;
		state.update();

		ThrowingManager manager = manager();
		state.gameManager = manager;
		manager.engine[0].isInGame = true;
		manager.bgmStatus.bgm = -1;
		ResourceHolderSDL.bgmPlaying = -1;
		state.update();
		manager.quit = true;
		state.update();
		manager.quit = false;

		int retryKey = GameKeySDL.gamekey[0].keymap[GameKeySDL.BUTTON_RETRY];
		NullpoMinoSDL.keyPressedState[retryKey] = true;
		state.update();
		NullpoMinoSDL.keyPressedState[retryKey] = false;

		manager.updateFailure = new NullPointerException("matrix");
		state.update();
		manager.quit = true;
		state.update();
		manager.quit = false;
		manager.updateFailure = new IllegalStateException("matrix");
		state.update();
		manager.quit = true;
		state.update();
		manager.updateFailure = null;

		state.gameManager = manager;
		NullpoMinoSDL.propConfig.setProperty("option.bgm", false);
		manager.bgmStatus.bgm = 0;
		ResourceHolderSDL.bgmPlaying = -1;
		state.update();
		NullpoMinoSDL.mixerLib = playingMixer();
		ResourceHolderSDL.bgmTrack = new MixTrack() {};
		ResourceHolderSDL.bgmPlaying = 0;
		manager.bgmStatus.volume = -1f;
		state.update();
		manager.bgmStatus.volume = 2f;
		state.update();
		manager.bgmStatus.volume = 0.5f;
		state.update();
		NullpoMinoSDL.mixerLib = null;
		ResourceHolderSDL.bgmTrack = null;

		state.gameManager = null;
		set(state, "strModeToEnter", "TEST-NET");
		state.update();
		NullpoMinoSDL.keyPressedState = new boolean[0];
		set(state, "strModeToEnter", "");
		state.update();
		NullpoMinoSDL.keyPressedState = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
	}

	@Test
	void enterNewModeTitleAndListenerMatrices() {
		state.enter();
		invoke(state, "enterNewMode", new Class<?>[] {String.class}, new Object[] {null});
		invoke(state, "enterNewMode", new Class<?>[] {String.class}, "missing");
		invoke(state, "enterNewMode", new Class<?>[] {String.class}, "state-matrix");
		invoke(state, "enterNewMode", new Class<?>[] {String.class}, "TEST-NET");
		state.gameManager.engine[0].ai = new DummyAI();
		invoke(state, "enterNewMode", new Class<?>[] {String.class}, "TEST-NET");
		NullpoMinoSDL.propGlobal.setProperty("0.ruleopt.strRandomizer", "missing.Randomizer");
		NullpoMinoSDL.propGlobal.setProperty("0.ruleopt.strWallkick", "missing.Wallkick");
		NullpoMinoSDL.propGlobal.setProperty("0.ai", "missing.AI");
		invoke(state, "enterNewMode", new Class<?>[] {String.class}, "TEST-NET");
		NullpoMinoSDL.propGlobal.setProperty("0.ruleopt.strRandomizer", "");
		NullpoMinoSDL.propGlobal.setProperty("0.ruleopt.strWallkick", "");
		invoke(state, "enterNewMode", new Class<?>[] {String.class}, "TEST-NET");
		NullpoMinoSDL.propGlobal.setProperty("0.rule.1", "missing-rule");
		invoke(state, "enterNewMode", new Class<?>[] {String.class}, "TEST-NET-STYLE");
		assertFalse((Boolean)invoke(null, "hasConfiguredClass", new Class<?>[] {String.class}, new Object[] {null}));
		assertFalse((Boolean)invoke(null, "hasConfiguredClass", new Class<?>[] {String.class}, ""));
		assertTrue((Boolean)invoke(null, "hasConfiguredClass", new Class<?>[] {String.class}, "configured.Class"));

		state.modeName = null;
		state.updateTitleBarCaption();
		state.modeName = "NET-DUMMY";
		state.updateTitleBarCaption();
		state.modeName = "TEST-NET";
		GameManager manager = state.gameManager;
		state.gameManager = null;
		state.updateTitleBarCaption();
		state.gameManager = manager;
		GameEngine[] engines = manager.engine;
		manager.engine = null;
		state.updateTitleBarCaption();
		manager.engine = new GameEngine[0];
		state.updateTitleBarCaption();
		manager.engine = new GameEngine[] {null};
		state.updateTitleBarCaption();
		manager.engine = engines;
		engines[0].isInGame = true;
		manager.replayMode = false;
		manager.replayRerecord = false;
		state.updateTitleBarCaption();
		manager.replayMode = true;
		state.updateTitleBarCaption();
		manager.replayRerecord = true;
		state.updateTitleBarCaption();
		manager.replayMode = false;
		state.updateTitleBarCaption();
		engines[0].isInGame = false;
		state.updateTitleBarCaption();

		state.netlobbyOnDisconnect(lobby, null, null);
		assertNull(get(state, "strModeToEnter"));
		state.netlobbyOnRoomLeave(lobby, null);
		state.netlobbyOnRoomJoin(lobby, null, room("TEST-NET"));
		assertEquals("TEST-NET", get(state, "strModeToEnter"));
		state.netlobbyOnExit(lobby);
		manager.engine = new GameEngine[0];
		state.netlobbyOnExit(lobby);
		manager.engine = new GameEngine[] {null};
		state.netlobbyOnExit(lobby);
		state.gameManager = null;
		state.netlobbyOnExit(lobby);
	}

	private StateNetGameSDL enteredState() {
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		NullpoMinoSDL.frameKeyEvents.clear();
		lobby = new TestLobby();
		lobby.propConfig = new CustomProperties();
		lobby.propGlobal = new CustomProperties();
		NullpoMinoSDL.netLobby = lobby;
		StateNetGameSDL result = new StateNetGameSDL();
		result.enter();
		return result;
	}

	private static ThrowingManager manager() {
		ThrowingManager result = new ThrowingManager(new RendererSDL());
		result.mode = new TestNetMode("TEST-NET", 0);
		result.init();
		for(GameEngine engine : result.engine) engine.init();
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

	private static NetPlayerInfo player(int roomID) {
		NetPlayerInfo p = new NetPlayerInfo();
		p.uid = 1;
		p.strName = "Alice";
		p.roomID = roomID;
		return p;
	}

	private static NetRoomInfo room(String mode) {
		NetRoomInfo r = new NetRoomInfo();
		r.roomID = 7;
		r.strName = "Room";
		r.strMode = mode;
		return r;
	}

	private static Object invoke(Object target, String name, Class<?>[] types, Object... args) {
		try {
			Method method = StateNetGameSDL.class.getDeclaredMethod(name, types);
			method.setAccessible(true);
			return method.invoke(target, args);
		} catch(Exception e) {
			throw new AssertionError(name, e);
		}
	}

	private static Object get(Object target, String name) {
		try {
			Field field = StateNetGameSDL.class.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(target);
		} catch(Exception e) {
			throw new AssertionError(name, e);
		}
	}

	private static void set(Object target, String name, Object value) {
		try {
			Field field = StateNetGameSDL.class.getDeclaredField(name);
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

	private static SDL3Mixer playingMixer() {
		return (SDL3Mixer)Proxy.newProxyInstance(SDL3Mixer.class.getClassLoader(),
				new Class<?>[] {SDL3Mixer.class}, (proxy, method, args) -> {
					if(method.getName().equals("MIX_TrackPlaying")) return (byte)1;
					return defaultValue(method.getReturnType());
				});
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
		boolean roomSession;
		@Override public void pump() { pumps++; }
		@Override public boolean isRoomSession() { return roomSession; }
		@Override public void saveConfig() {}
	}

	private static final class FakeClient extends NetPlayerClient {
		boolean connected = true;
		NetPlayerInfo me;
		NetRoomInfo room;
		@Override public boolean isConnected() { return connected; }
		@Override public NetPlayerInfo getYourPlayerInfo() { return me; }
		@Override public NetRoomInfo getRoomInfo(int roomID) { return room; }
	}

	private static final class ThrowingManager extends GameManager {
		RuntimeException renderFailure;
		RuntimeException updateFailure;
		boolean quit;
		ThrowingManager(RendererSDL receiver) { super(receiver); }
		@Override public void renderAll() { if(renderFailure != null) throw renderFailure; }
		@Override public void updateAll() { if(updateFailure != null) throw updateFailure; }
		@Override public boolean getQuitFlag() { return quit; }
		@Override public void shutdown() {}
	}

	private static final class TestNetMode extends NetDummyMode {
		private final String name;
		private final int style;
		int retries;
		TestNetMode(String name, int style) { this.name = name; this.style = style; }
		@Override public String getName() { return name; }
		@Override public int getPlayers() { return 1; }
		@Override public int getGameStyle() { return style; }
		@Override public void modeInit(GameManager manager) {}
		@Override public void playerInit(GameEngine engine, int playerID) {}
		@Override public void netplayInit(Object lobby) {}
		@Override public void netplayUnload(Object lobby) {}
		@Override public void netplayOnRetryKey(GameEngine engine, int playerID) { retries++; }
	}

	private static final class TestMode implements GameMode {
		@Override public String getName() { return "state-matrix"; }
		@Override public int getPlayers() { return 1; }
		@Override public int getGameStyle() { return 0; }
		@Override public void modeInit(GameManager manager) {}
		@Override public void playerInit(GameEngine engine, int playerID) {}
		@Override public void renderInput(GameEngine engine, int playerID) {}
		@Override public boolean isNetplayMode() { return false; }
	}

	private static final class NoopState extends BaseStateSDL {
		@Override public void enter() {}
		@Override public void leave() {}
	}
}

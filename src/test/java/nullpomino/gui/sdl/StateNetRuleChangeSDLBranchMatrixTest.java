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

import nullpomino.game.component.RuleOptions;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.play.GameEngine;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.gui.net.NetLobbyFrame.RuleEntry;
import nullpomino.gui.sdl.binding.Ref.FloatRef;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SdlBackend;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlTexture;
import nullpomino.gui.sdl.widget.TabStripSDL;
import nullpomino.gui.sdl.widget.TableSDL;
import nullpomino.gui.sdl.widget.WidgetSDL;
import nullpomino.util.CustomProperties;

/** Headless rule catalog, selection, persistence, input, and render matrix. */
class StateNetRuleChangeSDLBranchMatrixTest {
	private static final SdlRenderer RENDERER = new SdlRenderer() {};
	private static final SdlTexture TEXTURE = new SdlTexture() {};
	private static int mouseX;
	private static int mouseY;
	private static int mouseButtons;

	private StateNetRuleChangeSDL state;
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
		NullpoMinoSDL.keyPressedState = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.gameStates = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < NullpoMinoSDL.gameStates.length; i++) NullpoMinoSDL.gameStates[i] = new NoopState();
		NullpoMinoSDL.currentState = -1;
		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_TITLE);
		MouseInputSDL.initalizeMouseInput();
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		ResourceHolderSDL.imgFont = TEXTURE;
		ResourceHolderSDL.imgFontSmall = TEXTURE;
		ResourceHolderSDL.imgFontBig = TEXTURE;
		ResourceHolderSDL.imgMenu = TEXTURE;

		lobby = new TestLobby();
		lobby.propConfig = new CustomProperties();
		lobby.propGlobal = new CustomProperties();
		lobby.ruleEntries.add(entry("a.rul", "A", "missing-a", 0));
		lobby.ruleEntries.add(entry("b.rul", null, "missing-b", 0));
		lobby.ruleEntries.add(entry("c.rul", "C", "missing-c", 1));
		lobby.ruleEntries.add(entry(null, null, null, 2));
		NullpoMinoSDL.netLobby = lobby;
		state = new StateNetRuleChangeSDL();
	}

	@Test
	void enterFindSelectionAndReturnFromTuningMatrices() {
		NullpoMinoSDL.netLobby = null;
		state.enter();
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);

		NullpoMinoSDL.netLobby = lobby;
		lobby.propGlobal.setProperty("0.rulefile", "b.rul");
		lobby.propGlobal.setProperty("0.rulefile.1", "missing");
		state.enter();
		assertEquals(1, invoke(state, "findCurrentRuleIndex",
				new Class<?>[] {NetLobbyFrame.class, int.class}, lobby, 0));
		assertEquals(0, invoke(state, "findCurrentRuleIndex",
				new Class<?>[] {NetLobbyFrame.class, int.class}, lobby, 1));

		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, get(state, "ruleTable"));
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, get(state, "ruleTable"));
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, new Object[] {null});
		state.leave();

		invoke(state, "openTuning", noTypes());
		assertTrue((Boolean)get(state, "returningFromTuning"));
		state.enter();
		assertFalse((Boolean)get(state, "returningFromTuning"));
	}

	@Test
	void refreshTableSelectionAndNullCatalogMatrices() {
		state.enter();
		NullpoMinoSDL.netLobby = null;
		invoke(state, "refreshTable", noTypes());
		NullpoMinoSDL.netLobby = lobby;

		TabStripSDL tabs = (TabStripSDL)get(state, "tabStrip");
		TableSDL table = (TableSDL)get(state, "ruleTable");
		int[] selected = (int[])get(state, "selectedIndex");
		tabs.setActiveTab(0);
		selected[0] = 1;
		invoke(state, "refreshTable", noTypes());
		assertEquals(1, table.getSelectedIndex());
		selected[0] = -1;
		invoke(state, "refreshTable", noTypes());
		selected[0] = 99;
		invoke(state, "refreshTable", noTypes());

		tabs.setActiveTab(3);
		selected[3] = -1;
		invoke(state, "refreshTable", noTypes());
		assertEquals(0, table.getRowCount());
		tabs.setActiveTab(2);
		invoke(state, "refreshTable", noTypes());
	}

	@Test
	void applyPersistenceRuleLoadAndClientMatrices() {
		state.enter();
		NullpoMinoSDL.netLobby = null;
		invoke(state, "apply", noTypes());
		NullpoMinoSDL.netLobby = lobby;

		int[] selected = (int[])get(state, "selectedIndex");
		selected[0] = 0;
		selected[1] = -1;
		selected[2] = 0;
		selected[3] = 0;
		invoke(state, "apply", noTypes());
		assertTrue(lobby.globalSaves > 0);
		assertNull(invoke(null, "loadRule", new Class<?>[] {String.class}, "missing-rule"));

		lobby.ruleEntries.clear();
		state = new StateNetRuleChangeSDL();
		state.enter();
		invoke(state, "apply", noTypes());
		lobby.ruleEntries.add(entry("a.rul", "A", "missing-a", 0));
		lobby.ruleEntries.add(entry("c.rul", "C", "missing-c", 1));
		state = new StateNetRuleChangeSDL();
		state.enter();
		TabStripSDL applyTabs = (TabStripSDL)get(state, "tabStrip");
		int[] applySelected = (int[])get(state, "selectedIndex");
		applyTabs.setActiveTab(1);
		applySelected[0] = -1;
		invoke(state, "apply", noTypes());
		state = new StateNetRuleChangeSDL();
		state.enter();
		applyTabs = (TabStripSDL)get(state, "tabStrip");
		applySelected = (int[])get(state, "selectedIndex");
		applyTabs.setActiveTab(1);
		applySelected[0] = 99;
		invoke(state, "apply", noTypes());

		String[] files = lobby.getRuleFileList();
		assertNotNull(files);
		if(files.length > 0) {
			lobby.createRuleEntries(files);
			RuleEntry first = lobby.ruleEntries.getFirst();
			RuleOptions loaded = (RuleOptions)invoke(null, "loadRule",
					new Class<?>[] {String.class}, first.filepath);
			assertNotNull(loaded);
			state = new StateNetRuleChangeSDL();
			state.enter();
			invoke(state, "apply", noTypes());
		}

		lobby.netPlayerClient = new FakeClient(false);
		state = new StateNetRuleChangeSDL();
		state.enter();
		invoke(state, "apply", noTypes());
		lobby.netPlayerClient = new FakeClient(true);
		state = new StateNetRuleChangeSDL();
		state.enter();
		invoke(state, "apply", noTypes());
		assertTrue(lobby.uploads > 0);
	}

	@Test
	void updateMouseTabKeysFocusAndActivationMatrices() {
		state.enter();
		state.update();
		assertTrue(lobby.pumps > 0);
		NullpoMinoSDL.netLobby = null;
		state.update();
		NullpoMinoSDL.netLobby = lobby;

		state = enteredState();
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X1MASK);
		state.update();
		state = enteredState();
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X2MASK);
		state.update();

		state = enteredState();
		clickForUpdate(170, 50);
		state = enteredState();
		clickForUpdate(330, 50);
		clickForUpdate(20, 100);
		clickForUpdate(20, 100);

		state = enteredState();
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_F1, 0, true));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_F1, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_PAGEUP, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_PAGEDOWN, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_TAB, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_TAB, SDLConstants.SDL_KMOD_SHIFT, false));
		state.update();
		NullpoMinoSDL.frameKeyEvents.clear();

		TabStripSDL tabs = (TabStripSDL)get(state, "tabStrip");
		tabs.setActiveTab(0);
		key(SDLConstants.SDL_SCANCODE_PAGEUP, 0, false);
		tabs.setActiveTab(2);
		key(SDLConstants.SDL_SCANCODE_PAGEUP, 0, false);
		tabs.setActiveTab(GameEngine.GAMESTYLE_NAMES.length - 1);
		key(SDLConstants.SDL_SCANCODE_PAGEDOWN, 0, false);
		tabs.setActiveTab(0);
		key(SDLConstants.SDL_SCANCODE_PAGEDOWN, 0, false);
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, new Object[] {null});
		key(SDLConstants.SDL_SCANCODE_F1, 0, false);
		key(SDLConstants.SDL_SCANCODE_ESCAPE, 0, true);
		key(SDLConstants.SDL_SCANCODE_ESCAPE, 0, false);
	}

	@Test
	void nextFocusAndRenderMatrices() {
		state.enter();
		WidgetSDL table = (WidgetSDL)get(state, "ruleTable");
		WidgetSDL ok = (WidgetSDL)get(state, "okBtn");
		WidgetSDL cancel = (WidgetSDL)get(state, "cancelBtn");
		assertNotNull(invoke(state, "nextFocus", new Class<?>[] {WidgetSDL.class, boolean.class}, table, false));
		assertNotNull(invoke(state, "nextFocus", new Class<?>[] {WidgetSDL.class, boolean.class}, table, true));
		assertNotNull(invoke(state, "nextFocus", new Class<?>[] {WidgetSDL.class, boolean.class}, cancel, false));
		assertNotNull(invoke(state, "nextFocus", new Class<?>[] {WidgetSDL.class, boolean.class}, ok, true));
		assertNotNull(invoke(state, "nextFocus", new Class<?>[] {WidgetSDL.class, boolean.class}, new Object[] {null, false}));

		state.render();
		NullpoMinoSDL.netLobby = null;
		state.render();
	}

	private StateNetRuleChangeSDL enteredState() {
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.netLobby = lobby;
		StateNetRuleChangeSDL result = new StateNetRuleChangeSDL();
		result.enter();
		return result;
	}

	private void key(int code, int mod, boolean repeat) {
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(code, mod, repeat));
		state.update();
		NullpoMinoSDL.frameKeyEvents.clear();
	}

	private void clickForUpdate(int x, int y) {
		setMouse(x, y, 0);
		state.update();
		setMouse(x, y, SDLConstants.SDL_BUTTON_LMASK);
		state.update();
		setMouse(x, y, 0);
		state.update();
	}

	private static RuleEntry entry(String filename, String name, String path, int style) {
		RuleEntry result = new RuleEntry();
		result.filename = filename;
		result.rulename = name;
		result.filepath = path;
		result.style = style;
		return result;
	}

	private static Class<?>[] noTypes() { return new Class<?>[0]; }

	private static Object invoke(Object target, String name, Class<?>[] types, Object... args) {
		try {
			Method method = StateNetRuleChangeSDL.class.getDeclaredMethod(name, types);
			method.setAccessible(true);
			return method.invoke(target, args);
		} catch(Exception e) {
			throw new AssertionError(name, e);
		}
	}

	private static Object get(Object target, String name) {
		try {
			Field field = StateNetRuleChangeSDL.class.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(target);
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
		int globalSaves;
		int uploads;
		@Override public void pump() { pumps++; }
		@Override public void saveGlobalConfig() { globalSaves++; }
		@Override public void sendMyRuleDataToServer() { uploads++; }
	}

	private static final class FakeClient extends NetPlayerClient {
		private final boolean connected;
		FakeClient(boolean connected) { this.connected = connected; }
		@Override public boolean isConnected() { return connected; }
	}

	private static final class NoopState extends BaseStateSDL {
		@Override public void enter() {}
		@Override public void leave() {}
	}
}

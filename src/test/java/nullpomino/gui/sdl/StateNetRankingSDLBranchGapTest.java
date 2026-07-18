package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.game.net.NetPlayerClient;
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
import nullpomino.gui.sdl.widget.TabStripSDL;
import nullpomino.gui.sdl.widget.TableSDL;
import nullpomino.gui.sdl.widget.WidgetSDL;
import nullpomino.util.CustomProperties;

/** Headless enter/request/populate/update/render branch matrix for the MP ranking screen. */
class StateNetRankingSDLBranchGapTest {
	private static final SdlRenderer RENDERER = new SdlRenderer() {};
	private static final SdlTexture TEXTURE = new SdlTexture() {};
	private static final String[][] TWO_ROWS = {
		{"1", "alice", "1600", "10", "5"},
		{"2", "bob", "1500", "8", "3"},
	};
	private static int mouseX;
	private static int mouseY;
	private static int mouseButtons;
	private static int renderTextureCalls;

	private TestLobby lobby;
	private StateNetRankingSDL state;

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
		NullpoMinoSDL.mouseWheelDelta = 0;
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.gameStates = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < NullpoMinoSDL.gameStates.length; i++) {
			NullpoMinoSDL.gameStates[i] = new NoopState();
		}
		NullpoMinoSDL.currentState = -1;
		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_TITLE);

		MouseInputSDL.initalizeMouseInput();
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();

		ResourceHolderSDL.ttfFont = null;
		ResourceHolderSDL.imgFont = TEXTURE;
		ResourceHolderSDL.imgFontSmall = TEXTURE;
		ResourceHolderSDL.imgFontBig = TEXTURE;
		ResourceHolderSDL.imgMenu = TEXTURE;

		lobby = new TestLobby();
		NullpoMinoSDL.netLobby = lobby;
		state = new StateNetRankingSDL();
		renderTextureCalls = 0;
	}

	@Test
	void enterWithoutLobbyBailsToTitle() {
		NullpoMinoSDL.netLobby = null;
		state.enter();
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
	}

	@Test
	void enterRequestsStyleAndPopulatesSelection() {
		lobby.mpRankingRows[0] = TWO_ROWS;
		lobby.mpRankingMyRank[0] = 1;
		state.enter();
		assertEquals(0, get(state, "lastRequestedStyle"));
		assertEquals(1, lobby.injected.size());
		assertTrue(lobby.injected.get(0).startsWith("mpranking\t0\t"));
		TableSDL table = table();
		assertEquals(2, table.getRowCount());
		assertEquals(1, table.getSelectedIndex());
		assertSame(table, get(state, "focused"));

		state.leave();
		assertNull(get(state, "focused"));
	}

	@Test
	void setFocusHandlesSameNullAndSwitch() {
		state.enter();
		WidgetSDL table = table();
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, table);
		assertSame(table, get(state, "focused"));
		WidgetSDL back = (WidgetSDL)get(state, "backBtn");
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, back);
		assertSame(back, get(state, "focused"));
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, new Object[] {null});
		assertNull(get(state, "focused"));
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, table);
		assertSame(table, get(state, "focused"));
	}

	@Test
	void requestStyleCoversDedupNullLobbyAndClientStates() {
		state.enter();
		assertEquals(1, lobby.injected.size());

		invoke(state, "requestStyle", new Class<?>[] {int.class}, 0);
		assertEquals(1, lobby.injected.size());

		NullpoMinoSDL.netLobby = null;
		invoke(state, "requestStyle", new Class<?>[] {int.class}, 1);
		assertEquals(0, get(state, "lastRequestedStyle"));

		NullpoMinoSDL.netLobby = lobby;
		FakeClient client = new FakeClient();
		lobby.netPlayerClient = client;
		invoke(state, "requestStyle", new Class<?>[] {int.class}, 1);
		assertEquals(1, client.sent.size());
		assertEquals("mpranking\t1\n", client.sent.get(0));

		client.connected = false;
		invoke(state, "requestStyle", new Class<?>[] {int.class}, 2);
		assertEquals(2, lobby.injected.size());

		lobby.netPlayerClient = null;
		invoke(state, "requestStyle", new Class<?>[] {int.class}, 3);
		assertEquals(3, lobby.injected.size());
		assertEquals(3, get(state, "lastRequestedStyle"));
	}

	@Test
	void populateCoversNullLobbyRowShapesAndRankSelection() {
		state.enter();
		TableSDL table = table();
		assertEquals(0, table.getRowCount());

		table.addRow(new String[] {"stale"});
		NullpoMinoSDL.netLobby = null;
		invoke(state, "populateFromSession", new Class<?>[0]);
		assertEquals(1, table.getRowCount());
		NullpoMinoSDL.netLobby = lobby;

		lobby.mpRankingRows[0] = new String[0][];
		lobby.mpRankingMyRank[0] = -1;
		invoke(state, "populateFromSession", new Class<?>[0]);
		assertEquals(0, table.getRowCount());
		assertEquals(-1, table.getSelectedIndex());

		lobby.mpRankingRows[0] = TWO_ROWS;
		lobby.mpRankingMyRank[0] = 5;
		invoke(state, "populateFromSession", new Class<?>[0]);
		assertEquals(1, table.getSelectedIndex());

		lobby.mpRankingMyRank[0] = 0;
		invoke(state, "populateFromSession", new Class<?>[0]);
		assertEquals(0, table.getSelectedIndex());

		forceTab(-1);
		invoke(state, "populateFromSession", new Class<?>[0]);
		assertEquals(0, table.getRowCount());
		forceTab(999);
		invoke(state, "populateFromSession", new Class<?>[0]);
		assertEquals(0, table.getRowCount());
	}

	@Test
	void updateCoversNullLobbyDirtyAndMouseNavigation() {
		NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_NET_RANKING] = state;
		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_NET_RANKING);
		assertEquals(NullpoMinoSDL.STATE_NET_RANKING, NullpoMinoSDL.currentState);

		lobby.mpRankingRows[0] = TWO_ROWS;
		lobby.mpRankingMyRank[0] = 0;
		lobby.mpRankingDirty = true;
		state.update();
		assertTrue(lobby.pumps > 0);
		assertEquals(false, lobby.mpRankingDirty);
		assertEquals(2, table().getRowCount());
		state.update();
		assertEquals(2, table().getRowCount());

		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X1MASK);
		state.update();
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
		setMouse(-1, -1, 0);
		state.update();
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X2MASK);
		state.update();
		assertEquals(NullpoMinoSDL.STATE_NET_RANKING, NullpoMinoSDL.currentState);
		setMouse(-1, -1, 0);
		state.update();

		NullpoMinoSDL.netLobby = null;
		state.update();
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
	}

	@Test
	void updateCoversTableClickThenTabClick() {
		lobby.mpRankingRows[0] = TWO_ROWS;
		lobby.mpRankingMyRank[0] = 0;
		state.enter();
		assertEquals(1, lobby.injected.size());

		// Click row 0 of the table (rows start at y=96) with focus elsewhere.
		invoke(state, "setFocus", new Class<?>[] {WidgetSDL.class}, get(state, "backBtn"));
		click(20, 100);
		assertSame(table(), get(state, "focused"));
		assertEquals(0, table().getSelectedIndex());

		// Click the second tab header: switches style, requests it, repopulates.
		click(200, 50);
		assertEquals(1, tabStrip().getActiveTab());
		assertEquals(2, lobby.injected.size());
		assertTrue(lobby.injected.get(1).startsWith("mpranking\t1\t"));
		assertEquals(0, table().getRowCount());
	}

	@Test
	void updateCoversEscapeKeyBranches() {
		NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_NET_RANKING] = state;
		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_NET_RANKING);

		key(SDLConstants.SDL_SCANCODE_ESCAPE, true);
		state.update();
		assertEquals(NullpoMinoSDL.STATE_NET_RANKING, NullpoMinoSDL.currentState);

		key(SDLConstants.SDL_SCANCODE_ESCAPE, false);
		state.update();
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
	}

	@Test
	void updateCoversTabCycleKeysAndWrapping() {
		state.enter();
		assertEquals(0, tabStrip().getActiveTab());

		key(SDLConstants.SDL_SCANCODE_LEFT, false);
		state.update();
		assertEquals(3, tabStrip().getActiveTab());

		key(SDLConstants.SDL_SCANCODE_PAGEUP, false);
		state.update();
		assertEquals(2, tabStrip().getActiveTab());

		key(SDLConstants.SDL_SCANCODE_RIGHT, false);
		state.update();
		assertEquals(3, tabStrip().getActiveTab());

		key(SDLConstants.SDL_SCANCODE_PAGEDOWN, false);
		state.update();
		assertEquals(0, tabStrip().getActiveTab());

		key(SDLConstants.SDL_SCANCODE_RIGHT, false);
		state.update();
		assertEquals(1, tabStrip().getActiveTab());

		// Repeat events skip tab cycling and land in focused.handleKey.
		key(SDLConstants.SDL_SCANCODE_LEFT, true);
		key(SDLConstants.SDL_SCANCODE_RIGHT, true);
		state.update();
		assertEquals(1, tabStrip().getActiveTab());
	}

	@Test
	void updateCoversTabFocusToggleAndFocusedKeyDelivery() {
		lobby.mpRankingRows[0] = TWO_ROWS;
		lobby.mpRankingMyRank[0] = 1;
		state.enter();
		assertSame(table(), get(state, "focused"));

		key(SDLConstants.SDL_SCANCODE_TAB, false);
		state.update();
		assertSame(get(state, "backBtn"), get(state, "focused"));

		key(SDLConstants.SDL_SCANCODE_TAB, false);
		state.update();
		assertSame(table(), get(state, "focused"));

		key(SDLConstants.SDL_SCANCODE_TAB, true);
		state.update();
		assertSame(table(), get(state, "focused"));

		// Non-tab key reaches the focused table's handleKey (UP moves selection).
		assertEquals(1, table().getSelectedIndex());
		key(SDLConstants.SDL_SCANCODE_UP, false);
		state.update();
		assertEquals(0, table().getSelectedIndex());

		set(state, "focused", null);
		key(SDLConstants.SDL_SCANCODE_UP, false);
		state.update();
		assertEquals(0, table().getSelectedIndex());
	}

	@Test
	void renderCoversNullLobbyLoadingAndStyleRangeGuards() {
		// nl == null early-return guard: nothing is drawn before a lobby exists
		NullpoMinoSDL.netLobby = null;
		state.render();
		assertEquals(0, renderTextureCalls);
		NullpoMinoSDL.netLobby = lobby;

		state.enter();
		int before = renderTextureCalls;
		state.render();
		assertTrue(renderTextureCalls > before);

		lobby.mpRankingRows[0] = TWO_ROWS;
		before = renderTextureCalls;
		state.render();
		assertTrue(renderTextureCalls > before);

		forceTab(-1);
		before = renderTextureCalls;
		state.render();
		assertTrue(renderTextureCalls > before);

		forceTab(999);
		before = renderTextureCalls;
		state.render();
		assertTrue(renderTextureCalls > before);
	}

	/** Replaces the tab strip with one reporting an out-of-range active tab. */
	private void forceTab(final int forced) {
		set(state, "tabStrip", new TabStripSDL(8, 40, 624, 28, new String[] {"X"}) {
			@Override public int getActiveTab() { return forced; }
			@Override public void render() {}
		});
	}

	private TableSDL table() { return (TableSDL)get(state, "rankingTable"); }
	private TabStripSDL tabStrip() { return (TabStripSDL)get(state, "tabStrip"); }

	private void key(int scancode, boolean repeat) {
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(scancode, 0, repeat));
	}

	private void click(int x, int y) {
		NullpoMinoSDL.frameKeyEvents.clear();
		setMouse(x, y, 0);
		state.update();
		setMouse(x, y, SDLConstants.SDL_BUTTON_LMASK);
		state.update();
		setMouse(x, y, 0);
		state.update();
	}

	private static void setMouse(int x, int y, int buttons) {
		mouseX = x;
		mouseY = y;
		mouseButtons = buttons;
	}

	private static Object invoke(Object target, String name, Class<?>[] types, Object... args) {
		try {
			Method method = StateNetRankingSDL.class.getDeclaredMethod(name, types);
			method.setAccessible(true);
			return method.invoke(target, args);
		} catch(Exception e) {
			throw new AssertionError(name, e);
		}
	}

	private static Object get(Object target, String name) {
		try {
			Field field = StateNetRankingSDL.class.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(target);
		} catch(Exception e) {
			throw new AssertionError(name, e);
		}
	}

	private static void set(Object target, String name, Object value) {
		try {
			Field field = StateNetRankingSDL.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch(Exception e) {
			throw new AssertionError(name, e);
		}
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
					if(method.getName().equals("SDL_RenderTexture")) {
						renderTextureCalls++;
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
		final List<String> injected = new ArrayList<String>();
		@Override public void pump() { pumps++; }
		@Override public void injectLocalMessage(String line) { injected.add(line); }
	}

	private static final class FakeClient extends NetPlayerClient {
		boolean connected = true;
		final List<String> sent = new ArrayList<String>();
		@Override public boolean isConnected() { return connected; }
		@Override public boolean send(String msg) { sent.add(msg); return true; }
	}

	private static final class NoopState extends BaseStateSDL {
		@Override public void enter() {}
		@Override public void leave() {}
	}
}

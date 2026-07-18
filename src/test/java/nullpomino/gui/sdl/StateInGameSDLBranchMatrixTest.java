package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.game.component.Controller;
import nullpomino.game.component.ReplayData;
import nullpomino.game.mode.GameMode;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.sdl.binding.Ref.FloatRef;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SdlBackend;
import nullpomino.gui.sdl.binding.SdlHandles.MixTrack;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlTexture;
import nullpomino.util.CustomProperties;
import nullpomino.util.ModeManager;

/** Headless state, input, mouse, and replay-timeline branch matrix. */
class StateInGameSDLBranchMatrixTest {
	private static final SdlRenderer RENDERER = new SdlRenderer() {};
	private static final SdlTexture TEXTURE = new SdlTexture() {};
	private static int mouseX;
	private static int mouseY;
	private static int mouseButtons;

	private Harness state;
	private RecordingManager manager;

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
		NullpoMinoSDL.keyPressedState = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		NullpoMinoSDL.joystickMax = 0;
		NullpoMinoSDL.joyUseNumber = new int[] {-1, -1};
		NullpoMinoSDL.mouseWheelDelta = 0;
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.webMode = true;
		NullpoMinoSDL.gameStates = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < NullpoMinoSDL.gameStates.length; i++) NullpoMinoSDL.gameStates[i] = new NoopState();
		NullpoMinoSDL.currentState = -1;

		GameKeySDL.initGlobalGameKeySDL();
		for(GameKeySDL key : GameKeySDL.gamekey) key.loadDefaultKeymap(0);
		MouseInputSDL.initalizeMouseInput();
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();

		ResourceHolderSDL.soundManager = new SoundManagerSDL();
		ResourceHolderSDL.imgFont = TEXTURE;
		ResourceHolderSDL.imgFontSmall = TEXTURE;
		ResourceHolderSDL.imgFontBig = TEXTURE;
		ResourceHolderSDL.imgSprite = TEXTURE;
		ResourceHolderSDL.bgmPlaying = -1;
		ResourceHolderSDL.bgmTrack = null;
		NullpoMinoSDL.mixerLib = null;
		NullpoMinoSDL.mixer = null;

		state = new Harness();
		state.enter();
		manager = manager(2);
		state.gameManager = manager;
	}

	@Test
	void startLifecycleTitleAndRenderMatrices() {
		TestMode mode = new TestMode(2, 0);
		NullpoMinoSDL.modeManager.addMode(mode);
		NullpoMinoSDL.propGlobal.setProperty("name.mode", mode.getName());
		state.startNewGame();
		assertTrue(state.gameManager.engine.length == 2);
		state.startNewGame("");
		state.startNewGame("missing-rule-file");

		NullpoMinoSDL.propGlobal.setProperty("0.ruleopt.strRandomizer", "missing.Randomizer");
		NullpoMinoSDL.propGlobal.setProperty("0.ruleopt.strWallkick", "missing.Wallkick");
		NullpoMinoSDL.propGlobal.setProperty("0.ai", "missing.AI");
		NullpoMinoSDL.webMode = false;
		state.startNewGame("");
		NullpoMinoSDL.propGlobal.setProperty("0.aiUseThread", false);
		state.startNewGame("");
		NullpoMinoSDL.propGlobal.setProperty("0.aiUseThread", true);
		NullpoMinoSDL.webMode = true;
		state.startNewGame("");

		NullpoMinoSDL.modeManager = new ModeManager();
		TestMode styledMode = new TestMode(1, 1);
		NullpoMinoSDL.modeManager.addMode(styledMode);
		NullpoMinoSDL.propGlobal.setProperty("name.mode", styledMode.getName());
		state.startNewGame();

		CustomProperties replay = new CustomProperties();
		replay.setProperty("name.mode", mode.getName());
		replay.setProperty("0.ruleopt.strRandomizer", "missing.Randomizer");
		replay.setProperty("0.ruleopt.strWallkick", "missing.Wallkick");
		state.startReplayGame(replay);
		assertTrue(state.gameManager.replayMode);
		NullpoMinoSDL.propGlobal.setProperty("0.ai", "");
		state.startReplayGame(replay);
		NullpoMinoSDL.propGlobal.setProperty("0.ai", "missing.AI");
		NullpoMinoSDL.propGlobal.setProperty("0.aiUseThread", true);
		NullpoMinoSDL.webMode = false;
		state.startReplayGame(replay);
		NullpoMinoSDL.propGlobal.setProperty("0.aiUseThread", false);
		state.startReplayGame(replay);
		NullpoMinoSDL.webMode = true;

		NullpoMinoSDL.propGlobal.setProperty("name.mode", "missing");
		assertThrows(NullPointerException.class, () -> state.startNewGame());
		CustomProperties missingReplay = new CustomProperties();
		missingReplay.setProperty("name.mode", "missing");
		assertDoesNotThrow(() -> state.startReplayGame(missingReplay));

		state.gameManager = null;
		state.updateTitleBarCaption();
		state.render();
		state.gameManager = manager;
		state.modeName = "matrix";
		GameEngine engine = manager.engine[0];
		GameEngine[] engines = manager.engine;
		manager.engine = null;
		state.updateTitleBarCaption();
		manager.engine = new GameEngine[0];
		state.updateTitleBarCaption();
		state.render();
		manager.engine = new GameEngine[] {null};
		state.updateTitleBarCaption();
		state.render();
		manager.engine = engines;

		state.pause = true;
		state.enableframestep = false;
		state.updateTitleBarCaption();
		state.pause = false;
		engine.isInGame = true;
		manager.replayMode = false;
		manager.replayRerecord = false;
		state.updateTitleBarCaption();
		manager.replayMode = true;
		state.updateTitleBarCaption();
		manager.replayRerecord = true;
		state.updateTitleBarCaption();
		manager.replayMode = false;
		state.updateTitleBarCaption();
		engine.isInGame = false;
		manager.replayMode = true;
		manager.replayRerecord = true;
		state.updateTitleBarCaption();
		manager.replayRerecord = false;
		state.updateTitleBarCaption();
		manager.replayMode = false;
		state.updateTitleBarCaption();

		state.pause = true;
		state.pauseMessageHide = false;
		state.fastforward = 2;
		manager.replayMode = true;
		manager.replayRerecord = false;
		manager.replayShowInvisible = true;
		for(int cursor = 0; cursor < 4; cursor++) {
			state.cursor = cursor;
			state.render();
		}
		state.pauseMessageHide = true;
		state.render();
		state.pauseMessageHide = false;
		state.enableframestep = true;
		state.render();
		state.enableframestep = false;
		manager.replayMode = true;
		manager.replayRerecord = true;
		state.render();
		manager.replayMode = false;
		state.render();
		manager.replayMode = true;
		manager.replayRerecord = false;
		state.pause = false;
		state.fastforward = 0;
		manager.replayShowInvisible = false;

		ReplayData data = replayData(20);
		engine.replayData = data;
		engine.replayTimer = 10;
		manager.replayMode = false;
		engine.stat = GameEngine.Status.MOVE;
		state.render();
		manager.replayMode = true;
		engine.replayData = null;
		state.render();
		engine.replayData = data;
		engine.stat = GameEngine.Status.SETTING;
		state.render();
		engine.stat = GameEngine.Status.RESULT;
		state.render();
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		state.render();
		setInt(state, "settingFrames", 5);
		state.replayPaused = true;
		state.render();
		state.replayPaused = false;

		RecordingManager shutdownManager = manager(1);
		state.gameManager = shutdownManager;
		state.leave();
		assertTrue(shutdownManager.shutdowns > 0);
		state.leave();
	}

	@Test
	void pauseMenuCoreLoopAndReplayKeyMatrices() {
		RecordingManager validManager = manager;
		state.gameManager = null;
		updateNoKeys();
		state.gameManager = validManager;
		GameEngine[] validEngines = validManager.engine;
		validManager.engine = null;
		updateNoKeys();
		validManager.engine = new GameEngine[0];
		updateNoKeys();
		validManager.engine = new GameEngine[] {null};
		updateNoKeys();
		validManager.engine = validEngines;
		RecordingManager singlePlayer = manager(1);
		manager = singlePlayer;
		state.gameManager = singlePlayer;
		updateNoKeys();
		manager = validManager;
		state.gameManager = validManager;

		GameEngine engine = manager.engine[0];
		engine.gameActive = true;
		engine.isInGame = true;
		state.pauseFrame = 0;

		pulse(0, GameKeySDL.BUTTON_PAUSE);
		assertTrue(state.pause);
		releaseAndUpdate();
		state.pauseFrame = 0;
		pulse(0, GameKeySDL.BUTTON_PAUSE);
		assertFalse(state.pause);
		engine.gameActive = false;
		pulse(0, GameKeySDL.BUTTON_PAUSE);
		engine.gameActive = true;
		state.pauseFrame = 2;
		pulse(0, GameKeySDL.BUTTON_PAUSE);
		state.pauseFrame = 0;
		pulsePlayerOneOnly(GameKeySDL.BUTTON_PAUSE);
		pulsePlayerOneOnly(GameKeySDL.BUTTON_PAUSE);

		state.enableframestep = true;
		pulse(0, GameKeySDL.BUTTON_PAUSE);
		updateNoKeys();
		pulse(0, GameKeySDL.BUTTON_PAUSE);
		state.enableframestep = false;
		state.pause = true;
		state.pauseMessageHide = true;
		updateNoKeys();
		state.pauseMessageHide = false;

		state.pause = true;
		state.pauseFrame = 0;
		manager.replayMode = true;
		manager.replayRerecord = false;
		state.cursor = 0;
		pulse(0, GameKeySDL.BUTTON_UP);
		state.cursor = 3;
		pulse(0, GameKeySDL.BUTTON_DOWN);
		manager.replayRerecord = true;
		state.cursor = 3;
		pulse(0, GameKeySDL.BUTTON_DOWN);
		manager.replayRerecord = false;
		manager.replayMode = false;
		state.cursor = 0;
		pulse(0, GameKeySDL.BUTTON_UP);
		state.pause = true;
		state.cursor = 1;
		pulse(0, GameKeySDL.BUTTON_UP);
		state.pause = true;
		manager.replayMode = true;
		manager.replayRerecord = true;
		state.cursor = 0;
		pulse(0, GameKeySDL.BUTTON_UP);
		manager.replayRerecord = false;
		state.cursor = 2;
		pulse(0, GameKeySDL.BUTTON_DOWN);
		state.pause = true;
		manager.replayMode = false;
		state.cursor = 0;
		pulse(0, GameKeySDL.BUTTON_DOWN);

		for(int cursor = 0; cursor < 4; cursor++) {
			manager = manager(2);
			state.gameManager = manager;
			state.pause = true;
			state.pauseFrame = 0;
			state.cursor = cursor;
			manager.replayMode = cursor == 3;
			pulse(0, GameKeySDL.BUTTON_A);
		}

		manager = manager(2);
		state.gameManager = manager;
		state.pause = true;
		state.pauseFrame = 0;
		pulse(0, GameKeySDL.BUTTON_B);
		state.pause = true;
		state.pauseFrame = 2;
		pulse(0, GameKeySDL.BUTTON_B);
		state.pauseFrame = 0;
		state.pause = true;
		pulse(0, GameKeySDL.BUTTON_C);

		engine = manager.engine[0];
		state.pause = false;
		manager.replayMode = true;
		manager.replayRerecord = false;
		engine.gameActive = true;
		engine.isInGame = true;
		pulse(0, GameKeySDL.BUTTON_LEFT);
		pulse(0, GameKeySDL.BUTTON_RIGHT);
		pulse(0, GameKeySDL.BUTTON_E);
		pulse(0, GameKeySDL.BUTTON_E);
		manager.replayRerecord = false;
		pulse(0, GameKeySDL.BUTTON_D);
		manager.replayMode = false;
		state.fastforward = 7;
		updateNoKeys();
		manager.replayMode = true;
		manager.replayRerecord = true;
		state.fastforward = 7;
		updateNoKeys();
		ResourceHolderSDL.bgmPlaying = 99;
		updateNoKeys();

		NullpoMinoSDL.mixerLib = mixerStub();
		ResourceHolderSDL.bgmTrack = new MixTrack() {};
		ResourceHolderSDL.bgmPlaying = manager.bgmStatus.bgm;
		manager.bgmStatus.volume = -1f;
		updateNoKeys();
		ResourceHolderSDL.bgmTrack = new MixTrack() {};
		manager.bgmStatus.volume = 2f;
		updateNoKeys();
		manager.bgmStatus.volume = 0.5f;
		updateNoKeys();
		NullpoMinoSDL.mixerLib = null;
		ResourceHolderSDL.bgmTrack = null;

		NullpoMinoSDL.joystickMax = 1;
		NullpoMinoSDL.joyUseNumber[0] = 0;
		NullpoMinoSDL.joyPressedState = new boolean[][] {new boolean[32]};
		NullpoMinoSDL.joyAxisX = new int[] {0};
		NullpoMinoSDL.joyAxisY = new int[] {0};
		NullpoMinoSDL.joyHatState = new int[] {0};
		updateNoKeys();
		NullpoMinoSDL.joyUseNumber[0] = 2;
		updateNoKeys();

		manager.engine[0].stat = GameEngine.Status.RESULT;
		manager.engine[1].stat = GameEngine.Status.MOVE;
		updateNoKeys();
		manager.engine[1].stat = GameEngine.Status.RESULT;
		updateNoKeys();
		assertTrue(manager.updates > 0);
	}

	@Test
	void pauseResultAndExitMouseMatrices() {
		GameEngine engine = manager.engine[0];
		engine.gameActive = true;
		state.pause = true;
		state.pauseFrame = 0;
		manager.replayMode = true;
		manager.replayRerecord = false;
		int offsetX = manager.receiver.getFieldDisplayPositionX(engine, 0);
		int offsetY = manager.receiver.getFieldDisplayPositionY(engine, 0);
		int menuX = offsetX + 20;
		int menuY = offsetY + 188;

		clearInput();
		NullpoMinoSDL.mouseWheelDelta = 1;
		state.update();
		NullpoMinoSDL.mouseWheelDelta = 0;
		state.cursor = 0;
		moveMouse(menuX, menuY);
		state.update();
		int[][] outsideMenu = {
				{menuX - 20, menuY}, {menuX + 200, menuY},
				{menuX, menuY - 20}, {menuX, menuY + 100}
		};
		for(int[] point : outsideMenu) {
			moveMouse(point[0], point[1]);
			state.update();
			clickMouse(point[0], point[1]);
			state.update();
		}
		state.cursor = 0;
		clickMouse(menuX, menuY);
		state.update();
		state.pause = true;
		state.pauseFrame = 0;

		moveMouse(menuX, menuY + 16);
		state.cursor = 0;
		state.update();
		assertTrue(state.cursor == 1);
		moveMouse(menuX, menuY + 16);
		state.cursor = 1;
		state.update();
		assertTrue(state.cursor == 1);

		clickMouse(menuX, menuY + 16);
		state.cursor = 1;
		state.update();
		manager = manager(2);
		state.gameManager = manager;
		state.pause = true;
		state.pauseFrame = 0;
		engine = manager.engine[0];
		menuX = manager.receiver.getFieldDisplayPositionX(engine, 0) + 20;
		menuY = manager.receiver.getFieldDisplayPositionY(engine, 0) + 188;
		state.cursor = 0;
		clickMouse(menuX, menuY + 16);
		state.update();

		manager = manager(2);
		state.gameManager = manager;
		state.pause = true;
		state.pauseFrame = 0;
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X1MASK);
		state.update();

		manager = manager(2);
		state.gameManager = manager;
		state.pause = true;
		state.pauseFrame = 0;
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_RMASK);
		state.update();

		manager = manager(2);
		state.gameManager = manager;
		state.pause = true;
		state.pauseFrame = 0;
		clickMouse(550, 10);
		state.update();

		manager = manager(2);
		state.gameManager = manager;
		state.pause = false;
		engine = manager.engine[0];
		engine.stat = GameEngine.Status.RESULT;
		offsetX = manager.receiver.getFieldDisplayPositionX(engine, 0);
		offsetY = manager.receiver.getFieldDisplayPositionY(engine, 0);
		moveMouse(offsetX + 20, offsetY + 340);
		state.update();
		clickMouse(offsetX + 20, offsetY + 340);
		state.update();

		manager = manager(2);
		state.gameManager = manager;
		engine = manager.engine[0];
		engine.stat = GameEngine.Status.RESULT;
		offsetX = manager.receiver.getFieldDisplayPositionX(engine, 0);
		offsetY = manager.receiver.getFieldDisplayPositionY(engine, 0);
		clickMouse(550, 10);
		state.update();

		manager = manager(2);
		state.gameManager = manager;
		engine = manager.engine[0];
		engine.stat = GameEngine.Status.RESULT;
		offsetX = manager.receiver.getFieldDisplayPositionX(engine, 0);
		offsetY = manager.receiver.getFieldDisplayPositionY(engine, 0);
		clickMouse(offsetX + 120, offsetY + 340);
		state.update();

		manager = manager(2);
		state.gameManager = manager;
		pulse(0, GameKeySDL.BUTTON_RETRY);
		manager = manager(2);
		state.gameManager = manager;
		pulsePlayerOneOnly(GameKeySDL.BUTTON_RETRY);
		manager.engine[0].quitflag = true;
		manager.engine[0].stat = GameEngine.Status.RESULT;
		updateNoKeys();

		manager = manager(2);
		state.gameManager = manager;
		manager.engine[0].quitflag = true;
		manager.engine[0].stat = GameEngine.Status.MOVE;
		updateNoKeys();

		manager = manager(2);
		state.gameManager = manager;
		pulsePlayerOneOnly(GameKeySDL.BUTTON_GIVEUP);
	}

	@Test
	void replayUpdateGatingAndTimelineExitMatrices() {
		GameEngine engine = manager.engine[0];
		manager.replayMode = true;
		manager.replayRerecord = false;
		engine.replayData = replayData(30);
		engine.gameActive = false;
		engine.stat = GameEngine.Status.SETTING;
		updateNoKeys();
		engine.stat = GameEngine.Status.SETTING;
		updateNoKeys();
		setInt(state, "settingFrames", 100);
		engine.stat = GameEngine.Status.SETTING;
		updateNoKeys();

		GameMode mode = manager.mode;
		manager.mode = null;
		engine.stat = GameEngine.Status.SETTING;
		updateNoKeys();
		manager.mode = mode;

		state.pause = true;
		state.replayPaused = true;
		setBoolean(state, "scrubbing", true);
		updateNoKeys();
		state.enableframestep = true;
		int steppedUpdates = manager.updates;
		pulse(0, GameKeySDL.BUTTON_FRAMESTEP);
		assertTrue(manager.updates > steppedUpdates);
		state.enableframestep = false;
		state.pause = false;
		state.replayPaused = false;
		setBoolean(state, "scrubbing", false);

		manager.replayMode = false;
		updateNoKeys();
		manager.replayMode = true;
		manager.replayRerecord = true;
		updateNoKeys();
		manager.replayRerecord = false;
		engine.gameActive = false;
		updateNoKeys();
		engine.gameActive = true;
		engine.stat = GameEngine.Status.MOVE;
		updateNoKeys();

		engine.stat = GameEngine.Status.RESULT;
		updateNoKeys();
		engine.stat = GameEngine.Status.SETTING;
		state.replayPaused = true;
		updateNoKeys();
		clickMouse(550, 10);
		state.update();
		engine.stat = GameEngine.Status.SETTING;
		state.replayPaused = true;
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X1MASK);
		state.update();
		state.replayPaused = false;
		setBoolean(state, "scrubbing", true);
		updateNoKeys();
		setBoolean(state, "scrubbing", false);

		engine.stat = GameEngine.Status.MOVE;
		clickMouse(550, 10);
		state.update();

		manager = manager(2);
		state.gameManager = manager;
		manager.replayMode = true;
		manager.engine[0].stat = GameEngine.Status.MOVE;
		manager.engine[0].gameActive = true;
		manager.engine[0].replayData = replayData(20);
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X1MASK);
		state.update();

		manager = manager(2);
		state.gameManager = manager;
		manager.engine[0].stat = GameEngine.Status.RESULT;
		pulse(0, GameKeySDL.BUTTON_B);
		manager = manager(2);
		state.gameManager = manager;
		manager.engine[0].stat = GameEngine.Status.RESULT;
		clearInput();
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(
				SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));
		state.update();
		for(int mask : new int[] {SDLConstants.SDL_BUTTON_X1MASK, SDLConstants.SDL_BUTTON_RMASK}) {
			manager = manager(2);
			state.gameManager = manager;
			manager.engine[0].stat = GameEngine.Status.RESULT;
			setMouse(-1, -1, 0);
			MouseInputSDL.mouseInput.update();
			setMouse(-1, -1, mask);
			state.update();
		}

		manager = manager(2);
		state.gameManager = manager;
		pulse(0, GameKeySDL.BUTTON_GIVEUP);
	}

	@Test
	void settingResultMouseAndTimelineMatrices() {
		GameEngine engine = manager.engine[0];
		TestMode mode = (TestMode)manager.mode;
		engine.stat = GameEngine.Status.SETTING;
		manager.menuOnly = false;
		manager.replayMode = false;
		int settingY = manager.receiver.getFieldDisplayPositionY(engine, 0) + 52;

		setMouse(10, settingY, 0);
		NullpoMinoSDL.mouseWheelDelta = 1;
		assertFalse((Boolean)invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine));
		setMouse(10, settingY + 16, 0);
		NullpoMinoSDL.mouseWheelDelta = -1;
		invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine);
		assertTrue(mode.cursor >= 0);
		mode.cursor = -1;
		setMouse(10, settingY + 32, 0);
		invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine);
		mode.cursor = 0;
		setMouse(10, settingY - 1, 0);
		invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine);
		engine.displaysize = -1;
		setMouse(10, settingY + 4, 0);
		invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine);
		engine.displaysize = 0;

		setMouse(10, settingY, SDLConstants.SDL_BUTTON_LMASK);
		NullpoMinoSDL.mouseWheelDelta = 0;
		invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine);
		assertTrue(engine.ctrl.buttonPress[Controller.BUTTON_A]);
		engine.ctrl.reset();
		manager.menuOnly = true;
		setMouse(10, 16, 0);
		invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine);

		manager.replayMode = true;
		manager.replayRerecord = false;
		engine.replayData = replayData(30);
		setMouse(100, 460, 0);
		NullpoMinoSDL.mouseWheelDelta = 1;
		invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine);
		clickMouse(100, 460);
		invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine);
		NullpoMinoSDL.mouseWheelDelta = 0;
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X1MASK);
		assertTrue((Boolean)invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine));
		manager.replayRerecord = true;
		assertFalse((Boolean)invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine));
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_RMASK);
		invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine);
		manager.replayMode = false;
		clickMouse(550, 10);
		invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine);
		setMouse(-1, -1, 0);
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(
				SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));
		invoke(state, "injectSettingMouseInput", new Class<?>[] {GameEngine.class}, engine);
		NullpoMinoSDL.frameKeyEvents.clear();

		engine.stat = GameEngine.Status.RESULT;
		manager.replayMode = false;
		int resultX = manager.receiver.getFieldDisplayPositionX(engine, 0);
		int resultY = manager.receiver.getFieldDisplayPositionY(engine, 0);
		setMouse(resultX + 13, resultY + 340, 0);
		MouseInputSDL.mouseInput.update();
		invoke(state, "handleResultMouse", new Class<?>[] {GameEngine.class, int.class}, engine, 0);
		setMouse(resultX + 13, resultY + 340, SDLConstants.SDL_BUTTON_LMASK);
		MouseInputSDL.mouseInput.update();
		invoke(state, "handleResultMouse", new Class<?>[] {GameEngine.class, int.class}, engine, 0);
		setMouse(resultX + 109, resultY + 340, 0);
		MouseInputSDL.mouseInput.update();
		invoke(state, "handleResultMouse", new Class<?>[] {GameEngine.class, int.class}, engine, 0);
		setMouse(resultX + 109, resultY + 340, SDLConstants.SDL_BUTTON_LMASK);
		MouseInputSDL.mouseInput.update();
		invoke(state, "handleResultMouse", new Class<?>[] {GameEngine.class, int.class}, engine, 0);
		int[][] outsideResult = {
				{resultX, resultY + 356},
				{resultX + 11, resultY + 340},
				{resultX + 100, resultY + 340},
				{resultX + 156, resultY + 340}
		};
		for(int[] point : outsideResult) {
			setMouse(point[0], point[1], 0);
			MouseInputSDL.mouseInput.update();
			invoke(state, "handleResultMouse", new Class<?>[] {GameEngine.class, int.class}, engine, 0);
		}
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		invoke(state, "handleResultMouse", new Class<?>[] {GameEngine.class, int.class}, engine, 0);

		manager.replayMode = true;
		manager.replayRerecord = false;
		engine.quitflag = false;
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.replayTimer = 5;
		engine.replayData = replayData(30);
		setInt(state, "settingFrames", 5);
		invoke(state, "layoutReplayTimeline", new Class<?>[0]);
		GameEngine savedEngine = manager.engine[0];
		manager.engine[0] = null;
		invoke(state, "replayTotalFrames", new Class<?>[0]);
		manager.engine[0] = savedEngine;
		ReplayData noInputs = new ReplayData();
		noInputs.inputDataArray = null;
		engine.replayData = noInputs;
		invoke(state, "replayTotalFrames", new Class<?>[0]);
		engine.replayData = replayData(30);
		setInt(state, "barW", 0);
		invoke(state, "pxOf", new Class<?>[] {int.class, int.class}, 1, 1);
		invoke(state, "layoutReplayTimeline", new Class<?>[0]);
		invoke(state, "pxOf", new Class<?>[] {int.class, int.class}, 1, 0);

		state.replayPaused = false;
		setMouse(10, 460, SDLConstants.SDL_BUTTON_LMASK);
		MouseInputSDL.mouseInput.update();
		assertTrue((Boolean)invoke(state, "updateReplayTimeline", new Class<?>[0]));
		setMouse(10, 460, 0);
		MouseInputSDL.mouseInput.update();
		setMouse(10, 460, SDLConstants.SDL_BUTTON_LMASK);
		MouseInputSDL.mouseInput.update();
		invoke(state, "updateReplayTimeline", new Class<?>[0]);
		setMouse(10, 460, 0);
		MouseInputSDL.mouseInput.update();
		for(int[] point : new int[][] {{0, 460}, {632, 460}, {100, 450}, {100, 500},
				{10, 450}, {10, 500}, {40, 460}}) {
			setMouse(point[0], point[1], 0);
			MouseInputSDL.mouseInput.update();
			invoke(state, "updateReplayTimeline", new Class<?>[0]);
		}

		for(int wheel : new int[] {-2, 2}) {
			setMouse(100, 460, 0);
			MouseInputSDL.mouseInput.update();
			NullpoMinoSDL.mouseWheelDelta = wheel;
			invoke(state, "updateReplayTimeline", new Class<?>[0]);
			setMouse(10, 460, 0);
			MouseInputSDL.mouseInput.update();
			invoke(state, "updateReplayTimeline", new Class<?>[0]);
		}
		int speed = state.fastforward;
		setMouse(10, 460, 0);
		MouseInputSDL.mouseInput.update();
		NullpoMinoSDL.mouseWheelDelta = 1;
		assertTrue((Boolean)invoke(state, "updateReplayTimeline", new Class<?>[0]));
		assertTrue(state.fastforward > speed);
		setMouse(0, 460, 0);
		MouseInputSDL.mouseInput.update();
		NullpoMinoSDL.mouseWheelDelta = 1;
		assertFalse((Boolean)invoke(state, "updateReplayTimeline", new Class<?>[0]));
		NullpoMinoSDL.mouseWheelDelta = 0;

		setMouse(200, 460, SDLConstants.SDL_BUTTON_LMASK);
		MouseInputSDL.mouseInput.update();
		invoke(state, "updateReplayTimeline", new Class<?>[0]);
		setMouse(250, 460, 0);
		MouseInputSDL.mouseInput.update();
		invoke(state, "updateReplayTimeline", new Class<?>[0]);
		setBoolean(state, "scrubbing", true);
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		invoke(state, "updateReplayTimeline", new Class<?>[0]);

		engine.stat = GameEngine.Status.SETTING;
		setMouse(100, 460, 0);
		MouseInputSDL.mouseInput.update();
		NullpoMinoSDL.mouseWheelDelta = -1;
		invoke(state, "updateReplayTimeline", new Class<?>[0]);
		NullpoMinoSDL.mouseWheelDelta = 1;
		invoke(state, "updateReplayTimeline", new Class<?>[0]);
		NullpoMinoSDL.mouseWheelDelta = 0;

		state.replayPaused = false;
		engine.stat = GameEngine.Status.MOVE;
		engine.goEnd = 1;
		engine.replayTimer = 10;
		invoke(state, "renderReplayTimeline", new Class<?>[0]);
		state.replayPaused = true;
		setBoolean(state, "scrubbing", true);
		setInt(state, "scrubTarget", 100);
		invoke(state, "renderReplayTimeline", new Class<?>[0]);
		setBoolean(state, "scrubbing", false);

		invoke(state, "seekDomain", new Class<?>[] {int.class}, -5);
		invoke(state, "seekDomain", new Class<?>[] {int.class}, 500);
		engine.replayTimer = 10;
		invoke(state, "seekReplay", new Class<?>[] {int.class}, 2);
		invoke(state, "seekReplay", new Class<?>[] {int.class}, -1);
		manager.advanceReplay = false;
		engine.replayTimer = 0;
		engine.gameActive = false;
		engine.stat = GameEngine.Status.MOVE;
		invoke(state, "seekReplay", new Class<?>[] {int.class}, 0);
		engine.replayTimer = 1;
		engine.stat = GameEngine.Status.RESULT;
		invoke(state, "seekReplay", new Class<?>[] {int.class}, 1);
		engine.stat = GameEngine.Status.MOVE;
		engine.quitflag = true;
		invoke(state, "seekReplay", new Class<?>[] {int.class}, 1);
		engine.quitflag = false;
		manager.advanceReplay = true;
		invoke(state, "seekSetting", new Class<?>[] {int.class}, -1);
		engine.stat = GameEngine.Status.SETTING;
		invoke(state, "seekSetting", new Class<?>[] {int.class}, 2);
		manager.advanceReplay = false;
		engine.quitflag = true;
		invoke(state, "seekSetting", new Class<?>[] {int.class}, 2);
		engine.quitflag = false;
		invoke(state, "seekSetting", new Class<?>[] {int.class}, 2);
	}

	private RecordingManager manager(int players) {
		RecordingManager result = new RecordingManager(new RendererSDL());
		result.mode = new TestMode(players, 0);
		result.init();
		for(GameEngine engine : result.engine) engine.init();
		return result;
	}

	private void pulse(int player, int button) {
		clearInput();
		GameKeySDL key = GameKeySDL.gamekey[player];
		NullpoMinoSDL.keyPressedState[key.keymap[button]] = true;
		NullpoMinoSDL.keyPressedState[key.keymapNav[button]] = true;
		state.update();
		Arrays.fill(NullpoMinoSDL.keyPressedState, false);
	}

	private void pulsePlayerOneOnly(int button) {
		int keymap = GameKeySDL.gamekey[0].keymap[button];
		int keymapNav = GameKeySDL.gamekey[0].keymapNav[button];
		GameKeySDL.gamekey[0].keymap[button] = SDLConstants.SDL_SCANCODE_F11;
		GameKeySDL.gamekey[0].keymapNav[button] = SDLConstants.SDL_SCANCODE_F11;
		try {
			pulse(1, button);
		} finally {
			GameKeySDL.gamekey[0].keymap[button] = keymap;
			GameKeySDL.gamekey[0].keymapNav[button] = keymapNav;
		}
	}

	private void releaseAndUpdate() {
		Arrays.fill(NullpoMinoSDL.keyPressedState, false);
		state.update();
	}

	private void updateNoKeys() {
		clearInput();
		state.update();
	}

	private static void moveMouse(int x, int y) {
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		setMouse(x, y, 0);
	}

	private static void clickMouse(int x, int y) {
		setMouse(x, y, 0);
		MouseInputSDL.mouseInput.update();
		setMouse(x, y, SDLConstants.SDL_BUTTON_LMASK);
	}

	private static void clearInput() {
		Arrays.fill(NullpoMinoSDL.keyPressedState, false);
		for(GameKeySDL key : GameKeySDL.gamekey) key.clear();
		NullpoMinoSDL.mouseWheelDelta = 0;
		NullpoMinoSDL.frameKeyEvents.clear();
		setMouse(-1, -1, 0);
	}

	private static ReplayData replayData(int size) {
		ReplayData data = new ReplayData();
		for(int i = 0; i < size; i++) data.inputDataArray.add(0);
		return data;
	}

	private static void setMouse(int x, int y, int buttons) {
		mouseX = x;
		mouseY = y;
		mouseButtons = buttons;
	}

	private static Object invoke(Object target, String name, Class<?>[] types, Object... args) {
		try {
			Method method = StateInGameSDL.class.getDeclaredMethod(name, types);
			method.setAccessible(true);
			return method.invoke(target, args);
		} catch(Exception e) {
			throw new AssertionError(e);
		}
	}

	private static void setInt(Object target, String name, int value) { setField(target, name, value); }
	private static void setBoolean(Object target, String name, boolean value) { setField(target, name, value); }
	private static void setField(Object target, String name, Object value) {
		try {
			Field field = StateInGameSDL.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(target, value);
		} catch(Exception e) {
			throw new AssertionError(e);
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
					return defaultValue(method.getReturnType());
				});
	}

	private static <T> T stub(Class<T> iface) {
		return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface},
				(proxy, method, args) -> defaultValue(method.getReturnType())));
	}

	private static SDL3Mixer mixerStub() {
		return (SDL3Mixer)Proxy.newProxyInstance(SDL3Mixer.class.getClassLoader(), new Class<?>[] {SDL3Mixer.class},
				(proxy, method, args) -> method.getName().equals("MIX_TrackPlaying") ? (byte)1
						: defaultValue(method.getReturnType()));
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

	private static final class Harness extends StateInGameSDL {}

	private static final class RecordingManager extends GameManager {
		int updates;
		int resets;
		int renders;
		int shutdowns;
		boolean advanceReplay = true;
		RecordingManager(RendererSDL receiver) { super(receiver); }
		@Override public void updateAll() {
			updates++;
			for(GameEngine engine : engine) {
				if(engine != null && replayMode && advanceReplay) {
					engine.replayTimer++;
					engine.gameActive = true;
					if(engine.stat == GameEngine.Status.SETTING) engine.stat = GameEngine.Status.MOVE;
				}
			}
		}
		@Override public void reset() {
			resets++;
			for(GameEngine engine : engine) {
				if(engine != null) {
					engine.replayTimer = 0;
					engine.gameActive = false;
					engine.stat = GameEngine.Status.SETTING;
				}
			}
		}
		@Override public void renderAll() { renders++; }
		@Override public void shutdown() { shutdowns++; }
	}

	private static final class TestMode implements GameMode {
		private final int players;
		private final int style;
		int cursor;
		TestMode(int players, int style) { this.players = players; this.style = style; }
		@Override public String getName() { return "state-matrix"; }
		@Override public int getPlayers() { return players; }
		@Override public int getGameStyle() { return style; }
		@Override public void modeInit(GameManager manager) {}
		@Override public void playerInit(GameEngine engine, int playerID) {}
		@Override public void renderInput(GameEngine engine, int playerID) {}
		@Override public boolean isNetplayMode() { return false; }
		@Override public int getMenuItemForRow(int row) { return row >= 0 && row < 3 ? row : -1; }
		@Override public int getMenuCursor() { return cursor; }
		@Override public void setMenuCursor(int cursor) { this.cursor = cursor; }
	}

	private static final class NoopState extends BaseStateSDL {
		@Override public void enter() {}
		@Override public void leave() {}
	}
}

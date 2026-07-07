// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameManager;
import nullpomino.game.ai.DummyAI;
import nullpomino.game.mode.GameMode;
import nullpomino.game.mode.NetDummyMode;
import nullpomino.game.wallkick.Wallkick;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.gui.net.NetLobbyListener;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.widget.ButtonSDL;
import nullpomino.util.GeneralUtil;
import nullpomino.game.randomizer.Randomizer;

/**
 * Runs the actual netplay game engine inside the SDL main loop.  The lobby UI
 * lives in the {@code StateNet*SDL} states; this state takes over once the
 * server sends {@code roomjoinsuccess} and the game mode has been loaded.
 *
 * <p>Entry contract: {@link NullpoMinoSDL#netLobby} must be non-null and
 * connected.  The mode to load is read from the current room's {@code strMode}
 * or from {@link #strModeToEnter} if set by a {@link NetLobbyListener} callback
 * (back-compat).</p>
 */
public class StateNetGameSDL extends BaseStateSDL implements NetLobbyListener {
	static final Logger log = LoggerFactory.getLogger(StateNetGameSDL.class);

	/**
	 * Shared instance, set in {@link #enter()} and cleared in {@link #leave()}.
	 * Exposed because a couple of legacy callers still reach into the game-manager
	 * field for title-bar updates.
	 */
	public static StateNetGameSDL instance;

	protected GameManager gameManager;
	protected String strModeToEnter = "";
	protected boolean prevInGameFlag = false;
	protected String modeName;

	/**
	 * Legacy shim: {@code public NetLobbyFrame netLobby} field is the only thing
	 * some old game modes / helper code look at on this state object.  We proxy
	 * it to the shared {@link NullpoMinoSDL#netLobby} so existing references still
	 * compile without change.  Reads are always up to date; writes do nothing.
	 */
	public NetLobbyFrame netLobby;

	/** Top-right "back" close button. No inline action - folded into the leaveRoom flag in update() so it goes through the same safe room-teardown path as ESC/mouse-back. */
	private ButtonSDL closeBtn;

	@Override
	public void enter() {
		NullpoMinoSDL.disableAutoInputUpdate = true;
		NullpoMinoSDL.isInGame = true;
		prevInGameFlag = false;
		NullpoMinoSDL.maxFPS = 60;
		NullpoMinoSDL.allowQuit = false;
		instance = this;
		closeBtn = ButtonSDL.newCloseButton(null);

		netLobby = NullpoMinoSDL.netLobby;
		if(netLobby == null) {
			// No session — someone routed us here without going through the lobby.
			// Bail cleanly back to title.
			NullpoMinoSDL.isInGame = false;
			NullpoMinoSDL.allowQuit = true;
			NullpoMinoSDL.disableAutoInputUpdate = false;
			NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_TITLE);
			return;
		}
		netLobby.addListener(this);

		gameManager = new GameManager(new RendererSDL());
		gameManager.receiver.setGraphics(NullpoMinoSDL.renderer);

		// Seed the mode: prefer the current room's strMode, else the stub NET-DUMMY.
		NetPlayerClient npc = netLobby.netPlayerClient;
		if(npc != null && npc.getYourPlayerInfo() != null && npc.getYourPlayerInfo().roomID != -1) {
			NetRoomInfo room = npc.getRoomInfo(npc.getYourPlayerInfo().roomID);
			if(room != null && room.strMode != null && room.strMode.length() > 0) {
				strModeToEnter = room.strMode;
			}
		}
		enterNewMode(strModeToEnter.length() > 0 ? strModeToEnter : null);
		strModeToEnter = "";
	}

	@Override
	public void leave() {
		if(gameManager != null) {
			gameManager.shutdown();
			gameManager = null;
		}
		if(netLobby != null) {
			// Don't shut down the session on leave — the lobby states keep using it.
			netLobby.removeListener(this);
			netLobby = null;
		}
		ResourceHolderSDL.bgmStop();

		NullpoMinoSDL.maxFPS = NullpoMinoSDL.propConfig.getProperty("option.maxfps", 60);
		NullpoMinoSDL.allowQuit = true;
		NullpoMinoSDL.disableAutoInputUpdate = false;
		NullpoMinoSDL.isInGame = false;
		NullpoMinoSDL.loadGlobalConfig();
		if(instance == this) instance = null;
	}

	@Override
	public void render() {
		try {
			if(gameManager != null) gameManager.renderAll();
		} catch(NullPointerException e) {
			if(gameManager == null || !gameManager.getQuitFlag()) log.error("render NPE", e);
		} catch(Exception e) {
			if(gameManager == null || !gameManager.getQuitFlag()) log.error("render fail", e);
		}
		closeBtn.render();
	}

	@Override
	public void update() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl != null) nl.pump();

		// ESC / mouse back leave the room and return to the lobby. The server
		// acknowledges the 'roomjoin -1' with roomjoinsuccess, which drives
		// netlobbyOnRoomLeave and the strModeToEnter==null branch below — so
		// we don't enterState manually here; just send the request.
		boolean leaveRoom = false;
		for(NullpoMinoSDL.KeyEvent ev : NullpoMinoSDL.frameKeyEvents) {
			if(ev.scancode == nullpomino.gui.sdl.binding.SDLConstants.SDL_SCANCODE_ESCAPE && !ev.repeat) {
				leaveRoom = true;
				break;
			}
		}
		MouseInputSDL.mouseInput.update();
		if(closeBtn.update(MouseInputSDL.mouseInput.getMouseX(), MouseInputSDL.mouseInput.getMouseY(), MouseInputSDL.mouseInput.isMouseClicked())) leaveRoom = true;
		if(MouseInputSDL.mouseInput.isMouseBackClicked()) leaveRoom = true;
		if(leaveRoom) {
			if(nl != null && nl.isRoomSession() && nl.netPlayerClient != null && nl.netPlayerClient.isConnected()) {
				// One room = one session: leaving the room leaves the session.
				// The teardown fires netlobbyOnDisconnect, whose strModeToEnter=null
				// path drives the goBack() to the lounge - no new navigation here.
				NullpoMinoSDL.stopRoomSession();
			} else {
				NullpoMinoSDL.goBack();
				return;
			}
		}

		try {
			int joynum = NullpoMinoSDL.joyUseNumber[0];
			boolean ingame = gameManager != null && gameManager.engine.length > 0
					&& gameManager.engine[0] != null && gameManager.engine[0].isInGame;

			if(NullpoMinoSDL.joystickMax > 0 && joynum >= 0 && joynum < NullpoMinoSDL.joystickMax) {
				GameKeySDL.gamekey[0].update(
						NullpoMinoSDL.keyPressedState,
						NullpoMinoSDL.joyPressedState[joynum],
						NullpoMinoSDL.joyAxisX[joynum],
						NullpoMinoSDL.joyAxisY[joynum],
						NullpoMinoSDL.joyHatState[joynum],
						ingame);
			} else {
				GameKeySDL.gamekey[0].update(NullpoMinoSDL.keyPressedState, ingame);
			}

			if(gameManager != null && gameManager.engine != null && gameManager.engine.length > 0 && gameManager.engine[0] != null) {
				boolean nowInGame = gameManager.engine[0].isInGame;
				if(prevInGameFlag != nowInGame) {
					prevInGameFlag = nowInGame;
					updateTitleBarCaption();
				}
			}

			if(gameManager != null) {
				if(ResourceHolderSDL.bgmPlaying != gameManager.bgmStatus.bgm) {
					ResourceHolderSDL.bgmStart(gameManager.bgmStatus.bgm);
				}
				if(ResourceHolderSDL.bgmIsPlaying()) {
					int basevolume = NullpoMinoSDL.propConfig.getProperty("option.bgmvolume", 128);
					float basevolume2 = (float)basevolume / 128;
					int newvolume = (int)(128 * (gameManager.bgmStatus.volume * basevolume2));
					if(newvolume < 0) newvolume = 0;
					if(newvolume > 128) newvolume = 128;
					if(NullpoMinoSDL.mixerLib != null && ResourceHolderSDL.bgmTrack != null)
						NullpoMinoSDL.mixerLib.MIX_SetTrackGain(ResourceHolderSDL.bgmTrack, newvolume / 128.0f);
					if(newvolume <= 0) ResourceHolderSDL.bgmStop();
				}
			}

			if(gameManager != null && gameManager.mode != null) {
				GameKeySDL.gamekey[0].inputStatusUpdate(gameManager.engine[0].ctrl);
				gameManager.updateAll();

				if(gameManager.getQuitFlag()) {
					// Quit flag can be set by a retry/exit-mode action; return to the room view.
					NullpoMinoSDL.goBack();
					return;
				}

				if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_RETRY)) {
					gameManager.mode.netplayOnRetryKey(gameManager.engine[0], 0);
				}
			}

			// Mode switches driven by lobby callbacks.
			if(strModeToEnter == null) {
				// netlobbyOnDisconnect / RoomLeave set this to null — bail to room view.
				strModeToEnter = "";
				NullpoMinoSDL.goBack();
				return;
			} else if(strModeToEnter.length() > 0) {
				enterNewMode(strModeToEnter);
				strModeToEnter = "";
			}
		} catch(NullPointerException e) {
			if(gameManager != null && gameManager.getQuitFlag()) {
				NullpoMinoSDL.goBack();
				return;
			}
			log.error("update NPE", e);
		} catch(Exception e) {
			if(gameManager != null && gameManager.getQuitFlag()) {
				NullpoMinoSDL.goBack();
				return;
			}
			log.error("update fail", e);
		}
	}

	private void enterNewMode(String newModeName) {
		NullpoMinoSDL.loadGlobalConfig();

		GameMode previousMode = gameManager.mode;
		GameMode newModeTemp = (newModeName == null) ? new NetDummyMode() : NullpoMinoSDL.modeManager.getMode(newModeName);

		if(newModeTemp == null) {
			log.error("Cannot find a mode: {}", newModeName);
			return;
		}
		if(!(newModeTemp instanceof NetDummyMode)) {
			log.error("Mode does not support netplay: {}", newModeName);
			return;
		}

		NetDummyMode newMode = (NetDummyMode)newModeTemp;
		modeName = newMode.getName();
		log.info("Enter new netplay mode: {}", modeName);

		if(previousMode != null) {
			if(gameManager.engine[0].ai != null) gameManager.engine[0].ai.shutdown(gameManager.engine[0], 0);
			previousMode.netplayUnload(netLobby);
		}
		gameManager.mode = newMode;
		gameManager.init();

		gameManager.engine[0].owRotateButtonDefaultRight = NullpoMinoSDL.propGlobal.getProperty("0.tuning.owRotateButtonDefaultRight", -1);
		gameManager.engine[0].owSkin = NullpoMinoSDL.propGlobal.getProperty("0.tuning.owSkin", -1);
		gameManager.engine[0].owMinDAS = NullpoMinoSDL.propGlobal.getProperty("0.tuning.owMinDAS", -1);
		gameManager.engine[0].owMaxDAS = NullpoMinoSDL.propGlobal.getProperty("0.tuning.owMaxDAS", -1);
		gameManager.engine[0].owDasDelay = NullpoMinoSDL.propGlobal.getProperty("0.tuning.owDasDelay", -1);
		gameManager.engine[0].owReverseUpDown = NullpoMinoSDL.propGlobal.getProperty("0.tuning.owReverseUpDown", false);
		gameManager.engine[0].owMoveDiagonal = NullpoMinoSDL.propGlobal.getProperty("0.tuning.owMoveDiagonal", -1);
		gameManager.engine[0].owBlockOutlineType = NullpoMinoSDL.propGlobal.getProperty("0.tuning.owBlockOutlineType", -1);
		gameManager.engine[0].owBlockShowOutlineOnly = NullpoMinoSDL.propGlobal.getProperty("0.tuning.owBlockShowOutlineOnly", -1);

		RuleOptions ruleopt;
		String rulename = NullpoMinoSDL.propGlobal.getProperty("0.rule", "");
		if(gameManager.mode.getGameStyle() > 0) {
			rulename = NullpoMinoSDL.propGlobal.getProperty("0.rule." + gameManager.mode.getGameStyle(), "");
		}
		if(rulename != null && rulename.length() > 0) {
			log.info("Load rule options from {}", rulename);
			ruleopt = GeneralUtil.loadRule(rulename);
		} else {
			ruleopt = new RuleOptions();
			ruleopt.readProperty(NullpoMinoSDL.propGlobal, 0);
		}
		gameManager.engine[0].ruleopt = ruleopt;

		if(ruleopt.strRandomizer != null && ruleopt.strRandomizer.length() > 0) {
			Randomizer randomizerObject = GeneralUtil.loadRandomizer(ruleopt.strRandomizer);
			gameManager.engine[0].randomizer = randomizerObject;
		}
		if(ruleopt.strWallkick != null && ruleopt.strWallkick.length() > 0) {
			Wallkick wallkickObject = GeneralUtil.loadWallkick(ruleopt.strWallkick);
			gameManager.engine[0].wallkick = wallkickObject;
		}

		String aiName = NullpoMinoSDL.propGlobal.getProperty("0.ai", "");
		if(aiName.length() > 0) {
			DummyAI aiObj = GeneralUtil.loadAIPlayer(aiName);
			gameManager.engine[0].ai = aiObj;
			gameManager.engine[0].aiMoveDelay = NullpoMinoSDL.propGlobal.getProperty("0.aiMoveDelay", 0);
			gameManager.engine[0].aiThinkDelay = NullpoMinoSDL.propGlobal.getProperty("0.aiThinkDelay", 0);
			gameManager.engine[0].aiUseThread = NullpoMinoSDL.propGlobal.getProperty("0.aiUseThread", true);
			gameManager.engine[0].aiShowHint = NullpoMinoSDL.propGlobal.getProperty("0.aiShowHint", false);
			gameManager.engine[0].aiPrethink = NullpoMinoSDL.propGlobal.getProperty("0.aiPrethink", false);
			gameManager.engine[0].aiShowState = NullpoMinoSDL.propGlobal.getProperty("0.aiShowState", false);
		}
		gameManager.showInput = NullpoMinoSDL.propConfig.getProperty("option.showInput", false);

		for(int i = 0; i < gameManager.getPlayers(); i++) gameManager.engine[i].init();

		newMode.netplayInit(netLobby);
		updateTitleBarCaption();
	}

	public void updateTitleBarCaption() {
		String netplayTitle = NullpoMinoSDL.GAME_NAME + " Netplay";
		String strTitle = netplayTitle + " - " + modeName;
		if(modeName != null && modeName.equals("NET-DUMMY")) {
			strTitle = netplayTitle;
		} else if(gameManager != null && gameManager.engine != null && gameManager.engine.length > 0 && gameManager.engine[0] != null) {
			if(gameManager.engine[0].isInGame && !gameManager.replayMode && !gameManager.replayRerecord)
				strTitle = "[PLAY] " + netplayTitle + " - " + modeName;
			else
				strTitle = "[MENU] " + netplayTitle + " - " + modeName;
		}
		SDL3.INSTANCE.SDL_SetWindowTitle(NullpoMinoSDL.window, strTitle);
	}

	// ---------------- NetLobbyListener ----------------

	@Override public void netlobbyOnDisconnect(NetLobbyFrame lobby, NetPlayerClient client, Throwable ex) {
		strModeToEnter = null;
	}
	@Override public void netlobbyOnExit(NetLobbyFrame lobby) {
		if(gameManager != null && gameManager.engine.length > 0 && gameManager.engine[0] != null) {
			gameManager.engine[0].quitflag = true;
		}
	}
	@Override public void netlobbyOnInit(NetLobbyFrame lobby) {}
	@Override public void netlobbyOnLoginOK(NetLobbyFrame lobby, NetPlayerClient client) {}
	@Override public void netlobbyOnMessage(NetLobbyFrame lobby, NetPlayerClient client, String[] message) throws IOException {}
	@Override public void netlobbyOnRoomJoin(NetLobbyFrame lobby, NetPlayerClient client, NetRoomInfo roomInfo) {
		strModeToEnter = roomInfo.strMode;
	}
	@Override public void netlobbyOnRoomLeave(NetLobbyFrame lobby, NetPlayerClient client) {
		strModeToEnter = null;
	}
}

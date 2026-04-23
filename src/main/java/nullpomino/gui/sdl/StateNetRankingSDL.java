/*
    Copyright (c) 2010, NullNoname — see LICENSE for details.
*/
package nullpomino.gui.sdl;

import nullpomino.game.play.GameEngine;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.widget.ButtonSDL;
import nullpomino.gui.sdl.widget.TabStripSDL;
import nullpomino.gui.sdl.widget.TableSDL;
import nullpomino.gui.sdl.widget.WidgetSDL;

/**
 * Multiplayer ranking viewer: per-style tabs showing rank / name / rating /
 * plays / wins, populated from server {@code mpranking} responses.  Requests
 * the active style's ranking on entry and any time the user switches tabs.
 */
public class StateNetRankingSDL extends BaseStateSDL {
	private TabStripSDL tabStrip;
	private TableSDL rankingTable;
	private ButtonSDL backBtn;
	private WidgetSDL focused;
	private int lastRequestedStyle = -1;

	@Override
	public void enter() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) { NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_TITLE); return; }

		tabStrip = new TabStripSDL(8, 40, 624, 28, GameEngine.GAMESTYLE_NAMES);
		tabStrip.setActiveTab(0);

		TableSDL.Column[] cols = {
			new TableSDL.Column("RANK",   68),
			new TableSDL.Column("NAME",  260),
			new TableSDL.Column("RATE",   96),
			new TableSDL.Column("PLAYS",  96),
			new TableSDL.Column("WINS",   96),
		};
		rankingTable = new TableSDL(8, 76, 624, 336, cols);

		backBtn = new ButtonSDL(508, 424, 124, 32, "BACK",
				new Runnable() { public void run() { NullpoMinoSDL.goBack(); } });
		backBtn.primary = true;

		setFocus(rankingTable);
		requestStyle(tabStrip.getActiveTab());
		populateFromSession();
	}

	@Override
	public void leave() {
		setFocus(null);
	}

	private void setFocus(WidgetSDL w) {
		if(focused == w) return;
		if(focused != null) focused.setFocused(false);
		focused = w;
		if(focused != null) focused.setFocused(true);
	}

	private void requestStyle(int style) {
		if(style == lastRequestedStyle) return;
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null || nl.netPlayerClient == null || !nl.netPlayerClient.isConnected()) return;
		nl.netPlayerClient.send("mpranking\t" + style + "\n");
		lastRequestedStyle = style;
	}

	private void populateFromSession() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;
		int style = tabStrip.getActiveTab();
		rankingTable.clear();
		String[][] rows = (style >= 0 && style < nl.mpRankingRows.length) ? nl.mpRankingRows[style] : null;
		if(rows != null) {
			for(String[] r : rows) rankingTable.addRow(r);
			int myRank = nl.mpRankingMyRank[style];
			if(myRank >= 0 && myRank < rankingTable.getRowCount()) rankingTable.setSelectedIndex(myRank);
			else if(rankingTable.getRowCount() > 0) rankingTable.setSelectedIndex(rankingTable.getRowCount() - 1);
		}
	}

	@Override
	public void update() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) { NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_TITLE); return; }
		nl.pump();
		MouseInputSDL.mouseInput.update();

		if(nl.mpRankingDirty) {
			nl.mpRankingDirty = false;
			populateFromSession();
		}

		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		boolean clicked = MouseInputSDL.mouseInput.isMouseClicked();

		// Mouse back button aliases Escape → return to the lobby.
		if(MouseInputSDL.mouseInput.isMouseBackClicked()) {
			NullpoMinoSDL.goBack();
			return;
		}

		int prevTab = tabStrip.getActiveTab();
		tabStrip.update(mx, my, clicked);
		if(tabStrip.getActiveTab() != prevTab) {
			requestStyle(tabStrip.getActiveTab());
			populateFromSession();
		}

		if(rankingTable.update(mx, my, clicked)) setFocus(rankingTable);
		backBtn.update(mx, my, clicked);

		for(NullpoMinoSDL.KeyEvent ev : NullpoMinoSDL.frameKeyEvents) {
			if(ev.scancode == SDLConstants.SDL_SCANCODE_ESCAPE && !ev.repeat) {
				NullpoMinoSDL.goBack();
				return;
			}
			if(!ev.repeat
				&& (ev.scancode == SDLConstants.SDL_SCANCODE_LEFT
				  || ev.scancode == SDLConstants.SDL_SCANCODE_PAGEUP)) {
				int next = tabStrip.getActiveTab() - 1;
				if(next < 0) next = GameEngine.GAMESTYLE_NAMES.length - 1;
				tabStrip.setActiveTab(next);
				requestStyle(next);
				populateFromSession();
				continue;
			}
			if(!ev.repeat
				&& (ev.scancode == SDLConstants.SDL_SCANCODE_RIGHT
				  || ev.scancode == SDLConstants.SDL_SCANCODE_PAGEDOWN)) {
				int next = tabStrip.getActiveTab() + 1;
				if(next >= GameEngine.GAMESTYLE_NAMES.length) next = 0;
				tabStrip.setActiveTab(next);
				requestStyle(next);
				populateFromSession();
				continue;
			}
			if(ev.scancode == SDLConstants.SDL_SCANCODE_TAB && !ev.repeat) {
				setFocus(focused == rankingTable ? backBtn : rankingTable);
				continue;
			}
			if(focused != null) focused.handleKey(ev);
		}
	}

	@Override
	public void render() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;

		// Share the menu.png background with Mode Select so the whole netplay
		// flow sits on a consistent backdrop (in-game excluded — it renders its
		// own field chrome).
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);

		NormalFontSDL.printFont(8, 8, "MULTIPLAYER RANKING", NormalFontSDL.COLOR_CYAN);
		tabStrip.render();
		rankingTable.render();
		backBtn.render();

		int style = tabStrip.getActiveTab();
		if(style >= 0 && style < nl.mpRankingRows.length && nl.mpRankingRows[style] == null) {
			NormalFontSDL.printFont(240, 240, "LOADING...", NormalFontSDL.COLOR_YELLOW);
		}
	}
}

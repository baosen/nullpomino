/*
    Copyright (c) 2010, NullNoname — see LICENSE for details.
*/
package mu.nu.nullpo.gui.sdl;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;

import mu.nu.nullpo.game.component.RuleOptions;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.gui.net.NetLobbyFrame;
import mu.nu.nullpo.gui.net.NetLobbyFrame.RuleEntry;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;
import mu.nu.nullpo.gui.sdl.widget.ButtonSDL;
import mu.nu.nullpo.gui.sdl.widget.TabStripSDL;
import mu.nu.nullpo.gui.sdl.widget.TableSDL;
import mu.nu.nullpo.gui.sdl.widget.WidgetSDL;
import mu.nu.nullpo.util.CustomProperties;

/**
 * Rule-change screen: per-style tabs listing every {@code .rul} file, with OK
 * to apply the selection (saved to {@code propGlobal} and re-sent to the server
 * via {@code sendMyRuleDataToServer()}).
 */
public class StateNetRuleChangeSDL extends BaseStateSDL {
	private TabStripSDL tabStrip;
	private TableSDL ruleTable;
	private ButtonSDL okBtn;
	private ButtonSDL cancelBtn;
	private WidgetSDL focused;

	/** Current per-style selection indices, restored on enter from propGlobal. */
	private int[] selectedIndex;

	@Override
	public void enter() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_TITLE); return; }

		tabStrip = new TabStripSDL(8, 40, 624, 28, GameEngine.GAMESTYLE_NAMES);
		tabStrip.setActiveTab(0);

		TableSDL.Column[] cols = {
			new TableSDL.Column("RULE",     320),
			new TableSDL.Column("FILE",     296),
		};
		ruleTable = new TableSDL(8, 76, 624, 336, cols);

		// Same positions as CreateRoom's OK/CANCEL so the buttons stay where the
		// eye expects them when flipping between the two screens.
		okBtn = new ButtonSDL( 16, 432, 120, 30, "OK",
				new Runnable() { public void run() { apply(); } });
		okBtn.primary = true;
		cancelBtn = new ButtonSDL(508, 432, 120, 30, "CANCEL",
				new Runnable() { public void run() { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY); } });

		selectedIndex = new int[GameEngine.MAX_GAMESTYLE];
		for(int i = 0; i < selectedIndex.length; i++) selectedIndex[i] = findCurrentRuleIndex(nl, i);

		refreshTable();
		setFocus(ruleTable);
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

	/**
	 * Locate the saved rule file for a given style in {@code propGlobal} and
	 * return its index within the per-style subset of {@link NetLobbyFrame#ruleEntries}.
	 * Returns 0 if the current config doesn't match anything.
	 */
	private int findCurrentRuleIndex(NetLobbyFrame nl, int style) {
		String key = (style == 0) ? "0.rulefile" : "0.rulefile." + style;
		String current = nl.propGlobal.getProperty(key, "");
		LinkedList<RuleEntry> sub = nl.getSubsetEntries(style);
		for(int i = 0; i < sub.size(); i++) {
			if(sub.get(i).filename.equals(current)) return i;
		}
		return 0;
	}

	private void refreshTable() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;
		int style = tabStrip.getActiveTab();
		ruleTable.clear();
		LinkedList<RuleEntry> sub = nl.getSubsetEntries(style);
		for(RuleEntry e : sub) {
			ruleTable.addRow(new String[] {
				e.rulename == null ? "" : e.rulename,
				e.filename == null ? "" : e.filename,
			});
		}
		int sel = selectedIndex[style];
		if(sel >= 0 && sel < ruleTable.getRowCount()) ruleTable.setSelectedIndex(sel);
		else if(ruleTable.getRowCount() > 0) ruleTable.setSelectedIndex(0);
	}

	private void apply() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;

		// Save the current tab's selection back into the per-style index array
		// (the other tabs keep whatever the user already picked).
		selectedIndex[tabStrip.getActiveTab()] = Math.max(0, ruleTable.getSelectedIndex());

		// Persist chosen rule files + rule names into propGlobal for every style.
		for(int style = 0; style < GameEngine.MAX_GAMESTYLE; style++) {
			LinkedList<RuleEntry> sub = nl.getSubsetEntries(style);
			int sel = selectedIndex[style];
			if(sub.isEmpty() || sel < 0 || sel >= sub.size()) continue;
			RuleEntry chosen = sub.get(sel);
			String prefixName = (style == 0) ? "0.rulename" : "0.rulename." + style;
			String prefixFile = (style == 0) ? "0.rulefile" : "0.rulefile." + style;
			String prefixPath = (style == 0) ? "0.rule"     : "0.rule."     + style;
			nl.propGlobal.setProperty(prefixName, chosen.rulename == null ? "" : chosen.rulename);
			nl.propGlobal.setProperty(prefixFile, chosen.filename);
			nl.propGlobal.setProperty(prefixPath, chosen.filepath);
		}

		// Load the current-style rule into ruleOptPlayer and re-send to the server.
		int activeStyle = 0;
		LinkedList<RuleEntry> sub = nl.getSubsetEntries(activeStyle);
		int sel = selectedIndex[activeStyle];
		if(!sub.isEmpty() && sel >= 0 && sel < sub.size()) {
			RuleEntry chosen = sub.get(sel);
			RuleOptions opt = loadRule(chosen.filepath);
			if(opt != null) nl.ruleOptPlayer = opt;
		}

		nl.saveGlobalConfig();
		if(nl.netPlayerClient != null && nl.netPlayerClient.isConnected()) nl.sendMyRuleDataToServer();
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY);
	}

	private static RuleOptions loadRule(String filepath) {
		CustomProperties prop = new CustomProperties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(filepath);
			prop.load(in);
		} catch(IOException ignore) {
			return null;
		} finally {
			if(in != null) try { in.close(); } catch(IOException ignore) {}
		}
		RuleOptions opt = new RuleOptions();
		opt.readProperty(prop, 0);
		return opt;
	}

	@Override
	public void update() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_TITLE); return; }
		nl.pump();
		MouseInputSDL.mouseInput.update();

		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		boolean clicked = MouseInputSDL.mouseInput.isMouseClicked();

		int prevTab = tabStrip.getActiveTab();
		tabStrip.update(mx, my, clicked);
		if(tabStrip.getActiveTab() != prevTab) {
			// Store current tab's selection before switching.
			selectedIndex[prevTab] = Math.max(0, ruleTable.getSelectedIndex());
			refreshTable();
		}

		if(ruleTable.update(mx, my, clicked)) setFocus(ruleTable);
		if(ruleTable.activated) apply();
		okBtn.update(mx, my, clicked);
		cancelBtn.update(mx, my, clicked);

		for(NullpoMinoSDL.KeyEvent ev : NullpoMinoSDL.frameKeyEvents) {
			if(ev.scancode == SDLConstants.SDL_SCANCODE_ESCAPE && !ev.repeat) {
				NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY);
				return;
			}
			if(!ev.repeat
				&& (ev.scancode == SDLConstants.SDL_SCANCODE_PAGEUP)) {
				int next = tabStrip.getActiveTab() - 1;
				if(next < 0) next = GameEngine.GAMESTYLE_NAMES.length - 1;
				selectedIndex[tabStrip.getActiveTab()] = Math.max(0, ruleTable.getSelectedIndex());
				tabStrip.setActiveTab(next);
				refreshTable();
				continue;
			}
			if(!ev.repeat
				&& (ev.scancode == SDLConstants.SDL_SCANCODE_PAGEDOWN)) {
				int next = tabStrip.getActiveTab() + 1;
				if(next >= GameEngine.GAMESTYLE_NAMES.length) next = 0;
				selectedIndex[tabStrip.getActiveTab()] = Math.max(0, ruleTable.getSelectedIndex());
				tabStrip.setActiveTab(next);
				refreshTable();
				continue;
			}
			if(!ev.repeat && ev.scancode == SDLConstants.SDL_SCANCODE_TAB) {
				setFocus(nextFocus(focused, (ev.keymod & SDLConstants.SDL_KMOD_SHIFT) != 0));
				continue;
			}
			if(focused != null) focused.handleKey(ev);
		}
	}

	private WidgetSDL nextFocus(WidgetSDL cur, boolean back) {
		WidgetSDL[] order = { ruleTable, okBtn, cancelBtn };
		int idx = 0;
		for(int i = 0; i < order.length; i++) if(order[i] == cur) { idx = i; break; }
		int next = back ? idx - 1 : idx + 1;
		if(next < 0) next = order.length - 1;
		if(next >= order.length) next = 0;
		return order[next];
	}

	@Override
	public void render() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;

		NormalFontSDL.printFont(8, 8, "RULE CHANGE", NormalFontSDL.COLOR_CYAN);
		tabStrip.render();
		ruleTable.render();
		okBtn.render();
		cancelBtn.render();
	}
}

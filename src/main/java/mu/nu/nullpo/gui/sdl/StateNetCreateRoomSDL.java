/*
    Copyright (c) 2010, NullNoname
    All rights reserved.
    See LICENSE for details.
*/
package mu.nu.nullpo.gui.sdl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import mu.nu.nullpo.game.net.NetPlayerInfo;
import mu.nu.nullpo.game.net.NetRoomInfo;
import mu.nu.nullpo.game.net.NetUtil;
import mu.nu.nullpo.gui.net.NetLobbyFrame;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;
import mu.nu.nullpo.gui.sdl.widget.ButtonSDL;
import mu.nu.nullpo.gui.sdl.widget.CheckboxSDL;
import mu.nu.nullpo.gui.sdl.widget.DropdownSDL;
import mu.nu.nullpo.gui.sdl.widget.SpinnerSDL;
import mu.nu.nullpo.gui.sdl.widget.TabStripSDL;
import mu.nu.nullpo.gui.sdl.widget.TextInputSDL;
import mu.nu.nullpo.gui.sdl.widget.WidgetSDL;

/**
 * Create Room / Room Detail form.  Five tabs mirror the Swing lobby's
 * CreateRoom panel (Basic / Speed / Bonus / Garbage / Misc).  Values are backed
 * by a {@link NetRoomInfo} held on the session so the form persists when the
 * user flips between tabs or switches mode.
 *
 * Two modes:
 *   - create:    user edits the form and presses OK to open a new room.
 *   - detail:    user is viewing an existing room's settings.  OK is hidden,
 *                JOIN / WATCH appear instead.  All editable widgets are disabled.
 *
 * Navigation:
 *   - Mouse: click anywhere; buttons run their actions on click.
 *   - Keyboard: UP/DOWN cycles through the focusable widgets of the active tab;
 *     LEFT/RIGHT work within the button row and for horizontal widgets; Enter
 *     activates the focused button.  PageUp/PageDown (or LEFT/RIGHT on the tab
 *     strip) switch tabs.
 */
public class StateNetCreateRoomSDL extends BaseStateSDL {
	private static final String[] TAB_LABELS = { "BASIC", "SPEED", "BONUS", "GARBAGE", "MISC" };

	private static final String[] TSPIN_TYPE_LABELS   = { "DISABLE", "T-ONLY", "ALL SPIN" };
	private static final String[] SPIN_CHECK_LABELS   = { "4-POINT", "IMMOBILE" };

	private TabStripSDL tabStrip;

	// BASIC
	private TextInputSDL roomName;
	private DropdownSDL modeDropdown;
	private SpinnerSDL maxPlayers;
	private SpinnerSDL autoStartSeconds;
	private CheckboxSDL useMap;
	private CheckboxSDL ruleLock;

	// SPEED
	private SpinnerSDL gravity;
	private SpinnerSDL denominator;
	private SpinnerSDL are;
	private SpinnerSDL areLine;
	private SpinnerSDL lineDelay;
	private SpinnerSDL lockDelay;
	private SpinnerSDL das;

	// BONUS
	private DropdownSDL tspinType;
	private DropdownSDL spinCheck;
	private CheckboxSDL ezTSpin;
	private CheckboxSDL b2b;
	private CheckboxSDL combo;
	private CheckboxSDL rensaBlock;
	private CheckboxSDL counter;
	private CheckboxSDL bravo;

	// GARBAGE
	private SpinnerSDL garbagePercent;
	private SpinnerSDL targetTimer;
	private CheckboxSDL changePerAttack;
	private CheckboxSDL divideRate;
	private CheckboxSDL b2bChunk;
	private CheckboxSDL fractGarbage;
	private CheckboxSDL target;
	private CheckboxSDL reduceLineSend;

	// MISC
	private SpinnerSDL hurryupSeconds;
	private SpinnerSDL hurryupInterval;
	private CheckboxSDL tnet2Timer;
	private CheckboxSDL disableAfterCancel;

	// Bottom button row
	private ButtonSDL okBtn;
	private ButtonSDL joinBtn;
	private ButtonSDL watchBtn;
	private ButtonSDL cancelBtn;

	/** Label rendered to the left of each widget. */
	private static final class Field {
		final String label;
		final WidgetSDL widget;
		Field(String label, WidgetSDL widget) { this.label = label; this.widget = widget; }
	}

	/** Per-tab focusable widgets; populated in {@link #enter()}. */
	private Field[][] tabFields;

	private WidgetSDL focused;
	private boolean detailMode;  // true when viewing an existing room (read-only)
	private String statusLine = "";

	@Override
	public void enter() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_TITLE); return; }

		detailMode = nl.currentViewDetailRoomID != -1;
		NetRoomInfo source = resolveSource(nl);

		tabStrip = new TabStripSDL(8, 32, 624, 28, TAB_LABELS);
		tabStrip.setActiveTab(0);

		buildWidgets(nl, source);
		applyDetailModeEnabled();
		setFocus(detailMode ? (WidgetSDL)joinBtn : (WidgetSDL)roomName);
	}

	@Override
	public void leave() {
		setFocus(null);
		NullpoMinoSDL.stopTextInput();
	}

	/** Either the detail room info (view mode) or backupRoomInfo (create mode). */
	private NetRoomInfo resolveSource(NetLobbyFrame nl) {
		if(detailMode && nl.netPlayerClient != null) {
			NetRoomInfo r = nl.netPlayerClient.getRoomInfo(nl.currentViewDetailRoomID);
			if(r != null) return r;
		}
		if(nl.backupRoomInfo == null) {
			NetRoomInfo r = new NetRoomInfo();
			nl.backupRoomInfo = r;
			loadDefaultsFromConfig(nl, r);
		}
		return nl.backupRoomInfo;
	}

	/** Read saved defaults (createroom.default*) from lobby config into the given room info. */
	private void loadDefaultsFromConfig(NetLobbyFrame nl, NetRoomInfo r) {
		r.maxPlayers = nl.propConfig.getProperty("createroom.defaultMaxPlayers", 6);
		r.autoStartSeconds = nl.propConfig.getProperty("createroom.defaultAutoStartSeconds", 15);
		r.gravity = nl.propConfig.getProperty("createroom.defaultGravity", 1);
		r.denominator = nl.propConfig.getProperty("createroom.defaultDenominator", 60);
		r.are = nl.propConfig.getProperty("createroom.defaultARE", 0);
		r.areLine = nl.propConfig.getProperty("createroom.defaultARELine", 0);
		r.lineDelay = nl.propConfig.getProperty("createroom.defaultLineDelay", 0);
		r.lockDelay = nl.propConfig.getProperty("createroom.defaultLockDelay", 30);
		r.das = nl.propConfig.getProperty("createroom.defaultDAS", 11);
		r.hurryupSeconds = nl.propConfig.getProperty("createroom.defaultHurryupSeconds", 180);
		r.hurryupInterval = nl.propConfig.getProperty("createroom.defaultHurryupInterval", 5);
		r.garbagePercent = nl.propConfig.getProperty("createroom.defaultGarbagePercent", 90);
		r.targetTimer = nl.propConfig.getProperty("createroom.defaultTargetTimer", 60);
		r.ruleLock = nl.propConfig.getProperty("createroom.defaultRuleLock", false);
		r.tspinEnableType = nl.propConfig.getProperty("createroom.defaultTSpinEnableType", 1);
		r.spinCheckType = nl.propConfig.getProperty("createroom.defaultSpinCheckType", 0);
		r.tspinEnableEZ = nl.propConfig.getProperty("createroom.defaultTSpinEnableEZ", false);
		r.b2b = nl.propConfig.getProperty("createroom.defaultB2B", true);
		r.combo = nl.propConfig.getProperty("createroom.defaultCombo", true);
		r.rensaBlock = nl.propConfig.getProperty("createroom.defaultRensaBlock", true);
		r.counter = nl.propConfig.getProperty("createroom.defaultCounter", true);
		r.bravo = nl.propConfig.getProperty("createroom.defaultBravo", true);
		r.reduceLineSend = nl.propConfig.getProperty("createroom.defaultReduceLineSend", true);
		r.garbageChangePerAttack = nl.propConfig.getProperty("createroom.defaultGarbageChangePerAttack", true);
		r.divideChangeRateByPlayers = nl.propConfig.getProperty("createroom.defaultDivideChangeRateByPlayers", false);
		r.b2bChunk = nl.propConfig.getProperty("createroom.defaultB2BChunk", false);
		r.useFractionalGarbage = nl.propConfig.getProperty("createroom.defaultUseFractionalGarbage", false);
		r.isTarget = nl.propConfig.getProperty("createroom.defaultIsTarget", false);
		r.autoStartTNET2 = nl.propConfig.getProperty("createroom.defaultAutoStartTNET2", false);
		r.disableTimerAfterSomeoneCancelled = nl.propConfig.getProperty("createroom.defaultDisableTimerAfterSomeoneCancelled", false);
		r.useMap = nl.propConfig.getProperty("createroom.defaultUseMap", false);
	}

	/** Construct all widgets and populate them from {@code src}. */
	private void buildWidgets(NetLobbyFrame nl, NetRoomInfo src) {
		// Layout: labels at x=16, widgets at x=200, each row 26px tall, first row y=76.
		final int colL = 16, colR = 200, rowY = 76, rowH = 26;
		final int wFull = 416, wShort = 180;

		// BASIC
		roomName = new TextInputSDL(colR, rowY + rowH * 0, wFull, 22);
		roomName.maxChars = 64;
		roomName.placeholder = "ROOM NAME";
		roomName.setText(src.strName);

		modeDropdown = new DropdownSDL(colR, rowY + rowH * 1, wFull, 22, loadModeList());
		setDropdownSelection(modeDropdown, src.strMode);

		maxPlayers       = new SpinnerSDL(colR, rowY + rowH * 2, wShort, 22, 1, 6, 1, src.maxPlayers);
		autoStartSeconds = new SpinnerSDL(colR, rowY + rowH * 3, wShort, 22, 0, 600, 1, src.autoStartSeconds);
		useMap    = new CheckboxSDL(colR, rowY + rowH * 4, wFull, 22, "USE MAP",   src.useMap);
		ruleLock  = new CheckboxSDL(colR, rowY + rowH * 5, wFull, 22, "RULE LOCK", src.ruleLock);

		// SPEED
		gravity     = new SpinnerSDL(colR, rowY + rowH * 0, wShort, 22, 0, 99, 1, src.gravity);
		denominator = new SpinnerSDL(colR, rowY + rowH * 1, wShort, 22, 1, 999, 1, src.denominator);
		are         = new SpinnerSDL(colR, rowY + rowH * 2, wShort, 22, 0, 99, 1, src.are);
		areLine     = new SpinnerSDL(colR, rowY + rowH * 3, wShort, 22, 0, 99, 1, src.areLine);
		lineDelay   = new SpinnerSDL(colR, rowY + rowH * 4, wShort, 22, 0, 99, 1, src.lineDelay);
		lockDelay   = new SpinnerSDL(colR, rowY + rowH * 5, wShort, 22, 0, 99, 1, src.lockDelay);
		das         = new SpinnerSDL(colR, rowY + rowH * 6, wShort, 22, 0, 99, 1, src.das);

		// BONUS
		tspinType = new DropdownSDL(colR, rowY + rowH * 0, wFull, 22, TSPIN_TYPE_LABELS);
		tspinType.setSelectedIndex(src.tspinEnableType);
		spinCheck = new DropdownSDL(colR, rowY + rowH * 1, wFull, 22, SPIN_CHECK_LABELS);
		spinCheck.setSelectedIndex(src.spinCheckType);
		ezTSpin    = new CheckboxSDL(colR, rowY + rowH * 2, wFull, 22, "EZ T-SPIN",   src.tspinEnableEZ);
		b2b        = new CheckboxSDL(colR, rowY + rowH * 3, wFull, 22, "B2B",         src.b2b);
		combo      = new CheckboxSDL(colR, rowY + rowH * 4, wFull, 22, "COMBO",       src.combo);
		rensaBlock = new CheckboxSDL(colR, rowY + rowH * 5, wFull, 22, "RENSA BLOCK", src.rensaBlock);
		counter    = new CheckboxSDL(colR, rowY + rowH * 6, wFull, 22, "COUNTER",     src.counter);
		bravo      = new CheckboxSDL(colR, rowY + rowH * 7, wFull, 22, "BRAVO",       src.bravo);

		// GARBAGE
		garbagePercent = new SpinnerSDL(colR, rowY + rowH * 0, wShort, 22, 0, 100, 1, src.garbagePercent);
		targetTimer    = new SpinnerSDL(colR, rowY + rowH * 1, wShort, 22, 0, 9999, 10, src.targetTimer);
		changePerAttack = new CheckboxSDL(colR, rowY + rowH * 2, wFull, 22, "CHANGE PER ATTACK",  src.garbageChangePerAttack);
		divideRate      = new CheckboxSDL(colR, rowY + rowH * 3, wFull, 22, "DIVIDE RATE",        src.divideChangeRateByPlayers);
		b2bChunk        = new CheckboxSDL(colR, rowY + rowH * 4, wFull, 22, "B2B CHUNK",          src.b2bChunk);
		fractGarbage    = new CheckboxSDL(colR, rowY + rowH * 5, wFull, 22, "FRACTIONAL GARBAGE", src.useFractionalGarbage);
		target          = new CheckboxSDL(colR, rowY + rowH * 6, wFull, 22, "TARGET",             src.isTarget);
		reduceLineSend  = new CheckboxSDL(colR, rowY + rowH * 7, wFull, 22, "REDUCE LINE SEND",   src.reduceLineSend);

		// MISC
		hurryupSeconds  = new SpinnerSDL(colR, rowY + rowH * 0, wShort, 22, -1, 9999, 1, src.hurryupSeconds);
		hurryupInterval = new SpinnerSDL(colR, rowY + rowH * 1, wShort, 22, 1, 99,   1, src.hurryupInterval);
		tnet2Timer         = new CheckboxSDL(colR, rowY + rowH * 2, wFull, 22, "TNET2 TIMER",           src.autoStartTNET2);
		disableAfterCancel = new CheckboxSDL(colR, rowY + rowH * 3, wFull, 22, "DISABLE AFTER CANCEL",  src.disableTimerAfterSomeoneCancelled);

		// Bottom button row — OK (create), JOIN/WATCH (detail view), CANCEL.
		int btnY = 432;
		okBtn     = new ButtonSDL( 16, btnY, 120, 30, "OK",     new Runnable() { public void run() { submit(false, false); } });
		okBtn.primary = true;
		joinBtn   = new ButtonSDL(140, btnY, 120, 30, "JOIN",   new Runnable() { public void run() { submit(true,  false); } });
		joinBtn.primary = true;
		watchBtn  = new ButtonSDL(264, btnY, 120, 30, "WATCH",  new Runnable() { public void run() { submit(true,  true); } });
		cancelBtn = new ButtonSDL(508, btnY, 120, 30, "CANCEL", new Runnable() { public void run() { cancel(); } });

		// Build the per-tab widget arrays in the order they appear on screen.
		tabFields = new Field[][] {
			// BASIC
			{
				new Field("ROOM NAME",    roomName),
				new Field("MODE",         modeDropdown),
				new Field("MAX PLAYERS",  maxPlayers),
				new Field("AUTOSTART S",  autoStartSeconds),
				new Field("",             useMap),
				new Field("",             ruleLock),
			},
			// SPEED
			{
				new Field("GRAVITY",      gravity),
				new Field("DENOMINATOR",  denominator),
				new Field("ARE",          are),
				new Field("ARE LINE",     areLine),
				new Field("LINE DELAY",   lineDelay),
				new Field("LOCK DELAY",   lockDelay),
				new Field("DAS",          das),
			},
			// BONUS
			{
				new Field("T-SPIN",       tspinType),
				new Field("SPIN CHECK",   spinCheck),
				new Field("",             ezTSpin),
				new Field("",             b2b),
				new Field("",             combo),
				new Field("",             rensaBlock),
				new Field("",             counter),
				new Field("",             bravo),
			},
			// GARBAGE
			{
				new Field("GARBAGE %",    garbagePercent),
				new Field("TARGET TIMER", targetTimer),
				new Field("",             changePerAttack),
				new Field("",             divideRate),
				new Field("",             b2bChunk),
				new Field("",             fractGarbage),
				new Field("",             target),
				new Field("",             reduceLineSend),
			},
			// MISC
			{
				new Field("HURRYUP S",    hurryupSeconds),
				new Field("HURRYUP INT",  hurryupInterval),
				new Field("",             tnet2Timer),
				new Field("",             disableAfterCancel),
			},
		};
	}

	/** Disable all editable widgets when viewing a room's settings (detail mode). */
	private void applyDetailModeEnabled() {
		boolean editable = !detailMode;
		for(Field[] tab : tabFields) for(Field f : tab) f.widget.enabled = editable;
		okBtn.visible = editable;
		joinBtn.visible = detailMode;
		watchBtn.visible = detailMode;
	}

	private String[] loadModeList() {
		List<String> list = new ArrayList<String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader("config/list/netlobby_multimode.lst"));
			String line;
			while((line = in.readLine()) != null) {
				line = line.trim();
				if(line.length() > 0 && !line.startsWith("#")) list.add(line);
			}
		} catch(IOException ignore) {
			// Fallback: just one default entry so users can create a vs-battle room.
			list.add("NET-VS-BATTLE");
		} finally {
			if(in != null) try { in.close(); } catch(IOException ignore) {}
		}
		if(list.isEmpty()) list.add("NET-VS-BATTLE");
		return list.toArray(new String[list.size()]);
	}

	private static void setDropdownSelection(DropdownSDL d, String item) {
		if(item == null || item.length() == 0) { d.setSelectedIndex(0); return; }
		for(int i = 0; ; i++) {
			if(i >= 256) break;  // safety; lists are short in practice
			String opt = null;
			try { d.setSelectedIndex(i); opt = d.getSelectedItem(); } catch(Throwable t) { break; }
			if(opt == null || opt.length() == 0) break;
			if(item.equals(opt)) return;
		}
		d.setSelectedIndex(0);
	}

	private void setFocus(WidgetSDL w) {
		if(focused == w) return;
		if(focused != null) focused.setFocused(false);
		focused = w;
		if(focused != null) focused.setFocused(true);
	}

	private Field[] activeTab() { return tabFields[tabStrip.getActiveTab()]; }

	@Override
	public void update() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_TITLE); return; }
		nl.pump();
		MouseInputSDL.mouseInput.update();

		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		boolean clicked = MouseInputSDL.mouseInput.isMouseClicked();

		// Tab switching — click on strip, or auto-handled by tab label below.
		if(tabStrip.update(mx, my, clicked)) setFocus(activeTab()[0].widget);

		// Update all widgets on the active tab so their hover states stay live.
		for(Field f : activeTab()) {
			if(f.widget.update(mx, my, clicked)) setFocus(f.widget);
		}

		// Button row always visible
		okBtn.update(mx, my, clicked);
		joinBtn.update(mx, my, clicked);
		watchBtn.update(mx, my, clicked);
		cancelBtn.update(mx, my, clicked);

		// Render-overlay step for any open dropdown happens in render().

		String typed = NullpoMinoSDL.consumeTextInput();
		if(focused != null && typed.length() > 0) focused.handleTextInput(typed);
		for(NullpoMinoSDL.KeyEvent ev : NullpoMinoSDL.frameKeyEvents) {
			if(handleGlobalKey(ev)) continue;

			boolean up   = ev.scancode == SDLConstants.SDL_SCANCODE_UP;
			boolean down = ev.scancode == SDLConstants.SDL_SCANCODE_DOWN;
			boolean left = ev.scancode == SDLConstants.SDL_SCANCODE_LEFT;
			boolean right= ev.scancode == SDLConstants.SDL_SCANCODE_RIGHT;

			// Don't let held LEFT/RIGHT/UP/DOWN on a dropdown keep flipping selection
			// if the user is really trying to nav widgets.  Dropdowns handle non-repeat
			// up/down internally via setSelectedIndex; we leave those alone.

			// LEFT/RIGHT on the button row cycle between buttons.
			if((left || right) && !ev.repeat && tryButtonRowNav(left)) continue;

			if((up || down) && !ev.repeat && tryTabFormNav(up)) continue;

			if(focused != null) focused.handleKey(ev);
		}
	}

	/** Return true if the key was consumed globally (ESC, tab switch, etc.). */
	private boolean handleGlobalKey(NullpoMinoSDL.KeyEvent ev) {
		if(ev.repeat) return false;
		switch(ev.scancode) {
			case SDLConstants.SDL_SCANCODE_ESCAPE:
				cancel();
				return true;
			case SDLConstants.SDL_SCANCODE_PAGEUP:
				switchTab(tabStrip.getActiveTab() - 1);
				return true;
			case SDLConstants.SDL_SCANCODE_PAGEDOWN:
				switchTab(tabStrip.getActiveTab() + 1);
				return true;
			case SDLConstants.SDL_SCANCODE_TAB: {
				boolean shift = (ev.keymod & SDLConstants.SDL_KMOD_SHIFT) != 0;
				tryTabFormNav(shift);
				return true;
			}
			default: return false;
		}
	}

	private void switchTab(int target) {
		int n = TAB_LABELS.length;
		if(target < 0) target = n - 1;
		if(target >= n) target = 0;
		tabStrip.setActiveTab(target);
		setFocus(activeTab()[0].widget);
	}

	/**
	 * Vertical nav: cycles through the current tab's fields, then the buttons,
	 * then wraps back to the first field.  Returns true if focus moved.
	 */
	private boolean tryTabFormNav(boolean up) {
		Field[] tab = activeTab();
		List<WidgetSDL> list = new ArrayList<WidgetSDL>();
		for(Field f : tab) if(f.widget.enabled) list.add(f.widget);
		if(okBtn.visible) list.add(okBtn);
		if(joinBtn.visible) list.add(joinBtn);
		if(watchBtn.visible) list.add(watchBtn);
		list.add(cancelBtn);

		int idx = list.indexOf(focused);
		if(idx < 0) { setFocus(list.get(0)); return true; }
		int next = up ? idx - 1 : idx + 1;
		if(next < 0) next = list.size() - 1;
		if(next >= list.size()) next = 0;
		setFocus(list.get(next));
		return true;
	}

	/** LEFT/RIGHT moves between buttons in the bottom button row. */
	private boolean tryButtonRowNav(boolean left) {
		List<ButtonSDL> row = new ArrayList<ButtonSDL>();
		if(okBtn.visible) row.add(okBtn);
		if(joinBtn.visible) row.add(joinBtn);
		if(watchBtn.visible) row.add(watchBtn);
		row.add(cancelBtn);
		for(int i = 0; i < row.size(); i++) {
			if(focused == row.get(i)) {
				int next = left ? i - 1 : i + 1;
				if(next < 0) next = row.size() - 1;
				if(next >= row.size()) next = 0;
				setFocus(row.get(next));
				return true;
			}
		}
		return false;
	}

	private void submit(boolean join, boolean watch) {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null || nl.netPlayerClient == null) return;

		if(join) {
			if(nl.currentViewDetailRoomID != -1) {
				nl.joinRoom(nl.currentViewDetailRoomID, watch);
			}
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY);
			return;
		}

		// Create path: snapshot current values into backupRoomInfo, then send.
		NetRoomInfo r = (nl.backupRoomInfo != null) ? nl.backupRoomInfo : new NetRoomInfo();
		collectFormInto(r);

		NetPlayerInfo me = nl.netPlayerClient.getYourPlayerInfo();
		if(me != null) r.style = 0;  // NullpoMino's default style for multiplayer

		String msg = buildRoomCreateMessage(r);
		if(msg == null) { statusLine = "ROOM NAME REQUIRED"; return; }

		nl.backupRoomInfo = r;
		// Persist the defaults so the user's tuning survives reconnects.
		saveDefaultsToConfig(nl, r);
		nl.saveConfig();
		nl.netPlayerClient.send(msg);
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY);
	}

	private void collectFormInto(NetRoomInfo r) {
		r.strName = roomName.getText();
		r.strMode = modeDropdown.getSelectedItem();
		r.maxPlayers = maxPlayers.getValue();
		r.autoStartSeconds = autoStartSeconds.getValue();
		r.useMap = useMap.checked;
		r.ruleLock = ruleLock.checked;
		r.gravity = gravity.getValue();
		r.denominator = denominator.getValue();
		r.are = are.getValue();
		r.areLine = areLine.getValue();
		r.lineDelay = lineDelay.getValue();
		r.lockDelay = lockDelay.getValue();
		r.das = das.getValue();
		r.tspinEnableType = tspinType.getSelectedIndex();
		r.spinCheckType = spinCheck.getSelectedIndex();
		r.tspinEnableEZ = ezTSpin.checked;
		r.b2b = b2b.checked;
		r.combo = combo.checked;
		r.rensaBlock = rensaBlock.checked;
		r.counter = counter.checked;
		r.bravo = bravo.checked;
		r.garbagePercent = garbagePercent.getValue();
		r.targetTimer = targetTimer.getValue();
		r.garbageChangePerAttack = changePerAttack.checked;
		r.divideChangeRateByPlayers = divideRate.checked;
		r.b2bChunk = b2bChunk.checked;
		r.useFractionalGarbage = fractGarbage.checked;
		r.isTarget = target.checked;
		r.reduceLineSend = reduceLineSend.checked;
		r.hurryupSeconds = hurryupSeconds.getValue();
		r.hurryupInterval = hurryupInterval.getValue();
		r.autoStartTNET2 = tnet2Timer.checked;
		r.disableTimerAfterSomeoneCancelled = disableAfterCancel.checked;
	}

	/**
	 * Build the roomcreate protocol string.  Mirrors the tab-delimited format
	 * the Swing lobby used, which the server parses via NetRoomInfo's ctor.
	 */
	private String buildRoomCreateMessage(NetRoomInfo r) {
		if(r.strName == null || r.strName.trim().length() == 0) return null;
		StringBuilder sb = new StringBuilder("roomcreate\t");
		sb.append(NetUtil.urlEncode(r.strName)).append('\t');
		sb.append(r.maxPlayers).append('\t');
		sb.append(r.autoStartSeconds).append('\t');
		sb.append(r.gravity).append('\t');
		sb.append(r.denominator).append('\t');
		sb.append(r.are).append('\t');
		sb.append(r.areLine).append('\t');
		sb.append(r.lineDelay).append('\t');
		sb.append(r.lockDelay).append('\t');
		sb.append(r.das).append('\t');
		sb.append(r.ruleLock).append('\t');
		sb.append(r.tspinEnableType).append('\t');
		sb.append(r.b2b).append('\t');
		sb.append(r.combo).append('\t');
		sb.append(NetUtil.urlEncode(r.strMode == null ? "" : r.strMode)).append('\t');
		sb.append(r.useMap).append('\t');
		sb.append(r.reduceLineSend).append('\t');
		sb.append(r.hurryupSeconds).append('\t');
		sb.append(r.hurryupInterval).append('\t');
		sb.append(r.rensaBlock).append('\t');
		sb.append(r.counter).append('\t');
		sb.append(r.bravo).append('\t');
		sb.append(r.autoStartTNET2).append('\t');
		sb.append(r.disableTimerAfterSomeoneCancelled).append('\t');
		sb.append(r.useFractionalGarbage).append('\t');
		sb.append(r.garbagePercent).append('\t');
		sb.append(r.garbageChangePerAttack).append('\t');
		sb.append(r.b2bChunk).append('\t');
		sb.append(r.tspinEnableEZ).append('\t');
		sb.append(r.spinCheckType).append('\t');
		sb.append(r.isTarget).append('\t');
		sb.append(r.targetTimer).append('\t');
		sb.append(r.divideChangeRateByPlayers).append('\t');
		sb.append(r.style).append('\n');
		return sb.toString();
	}

	private static void saveDefaultsToConfig(NetLobbyFrame nl, NetRoomInfo r) {
		nl.propConfig.setProperty("createroom.defaultMaxPlayers", r.maxPlayers);
		nl.propConfig.setProperty("createroom.defaultAutoStartSeconds", r.autoStartSeconds);
		nl.propConfig.setProperty("createroom.defaultGravity", r.gravity);
		nl.propConfig.setProperty("createroom.defaultDenominator", r.denominator);
		nl.propConfig.setProperty("createroom.defaultARE", r.are);
		nl.propConfig.setProperty("createroom.defaultARELine", r.areLine);
		nl.propConfig.setProperty("createroom.defaultLineDelay", r.lineDelay);
		nl.propConfig.setProperty("createroom.defaultLockDelay", r.lockDelay);
		nl.propConfig.setProperty("createroom.defaultDAS", r.das);
		nl.propConfig.setProperty("createroom.defaultHurryupSeconds", r.hurryupSeconds);
		nl.propConfig.setProperty("createroom.defaultHurryupInterval", r.hurryupInterval);
		nl.propConfig.setProperty("createroom.defaultGarbagePercent", r.garbagePercent);
		nl.propConfig.setProperty("createroom.defaultTargetTimer", r.targetTimer);
		nl.propConfig.setProperty("createroom.defaultRuleLock", r.ruleLock);
		nl.propConfig.setProperty("createroom.defaultTSpinEnableType", r.tspinEnableType);
		nl.propConfig.setProperty("createroom.defaultSpinCheckType", r.spinCheckType);
		nl.propConfig.setProperty("createroom.defaultTSpinEnableEZ", r.tspinEnableEZ);
		nl.propConfig.setProperty("createroom.defaultB2B", r.b2b);
		nl.propConfig.setProperty("createroom.defaultCombo", r.combo);
		nl.propConfig.setProperty("createroom.defaultRensaBlock", r.rensaBlock);
		nl.propConfig.setProperty("createroom.defaultCounter", r.counter);
		nl.propConfig.setProperty("createroom.defaultBravo", r.bravo);
		nl.propConfig.setProperty("createroom.defaultReduceLineSend", r.reduceLineSend);
		nl.propConfig.setProperty("createroom.defaultGarbageChangePerAttack", r.garbageChangePerAttack);
		nl.propConfig.setProperty("createroom.defaultDivideChangeRateByPlayers", r.divideChangeRateByPlayers);
		nl.propConfig.setProperty("createroom.defaultB2BChunk", r.b2bChunk);
		nl.propConfig.setProperty("createroom.defaultUseFractionalGarbage", r.useFractionalGarbage);
		nl.propConfig.setProperty("createroom.defaultIsTarget", r.isTarget);
		nl.propConfig.setProperty("createroom.defaultAutoStartTNET2", r.autoStartTNET2);
		nl.propConfig.setProperty("createroom.defaultDisableTimerAfterSomeoneCancelled", r.disableTimerAfterSomeoneCancelled);
		nl.propConfig.setProperty("createroom.defaultUseMap", r.useMap);
	}

	private void cancel() {
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY);
	}

	@Override
	public void render() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;

		NormalFontSDL.printFont(16, 8, detailMode ? "ROOM DETAIL" : "CREATE ROOM", NormalFontSDL.COLOR_CYAN);
		tabStrip.render();

		// Render labels + widgets for the active tab.
		Field[] tab = activeTab();
		for(int i = 0; i < tab.length; i++) {
			Field f = tab[i];
			if(f.label.length() > 0) {
				NormalFontSDL.printFont(16, 76 + i * 26 + 3, NormalFontSDL.safeString(f.label),
						NormalFontSDL.COLOR_WHITE);
			}
			f.widget.render();
		}

		// Button row
		okBtn.render();
		joinBtn.render();
		watchBtn.render();
		cancelBtn.render();

		// Open dropdowns render on top of everything else.
		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		for(Field f : tab) {
			if(f.widget instanceof DropdownSDL) ((DropdownSDL)f.widget).renderOverlay(mx, my);
		}

		if(statusLine.length() > 0) {
			NormalFontSDL.printFont(16, 466, NormalFontSDL.safeString(statusLine), NormalFontSDL.COLOR_RED);
		}
	}
}

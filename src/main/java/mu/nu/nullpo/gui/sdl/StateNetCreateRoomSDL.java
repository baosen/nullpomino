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

import org.apache.log4j.Logger;

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
	private static final Logger log = Logger.getLogger(StateNetCreateRoomSDL.class);

	private static final String[] TAB_LABELS = { "BASIC", "SPEED", "BONUS", "GARBAGE", "MISC", "PRESET" };

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
	private SpinnerSDL mapSetID;

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

	// PRESET
	private SpinnerSDL presetID;
	private ButtonSDL presetSaveBtn;
	private ButtonSDL presetLoadBtn;
	private TextInputSDL presetCodeInput;
	private ButtonSDL presetExportBtn;
	private ButtonSDL presetImportBtn;

	// Bottom button row
	private ButtonSDL okBtn;
	private ButtonSDL joinBtn;
	private ButtonSDL watchBtn;
	private ButtonSDL cancelBtn;
	/** Rated-mode only: convert the current preset selection into an editable custom room. */
	private ButtonSDL customRatedBtn;

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
	private boolean ratedMode;   // true when building a rated room (preset required)
	private String statusLine = "";

	/** Preset dropdown used in rated mode; shown only on the BASIC tab when rated. */
	private DropdownSDL presetDropdown;

	@Override
	public void enter() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_TITLE); return; }

		detailMode = nl.currentViewDetailRoomID != -1;
		ratedMode  = !detailMode && nl.createRoomRated;
		NetRoomInfo source = resolveSource(nl);

		tabStrip = new TabStripSDL(8, 32, 624, 28, TAB_LABELS);
		tabStrip.setActiveTab(0);

		buildWidgets(nl, source);
		applyDetailModeEnabled();

		if(ratedMode) {
			// Ask the server for the current style's rated-room presets; the
			// response arrives asynchronously and will populate presetDropdown.
			nl.presets.clear();
			nl.presetsDirty = false;
			if(nl.netPlayerClient != null && nl.netPlayerClient.isConnected()) {
				nl.netPlayerClient.send("getpresets\t" + nl.createRoomStyle + "\n");
			}
			presetDropdown = new DropdownSDL(216, 76 + 26 * 6, 400, 22);
			refreshPresetDropdown();
		}

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
		// Layout: labels at x=16, widgets at x=216 (12-char label + 8 px gap),
		// each row 26 px tall, first row y=76.
		final int colL = 16, colR = 216, rowY = 76, rowH = 26;
		final int wFull = 400, wShort = 180;

		// BASIC
		roomName = new TextInputSDL(colR, rowY + rowH * 0, wFull, 22);
		roomName.maxChars = 64;
		roomName.setText(src.strName);

		modeDropdown = new DropdownSDL(colR, rowY + rowH * 1, wFull, 22, loadModeList());
		setDropdownSelection(modeDropdown, src.strMode);

		maxPlayers       = new SpinnerSDL(colR, rowY + rowH * 2, wShort, 22, 1, 6, 1, src.maxPlayers);
		autoStartSeconds = new SpinnerSDL(colR, rowY + rowH * 3, wShort, 22, 0, 600, 1, src.autoStartSeconds);
		useMap    = new CheckboxSDL(colR, rowY + rowH * 4, wFull, 22, "USE MAP",   src.useMap);
		mapSetID  = new SpinnerSDL (colR, rowY + rowH * 5, wShort, 22, 0, 99, 1,
				nl.propConfig.getProperty("createroom.defaultMapSetID", 0));
		ruleLock  = new CheckboxSDL(colR, rowY + rowH * 6, wFull, 22, "RULE LOCK", src.ruleLock);

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

		// PRESET tab: save/load current form state from numbered slots in
		// netlobby.cfg, plus a text-based preset-code for sharing settings.
		presetID        = new SpinnerSDL(colR, rowY + rowH * 0, wShort, 22, 0, 99, 1,
				nl.propConfig.getProperty("createroom.defaultPresetID", 0));
		presetSaveBtn   = new ButtonSDL(colR,        rowY + rowH * 1, 140, 22, "SAVE SLOT",
				new Runnable() { public void run() { saveCurrentAsPreset(); } });
		presetLoadBtn   = new ButtonSDL(colR + 152,  rowY + rowH * 1, 140, 22, "LOAD SLOT",
				new Runnable() { public void run() { loadCurrentPreset(); } });
		presetCodeInput = new TextInputSDL(colR, rowY + rowH * 3, wFull, 22);
		presetCodeInput.maxChars = 2048;
		presetExportBtn = new ButtonSDL(colR,        rowY + rowH * 4, 140, 22, "EXPORT",
				new Runnable() { public void run() { exportPresetCode(); } });
		presetImportBtn = new ButtonSDL(colR + 152,  rowY + rowH * 4, 140, 22, "IMPORT",
				new Runnable() { public void run() { importPresetCode(); } });

		// Bottom button row — OK (create) aligned with the tab strip's left
		// edge, CANCEL with its right edge; JOIN / WATCH fill the middle in
		// detail-view mode.
		int btnY = 424;
		okBtn     = new ButtonSDL(  8, btnY, 124, 32, "OK",     new Runnable() { public void run() { submit(false, false); } });
		okBtn.primary = true;
		joinBtn   = new ButtonSDL(140, btnY, 124, 32, "JOIN",   new Runnable() { public void run() { submit(true,  false); } });
		joinBtn.primary = true;
		watchBtn  = new ButtonSDL(272, btnY, 124, 32, "WATCH",  new Runnable() { public void run() { submit(true,  true); } });
		// In rated mode, CUSTOM sits where JOIN/WATCH would live in detail mode.
		customRatedBtn = new ButtonSDL(140, btnY, 160, 32, "CUSTOMIZE",
				new Runnable() { public void run() { flipRatedToCustom(); } });
		cancelBtn = new ButtonSDL(508, btnY, 124, 32, "CANCEL", new Runnable() { public void run() { cancel(); } });

		// Build the per-tab widget arrays in the order they appear on screen.
		tabFields = new Field[][] {
			// BASIC
			{
				new Field("ROOM NAME",    roomName),
				new Field("MODE",         modeDropdown),
				new Field("MAX PLAYERS",  maxPlayers),
				new Field("AUTOSTART S",  autoStartSeconds),
				new Field("",             useMap),
				new Field("MAP SET ID",   mapSetID),
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
			// PRESET
			{
				new Field("PRESET ID",    presetID),
				new Field("",             presetSaveBtn),
				new Field("",             presetLoadBtn),
				new Field("PRESET CODE",  presetCodeInput),
				new Field("",             presetExportBtn),
				new Field("",             presetImportBtn),
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
		customRatedBtn.visible = ratedMode && !detailMode;
	}

	/**
	 * Switch out of rated-preset mode: keep everything the user has typed so
	 * far, seed the form from the currently-selected preset, and continue
	 * editing as a regular custom room.
	 */
	private void flipRatedToCustom() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;
		if(presetDropdown != null && !nl.presets.isEmpty()) {
			int idx = Math.max(0, presetDropdown.getSelectedIndex());
			if(idx < nl.presets.size()) applyRoomInfoToForm(nl.presets.get(idx));
		}
		nl.createRoomRated = false;
		ratedMode = false;
		presetDropdown = null;
		applyDetailModeEnabled();
	}

	/**
	 * Load mode names from {@code config/list/netlobby_{multi,single}mode.lst}.
	 * The file groups modes under {@code :STYLE} section headers and each mode
	 * line is {@code "modeName,isRace"}. Section markers are skipped and the
	 * isRace suffix is stripped before returning.
	 */
	private String[] loadModeList() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		String file = (nl != null && nl.createRoomSinglePlayer)
				? "config/list/netlobby_singlemode.lst"
				: "config/list/netlobby_multimode.lst";
		List<String> list = new ArrayList<String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(file));
			String line;
			while((line = in.readLine()) != null) {
				line = line.trim();
				if(line.length() == 0 || line.startsWith("#") || line.startsWith(":")) continue;
				int comma = line.indexOf(',');
				list.add(comma == -1 ? line : line.substring(0, comma));
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

	/**
	 * Find {@code item} in the dropdown's current contents and select it.  Falls
	 * back to the first item if no match.  Walks a snapshot of the list instead
	 * of mutating selection along the way (which would clamp past the last
	 * index and never terminate the search).
	 */
	private static void setDropdownSelection(DropdownSDL d, String item) {
		if(item == null || item.length() == 0) { d.setSelectedIndex(0); return; }
		int saved = d.getSelectedIndex();
		for(int i = 0; ; i++) {
			d.setSelectedIndex(i);
			if(d.getSelectedIndex() != i) break;    // clamped — past the end
			if(item.equals(d.getSelectedItem())) return;
		}
		d.setSelectedIndex(saved < 0 ? 0 : saved);
	}

	private void setFocus(WidgetSDL w) {
		if(focused == w) return;
		if(focused != null) focused.setFocused(false);
		focused = w;
		if(focused != null) focused.setFocused(true);
	}

	private Field[] activeTab() { return tabFields[tabStrip.getActiveTab()]; }

	/** Rebuild preset dropdown items from {@code nl.presets}. */
	private void refreshPresetDropdown() {
		if(presetDropdown == null) return;
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;
		String[] labels = new String[nl.presets.size()];
		for(int i = 0; i < nl.presets.size(); i++) {
			NetRoomInfo p = nl.presets.get(i);
			labels[i] = (p.strName != null && p.strName.length() > 0) ? p.strName : ("PRESET " + i);
		}
		presetDropdown.setItems(labels);
		if(labels.length > 0) presetDropdown.setSelectedIndex(0);
	}

	@Override
	public void update() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) { NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_TITLE); return; }
		nl.pump();
		MouseInputSDL.mouseInput.update();

		// Rated preset response arrived — repopulate the dropdown.
		if(ratedMode && nl.presetsDirty) {
			nl.presetsDirty = false;
			refreshPresetDropdown();
		}

		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		boolean clicked = MouseInputSDL.mouseInput.isMouseClicked();

		// Tab switching — click on strip, or auto-handled by tab label below.
		if(tabStrip.update(mx, my, clicked)) setFocus(activeTab()[0].widget);

		// Update all widgets on the active tab so their hover states stay live.
		for(Field f : activeTab()) {
			if(f.widget.update(mx, my, clicked)) setFocus(f.widget);
		}
		// Rated mode: the preset dropdown lives on the BASIC tab below the form.
		if(ratedMode && presetDropdown != null && tabStrip.getActiveTab() == 0) {
			if(presetDropdown.update(mx, my, clicked)) setFocus(presetDropdown);
		}

		// Button row always visible
		okBtn.update(mx, my, clicked);
		joinBtn.update(mx, my, clicked);
		watchBtn.update(mx, my, clicked);
		customRatedBtn.update(mx, my, clicked);
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
		if(customRatedBtn.visible) list.add(customRatedBtn);
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
		if(customRatedBtn.visible) row.add(customRatedBtn);
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

		String msg;
		if(ratedMode) {
			// ratedroomcreate\t<name>\t<maxPlayers>\t<presetIndex>\t<mode>
			if(r.strName == null || r.strName.trim().length() == 0) { statusLine = "ROOM NAME REQUIRED"; return; }
			if(presetDropdown == null || nl.presets.isEmpty()) { statusLine = "NO PRESETS AVAILABLE"; return; }
			int presetIdx = Math.max(0, presetDropdown.getSelectedIndex());
			String name = NetUtil.urlEncode(r.strName);
			String mode = NetUtil.urlEncode(r.strMode == null ? "" : r.strMode);
			msg = "ratedroomcreate\t" + name + "\t" + r.maxPlayers + "\t" + presetIdx + "\t" + mode + "\n";
		} else if(nl.createRoomSinglePlayer) {
			// singleroomcreate\t<name>\t<mode> — server fills the rest from the player's rule.
			if(r.strName == null || r.strName.trim().length() == 0) { statusLine = "ROOM NAME REQUIRED"; return; }
			String name = NetUtil.urlEncode(r.strName);
			String mode = NetUtil.urlEncode(r.strMode == null ? "" : r.strMode);
			msg = "singleroomcreate\t" + name + "\t" + mode + "\n";
		} else {
			msg = buildRoomCreateMessage(r, mapSetID.getValue());
			if(msg == null) { statusLine = "ROOM NAME REQUIRED"; return; }
		}

		nl.backupRoomInfo = r;
		// Persist the defaults so the user's tuning survives reconnects.
		saveDefaultsToConfig(nl, r);
		saveMapSetIDDefault(nl);
		nl.saveConfig();
		nl.netPlayerClient.send(msg);
		nl.createRoomSinglePlayer = false;
		nl.createRoomRated = false;
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
	 * Build the roomcreate protocol string in the format the server expects:
	 * {@code roomcreate\t<name>\t<export>\t<mode>[\t<compressedMaps>]\n}.
	 * When {@code useMap} is set, the map-set file at
	 * {@code config/map/vsbattle/<mapSetID>.map} is loaded, its entries joined
	 * with tabs, and appended as the compressed 5th field.  Also caches the
	 * map list on the session so the game mode can reference it later.
	 */
	private String buildRoomCreateMessage(NetRoomInfo r, int mapSetID) {
		if(r.strName == null || r.strName.trim().length() == 0) return null;
		String name = NetUtil.urlEncode(r.strName);
		String export = NetUtil.urlEncode(r.exportString());
		String mode = NetUtil.urlEncode(r.strMode == null ? "" : r.strMode);
		StringBuilder sb = new StringBuilder("roomcreate\t");
		sb.append(name).append('\t').append(export).append('\t').append(mode);

		if(r.useMap) {
			String compressedMaps = loadAndCompressMapSet(mapSetID);
			if(compressedMaps != null) sb.append('\t').append(compressedMaps);
			else r.useMap = false;  // silently disable useMap if we can't find the file
		}
		sb.append('\n');
		return sb.toString();
	}

	/**
	 * Load the numbered map set file and return a Deflate-compressed
	 * tab-joined string of all its {@code map.N} entries, or null if the
	 * file is missing/empty.  Also populates
	 * {@link NetLobbyFrame#mapList} so the game mode can pull maps later.
	 */
	private static String loadAndCompressMapSet(int mapSetID) {
		String path = "config/map/vsbattle/" + mapSetID + ".map";
		mu.nu.nullpo.util.CustomProperties propMap = new mu.nu.nullpo.util.CustomProperties();
		java.io.FileInputStream in = null;
		try {
			in = new java.io.FileInputStream(path);
			propMap.load(in);
		} catch(java.io.IOException e) {
			return null;
		} finally {
			if(in != null) try { in.close(); } catch(java.io.IOException ignore) {}
		}

		int maxMap = propMap.getProperty("map.maxMapNumber", 0);
		if(maxMap <= 0) return null;

		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl != null) nl.mapList.clear();

		StringBuilder strMap = new StringBuilder();
		for(int i = 0; i < maxMap; i++) {
			String m = propMap.getProperty("map." + i, "");
			if(nl != null) nl.mapList.add(m);
			strMap.append(m);
			if(i < maxMap - 1) strMap.append('\t');
		}
		if(strMap.length() == 0) return null;
		return NetUtil.compressString(strMap.toString());
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
		// mapSetID is written by the caller of this helper — it lives on the state, not NetRoomInfo.
	}

	/** Persist the map-set spinner separately since it's not part of NetRoomInfo. */
	private void saveMapSetIDDefault(NetLobbyFrame nl) {
		nl.propConfig.setProperty("createroom.defaultMapSetID", mapSetID.getValue());
	}

	private void cancel() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl != null) {
			nl.createRoomSinglePlayer = false;
			nl.createRoomRated = false;
		}
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY);
	}

	// ---------------- Preset slots ----------------

	/**
	 * Capture the current form into a NetRoomInfo, compress its export string,
	 * and store under {@code 0.preset.<id>} in netlobby.cfg.  One-shot preset
	 * save identical to the Swing lobby's behaviour.
	 */
	private void saveCurrentAsPreset() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;
		NetRoomInfo r = new NetRoomInfo();
		collectFormInto(r);
		int id = presetID.getValue();
		nl.propConfig.setProperty("0.preset." + id, NetUtil.compressString(r.exportString()));
		nl.propConfig.setProperty("createroom.defaultPresetID", id);
		nl.saveConfig();
		statusLine = "SAVED TO SLOT " + id;
	}

	/**
	 * Read the preset saved under the current slot ID, decompress it into a
	 * NetRoomInfo, and repopulate every widget on the form.
	 */
	private void loadCurrentPreset() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;
		int id = presetID.getValue();
		String stored = nl.propConfig.getProperty("0.preset." + id);
		if(stored == null || stored.length() == 0) { statusLine = "SLOT " + id + " EMPTY"; return; }
		try {
			String decoded = NetUtil.decompressString(stored);
			NetRoomInfo r = new NetRoomInfo(decoded);
			applyRoomInfoToForm(r);
			nl.propConfig.setProperty("createroom.defaultPresetID", id);
			statusLine = "LOADED SLOT " + id;
		} catch(Exception e) {
			log.error("Failed to load preset " + id, e);
			statusLine = "SLOT " + id + " INVALID";
		}
	}

	/** Put the current form state into the presetCode input as a shareable base64 string. */
	private void exportPresetCode() {
		NetRoomInfo r = new NetRoomInfo();
		collectFormInto(r);
		String code = NetUtil.compressString(r.exportString());
		presetCodeInput.setText(code);
		statusLine = "CODE EXPORTED";
	}

	/** Take the presetCode input, validate, decompress, and apply to the form. */
	private void importPresetCode() {
		String code = presetCodeInput.getText().replaceAll("[^a-zA-Z0-9+/=]", "");
		if(code.length() == 0) { statusLine = "NO CODE TO IMPORT"; return; }
		try {
			String decoded = NetUtil.decompressString(code);
			NetRoomInfo r = new NetRoomInfo(decoded);
			applyRoomInfoToForm(r);
			statusLine = "CODE IMPORTED";
		} catch(Exception e) {
			log.error("Failed to import preset code", e);
			statusLine = "INVALID PRESET CODE";
		}
	}

	/** Copy every field of {@code r} back into the widget set. */
	private void applyRoomInfoToForm(NetRoomInfo r) {
		roomName.setText(r.strName == null ? "" : r.strName);
		setDropdownSelection(modeDropdown, r.strMode);
		maxPlayers.setValue(r.maxPlayers);
		autoStartSeconds.setValue(r.autoStartSeconds);
		useMap.checked = r.useMap;
		ruleLock.checked = r.ruleLock;
		gravity.setValue(r.gravity);
		denominator.setValue(r.denominator);
		are.setValue(r.are);
		areLine.setValue(r.areLine);
		lineDelay.setValue(r.lineDelay);
		lockDelay.setValue(r.lockDelay);
		das.setValue(r.das);
		tspinType.setSelectedIndex(r.tspinEnableType);
		spinCheck.setSelectedIndex(r.spinCheckType);
		ezTSpin.checked = r.tspinEnableEZ;
		b2b.checked = r.b2b;
		combo.checked = r.combo;
		rensaBlock.checked = r.rensaBlock;
		counter.checked = r.counter;
		bravo.checked = r.bravo;
		garbagePercent.setValue(r.garbagePercent);
		targetTimer.setValue(r.targetTimer);
		changePerAttack.checked = r.garbageChangePerAttack;
		divideRate.checked = r.divideChangeRateByPlayers;
		b2bChunk.checked = r.b2bChunk;
		fractGarbage.checked = r.useFractionalGarbage;
		target.checked = r.isTarget;
		reduceLineSend.checked = r.reduceLineSend;
		hurryupSeconds.setValue(r.hurryupSeconds);
		hurryupInterval.setValue(r.hurryupInterval);
		tnet2Timer.checked = r.autoStartTNET2;
		disableAfterCancel.checked = r.disableTimerAfterSomeoneCancelled;
	}

	@Override
	public void render() {
		NetLobbyFrame nl = NullpoMinoSDL.netLobby;
		if(nl == null) return;

		String title;
		if(detailMode) title = "ROOM DETAIL";
		else if(ratedMode) title = "CREATE RATED ROOM";
		else if(nl.createRoomSinglePlayer) title = "CREATE 1P ROOM";
		else title = "CREATE ROOM";
		NormalFontSDL.printFont(16, 8, title, NormalFontSDL.COLOR_CYAN);
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

		// Rated mode: show the preset dropdown + loading status on the BASIC tab.
		if(ratedMode && tabStrip.getActiveTab() == 0 && presetDropdown != null) {
			NormalFontSDL.printFont(16, 76 + 26 * 6 + 3, "PRESET", NormalFontSDL.COLOR_WHITE);
			if(nl.presets.isEmpty()) {
				NormalFontSDL.printFont(216, 76 + 26 * 6 + 3,
						"WAITING FOR PRESETS...", NormalFontSDL.COLOR_YELLOW);
			} else {
				presetDropdown.render();
			}
		}

		// Button row
		okBtn.render();
		joinBtn.render();
		watchBtn.render();
		customRatedBtn.render();
		cancelBtn.render();

		// Open dropdowns render on top of everything else.
		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		for(Field f : tab) {
			if(f.widget instanceof DropdownSDL) ((DropdownSDL)f.widget).renderOverlay(mx, my);
		}
		if(ratedMode && tabStrip.getActiveTab() == 0 && presetDropdown != null && !nl.presets.isEmpty()) {
			presetDropdown.renderOverlay(mx, my);
		}

		if(statusLine.length() > 0) {
			NormalFontSDL.printFont(16, 466, NormalFontSDL.safeString(statusLine), NormalFontSDL.COLOR_RED);
		}
	}
}

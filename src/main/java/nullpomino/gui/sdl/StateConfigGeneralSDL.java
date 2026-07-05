// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

/**
 * State of the general settings screen
 */
public class StateConfigGeneralSDL extends DummyMenuScrollStateSDL {
	/** UI Text identifier Strings */
	protected static final String[] UI_TEXT = {
		"ConfigGeneral_SE",
		"ConfigGeneral_BGM",
		"ConfigGeneral_BGMPreload",
		"ConfigGeneral_SEVolume",
		"ConfigGeneral_BGMVolume",
		"ConfigGeneral_Background",
		"ConfigGeneral_UseBackgroundFade",
		"ConfigGeneral_ShowLineEffect",
		"ConfigGeneral_LineEffectSpeed",
		"ConfigGeneral_ShowMeter",
		"ConfigGeneral_DarkNextArea",
		"ConfigGeneral_NextShadow",
		"ConfigGeneral_NextType",
		"ConfigGeneral_OutlineGhost",
		"ConfigGeneral_FieldBGBright",
		"ConfigGeneral_ShowFieldBGGrid",
		"ConfigGeneral_ShowInput",
		"ConfigGeneral_Fullscreen",
		"ConfigGeneral_ShowFPS",
		"ConfigGeneral_MaxFPS",
		"ConfigGeneral_FrameStep",
		"ConfigGeneral_PerfectFPS",
		"ConfigGeneral_PerfectYield",
		"ConfigGeneral_SoundBufferSize",
		"ConfigGeneral_SoundChannels",
	};

	/** Piece preview type options */
	protected static final String[] NEXTTYPE_OPTIONS = {"TOP", "SIDE(SMALL)", "SIDE(BIG)"};

	/** Cursor positions to jump between on Page Up / Page Down */
	protected static final int[] PAGE_BOUNDARIES = {0, 17, 23};

	/** Full screen flag */
	protected boolean fullscreen;

	/** Sound effectsON/OFF */
	protected boolean se;

	/** BGMOfON/OFF */
	protected boolean bgm;

	/** BGMPreloading of */
	protected boolean bgmpreload;

	/** BackgroundDisplay */
	protected boolean showbg;

	/** FPSDisplay */
	protected boolean showfps;

	/**  frame Step is enabled */
	protected boolean enableframestep;

	/** MaximumFPS */
	protected int maxfps;

	/** Line clearDisplay Effects */
	protected boolean showlineeffect;

	/** Line clear effect speed */
	protected int lineeffectspeed;

	/** Sound buffer size */
	protected int soundbuffer;

	/** Heavy production use */
	protected boolean heavyeffect;

	/** fieldBackgroundThe brightness of the */
	protected int fieldbgbright;

	/** Show field BG grid */
	protected boolean showfieldbggrid;

	/** NEXTDarken the field */
	protected boolean darknextarea;

	/** Sound effects volume */
	protected int sevolume;

	/** BGM volume */
	protected int bgmvolume;

	/** You can play simultaneouslySound effectsOfcount */
	protected int soundChannels;

	/** fieldTo the rightMeterShow */
	protected boolean showmeter;

	/** ghost On top of the pieceNEXTDisplay */
	protected boolean nextshadow;

	/** Linear frameghost Peace */
	protected boolean outlineghost;

	/** Piece preview type (0=Top 1=Side small 2=Side big) */
	protected int nexttype;

	/** True to use perfect FPS */
	protected boolean perfectFPSMode;

	/** Execute Thread.yield() during Perfect FPS mode */
	protected boolean perfectYield;

	/** Show player input */
	protected boolean showInput;

	/** Last fullscreen state applied to the SDL window */
	protected boolean lastRuntimeFullscreen;

	/**
	 * Constructor
	 */
	public StateConfigGeneralSDL() {
		pageHeight = 23;
		maxCursor = 24;
		cursor = 0;
		loadConfig(NullpoMinoSDL.propConfig);
		rebuildList();
	}

	@Override
	public void enter() {
		loadConfig(NullpoMinoSDL.propConfig);
		syncRuntimeFullscreen(true);
		rebuildList();
	}

	/**
	 * Load settings
	 * @param prop Property file to read from
	 */
	protected void loadConfig(CustomProperties prop) {
		fullscreen = prop.getProperty("option.fullscreen", false);
		se = prop.getProperty("option.se", true);
		bgm = prop.getProperty("option.bgm", false);
		bgmpreload = prop.getProperty("option.bgmpreload", false);
		showbg = prop.getProperty("option.showbg", true);
		showfps = prop.getProperty("option.showfps", false);
		enableframestep = prop.getProperty("option.enableframestep", false);
		maxfps = prop.getProperty("option.maxfps", 60);
		showlineeffect = prop.getProperty("option.showlineeffect", true);
		lineeffectspeed = prop.getProperty("option.lineeffectspeed", 0);
		soundbuffer = prop.getProperty("option.soundbuffer", 1024);
		heavyeffect = prop.getProperty("option.heavyeffect", false);
		fieldbgbright = prop.getProperty("option.fieldbgbright", 128);
		showfieldbggrid = prop.getProperty("option.showfieldbggrid", true);
		darknextarea = prop.getProperty("option.darknextarea", true);
		sevolume = prop.getProperty("option.sevolume", 128);
		bgmvolume = prop.getProperty("option.bgmvolume", 128);
		soundChannels = prop.getProperty("option.soundChannels", 15);
		showmeter = prop.getProperty("option.showmeter", true);
		nextshadow = prop.getProperty("option.nextshadow", false);
		outlineghost = prop.getProperty("option.outlineghost", false);
		perfectFPSMode = prop.getProperty("option.perfectFPSMode", false);
		perfectYield = prop.getProperty("option.perfectYield", false);
		showInput = prop.getProperty("option.showInput", false);
		boolean side = prop.getProperty("option.sidenext", false);
		boolean big = prop.getProperty("option.bigsidenext", false);
		nexttype = side ? (big ? 2 : 1) : 0;
		lastRuntimeFullscreen = fullscreen;
	}

	/**
	 * Keep the menu in sync with live fullscreen changes such as F11.
	 * Pending menu edits are discarded when the runtime state changes.
	 * @param force true to always resync from runtime state
	 */
	protected void syncRuntimeFullscreen(boolean force) {
		boolean runtimeFullscreen = NullpoMinoSDL.fullscreen;
		if(force || (runtimeFullscreen != lastRuntimeFullscreen)) {
			fullscreen = runtimeFullscreen;
			lastRuntimeFullscreen = runtimeFullscreen;
		}
	}

	/**
	 * Save settings
	 * @param prop Property file to save to
	 */
	protected void saveConfig(CustomProperties prop) {
		prop.setProperty("option.fullscreen", fullscreen);
		prop.setProperty("option.se", se);
		prop.setProperty("option.bgm", bgm);
		prop.setProperty("option.bgmpreload", bgmpreload);
		prop.setProperty("option.showbg", showbg);
		prop.setProperty("option.showfps", showfps);
		prop.setProperty("option.enableframestep", enableframestep);
		prop.setProperty("option.maxfps", maxfps);
		prop.setProperty("option.showlineeffect", showlineeffect);
		prop.setProperty("option.lineeffectspeed", lineeffectspeed);
		prop.setProperty("option.soundbuffer", soundbuffer);
		prop.setProperty("option.heavyeffect", heavyeffect);
		prop.setProperty("option.fieldbgbright", fieldbgbright);
		prop.setProperty("option.showfieldbggrid", showfieldbggrid);
		prop.setProperty("option.darknextarea", darknextarea);
		prop.setProperty("option.sevolume", sevolume);
		prop.setProperty("option.bgmvolume", bgmvolume);
		prop.setProperty("option.soundChannels", soundChannels);
		prop.setProperty("option.showmeter", showmeter);
		prop.setProperty("option.nextshadow", nextshadow);
		prop.setProperty("option.outlineghost", outlineghost);
		prop.setProperty("option.perfectFPSMode", perfectFPSMode);
		prop.setProperty("option.perfectYield", perfectYield);
		prop.setProperty("option.showInput", showInput);
		prop.setProperty("option.sidenext", nexttype >= 1);
		prop.setProperty("option.bigsidenext", nexttype >= 2);
	}

	/**
	 * Rebuild {@link #list} with the current option values. Called each frame
	 * from {@link #render()} so the on-screen labels track live edits made via
	 * onChange without any per-case refresh code.
	 */
	protected void rebuildList() {
		list = new String[] {
			"SE:" + GeneralUtil.getOorX(se),
			"BGM:" + GeneralUtil.getOorX(bgm),
			"BGM PRELOAD:" + GeneralUtil.getOorX(bgmpreload),
			"SE VOLUME:" + sevolume + "(" + (sevolume * 100 / 128) + "%)",
			"BGM VOLUME:" + bgmvolume + "(" + (bgmvolume * 100 / 128) + "%)",
			"SHOW BACKGROUND:" + GeneralUtil.getOorX(showbg),
			"USE BACKGROUND FADE:" + GeneralUtil.getOorX(heavyeffect),
			"SHOW LINE EFFECT:" + GeneralUtil.getOorX(showlineeffect),
			"LINE EFFECT SPEED:X " + (lineeffectspeed + 1),
			"SHOW METER:" + GeneralUtil.getOorX(showmeter),
			"DARK NEXT AREA:" + GeneralUtil.getOorX(darknextarea),
			"SHOW NEXT ABOVE SHADOW:" + GeneralUtil.getOorX(nextshadow),
			"NEXT DISPLAY TYPE:" + NEXTTYPE_OPTIONS[nexttype],
			"OUTLINE GHOST PIECE:" + GeneralUtil.getOorX(outlineghost),
			"FIELD BG BRIGHT:" + fieldbgbright + "(" + (fieldbgbright * 100 / 255) + "%)",
			"SHOW FIELD BG GRID:" + GeneralUtil.getOorX(showfieldbggrid),
			"SHOW CONTROLLER INPUT:" + GeneralUtil.getOorX(showInput),
			"FULLSCREEN:" + GeneralUtil.getOorX(fullscreen),
			"SHOW FPS:" + GeneralUtil.getOorX(showfps),
			"MAX FPS:" + maxfps,
			"FRAME STEP:" + GeneralUtil.getOorX(enableframestep),
			"FPS PERFECT MODE:" + GeneralUtil.getOorX(perfectFPSMode),
			"FPS PERFECT YIELD:" + GeneralUtil.getOorX(perfectYield),
			"SOUND BUFFER SIZE:" + soundbuffer,
			"MAX SOUND CHANNELS:" + soundChannels,
		};
	}

	@Override
	public void render() {
		rebuildList();
		super.render();
	}

	@Override
	protected void drawRow(int row, int y) {
		// Rows embed font-sprite characters (getOorX returns 'c'/'e' glyphs
		// that render as O/X symbols), so we must not upper-case the label.
		NormalFontSDL.printFontGrid(2, 3 + y, list[row], (cursor == row));
		if(cursor == row) NormalFontSDL.printFontGrid(1, 3 + y, "b", NormalFontSDL.COLOR_RED);
	}

	@Override
	protected void onRenderSuccess() {
		NormalFontSDL.printFontGrid(1, 1, "GENERAL OPTIONS (" + (cursor + 1) + "/" + list.length + ")", NormalFontSDL.COLOR_ORANGE);
		if(cursor >= 0 && cursor < UI_TEXT.length) {
			NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText(UI_TEXT[cursor]));
		}
	}

	@Override
	public void update() {
		syncRuntimeFullscreen(false);
		super.update();
	}

	@Override
	public boolean updateMouseInput() {
		// Inherit cursor clicks, scroll-bar drag, wheel, page-clicks. A row
		// click forwards to onChange(+1) — same as pressing RIGHT — so a
		// click toggles booleans and bumps integers on the clicked row.
		// Always return false so BUTTON_A keeps exclusive ownership of the
		// save-and-exit path, preventing a stray click from committing
		// every pending edit.
		if (super.updateMouseInput()) {
			onChange(1);
		}
		return false;
	}

	/**
	 * Persist the current in-memory option values to propConfig (and disk)
	 * and propagate them to the runtime fields so each edit takes effect the
	 * moment it happens — no BUTTON_A commit required. Called from onChange
	 * after mutating a single option.
	 */
	protected void applyAndSave() {
		// Snapshot settings that need change detection before saving
		boolean prevShowLineEffect = NullpoMinoSDL.propConfig.getProperty("option.showlineeffect", true);
		boolean prevShowBg = NullpoMinoSDL.propConfig.getProperty("option.showbg", true);

		saveConfig(NullpoMinoSDL.propConfig);
		NullpoMinoSDL.saveConfig();

		NullpoMinoSDL.showfps = showfps;
		NullpoMinoSDL.maxFPS = maxfps;
		NullpoMinoSDL.perfectFPSMode = perfectFPSMode;
		NullpoMinoSDL.perfectYield = perfectYield;

		// Apply fullscreen change at runtime
		if(fullscreen != NullpoMinoSDL.fullscreen) {
			NullpoMinoSDL.toggleFullscreen();
			lastRuntimeFullscreen = NullpoMinoSDL.fullscreen;
		}

		ResourceHolderSDL.soundManager.changeVolume(sevolume);
		if(showlineeffect && !prevShowLineEffect) ResourceHolderSDL.loadLineClearEffectImages();
		if(showbg && !prevShowBg) ResourceHolderSDL.loadBackgroundImages();
	}

	@Override
	protected void onChange(int change) {
		ResourceHolderSDL.soundManager.play("change");

		switch(cursor) {
		case 0:
			se = !se;
			break;
		case 1:
			bgm = !bgm;
			break;
		case 2:
			bgmpreload = !bgmpreload;
			break;
		case 3:
			sevolume += change;
			if(sevolume < 0) sevolume = 128;
			if(sevolume > 128) sevolume = 0;
			break;
		case 4:
			bgmvolume += change;
			if(bgmvolume < 0) bgmvolume = 128;
			if(bgmvolume > 128) bgmvolume = 0;
			break;
		case 5:
			showbg = !showbg;
			break;
		case 6:
			heavyeffect = !heavyeffect;
			break;
		case 7:
			showlineeffect = !showlineeffect;
			break;
		case 8:
			lineeffectspeed += change;
			if(lineeffectspeed < 0) lineeffectspeed = 9;
			if(lineeffectspeed > 9) lineeffectspeed = 0;
			break;
		case 9:
			showmeter = !showmeter;
			break;
		case 10:
			darknextarea = !darknextarea;
			break;
		case 11:
			nextshadow = !nextshadow;
			break;
		case 12:
			nexttype += change;
			if(nexttype < 0) nexttype = 2;
			if(nexttype > 2) nexttype = 0;
			break;
		case 13:
			outlineghost = !outlineghost;
			break;
		case 14:
			fieldbgbright += change;
			if(fieldbgbright < 0) fieldbgbright = 255;
			if(fieldbgbright > 255) fieldbgbright = 0;
			break;
		case 15:
			showfieldbggrid = !showfieldbggrid;
			break;
		case 16:
			showInput = !showInput;
			break;
		case 17:
			fullscreen = !fullscreen;
			break;
		case 18:
			showfps = !showfps;
			break;
		case 19:
			maxfps += change;
			if(maxfps < 0) maxfps = 99;
			if(maxfps > 99) maxfps = 0;
			break;
		case 20:
			enableframestep = !enableframestep;
			break;
		case 21:
			perfectFPSMode = !perfectFPSMode;
			break;
		case 22:
			perfectYield = !perfectYield;
			break;
		case 23:
			soundbuffer += change * 256;
			if(soundbuffer < 0) soundbuffer = 65535;
			if(soundbuffer > 65535) soundbuffer = 0;
			break;
		case 24:
			soundChannels += change;
			if(soundChannels < 0) soundChannels = 50;
			if(soundChannels > 50) soundChannels = 0;
			break;
		}

		applyAndSave();
	}

	@Override
	protected void onPageEvent(int direction) {
		if(direction == 1) {
			for(int i = 0; i < PAGE_BOUNDARIES.length; i++) {
				if(PAGE_BOUNDARIES[i] > cursor) {
					cursor = PAGE_BOUNDARIES[i];
					ResourceHolderSDL.soundManager.play("cursor");
					return;
				}
			}
		} else if(direction == -1) {
			for(int i = PAGE_BOUNDARIES.length - 1; i >= 0; i--) {
				if(PAGE_BOUNDARIES[i] < cursor) {
					cursor = PAGE_BOUNDARIES[i];
					ResourceHolderSDL.soundManager.play("cursor");
					return;
				}
			}
		}
	}

	@Override
	protected boolean onDecide() {
		ResourceHolderSDL.soundManager.play("decide");
		NullpoMinoSDL.goBack();
		return true;
	}

	@Override
	protected boolean onCancel() {
		NullpoMinoSDL.goBack();
		return true;
	}
}

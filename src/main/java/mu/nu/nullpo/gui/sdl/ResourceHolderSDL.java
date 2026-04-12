/*
    Copyright (c) 2010, NullNoname
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright
          notice, this list of conditions and the following disclaimer in the
          documentation and/or other materials provided with the distribution.
        * Neither the name of NullNoname nor the names of its
          contributors may be used to endorse or promote products derived from
          this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
*/
package mu.nu.nullpo.gui.sdl;

import java.io.File;
import java.util.LinkedList;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;

import mu.nu.nullpo.game.component.BGMStatus;
import mu.nu.nullpo.gui.sdl.binding.SDL3;
import mu.nu.nullpo.gui.sdl.binding.SDL3Image;
import mu.nu.nullpo.gui.sdl.binding.SDL3Mixer;
import mu.nu.nullpo.gui.sdl.binding.SDL3TTF;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;

import org.apache.log4j.Logger;

/**
 * Class to manage images and sounds (SDL3 version)
 */
public class ResourceHolderSDL {
	/** Log */
	static Logger log = Logger.getLogger(ResourceHolderSDL.class);

	/** Background count */
	public static final int BACKGROUND_MAX = 20;

	/** Number of images for block spatter animation during line clears */
	public static final int BLOCK_BREAK_MAX = 8;

	/** Number of image splits for block spatter animation during line clears */
	public static final int BLOCK_BREAK_SEGMENTS = 2;

	/** Number of gem block clear effects */
	public static final int PERASE_MAX = 7;

	/** Block images (SDL_Texture*) */
	public static LinkedList<Pointer> imgNormalBlockList, imgSmallBlockList, imgBigBlockList;

	/** Block sticky flag */
	public static LinkedList<Boolean> blockStickyFlagList;

	/** Regular font textures */
	public static Pointer imgFont, imgFontSmall, imgFontBig;

	/** Small image */
	public static Pointer imgSprite;

	/** Title */
	public static Pointer imgTitle;

	/** Menu background */
	public static Pointer imgMenu;

	/** Field frame */
	public static Pointer imgFrame;

	/** Field background */
	public static Pointer imgFieldbg, imgFieldbg2, imgFieldbg2Small, imgFieldbg2Big;

	/** Black and white textures (used for darkness/brightness overlays) */
	public static Pointer imgBlankBlack, imgBlankWhite;

	/** Block spatter animation during line clears */
	public static Pointer[][] imgBreak;

	/** Effects for clearing gem blocks */
	public static Pointer[] imgPErase;

	/** In play background */
	public static Pointer[] imgPlayBG;

	/** TTF font (TTF_Font*) */
	public static Pointer ttfFont;

	/** Sound effects */
	public static SoundManagerSDL soundManager;

	/** BGM audio data (MIX_Audio*) */
	public static Pointer[] bgm;

	/** BGM playback track (MIX_Track*) — single track for all BGM */
	public static Pointer bgmTrack;

	/** Current BGM number */
	public static int bgmPlaying;

	/**
	 * Load images and sound files
	 */
	public static void load() {
		String skindir = NullpoMinoSDL.propConfig.getProperty("custom.skin.directory", "res");

		log.info("Loading Image");

		// Blocks
		int numBlocks = 0;
		File file = null;
		while(true) {
			file = new File(skindir + "/graphics/blockskin/normal/n" + numBlocks + ".png");
			if(file.canRead()) {
				numBlocks++;
			} else {
				break;
			}
		}
		log.debug(numBlocks + " block skins found");

		imgNormalBlockList = new LinkedList<Pointer>();
		imgSmallBlockList = new LinkedList<Pointer>();
		imgBigBlockList = new LinkedList<Pointer>();
		blockStickyFlagList = new LinkedList<Boolean>();

		for(int i = 0; i < numBlocks; i++) {
			Pointer imgNormal = loadImage(skindir + "/graphics/blockskin/normal/n" + i + ".png");
			imgNormalBlockList.add(imgNormal);
			imgSmallBlockList.add(loadImage(skindir + "/graphics/blockskin/small/s" + i + ".png"));
			imgBigBlockList.add(loadImage(skindir + "/graphics/blockskin/big/b" + i + ".png"));

			if(imgNormal != null) {
				FloatByReference tw = new FloatByReference();
				FloatByReference th = new FloatByReference();
				SDL3.INSTANCE.SDL_GetTextureSize(imgNormal, tw, th);
				if(tw.getValue() >= 400 && th.getValue() >= 304) {
					blockStickyFlagList.add(Boolean.TRUE);
				} else {
					blockStickyFlagList.add(Boolean.FALSE);
				}
			} else {
				blockStickyFlagList.add(Boolean.FALSE);
			}
		}

		// Other images
		imgFont = loadImage(skindir + "/graphics/font.png");
		imgFontSmall = loadImage(skindir + "/graphics/font_small.png");
		imgFontBig = loadImage(skindir + "/graphics/font_big.png");
		imgSprite = loadImage(skindir + "/graphics/sprite.png");
		imgTitle = loadImage(skindir + "/graphics/title.png");
		imgMenu = loadImage(skindir + "/graphics/menu.png");
		imgFrame = loadImage(skindir + "/graphics/frame.png");
		imgFieldbg = loadImage(skindir + "/graphics/fieldbg.png");
		imgFieldbg2 = loadImage(skindir + "/graphics/fieldbg2.png");
		imgFieldbg2Small = loadImage(skindir + "/graphics/fieldbg2_small.png");
		imgFieldbg2Big = loadImage(skindir + "/graphics/fieldbg2_big.png");
		imgBlankBlack = loadImage(skindir + "/graphics/blank_black.png");
		imgBlankWhite = loadImage(skindir + "/graphics/blank_white.png");

		if(NullpoMinoSDL.propConfig.getProperty("option.showlineeffect", true) == true)
			loadLineClearEffectImages();
		if(NullpoMinoSDL.propConfig.getProperty("option.showbg", true) == true)
			loadBackgroundImages();

		// Font
		try {
			ttfFont = SDL3TTF.INSTANCE.TTF_OpenFont(skindir + "/font/font.ttf", 16);
		} catch (Throwable e) {
			log.warn("TTF Font load failed", e);
			ttfFont = null;
		}

		// Sound effects
		soundManager = new SoundManagerSDL();
		if(NullpoMinoSDL.propConfig.getProperty("option.se", true) == true) {
			log.info("Loading Sound Effect");
			soundManager.load("cursor", skindir + "/se/cursor.wav");
			soundManager.load("decide", skindir + "/se/decide.wav");
			soundManager.load("erase1", skindir + "/se/erase1.wav");
			soundManager.load("erase2", skindir + "/se/erase2.wav");
			soundManager.load("erase3", skindir + "/se/erase3.wav");
			soundManager.load("erase4", skindir + "/se/erase4.wav");
			soundManager.load("died", skindir + "/se/died.wav");
			soundManager.load("gameover", skindir + "/se/gameover.wav");
			soundManager.load("hold", skindir + "/se/hold.wav");
			soundManager.load("holdfail", skindir + "/se/holdfail.wav");
			soundManager.load("initialhold", skindir + "/se/initialhold.wav");
			soundManager.load("initialrotate", skindir + "/se/initialrotate.wav");
			soundManager.load("levelup", skindir + "/se/levelup.wav");
			soundManager.load("linefall", skindir + "/se/linefall.wav");
			soundManager.load("lock", skindir + "/se/lock.wav");
			soundManager.load("move", skindir + "/se/move.wav");
			soundManager.load("pause", skindir + "/se/pause.wav");
			soundManager.load("rotate", skindir + "/se/rotate.wav");
			soundManager.load("step", skindir + "/se/step.wav");
			soundManager.load("piece0", skindir + "/se/piece0.wav");
			soundManager.load("piece1", skindir + "/se/piece1.wav");
			soundManager.load("piece2", skindir + "/se/piece2.wav");
			soundManager.load("piece3", skindir + "/se/piece3.wav");
			soundManager.load("piece4", skindir + "/se/piece4.wav");
			soundManager.load("piece5", skindir + "/se/piece5.wav");
			soundManager.load("piece6", skindir + "/se/piece6.wav");
			soundManager.load("piece7", skindir + "/se/piece7.wav");
			soundManager.load("piece8", skindir + "/se/piece8.wav");
			soundManager.load("piece9", skindir + "/se/piece9.wav");
			soundManager.load("piece10", skindir + "/se/piece10.wav");
			soundManager.load("harddrop", skindir + "/se/harddrop.wav");
			soundManager.load("softdrop", skindir + "/se/softdrop.wav");
			soundManager.load("levelstop", skindir + "/se/levelstop.wav");
			soundManager.load("endingstart", skindir + "/se/endingstart.wav");
			soundManager.load("excellent", skindir + "/se/excellent.wav");
			soundManager.load("b2b_start", skindir + "/se/b2b_start.wav");
			soundManager.load("b2b_continue", skindir + "/se/b2b_continue.wav");
			soundManager.load("b2b_end", skindir + "/se/b2b_end.wav");
			soundManager.load("gradeup", skindir + "/se/gradeup.wav");
			soundManager.load("countdown", skindir + "/se/countdown.wav");
			soundManager.load("tspin0", skindir + "/se/tspin0.wav");
			soundManager.load("tspin1", skindir + "/se/tspin1.wav");
			soundManager.load("tspin2", skindir + "/se/tspin2.wav");
			soundManager.load("tspin3", skindir + "/se/tspin3.wav");
			soundManager.load("ready", skindir + "/se/ready.wav");
			soundManager.load("go", skindir + "/se/go.wav");
			soundManager.load("movefail", skindir + "/se/movefail.wav");
			soundManager.load("rotfail", skindir + "/se/rotfail.wav");
			soundManager.load("medal", skindir + "/se/medal.wav");
			soundManager.load("change", skindir + "/se/change.wav");
			soundManager.load("bravo", skindir + "/se/bravo.wav");
			soundManager.load("cool", skindir + "/se/cool.wav");
			soundManager.load("regret", skindir + "/se/regret.wav");
			soundManager.load("garbage", skindir + "/se/garbage.wav");
			soundManager.load("stageclear", skindir + "/se/stageclear.wav");
			soundManager.load("stagefail", skindir + "/se/stagefail.wav");
			soundManager.load("gem", skindir + "/se/gem.wav");
			soundManager.load("danger", skindir + "/se/danger.wav");
			soundManager.load("matchend", skindir + "/se/matchend.wav");
			soundManager.load("hurryup", skindir + "/se/hurryup.wav");
			soundManager.load("square_s", skindir + "/se/square_s.wav");
			soundManager.load("square_g", skindir + "/se/square_g.wav");

			for(int i = 0; i < 20; i++) {
				soundManager.load("combo" + (i + 1), skindir + "/se/combo" + (i + 1) + ".wav");
			}
		}

		// Warm up SE tracks so first play doesn't allocate audio streams
		soundManager.warmUp();

		// Music
		bgm = new Pointer[BGMStatus.BGM_COUNT];
		bgmPlaying = -1;

		if(NullpoMinoSDL.propConfig.getProperty("option.bgmpreload", false) == true) {
			for(int i = 0; i < BGMStatus.BGM_COUNT; i++) {
				bgmLoad(i, false);
			}
		}
	}

	/**
	 * Load background images.
	 */
	public static void loadBackgroundImages() {
		if(imgPlayBG == null) {
			imgPlayBG = new Pointer[BACKGROUND_MAX];

			String skindir = NullpoMinoSDL.propConfig.getProperty("custom.skin.directory", "res");
			for(int i = 0; i < imgPlayBG.length; i++) {
				imgPlayBG[i] = loadImage(skindir + "/graphics/back" + i + ".png");
			}
		}
	}

	/**
	 * Load line-clear effect images.
	 */
	public static void loadLineClearEffectImages() {
		String skindir = NullpoMinoSDL.propConfig.getProperty("custom.skin.directory", "res");

		if(imgBreak == null) {
			imgBreak = new Pointer[BLOCK_BREAK_MAX][BLOCK_BREAK_SEGMENTS];

			for(int i = 0; i < BLOCK_BREAK_MAX; i++) {
				for(int j = 0; j < BLOCK_BREAK_SEGMENTS; j++) {
					imgBreak[i][j] = loadImage(skindir + "/graphics/break" + i + "_" + j + ".png");
				}
			}
		}
		if(imgPErase == null) {
			imgPErase = new Pointer[PERASE_MAX];

			for(int i = 0; i < imgPErase.length; i++) {
				imgPErase[i] = loadImage(skindir + "/graphics/perase" + i + ".png");
			}
		}
	}

	/**
	 * Load an image as an SDL3 texture.
	 * @param filename file path
	 * @return SDL_Texture* pointer, or null on failure
	 */
	public static Pointer loadImage(String filename) {
		Pointer surface = SDL3Image.INSTANCE.IMG_Load(filename);
		if(surface == null) {
			log.error("Failed to load image from " + filename + ": " + SDL3.INSTANCE.SDL_GetError());
			return null;
		}

		Pointer texture = SDL3.INSTANCE.SDL_CreateTextureFromSurface(NullpoMinoSDL.renderer, surface);
		SDL3.INSTANCE.SDL_DestroySurface(surface);

		if(texture == null) {
			log.error("Failed to create texture from " + filename + ": " + SDL3.INSTANCE.SDL_GetError());
			return null;
		}

		// Enable blending for alpha transparency
		SDL3.INSTANCE.SDL_SetTextureBlendMode(texture, SDLConstants.SDL_BLENDMODE_BLEND);
		// Use nearest-neighbor scaling for crisp pixel art
		SDL3.INSTANCE.SDL_SetTextureScaleMode(texture, SDLConstants.SDL_SCALEMODE_NEAREST);

		return texture;
	}

	/**
	 * Load a specific BGM into memory
	 * @param no BGM number
	 * @param showerr display errors on console
	 */
	public static void bgmLoad(int no, boolean showerr) {
		if(NullpoMinoSDL.propConfig.getProperty("option.bgm", false) == false) return;
		if(NullpoMinoSDL.mixerLib == null || NullpoMinoSDL.mixer == null) return;

		if(bgm[no] == null) {
			if(showerr) {
				log.info("Loading BGM " + no);
			}

			try {
				String filename = NullpoMinoSDL.propMusic.getProperty("music.filename." + no, null);
				if((filename == null) || (filename.length() < 1)) {
					if(showerr) log.info("BGM" + no + " not available");
					return;
				}

				bgm[no] = NullpoMinoSDL.mixerLib.MIX_LoadAudio(NullpoMinoSDL.mixer, filename, 0);

				if(bgm[no] == null) {
					if(showerr) log.warn("BGM " + no + " load failed: " + SDL3.INSTANCE.SDL_GetError());
				} else if(!showerr) {
					log.info("Loaded BGM " + no);
				}
			} catch(Throwable e) {
				if(showerr) {
					log.warn("BGM " + no + " load failed", e);
				} else {
					log.warn("BGM " + no + " load failed");
				}
			}
		}
	}

	/**
	 * Play a specific BGM
	 * @param no BGM number
	 */
	public static void bgmStart(int no) {
		if(NullpoMinoSDL.propConfig.getProperty("option.bgm", false) == false) return;
		SDL3Mixer lib = NullpoMinoSDL.mixerLib;
		Pointer mixer = NullpoMinoSDL.mixer;
		if(lib == null || mixer == null) return;

		bgmStop();
		bgmPlaying = no;

		if(no < 0) return;

		bgmLoad(no, true);

		if(bgm[no] != null) {
			try {
				// Create a BGM track if we don't have one
				if(bgmTrack == null) {
					bgmTrack = lib.MIX_CreateTrack(mixer);
				}
				if(bgmTrack == null) {
					log.warn("Failed to create BGM track");
					return;
				}

				lib.MIX_SetTrackAudio(bgmTrack, bgm[no]);

				if(NullpoMinoSDL.propMusic.getProperty("music.noloop." + no, false) == true)
					lib.MIX_SetTrackLoops(bgmTrack, 0);
				else
					lib.MIX_SetTrackLoops(bgmTrack, -1);

				float gain = NullpoMinoSDL.propConfig.getProperty("option.bgmvolume", 128) / 128.0f;
				lib.MIX_SetTrackGain(bgmTrack, Math.max(0.0f, Math.min(1.0f, gain)));

				lib.MIX_PlayTrack(bgmTrack, 0);
			} catch (Exception e) {
				log.warn("BGM " + no + " start failed", e);
			}
		}
	}

	/**
	 * Pause current BGM
	 */
	public static void bgmPause() {
		if(NullpoMinoSDL.mixerLib != null && bgmTrack != null && bgmIsPlaying()) {
			NullpoMinoSDL.mixerLib.MIX_PauseTrack(bgmTrack);
		}
	}

	/**
	 * Resume paused BGM
	 */
	public static void bgmResume() {
		if(NullpoMinoSDL.mixerLib != null && bgmTrack != null) {
			NullpoMinoSDL.mixerLib.MIX_ResumeTrack(bgmTrack);
		}
	}

	/**
	 * Whether BGM is playing
	 * @return true if playing
	 */
	public static boolean bgmIsPlaying() {
		if(NullpoMinoSDL.mixerLib == null || bgmTrack == null) return false;
		return NullpoMinoSDL.mixerLib.MIX_TrackPlaying(bgmTrack) != 0;
	}

	/**
	 * Stop BGM
	 */
	public static void bgmStop() {
		if(NullpoMinoSDL.mixerLib == null) return;
		try {
			if(bgmTrack != null && bgmIsPlaying()) {
				NullpoMinoSDL.mixerLib.MIX_StopTrack(bgmTrack, 0);
				bgmPlaying = -1;
			}
		} catch (Throwable e) {
			log.debug("BGM stop failed", e);
		}
	}

	/**
	 * Destroy all loaded resources.
	 */
	public static void destroy() {
		SDL3Mixer lib = NullpoMinoSDL.mixerLib;

		// Destroy BGM track before audio
		if(lib != null && bgmTrack != null) {
			lib.MIX_DestroyTrack(bgmTrack);
			bgmTrack = null;
		}
		// Destroy BGM audio
		if(lib != null && bgm != null) {
			for(int i = 0; i < bgm.length; i++) {
				if(bgm[i] != null) {
					lib.MIX_DestroyAudio(bgm[i]);
					bgm[i] = null;
				}
			}
		}
		// Destroy sound effects
		if(soundManager != null) {
			soundManager.destroy();
		}
		// Textures
		if(imgNormalBlockList != null) {
			for(Pointer p : imgNormalBlockList) if(p != null) SDL3.INSTANCE.SDL_DestroyTexture(p);
			imgNormalBlockList = null;
		}
		if(imgSmallBlockList != null) {
			for(Pointer p : imgSmallBlockList) if(p != null) SDL3.INSTANCE.SDL_DestroyTexture(p);
			imgSmallBlockList = null;
		}
		if(imgBigBlockList != null) {
			for(Pointer p : imgBigBlockList) if(p != null) SDL3.INSTANCE.SDL_DestroyTexture(p);
			imgBigBlockList = null;
		}
		if(ttfFont != null) {
			SDL3TTF.INSTANCE.TTF_CloseFont(ttfFont);
			ttfFont = null;
		}
	}
}

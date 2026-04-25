// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Pointer;

import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLStructs.SDL_AudioSpec;

import org.apache.log4j.Logger;

/**
 * Sound effects manager using pre-allocated tracks (like the old channel model).
 *
 * Pre-creates a pool of MIX_Track objects at init time and reuses the first
 * idle track, only stealing in round-robin order when all tracks are busy.
 */
public class SoundManagerSDL {
	/** Log */
	static Logger log = Logger.getLogger(SoundManagerSDL.class);

	/** Number of pre-allocated tracks (equivalent to old Mix_AllocateChannels) */
	private static final int NUM_TRACKS = 16;
	/** Fallback SE duration when SDL_mixer can't report one. */
	private static final long UNKNOWN_CLIP_DURATION_MS = 250L;
	/** Extra time to leave a track unavailable after the clip length drains. */
	private static final long TRACK_REUSE_PADDING_MS = 50L;
	/** Extra time to let muted warm-up audio drain out of the output stream. */
	private static final long WARMUP_DRAIN_PADDING_MS = 100L;
	/** Poll interval while waiting for warm-up tracks to finish. */
	private static final long WARMUP_POLL_INTERVAL_MS = 10L;
	/** Upper bound so startup never stalls too long if metadata is wrong. */
	private static final long WARMUP_MAX_WAIT_MS = 2000L;

	/** WAVE file data (Name -> MIX_Audio*) */
	protected HashMap<String, Pointer> clipMap;
	/** Approximate clip length in milliseconds (Name -> duration). */
	private HashMap<String, Long> clipDurationMap;
	/** Dedicated per-sound tracks for rapid retriggered sounds. */
	private HashMap<String, Integer> dedicatedTrackBySound;

	/** Pre-allocated track pool (MIX_Track*) */
	private Pointer[] tracks;
	/** Currently assigned audio on each track. */
	private Pointer[] trackAudio;
	/** Name of the currently assigned audio on each track. */
	private String[] trackAudioName;
	/** Dedicated sound currently owning each track, or null if shared. */
	private String[] trackDedicatedSound;
	/** Earliest SDL tick when each track is safe to reuse. */
	private long[] trackReadyAtTicks;

	/** Current gain for SE tracks */
	private float seGain = 1.0f;

	/**
	 * Constructor — pre-allocates the track pool.
	 */
	public SoundManagerSDL() {
		clipMap = new HashMap<String, Pointer>();
		clipDurationMap = new HashMap<String, Long>();
		dedicatedTrackBySound = new HashMap<String, Integer>();
		tracks = new Pointer[NUM_TRACKS];
		trackAudio = new Pointer[NUM_TRACKS];
		trackAudioName = new String[NUM_TRACKS];
		trackDedicatedSound = new String[NUM_TRACKS];
		trackReadyAtTicks = new long[NUM_TRACKS];

		SDL3Mixer lib = NullpoMinoSDL.mixerLib;
		Pointer mixer = NullpoMinoSDL.mixer;
		if(lib != null && mixer != null) {
			for(int i = 0; i < NUM_TRACKS; i++) {
				tracks[i] = lib.MIX_CreateTrack(mixer);
			}
			float vol = NullpoMinoSDL.propConfig.getProperty("option.sevolume", 128) / 128.0f;
			seGain = Math.max(0.0f, Math.min(1.0f, vol));
			for(int i = 0; i < NUM_TRACKS; i++) {
				if(tracks[i] != null) lib.MIX_SetTrackGain(tracks[i], seGain);
			}
		}
	}

	/**
	 * Load WAVE file
	 * @param name Registered name
	 * @param filename Filename
	 * @return true if successful
	 */
	public boolean load(String name, String filename) {
		SDL3Mixer lib = NullpoMinoSDL.mixerLib;
		Pointer mixer = NullpoMinoSDL.mixer;
		if(lib == null || mixer == null) return false;

		try {
			Pointer audio = lib.MIX_LoadAudio(mixer, filename, 1);
			if(audio == null) {
				log.warn("Failed to load wav file from " + filename);
				return false;
			}
			clipMap.put(name, audio);
			clipDurationMap.put(name, getAudioDurationMillis(lib, audio));
		} catch(Throwable e) {
			log.warn("Failed to load wav file from " + filename, e);
			return false;
		}

		return true;
	}

	/**
	 * Warm up all tracks by silently playing audio through the full pipeline.
	 * This forces SDL_CreateAudioStream, decoder init, and mixer-thread
	 * processing for every track so the first real play is glitch-free.
	 */
	public void warmUp() {
		SDL3Mixer lib = NullpoMinoSDL.mixerLib;
		if(lib == null || clipMap.isEmpty()) return;

		String warmUpClipName = getWarmUpClipName();
		Pointer warmUpAudio = clipMap.get(warmUpClipName);
		if(warmUpAudio == null) return;

		for(int i = 0; i < NUM_TRACKS; i++) {
			if(tracks[i] != null) {
				lib.MIX_SetTrackGain(tracks[i], 0.0f);
				lib.MIX_SetTrackAudio(tracks[i], warmUpAudio);
				trackAudio[i] = warmUpAudio;
				trackAudioName[i] = warmUpClipName;
				lib.MIX_PlayTrack(tracks[i], 0);
			}
		}

		// Wait for every muted warm-up track to finish instead of assuming the
		// clip duration is enough. Some backends keep extra converted audio queued
		// after the decoded clip is exhausted.
		long deadline = System.currentTimeMillis() + WARMUP_MAX_WAIT_MS;
		while(!areAllTracksStopped(lib) && System.currentTimeMillis() < deadline) {
			try {
				Thread.sleep(WARMUP_POLL_INTERVAL_MS);
			} catch(InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		// Give the device-side queue a little more time to drain before we
		// restore the real track gain.
		try {
			Thread.sleep(WARMUP_DRAIN_PADDING_MS);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		for(int i = 0; i < NUM_TRACKS; i++) {
			if(tracks[i] != null) {
				lib.MIX_SetTrackGain(tracks[i], seGain);
			}
			trackReadyAtTicks[i] = 0L;
		}
	}

	/**
	 * Play a sound effect on an idle track to avoid cutting off active audio.
	 * Falls back to the oldest track if all are busy.
	 * @param name Registered name
	 */
	public void play(String name) {
		SDL3Mixer lib = NullpoMinoSDL.mixerLib;
		if(lib == null) return;

		Pointer audio = clipMap.get(name);

		if(audio != null) {
			long now = SDL3.INSTANCE.SDL_GetTicks();
			int chosen = findTrackForSound(lib, now, audio, name);
			if(chosen == -1) return;

			playOnTrack(lib, chosen, audio, name, now);
		}
	}

	/**
	 * Change sound effect volume.
	 * @param volume Volume (0–128 for compatibility, mapped to 0.0–1.0 gain)
	 */
	public void changeVolume(int volume) {
		SDL3Mixer lib = NullpoMinoSDL.mixerLib;
		if(lib == null) return;

		seGain = Math.max(0.0f, Math.min(1.0f, volume / 128.0f));
		for(int i = 0; i < NUM_TRACKS; i++) {
			if(tracks[i] != null) lib.MIX_SetTrackGain(tracks[i], seGain);
		}
	}

	/**
	 * Destroy all loaded audio clips and tracks.
	 */
	public void destroy() {
		SDL3Mixer lib = NullpoMinoSDL.mixerLib;
		if(lib != null) {
			for(int i = 0; i < NUM_TRACKS; i++) {
				if(tracks[i] != null) {
					lib.MIX_DestroyTrack(tracks[i]);
					tracks[i] = null;
				}
				trackAudio[i] = null;
				trackAudioName[i] = null;
				trackDedicatedSound[i] = null;
			}
			for(Pointer audio : clipMap.values()) {
				if(audio != null) lib.MIX_DestroyAudio(audio);
			}
		}
		clipMap.clear();
		clipDurationMap.clear();
		dedicatedTrackBySound.clear();
	}

	/**
	 * Pick the shortest clip we have so warm-up finishes quickly.
	 * Prefer the menu cursor sound when available because that's where startup
	 * pops are most noticeable.
	 */
	private String getWarmUpClipName() {
		if(clipMap.containsKey("cursor")) return "cursor";

		String bestName = null;
		long bestDuration = Long.MAX_VALUE;
		for(Map.Entry<String, Pointer> entry : clipMap.entrySet()) {
			long duration = clipDurationMap.get(entry.getKey()).longValue();
			if(bestName == null || duration < bestDuration) {
				bestName = entry.getKey();
				bestDuration = duration;
			}
		}
		return bestName;
	}

	/**
	 * Convert SDL_mixer's frame duration into milliseconds.
	 */
	private long getAudioDurationMillis(SDL3Mixer lib, Pointer audio) {
		long durationFrames = lib.MIX_GetAudioDuration(audio);
		if(durationFrames <= 0L) return UNKNOWN_CLIP_DURATION_MS;

		SDL_AudioSpec spec = new SDL_AudioSpec();
		if(lib.MIX_GetAudioFormat(audio, spec) == 0 || spec.freq <= 0) {
			return UNKNOWN_CLIP_DURATION_MS;
		}

		long durationMs = ((durationFrames * 1000L) + spec.freq - 1) / spec.freq;
		return Math.max(durationMs, 1L);
	}

	/**
	 * Resolve the track to use for a sound. Some sounds use a dedicated track,
	 * meaning they keep the same MIX_Track every time they play instead of
	 * borrowing from the shared pool. This lets rapid retriggers restart
	 * cleanly without overlapping or rebinding on random tracks.
	 */
	private int findTrackForSound(SDL3Mixer lib, long now, Pointer audio, String name) {
		if(usesDedicatedTrack(name)) {
			Integer dedicatedIndex = dedicatedTrackBySound.get(name);
			if(dedicatedIndex != null && dedicatedIndex.intValue() >= 0 && dedicatedIndex.intValue() < NUM_TRACKS
				&& tracks[dedicatedIndex.intValue()] != null) {
				return dedicatedIndex.intValue();
			}

			int chosen = findBestIdleTrack(lib, now, audio, name);
			if(chosen != -1) {
				dedicatedTrackBySound.put(name, Integer.valueOf(chosen));
				trackDedicatedSound[chosen] = name;
			}
			return chosen;
		}

		return findBestSharedTrack(lib, now, audio, name);
	}

	/**
	 * Prefer an idle track that is already bound to the requested audio, so
	 * short sounds can replay without rebuilding the track input. This path is
	 * only used when a sound is claiming its own dedicated track.
	 */
	private int findBestIdleTrack(SDL3Mixer lib, long now, Pointer audio, String name) {
		for(int i = 0; i < NUM_TRACKS; i++) {
			if(isDedicatedTrackCandidate(lib, now, i) && trackAudio[i] == audio && name.equals(trackAudioName[i])) {
				return i;
			}
		}
		for(int i = 0; i < NUM_TRACKS; i++) {
			if(isDedicatedTrackCandidate(lib, now, i)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Shared sounds use only non-dedicated tracks.
	 */
	private int findBestSharedTrack(SDL3Mixer lib, long now, Pointer audio, String name) {
		for(int i = 0; i < NUM_TRACKS; i++) {
			if(isSharedTrackCandidate(lib, now, i) && trackAudio[i] == audio && name.equals(trackAudioName[i])) {
				return i;
			}
		}
		for(int i = 0; i < NUM_TRACKS; i++) {
			if(isSharedTrackCandidate(lib, now, i)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Start or restart playback on a specific track.
	 */
	private void playOnTrack(SDL3Mixer lib, int index, Pointer audio, String name, long now) {
		Pointer track = tracks[index];
		if(track == null) return;

		boolean ready = true;
		if(trackAudio[index] != audio) {
			ready = lib.MIX_SetTrackAudio(track, audio) != 0;
			if(ready) {
				trackAudio[index] = audio;
				trackAudioName[index] = name;
			}
		}

		if(ready) {
			byte played = lib.MIX_PlayTrack(track, 0);
			if(played != 0) {
				long durationMs = clipDurationMap.get(name).longValue();
				trackReadyAtTicks[index] = now + durationMs + TRACK_REUSE_PADDING_MS;
			}
		}
	}

	/**
	 * A pooled track is reusable once SDL_mixer reports it stopped and we've
	 * given its queued output a little time to drain.
	 */
	private boolean isTrackIdle(SDL3Mixer lib, long now, int index) {
		return tracks[index] != null && now >= trackReadyAtTicks[index] && lib.MIX_TrackPlaying(tracks[index]) == 0;
	}

	/**
	 * Dedicated-track sounds claim only unowned tracks.
	 */
	private boolean isDedicatedTrackCandidate(SDL3Mixer lib, long now, int index) {
		if(!isTrackIdle(lib, now, index)) return false;
		return trackDedicatedSound[index] == null;
	}

	/**
	 * Shared sounds never borrow a dedicated track that belongs to another
	 * rapid retrigger sound.
	 */
	private boolean isSharedTrackCandidate(SDL3Mixer lib, long now, int index) {
		if(!isTrackIdle(lib, now, index)) return false;
		return trackDedicatedSound[index] == null;
	}

	/**
	 * True when every pooled track has reached the stopped state.
	 */
	private boolean areAllTracksStopped(SDL3Mixer lib) {
		for(int i = 0; i < NUM_TRACKS; i++) {
			if(tracks[i] != null && lib.MIX_TrackPlaying(tracks[i]) != 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * UI sounds are frequent and short, so they also benefit from using a
	 * dedicated track.
	 */
	private boolean isUISound(String name) {
		return "cursor".equals(name) || "decide".equals(name) || "change".equals(name);
	}

	/**
	 * High-rate gameplay sounds sound cleaner if they restart on their own
	 * dedicated track instead of layering on multiple shared tracks.
	 */
	private boolean usesDedicatedTrack(String name) {
		return isUISound(name) || "move".equals(name) || "rotate".equals(name)
			|| "step".equals(name) || "softdrop".equals(name);
	}
}

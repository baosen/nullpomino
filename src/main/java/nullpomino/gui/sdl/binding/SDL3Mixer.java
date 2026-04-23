package nullpomino.gui.sdl.binding;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import nullpomino.gui.sdl.binding.SDLStructs.SDL_AudioSpec;

/**
 * JNA interface to SDL3_mixer 3.x (libSDL3_mixer.so).
 *
 * SDL_mixer 3.x replaced the old channel-based Mix_* API with a
 * track-based MIX_* API:
 * <ul>
 *   <li>{@code MIX_CreateMixer} replaces {@code Mix_OpenAudio}</li>
 *   <li>{@code MIX_LoadAudio} replaces {@code Mix_LoadWAV} / {@code Mix_LoadMUS}</li>
 *   <li>{@code MIX_PlayAudio} is fire-and-forget (replaces {@code Mix_PlayChannel})</li>
 *   <li>{@code MIX_CreateTrack} + {@code MIX_PlayTrack} for managed playback (BGM)</li>
 *   <li>Volume uses float gain (0.0–1.0) instead of int (0–128)</li>
 * </ul>
 */
public interface SDL3Mixer extends Library {

	/** Load the library, or null if not available. */
	static SDL3Mixer loadOrNull() {
		try {
			return Native.load("SDL3_mixer", SDL3Mixer.class);
		} catch (UnsatisfiedLinkError e) {
			return null;
		}
	}

	// --- Init / Quit ---
	byte MIX_Init();
	void MIX_Quit();

	// --- Mixer ---
	/** Create a mixer that outputs to an audio device. Pass 0xFFFFFFFF for default playback device. */
	Pointer MIX_CreateMixerDevice(int devid, Pointer spec);
	/** Create a mixer for memory-buffer rendering (not device playback). */
	Pointer MIX_CreateMixer(Pointer spec);
	void MIX_DestroyMixer(Pointer mixer);
	byte MIX_SetMixerGain(Pointer mixer, float gain);
	float MIX_GetMixerGain(Pointer mixer);

	// --- Audio (loaded samples/music) ---
	Pointer MIX_LoadAudio(Pointer mixer, String path, int predecode);
	void MIX_DestroyAudio(Pointer audio);
	long MIX_GetAudioDuration(Pointer audio);
	byte MIX_GetAudioFormat(Pointer audio, SDL_AudioSpec spec);

	// --- Fire-and-forget playback (sound effects) ---
	byte MIX_PlayAudio(Pointer mixer, Pointer audio);

	// --- Track-based playback (BGM, stoppable sounds) ---
	Pointer MIX_CreateTrack(Pointer mixer);
	void MIX_DestroyTrack(Pointer track);
	byte MIX_SetTrackAudio(Pointer track, Pointer audio);
	byte MIX_SetTrackLoops(Pointer track, int num_loops);
	byte MIX_PlayTrack(Pointer track, int options);
	byte MIX_StopTrack(Pointer track, long fade_out_frames);
	byte MIX_PauseTrack(Pointer track);
	byte MIX_ResumeTrack(Pointer track);
	byte MIX_TrackPlaying(Pointer track);
	byte MIX_SetTrackGain(Pointer track, float gain);
	float MIX_GetTrackGain(Pointer track);
	byte MIX_PauseAllTracks(Pointer mixer);
	byte MIX_ResumeAllTracks(Pointer mixer);
}

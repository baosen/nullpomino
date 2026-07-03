package nullpomino.gui.sdl.binding;

import nullpomino.gui.sdl.binding.SdlHandles.MixAudio;
import nullpomino.gui.sdl.binding.SdlHandles.MixMixer;
import nullpomino.gui.sdl.binding.SdlHandles.MixTrack;
import nullpomino.gui.sdl.binding.SDLStructs.SDL_AudioSpec;

/**
 * Backend-neutral interface to SDL3_mixer 3.x.
 *
 * SDL_mixer 3.x replaced the old channel-based Mix_* API with a
 * track-based MIX_* API:
 * <ul>
 *   <li>{@code MIX_CreateMixerDevice} replaces {@code Mix_OpenAudio}</li>
 *   <li>{@code MIX_LoadAudio} replaces {@code Mix_LoadWAV} / {@code Mix_LoadMUS}</li>
 *   <li>{@code MIX_CreateTrack} + {@code MIX_PlayTrack} for managed playback</li>
 *   <li>Volume uses float gain (0.0–1.0) instead of int (0–128)</li>
 * </ul>
 */
public interface SDL3Mixer {

	/** Get the mixer binding of the active backend, or null if audio is unavailable. */
	static SDL3Mixer loadOrNull() {
		return SdlBackend.get().mixerOrNull();
	}

	// --- Init / Quit ---
	byte MIX_Init();
	void MIX_Quit();

	// --- Mixer ---
	/** Create a mixer that outputs to an audio device. Pass 0xFFFFFFFF for default playback device. */
	MixMixer MIX_CreateMixerDevice(int devid, SDL_AudioSpec spec);
	void MIX_DestroyMixer(MixMixer mixer);

	// --- Audio (loaded samples/music) ---
	MixAudio MIX_LoadAudio(MixMixer mixer, String path, int predecode);
	void MIX_DestroyAudio(MixAudio audio);
	long MIX_GetAudioDuration(MixAudio audio);
	byte MIX_GetAudioFormat(MixAudio audio, SDL_AudioSpec spec);

	// --- Track-based playback (BGM, stoppable sounds) ---
	MixTrack MIX_CreateTrack(MixMixer mixer);
	void MIX_DestroyTrack(MixTrack track);
	byte MIX_SetTrackAudio(MixTrack track, MixAudio audio);
	byte MIX_SetTrackLoops(MixTrack track, int num_loops);
	byte MIX_PlayTrack(MixTrack track, int options);
	byte MIX_StopTrack(MixTrack track, long fade_out_frames);
	byte MIX_PauseTrack(MixTrack track);
	byte MIX_ResumeTrack(MixTrack track);
	byte MIX_TrackPlaying(MixTrack track);
	byte MIX_SetTrackGain(MixTrack track, float gain);
}

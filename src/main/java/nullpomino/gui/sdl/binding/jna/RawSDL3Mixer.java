package nullpomino.gui.sdl.binding.jna;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * JNA interface to SDL3_mixer 3.x (libSDL3_mixer.so).
 */
interface RawSDL3Mixer extends Library {

	// --- Init / Quit ---
	byte MIX_Init();
	void MIX_Quit();

	// --- Mixer ---
	Pointer MIX_CreateMixerDevice(int devid, JnaStructs.SDL_AudioSpec spec);
	void MIX_DestroyMixer(Pointer mixer);

	// --- Audio (loaded samples/music) ---
	Pointer MIX_LoadAudio(Pointer mixer, String path, int predecode);
	void MIX_DestroyAudio(Pointer audio);
	long MIX_GetAudioDuration(Pointer audio);
	byte MIX_GetAudioFormat(Pointer audio, JnaStructs.SDL_AudioSpec spec);

	// --- Track-based playback ---
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
}

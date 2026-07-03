package nullpomino.gui.sdl.binding.jna;

import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDLStructs.SDL_AudioSpec;
import nullpomino.gui.sdl.binding.SdlHandles.MixAudio;
import nullpomino.gui.sdl.binding.SdlHandles.MixMixer;
import nullpomino.gui.sdl.binding.SdlHandles.MixTrack;

/**
 * Desktop implementation of the neutral {@link SDL3Mixer} interface.
 */
final class JnaSDL3Mixer implements SDL3Mixer {

	private final RawSDL3Mixer raw;

	JnaSDL3Mixer(RawSDL3Mixer raw) {
		this.raw = raw;
	}

	@Override public byte MIX_Init() { return raw.MIX_Init(); }
	@Override public void MIX_Quit() { raw.MIX_Quit(); }

	@Override public MixMixer MIX_CreateMixerDevice(int devid, SDL_AudioSpec spec) {
		JnaStructs.SDL_AudioSpec nativeSpec = null;
		if(spec != null) {
			nativeSpec = new JnaStructs.SDL_AudioSpec();
			nativeSpec.format = spec.format;
			nativeSpec.channels = spec.channels;
			nativeSpec.freq = spec.freq;
		}
		return Ptr.wrap(raw.MIX_CreateMixerDevice(devid, nativeSpec));
	}
	@Override public void MIX_DestroyMixer(MixMixer mixer) { raw.MIX_DestroyMixer(Ptr.p(mixer)); }

	@Override public MixAudio MIX_LoadAudio(MixMixer mixer, String path, int predecode) {
		return Ptr.wrap(raw.MIX_LoadAudio(Ptr.p(mixer), path, predecode));
	}
	@Override public void MIX_DestroyAudio(MixAudio audio) { raw.MIX_DestroyAudio(Ptr.p(audio)); }
	@Override public long MIX_GetAudioDuration(MixAudio audio) { return raw.MIX_GetAudioDuration(Ptr.p(audio)); }
	@Override public byte MIX_GetAudioFormat(MixAudio audio, SDL_AudioSpec spec) {
		JnaStructs.SDL_AudioSpec nativeSpec = new JnaStructs.SDL_AudioSpec();
		byte ok = raw.MIX_GetAudioFormat(Ptr.p(audio), nativeSpec);
		spec.format = nativeSpec.format;
		spec.channels = nativeSpec.channels;
		spec.freq = nativeSpec.freq;
		return ok;
	}

	@Override public MixTrack MIX_CreateTrack(MixMixer mixer) {
		return Ptr.wrap(raw.MIX_CreateTrack(Ptr.p(mixer)));
	}
	@Override public void MIX_DestroyTrack(MixTrack track) { raw.MIX_DestroyTrack(Ptr.p(track)); }
	@Override public byte MIX_SetTrackAudio(MixTrack track, MixAudio audio) {
		return raw.MIX_SetTrackAudio(Ptr.p(track), Ptr.p(audio));
	}
	@Override public byte MIX_SetTrackLoops(MixTrack track, int numLoops) {
		return raw.MIX_SetTrackLoops(Ptr.p(track), numLoops);
	}
	@Override public byte MIX_PlayTrack(MixTrack track, int options) {
		return raw.MIX_PlayTrack(Ptr.p(track), options);
	}
	@Override public byte MIX_StopTrack(MixTrack track, long fadeOutFrames) {
		return raw.MIX_StopTrack(Ptr.p(track), fadeOutFrames);
	}
	@Override public byte MIX_PauseTrack(MixTrack track) { return raw.MIX_PauseTrack(Ptr.p(track)); }
	@Override public byte MIX_ResumeTrack(MixTrack track) { return raw.MIX_ResumeTrack(Ptr.p(track)); }
	@Override public byte MIX_TrackPlaying(MixTrack track) { return raw.MIX_TrackPlaying(Ptr.p(track)); }
	@Override public byte MIX_SetTrackGain(MixTrack track, float gain) {
		return raw.MIX_SetTrackGain(Ptr.p(track), gain);
	}
}

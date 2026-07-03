package nullpomino.gui.sdl.binding.web;

import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDLStructs.SDL_AudioSpec;
import nullpomino.gui.sdl.binding.SdlHandles.MixAudio;
import nullpomino.gui.sdl.binding.SdlHandles.MixMixer;
import nullpomino.gui.sdl.binding.SdlHandles.MixTrack;

/**
 * Browser implementation of {@link SDL3Mixer} on top of the Web Audio bridge
 * ({@link BrowserAudio}). Used under CheerpJ, whose Java runtime has no
 * javax.sound output.
 */
final class JsSDL3Mixer implements SDL3Mixer {

	/** SDL_AUDIO_S16 — the value MIX_GetAudioFormat reports. */
	private static final int SDL_AUDIO_S16 = 0x8010;

	private static final class JsMixer implements MixMixer {}

	private static final class JsAudio implements MixAudio {
		final int id;
		final int durationFrames;
		final int sampleRate;

		JsAudio(int id) {
			this.id = id;
			this.durationFrames = BrowserAudio.jsAudioDurationFrames(id);
			this.sampleRate = BrowserAudio.jsAudioSampleRate(id);
		}
	}

	private static final class JsTrack implements MixTrack {
		final int id;

		JsTrack(int id) {
			this.id = id;
		}
	}

	@Override public byte MIX_Init() { return 1; }
	@Override public void MIX_Quit() {}

	@Override public MixMixer MIX_CreateMixerDevice(int devid, SDL_AudioSpec spec) {
		return new JsMixer();
	}

	@Override public void MIX_DestroyMixer(MixMixer mixer) {}

	@Override public MixAudio MIX_LoadAudio(MixMixer mixer, String path, int predecode) {
		if(path == null || path.isEmpty()) return null;
		// The game passes virtual-filesystem paths; assets live on the
		// HTTP-backed /app mount, whose paths map to server URLs 1:1 once
		// the mount prefix is dropped.
		String url = path.startsWith("/app/") ? path.substring("/app".length()) : path;
		int id = BrowserAudio.jsLoadAudio(url);
		return id > 0 ? new JsAudio(id) : null;
	}

	@Override public void MIX_DestroyAudio(MixAudio audio) {}

	@Override public long MIX_GetAudioDuration(MixAudio audio) {
		return ((JsAudio) audio).durationFrames;
	}

	@Override public byte MIX_GetAudioFormat(MixAudio audio, SDL_AudioSpec spec) {
		JsAudio js = (JsAudio) audio;
		spec.format = SDL_AUDIO_S16;
		spec.channels = 2;
		spec.freq = js.sampleRate > 0 ? js.sampleRate : 44100;
		return 1;
	}

	@Override public MixTrack MIX_CreateTrack(MixMixer mixer) {
		int id = BrowserAudio.jsCreateTrack();
		return id > 0 ? new JsTrack(id) : null;
	}

	@Override public void MIX_DestroyTrack(MixTrack track) {
		BrowserAudio.jsStopTrack(((JsTrack) track).id);
	}

	@Override public byte MIX_SetTrackAudio(MixTrack track, MixAudio audio) {
		if(audio == null) return 0;
		BrowserAudio.jsSetTrackAudio(((JsTrack) track).id, ((JsAudio) audio).id);
		return 1;
	}

	@Override public byte MIX_SetTrackLoops(MixTrack track, int numLoops) {
		BrowserAudio.jsSetTrackLoops(((JsTrack) track).id, numLoops);
		return 1;
	}

	@Override public byte MIX_PlayTrack(MixTrack track, int options) {
		BrowserAudio.jsPlayTrack(((JsTrack) track).id);
		return 1;
	}

	@Override public byte MIX_StopTrack(MixTrack track, long fadeOutFrames) {
		BrowserAudio.jsStopTrack(((JsTrack) track).id);
		return 1;
	}

	@Override public byte MIX_PauseTrack(MixTrack track) {
		BrowserAudio.jsPauseTrack(((JsTrack) track).id);
		return 1;
	}

	@Override public byte MIX_ResumeTrack(MixTrack track) {
		BrowserAudio.jsResumeTrack(((JsTrack) track).id);
		return 1;
	}

	@Override public byte MIX_TrackPlaying(MixTrack track) {
		return (byte) (BrowserAudio.jsTrackPlaying(((JsTrack) track).id) != 0 ? 1 : 0);
	}

	@Override public byte MIX_SetTrackGain(MixTrack track, float gain) {
		BrowserAudio.jsSetTrackGain(((JsTrack) track).id, gain);
		return 1;
	}
}

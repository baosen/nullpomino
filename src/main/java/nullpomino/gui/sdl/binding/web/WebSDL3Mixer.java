package nullpomino.gui.sdl.binding.web;

import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDLStructs.SDL_AudioSpec;
import nullpomino.gui.sdl.binding.SdlHandles.MixAudio;
import nullpomino.gui.sdl.binding.SdlHandles.MixMixer;
import nullpomino.gui.sdl.binding.SdlHandles.MixTrack;

/**
 * Pure-Java implementation of {@link SDL3Mixer} via javax.sound.sampled.
 * Every operation degrades to a silent no-op failure rather than throwing,
 * matching the game's existing null-guarded audio paths.
 */
final class WebSDL3Mixer implements SDL3Mixer {

	/** SDL_AUDIO_S16 — the value MIX_GetAudioFormat reports. */
	private static final int SDL_AUDIO_S16 = 0x8010;

	private static final class WebMixer implements MixMixer {}

	@Override public byte MIX_Init() { return 1; }
	@Override public void MIX_Quit() {}

	@Override public MixMixer MIX_CreateMixerDevice(int devid, SDL_AudioSpec spec) {
		return new WebMixer();
	}

	@Override public void MIX_DestroyMixer(MixMixer mixer) {}

	@Override public MixAudio MIX_LoadAudio(MixMixer mixer, String path, int predecode) {
		try(AudioInputStream in = AudioSystem.getAudioInputStream(new File(path))) {
			AudioFormat base = in.getFormat();
			AudioFormat pcm = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,
				base.getSampleRate(),
				16,
				base.getChannels(),
				base.getChannels() * 2,
				base.getSampleRate(),
				false);
			try(AudioInputStream decoded = AudioSystem.getAudioInputStream(pcm, in)) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				byte[] buf = new byte[16384];
				int n;
				while((n = decoded.read(buf)) > 0) {
					out.write(buf, 0, n);
				}
				return new WebAudio(pcm, out.toByteArray());
			}
		} catch(Exception | Error e) {
			return null;
		}
	}

	@Override public void MIX_DestroyAudio(MixAudio audio) {}

	@Override public long MIX_GetAudioDuration(MixAudio audio) {
		return ((WebAudio) audio).frames;
	}

	@Override public byte MIX_GetAudioFormat(MixAudio audio, SDL_AudioSpec spec) {
		AudioFormat format = ((WebAudio) audio).format;
		spec.format = SDL_AUDIO_S16;
		spec.channels = format.getChannels();
		spec.freq = (int) format.getSampleRate();
		return 1;
	}

	@Override public MixTrack MIX_CreateTrack(MixMixer mixer) {
		return new WebTrack();
	}

	@Override public void MIX_DestroyTrack(MixTrack track) {
		((WebTrack) track).destroy();
	}

	@Override public byte MIX_SetTrackAudio(MixTrack track, MixAudio audio) {
		return ((WebTrack) track).setAudio((WebAudio) audio) ? (byte) 1 : 0;
	}

	@Override public byte MIX_SetTrackLoops(MixTrack track, int numLoops) {
		((WebTrack) track).setLoops(numLoops);
		return 1;
	}

	@Override public byte MIX_PlayTrack(MixTrack track, int options) {
		return ((WebTrack) track).play() ? (byte) 1 : 0;
	}

	@Override public byte MIX_StopTrack(MixTrack track, long fadeOutFrames) {
		((WebTrack) track).stop();
		return 1;
	}

	@Override public byte MIX_PauseTrack(MixTrack track) {
		((WebTrack) track).pause();
		return 1;
	}

	@Override public byte MIX_ResumeTrack(MixTrack track) {
		((WebTrack) track).resume();
		return 1;
	}

	@Override public byte MIX_TrackPlaying(MixTrack track) {
		return ((WebTrack) track).playing() ? (byte) 1 : 0;
	}

	@Override public byte MIX_SetTrackGain(MixTrack track, float gain) {
		((WebTrack) track).setGain(gain);
		return 1;
	}
}

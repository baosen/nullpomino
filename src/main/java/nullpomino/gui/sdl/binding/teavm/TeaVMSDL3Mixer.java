package nullpomino.gui.sdl.binding.teavm;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.webaudio.AudioBuffer;
import org.teavm.jso.webaudio.AudioBufferSourceNode;
import org.teavm.jso.webaudio.AudioContext;
import org.teavm.jso.webaudio.GainNode;

import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDLStructs.SDL_AudioSpec;
import nullpomino.gui.sdl.binding.SdlHandles.MixAudio;
import nullpomino.gui.sdl.binding.SdlHandles.MixMixer;
import nullpomino.gui.sdl.binding.SdlHandles.MixTrack;

/**
 * Web Audio implementation of {@link SDL3Mixer}. The AudioContext starts
 * suspended and is resumed on the first user gesture; plays requested while
 * suspended (one-shots included) are queued and fire the instant it resumes,
 * rather than being silently dropped.
 *
 * Audio decoding blocks the game's green thread (via {@link JsAsync}), so a
 * loaded track always has a real buffer — no async-load deferral is needed.
 */
final class TeaVMSDL3Mixer implements SDL3Mixer {

	private static final int SDL_AUDIO_S16 = 0x8010;

	private final AudioContext ctx;

	static TeaVMSDL3Mixer createOrNull() {
		try {
			AudioContext ctx = JsAsync.createAudioContext();
			return ctx != null ? new TeaVMSDL3Mixer(ctx) : null;
		} catch (Exception | Error e) {
			return null;
		}
	}

	private TeaVMSDL3Mixer(AudioContext ctx) {
		this.ctx = ctx;

		EventListener<Event> resume = e -> resumeAudio();
		for (String type : new String[] {"keydown", "keyup", "mousedown", "pointerdown", "touchstart"}) {
			Window.current().addEventListener(type, resume, true);
		}
		ctx.setOnStateChange(e -> {
			if (!"running".equals(ctx.getState())) return;
			for (JsTrack t : tracks) {
				if (t.pendingStart) {
					t.pendingStart = false;
					startTrack(t, 0);
				}
			}
		});
	}

	private void resumeAudio() {
		if ("suspended".equals(ctx.getState())) ctx.resume();
	}

	private static final class JsMixer implements MixMixer {}

	private static final class JsAudio implements MixAudio {
		final AudioBuffer buffer;
		JsAudio(AudioBuffer buffer) {
			this.buffer = buffer;
		}
	}

	private final java.util.List<JsTrack> tracks = new java.util.ArrayList<>();

	private final class JsTrack implements MixTrack {
		final GainNode gain;
		AudioBuffer buffer;
		AudioBufferSourceNode source;
		boolean loop;
		boolean playing;
		double startedAt;
		double pausedAt;
		boolean paused;
		boolean pendingStart;

		JsTrack() {
			gain = ctx.createGain();
			gain.connect(ctx.getDestination());
		}
	}

	private void stopSource(JsTrack t) {
		if (t.source != null) {
			AudioBufferSourceNode s = t.source;
			t.source = null;
			try {
				s.stop();
			} catch (Exception | Error ignored) {
				// stop() on an already-stopped source throws; ignore.
			}
		}
		t.playing = false;
	}

	private void startTrack(JsTrack t, double offset) {
		if (t.buffer == null) return;
		stopSource(t);
		AudioBufferSourceNode s = ctx.createBufferSource();
		s.setBuffer(t.buffer);
		s.setLoop(t.loop);
		s.connect(t.gain);
		s.setOnEnded(e -> {
			if (t.source == s) {
				t.source = null;
				t.playing = false;
			}
		});
		t.source = s;
		t.playing = true;
		t.startedAt = ctx.getCurrentTime() - offset;
		t.paused = false;
		s.start(0, offset);
	}

	@Override public byte MIX_Init() { return 1; }
	@Override public void MIX_Quit() {}

	@Override public MixMixer MIX_CreateMixerDevice(int devid, SDL_AudioSpec spec) {
		return new JsMixer();
	}

	@Override public void MIX_DestroyMixer(MixMixer mixer) {}

	@Override public MixAudio MIX_LoadAudio(MixMixer mixer, String path, int predecode) {
		if (path == null || path.isEmpty()) return null;
		byte[] bytes = JsAsync.fetchBytes(Assets.toUrl(path));
		if (bytes == null) return null;
		AudioBuffer buffer = JsAsync.decodeAudio(ctx, JsAsync.toArrayBuffer(bytes));
		return buffer != null ? new JsAudio(buffer) : null;
	}

	@Override public void MIX_DestroyAudio(MixAudio audio) {}

	@Override public long MIX_GetAudioDuration(MixAudio audio) {
		return ((JsAudio) audio).buffer.getLength();
	}

	@Override public byte MIX_GetAudioFormat(MixAudio audio, SDL_AudioSpec spec) {
		AudioBuffer b = ((JsAudio) audio).buffer;
		spec.format = SDL_AUDIO_S16;
		spec.channels = 2;
		spec.freq = b.getSampleRate() > 0 ? (int) b.getSampleRate() : 44100;
		return 1;
	}

	@Override public MixTrack MIX_CreateTrack(MixMixer mixer) {
		JsTrack t = new JsTrack();
		tracks.add(t);
		return t;
	}

	@Override public void MIX_DestroyTrack(MixTrack track) {
		JsTrack t = (JsTrack) track;
		stopSource(t);
		tracks.remove(t);
	}

	@Override public byte MIX_SetTrackAudio(MixTrack track, MixAudio audio) {
		if (audio == null) return 0;
		JsTrack t = (JsTrack) track;
		stopSource(t);
		t.buffer = ((JsAudio) audio).buffer;
		t.paused = false;
		t.pendingStart = false;
		return 1;
	}

	@Override public byte MIX_SetTrackLoops(MixTrack track, int numLoops) {
		((JsTrack) track).loop = numLoops != 0;
		return 1;
	}

	@Override public byte MIX_PlayTrack(MixTrack track, int options) {
		JsTrack t = (JsTrack) track;
		if (t.buffer == null) return 0;
		if (!"running".equals(ctx.getState())) {
			// Queue (one-shots included) so the sound fires the instant the
			// context resumes rather than being dropped; the resume() from the
			// same gesture is already in flight but resolves asynchronously.
			t.pendingStart = true;
			resumeAudio();
			return 1;
		}
		startTrack(t, 0);
		return 1;
	}

	@Override public byte MIX_StopTrack(MixTrack track, long fadeOutFrames) {
		JsTrack t = (JsTrack) track;
		stopSource(t);
		t.paused = false;
		t.pendingStart = false;
		return 1;
	}

	@Override public byte MIX_PauseTrack(MixTrack track) {
		JsTrack t = (JsTrack) track;
		if (t.source == null || t.buffer == null) return 1;
		t.pausedAt = (ctx.getCurrentTime() - t.startedAt) % t.buffer.getDuration();
		t.paused = true;
		stopSource(t);
		return 1;
	}

	@Override public byte MIX_ResumeTrack(MixTrack track) {
		JsTrack t = (JsTrack) track;
		if (t.paused) startTrack(t, t.pausedAt);
		return 1;
	}

	@Override public byte MIX_TrackPlaying(MixTrack track) {
		return (byte) (((JsTrack) track).playing ? 1 : 0);
	}

	@Override public byte MIX_SetTrackGain(MixTrack track, float gain) {
		((JsTrack) track).gain.getGain().setValue(gain);
		return 1;
	}
}

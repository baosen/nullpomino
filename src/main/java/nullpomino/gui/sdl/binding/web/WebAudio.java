package nullpomino.gui.sdl.binding.web;

import javax.sound.sampled.AudioFormat;

import nullpomino.gui.sdl.binding.SdlHandles.MixAudio;

/** Fully-decoded PCM audio data. */
final class WebAudio implements MixAudio {
	final AudioFormat format;
	final byte[] data;
	final long frames;

	WebAudio(AudioFormat format, byte[] data) {
		this.format = format;
		this.data = data;
		int frameSize = Math.max(format.getFrameSize(), 1);
		this.frames = data.length / frameSize;
	}
}

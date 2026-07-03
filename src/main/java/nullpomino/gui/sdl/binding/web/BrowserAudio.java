package nullpomino.gui.sdl.binding.web;

/**
 * Native bridge to the page's Web Audio implementation, provided by
 * cheerpjInit({natives}) in web/index.html. CheerpJ's Java 17 runtime has no
 * javax.sound output (AudioSystem.getMixerInfo() is empty), so browser audio
 * goes straight to the Web Audio API instead.
 *
 * On a desktop JVM these methods throw UnsatisfiedLinkError, which the
 * backend catches to fall back to javax.sound.
 *
 * Handles are positive int ids; 0 means failure/none.
 */
final class BrowserAudio {
	private BrowserAudio() {}

	/** @return nonzero if the page provides a working AudioContext */
	static native int jsAudioInit();

	/**
	 * Fetch and decode an audio file.
	 * @param url page-relative URL (the /app filesystem prefix already stripped)
	 * @return audio id, or 0 on failure
	 */
	static native int jsLoadAudio(String url);

	/** @return decoded length in sample frames */
	static native int jsAudioDurationFrames(int audioId);

	/** @return sample rate in Hz */
	static native int jsAudioSampleRate(int audioId);

	/** @return track id (a gain node routed to the destination) */
	static native int jsCreateTrack();

	static native void jsSetTrackAudio(int trackId, int audioId);

	/** @param loops 0 = play once, nonzero = loop forever */
	static native void jsSetTrackLoops(int trackId, int loops);

	static native void jsPlayTrack(int trackId);

	static native void jsStopTrack(int trackId);

	static native void jsPauseTrack(int trackId);

	static native void jsResumeTrack(int trackId);

	/** @return nonzero while the track is playing */
	static native int jsTrackPlaying(int trackId);

	static native void jsSetTrackGain(int trackId, float gain);
}

package nullpomino.gui.sdl.binding.web;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import nullpomino.gui.sdl.binding.SdlHandles.MixTrack;

/** A playback track backed by one javax.sound Clip. */
final class WebTrack implements MixTrack {

	private Clip clip;
	private WebAudio audio;
	private float gain = 1.0f;
	private int loops = 0;

	synchronized boolean setAudio(WebAudio newAudio) {
		if(newAudio == null) return false;
		try {
			if(clip != null) {
				clip.stop();
				clip.close();
				clip = null;
			}
			Clip c = AudioSystem.getClip();
			c.open(newAudio.format, newAudio.data, 0, newAudio.data.length);
			clip = c;
			audio = newAudio;
			applyGain();
			return true;
		} catch(Exception | Error e) {
			clip = null;
			audio = null;
			return false;
		}
	}

	synchronized void setLoops(int numLoops) {
		loops = numLoops;
	}

	synchronized boolean play() {
		if(clip == null) return false;
		try {
			clip.stop();
			clip.setFramePosition(0);
			if(loops < 0) {
				clip.loop(Clip.LOOP_CONTINUOUSLY);
			} else if(loops > 0) {
				clip.loop(loops);
			} else {
				clip.start();
			}
			return true;
		} catch(Exception | Error e) {
			return false;
		}
	}

	synchronized void stop() {
		if(clip != null) {
			clip.stop();
			clip.setFramePosition(0);
		}
	}

	synchronized void pause() {
		if(clip != null) clip.stop();
	}

	synchronized void resume() {
		if(clip != null) clip.start();
	}

	synchronized boolean playing() {
		return clip != null && clip.isActive();
	}

	synchronized void setGain(float newGain) {
		gain = newGain;
		applyGain();
	}

	synchronized void destroy() {
		if(clip != null) {
			clip.stop();
			clip.close();
			clip = null;
		}
		audio = null;
	}

	private void applyGain() {
		if(clip == null || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
		FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		float db;
		if(gain <= 0.0001f) {
			db = control.getMinimum();
		} else {
			db = (float) (20.0 * Math.log10(gain));
		}
		control.setValue(Math.max(control.getMinimum(), Math.min(control.getMaximum(), db)));
	}
}

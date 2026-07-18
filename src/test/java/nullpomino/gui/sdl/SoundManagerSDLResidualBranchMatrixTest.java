package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLStructs.SDL_AudioSpec;
import nullpomino.gui.sdl.binding.SdlBackend;
import nullpomino.gui.sdl.binding.SdlHandles.MixAudio;
import nullpomino.gui.sdl.binding.SdlHandles.MixMixer;
import nullpomino.gui.sdl.binding.SdlHandles.MixTrack;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercises every mixer, pool-selection, warm-up, and teardown predicate. */
class SoundManagerSDLResidualBranchMatrixTest {

	private static final MixMixer MIXER = new MixMixer() {};
	private static final Audio AUDIO_A = new Audio("a");
	private static final Audio AUDIO_B = new Audio("b");
	private static final Audio AUDIO_C = new Audio("c");

	private static FakeMixer fake;
	private static SDL3Mixer mixer;

	@BeforeAll
	static void installBackendBeforeBindingInitialization() {
		fake = new FakeMixer();
		SdlBackend.Backend backend = new SdlBackend.Backend() {
			private SDL3 sdl;
			private SDL3Image image;
			private SDL3TTF ttf;
			@Override public SDL3 sdl3() {
				if(sdl == null) sdl = proxy(SDL3.class, (proxy, method, args) -> {
					if(method.getName().equals("SDL_GetTicks")) return fake.ticks;
					return defaultValue(method.getReturnType(), true);
				});
				return sdl;
			}
			@Override public SDL3Image image() {
				if(image == null) image = proxy(SDL3Image.class,
					(proxy, method, args) -> defaultValue(method.getReturnType(), true));
				return image;
			}
			@Override public SDL3TTF ttf() {
				if(ttf == null) ttf = proxy(SDL3TTF.class,
					(proxy, method, args) -> defaultValue(method.getReturnType(), true));
				return ttf;
			}
			@Override public SDL3Mixer mixerOrNull() {
				if(mixer == null) mixer = proxy(SDL3Mixer.class, fake);
				return mixer;
			}
		};
		SdlBackend.set(backend);
	}

	@BeforeEach
	void resetMixerState() {
		fake.reset();
		NullpoMinoSDL.propConfig = new CustomProperties();
		NullpoMinoSDL.propConfig.setProperty("option.sevolume", 128);
		NullpoMinoSDL.mixerLib = SDL3Mixer.loadOrNull();
		NullpoMinoSDL.mixer = MIXER;
		NullpoMinoSDL.webMode = false;
		SoundManagerSDL.currentTimeMillis = System::currentTimeMillis;
	}

	@AfterEach
	void restoreClockAndMixer() {
		SoundManagerSDL.currentTimeMillis = System::currentTimeMillis;
		NullpoMinoSDL.mixerLib = null;
		NullpoMinoSDL.mixer = null;
		NullpoMinoSDL.webMode = false;
	}

	@Test
	void constructorLoadVolumeAndDestroyCoverNullableMixerResources() throws Exception {
		NullpoMinoSDL.mixerLib = null;
		new SoundManagerSDL();
		NullpoMinoSDL.mixerLib = mixer;
		NullpoMinoSDL.mixer = null;
		new SoundManagerSDL();

		NullpoMinoSDL.mixer = MIXER;
		fake.nullTrackIndexes.add(1);
		SoundManagerSDL manager = new SoundManagerSDL();
		assertNotNull(tracks(manager)[0]);
		assertEquals(null, tracks(manager)[1]);

		NullpoMinoSDL.mixerLib = null;
		assertFalse(manager.load("guard-lib", "ok"));
		NullpoMinoSDL.mixerLib = mixer;
		NullpoMinoSDL.mixer = null;
		assertFalse(manager.load("guard-mixer", "ok"));
		NullpoMinoSDL.mixer = MIXER;
		fake.nullLoadNames.add("null");
		fake.throwLoadNames.add("throw");
		assertFalse(manager.load("null", "null"));
		assertFalse(manager.load("throw", "throw"));

		fake.loadedAudio.put("unknown-frames", AUDIO_A);
		fake.durationFrames.put(AUDIO_A, 0L);
		assertTrue(manager.load("unknown-frames", "unknown-frames"));
		fake.loadedAudio.put("unknown-format", AUDIO_B);
		fake.durationFrames.put(AUDIO_B, 100L);
		fake.formatSuccess.put(AUDIO_B, false);
		assertTrue(manager.load("unknown-format", "unknown-format"));
		fake.loadedAudio.put("unknown-frequency", AUDIO_C);
		fake.durationFrames.put(AUDIO_C, 100L);
		fake.frequency.put(AUDIO_C, 0);
		assertTrue(manager.load("unknown-frequency", "unknown-frequency"));
		Audio valid = new Audio("valid");
		fake.loadedAudio.put("valid", valid);
		fake.durationFrames.put(valid, 1L);
		fake.frequency.put(valid, 48_000);
		assertTrue(manager.load("valid", "valid"));

		manager.changeVolume(-1);
		manager.changeVolume(256);
		NullpoMinoSDL.mixerLib = null;
		manager.changeVolume(64);
		manager.destroy();
		NullpoMinoSDL.mixerLib = mixer;
		clips(manager).put("audio", AUDIO_A);
		clips(manager).put("null-audio", null);
		manager.destroy();
		assertTrue(fake.destroyedTracks > 0);
		assertTrue(fake.destroyedAudio > 0);
	}

	@Test
	void warmUpCoversGuardsSelectionPollingDeadlineAndNullableTracks() throws Exception {
		SoundManagerSDL manager = new SoundManagerSDL();
		NullpoMinoSDL.mixerLib = null;
		manager.warmUp();
		NullpoMinoSDL.mixerLib = mixer;
		manager.warmUp();

		clips(manager).put("cursor", AUDIO_A);
		durations(manager).put("cursor", 10L);
		NullpoMinoSDL.webMode = true;
		manager.warmUp();
		NullpoMinoSDL.webMode = false;
		clips(manager).put("cursor", null);
		manager.warmUp();

		clips(manager).put("cursor", AUDIO_A);
		MixTrack[] tracks = tracks(manager);
		tracks[1] = null;
		fake.playingUntilCall = 1;
		AtomicLong shortClock = new AtomicLong();
		SoundManagerSDL.currentTimeMillis = shortClock::incrementAndGet;
		manager.warmUp();
		assertTrue(fake.gainCalls > 0);

		fake.playingUntilCall = Integer.MAX_VALUE;
		AtomicLong deadlineClock = new AtomicLong();
		SoundManagerSDL.currentTimeMillis = () -> deadlineClock.addAndGet(3_000L);
		manager.warmUp();

		clips(manager).clear();
		durations(manager).clear();
		clips(manager).put("long", AUDIO_A);
		durations(manager).put("long", 100L);
		clips(manager).put("short", AUDIO_B);
		durations(manager).put("short", 10L);
		clips(manager).put("middle", AUDIO_C);
		durations(manager).put("middle", 50L);
		assertEquals("short", invoke(manager, "getWarmUpClipName", new Class<?>[0]));
	}

	@Test
	void publicPlayCoversMuteMissingAudioNoTrackDedicatedAndSharedPaths() throws Exception {
		SoundManagerSDL manager = new SoundManagerSDL();
		clips(manager).put("cursor", AUDIO_A);
		durations(manager).put("cursor", 10L);
		clips(manager).put("shared", AUDIO_B);
		durations(manager).put("shared", 20L);

		manager.mute = true;
		manager.play("cursor");
		manager.mute = false;
		NullpoMinoSDL.mixerLib = null;
		manager.play("cursor");
		NullpoMinoSDL.mixerLib = mixer;
		manager.play("missing");

		fake.ticks = 100;
		manager.play("cursor");
		manager.play("cursor");
		manager.play("shared");

		Arrays.fill(tracks(manager), null);
		manager.play("shared");
		manager.play("cursor");
		assertTrue(fake.playCalls > 0);
	}

	@Test
	void selectionAndPlaybackHelpersCoverEveryShortCircuitPosition() throws Exception {
		SoundManagerSDL manager = new SoundManagerSDL();
		MixTrack[] tracks = tracks(manager);
		MixAudio[] audio = field(manager, "trackAudio");
		String[] names = field(manager, "trackAudioName");
		String[] dedicated = field(manager, "trackDedicatedSound");
		long[] ready = field(manager, "trackReadyAtTicks");
		Map<String, Integer> dedicatedMap = field(manager, "dedicatedTrackBySound");
		durations(manager).put("cursor", 10L);
		durations(manager).put("shared", 10L);

		tracks[0] = null;
		assertFalse((boolean)invoke(manager, "isTrackIdle",
			new Class<?>[]{SDL3Mixer.class, long.class, int.class}, mixer, 100L, 0));
		tracks[0] = new Track(100);
		ready[0] = 200;
		assertFalse((boolean)invoke(manager, "isTrackIdle",
			new Class<?>[]{SDL3Mixer.class, long.class, int.class}, mixer, 100L, 0));
		ready[0] = 0;
		fake.playing.put(tracks[0], (byte)1);
		assertFalse((boolean)invoke(manager, "isTrackIdle",
			new Class<?>[]{SDL3Mixer.class, long.class, int.class}, mixer, 100L, 0));
		fake.playing.put(tracks[0], (byte)0);
		assertTrue((boolean)invoke(manager, "isTrackIdle",
			new Class<?>[]{SDL3Mixer.class, long.class, int.class}, mixer, 100L, 0));

		dedicated[0] = "owned";
		assertFalse((boolean)invoke(manager, "isDedicatedTrackCandidate",
			new Class<?>[]{SDL3Mixer.class, long.class, int.class}, mixer, 100L, 0));
		assertFalse((boolean)invoke(manager, "isSharedTrackCandidate",
			new Class<?>[]{SDL3Mixer.class, long.class, int.class}, mixer, 100L, 0));
		dedicated[0] = null;
		assertTrue((boolean)invoke(manager, "isDedicatedTrackCandidate",
			new Class<?>[]{SDL3Mixer.class, long.class, int.class}, mixer, 100L, 0));
		assertTrue((boolean)invoke(manager, "isSharedTrackCandidate",
			new Class<?>[]{SDL3Mixer.class, long.class, int.class}, mixer, 100L, 0));
		tracks[0] = null;
		invoke(manager, "isDedicatedTrackCandidate",
			new Class<?>[]{SDL3Mixer.class, long.class, int.class}, mixer, 100L, 0);
		invoke(manager, "isSharedTrackCandidate",
			new Class<?>[]{SDL3Mixer.class, long.class, int.class}, mixer, 100L, 0);

		resetPool(manager);
		tracks = tracks(manager);
		audio = field(manager, "trackAudio");
		names = field(manager, "trackAudioName");
		dedicated = field(manager, "trackDedicatedSound");
		ready = field(manager, "trackReadyAtTicks");
		audio[0] = AUDIO_A;
		names[0] = "other";
		assertEquals(0, invoke(manager, "findBestIdleTrack",
			new Class<?>[]{SDL3Mixer.class, long.class, MixAudio.class, String.class},
			mixer, 100L, AUDIO_A, "cursor"));
		names[0] = "cursor";
		assertEquals(0, invoke(manager, "findBestIdleTrack",
			new Class<?>[]{SDL3Mixer.class, long.class, MixAudio.class, String.class},
			mixer, 100L, AUDIO_A, "cursor"));
		audio[0] = AUDIO_B;
		assertEquals(0, invoke(manager, "findBestIdleTrack",
			new Class<?>[]{SDL3Mixer.class, long.class, MixAudio.class, String.class},
			mixer, 100L, AUDIO_A, "cursor"));
		Arrays.fill(tracks, null);
		assertEquals(-1, invoke(manager, "findBestIdleTrack",
			new Class<?>[]{SDL3Mixer.class, long.class, MixAudio.class, String.class},
			mixer, 100L, AUDIO_A, "cursor"));

		resetPool(manager);
		tracks = tracks(manager);
		audio = field(manager, "trackAudio");
		names = field(manager, "trackAudioName");
		audio[0] = AUDIO_A;
		names[0] = "other";
		assertEquals(0, invoke(manager, "findBestSharedTrack",
			new Class<?>[]{SDL3Mixer.class, long.class, MixAudio.class, String.class},
			mixer, 100L, AUDIO_A, "shared"));
		names[0] = "shared";
		assertEquals(0, invoke(manager, "findBestSharedTrack",
			new Class<?>[]{SDL3Mixer.class, long.class, MixAudio.class, String.class},
			mixer, 100L, AUDIO_A, "shared"));
		audio[0] = AUDIO_B;
		assertEquals(0, invoke(manager, "findBestSharedTrack",
			new Class<?>[]{SDL3Mixer.class, long.class, MixAudio.class, String.class},
			mixer, 100L, AUDIO_A, "shared"));
		Arrays.fill(tracks, null);
		assertEquals(-1, invoke(manager, "findBestSharedTrack",
			new Class<?>[]{SDL3Mixer.class, long.class, MixAudio.class, String.class},
			mixer, 100L, AUDIO_A, "shared"));

		resetPool(manager);
		tracks = tracks(manager);
		dedicatedMap.clear();
		dedicatedMap.put("cursor", -1);
		invokeFind(manager, "cursor", AUDIO_A);
		dedicatedMap.put("cursor", 16);
		invokeFind(manager, "cursor", AUDIO_A);
		dedicatedMap.put("cursor", 0);
		tracks[0] = null;
		invokeFind(manager, "cursor", AUDIO_A);
		tracks[0] = new Track(200);
		dedicatedMap.put("cursor", 0);
		assertEquals(0, invokeFind(manager, "cursor", AUDIO_A));

		tracks[0] = null;
		invoke(manager, "playOnTrack",
			new Class<?>[]{SDL3Mixer.class, int.class, MixAudio.class, String.class, long.class},
			mixer, 0, AUDIO_A, "cursor", 100L);
		tracks[0] = new Track(201);
		audio = field(manager, "trackAudio");
		audio[0] = AUDIO_B;
		fake.setAudioResult = 0;
		invokePlayOnTrack(manager, AUDIO_A);
		fake.setAudioResult = 1;
		fake.playResult = 0;
		invokePlayOnTrack(manager, AUDIO_A);
		fake.playResult = 1;
		invokePlayOnTrack(manager, AUDIO_A);
		invokePlayOnTrack(manager, AUDIO_A);

		fake.playing.put(tracks[0], (byte)1);
		assertFalse((boolean)invoke(manager, "areAllTracksStopped",
			new Class<?>[]{SDL3Mixer.class}, mixer));
		fake.playing.put(tracks[0], (byte)0);
		assertTrue((boolean)invoke(manager, "areAllTracksStopped",
			new Class<?>[]{SDL3Mixer.class}, mixer));
	}

	private static int invokeFind(SoundManagerSDL manager, String name, MixAudio audio)
		throws Exception {
		return (int)invoke(manager, "findTrackForSound",
			new Class<?>[]{SDL3Mixer.class, long.class, MixAudio.class, String.class},
			mixer, 100L, audio, name);
	}

	private static void invokePlayOnTrack(SoundManagerSDL manager, MixAudio audio)
		throws Exception {
		invoke(manager, "playOnTrack",
			new Class<?>[]{SDL3Mixer.class, int.class, MixAudio.class, String.class, long.class},
			mixer, 0, audio, "cursor", 100L);
	}

	private static void resetPool(SoundManagerSDL manager) throws Exception {
		MixTrack[] tracks = tracks(manager);
		MixAudio[] audio = field(manager, "trackAudio");
		String[] names = field(manager, "trackAudioName");
		String[] dedicated = field(manager, "trackDedicatedSound");
		long[] ready = field(manager, "trackReadyAtTicks");
		for(int i = 0; i < tracks.length; i++) {
			tracks[i] = new Track(i);
			audio[i] = null;
			names[i] = null;
			dedicated[i] = null;
			ready[i] = 0;
			fake.playing.put(tracks[i], (byte)0);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, MixAudio> clips(SoundManagerSDL manager) throws Exception {
		return (Map<String, MixAudio>)get(manager, "clipMap");
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Long> durations(SoundManagerSDL manager) throws Exception {
		return (Map<String, Long>)get(manager, "clipDurationMap");
	}

	private static MixTrack[] tracks(SoundManagerSDL manager) throws Exception {
		return (MixTrack[])get(manager, "tracks");
	}

	@SuppressWarnings("unchecked")
	private static <T> T field(SoundManagerSDL manager, String name) throws Exception {
		return (T)get(manager, name);
	}

	private static Object get(SoundManagerSDL manager, String name) throws Exception {
		Field field = SoundManagerSDL.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(manager);
	}

	private static Object invoke(SoundManagerSDL manager, String name, Class<?>[] types,
		Object... args) throws Exception {
		Method method = SoundManagerSDL.class.getDeclaredMethod(name, types);
		method.setAccessible(true);
		return method.invoke(manager, args);
	}

	private static <T> T proxy(Class<T> type, InvocationHandler handler) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
	}

	private static Object defaultValue(Class<?> type, boolean successfulByte) {
		if(type == void.class) return null;
		if(type == byte.class) return (byte)(successfulByte ? 1 : 0);
		if(type == short.class) return (short)0;
		if(type == int.class) return 0;
		if(type == long.class) return 0L;
		if(type == float.class) return 0f;
		if(type == double.class) return 0d;
		if(type == boolean.class) return false;
		if(type == int[].class) return new int[0];
		return null;
	}

	private static final class Track implements MixTrack {
		final int id;
		Track(int id) { this.id = id; }
	}

	private static final class Audio implements MixAudio {
		final String name;
		Audio(String name) { this.name = name; }
	}

	private static final class FakeMixer implements InvocationHandler {
		final Set<Integer> nullTrackIndexes = new HashSet<>();
		final Set<String> nullLoadNames = new HashSet<>();
		final Set<String> throwLoadNames = new HashSet<>();
		final Map<String, MixAudio> loadedAudio = new HashMap<>();
		final Map<MixAudio, Long> durationFrames = new HashMap<>();
		final Map<MixAudio, Boolean> formatSuccess = new HashMap<>();
		final Map<MixAudio, Integer> frequency = new HashMap<>();
		final Map<MixTrack, Byte> playing = new HashMap<>();
		int createTrackIndex;
		int playingCalls;
		int playingUntilCall;
		byte setAudioResult = 1;
		byte playResult = 1;
		long ticks;
		int gainCalls;
		int playCalls;
		int destroyedTracks;
		int destroyedAudio;

		void reset() {
			nullTrackIndexes.clear();
			nullLoadNames.clear();
			throwLoadNames.clear();
			loadedAudio.clear();
			durationFrames.clear();
			formatSuccess.clear();
			frequency.clear();
			playing.clear();
			createTrackIndex = playingCalls = playingUntilCall = 0;
			setAudioResult = playResult = 1;
			ticks = 0;
			gainCalls = playCalls = destroyedTracks = destroyedAudio = 0;
		}

		@Override public Object invoke(Object proxy, Method method, Object[] args) {
			String name = method.getName();
			if(name.equals("toString")) return "FakeMixer";
			if(name.equals("hashCode")) return System.identityHashCode(proxy);
			if(name.equals("equals")) return proxy == args[0];
			if(name.equals("MIX_CreateTrack")) {
				int index = createTrackIndex++;
				return nullTrackIndexes.contains(index) ? null : new Track(index);
			}
			if(name.equals("MIX_LoadAudio")) {
				String filename = (String)args[1];
				if(throwLoadNames.contains(filename)) throw new IllegalStateException("load failure");
				if(nullLoadNames.contains(filename)) return null;
				return loadedAudio.computeIfAbsent(filename, Audio::new);
			}
			if(name.equals("MIX_GetAudioDuration"))
				return durationFrames.getOrDefault((MixAudio)args[0], 48_000L);
			if(name.equals("MIX_GetAudioFormat")) {
				MixAudio audio = (MixAudio)args[0];
				((SDL_AudioSpec)args[1]).freq = frequency.getOrDefault(audio, 48_000);
				return (byte)(formatSuccess.getOrDefault(audio, true) ? 1 : 0);
			}
			if(name.equals("MIX_SetTrackGain")) { gainCalls++; return (byte)1; }
			if(name.equals("MIX_SetTrackAudio")) return setAudioResult;
			if(name.equals("MIX_PlayTrack")) { playCalls++; return playResult; }
			if(name.equals("MIX_TrackPlaying")) {
				playingCalls++;
				if(playingCalls <= playingUntilCall) return (byte)1;
				return playing.getOrDefault((MixTrack)args[0], (byte)0);
			}
			if(name.equals("MIX_DestroyTrack")) { destroyedTracks++; return null; }
			if(name.equals("MIX_DestroyAudio")) { destroyedAudio++; return null; }
			return defaultValue(method.getReturnType(), true);
		}
	}
}

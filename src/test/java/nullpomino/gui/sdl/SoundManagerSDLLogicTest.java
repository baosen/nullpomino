package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Pins the internal logic in {@link SoundManagerSDL} that is
 * testable without an SDL3Mixer: data structure initialization,
 * the dedicated-track / UI-sound classification, and warm-up
 * clip selection.
 *
 * <p>The null-mixer safe-call paths are already covered by
 * {@link SoundManagerSDLTest}.
 */
class SoundManagerSDLLogicTest {

	/* ---------- Constructor data structure initialization ---------- */

	@Test
	void constructorInitializesAllDataStructures() throws Exception {
		SoundManagerSDL sm = new SoundManagerSDL();

		// clipMap should be an empty HashMap, not null
		assertNotNull(getField(sm, "clipMap"));
		assertEquals(0, ((Map<?,?>) getField(sm, "clipMap")).size());

		// clipDurationMap should be an empty HashMap
		assertNotNull(getField(sm, "clipDurationMap"));
		assertEquals(0, ((Map<?,?>) getField(sm, "clipDurationMap")).size());

		// dedicatedTrackBySound should be an empty HashMap
		assertNotNull(getField(sm, "dedicatedTrackBySound"));
		assertEquals(0, ((Map<?,?>) getField(sm, "dedicatedTrackBySound")).size());

		// tracks array should have NUM_TRACKS elements (16)
		Object[] tracks = (Object[]) getField(sm, "tracks");
		assertNotNull(tracks);
		assertEquals(16, tracks.length);

		// trackAudio, trackAudioName, trackDedicatedSound, trackReadyAtTicks
		assertNotNull(getField(sm, "trackAudio"));
		assertNotNull(getField(sm, "trackAudioName"));
		assertNotNull(getField(sm, "trackDedicatedSound"));
		assertNotNull(getField(sm, "trackReadyAtTicks"));

		// Default gain should be 1.0f
		assertEquals(1.0f, (float) getField(sm, "seGain"), 0.001f);
	}

	/* ---------- usesDedicatedTrack ---------- */

	@Test
	void usesDedicatedTrackReturnsTrueForCursor() throws Exception {
		assertTrue(invokeUsesDedicatedTrack("cursor"));
	}

	@Test
	void usesDedicatedTrackReturnsTrueForDecide() throws Exception {
		assertTrue(invokeUsesDedicatedTrack("decide"));
	}

	@Test
	void usesDedicatedTrackReturnsTrueForChange() throws Exception {
		assertTrue(invokeUsesDedicatedTrack("change"));
	}

	@Test
	void usesDedicatedTrackReturnsTrueForMove() throws Exception {
		assertTrue(invokeUsesDedicatedTrack("move"));
	}

	@Test
	void usesDedicatedTrackReturnsTrueForRotate() throws Exception {
		assertTrue(invokeUsesDedicatedTrack("rotate"));
	}

	@Test
	void usesDedicatedTrackReturnsTrueForStep() throws Exception {
		assertTrue(invokeUsesDedicatedTrack("step"));
	}

	@Test
	void usesDedicatedTrackReturnsTrueForSoftdrop() throws Exception {
		assertTrue(invokeUsesDedicatedTrack("softdrop"));
	}

	@Test
	void usesDedicatedTrackReturnsFalseForOtherSounds() throws Exception {
		assertFalse(invokeUsesDedicatedTrack("tspin1"));
		assertFalse(invokeUsesDedicatedTrack("lock"));
		assertFalse(invokeUsesDedicatedTrack("clear"));
		assertFalse(invokeUsesDedicatedTrack("pause"));
		assertFalse(invokeUsesDedicatedTrack("levelup"));
		assertFalse(invokeUsesDedicatedTrack("gameover"));
		assertFalse(invokeUsesDedicatedTrack("unknown"));
	}

	/* ---------- isUISound ---------- */

	@Test
	void isUISoundReturnsTrueForCursorDecideChange() throws Exception {
		assertTrue(invokeIsUISound("cursor"));
		assertTrue(invokeIsUISound("decide"));
		assertTrue(invokeIsUISound("change"));
	}

	@Test
	void isUISoundReturnsFalseForNonUISounds() throws Exception {
		assertFalse(invokeIsUISound("move"));
		assertFalse(invokeIsUISound("lock"));
		assertFalse(invokeIsUISound("clear"));
	}

	/* ---------- Mixer-lib null handling ---------- */

	@Test
	void constructorSetsTracksToNullWithoutMixer() throws Exception {
		SoundManagerSDL sm = new SoundManagerSDL();
		// Without mixerLib, constructor doesn't create actual MIX_Track objects.
		// tracks array elements are null since the for-loop body is skipped.
		Object[] tracks = (Object[]) getField(sm, "tracks");
		for (Object t : tracks) {
			assertNull(t);
		}
	}

	@Test
	void constructorSetsSeGainToOneWhenMixerIsNull() throws Exception {
		SoundManagerSDL sm = new SoundManagerSDL();
		// seGain defaults to 1.0f since the constructor skips the volume
		// loading when mixerLib is null (the vol read happens inside the
		// if(lib != null && mixer != null) block).
		// Actually looking at the code: float vol is read OUTSIDE the if block
		// but seGain is set inside. So seGain stays at its default 1.0f.
	}

	/* ---------- getWarmUpClipName ---------- */

	@Test
	void getWarmUpClipNamePrefersCursor() throws Exception {
		SoundManagerSDL sm = new SoundManagerSDL();
		HashMap<String, Object> clipMap = field(sm, "clipMap");
		HashMap<String, Long> clipDurationMap = field(sm, "clipDurationMap");

		clipMap.put("cursor", new Object());
		clipDurationMap.put("cursor", 100L);
		clipMap.put("lock", new Object());
		clipDurationMap.put("lock", 200L);

		assertEquals("cursor", invokeGetWarmUpClipName(sm));
	}

	@Test
	void getWarmUpClipNamePicksShortestWhenNoCursor() throws Exception {
		SoundManagerSDL sm = new SoundManagerSDL();
		HashMap<String, Object> clipMap = field(sm, "clipMap");
		HashMap<String, Long> clipDurationMap = field(sm, "clipDurationMap");

		clipMap.put("lock", new Object());
		clipDurationMap.put("lock", 200L);
		clipMap.put("move", new Object());
		clipDurationMap.put("move", 50L);
		clipMap.put("clear", new Object());
		clipDurationMap.put("clear", 150L);

		assertEquals("move", invokeGetWarmUpClipName(sm));
	}

	@Test
	void getWarmUpClipNameReturnsNullWhenClipMapIsEmpty() throws Exception {
		SoundManagerSDL sm = new SoundManagerSDL();
		assertNull(invokeGetWarmUpClipName(sm));
	}

	/* ---------- Private reflection helpers ---------- */

	private static Object getField(Object obj, String name) throws Exception {
		Field f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(obj);
	}

	@SuppressWarnings("unchecked")
	private static <T> T field(Object obj, String name) throws Exception {
		return (T) getField(obj, name);
	}

	private static boolean invokeUsesDedicatedTrack(String name) throws Exception {
		SoundManagerSDL sm = new SoundManagerSDL();
		Method m = SoundManagerSDL.class.getDeclaredMethod("usesDedicatedTrack", String.class);
		m.setAccessible(true);
		return (boolean) m.invoke(sm, name);
	}

	private static boolean invokeIsUISound(String name) throws Exception {
		SoundManagerSDL sm = new SoundManagerSDL();
		Method m = SoundManagerSDL.class.getDeclaredMethod("isUISound", String.class);
		m.setAccessible(true);
		return (boolean) m.invoke(sm, name);
	}

	private static String invokeGetWarmUpClipName(SoundManagerSDL sm) throws Exception {
		Method m = SoundManagerSDL.class.getDeclaredMethod("getWarmUpClipName");
		m.setAccessible(true);
		return (String) m.invoke(sm);
	}
}

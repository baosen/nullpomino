package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins the {@code modeInit} per-room state allocations on
 * {@link NetVSLineRaceMode} and {@link NetVSBattleMode}.
 *
 * <p>NetVSLineRaceMode seeds {@code goalLines = 40} (the documented
 * line-race target). NetVSBattleMode allocates a 9-array per-player
 * state vector at NETVS_MAX_PLAYERS=6 covering K.O. tracking, score
 * timing, last-event memory, B2B/combo/piece memory, garbage
 * counters, and APL/APM stats.
 */
class NetVSLineRaceAndBattleModeInitTest {

	@Test
	void netVSLineRaceSeedsGoalLinesAtFortyInModeInit() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		assertEquals(40, readInt(mode, "goalLines"),
				"NET-VS-LINE RACE seeds goalLines at 40 in modeInit");
	}

	@Test
	void netVSLineRaceParentArraysAreAlsoAllocatedAtNetVsMaxPlayers() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		assertEquals(6, ((boolean[]) read(mode, "netvsPlayerExist")).length);
		assertEquals(6, ((int[]) read(mode, "netvsPlayerSeatID")).length);
	}

	@Test
	void netVSBattleAllocatesAllNinePerPlayerArraysAtNetVsMaxPlayers() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		// NETVS_MAX_PLAYERS = 6 — every per-player vector here matches that.
		assertEquals(6, ((boolean[]) read(mode, "playerKObyYou")).length);
		assertEquals(6, ((int[]) read(mode, "scgettime")).length);
		assertEquals(6, ((int[]) read(mode, "lastevent")).length);
		assertEquals(6, ((boolean[]) read(mode, "lastb2b")).length);
		assertEquals(6, ((int[]) read(mode, "lastcombo")).length);
		assertEquals(6, ((int[]) read(mode, "lastpiece")).length);
		assertEquals(6, ((int[]) read(mode, "garbageSent")).length);
		assertEquals(6, ((int[]) read(mode, "garbage")).length);
		assertEquals(6, ((float[]) read(mode, "playerAPL")).length);
		assertEquals(6, ((float[]) read(mode, "playerAPM")).length);
	}

	@Test
	void netVSBattleParentNetDummyArraysAreAlsoAllocated() throws Exception {
		// Belt-and-braces: super.modeInit must run first so the
		// parent's arrays exist alongside the subclass-specific ones.
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		assertNotNull(read(mode, "netvsPlayerExist"));
		assertNotNull(read(mode, "netvsPlayerSkin"));
		assertNotNull(read(mode, "netvsPlayerName"));
	}

	@Test
	void netVSBattlePerPlayerArraysAreFreshAndZeroSeeded() throws Exception {
		// modeInit allocates new arrays each call; the int[] / float[] /
		// boolean[] defaults give 0 / 0f / false for every slot.
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		boolean[] kObyYou = (boolean[]) read(mode, "playerKObyYou");
		int[] garbageSent = (int[]) read(mode, "garbageSent");
		float[] apm = (float[]) read(mode, "playerAPM");
		for(boolean v : kObyYou) assertEquals(false, v);
		for(int v : garbageSent) assertEquals(0, v);
		for(float v : apm) assertEquals(0f, v);
	}

	private static int readInt(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.getInt(instance);
	}

	private static Object read(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.get(instance);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while(c != null) {
			try {
				return c.getDeclaredField(name);
			} catch(NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}

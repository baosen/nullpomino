package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link NetVSDigRaceMode}'s {@code modeInit} per-room state
 * allocation. Beyond the parent's NETVS_MAX_PLAYERS arrays, this
 * subclass allocates {@code playerRemainLines} and
 * {@code playerStartGems} both at NETVS_MAX_PLAYERS=6, and seeds
 * {@code goalLines} at the documented 18.
 */
class NetVSDigRaceModeModeInitTest {

	@Test
	void modeInitAllocatesPerPlayerArraysAndSeedsGoalLines() throws Exception {
		NetVSDigRaceMode mode = new NetVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		// goalLines is the documented Dig Race target (18 garbage rows).
		assertEquals(18, readInt(mode, "goalLines"),
				"NET-VS-DIG RACE seeds goalLines at 18 in modeInit");

		int[] playerRemainLines = (int[]) read(mode, "playerRemainLines");
		int[] playerStartGems = (int[]) read(mode, "playerStartGems");
		assertNotNull(playerRemainLines);
		assertEquals(6, playerRemainLines.length,
				"per-player array sized at NETVS_MAX_PLAYERS=6");
		assertNotNull(playerStartGems);
		assertEquals(6, playerStartGems.length,
				"per-player gem-tally array sized at NETVS_MAX_PLAYERS=6");
		// All entries default to 0 from the int[] allocation; pin that
		// modeInit doesn't seed them with anything else.
		for(int v : playerRemainLines) assertEquals(0, v);
		for(int v : playerStartGems) assertEquals(0, v);
	}

	@Test
	void modeInitParentArraysAreAlsoAllocatedForTheNetVSDigRaceSubclass() throws Exception {
		// Belt-and-braces: confirm the parent class's array-allocation
		// happens first so the subclass-specific arrays don't accidentally
		// hide a parent regression.
		NetVSDigRaceMode mode = new NetVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		assertEquals(6, ((boolean[]) read(mode, "netvsPlayerExist")).length);
		assertEquals(6, ((int[]) read(mode, "netvsPlayerSeatID")).length);
		assertEquals(6, ((int[]) read(mode, "netvsPlayerSkin")).length);
	}

	private static int readInt(NetVSDigRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static Object read(NetVSDigRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
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

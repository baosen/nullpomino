package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.subsystem.mode.GameMode;

import org.junit.jupiter.api.Test;

class ModeManagerTest {

	@Test
	void lookupsHandleInvalidIdsNamesAndNullEntries() {
		ModeManager manager = new ModeManager();
		TestMode normal = new TestMode("normal", false);
		TestMode net = new TestMode("net", true);
		manager.modelist.add(normal);
		manager.modelist.add(null);
		manager.modelist.add(net);

		assertEquals(3, manager.getSize());
		assertEquals("*INVALID MODE*", manager.getName(-1));
		assertEquals("*INVALID MODE*", manager.getName(1));
		assertEquals("*INVALID MODE*", manager.getName(99));
		assertNull(manager.getMode(-1));
		assertNull(manager.getMode(1));
		assertNull(manager.getMode("missing"));
		assertEquals(-1, manager.getIDbyName(null));
		assertSame(normal, manager.getMode("normal"));
		assertSame(net, manager.getMode("net"));
	}

	@Test
	void modeNamesFilterByNetplayFlag() {
		ModeManager manager = new ModeManager();
		manager.modelist.add(new TestMode("normal-a", false));
		manager.modelist.add(new TestMode("net-a", true));
		manager.modelist.add(new TestMode("normal-b", false));

		assertEquals(2, manager.getNumberOfModes(false));
		assertEquals(1, manager.getNumberOfModes(true));
		assertArrayEquals(new String[] {"normal-a", "normal-b"}, manager.getModeNames(false));
		assertArrayEquals(new String[] {"net-a"}, manager.getModeNames(true));
		assertArrayEquals(new String[] {"normal-a", "net-a", "normal-b"}, manager.getAllModeNames());
	}

	@Test
	void copyConstructorSharesLoadedModeReferences() {
		ModeManager manager = new ModeManager();
		TestMode mode = new TestMode("normal", false);
		manager.modelist.add(mode);

		ModeManager copy = new ModeManager(manager);

		assertSame(mode, copy.getMode(0));
		assertEquals(1, copy.getSize());
	}

	static final class TestMode implements GameMode {
		private final String name;
		private final boolean netplay;

		TestMode(String name, boolean netplay) {
			this.name = name;
			this.netplay = netplay;
		}

		public String getName() {
			return name;
		}

		public int getPlayers() {
			return 1;
		}

		public int getGameStyle() {
			return 0;
		}

		public void modeInit(GameManager manager) {
		}

		public void playerInit(GameEngine engine, int playerID) {
		}

		public void renderInput(GameEngine engine, int playerID) {
		}

		public boolean isNetplayMode() {
			return netplay;
		}
	}
}

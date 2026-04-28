package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

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
		manager.addMode(normal);
		manager.addMode(null);
		manager.addMode(net);

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
		manager.addMode(new TestMode("normal-a", false));
		manager.addMode(new TestMode("net-a", true));
		manager.addMode(new TestMode("normal-b", false));

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
		manager.addMode(mode);

		ModeManager copy = new ModeManager(manager);

		assertSame(mode, copy.getMode(0));
		assertEquals(1, copy.getSize());
	}

	@Test
	void loadGameModesFromClassListInstantiatesEachEntry() {
		ModeManager manager = new ModeManager();

		manager.loadGameModes(List.of(LoadableMode.class, LoadableMode.class));

		assertEquals(2, manager.getSize());
		assertEquals("loadable", manager.getName(0));
		assertEquals("loadable", manager.getName(1));
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

	public static final class LoadableMode implements GameMode {
		public String getName() {
			return "loadable";
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
	}
}

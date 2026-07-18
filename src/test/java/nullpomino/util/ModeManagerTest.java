package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.mode.GameMode;

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

		assertNull(manager.getMode(-1));
		assertNull(manager.getMode(1));
		assertNull(manager.getMode(3));
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

		assertArrayEquals(new String[] {"normal-a", "normal-b"}, manager.getModeNames(false));
		assertArrayEquals(new String[] {"net-a"}, manager.getModeNames(true));
	}

	@Test
	void filteredModeCountsAndNamesIgnoreNullEntries() {
		ModeManager manager = new ModeManager();
		manager.addMode(null);
		manager.addMode(new TestMode("normal", false));
		manager.addMode(new TestMode("net", true));

		assertArrayEquals(new String[] {"normal"}, manager.getModeNames(false));
		assertArrayEquals(new String[] {"net"}, manager.getModeNames(true));
	}

	@Test
	void loadGameModesFromClassListInstantiatesEachEntry() {
		ModeManager manager = new ModeManager();

		manager.loadGameModes(List.of(LoadableMode.class, LoadableMode.class));

		assertArrayEquals(new String[] {"loadable", "loadable"}, manager.getModeNames(false));
	}

	@Test
	void loadGameModesSkipsConstructorThatThrows() {
		ModeManager manager = new ModeManager();

		manager.loadGameModes(List.of(LoadableMode.class, FailingMode.class, LoadableMode.class));

		assertArrayEquals(new String[] {"loadable", "loadable"}, manager.getModeNames(false));
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

	public static final class FailingMode implements GameMode {
		public FailingMode() {
			throw new IllegalStateException("intentional load failure");
		}

		public String getName() {
			return "failing";
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

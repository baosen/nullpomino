package nullpomino.game.mode;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

/**
 * Tail coverage for {@link NetVSDigRaceMode}: calcScore map-game remaining-gems
 * branch, and the renderLast place rendering branches (2ND..6TH) in both the
 * menu-font and small/direct-font layouts, driven by computed places 0..5.
 */
class NetVSDigRaceModeTailCoverageTest {

	private NetVSDigRaceMode mode;
	private GameManager manager;
	private GameEngine engine;

	@BeforeEach
	void setUp() throws Exception {
		mode = new NetVSDigRaceMode();
		manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		for (int i = 0; i < manager.engine.length; i++) {
			manager.engine[i].init();
			manager.engine[i].createFieldIfNeeded();
		}
		engine = manager.engine[0];
		engine.stat = GameEngine.Status.MOVE;
		engine.isVisible = true;

		set(mode, "netvsPlayerExist", new boolean[]{true, true, true, true, true, true});
		set(mode, "netvsPlayerDead", new boolean[]{false, false, false, false, false, false});
		set(mode, "netvsIsGameActive", true);
	}

	// ---- calcScore map-game branch (lines 251-252) ----
	@Test
	void calcScoreMapGameUpdatesRemainGems() throws Exception {
		mode.netvsIsPractice = false;
		mode.netCurrentRoomInfo = new NetRoomInfo();
		mode.netCurrentRoomInfo.useMap = true;
		mode.netLobby = new NetLobbyFrame();
		mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();
		set(mode, "playerStartGems", new int[]{4, 0, 0, 0, 0, 0});
		set(mode, "netvsPlayerUID", new int[]{100, -1, -1, -1, -1, -1});

		engine.playerID = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		// Put a couple of gem blocks so remaining gems > 0 (avoids race-win path)
		engine.field.setBlock(0, 19, new Block(Block.BLOCK_COLOR_GEM_RED, 0,
				Block.BLOCK_ATTRIBUTE_VISIBLE));
		engine.field.setBlock(1, 19, new Block(Block.BLOCK_COLOR_GEM_BLUE, 0,
				Block.BLOCK_ATTRIBUTE_VISIBLE));

		mode.calcScore(engine, 0, 1);
		// branch executed; remaining lines recomputed from gems
	}

	/**
	 * Give player 0 more remaining lines than {@code numAhead} of the other
	 * five players (and equal-or-fewer than the rest) so
	 * getNowPlayerPlace(engine0) == numAhead.
	 */
	private void setPlaceTo(int numAhead) throws Exception {
		int[] remain = new int[6];
		remain[0] = 50;
		for (int i = 1; i < 6; i++) {
			remain[i] = (i <= numAhead) ? 10 : 90;
		}
		set(mode, "playerRemainLines", remain);
	}

	// ---- renderLast menu-font place branches (lines 337-349) ----
	@Test
	void renderLastPlaceMenuLayoutAllRanks() throws Exception {
		engine.displaysize = 0;
		for (int place = 0; place <= 5; place++) {
			setPlaceTo(place);
			mode.renderLast(engine, 0);
		}
	}

	// ---- renderLast small/direct-font place branches (lines 350-363) ----
	@Test
	void renderLastPlaceDirectLayoutAllRanks() throws Exception {
		engine.displaysize = -1;
		for (int place = 0; place <= 5; place++) {
			setPlaceTo(place);
			mode.renderLast(engine, 0);
		}
	}

	// ---- dead player path uses netvsPlayerPlace (line 334) for high ranks ----
	@Test
	void renderLastDeadPlayerUsesStoredPlace() throws Exception {
		engine.displaysize = 0;
		set(mode, "netvsPlayerDead", new boolean[]{true, false, false, false, false, false});
		set(mode, "playerRemainLines", new int[]{50, 0, 0, 0, 0, 0});
		for (int place = 0; place <= 5; place++) {
			set(mode, "netvsPlayerPlace", new int[]{place, 0, 0, 0, 0, 0});
			mode.renderLast(engine, 0);
		}
	}

	// ================================================================
	// Helpers
	// ================================================================

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name + " in " + cls.getName());
	}

	private static void set(Object obj, String name, Object value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.set(obj, value);
	}
}

package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedList;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests for {@link VSBattleMode} targeting the still-uncovered
 * <em>wraparound</em> branches of {@code onSetting} (the {@code if(x < min) x =
 * max;} / {@code if(x > max) x = min;} arms that earlier tests never reached
 * because they only nudged a value once from a mid-range start), plus several
 * logic branches in {@code calcScore} (B2B non-triple arm, the
 * {@code (hole == -1) || (version <= 4)} false-false arm, the
 * {@code random < garbagePercent} FALSE arms, and the {@code newHole < hole}
 * no-increment arms) and the replay-mode win/lose arms of {@code onLast}.
 *
 * <p>Each wraparound test sets the field to the relevant boundary value first
 * and then injects a single LEFT (underflow) / RIGHT (overflow) press so the
 * out-of-range guard fires and we can assert the wrapped value.</p>
 */
class VSBattleModeBranchCoverageTest3 {

	// =====================================================================
	// Settings-menu wraparound (assertion-backed)
	// =====================================================================

	@Test
	void gravityWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		engine.speed.gravity = -1;
		change(mode, engine, 0, Controller.BUTTON_LEFT);   // -1 - 1 = -2 < -1 -> 99999
		assertEquals(99999, engine.speed.gravity);

		engine.speed.gravity = 99999;
		change(mode, engine, 0, Controller.BUTTON_RIGHT);  // 99999 + 1 > 99999 -> -1
		assertEquals(-1, engine.speed.gravity);
	}

	@Test
	void denominatorWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		engine.speed.denominator = -1;
		change(mode, engine, 1, Controller.BUTTON_LEFT);
		assertEquals(99999, engine.speed.denominator);

		engine.speed.denominator = 99999;
		change(mode, engine, 1, Controller.BUTTON_RIGHT);
		assertEquals(-1, engine.speed.denominator);
	}

	@Test
	void areWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		engine.speed.are = 0;
		change(mode, engine, 2, Controller.BUTTON_LEFT);
		assertEquals(99, engine.speed.are);

		engine.speed.are = 99;
		change(mode, engine, 2, Controller.BUTTON_RIGHT);
		assertEquals(0, engine.speed.are);
	}

	@Test
	void areLineWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		engine.speed.areLine = 0;
		change(mode, engine, 3, Controller.BUTTON_LEFT);
		assertEquals(99, engine.speed.areLine);

		engine.speed.areLine = 99;
		change(mode, engine, 3, Controller.BUTTON_RIGHT);
		assertEquals(0, engine.speed.areLine);
	}

	@Test
	void lineDelayWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		engine.speed.lineDelay = 0;
		change(mode, engine, 4, Controller.BUTTON_LEFT);
		assertEquals(99, engine.speed.lineDelay);

		engine.speed.lineDelay = 99;
		change(mode, engine, 4, Controller.BUTTON_RIGHT);
		assertEquals(0, engine.speed.lineDelay);
	}

	@Test
	void lockDelayWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		engine.speed.lockDelay = 0;
		change(mode, engine, 5, Controller.BUTTON_LEFT);
		assertEquals(99, engine.speed.lockDelay);

		engine.speed.lockDelay = 99;
		change(mode, engine, 5, Controller.BUTTON_RIGHT);
		assertEquals(0, engine.speed.lockDelay);
	}

	@Test
	void dasWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		engine.speed.das = 0;
		change(mode, engine, 6, Controller.BUTTON_LEFT);
		assertEquals(99, engine.speed.das);

		engine.speed.das = 99;
		change(mode, engine, 6, Controller.BUTTON_RIGHT);
		assertEquals(0, engine.speed.das);
	}

	@Test
	void presetNumberWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		// cursor 7 and 8 share the presetNumber adjust block (case 7: case 8:)
		setIntArray(mode, "presetNumber", 0, 0);
		change(mode, engine, 7, Controller.BUTTON_LEFT);
		assertEquals(99, intArray(mode, "presetNumber")[0]);

		setIntArray(mode, "presetNumber", 0, 99);
		change(mode, engine, 8, Controller.BUTTON_RIGHT);
		assertEquals(0, intArray(mode, "presetNumber")[0]);
	}

	@Test
	void tspinEnableTypeWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		setIntArray(mode, "tspinEnableType", 0, 0);
		change(mode, engine, 13, Controller.BUTTON_LEFT);   // < 0 -> 2
		assertEquals(2, intArray(mode, "tspinEnableType")[0]);

		setIntArray(mode, "tspinEnableType", 0, 2);
		change(mode, engine, 13, Controller.BUTTON_RIGHT);  // > 2 -> 0
		assertEquals(0, intArray(mode, "tspinEnableType")[0]);
	}

	@Test
	void spinCheckTypeWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		setIntArray(mode, "spinCheckType", 0, 0);
		change(mode, engine, 15, Controller.BUTTON_LEFT);   // < 0 -> 1
		assertEquals(1, intArray(mode, "spinCheckType")[0]);

		setIntArray(mode, "spinCheckType", 0, 1);
		change(mode, engine, 15, Controller.BUTTON_RIGHT);  // > 1 -> 0
		assertEquals(0, intArray(mode, "spinCheckType")[0]);
	}

	@Test
	void b2bTypeWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		setIntArray(mode, "b2bType", 0, 0);
		change(mode, engine, 17, Controller.BUTTON_LEFT);   // < 0 -> 2
		assertEquals(2, intArray(mode, "b2bType")[0]);

		setIntArray(mode, "b2bType", 0, 2);
		change(mode, engine, 17, Controller.BUTTON_RIGHT);  // > 2 -> 0
		assertEquals(0, intArray(mode, "b2bType")[0]);
	}

	@Test
	void hurryupSecondsWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		setIntArray(mode, "hurryupSeconds", 0, -1);
		change(mode, engine, 21, Controller.BUTTON_LEFT);   // -2 < -1 -> 300
		assertEquals(300, intArray(mode, "hurryupSeconds")[0]);

		setIntArray(mode, "hurryupSeconds", 0, 300);
		change(mode, engine, 21, Controller.BUTTON_RIGHT);  // 301 > 300 -> -1
		assertEquals(-1, intArray(mode, "hurryupSeconds")[0]);
	}

	@Test
	void hurryupIntervalWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		setIntArray(mode, "hurryupInterval", 0, 1);
		change(mode, engine, 22, Controller.BUTTON_LEFT);   // 0 < 1 -> 99
		assertEquals(99, intArray(mode, "hurryupInterval")[0]);

		setIntArray(mode, "hurryupInterval", 0, 99);
		change(mode, engine, 22, Controller.BUTTON_RIGHT);  // 100 > 99 -> 1
		assertEquals(1, intArray(mode, "hurryupInterval")[0]);
	}

	@Test
	void bgmnoWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "bgmno", 0);
		change(mode, engine, 23, Controller.BUTTON_LEFT);   // -1 < 0 -> BGM_COUNT-1
		assertEquals(BGMStatus.BGM_COUNT - 1, readInt(mode, "bgmno"));

		setInt(mode, "bgmno", BGMStatus.BGM_COUNT - 1);
		change(mode, engine, 23, Controller.BUTTON_RIGHT);  // > BGM_COUNT-1 -> 0
		assertEquals(0, readInt(mode, "bgmno"));
	}

	@Test
	void mapSetWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);

		// useMap false so the inner reload block (582-585) is skipped; only the
		// numeric wrap (580-581) is exercised here.
		boolArray(mode, "useMap")[0] = false;

		setIntArray(mode, "mapSet", 0, 0);
		change(mode, engine, 26, Controller.BUTTON_LEFT);   // < 0 -> 99
		assertEquals(99, intArray(mode, "mapSet")[0]);

		setIntArray(mode, "mapSet", 0, 99);
		change(mode, engine, 26, Controller.BUTTON_RIGHT);  // > 99 -> 0
		assertEquals(0, intArray(mode, "mapSet")[0]);
	}

	@Test
	void mapNumberWrapsAtBothBounds() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		// cursor 27 only adjusts mapNumber when useMap is true; give a known
		// mapMaxNo so the bounds are deterministic (mapMaxNo-1 == 2).
		boolArray(mode, "useMap")[0] = true;
		intArray(mode, "mapMaxNo")[0] = 3;

		// Underflow: -1 - 1 = -2 < -1 -> mapMaxNo-1 = 2
		setIntArray(mode, "mapNumber", 0, -1);
		change(mode, engine, 27, Controller.BUTTON_LEFT);
		assertEquals(2, intArray(mode, "mapNumber")[0]);

		// Overflow: 2 + 1 = 3 > mapMaxNo-1 (2) -> -1
		intArray(mode, "mapMaxNo")[0] = 3;
		setIntArray(mode, "mapNumber", 0, 2);
		change(mode, engine, 27, Controller.BUTTON_RIGHT);
		assertEquals(-1, intArray(mode, "mapNumber")[0]);
	}

	// =====================================================================
	// calcScore logic branches
	// =====================================================================

	/**
	 * B2B with a NON-triple line clear and {@code pts > 0}: drives the
	 * {@code else ptsB2B += 1} arm at line 993 (the {@code if} arm needing a
	 * T-Spin triple is covered elsewhere). With {@code b2bType == 1} the B2B
	 * bonus is folded into {@code pts}, so a double (base 1) + B2B (1) yields
	 * exactly 2 attack lines (field intentionally non-empty -> no all-clear).
	 */
	@Test
	void calcScoreB2BNonTripleAddsOneBonus() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode);
		GameEngine engine = manager.engine[0];
		setInt(mode, "version", 5);
		intArray(mode, "b2bType")[0] = 1;

		fillBottomRow(engine);                 // non-empty -> no all-clear bonus
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.tspin = false;
		engine.b2b = true;

		mode.calcScore(engine, 0, 2);          // double: base pts=1, +B2B 1 = 2

		assertEquals(2, intArray(mode, "garbageSent")[0],
				"double (1) + non-separated B2B bonus (1) = 2 attack lines");
		assertTrue(boolArray(mode, "lastb2b")[0], "lastb2b must be set true");
	}

	/**
	 * Rising garbage with {@code lastHole != -1} and {@code version >= 5}:
	 * makes {@code (hole == -1) || (version <= 4)} evaluate false-false so the
	 * re-randomize-hole line (1071) is skipped and {@code lastHole} is reused.
	 * Uses GARBAGE_TYPE_NORMAL with {@code garbagePercent == 0} so the hole is
	 * never moved and we can assert it equals the seeded lastHole.
	 */
	@Test
	void calcScoreRisingReusesLastHoleWhenSetAndVersion5() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode);
		GameEngine engine = manager.engine[0];
		setInt(mode, "version", 5);

		intArray(mode, "garbageType")[0] = 0;     // NORMAL
		intArray(mode, "garbagePercent")[0] = 0;  // never move the hole
		intArray(mode, "lastHole")[0] = 4;        // != -1 -> reused
		addGarbage(mode, 0, 3, 1);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 0);             // lines==0 -> rising path

		assertEquals(4, intArray(mode, "lastHole")[0],
				"hole reused (not re-randomized) and never moved at 0% rate");
	}

	/**
	 * GARBAGE_TYPE_NOCHANGE_ONE_RISE (type 1), version 5, {@code garbagePercent
	 * == 100} so the hole-move branch (1094) is taken, but with {@code lastHole}
	 * set high (9) so {@code newHole (0..8) >= hole (9)} is always false and the
	 * {@code newHole++} arm at line 1097 is NOT taken.
	 */
	@Test
	void calcScoreOneRiseNewHoleBelowHoleNoIncrement() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode);
		GameEngine engine = manager.engine[0];
		setInt(mode, "version", 5);

		intArray(mode, "garbageType")[0] = 1;     // ONE RISE
		intArray(mode, "garbagePercent")[0] = 100; // always enter hole-move branch
		intArray(mode, "lastHole")[0] = 9;         // newHole in 0..8 < 9 -> no ++
		addGarbage(mode, 0, 2, 1);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 0);

		// ONE RISE breaks out before writing lastHole, but the rising path runs:
		// the entry is delivered as a single block and the queue is cleared.
		LinkedList<?> q = ((LinkedList<?>[]) readObj(mode, "garbageEntries"))[0];
		assertTrue(q.isEmpty(),
				"ONE RISE delivers all lines at once and clears the queue");
	}

	/**
	 * GARBAGE_TYPE_NOCHANGE_ONE_ATTACK (type 2), version 5, {@code garbagePercent
	 * == 0} so the random-percent gate at line 1111 is FALSE and the hole is
	 * never moved: {@code lastHole} stays at its seeded value.
	 */
	@Test
	void calcScoreOneAttackPercentZeroKeepsHole() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode);
		GameEngine engine = manager.engine[0];
		setInt(mode, "version", 5);

		intArray(mode, "garbageType")[0] = 2;     // 1-ATTACK
		intArray(mode, "garbagePercent")[0] = 0;  // gate false -> never move
		intArray(mode, "lastHole")[0] = 6;
		addGarbage(mode, 0, 2, 1);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 0);

		assertEquals(6, intArray(mode, "lastHole")[0],
				"hole unchanged because percent gate was false");
	}

	/**
	 * GARBAGE_TYPE_NORMAL, version 5, {@code garbagePercent == 0}: inside the
	 * per-line loop the percent gate at line 1080 is FALSE so the hole is never
	 * re-randomized. Confirms the rising path clears the queue.
	 */
	@Test
	void calcScoreNormalPercentZeroNeverMovesHole() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode);
		GameEngine engine = manager.engine[0];
		setInt(mode, "version", 5);

		intArray(mode, "garbageType")[0] = 0;     // NORMAL
		intArray(mode, "garbagePercent")[0] = 0;  // gate false in loop
		intArray(mode, "lastHole")[0] = 3;
		addGarbage(mode, 0, 4, 1);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 0);

		assertEquals(3, intArray(mode, "lastHole")[0],
				"hole never moved (0% rate) across all rising lines");
		LinkedList<?> q = ((LinkedList<?>[]) readObj(mode, "garbageEntries"))[0];
		assertTrue(q.isEmpty(), "rising path drains the garbage queue");
	}

	// =====================================================================
	// onLast win/lose in REPLAY mode (winCount NOT incremented)
	// =====================================================================

	/**
	 * 1P-win settlement in replay mode: covers the {@code if(!owner.replayMode)
	 * winCount[0]++;} guard's FALSE arm (line 1196). winCount must stay 0.
	 */
	@Test
	void onLastPlayer1WinsReplayModeDoesNotCountWin() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode);
		manager.engine[0].owner.replayMode = true;
		manager.engine[1].owner.replayMode = true;

		manager.engine[0].gameActive = true;
		manager.engine[0].stat = GameEngine.Status.MOVE;       // 1P alive
		manager.engine[1].stat = GameEngine.Status.GAMEOVER;   // 2P dead

		mode.onLast(manager.engine[1], 1);

		assertEquals(0, readInt(mode, "winnerID"));
		assertEquals(0, intArray(mode, "winCount")[0],
				"replay mode must not increment winCount");
	}

	/**
	 * 2P-win settlement in replay mode: covers the {@code if(!owner.replayMode)
	 * winCount[1]++;} guard's FALSE arm (line 1206). winCount must stay 0.
	 */
	@Test
	void onLastPlayer2WinsReplayModeDoesNotCountWin() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode);
		manager.engine[0].owner.replayMode = true;
		manager.engine[1].owner.replayMode = true;

		manager.engine[0].gameActive = true;
		manager.engine[0].stat = GameEngine.Status.GAMEOVER;   // 1P dead
		manager.engine[1].stat = GameEngine.Status.MOVE;       // 2P alive

		mode.onLast(manager.engine[1], 1);

		assertEquals(1, readInt(mode, "winnerID"));
		assertEquals(0, intArray(mode, "winCount")[1],
				"replay mode must not increment winCount");
	}

	// =====================================================================
	// renderLast threshold branches (render-only, lower confidence)
	// =====================================================================

	/**
	 * Garbage-count colour thresholds in renderLast: garbage == 2 keeps the
	 * colour at YELLOW (only the {@code >= 1} arm taken, {@code >= 3}/{@code >=
	 * 4} not). Render-only; no observable state to assert beyond no-throw.
	 */
	@Test
	void renderLastGarbageYellowThresholdOnly() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode);
		GameEngine engine = manager.engine[0];
		setBool(mode, "showStats", true);
		intArray(mode, "garbage")[0] = 2;   // >=1 true, >=3/>=4 false

		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	// =====================================================================
	// Setup + reflection helpers
	// =====================================================================

	/** Single-engine helper for menu-only tests (mirrors the wraparound template). */
	private static GameEngine freshEngine(VSBattleMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		mode.playerInit(manager.engine[0], 0);
		return manager.engine[0];
	}

	/** Two-engine manager with playerInit run for both players. */
	private static GameManager initedManager(VSBattleMode mode) throws Exception {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		for (int i = 0; i < manager.engine.length; i++) {
			manager.engine[i].init();
			manager.engine[i].playerID = i;
			manager.engine[i].owner.replayMode = false;
		}
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		return manager;
	}

	/** Set menuCursor, reset controller, press one button, run onSetting for player 0. */
	private static void change(VSBattleMode mode, GameEngine engine, int cursor, int dirButton) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		// reset() clears both buttonPress and buttonTime; a prior LEFT must not
		// leave a stale buttonTime that would override a later RIGHT.
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dirButton] = true;
		engine.ctrl.buttonTime[dirButton] = 1;
		mode.onSetting(engine, 0);
	}

	private static void fillBottomRow(GameEngine engine) {
		engine.createFieldIfNeeded();
		for (int x = 0; x < engine.field.getWidth(); x++) {
			engine.field.setBlock(x, engine.field.getHeight() - 1,
					new Block(Block.BLOCK_COLOR_GRAY));
		}
	}

	@SuppressWarnings("unchecked")
	private static void addGarbage(VSBattleMode mode, int playerID, int lines, int srcPlayer) throws Exception {
		LinkedList<Object>[] entries = (LinkedList<Object>[]) readObj(mode, "garbageEntries");
		Class<?> entryCls = Class.forName("nullpomino.game.mode.VSBattleMode$GarbageEntry");
		Constructor<?> ctor = entryCls.getDeclaredConstructor(VSBattleMode.class, int.class, int.class);
		ctor.setAccessible(true);
		entries[playerID].add(ctor.newInstance(mode, lines, srcPlayer));
	}

	private static int[] intArray(Object obj, String name) throws Exception {
		return (int[]) readObj(obj, name);
	}

	private static boolean[] boolArray(Object obj, String name) throws Exception {
		return (boolean[]) readObj(obj, name);
	}

	private static void setIntArray(Object obj, String name, int index, int value) throws Exception {
		((int[]) readObj(obj, name))[index] = value;
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static Object readObj(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).get(obj);
	}

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}

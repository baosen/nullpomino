package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link PracticeMode}: the registry surface,
 * playerInit defaults including the per-piece-enable bookkeeping unique
 * to this mode, and the loadPreset / savePreset round-trip with all
 * 25+ practice options. Settings UI, render, and gameplay loops stay
 * out of scope (they need an SDL renderer).
 */
class PracticeModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("PRACTICE", new PracticeMode().getName());
	}

	@Test
	void playerInitInstallsFreshSettingsAndAllocatesPieceEnableArray() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertEquals(0, readInt(mode, "goal"));
		assertEquals(0, readInt(mode, "lastgoal"));
		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "lastcombo"));
		assertEquals(0, readInt(mode, "lastpiece"));
		assertEquals(false, readBoolean(mode, "lastb2b"));
		assertEquals(100, readInt(mode, "nextseclv"),
				"nextseclv defaults to 100 so the level-up animation fires once at level 100");
		assertEquals(false, readBoolean(mode, "lvupflag"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(0, readInt(mode, "harddropBonus"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(false, readBoolean(mode, "rollstarted"));
		assertEquals(0, readInt(mode, "secretGrade"));
		assertEquals(0, readInt(mode, "timelimitTimer"));

		// fldBackup must be cleared so the first reset takes the field
		// snapshot rather than restoring stale state.
		assertEquals(null, readField(mode, "fldBackup"));

		// pieceEnable array allocated at PIECE_COUNT length.
		boolean[] pieceEnable = (boolean[]) readField(mode, "pieceEnable");
		assertNotNull(pieceEnable);
		assertEquals(Piece.PIECE_COUNT, pieceEnable.length);

		assertEquals(GameEngine.FRAME_COLOR_YELLOW, engine.framecolor);
	}

	@Test
	void loadPresetReadsEverySpeedAndSpinFieldAndPiecePerPieceEnableSlot() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("practice.gravity.7", 64);
		prop.setProperty("practice.denominator.7", 256);
		prop.setProperty("practice.are.7", 30);
		prop.setProperty("practice.areLine.7", 25);
		prop.setProperty("practice.lineDelay.7", 40);
		prop.setProperty("practice.lockDelay.7", 30);
		prop.setProperty("practice.das.7", 14);
		prop.setProperty("practice.bgmno.7", 4);
		prop.setProperty("practice.tspinEnableType.7", 2);
		prop.setProperty("practice.enableTSpin.7", false);
		prop.setProperty("practice.tspinEnableEZ.7", true);
		prop.setProperty("practice.enableB2B.7", false);
		prop.setProperty("practice.comboType.7", 1);
		prop.setProperty("practice.big.7", true);
		prop.setProperty("practice.leveltype.7", 2);
		prop.setProperty("practice.lvstopse.7", false);
		prop.setProperty("practice.goallv.7", 200);
		prop.setProperty("practice.timelimit.7", 5400);
		prop.setProperty("practice.rolltimelimit.7", 600);
		// Disable the I piece, enable I3 (which is non-standard).
		prop.setProperty("practice.pieceEnable." + Piece.PIECE_I + ".7", false);
		prop.setProperty("practice.pieceEnable." + Piece.PIECE_I3 + ".7", true);
		prop.setProperty("practice.useMap.7", true);
		prop.setProperty("practice.timelimitResetEveryLevel.7", true);
		prop.setProperty("practice.bone.7", true);
		prop.setProperty("practice.blockHidden.7", 600);
		prop.setProperty("practice.blockHiddenAnim.7", false);
		prop.setProperty("practice.blockShowOutlineOnly.7", true);
		prop.setProperty("practice.heboHiddenLevel.7", 3);

		invokeLoadPreset(mode, engine, prop, 7);

		assertEquals(64, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(40, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(2, readInt(mode, "tspinEnableType"));
		assertEquals(false, readBoolean(mode, "enableTSpin"));
		assertEquals(true, readBoolean(mode, "tspinEnableEZ"));
		assertEquals(false, readBoolean(mode, "enableB2B"));
		assertEquals(1, readInt(mode, "comboType"));
		assertEquals(true, readBoolean(mode, "big"));
		assertEquals(2, readInt(mode, "leveltype"));
		assertEquals(false, readBoolean(mode, "lvstopse"));
		assertEquals(200, readInt(mode, "goallv"));
		assertEquals(5400, readInt(mode, "timelimit"));
		assertEquals(600, readInt(mode, "rolltimelimit"));
		assertEquals(true, readBoolean(mode, "useMap"));
		assertEquals(true, readBoolean(mode, "timelimitResetEveryLevel"));
		assertEquals(true, readBoolean(mode, "bone"));
		assertEquals(600, readInt(mode, "blockHidden"));
		assertEquals(false, readBoolean(mode, "blockHiddenAnim"));
		assertEquals(true, readBoolean(mode, "blockShowOutlineOnly"));
		assertEquals(3, readInt(mode, "heboHiddenLevel"));

		boolean[] pieceEnable = (boolean[]) readField(mode, "pieceEnable");
		assertEquals(false, pieceEnable[Piece.PIECE_I]);
		assertEquals(true, pieceEnable[Piece.PIECE_I3]);
	}

	@Test
	void loadPresetAppliesPieceStandardCountFallbackWhenPieceEnableKeyAbsent() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadPreset(mode, engine, new CustomProperties(), 99);

		boolean[] pieceEnable = (boolean[]) readField(mode, "pieceEnable");
		// Default: enabled for the seven standard pieces (i < PIECE_STANDARD_COUNT),
		// disabled for everything past it.
		for(int i = 0; i < Piece.PIECE_STANDARD_COUNT; i++) {
			assertTrue(pieceEnable[i],
					"piece " + i + " (standard) must default to enabled");
		}
		for(int i = Piece.PIECE_STANDARD_COUNT; i < Piece.PIECE_COUNT; i++) {
			assertEquals(false, pieceEnable[i],
					"piece " + i + " (non-standard) must default to disabled");
		}
	}

	@Test
	void loadPresetAppliesDocumentedDefaultsForMissingNumericAndBooleanKeys() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadPreset(mode, engine, new CustomProperties(), 99);

		assertEquals(4, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(0, engine.speed.are);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
		assertEquals(0, readInt(mode, "bgmno"));
		assertEquals(1, readInt(mode, "tspinEnableType"));
		assertEquals(true, readBoolean(mode, "enableTSpin"));
		assertEquals(false, readBoolean(mode, "tspinEnableEZ"));
		assertEquals(true, readBoolean(mode, "enableB2B"));
		assertEquals(GameEngine.COMBO_TYPE_NORMAL, readInt(mode, "comboType"));
		assertEquals(false, readBoolean(mode, "big"));
		assertEquals(0, readInt(mode, "leveltype"),
				"leveltype defaults to LEVELTYPE_NONE = 0");
		assertEquals(true, readBoolean(mode, "lvstopse"));
		assertEquals(-1, readInt(mode, "goallv"));
		assertEquals(0, readInt(mode, "timelimit"));
		assertEquals(0, readInt(mode, "rolltimelimit"));
		assertEquals(false, readBoolean(mode, "useMap"));
		assertEquals(false, readBoolean(mode, "bone"));
		assertEquals(-1, readInt(mode, "blockHidden"));
		assertEquals(true, readBoolean(mode, "blockHiddenAnim"));
		assertEquals(GameEngine.BLOCK_OUTLINE_NORMAL, readInt(mode, "blockOutlineType"));
		assertEquals(false, readBoolean(mode, "blockShowOutlineOnly"));
		assertEquals(0, readInt(mode, "heboHiddenLevel"));
	}

	@Test
	void savePresetAndLoadPresetRoundTripUnderPracticePrefix() throws Exception {
		PracticeMode source = new PracticeMode();
		GameEngine sourceEngine = freshEngine(source);
		source.playerInit(sourceEngine, 0);
		sourceEngine.speed.gravity = 99;
		sourceEngine.speed.lockDelay = 25;
		setInt(source, "bgmno", 7);
		setInt(source, "tspinEnableType", 2);
		setBoolean(source, "enableTSpin", false);
		setBoolean(source, "big", true);
		setInt(source, "leveltype", 1);
		setInt(source, "goallv", 99);
		setInt(source, "timelimit", 3600);
		boolean[] pieceEnable = (boolean[]) readField(source, "pieceEnable");
		pieceEnable[Piece.PIECE_O] = false;
		pieceEnable[Piece.PIECE_T] = false;

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(source, sourceEngine, prop, 1);

		assertEquals(99, prop.getProperty("practice.gravity.1", -1));
		assertEquals(true, prop.getProperty("practice.big.1", false));
		assertEquals(1, prop.getProperty("practice.leveltype.1", -1));
		assertEquals(99, prop.getProperty("practice.goallv.1", -1));
		assertEquals(false,
				prop.getProperty("practice.pieceEnable." + Piece.PIECE_O + ".1", true));
		assertEquals(false,
				prop.getProperty("practice.pieceEnable." + Piece.PIECE_T + ".1", true));

		PracticeMode dest = new PracticeMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadPreset(dest, destEngine, prop, 1);

		assertEquals(99, destEngine.speed.gravity);
		assertEquals(25, destEngine.speed.lockDelay);
		assertEquals(2, readInt(dest, "tspinEnableType"));
		assertEquals(false, readBoolean(dest, "enableTSpin"));
		assertEquals(true, readBoolean(dest, "big"));
		assertEquals(99, readInt(dest, "goallv"));
		assertEquals(3600, readInt(dest, "timelimit"));

		boolean[] destEnable = (boolean[]) readField(dest, "pieceEnable");
		assertEquals(false, destEnable[Piece.PIECE_O]);
		assertEquals(false, destEnable[Piece.PIECE_T]);
		assertEquals(true, destEnable[Piece.PIECE_I]);
	}

	private static GameEngine freshEngine(PracticeMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(PracticeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(PracticeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(PracticeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(PracticeMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(PracticeMode mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
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

	private static void invokeLoadPreset(PracticeMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = PracticeMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(PracticeMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = PracticeMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}
}

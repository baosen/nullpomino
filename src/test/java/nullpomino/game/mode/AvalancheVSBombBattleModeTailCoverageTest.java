package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Tail coverage for {@link AvalancheVSBombBattleMode}: onSetting map sub-branches
 * (cases 26/27/28 when useMap is on), random map preview, the "WAIT" B-cancel
 * branch, renderLast hard/countdown block overlays, lineClearEnd's bomb-scan
 * null-block continue, and saveReplay's map-save path.
 */
class AvalancheVSBombBattleModeTailCoverageTest {

	// ---- case 26: useMap true -> false resets field (line 242) ----
	@Test
	void onSettingCursor26UseMapOffResetsField() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 26);
		engine.createFieldIfNeeded();
		setBoolArray(mode, "useMap", true, 0);

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertFalse(getBoolArray(mode, "useMap")[0]);
	}

	// ---- case 27: mapSet with useMap on (lines 252-253) ----
	@Test
	void onSettingCursor27MapSetWithUseMap() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 27);
		engine.createFieldIfNeeded();
		setBoolArray(mode, "useMap", true, 0);

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		// mapNumber reset to -1 and preview loaded; no throw
	}

	// ---- case 28: mapNumber with useMap on (lines 258-261) ----
	@Test
	void onSettingCursor28MapNumberWithUseMap() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 28);
		engine.createFieldIfNeeded();
		setBoolArray(mode, "useMap", true, 0);
		setIntArray(mode, "mapNumber", 0, 0);
		setIntArray(mode, "mapMaxNo", 5, 0);

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		// mapNumber adjusted with clamp + preview loaded
	}

	// ---- random map preview branch (lines 315-318) ----
	@Test
	void onSettingRandomMapPreview() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 0);
		engine.createFieldIfNeeded();
		setBoolArray(mode, "useMap", true, 0);
		setIntArray(mode, "mapNumber", -1, 0);
		setIntArray(mode, "mapMaxNo", 3, 0);
		// propMap[0] non-null so the random-preview block executes
		setObjArray(mode, "propMap", new CustomProperties(), 0);
		// menuTime such that menuTime != 0 (skip line 309) but menuTime % 30 == 0
		setFieldInt(mode, "menuTime", 30);
		engine.statc[5] = 2;

		mode.onSetting(engine, 0);
		// statc[5] incremented and wrapped; loadMapPreview(false) executed
	}

	// ---- "start" branch B-cancel (lines 344-345) ----
	@Test
	void onSettingStartScreenBCancel() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = false;
		engine.statc[4] = 1; // past menu, in the WAIT/start branch
		// Not both ready, so falls into the B-cancel else-if
		mode.owner.engine[0].statc[4] = 1;
		mode.owner.engine[1].statc[4] = 0;

		pressKey(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		// statc[4] reset to 0
		org.junit.jupiter.api.Assertions.assertEquals(0, engine.statc[4]);
	}

	// ---- renderLast hard/countdown block overlays (lines 489, 494, 500) ----
	@Test
	void renderLastDrawsHardAndCountdownBlocks() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode);
		engine.displaysize = 1;
		engine.gameStarted = true;
		engine.stat = GameEngine.Status.MOVE;
		engine.createFieldIfNeeded();

		// A hard block (line 494) and a countdown block (line 500); other cells
		// left null to exercise the b == null continue (line 489).
		Block hard = new Block(Block.BLOCK_COLOR_GRAY, engine.getSkin(),
				Block.BLOCK_ATTRIBUTE_VISIBLE);
		hard.hard = 3;
		engine.field.setBlock(1, engine.field.getHeight() - 1, hard);

		Block bomb = new Block(Block.BLOCK_COLOR_RED, engine.getSkin(),
				Block.BLOCK_ATTRIBUTE_VISIBLE);
		bomb.countdown = 4;
		engine.field.setBlock(2, engine.field.getHeight() - 1, bomb);

		mode.renderLast(engine, 0);
	}

	// ---- lineClearEnd bomb-scan null-block continue (line 542) ----
	@Test
	void lineClearEndScansBlocksWithNulls() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		// ojama < 6 so the garbage-drop branch is skipped and we reach the
		// per-block bomb scan, where most cells are null (line 542 continue).
		setIntArray(mode, "ojama", 0, 0);
		setIntArray(mode, "ojamaAdd", 0, 0);
		setIntArray(mode, "ojamaAdd", 0, 1);

		// One real block with countdown 2 so the countdown-- branch runs too.
		Block bomb = new Block(Block.BLOCK_COLOR_RED, engine.getSkin(),
				Block.BLOCK_ATTRIBUTE_VISIBLE);
		bomb.countdown = 2;
		engine.field.setBlock(0, engine.field.getHeight() - 1, bomb);

		mode.lineClearEnd(engine, 0);
	}

	// ---- saveReplay map-save path (line 623) ----
	@Test
	void saveReplaySavesMapWhenUseMap() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolArray(mode, "useMap", true, 0);

		// fldBackup[0] non-null triggers saveMap (line 623)
		nullpomino.game.component.Field fld = new nullpomino.game.component.Field(
				10, 20, 4);
		setObjArray(mode, "fldBackup", fld, 0);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		org.junit.jupiter.api.Assertions.assertEquals(0,
				prop.getProperty("avalanchevs.version", -1));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(AvalancheVSBombBattleMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.modeInit(manager);
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine,
			AvalancheVSBombBattleMode mode, int cursor) throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", cursor);
		setFieldInt(mode, "menuTime", 10);
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static boolean[] getBoolArray(Object obj, String name) throws Exception {
		return (boolean[]) findField(obj.getClass(), name).get(obj);
	}

	private static void setIntArray(Object obj, String name, int value, int index) throws Exception {
		((int[]) findField(obj.getClass(), name).get(obj))[index] = value;
	}

	private static void setBoolArray(Object obj, String name, boolean value, int index) throws Exception {
		((boolean[]) findField(obj.getClass(), name).get(obj))[index] = value;
	}

	private static void setObjArray(Object obj, String name, Object value, int index) throws Exception {
		((Object[]) findField(obj.getClass(), name).get(obj))[index] = value;
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
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

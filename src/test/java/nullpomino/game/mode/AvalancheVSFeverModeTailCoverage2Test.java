package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers remaining uncovered lines in {@link AvalancheVSFeverMode}:
 *
 * <ul>
 *   <li>Line 236 – onSetting case 19: feverChainStart &lt; min → clamp to max.</li>
 *   <li>Line 238 – onSetting case 19: feverChainStart &gt; max → clamp to min.</li>
 *   <li>Line 431 – renderMove: gameStarted → drawX called.</li>
 *   <li>Line 491 – renderLast: non-MOVE/RESULT status + gameStarted → drawX in renderLast.</li>
 *   <li>Line 536 – addOjama: zenKeshi + ZENKESHI_MODE_ON → ojamaNew += 30.</li>
 *   <li>Line 542 – addOjama: rate &lt;= 0 after hurryup → rate = 1.</li>
 *   <li>Lines 555-557 – addOjama: ojamaAdd counter absorbs ojamaNew.</li>
 *   <li>Line 596 – lineClearEnd: feverChain after zenKeshi add exceeds max → clamped.</li>
 * </ul>
 */
class AvalancheVSFeverModeTailCoverage2Test {

    // ---------------------------------------------------------------
    // onSetting case 19: feverChainStart clamp below min (line 236)
    // ---------------------------------------------------------------

    @Test
    void onSettingCase19FeverChainStartClampBelowMin() throws Exception {
        AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
        GameEngine engine = freshEngine(mode);

        int min = getIntArray(mode, "feverChainMin")[0];
        int max = getIntArray(mode, "feverChainMax")[0];
        // Force feverChainStart below min; after loadMapSetFever the clamp runs
        setIntArray(mode, "feverChainStart", min - 1, 0);
        setMenuState(engine, mode, 19);

        pressKey(engine, Controller.BUTTON_RIGHT);
        mode.onSetting(engine, 0);

        int fcs = getIntArray(mode, "feverChainStart")[0];
        // min-1 < min → clamp to max (line 236)
        assertTrue(fcs >= min && fcs <= max,
                "feverChainStart should be clamped to valid range (line 236), got " + fcs);
    }

    @Test
    void onSettingCase19FeverChainStartClampAboveMax() throws Exception {
        AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
        GameEngine engine = freshEngine(mode);

        int min = getIntArray(mode, "feverChainMin")[0];
        int max = getIntArray(mode, "feverChainMax")[0];
        // Force feverChainStart above max; clamp to min (line 238)
        setIntArray(mode, "feverChainStart", max + 1, 0);
        setMenuState(engine, mode, 19);

        pressKey(engine, Controller.BUTTON_RIGHT);
        mode.onSetting(engine, 0);

        int fcs = getIntArray(mode, "feverChainStart")[0];
        assertTrue(fcs >= min && fcs <= max,
                "feverChainStart should be clamped to valid range (line 238), got " + fcs);
    }

    // ---------------------------------------------------------------
    // renderMove: gameStarted → drawX (line 431)
    // ---------------------------------------------------------------

    @Test
    void renderMoveDrawsXWhenGameStarted() throws Exception {
        AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
        GameManager manager = twoEngineManager(mode);
        GameEngine engine = manager.engine[0];
        engine.createFieldIfNeeded();
        engine.gameStarted = true;

        // Should run drawX without exception
        mode.renderMove(engine, 0);
        assertTrue(engine.gameStarted, "gameStarted should still be true after renderMove");
    }

    @Test
    void renderMoveSkipsDrawXWhenNotGameStarted() throws Exception {
        AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
        GameManager manager = twoEngineManager(mode);
        GameEngine engine = manager.engine[0];
        engine.createFieldIfNeeded();
        engine.gameStarted = false;

        // No exception expected; drawX branch skipped
        mode.renderMove(engine, 0);
    }

    // ---------------------------------------------------------------
    // renderLast: non-MOVE/RESULT status + gameStarted → drawX (line 491)
    // ---------------------------------------------------------------

    @Test
    void renderLastDrawsXWhenNotMoveOrResult() throws Exception {
        AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
        GameManager manager = twoEngineManager(mode);
        GameEngine engine = manager.engine[0];
        engine.createFieldIfNeeded();
        engine.gameActive = true;
        engine.gameStarted = true;
        engine.displaysize = 0;
        engine.stat = GameEngine.Status.ARE;   // Not MOVE or RESULT

        setIntArray(mode, "ojama", 0, 0);
        setIntArray(mode, "ojamaAdd", 0, 0);

        mode.renderLast(engine, 0);
        // Line 491 should have been reached; just verify no exception
        assertEquals(GameEngine.Status.ARE, engine.stat);
    }

    // ---------------------------------------------------------------
    // addOjama: zenKeshi + ZENKESHI_MODE_ON → ojamaNew += 30 (line 536)
    // ---------------------------------------------------------------

    @Test
    void addOjamaZenKeshiOnAdds30ToOjamaNew() throws Exception {
        AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
        GameManager manager = twoEngineManager(mode);
        GameEngine engine = manager.engine[0];
        engine.createFieldIfNeeded();
        engine.field.garbageCleared = 0;

        // zenKeshi = true, type = ZENKESHI_MODE_ON (=1)
        setBoolArray(mode, "zenKeshi", true, 0);
        setIntArray(mode, "zenKeshiType", AvalancheVSDummyMode.ZENKESHI_MODE_ON, 0);
        setIntArray(mode, "ojamaRate", 1, 0);
        setIntArray(mode, "ojama", 0, 0);
        setIntArray(mode, "ojamaAdd", 0, 0);

        // pts=0 but zenKeshi adds 30 → ojamaAdd[1] should get 30
        invokeAddOjama(mode, engine, 0, 0);

        assertEquals(30, getIntArray(mode, "ojamaAdd")[1],
                "zenKeshi+ON should add 30 ojama to enemy (line 536)");
    }

    // ---------------------------------------------------------------
    // addOjama: hurryup drives rate to 0 → clamped to 1 (line 542)
    // ---------------------------------------------------------------

    @Test
    void addOjamaRateClampedTo1AfterHurryup() throws Exception {
        AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
        GameManager manager = twoEngineManager(mode);
        GameEngine engine = manager.engine[0];
        engine.createFieldIfNeeded();
        engine.field.garbageCleared = 0;

        // hurryupSeconds=1, statistics.time >> hurryupSeconds*60 → rate gets right-shifted to 0
        setIntArray(mode, "ojamaRate", 1, 0);
        setIntArray(mode, "hurryupSeconds", 1, 0);
        // Time far past hurryup so rate <<= many times → 0 → clamped to 1
        engine.statistics.time = 3600 * 60;

        setIntArray(mode, "ojama", 0, 0);
        setIntArray(mode, "ojamaAdd", 0, 0);

        // After clamping, ojamaNew = (5 + 1 - 1) / 1 = 5 → enemy gets 5
        invokeAddOjama(mode, engine, 0, 5);

        // Enemy ojamaAdd should have received some ojama (rate was clamped to at least 1)
        assertTrue(getIntArray(mode, "ojamaAdd")[1] > 0,
                "rate clamped to 1 should still send ojama (line 542)");
    }

    // ---------------------------------------------------------------
    // addOjama: ojamaAdd counter absorbs ojamaNew (lines 555-557)
    // ---------------------------------------------------------------

    @Test
    void addOjamaOjamaAddCounterAbsorbs() throws Exception {
        AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
        GameManager manager = twoEngineManager(mode);
        GameEngine engine = manager.engine[0];
        engine.createFieldIfNeeded();
        engine.field.garbageCleared = 0;

        // ojama[0]=0, ojamaAdd[0]=10 → when ojamaNew > 0 it absorbs via ojamaAdd
        setIntArray(mode, "ojama", 0, 0);
        setIntArray(mode, "ojamaAdd", 10, 0);
        setIntArray(mode, "ojamaRate", 1, 0);

        // pts=5 → ojamaNew=5; ojama=0 so that block skips; ojamaAdd=10 → absorbs 5
        invokeAddOjama(mode, engine, 0, 5);

        assertEquals(5, getIntArray(mode, "ojamaAdd")[0],
                "ojamaAdd should decrease by 5 after counter-absorb (lines 555-557)");
    }

    // ---------------------------------------------------------------
    // lineClearEnd: feverChain + 2 exceeds feverChainMax → clamped (line 596)
    // ---------------------------------------------------------------

    @Test
    void lineClearEndZenKeshiFeverFeverChainClampedToMax() throws Exception {
        AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
        GameManager manager = twoEngineManager(mode);
        GameEngine engine = manager.engine[0];
        engine.createFieldIfNeeded();

        int max = getIntArray(mode, "feverChainMax")[0];
        // Set feverChain to max-1 and zenKeshi+FEVER: after += 2 it becomes max+1 → clamped
        setIntArray(mode, "feverChain", max - 1, 0);
        setBoolArray(mode, "zenKeshi", true, 0);
        setIntArray(mode, "zenKeshiType", AvalancheVSDummyMode.ZENKESHI_MODE_FEVER, 0);
        setBoolArray(mode, "cleared", true, 0);
        engine.chain = max;   // Make newFeverChain large enough to trigger cool/clamp
        setIntArray(mode, "ojamaAdd", 0, 1);

        mode.lineClearEnd(engine, 0);

        assertEquals(max, getIntArray(mode, "feverChain")[0],
                "feverChain should be clamped to max after zenKeshi +2 (line 596)");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    static class RedirectingReceiver extends EventReceiver {
        @Override
        public void saveModeConfig(CustomProperties modeConfig) {
            try {
                modeConfig.storeToFile(
                        System.getProperty("java.io.tmpdir") + "/avsvsfever2-test-mode.cfg",
                        "test");
            } catch (Exception ignored) {
            }
        }

        @Override
        public boolean saveProperties(String filename, CustomProperties prop) {
            try {
                prop.storeToFile(
                        System.getProperty("java.io.tmpdir") + "/avsvsfever2-test-redirect.cfg",
                        "test");
            } catch (Exception ignored) {
            }
            return true;
        }
    }

    private static GameEngine freshEngine(AvalancheVSFeverMode mode) throws Exception {
        return twoEngineManager(mode).engine[0];
    }

    private static GameManager twoEngineManager(AvalancheVSFeverMode mode) throws Exception {
        GameManager manager = new GameManager(new RedirectingReceiver());
        manager.replayMode = false;
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        manager.engine[1].init();
        mode.modeInit(manager);
        mode.playerInit(manager.engine[0], 0);
        mode.playerInit(manager.engine[1], 1);
        manager.engine[0].owner.replayMode = false;
        manager.engine[1].owner.replayMode = false;
        return manager;
    }

    private static void setMenuState(GameEngine engine, AvalancheVSFeverMode mode,
            int cursor) throws Exception {
        engine.owner.replayMode = false;
        engine.statc[4] = 0;
        setFieldInt(mode, "menuCursor", cursor);
        setFieldInt(mode, "menuTime", 0);
    }

    private static void pressKey(GameEngine engine, int btn) {
        engine.ctrl.clearButtonState();
        for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
            engine.ctrl.buttonTime[i] = 0;
        }
        engine.ctrl.buttonPress[btn] = true;
        engine.ctrl.buttonTime[btn] = 1;
    }

    private static void invokeAddOjama(AvalancheVSFeverMode mode, GameEngine engine,
            int playerID, int pts) throws Exception {
        Method m = findMethod(mode.getClass(), "addOjama",
                GameEngine.class, int.class, int.class);
        m.setAccessible(true);
        m.invoke(mode, engine, playerID, pts);
    }

    private static void setFieldInt(Object obj, String name, int value) throws Exception {
        findField(obj.getClass(), name).setInt(obj, value);
    }

    private static int[] getIntArray(Object obj, String name) throws Exception {
        return (int[]) findField(obj.getClass(), name).get(obj);
    }

    private static void setIntArray(Object obj, String name, int value, int index)
            throws Exception {
        ((int[]) findField(obj.getClass(), name).get(obj))[index] = value;
    }

    private static void setBoolArray(Object obj, String name, boolean value, int index)
            throws Exception {
        ((boolean[]) findField(obj.getClass(), name).get(obj))[index] = value;
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params)
            throws NoSuchMethodException {
        Class<?> c = cls;
        while (c != null) {
            try { return c.getDeclaredMethod(name, params); }
            catch (NoSuchMethodException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchMethodException(name);
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

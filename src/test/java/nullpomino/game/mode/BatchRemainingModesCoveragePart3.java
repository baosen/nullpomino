package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

class BatchRemainingModesCoveragePart3 {

    @Test void digModePlayerInitReplay() throws Exception {
        DigChallengeMode m = new DigChallengeMode();
        GameEngine e = fe(m);
        e.owner.replayMode = true;
        m.playerInit(e, 0);
    }

    @Test void digModeSetSpeedRealtime() throws Exception {
        DigChallengeMode m = new DigChallengeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "goaltype", 1);
        m.setSpeed(e);
        assertEquals(0, e.speed.gravity);
    }

    @Test void digModeStartGame() throws Exception {
        DigChallengeMode m = new DigChallengeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "version", 1);
        setInt(m, "tspinEnableType", 2);
        m.startGame(e, 0);
        assertTrue(e.tspinEnable);
        assertTrue(e.useAllSpinBonus);
    }

    @Test void digModeCalcScoreTSpin() throws Exception {
        DigChallengeMode m = new DigChallengeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        setInt(m, "version", 2);
        setInt(m, "goaltype", 0);
        setInt(m, "garbagePending", 0);

        e.tspin = true; e.tspinez = true; e.useAllSpinBonus = false;
        e.b2b = true; e.combo = 1;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertTrue(readInt(m, "lastevent") == 10);
    }

    @Test void digModeCalcScoreNonTSpin() throws Exception {
        DigChallengeMode m = new DigChallengeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        setInt(m, "version", 2);
        setInt(m, "goaltype", 0);
        e.tspin = false; e.b2b = false; e.combo = 1;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(1, readInt(m, "lastevent"));
    }

    @Test void digModeOnLastNormal() throws Exception {
        DigChallengeMode m = new DigChallengeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true; e.timerActive = true;
        e.createFieldIfNeeded();
        setInt(m, "goaltype", 0);
        setInt(m, "version", 1);
        setInt(m, "garbageTimer", 200);
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.onLast(e, 0);
    }

    @Test void digModeSaveReplay() throws Exception {
        DigChallengeMode m = new DigChallengeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        CustomProperties prop = new CustomProperties();
        m.saveReplay(e, 0, prop);
    }

    @Test void techModePlayerInitReplay() throws Exception {
        TechnicianMode m = new TechnicianMode();
        GameEngine e = fe(m);
        e.owner.replayMode = true;
        m.playerInit(e, 0);
    }

    @Test void techModeStartGameAll() throws Exception {
        for (int g = 0; g < 5; g++) {
            TechnicianMode m = new TechnicianMode();
            GameEngine e = fe(m);
            m.playerInit(e, 0);
            setInt(m, "goaltype", g);
            setInt(m, "version", 2);
            m.startGame(e, 0);
        }
    }

    @Test void techModeCalcScore() throws Exception {
        TechnicianMode m = new TechnicianMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        m.startGame(e, 0);
        e.createFieldIfNeeded();
        e.tspin = false; e.ending = 0;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertTrue(readInt(m, "lastevent") > 0);
    }

    @Test void techModeOnLast() throws Exception {
        TechnicianMode m = new TechnicianMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true; e.timerActive = true;
        setInt(m, "goaltype", 0);
        m.onLast(e, 0);
    }

    @Test void techModeSaveReplay() throws Exception {
        TechnicianMode m = new TechnicianMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        CustomProperties prop = new CustomProperties();
        m.saveReplay(e, 0, prop);
    }

    @Test void phantomModePlayerInitReplay() throws Exception {
        PhantomManiaMode m = new PhantomManiaMode();
        GameEngine e = fe(m);
        e.owner.replayMode = true;
        m.playerInit(e, 0);
        int[] bs = (int[]) readField(m, "bestSectionTime");
        for (int i = 0; i < bs.length; i++) assertEquals(3600, bs[i]);
    }

    @Test void phantomModeSetSpeed() throws Exception {
        PhantomManiaMode m = new PhantomManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.statistics.level = 0;
        inv(m, "setSpeed", e);
        assertEquals(-1, e.speed.gravity);
    }

    @Test void phantomModeLevelUp() throws Exception {
        PhantomManiaMode m = new PhantomManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.statistics.level = 50;
        inv(m, "levelUp", e);
        assertEquals(GameEngine.METER_COLOR_YELLOW, e.meterColor);
    }

    @Test void phantomModeOnMove() throws Exception {
        PhantomManiaMode m = new PhantomManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 0; e.statc[0] = 0; e.holdDisable = false;
        e.statistics.level = 99; setInt(m, "nextseclv", 200);
        setBoolean(m, "lvupflag", false);
        e.createFieldIfNeeded();
        m.onMove(e, 0);
    }

    @Test void phantomModeOnARE() throws Exception {
        PhantomManiaMode m = new PhantomManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 0; e.statc[0] = 10; e.statc[1] = 10;
        setBoolean(m, "lvupflag", false);
        m.onARE(e, 0);
        assertTrue(readBoolean(m, "lvupflag"));
    }

    @Test void phantomModeOnLast() throws Exception {
        PhantomManiaMode m = new PhantomManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.timerActive = true; e.ending = 0;
        e.statistics.level = 0;
        m.onLast(e, 0);
    }

    @Test void phantomModeCalcScore() throws Exception {
        PhantomManiaMode m = new PhantomManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        e.ending = 0;
        m.calcScore(e, 0, 1);
    }

    @Test void phantomModeSaveReplay() throws Exception {
        PhantomManiaMode m = new PhantomManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        CustomProperties prop = new CustomProperties();
        m.saveReplay(e, 0, prop);
    }

    @Test void avaDigModeInit() throws Exception {
        AvalancheVSDigRaceMode m = new AvalancheVSDigRaceMode();
        GameManager mgr = new GameManager(new EventReceiver());
        m.modeInit(mgr);
    }

    @Test void avaDigModePlayerInitReplay() throws Exception {
        AvalancheVSDigRaceMode m = new AvalancheVSDigRaceMode();
        GameManager mgr = new GameManager(new EventReceiver());
        m.modeInit(mgr);
        mgr.mode = m; mgr.init(); mgr.engine[0].init();
        mgr.engine[0].owner.replayMode = true;
        m.playerInit(mgr.engine[0], 0);
    }

    @Test void avaDigModeOnReady() throws Exception {
        AvalancheVSDigRaceMode m = new AvalancheVSDigRaceMode();
        GameManager mgr = new GameManager(new EventReceiver());
        m.modeInit(mgr);
        mgr.mode = m; mgr.init(); mgr.engine[0].init();
        m.playerInit(mgr.engine[0], 0);
        mgr.engine[0].statc[0] = 0;
        mgr.engine[0].createFieldIfNeeded();
        m.onReady(mgr.engine[0], 0);
    }

    @Test void avaDigModeLineClearEnd() throws Exception {
        AvalancheVSDigRaceMode m = new AvalancheVSDigRaceMode();
        GameManager mgr = new GameManager(new EventReceiver());
        m.modeInit(mgr);
        mgr.mode = m; mgr.init(); mgr.engine[0].init();
        m.playerInit(mgr.engine[0], 0);
        mgr.engine[0].createFieldIfNeeded();
        int[] ojama = (int[]) readField(m, "ojama");
        ojama[0] = 5;
        int[] ojamaAdd = (int[]) readField(m, "ojamaAdd");
        ojamaAdd[0] = 0;
        boolean[] ojamaDrop = (boolean[]) readField(m, "ojamaDrop");
        ojamaDrop[0] = false;
        boolean[] cleared = (boolean[]) readField(m, "cleared");
        cleared[0] = false;
        int[] counterMode = (int[]) readField(m, "ojamaCounterMode");
        counterMode[0] = 0;
        int[] maxAttack = (int[]) readField(m, "maxAttack");
        maxAttack[0] = 5;
        m.lineClearEnd(mgr.engine[0], 0);
    }

    @Test void avaDigModeSaveReplay() throws Exception {
        AvalancheVSDigRaceMode m = new AvalancheVSDigRaceMode();
        GameManager mgr = new GameManager(new EventReceiver());
        m.modeInit(mgr);
        mgr.mode = m; mgr.init(); mgr.engine[0].init();
        m.playerInit(mgr.engine[0], 0);
        CustomProperties prop = new CustomProperties();
        m.saveReplay(mgr.engine[0], 0, prop);
    }

    @Test void avaDigModeRenderMove() throws Exception {
        AvalancheVSDigRaceMode m = new AvalancheVSDigRaceMode();
        GameManager mgr = new GameManager(new EventReceiver());
        m.modeInit(mgr);
        mgr.mode = m; mgr.init(); mgr.engine[0].init();
        m.playerInit(mgr.engine[0], 0);
        mgr.engine[0].gameStarted = true;
        m.renderMove(mgr.engine[0], 0);
    }

    private static GameEngine fe(GameMode m) {
        GameManager mg = new GameManager(new EventReceiver());
        mg.mode = m; mg.init(); mg.engine[0].init();
        return mg.engine[0];
    }

    private static Field ff(Object o, String n) throws NoSuchFieldException {
        Class<?> c = o.getClass();
        while (c != null) {
            try { Field f = c.getDeclaredField(n); f.setAccessible(true); return f; }
            catch (NoSuchFieldException x) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(n);
    }

    private static int readInt(Object o, String n) throws Exception { return ff(o, n).getInt(o); }
    private static boolean readBoolean(Object o, String n) throws Exception { return ff(o, n).getBoolean(o); }
    private static Object readField(Object o, String n) throws Exception { return ff(o, n).get(o); }
    private static void setInt(Object o, String n, int v) throws Exception { ff(o, n).setInt(o, v); }
    private static void setBoolean(Object o, String n, boolean v) throws Exception { ff(o, n).setBoolean(o, v); }
    private static void setField(Object o, String n, Object v) throws Exception { ff(o, n).set(o, v); }

    private static Object inv(Object obj, String name, Object... args) throws Exception {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
        for (int i = 0; i < args.length; i++) {
            if (types[i] == Integer.class) types[i] = int.class;
            if (types[i] == Boolean.class) types[i] = boolean.class;
        }
        Method m = findM(obj.getClass(), name, types);
        m.setAccessible(true);
        return m.invoke(obj, args);
    }

    private static Method findM(Class<?> cls, String name, Class<?>... pts) throws NoSuchMethodException {
        Class<?> c = cls;
        while (c != null) {
            try { return c.getDeclaredMethod(name, pts); }
            catch (NoSuchMethodException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchMethodException(name);
    }
}

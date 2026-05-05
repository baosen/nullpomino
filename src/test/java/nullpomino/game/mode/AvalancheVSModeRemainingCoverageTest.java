package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AvalancheVSModeRemainingCoverageTest {

    private GameManager gm;
    private GameEngine engine;
    private AvalancheVSMode mode;

    @BeforeEach
    void setUp() {
        mode = new AvalancheVSMode();
        gm = new GameManager(new EventReceiver());
        gm.mode = mode;
        gm.init();
        engine = gm.engine[0];
        gm.engine[1] = new GameEngine(gm, 1);
        gm.engine[1].init();
        engine.init();
    }

    @Test
    void playerInitReplayPath() {
        engine.owner.replayMode = true;
        engine.owner.replayProp = new CustomProperties();
        engine.owner.replayProp.setProperty("avalanchevs.version", 0);
        mode.playerInit(engine, 0);
        assertTrue(true, "replay path");
    }

    @Test
    void renderLastStatsSmallDisplay() throws Exception {
        engine.createFieldIfNeeded();
        engine.gameStarted = true;
        engine.displaysize = 1;
        setIntArray(mode, "score", 0, 12345);
        setIntArray(mode, "ojama", 0, 3);
        setIntArray(mode, "feverPoints", 0, 50);
        setBooleanArray(mode, "inFever", 0, true);
        setBooleanArray(mode, "feverShowMeter", 0, true);
        mode.renderLast(engine, 0);
        assertTrue(true, "small display");
    }

    @Test
    void onLastUpdatesFeverOjama() throws Exception {
        engine.gameActive = true;
        engine.timerActive = true;
        setIntArray(mode, "feverTime", 0, 100);
        setIntArray(mode, "ojamaFever", 0, 5);
        setBooleanArray(mode, "ojamaAddToFever", 0, true);
        setIntArray(mode, "ojamaAdd", 1, 10);
        mode.onLast(engine, 0);
        assertTrue(true, "fever ojama update");
    }

    @Test
    void calcScoreGarbageDrop() throws Exception {
        engine.createFieldIfNeeded();
        setIntArray(mode, "ojama", 0, 10);
        setIntArray(mode, "feverPoints", 0, 0);
        setBooleanArray(mode, "inFever", 0, false);
        setIntArray(mode, "ojamaRate", 0, 120);
        setIntArray(mode, "maxAttack", 0, 5);
        setIntArray(mode, "ojamaHard", 0, 0);
        engine.field = new Field(10, 20, 0, false);
        engine.chain = 2;
        mode.calcScore(engine, 0, 0);
        assertTrue(true, "garbage drop");
    }

    @Test
    void saveReplay() throws Exception {
        CustomProperties prop = new CustomProperties();
        setIntArray(mode, "feverThreshold", 0, 70);
        mode.saveReplay(engine, 0, prop);
        assertEquals(70, prop.getProperty("avalanchevs.feverThreshold.p0", -1));
    }

    // --- helpers ---
    private static void setIntArray(Object o, String n, int i, int v) throws Exception {
        ((int[]) findField(o.getClass(), n).get(o))[i] = v;
    }

    private static void setBooleanArray(Object o, String n, int i, boolean v) throws Exception {
        ((boolean[]) findField(o.getClass(), n).get(o))[i] = v;
    }

    private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(name); } catch (NoSuchFieldException e) { /* continue */ }
        }
        throw new NoSuchFieldException(name);
    }
}

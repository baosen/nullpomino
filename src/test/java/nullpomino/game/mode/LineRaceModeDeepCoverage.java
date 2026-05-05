package nullpomino.game.mode;
import static org.junit.jupiter.api.Assertions.*;
import nullpomino.game.component.*;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import org.junit.jupiter.api.Test;
class LineRaceModeDeepCoverage {
    @Test void onSettingCancel() throws Exception {
        LineRaceMode m=new LineRaceMode(); GameEngine e=fe(m); m.playerInit(e,0);
        m.netIsNetPlay=false;
        e.ctrl.buttonTime[Controller.BUTTON_B]=1; e.ctrl.buttonPress[Controller.BUTTON_B]=true;
        m.onSetting(e,0); assertTrue(e.quitflag);
    }
    @Test void startGameWatch() throws Exception {
        LineRaceMode m=new LineRaceMode(); GameEngine e=fe(m); m.playerInit(e,0);
        m.netIsWatch=true; m.startGame(e,0);
        assertEquals(BGMStatus.BGM_NOTHING,e.owner.bgmStatus.bgm);
    }
    @Test void calcScoreLineClear() throws Exception {
        LineRaceMode m=new LineRaceMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.nowPieceObject=new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
        m.calcScore(e,0,1);
    }
    @Test void netRecvStats() throws Exception {
        LineRaceMode m=new LineRaceMode(); GameEngine e=fe(m); m.playerInit(e,0);
        m.netRecvStats(e,new String[]{"game","0","0","stats","30","500","60000","5.0","2.0","1","true","true"});
        assertEquals(30,e.statistics.lines);
    }
    private static GameEngine fe(LineRaceMode m) { GameManager mg=new GameManager(new EventReceiver()); mg.mode=m; mg.init(); mg.engine[0].init(); return mg.engine[0]; }
}

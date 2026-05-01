package nullpomino.util;

import java.util.List;

import nullpomino.game.mode.AvalancheFeverMode;
import nullpomino.game.mode.AvalancheMode;
import nullpomino.game.mode.AvalancheVSBombBattleMode;
import nullpomino.game.mode.AvalancheVSDigRaceMode;
import nullpomino.game.mode.AvalancheVSFeverMode;
import nullpomino.game.mode.AvalancheVSMode;
import nullpomino.game.mode.AvalancheVSSPFMode;
import nullpomino.game.mode.ComboRaceMode;
import nullpomino.game.mode.DigChallengeMode;
import nullpomino.game.mode.DigRaceMode;
import nullpomino.game.mode.ExtremeMode;
import nullpomino.game.mode.FinalMode;
import nullpomino.game.mode.GameMode;
import nullpomino.game.mode.GarbageManiaMode;
import nullpomino.game.mode.GemManiaMode;
import nullpomino.game.mode.GradeMania2Mode;
import nullpomino.game.mode.GradeMania3Mode;
import nullpomino.game.mode.GradeManiaMode;
import nullpomino.game.mode.LineRaceMode;
import nullpomino.game.mode.MarathonMode;
import nullpomino.game.mode.MarathonPlusMode;
import nullpomino.game.mode.NetVSBattleMode;
import nullpomino.game.mode.NetVSDigRaceMode;
import nullpomino.game.mode.NetVSLineRaceMode;
import nullpomino.game.mode.PhantomManiaMode;
import nullpomino.game.mode.PhysicianMode;
import nullpomino.game.mode.PhysicianVSMode;
import nullpomino.game.mode.PracticeMode;
import nullpomino.game.mode.RetroManiaMode;
import nullpomino.game.mode.RetroMarathonMode;
import nullpomino.game.mode.RetroMasteryMode;
import nullpomino.game.mode.SPFMode;
import nullpomino.game.mode.ScoreAttackMode;
import nullpomino.game.mode.ScoreRaceMode;
import nullpomino.game.mode.SpeedMania2Mode;
import nullpomino.game.mode.SpeedManiaMode;
import nullpomino.game.mode.SquareMode;
import nullpomino.game.mode.TechnicianMode;
import nullpomino.game.mode.TimeAttackMode;
import nullpomino.game.mode.ToolVSMapEditMode;
import nullpomino.game.mode.UltraMode;
import nullpomino.game.mode.VSBattleMode;
import nullpomino.game.mode.VSDigRaceMode;
import nullpomino.game.mode.VSLineRaceMode;

/**
 * The roster of every {@link GameMode} the game knows about. Replaces the
 * pre-existing {@code config/list/mode.lst} text file, which listed the same
 * classes by FQN string and was loaded reflectively at startup. Entries here
 * are direct class references so the compiler catches renames and removals.
 *
 * <p>Order matches the legacy file. Both netplay and single-player modes are
 * included; consumers that want only one filter via
 * {@link nullpomino.game.mode.GameMode#isNetplayMode()}.
 */
public final class ModeRegistry {
	private static final List<Class<? extends GameMode>> MODES = List.of(
			MarathonMode.class,
			MarathonPlusMode.class,
			ExtremeMode.class,
			LineRaceMode.class,
			ScoreRaceMode.class,
			DigRaceMode.class,
			ComboRaceMode.class,
			UltraMode.class,
			TechnicianMode.class,
			SquareMode.class,
			DigChallengeMode.class,
			RetroMarathonMode.class,
			RetroMasteryMode.class,
			RetroManiaMode.class,
			GradeManiaMode.class,
			GradeMania2Mode.class,
			GradeMania3Mode.class,
			ScoreAttackMode.class,
			SpeedManiaMode.class,
			SpeedMania2Mode.class,
			GarbageManiaMode.class,
			PhantomManiaMode.class,
			FinalMode.class,
			TimeAttackMode.class,
			PracticeMode.class,
			GemManiaMode.class,
			VSLineRaceMode.class,
			VSDigRaceMode.class,
			VSBattleMode.class,
			ToolVSMapEditMode.class,
			NetVSBattleMode.class,
			NetVSLineRaceMode.class,
			NetVSDigRaceMode.class,
			AvalancheMode.class,
			AvalancheFeverMode.class,
			AvalancheVSMode.class,
			AvalancheVSFeverMode.class,
			AvalancheVSDigRaceMode.class,
			AvalancheVSBombBattleMode.class,
			AvalancheVSSPFMode.class,
			PhysicianMode.class,
			PhysicianVSMode.class,
			SPFMode.class);

	private ModeRegistry() {}

	public static List<Class<? extends GameMode>> all() {
		return MODES;
	}
}

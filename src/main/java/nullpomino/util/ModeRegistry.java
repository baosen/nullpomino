package nullpomino.util;

import java.util.List;

import nullpomino.game.subsystem.mode.AvalancheFeverMode;
import nullpomino.game.subsystem.mode.AvalancheMode;
import nullpomino.game.subsystem.mode.AvalancheVSBombBattleMode;
import nullpomino.game.subsystem.mode.AvalancheVSDigRaceMode;
import nullpomino.game.subsystem.mode.AvalancheVSFeverMode;
import nullpomino.game.subsystem.mode.AvalancheVSMode;
import nullpomino.game.subsystem.mode.AvalancheVSSPFMode;
import nullpomino.game.subsystem.mode.ComboRaceMode;
import nullpomino.game.subsystem.mode.DigChallengeMode;
import nullpomino.game.subsystem.mode.DigRaceMode;
import nullpomino.game.subsystem.mode.ExtremeMode;
import nullpomino.game.subsystem.mode.FinalMode;
import nullpomino.game.subsystem.mode.GameMode;
import nullpomino.game.subsystem.mode.GarbageManiaMode;
import nullpomino.game.subsystem.mode.GemManiaMode;
import nullpomino.game.subsystem.mode.GradeMania2Mode;
import nullpomino.game.subsystem.mode.GradeMania3Mode;
import nullpomino.game.subsystem.mode.GradeManiaMode;
import nullpomino.game.subsystem.mode.LineRaceMode;
import nullpomino.game.subsystem.mode.MarathonMode;
import nullpomino.game.subsystem.mode.MarathonPlusMode;
import nullpomino.game.subsystem.mode.NetVSBattleMode;
import nullpomino.game.subsystem.mode.NetVSDigRaceMode;
import nullpomino.game.subsystem.mode.NetVSLineRaceMode;
import nullpomino.game.subsystem.mode.PhantomManiaMode;
import nullpomino.game.subsystem.mode.PhysicianMode;
import nullpomino.game.subsystem.mode.PhysicianVSMode;
import nullpomino.game.subsystem.mode.PracticeMode;
import nullpomino.game.subsystem.mode.RetroManiaMode;
import nullpomino.game.subsystem.mode.RetroMarathonMode;
import nullpomino.game.subsystem.mode.RetroMasteryMode;
import nullpomino.game.subsystem.mode.SPFMode;
import nullpomino.game.subsystem.mode.ScoreAttackMode;
import nullpomino.game.subsystem.mode.ScoreRaceMode;
import nullpomino.game.subsystem.mode.SpeedMania2Mode;
import nullpomino.game.subsystem.mode.SpeedManiaMode;
import nullpomino.game.subsystem.mode.SquareMode;
import nullpomino.game.subsystem.mode.TechnicianMode;
import nullpomino.game.subsystem.mode.TimeAttackMode;
import nullpomino.game.subsystem.mode.ToolVSMapEditMode;
import nullpomino.game.subsystem.mode.UltraMode;
import nullpomino.game.subsystem.mode.VSBattleMode;
import nullpomino.game.subsystem.mode.VSDigRaceMode;
import nullpomino.game.subsystem.mode.VSLineRaceMode;

/**
 * The roster of every {@link GameMode} the game knows about. Replaces the
 * pre-existing {@code config/list/mode.lst} text file, which listed the same
 * classes by FQN string and was loaded reflectively at startup. Entries here
 * are direct class references so the compiler catches renames and removals.
 *
 * <p>Order matches the legacy file. Both netplay and single-player modes are
 * included; consumers that want only one filter via
 * {@link nullpomino.game.subsystem.mode.GameMode#isNetplayMode()}.
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

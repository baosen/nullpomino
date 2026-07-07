package nullpomino.util;

import java.util.List;
import java.util.function.Supplier;

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
 * Constructor suppliers for every {@link GameMode}, in {@link ModeRegistry}
 * order. Used by the browser (TeaVM) entry point, which loads modes by
 * instantiation rather than reflectively (TeaVM cannot resolve
 * {@code Class.forName}). Includes the netplay modes: they are web-safe,
 * since the netplay client stack reaches sockets only through the
 * RoomNet/RoomTransport seam (WebRTC on the web); the {@code NetBaseClient}
 * base class carries no transport of its own.
 * The desktop build keeps using {@link ModeRegistry#all()} unchanged.
 */
public final class StandaloneModeRegistry {
	private static final List<Supplier<? extends GameMode>> SUPPLIERS = List.of(
			MarathonMode::new,
			MarathonPlusMode::new,
			ExtremeMode::new,
			LineRaceMode::new,
			ScoreRaceMode::new,
			DigRaceMode::new,
			ComboRaceMode::new,
			UltraMode::new,
			TechnicianMode::new,
			SquareMode::new,
			DigChallengeMode::new,
			RetroMarathonMode::new,
			RetroMasteryMode::new,
			RetroManiaMode::new,
			GradeManiaMode::new,
			GradeMania2Mode::new,
			GradeMania3Mode::new,
			ScoreAttackMode::new,
			SpeedManiaMode::new,
			SpeedMania2Mode::new,
			GarbageManiaMode::new,
			PhantomManiaMode::new,
			FinalMode::new,
			TimeAttackMode::new,
			PracticeMode::new,
			GemManiaMode::new,
			VSLineRaceMode::new,
			VSDigRaceMode::new,
			VSBattleMode::new,
			ToolVSMapEditMode::new,
			NetVSBattleMode::new,
			NetVSLineRaceMode::new,
			NetVSDigRaceMode::new,
			AvalancheMode::new,
			AvalancheFeverMode::new,
			AvalancheVSMode::new,
			AvalancheVSFeverMode::new,
			AvalancheVSDigRaceMode::new,
			AvalancheVSBombBattleMode::new,
			AvalancheVSSPFMode::new,
			PhysicianMode::new,
			PhysicianVSMode::new,
			SPFMode::new);

	private StandaloneModeRegistry() {}

	/** Constructor suppliers for all modes, in ModeRegistry order. */
	public static List<Supplier<? extends GameMode>> suppliers() {
		return SUPPLIERS;
	}
}

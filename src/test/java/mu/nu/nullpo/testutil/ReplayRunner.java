package mu.nu.nullpo.testutil;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Objects;

import mu.nu.nullpo.game.component.RuleOptions;
import mu.nu.nullpo.game.component.Statistics;
import mu.nu.nullpo.game.event.EventReceiver;
import mu.nu.nullpo.game.play.GameManager;
import mu.nu.nullpo.game.subsystem.mode.GameMode;
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer;
import mu.nu.nullpo.util.CustomProperties;
import mu.nu.nullpo.util.GeneralUtil;
import mu.nu.nullpo.util.ModeManager;

/**
 * Headless replay player. Runs a .rep file through a real GameEngine
 * under a no-op EventReceiver and returns a numeric snapshot of the
 * finishing state, so characterisation tests can pin mode behaviour
 * before a refactor and compare after.
 */
public final class ReplayRunner {

	/** Hard cap on simulated frames so a broken replay can't hang the test. */
	private static final int MAX_FRAMES = 600_000;

	private ReplayRunner() {
	}

	public static Snapshot run(Path replayFile, ModeManager modeManager) throws Exception {
		CustomProperties prop = new CustomProperties();
		try (FileInputStream in = new FileInputStream(replayFile.toFile())) {
			prop.load(in);
		}

		String modeName = prop.getProperty("name.mode", "");
		GameMode mode = modeManager.getMode(modeName);
		if (mode == null) {
			throw new IllegalStateException("unknown mode in " + replayFile + ": " + modeName);
		}

		GameManager gm = new GameManager(new EventReceiver());
		gm.replayMode = true;
		gm.replayProp = prop;
		gm.mode = mode;
		gm.init();

		for (int i = 0; i < gm.getPlayers(); i++) {
			RuleOptions ruleopt = new RuleOptions();
			ruleopt.readProperty(prop, i);
			gm.engine[i].ruleopt = ruleopt;

			if (ruleopt.strRandomizer != null && !ruleopt.strRandomizer.isEmpty()) {
				Randomizer randomizer = GeneralUtil.loadRandomizer(ruleopt.strRandomizer);
				gm.engine[i].randomizer = randomizer;
			}
			if (ruleopt.strWallkick != null && !ruleopt.strWallkick.isEmpty()) {
				Wallkick wallkick = GeneralUtil.loadWallkick(ruleopt.strWallkick);
				gm.engine[i].wallkick = wallkick;
			}

			gm.engine[i].init();
		}

		int frames = 0;
		while (!gm.getQuitFlag() && frames < MAX_FRAMES) {
			gm.updateAll();
			frames++;
		}

		Statistics s = gm.engine[0].statistics;
		Snapshot snap = new Snapshot(modeName, s.score, s.lines, s.time, s.level, s.totalPieceLocked, frames);
		gm.shutdown();
		return snap;
	}

	/**
	 * Frozen numeric summary of a finished replay. Two snapshots equal iff
	 * every field matches — if a refactor shifts scoring by one point, the
	 * characterisation test will see it.
	 */
	public static final class Snapshot {
		public final String modeName;
		public final int score;
		public final int lines;
		public final int time;
		public final int level;
		public final int totalPieceLocked;
		public final int simulatedFrames;

		public Snapshot(String modeName, int score, int lines, int time, int level,
				int totalPieceLocked, int simulatedFrames) {
			this.modeName = modeName;
			this.score = score;
			this.lines = lines;
			this.time = time;
			this.level = level;
			this.totalPieceLocked = totalPieceLocked;
			this.simulatedFrames = simulatedFrames;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Snapshot)) return false;
			Snapshot that = (Snapshot) o;
			return score == that.score
					&& lines == that.lines
					&& time == that.time
					&& level == that.level
					&& totalPieceLocked == that.totalPieceLocked
					&& simulatedFrames == that.simulatedFrames
					&& Objects.equals(modeName, that.modeName);
		}

		@Override
		public int hashCode() {
			return Objects.hash(modeName, score, lines, time, level, totalPieceLocked, simulatedFrames);
		}

		@Override
		public String toString() {
			return "Snapshot{mode=" + modeName + ", score=" + score + ", lines=" + lines
					+ ", time=" + time + ", level=" + level
					+ ", pieces=" + totalPieceLocked + ", frames=" + simulatedFrames + "}";
		}
	}
}

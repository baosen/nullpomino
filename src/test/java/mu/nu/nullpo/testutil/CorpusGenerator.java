package mu.nu.nullpo.testutil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import mu.nu.nullpo.game.component.RuleOptions;
import mu.nu.nullpo.game.event.EventReceiver;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.game.play.GameManager;
import mu.nu.nullpo.game.subsystem.mode.GameMode;
import mu.nu.nullpo.game.subsystem.wallkick.Wallkick;
import mu.nu.nullpo.util.GeneralUtil;
import mu.nu.nullpo.util.ModeManager;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer;
import net.tetrisconcept.poochy.nullpomino.ai.PoochyBot;

/**
 * Headless AI-driven replay generator. For each named mode, wires
 * PoochyBot into a GameManager, bypasses the SETTING menu by
 * promoting each engine straight to Status.READY (which calls
 * mode.startGame as usual), then steps updateAll() until every
 * player has reached GAMEOVER/RESULT or a frame cap trips.
 *
 * On completion, writes gameManager.replayProp to disk as a .rep
 * file under {@code <outputDir>/<family>/<slug>-seed<seed>.rep},
 * ready to be run through {@link ReplayRunner} by
 * ModeReplayCharacterisationTest and baselined by
 * {@link BaselineRecorder}.
 *
 * Invocation:
 *   bazel run //src/test/java:CorpusGenerator -- \
 *       src/test/resources/replay \
 *       MARATHON+ GRADE\ MANIA GRADE\ MANIA\ 2 LINE\ RACE ...
 *
 * With no mode arguments, uses DEFAULT_MODES below (every family the
 * simplification plan targets).
 *
 * Skipped automatically:
 *   - net modes (isNetplayMode == true; they need a loopback server)
 *   - any mode missing from mode.lst
 */
public final class CorpusGenerator {

	/** Default ruleset. Pick any mode-agnostic rule file. */
	private static final String DEFAULT_RULE = "config/rule/Standard.rul";

	/** Deterministic seed so reruns produce identical .rep files. */
	private static final long DEFAULT_SEED = 0xC0FFEEL;

	/** Frame safety cap for a single generation run. 1000s @ 60fps. */
	private static final int MAX_FRAMES = 60_000;

	/** Families we need corpus for. Mode names must match GameMode.getName(). */
	private static final List<String> DEFAULT_MODES = Arrays.asList(
			"MARATHON+",
			"GRADE MANIA",
			"GRADE MANIA 2",
			"GRADE MANIA 3",
			"LINE RACE",
			"VS-LINE RACE",
			"DIG RACE",
			"VS-DIG RACE",
			"AVALANCHE 1P (RC2)"
			// "AVALANCHE 1P FEVER MARATHON (RC2)" needs propFeverMap, loaded only
			// by the SETTING menu we bypass. Covered transitively through the
			// Avalanche1PDummyMode base class via the plain 1P replay above.
	);

	public static void main(String[] args) throws Exception {
		String cwd = System.getenv("BUILD_WORKING_DIRECTORY");
		Path base = (cwd != null) ? Paths.get(cwd) : Paths.get("").toAbsolutePath();

		Path outputDir = (args.length >= 1) ? base.resolve(args[0]) : base.resolve("src/test/resources/replay");
		List<String> modes = (args.length >= 2) ? Arrays.asList(args).subList(1, args.length) : DEFAULT_MODES;

		ModeManager mm = new ModeManager();
		try (BufferedReader r = new BufferedReader(new FileReader(base.resolve("config/list/mode.lst").toFile()))) {
			mm.loadGameModes(r);
		}

		Path rulePath = base.resolve(DEFAULT_RULE);
		RuleOptions baseRule = GeneralUtil.loadRule(rulePath.toString());
		if (baseRule == null) {
			System.err.println("could not load ruleset: " + rulePath);
			System.exit(1);
		}

		int ok = 0, skipped = 0, failed = 0;
		for (String modeName : modes) {
			GameMode mode = mm.getMode(modeName);
			if (mode == null) {
				System.err.println("SKIP (unknown mode): " + modeName);
				skipped++;
				continue;
			}
			if (mode.isNetplayMode()) {
				System.err.println("SKIP (net mode, needs loopback server): " + modeName);
				skipped++;
				continue;
			}
			try {
				Path written = generateFor(mode, baseRule, outputDir);
				System.out.println("OK  " + modeName + " -> " + written);
				ok++;
			} catch (Throwable t) {
				System.err.println("FAIL " + modeName + ": " + t);
				t.printStackTrace(System.err);
				failed++;
			}
		}
		System.out.println("corpus generation: " + ok + " ok, " + skipped + " skipped, " + failed + " failed");
		if (failed > 0) System.exit(2);
	}

	private static Path generateFor(GameMode mode, RuleOptions baseRule, Path outputDir) throws Exception {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();

		for (int i = 0; i < gm.getPlayers(); i++) {
			GameEngine engine = gm.engine[i];
			engine.ruleopt = new RuleOptions(baseRule);
			if (engine.ruleopt.strRandomizer != null && !engine.ruleopt.strRandomizer.isEmpty()) {
				Randomizer randomizer = GeneralUtil.loadRandomizer(engine.ruleopt.strRandomizer);
				engine.randomizer = randomizer;
			}
			if (engine.ruleopt.strWallkick != null && !engine.ruleopt.strWallkick.isEmpty()) {
				Wallkick wallkick = GeneralUtil.loadWallkick(engine.ruleopt.strWallkick);
				engine.wallkick = wallkick;
			}
			engine.ai = new PoochyBot();
			engine.aiUseThread = false;
			engine.aiMoveDelay = 0;
			engine.aiThinkDelay = 0;
			engine.aiShowHint = false;
			engine.aiPrethink = false;
			engine.init();
			engine.randSeed = DEFAULT_SEED + i;
			engine.random = new java.util.Random(engine.randSeed);
			// Skip the mode's SETTING menu — startGame() runs when stat enters READY.
			engine.stat = GameEngine.Status.READY;
			engine.resetStatc();
		}

		int frames = 0;
		while (frames < MAX_FRAMES) {
			gm.updateAll();
			frames++;
			if (gm.getQuitFlag()) break;
			if (everyEngineDone(gm)) break;
		}

		// Auto-save triggers on GAMEOVER→RESULT transition; force it if the
		// loop bailed out early (safety cap, or a mode that doesn't terminate).
		if (gm.replayProp.getProperty("version.core") == null) {
			gm.saveReplay();
		}

		Path outFamily = outputDir.resolve(familyDir(mode.getName()));
		Files.createDirectories(outFamily);
		Path outFile = outFamily.resolve(slugify(mode.getName()) + "-poochybot-" + Long.toHexString(DEFAULT_SEED) + ".rep");
		try (OutputStream os = Files.newOutputStream(outFile)) {
			gm.replayProp.store(os, "NullpoMino Replay (corpus)");
		}
		gm.shutdown();
		return outFile;
	}

	private static boolean everyEngineDone(GameManager gm) {
		for (int i = 0; i < gm.getPlayers(); i++) {
			GameEngine.Status s = gm.engine[i].stat;
			if (s != GameEngine.Status.RESULT && s != GameEngine.Status.GAMEOVER) return false;
		}
		return true;
	}

	private static String slugify(String modeName) {
		String lower = modeName.toLowerCase(Locale.ROOT).replace("+", "plus");
		return lower.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
	}

	/** Pick a sensible on-disk directory per family. */
	private static String familyDir(String modeName) {
		String lower = modeName.toLowerCase(Locale.ROOT);
		if (lower.startsWith("marathon")) return "marathon";
		if (lower.startsWith("grade mania")) return "grademania";
		if (lower.endsWith("line race")) return "linerace";
		if (lower.endsWith("dig race")) return "digrace";
		if (lower.startsWith("avalanche")) return "avalanche";
		return slugify(modeName);
	}

	private CorpusGenerator() {
	}
}

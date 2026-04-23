package nullpomino.game.subsystem.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import nullpomino.testutil.ReplayRunner;
import nullpomino.testutil.ReplayRunner.Snapshot;
import nullpomino.util.ModeManager;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Characterisation suite. Runs every .rep file under
 * src/test/resources/replay/ through the real GameEngine and asserts
 * the resulting Snapshot exactly matches the expected properties file
 * under src/test/resources/expected/. Baselines are recorded once
 * against the pre-refactor tree via BaselineRecorder; any refactor
 * that changes scoring / timing / piece-counting reshapes a snapshot
 * and this suite fails with a precise diff.
 */
class ModeReplayCharacterisationTest {

	private static final Path REPLAY_ROOT = Paths.get("src/test/resources/replay");
	private static final Path EXPECTED_ROOT = Paths.get("src/test/resources/expected");

	@TestFactory
	Stream<DynamicTest> everyRecordedReplayStillMatchesItsBaseline() throws Exception {
		assertTrue(Files.isDirectory(REPLAY_ROOT), "replay corpus missing: " + REPLAY_ROOT);

		ModeManager modeManager = new ModeManager();
		try (BufferedReader r = new BufferedReader(new FileReader("config/list/mode.lst"))) {
			modeManager.loadGameModes(r);
		}

		List<Path> replays = new ArrayList<>();
		try (Stream<Path> walk = Files.walk(REPLAY_ROOT)) {
			walk.filter(p -> p.toString().endsWith(".rep")).forEach(replays::add);
		}
		assertTrue(!replays.isEmpty(), "no .rep files found under " + REPLAY_ROOT);

		return replays.stream().map(rep -> DynamicTest.dynamicTest(
				REPLAY_ROOT.relativize(rep).toString(),
				() -> assertReplayMatchesBaseline(rep, modeManager)));
	}

	private static void assertReplayMatchesBaseline(Path replay, ModeManager modeManager) throws Exception {
		Snapshot actual = ReplayRunner.run(replay, modeManager);

		Path relative = REPLAY_ROOT.relativize(replay);
		String stem = relative.toString().replaceFirst("\\.rep$", "") + ".properties";
		Path expectedFile = EXPECTED_ROOT.resolve(stem);
		assertTrue(Files.exists(expectedFile),
				"missing baseline: " + expectedFile + " (regenerate via //src/test/java:BaselineRecorder)");

		Properties p = new Properties();
		try (InputStream in = Files.newInputStream(expectedFile)) {
			p.load(in);
		}

		assertEquals(p.getProperty("modeName"), actual.modeName, "modeName mismatch for " + relative);
		assertEquals(Integer.parseInt(p.getProperty("score")), actual.score, "score mismatch for " + relative);
		assertEquals(Integer.parseInt(p.getProperty("lines")), actual.lines, "lines mismatch for " + relative);
		assertEquals(Integer.parseInt(p.getProperty("time")), actual.time, "time mismatch for " + relative);
		assertEquals(Integer.parseInt(p.getProperty("level")), actual.level, "level mismatch for " + relative);
		assertEquals(Integer.parseInt(p.getProperty("totalPieceLocked")), actual.totalPieceLocked,
				"totalPieceLocked mismatch for " + relative);
		assertEquals(Integer.parseInt(p.getProperty("simulatedFrames")), actual.simulatedFrames,
				"simulatedFrames mismatch for " + relative);
	}
}

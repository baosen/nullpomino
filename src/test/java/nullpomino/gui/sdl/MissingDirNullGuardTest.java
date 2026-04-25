package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pins the null-guard for File.list / Arrays.sort in the SDL replay-
 * select screen. File.list returns null when the path doesn't exist, and
 * the screen must propagate that null instead of NPE'ing inside
 * Arrays.sort. Hits StateReplaySelectSDL.getReplayFileList directly
 * (same-package, protected access).
 *
 * StateConfigRuleSelectSDL.getRuleFileList carries the same fix but
 * isn't covered here: its "config/rule" path is hard-coded against the
 * native CWD (user.dir doesn't redirect File's native resolution on
 * Linux), so the test sandbox's config/ data dependency always wins.
 * The fix is the same one-liner as the replay path; this test pins
 * the pattern.
 */
class MissingDirNullGuardTest {

	@Test
	void replaySelectReturnsNullWhenReplayDirIsMissing(@TempDir Path tmp) {
		Path missing = tmp.resolve("does-not-exist");
		NullpoMinoSDL.propGlobal = new CustomProperties();
		NullpoMinoSDL.propGlobal.setProperty(
				"custom.replay.directory", missing.toString());

		StateReplaySelectSDL state = new StateReplaySelectSDL();

		String[] list = assertDoesNotThrow(state::getReplayFileList,
				"getReplayFileList must not throw on a missing replay dir");
		assertNull(list, "missing replay dir must produce a null list");
	}
}

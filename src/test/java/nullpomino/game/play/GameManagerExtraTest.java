package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nullpomino.game.event.EventReceiver;

/**
 * Covers remaining uncovered lines and branches in GameManager.
 */
class GameManagerExtraTest {

	@TempDir
	Path tempDir;

	/**
	 * Test resolveCommitHash when .git/HEAD exists as a loose ref in CWD.
	 * This covers lines 130-135 (the body of resolveCommitHash after the
	 * isRegularFile check).
	 */
	@Test
	void resolveCommitHashReadsFromLooseRef() throws Exception {
		// Save current hash, then try to test the resolve path
		// Since resolveCommitHash uses Paths.get(".git", "HEAD") relative to CWD,
		// we create .git/HEAD in the Bazel test sandbox CWD.
		resetCachedCommitHash();

		// Create .git/HEAD and loose ref
		Path gitDir = Path.of(".git");
		Files.createDirectories(gitDir.resolve("refs/heads"));

		Path headFile = gitDir.resolve("HEAD");
		boolean headExisted = Files.exists(headFile);
		String originalHeadContent = null;
		if (headExisted) {
			originalHeadContent = Files.readString(headFile);
		}

		try {
			// Write HEAD as loose ref
			Files.writeString(headFile, "ref: refs/heads/test_branch\n", StandardCharsets.UTF_8);
			Files.writeString(gitDir.resolve("refs/heads/test_branch"),
					"deadbeefdeadbeefdeadbeefdeadbeefdeadbeef\n", StandardCharsets.UTF_8);

			resetCachedCommitHash();
			String hash = GameManager.getCommitHash();

			assertEquals("deadbee", hash);
		} finally {
			// Clean up
			Files.deleteIfExists(gitDir.resolve("refs/heads/test_branch"));
			if (!headExisted) {
				Files.deleteIfExists(headFile);
			} else if (originalHeadContent != null) {
				Files.writeString(headFile, originalHeadContent);
			}
		}
	}

	/**
	 * Test resolveCommitHash when .git/HEAD directly contains a hash (detached HEAD).
	 */
	@Test
	void resolveCommitHashReadsDetachedHead() throws Exception {
		resetCachedCommitHash();

		Path gitDir = Path.of(".git");
		Path headFile = gitDir.resolve("HEAD");
		boolean headExisted = Files.exists(headFile);
		String originalHeadContent = null;
		if (headExisted) {
			originalHeadContent = Files.readString(headFile);
		}

		try {
			// Write HEAD with a direct hash
			Files.writeString(headFile,
					"abcdef1234567890abcdef1234567890abcdef12\n", StandardCharsets.UTF_8);

			resetCachedCommitHash();
			String hash = GameManager.getCommitHash();

			assertEquals("abcdef1", hash);
		} finally {
			if (!headExisted) {
				Files.deleteIfExists(headFile);
			} else if (originalHeadContent != null) {
				Files.writeString(headFile, originalHeadContent);
			}
		}
	}

	/**
	 * Test resolveCommitHash when .git/HEAD is missing (returns "unknown").
	 */
	@Test
	void resolveCommitHashReturnsUnknownWhenMissingHead() throws Exception {
		Field cached = GameManager.class.getDeclaredField("cachedCommitHash");
		cached.setAccessible(true);
		cached.set(null, null);

		String hash = GameManager.getCommitHash();
		assertEquals("unknown", hash);
	}

	/**
	 * Test readRef with an empty line in packed-refs.
	 */
	@Test
	void readRefSkipsEmptyLinesInPackedRefs() throws Exception {
		Method readRef = GameManager.class.getDeclaredMethod("readRef", String.class);
		readRef.setAccessible(true);

		Path gitDir = Path.of(".git");
		boolean createdGitDir = !Files.isDirectory(gitDir);
		try {
			if (createdGitDir) {
				Files.createDirectories(gitDir);
			}
			Path packed = gitDir.resolve("packed-refs");
			boolean existed = Files.exists(packed);
			String oldContent = existed ? Files.readString(packed) : null;

			try {
				Files.writeString(packed,
						"\n" +  // empty line
						"# comment\n" +
						"deadbeefdeadbeefdeadbeefdeadbeefdeadbeef refs/heads/somebranch\n",
						StandardCharsets.UTF_8);

				String result = (String) readRef.invoke(null, "refs/heads/somebranch");
				assertEquals("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef", result);
			} finally {
				if (existed && oldContent != null) {
					Files.writeString(packed, oldContent);
				} else {
					Files.deleteIfExists(packed);
				}
			}
		} finally {
			if (createdGitDir) {
				deletePath(gitDir);
			}
		}
	}

	/**
	 * Test readRef when packed-refs has caret-prefixed lines (alternate ref).
	 */
	@Test
	void readRefSkipsCaretLinesInPackedRefs() throws Exception {
		Method readRef = GameManager.class.getDeclaredMethod("readRef", String.class);
		readRef.setAccessible(true);

		Path gitDir = Path.of(".git");
		boolean createdGitDir = !Files.isDirectory(gitDir);
		try {
			if (createdGitDir) {
				Files.createDirectories(gitDir);
			}
			Path packed = gitDir.resolve("packed-refs");
			boolean existed = Files.exists(packed);
			String oldContent = existed ? Files.readString(packed) : null;

			try {
				Files.writeString(packed,
						"^deadbeefdeadbeefdeadbeefdeadbeefdeadbeef\n" +
						"deadbeefdeadbeefdeadbeefdeadbeefdeadbeef refs/heads/somebranch\n",
						StandardCharsets.UTF_8);

				String result = (String) readRef.invoke(null, "refs/heads/somebranch");
				assertEquals("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef", result);
			} finally {
				if (existed && oldContent != null) {
					Files.writeString(packed, oldContent);
				} else {
					Files.deleteIfExists(packed);
				}
			}
		} finally {
			if (createdGitDir) {
				deletePath(gitDir);
			}
		}
	}

	/**
	 * Test readRef when ref is not found in packed-refs (search continues).
	 */
	@Test
	void readRefContinuesSearchWhenRefDoesNotMatch() throws Exception {
		Method readRef = GameManager.class.getDeclaredMethod("readRef", String.class);
		readRef.setAccessible(true);

		Path gitDir = Path.of(".git");
		boolean createdGitDir = !Files.isDirectory(gitDir);
		try {
			if (createdGitDir) {
				Files.createDirectories(gitDir);
			}
			Path packed = gitDir.resolve("packed-refs");
			boolean existed = Files.exists(packed);
			String oldContent = existed ? Files.readString(packed) : null;

			try {
				Files.writeString(packed,
						"deadbeefdeadbeefdeadbeefdeadbeefdeadbeef refs/heads/other\n",
						StandardCharsets.UTF_8);

				String result = (String) readRef.invoke(null, "refs/heads/nonexistent");
				assertNull(result);
			} finally {
				if (existed && oldContent != null) {
					Files.writeString(packed, oldContent);
				} else {
					Files.deleteIfExists(packed);
				}
			}
		} finally {
			if (createdGitDir) {
				deletePath(gitDir);
			}
		}
	}

	/**
	 * Test init() with modeConfig == null path.
	 */
	@Test
	void initSetsModeConfigWhenReceiverReturnsNull() {
		EventReceiver receiver = new EventReceiver() {
			@Override
			public nullpomino.util.CustomProperties loadModeConfig() {
				return null;
			}
		};
		GameManager gm = new GameManager(receiver);
		gm.init();

		assertNotNull(gm.modeConfig);
	}

	/**
	 * Test getQuitFlag with null engine slot.
	 */
	@Test
	void getQuitFlagWithNullEngineSlot() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0] = null;

		assertFalse(gm.getQuitFlag());
	}

	/**
	 * Test isGameActive with null engine slot.
	 */
	@Test
	void isGameActiveWithNullEngineSlot() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0] = null;

		assertFalse(gm.isGameActive());
	}

	/**
	 * Test that init properly handles re-init path.
	 */
	@Test
	void initReinitializesEngines() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.replayProp = new nullpomino.util.CustomProperties();
		gm.init();

		assertFalse(gm.replayMode);
		assertEquals(1, gm.engine.length);
	}

	private static void resetCachedCommitHash() throws Exception {
		Field f = GameManager.class.getDeclaredField("cachedCommitHash");
		f.setAccessible(true);
		f.set(null, null);
	}

	private static void deletePath(Path p) throws IOException {
		if (Files.isDirectory(p)) {
			try (var walk = Files.walk(p)) {
				walk.sorted(java.util.Comparator.reverseOrder())
					.forEach(path -> { try { Files.deleteIfExists(path); } catch (Exception ignored) {} });
			}
		} else {
			Files.deleteIfExists(p);
		}
	}
}

package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers the {@link GameManager#resolveCommitHash(Path)} and
 * {@link GameManager#readRef(Path, String)} branches.
 *
 * <p>These tests drive a throwaway git directory ({@code @TempDir}) rather than
 * the repository's real {@code .git}. Earlier versions wrote to the real
 * {@code .git/packed-refs}/{@code HEAD} relative to the working directory and
 * corrupted the repo when run outside Bazel's sandbox (e.g. via a direct
 * javac+JUnit run); the git-dir parameter added to GameManager removes that hazard.
 */
class GameManagerCommitHashEdgeTest {

	/** A throwaway stand-in for .git — never the repository's real one. */
	@TempDir
	Path gitDir;

	// ---- resolveCommitHash(Path) ----

	@Test
	void resolveReturnsUnknownWhenHeadMissing() {
		assertEquals("unknown", GameManager.resolveCommitHash(gitDir));
	}

	@Test
	void resolveReadsDetachedHeadHash() throws Exception {
		Files.writeString(gitDir.resolve("HEAD"), "abcdef1234567890abcdef1234567890abcdef12\n");
		assertEquals("abcdef1", GameManager.resolveCommitHash(gitDir));
	}

	@Test
	void resolveReturnsUnknownForShortDetachedHash() throws Exception {
		Files.writeString(gitDir.resolve("HEAD"), "abc\n");
		assertEquals("unknown", GameManager.resolveCommitHash(gitDir));
	}

	@Test
	void resolveFollowsHeadRefToLooseRef() throws Exception {
		Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/main\n");
		Path ref = gitDir.resolve("refs/heads/main");
		Files.createDirectories(ref.getParent());
		Files.writeString(ref, "0123456789012345678901234567890123456789\n");
		assertEquals("0123456", GameManager.resolveCommitHash(gitDir));
	}

	@Test
	void resolveFollowsHeadRefToPackedRefs() throws Exception {
		Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/main\n");
		Files.writeString(gitDir.resolve("packed-refs"),
				"# pack-refs with: peeled fully-peeled sorted\n"
				+ "^0000000000000000000000000000000000000000\n"
				+ "\n"
				+ "fedcba9876543210fedcba9876543210fedcba98 refs/heads/main\n");
		assertEquals("fedcba9", GameManager.resolveCommitHash(gitDir));
	}

	@Test
	void resolveReturnsUnknownWhenHeadRefUnresolvable() throws Exception {
		Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/missing\n");
		assertEquals("unknown", GameManager.resolveCommitHash(gitDir));
	}

	@Test
	void resolveReturnsUnknownWhenPackedRefsHasNoMatch() throws Exception {
		Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/main\n");
		Files.writeString(gitDir.resolve("packed-refs"),
				"# header\n"
				+ "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef refs/heads/other\n");
		assertEquals("unknown", GameManager.resolveCommitHash(gitDir));
	}

	@Test
	void resolveCatchesIoExceptionWhenHeadUnreadable() throws Exception {
		Path head = gitDir.resolve("HEAD");
		Files.writeString(head, "ref: refs/heads/main\n");
		// stat(2) still succeeds (isRegularFile true) but open(2) fails -> IOException.
		Files.setPosixFilePermissions(head, Collections.emptySet());
		try {
			assertEquals("unknown", GameManager.resolveCommitHash(gitDir));
		} finally {
			Files.setPosixFilePermissions(head,
					Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
		}
	}

	// ---- readRef(Path, String) ----

	@Test
	void readRefReturnsContentFromLooseRef() throws Exception {
		Path ref = gitDir.resolve("refs/heads/loose");
		Files.createDirectories(ref.getParent());
		Files.writeString(ref, "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef\n");
		assertEquals("deadbeefdeadbeefdeadbeefdeadbeefdeadbeef",
				GameManager.readRef(gitDir, "refs/heads/loose"));
	}

	@Test
	void readRefReturnsNullWhenNoLooseAndNoPackedRefs() throws Exception {
		assertNull(GameManager.readRef(gitDir, "refs/heads/none"));
	}

	@Test
	void readRefSkipsCommentCaretEmptyLinesAndReturnsNullWhenUnmatched() throws Exception {
		// Exercises the continue-arm for '#', '^' and empty lines, then the no-match return.
		Files.writeString(gitDir.resolve("packed-refs"),
				"# pack-refs with: peeled fully-peeled sorted\n"
				+ "^1111111111111111111111111111111111111111\n"
				+ "\n"
				+ "2222222222222222222222222222222222222222 refs/heads/other\n");
		assertNull(GameManager.readRef(gitDir, "refs/heads/none"));
	}

	@Test
	void readRefReturnsHashFromPackedRefs() throws Exception {
		Files.writeString(gitDir.resolve("packed-refs"),
				"abcdef1234567890abcdef1234567890abcdef12 refs/heads/target\n");
		assertEquals("abcdef1234567890abcdef1234567890abcdef12",
				GameManager.readRef(gitDir, "refs/heads/target"));
	}

	// ---- public API (read-only over the real .git) ----

	@Test
	void getCommitHashReturnsNonNull() {
		assertNotNull(GameManager.getCommitHash());
	}
}

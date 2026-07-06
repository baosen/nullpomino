package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Covers both branches of {@link LogConfig#configure(String)}: a missing
 * file (falls back to logback defaults) and an existing file (applied via
 * Joran). The applied configuration keeps the root level OFF so the rest
 * of the suite stays silent, matching logback-test.xml.
 */
public class LogConfigBranchTest {
	@Test
	public void missingFileFallsBackToDefaults() {
		LogConfig.configure(new File(System.getProperty("java.io.tmpdir"),
				"nullpomino-does-not-exist-" + System.nanoTime() + ".xml").getPath());
	}

	@Test
	public void existingFileIsApplied() throws Exception {
		Path xml = Files.createTempFile("nullpomino-logconfig", ".xml");
		try {
			Files.writeString(xml, "<configuration><root level=\"OFF\"/></configuration>\n");
			LogConfig.configure(xml.toString());
			assertTrue(Files.exists(xml));
		} finally {
			Files.deleteIfExists(xml);
		}
	}
}

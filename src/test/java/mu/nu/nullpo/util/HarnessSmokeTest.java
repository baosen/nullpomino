package mu.nu.nullpo.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class HarnessSmokeTest {

	@Test
	void modeManagerLoadsEveryEntryInModeLst() throws Exception {
		Path modeLst = Paths.get("config/list/mode.lst");
		assertTrue(Files.exists(modeLst), "config/list/mode.lst must be on runfiles path");

		long expected = Files.lines(modeLst)
				.map(String::trim)
				.filter(s -> !s.isEmpty() && !s.startsWith("#"))
				.count();

		ModeManager modeManager = new ModeManager();
		try (BufferedReader r = new BufferedReader(new FileReader(modeLst.toFile()))) {
			modeManager.loadGameModes(r);
		}

		assertEquals(expected, modeManager.getSize(),
				"every non-comment entry in mode.lst must resolve via Class.forName");
	}
}

package nullpomino.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SwingToolUtilTest {

	@TempDir
	Path tempDir;

	@Test
	void readNonEmptyLinesStopsAtFirstBlankLine() throws IOException {
		Path list = tempDir.resolve("sample.lst");
		Files.write(list, ("first\nsecond\n\nignored\n").getBytes(StandardCharsets.UTF_8));

		Vector<String> lines = SwingToolUtil.readNonEmptyLines(list.toString());

		assertEquals(2, lines.size());
		assertEquals("first", lines.get(0));
		assertEquals("second", lines.get(1));
	}

	@Test
	void readNonEmptyLinesReturnsEmptyVectorForMissingFile() {
		Vector<String> lines = SwingToolUtil.readNonEmptyLines(tempDir.resolve("missing.lst").toString());

		assertTrue(lines.isEmpty());
	}

	@Test
	void shortClassNamesStripPackagePrefix() {
		Vector<String> names = new Vector<String>();
		names.add("net.omegaboshi.nullpomino.game.subsystem.randomizer.BagRandomizer");
		names.add("MemorylessRandomizer");

		Vector<String> shortNames = SwingToolUtil.shortClassNames(names);

		assertEquals("BagRandomizer", shortNames.get(0));
		assertEquals("MemorylessRandomizer", shortNames.get(1));
	}

	@Test
	void fileUrlUsesFileProtocol() throws Exception {
		URL url = SwingToolUtil.fileUrl("config/list/randomizer.lst");

		assertEquals("file", url.getProtocol());
		assertTrue(url.getPath().endsWith("/config/list/randomizer.lst"), url.toString());
	}

	@Test
	void fileUrlAcceptsAbsolutePaths() throws Exception {
		Path file = tempDir.resolve("sample.lst");
		URL url = SwingToolUtil.fileUrl(file.toString());

		assertEquals(file.toUri().toURL(), url);
	}
}

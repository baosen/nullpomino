package nullpomino.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Path;
import java.util.Vector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SwingToolUtilTest {

	@TempDir
	Path tempDir;

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
		URL url = SwingToolUtil.fileUrl("config/rule/Standard.rul");

		assertEquals("file", url.getProtocol());
		assertTrue(url.getPath().endsWith("/config/rule/Standard.rul"), url.toString());
	}

	@Test
	void fileUrlAcceptsAbsolutePaths() throws Exception {
		Path file = tempDir.resolve("sample.lst");
		URL url = SwingToolUtil.fileUrl(file.toString());

		assertEquals(file.toUri().toURL(), url);
	}
}

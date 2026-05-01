package nullpomino.tool.musiclisteditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimpleFileFilterTest {

	@TempDir
	Path tempDir;

	@Test
	void defaultConstructorAppliesEmptyExtensionAndDescription() {
		SimpleFileFilter filter = new SimpleFileFilter();

		assertEquals("", filter.getExtension());
		assertEquals("", filter.getDescription());
	}

	@Test
	void extensionConstructorLeavesDescriptionEmpty() {
		SimpleFileFilter filter = new SimpleFileFilter(".lst");

		assertEquals(".lst", filter.getExtension());
		assertEquals("", filter.getDescription());
	}

	@Test
	void extensionAndDescriptionConstructorAssignsBoth() {
		SimpleFileFilter filter = new SimpleFileFilter(".lst", "Music list files");

		assertEquals(".lst", filter.getExtension());
		assertEquals("Music list files", filter.getDescription());
	}

	@Test
	void acceptReturnsTrueForDirectoriesAndMatchingExtensions() throws IOException {
		Path dir = Files.createDirectory(tempDir.resolve("sub"));
		Path matching = Files.createFile(tempDir.resolve("song.lst"));
		Path other = Files.createFile(tempDir.resolve("song.txt"));
		SimpleFileFilter filter = new SimpleFileFilter(".lst", "Music list files");

		assertTrue(filter.accept(dir.toFile()));
		assertTrue(filter.accept(matching.toFile()));
		assertFalse(filter.accept(other.toFile()));
	}

	@Test
	void settersUpdateExtensionAndDescription() {
		SimpleFileFilter filter = new SimpleFileFilter();
		filter.setExtension(".rep");
		filter.setDescription("Replay files");

		assertEquals(".rep", filter.getExtension());
		assertEquals("Replay files", filter.getDescription());
	}
}

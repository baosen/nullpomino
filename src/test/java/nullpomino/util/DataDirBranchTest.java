package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * Covers every branch of {@link DataDir}: null/non-null setRoot, and
 * path() with empty root, non-empty root + relative name, and
 * non-empty root + absolute name.
 */
public class DataDirBranchTest {
	private static String readRoot() throws Exception {
		Field f = DataDir.class.getDeclaredField("root");
		f.setAccessible(true);
		return (String) f.get(null);
	}

	@Test
	public void coversSetRootAndPathBranches() throws Exception {
		String original = readRoot();
		try {
			DataDir.setRoot(null);
			assertEquals("", readRoot());
			assertEquals("rel/file.cfg", DataDir.path("rel/file.cfg"));

			DataDir.setRoot("/data/root");
			assertEquals("/data/root", readRoot());
			assertEquals("/data/root/rel/file.cfg", DataDir.path("rel/file.cfg"));

			String abs = new File("/abs/file.cfg").getAbsolutePath();
			assertEquals(abs, DataDir.path(abs));

			assertEquals("/data/root/x", DataDir.file("x").getPath());
		} finally {
			DataDir.setRoot(original);
		}
	}
}

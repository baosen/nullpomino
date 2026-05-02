package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import nullpomino.game.component.Statistics;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pins the headless slice of {@link StateReplaySelectSDL}: the
 * constructor's PAGE_HEIGHT and the localised null/empty error strings,
 * the .rep filter and locale-stable sort applied by getReplayFileList,
 * and the metadata extraction performed by setReplayRuleAndModeList.
 *
 * <p>The replay directory path is read from
 * {@link NullpoMinoSDL#propGlobal} under 'custom.replay.directory', so
 * tests snapshot and restore the static.
 */
class StateReplaySelectSDLTest {

	@TempDir
	Path tempDir;

	private CustomProperties originalPropGlobal;

	@BeforeEach
	void setUp() {
		originalPropGlobal = NullpoMinoSDL.propGlobal;
		NullpoMinoSDL.propGlobal = new CustomProperties();
		NullpoMinoSDL.propGlobal.setProperty("custom.replay.directory", tempDir.toString());
	}

	@AfterEach
	void tearDown() {
		NullpoMinoSDL.propGlobal = originalPropGlobal;
	}

	@Test
	void constructorPinsPageHeightAndLocalisedNullEmptyMessages() throws Exception {
		StateReplaySelectSDL state = new StateReplaySelectSDL();

		assertEquals(20, StateReplaySelectSDL.PAGE_HEIGHT);
		assertEquals(20, readInt(state, DummyMenuScrollStateSDL.class, "pageHeight"));
		assertEquals("REPLAY DIRECTORY NOT FOUND", readString(state, "nullError"));
		assertEquals("NO REPLAY FILE", readString(state, "emptyError"));
	}

	@Test
	void getReplayFileListReturnsNullWhenReplayDirIsMissing() throws Exception {
		// Point at a path that doesn't exist on disk.
		NullpoMinoSDL.propGlobal.setProperty("custom.replay.directory",
				tempDir.resolve("missing").toString());
		StateReplaySelectSDL state = new StateReplaySelectSDL();

		assertNull(invokeGetReplayFileList(state),
				"missing replay directory must surface as null so enter() "
						+ "can fall through to the nullError branch");
	}

	@Test
	void getReplayFileListSkipsFilesWithoutRepSuffix() throws Exception {
		Files.write(tempDir.resolve("alpha.rep"), new byte[0]);
		Files.write(tempDir.resolve("beta.rep"), new byte[0]);
		Files.write(tempDir.resolve("gamma.txt"), new byte[0]);
		Files.write(tempDir.resolve("delta.bak"), new byte[0]);

		StateReplaySelectSDL state = new StateReplaySelectSDL();
		String[] list = invokeGetReplayFileList(state);

		assertNotNull(list);
		// Sorted alphabetically on non-Windows; the test runs on Linux.
		boolean isWindows = System.getProperty("os.name").startsWith("Windows");
		if(!isWindows) {
			assertArrayEquals(new String[] {"alpha.rep", "beta.rep"}, list);
		} else {
			// On Windows, just confirm only .rep files come back.
			assertEquals(2, list.length);
		}
	}

	@Test
	void getReplayFileListReturnsEmptyArrayWhenDirHasNoRepFiles() throws Exception {
		Files.write(tempDir.resolve("readme.md"), "hello".getBytes(StandardCharsets.UTF_8));

		StateReplaySelectSDL state = new StateReplaySelectSDL();
		String[] list = invokeGetReplayFileList(state);

		assertNotNull(list, "directory exists -> non-null even if empty of .rep");
		assertEquals(0, list.length);
	}

	@Test
	void enterPopulatesListAndMaxCursorAndPerFileMetadata() throws Exception {
		writeReplayFile(tempDir.resolve("game1.rep"), "MARATHON", "Standard",
				/*score=*/12345, /*lines=*/40);
		writeReplayFile(tempDir.resolve("game2.rep"), "LINE RACE", "Classic",
				/*score=*/9999, /*lines=*/20);

		StateReplaySelectSDL state = new StateReplaySelectSDL();
		state.enter();

		String[] list = (String[]) readField(state, "list");
		assertNotNull(list);
		assertEquals(2, list.length);
		assertEquals(1, readInt(state, DummyMenuChooseStateSDL.class, "maxCursor"),
				"maxCursor = list.length - 1");

		String[] modeNames = (String[]) readField(state, "modenameList");
		String[] ruleNames = (String[]) readField(state, "rulenameList");
		Statistics[] stats = (Statistics[]) readField(state, "statsList");

		// File order may be either game1/game2 (Linux sort) or
		// filesystem-arbitrary (Windows). Look up by name to stay portable.
		int idx1 = indexOf(list, "game1.rep");
		int idx2 = indexOf(list, "game2.rep");

		assertEquals("MARATHON", modeNames[idx1]);
		assertEquals("Standard", ruleNames[idx1]);
		assertEquals(12345, stats[idx1].score);
		assertEquals(40, stats[idx1].lines);

		assertEquals("LINE RACE", modeNames[idx2]);
		assertEquals("Classic", ruleNames[idx2]);
		assertEquals(9999, stats[idx2].score);
		assertEquals(20, stats[idx2].lines);
	}

	@Test
	void setReplayRuleAndModeListUsesEmptyDefaultsWhenKeysAreMissing() throws Exception {
		// File has no name.mode / name.rule keys at all — populate must
		// fall back to "" rather than NPE.
		Files.write(tempDir.resolve("naked.rep"),
				"".getBytes(StandardCharsets.UTF_8));

		StateReplaySelectSDL state = new StateReplaySelectSDL();
		state.enter();

		String[] modeNames = (String[]) readField(state, "modenameList");
		String[] ruleNames = (String[]) readField(state, "rulenameList");
		Statistics[] stats = (Statistics[]) readField(state, "statsList");

		assertEquals("", modeNames[0]);
		assertEquals("", ruleNames[0]);
		assertNotNull(stats[0],
				"Statistics is always allocated even with empty properties");
	}

	@Test
	void setReplayRuleAndModeListIsNoOpWhenListIsNull() throws Exception {
		// No directory set up beyond the property pointing at tempDir; we
		// then null-out list ourselves. Reflective call must short-circuit
		// without NPE on the modenameList allocation.
		StateReplaySelectSDL state = new StateReplaySelectSDL();
		setField(state, "list", null);

		Method m = StateReplaySelectSDL.class.getDeclaredMethod("setReplayRuleAndModeList");
		m.setAccessible(true);
		m.invoke(state);

		// modenameList stays null because the early return runs before the
		// allocations. Pin that contract; render() guards on cursor < list.length.
		assertNull(readField(state, "modenameList"));
	}

	private static void writeReplayFile(Path file, String modeName, String ruleName,
			int score, int lines) throws IOException {
		// Write a Properties-format file that includes name.mode / name.rule
		// plus the Statistics player-0 keys consumed by Statistics.readProperty.
		StringBuilder sb = new StringBuilder();
		sb.append("name.mode=").append(modeName).append('\n');
		sb.append("name.rule=").append(ruleName).append('\n');
		// Statistics.readProperty looks up '<id>.statistics.<field>'.
		sb.append("0.statistics.score=").append(score).append('\n');
		sb.append("0.statistics.lines=").append(lines).append('\n');
		Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
	}

	private static int indexOf(String[] arr, String value) {
		for(int i = 0; i < arr.length; i++) {
			if(value.equals(arr[i])) return i;
		}
		throw new IllegalStateException("missing " + value + " in " + java.util.Arrays.toString(arr));
	}

	private static int readInt(Object instance, Class<?> declaringClass, String name) throws Exception {
		Field f = declaringClass.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(instance);
	}

	private static String readString(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return (String) f.get(instance);
	}

	private static Object readField(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.get(instance);
	}

	private static void setField(Object instance, String name, Object value) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.set(instance, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while(c != null) {
			try {
				return c.getDeclaredField(name);
			} catch(NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static String[] invokeGetReplayFileList(StateReplaySelectSDL state) throws Exception {
		Method m = StateReplaySelectSDL.class.getDeclaredMethod("getReplayFileList");
		m.setAccessible(true);
		return (String[]) m.invoke(state);
	}
}

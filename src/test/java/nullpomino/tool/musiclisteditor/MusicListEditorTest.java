package nullpomino.tool.musiclisteditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.util.CustomProperties;

/**
 * Tests MusicListEditor's non-rendering logic: UI-text fallback and
 * the property-based music list management.
 *
 * <p>MusicListEditor extends JFrame; its constructor calls pack() and
 * setVisible(true), both of which throw in headless. We allocate an
 * instance via {@code Unsafe.allocateInstance} to bypass the constructor
 * and populate only the fields each method touches.
 */
class MusicListEditorTest {

	private MusicListEditor editor;

	@BeforeEach
	void setUp() throws Exception {
		editor = allocateMusicListEditor();
	}

	@AfterEach
	void tearDown() {
		editor = null;
	}

	// ------------------------------------------------------------------
	// getUIText (private method, tested via reflection)
	// ------------------------------------------------------------------

	@Test
	void getUITextReturnsKeyWhenBothPropsEmpty() throws Exception {
		setPropLang(editor, new CustomProperties());
		setPropLangDefault(editor, new CustomProperties());

		String result = invokeGetUIText(editor, "Title_MusicListEditor");
		assertEquals("Title_MusicListEditor", result);
	}

	@Test
	void getUITextPrefersPropLang() throws Exception {
		CustomProperties lang = new CustomProperties();
		lang.setProperty("Title_MusicListEditor", "Localized");
		setPropLang(editor, lang);

		CustomProperties def = new CustomProperties();
		def.setProperty("Title_MusicListEditor", "Default");
		setPropLangDefault(editor, def);

		String result = invokeGetUIText(editor, "Title_MusicListEditor");
		assertEquals("Localized", result);
	}

	@Test
	void getUITextFallsBackToDefault() throws Exception {
		setPropLang(editor, new CustomProperties());

		CustomProperties def = new CustomProperties();
		def.setProperty("Title_MusicListEditor", "DefaultTitle");
		setPropLangDefault(editor, def);

		String result = invokeGetUIText(editor, "Title_MusicListEditor");
		assertEquals("DefaultTitle", result);
	}

	// ------------------------------------------------------------------
	// loadMusicList (property file operations)
	// ------------------------------------------------------------------

	@Test
	void loadMusicListInitialisesPropMusic() throws Exception {
		invokeLoadMusicList(editor);
		Object propMusic = getPropMusic(editor);
		assertNotNull(propMusic);
		assertTrue(propMusic instanceof CustomProperties);
	}

	// ------------------------------------------------------------------
	// Helper methods
	// ------------------------------------------------------------------

	private static MusicListEditor allocateMusicListEditor() throws Exception {
		Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		f.setAccessible(true);
		sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);

		MusicListEditor instance = (MusicListEditor) unsafe.allocateInstance(MusicListEditor.class);

		// Init the properties fields via reflection (they are private)
		setPropLang(instance, new CustomProperties());
		setPropLangDefault(instance, new CustomProperties());
		setPropMusicField(instance, new CustomProperties());

		return instance;
	}

	private static void setPropLang(MusicListEditor instance, CustomProperties props) throws Exception {
		Field f = MusicListEditor.class.getDeclaredField("propLang");
		f.setAccessible(true);
		f.set(instance, props);
	}

	private static void setPropLangDefault(MusicListEditor instance, CustomProperties props) throws Exception {
		Field f = MusicListEditor.class.getDeclaredField("propLangDefault");
		f.setAccessible(true);
		f.set(instance, props);
	}

	private static void setPropMusicField(MusicListEditor instance, CustomProperties props) throws Exception {
		Field f = MusicListEditor.class.getDeclaredField("propMusic");
		f.setAccessible(true);
		f.set(instance, props);
	}

	private static Object getPropMusic(MusicListEditor instance) throws Exception {
		Field f = MusicListEditor.class.getDeclaredField("propMusic");
		f.setAccessible(true);
		return f.get(instance);
	}

	private static String invokeGetUIText(MusicListEditor instance, String key) throws Exception {
		java.lang.reflect.Method m = MusicListEditor.class.getDeclaredMethod("getUIText", String.class);
		m.setAccessible(true);
		return (String) m.invoke(instance, key);
	}

	private static void invokeLoadMusicList(MusicListEditor instance) throws Exception {
		java.lang.reflect.Method m = MusicListEditor.class.getDeclaredMethod("loadMusicList");
		m.setAccessible(true);
		m.invoke(instance);
	}
}

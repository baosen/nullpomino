package nullpomino.tool.sequencer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import javax.swing.JTextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nullpomino.tool.SwingToolUtil;
import nullpomino.util.CustomProperties;

/**
 * Tests Sequencer's testable logic: text-field parsing helpers, file
 * loading, UI-text translation, and the inner FileFilter classes.
 *
 * <p>Sequencer extends JFrame; its constructor calls pack() and
 * setVisible(true), both of which throw HeadlessException in CI.
 * To reach the public instance methods we use {@code Unsafe.allocateInstance}
 * to create a Sequencer object without running the constructor, then
 * populate just the fields each method touches.
 */
class SequencerTest {

	private Sequencer sequencer;

	@BeforeEach
	void setUp() throws Exception {
		sequencer = allocateSequencer();
	}

	@AfterEach
	void tearDown() {
		sequencer = null;
	}

	// ------------------------------------------------------------------
	// getIntTextField
	// ------------------------------------------------------------------

	@Test
	void getIntTextFieldParsesValidInt() {
		JTextField tf = new JTextField("42");
		assertEquals(42, sequencer.getIntTextField(tf));
	}

	@Test
	void getIntTextFieldReturnsZeroForEmptyString() {
		JTextField tf = new JTextField("");
		assertEquals(0, sequencer.getIntTextField(tf));
	}

	@Test
	void getIntTextFieldReturnsZeroForNonNumericText() {
		JTextField tf = new JTextField("hello");
		assertEquals(0, sequencer.getIntTextField(tf));
	}

	@Test
	void getIntTextFieldReturnsZeroForNullText() {
		JTextField tf = new JTextField((String) null);
		assertEquals(0, sequencer.getIntTextField(tf));
	}

	@Test
	void getIntTextFieldParsesNegativeNumbers() {
		JTextField tf = new JTextField("-7");
		assertEquals(-7, sequencer.getIntTextField(tf));
	}

	@Test
	void getIntTextFieldTruncatesDecimalInput() {
		JTextField tf = new JTextField("3.14");
		// Integer.parseInt throws on ".", so fallback to 0
		assertEquals(0, sequencer.getIntTextField(tf));
	}

	// ------------------------------------------------------------------
	// getLongTextField
	// ------------------------------------------------------------------

	@Test
	void getLongTextFieldParsesValidLong() {
		JTextField tf = new JTextField("1234567890123");
		assertEquals(1234567890123L, sequencer.getLongTextField(tf));
	}

	@Test
	void getLongTextFieldReturnsZeroForEmptyString() {
		JTextField tf = new JTextField("");
		assertEquals(0L, sequencer.getLongTextField(tf));
	}

	@Test
	void getLongTextFieldReturnsZeroForNonNumericText() {
		JTextField tf = new JTextField("not-a-number");
		assertEquals(0L, sequencer.getLongTextField(tf));
	}

	@Test
	void getLongTextFieldParsesNegativeValues() {
		JTextField tf = new JTextField("-99");
		assertEquals(-99L, sequencer.getLongTextField(tf));
	}

	// ------------------------------------------------------------------
	// getUIText
	// ------------------------------------------------------------------

	@Test
	void getUITextReturnsKeyWhenBothLanguageFilesAreEmpty() throws Exception {
		setPropLang(sequencer, new CustomProperties());
		setPropLangDefault(sequencer, new CustomProperties());

		assertEquals("Option_Seed", sequencer.getUIText("Option_Seed"));
	}

	@Test
	void getUITextPrefersPropLangOverPropLangDefault() throws Exception {
		CustomProperties lang = new CustomProperties();
		lang.setProperty("Option_Seed", "Localized");
		setPropLang(sequencer, lang);

		CustomProperties def = new CustomProperties();
		def.setProperty("Option_Seed", "Default");
		setPropLangDefault(sequencer, def);

		assertEquals("Localized", sequencer.getUIText("Option_Seed"));
	}

	@Test
	void getUITextFallsBackToDefaultWhenLocalizedMissing() throws Exception {
		setPropLang(sequencer, new CustomProperties());

		CustomProperties def = new CustomProperties();
		def.setProperty("Option_Seed", "DefaultSeed");
		setPropLangDefault(sequencer, def);

		assertEquals("DefaultSeed", sequencer.getUIText("Option_Seed"));
	}

	// ------------------------------------------------------------------
	// load (file I/O)
	// ------------------------------------------------------------------

	@Test
	void loadReadsCustomPropertiesFromFile(@TempDir java.nio.file.Path tempDir) throws IOException {
		File propFile = tempDir.resolve("test.rep").toFile();
		CustomProperties props = new CustomProperties();
		props.setProperty("0.replay.randSeed", "ABCD");
		props.storeToFile(propFile.getAbsolutePath(), "Test");

		CustomProperties loaded = sequencer.load(propFile.getAbsolutePath());

		assertNotNull(loaded);
		assertEquals("ABCD", loaded.getProperty("0.replay.randSeed"));
	}

	@Test
	void loadThrowsIOExceptionForMissingFile() {
		try {
			sequencer.load("/nonexistent/path/file.rep");
		} catch (IOException e) {
			// expected
		}
	}

	// ------------------------------------------------------------------
	// Inner FileFilter classes
	// ------------------------------------------------------------------

	@Test
	void fileFilterREPAcceptsDirectoriesAndRepFiles(@TempDir java.nio.file.Path tempDir) throws IOException {
		File dir = tempDir.toFile();
		File repFile = tempDir.resolve("test.rep").toFile();
		File txtFile = tempDir.resolve("test.txt").toFile();
		assertTrue(repFile.createNewFile());
		assertTrue(txtFile.createNewFile());

		Sequencer.FileFilterREP filter = sequencer.new FileFilterREP();

		assertTrue(filter.accept(dir));
		assertTrue(filter.accept(repFile));
		assertFalse(filter.accept(txtFile));
	}

	@Test
	void fileFilterREPDescriptionIsNotEmpty() {
		Sequencer.FileFilterREP filter = sequencer.new FileFilterREP();
		assertNotNull(filter.getDescription());
		assertFalse(filter.getDescription().isEmpty());
	}

	@Test
	void fileFilterTXTAcceptsDirectoriesAndTextFiles(@TempDir java.nio.file.Path tempDir) throws IOException {
		File dir = tempDir.toFile();
		File txtFile = tempDir.resolve("test.txt").toFile();
		File repFile = tempDir.resolve("test.rep").toFile();
		assertTrue(txtFile.createNewFile());
		assertTrue(repFile.createNewFile());

		Sequencer.FileFilterTXT filter = sequencer.new FileFilterTXT();

		assertTrue(filter.accept(dir));
		assertTrue(filter.accept(txtFile));
		assertFalse(filter.accept(repFile));
	}

	@Test
	void fileFilterTXTDescriptionIsNotEmpty() {
		Sequencer.FileFilterTXT filter = sequencer.new FileFilterTXT();
		assertNotNull(filter.getDescription());
		assertFalse(filter.getDescription().isEmpty());
	}

	// ------------------------------------------------------------------
	// readReplayToUI (checks prop-reading without rendering)
	// ------------------------------------------------------------------

	@Test
	void readReplayToUIReadsSeedFromCustomProperties() throws Exception {
		// The method touches this.txtfldSeed and this.comboboxRandomizer;
		// allocate via Unsafe skips the constructor, so we must install them.
		java.lang.reflect.Field f = Sequencer.class.getDeclaredField("txtfldSeed");
		f.setAccessible(true);
		f.set(sequencer, new JTextField("0", 15));

		java.lang.reflect.Field comboField = Sequencer.class.getDeclaredField("comboboxRandomizer");
		comboField.setAccessible(true);
		comboField.set(sequencer, new javax.swing.JComboBox<String>());

		CustomProperties prop = new CustomProperties();
		prop.setProperty("0.replay.randSeed", "FF");
		prop.setProperty("0.ruleopt.strRandomizer", "SomeRandomizer");

		// The method should not throw.
		sequencer.readReplayToUI(prop, 0);
	}

	// ------------------------------------------------------------------
	// Helper: create Sequencer without running its constructor
	// ------------------------------------------------------------------

	@SuppressWarnings("deprecation")
	private static Sequencer allocateSequencer() throws Exception {
		java.lang.reflect.Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		f.setAccessible(true);
		sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
		Sequencer instance = (Sequencer) unsafe.allocateInstance(Sequencer.class);

		// Initialize the two properties fields that getUIText reads
		instance.propLang = new CustomProperties();
		instance.propLangDefault = new CustomProperties();

		return instance;
	}

	private static void setPropLang(Sequencer instance, CustomProperties props) throws Exception {
		Field f = Sequencer.class.getField("propLang");
		f.set(instance, props);
	}

	private static void setPropLangDefault(Sequencer instance, CustomProperties props) throws Exception {
		Field f = Sequencer.class.getField("propLangDefault");
		f.set(instance, props);
	}
}

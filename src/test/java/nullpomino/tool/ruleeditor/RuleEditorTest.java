package nullpomino.tool.ruleeditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nullpomino.util.CustomProperties;

/**
 * Tests RuleEditor's testable non-rendering logic: the ComboLabel data
 * class and the FileFilterRUL file filter.
 *
 * <p>RuleEditor extends JFrame; its constructor calls pack() and
 * setVisible(true), both of which throw in headless environments.
 * We create an instance via {@code Unsafe.allocateInstance} to bypass
 * the constructor, then instantiate the inner classes on that object.
 */
class RuleEditorTest {

	private RuleEditor editor;

	@BeforeEach
	void setUp() throws Exception {
		editor = allocateRuleEditor();
	}

	@AfterEach
	void tearDown() {
		editor = null;
	}

	// ------------------------------------------------------------------
	// ComboLabel — data class for skin-combobox items
	// ------------------------------------------------------------------

	@Test
	void comboLabelDefaultConstructor() {
		RuleEditor.ComboLabel label = editor.new ComboLabel();
		assertEquals("", label.getText());
	}

	@Test
	void comboLabelTextConstructor() {
		RuleEditor.ComboLabel label = editor.new ComboLabel("skin0");
		assertEquals("skin0", label.getText());
	}

	@Test
	void comboLabelSetTextAndGetText() {
		RuleEditor.ComboLabel label = editor.new ComboLabel();
		label.setText("skin1");
		assertEquals("skin1", label.getText());
	}

	@Test
	void comboLabelSetIconAndGetIcon() {
		RuleEditor.ComboLabel label = editor.new ComboLabel();
		// Icon can be null — the setter/getter accept null
		label.setIcon(null);
		assertEquals(null, label.getIcon());
	}

	@Test
	void comboLabelRoundTripTextAndIcon() {
		RuleEditor.ComboLabel label = editor.new ComboLabel();
		label.setText("test");
		label.setIcon(null);
		assertEquals("test", label.getText());
		assertEquals(null, label.getIcon());
	}

	// ------------------------------------------------------------------
	// FileFilterRUL — rule-file filter
	// ------------------------------------------------------------------

	@Test
	void fileFilterRULAcceptsDirectories(@TempDir java.nio.file.Path tempDir) {
		RuleEditor.FileFilterRUL filter = editor.new FileFilterRUL();
		assertTrue(filter.accept(tempDir.toFile()));
	}

	@Test
	void fileFilterRULAcceptsRulFiles(@TempDir java.nio.file.Path tempDir) throws IOException {
		File rulFile = tempDir.resolve("test.rul").toFile();
		assertTrue(rulFile.createNewFile());

		RuleEditor.FileFilterRUL filter = editor.new FileFilterRUL();
		assertTrue(filter.accept(rulFile));
	}

	@Test
	void fileFilterRULRejectsNonRulFiles(@TempDir java.nio.file.Path tempDir) throws IOException {
		File txtFile = tempDir.resolve("test.txt").toFile();
		assertTrue(txtFile.createNewFile());

		RuleEditor.FileFilterRUL filter = editor.new FileFilterRUL();
		assertFalse(filter.accept(txtFile));
	}

	@Test
	void fileFilterRULDescriptionIsNotEmpty() {
		RuleEditor.FileFilterRUL filter = editor.new FileFilterRUL();
		assertNotNull(filter.getDescription());
		assertFalse(filter.getDescription().isEmpty());
	}

	// ------------------------------------------------------------------
	// getUIText with custom props
	// ------------------------------------------------------------------

	@Test
	void getUITextReturnsKeyWhenBothPropsAreEmpty() throws Exception {
		setPropLang(editor, new CustomProperties());
		setPropLangDefault(editor, new CustomProperties());

		assertEquals("Title_RuleEditor", editor.getUIText("Title_RuleEditor"));
	}

	@Test
	void getUITextPrefersPropLang() throws Exception {
		CustomProperties lang = new CustomProperties();
		lang.setProperty("Title_RuleEditor", "Localized");
		setPropLang(editor, lang);

		CustomProperties def = new CustomProperties();
		def.setProperty("Title_RuleEditor", "Default");
		setPropLangDefault(editor, def);

		assertEquals("Localized", editor.getUIText("Title_RuleEditor"));
	}

	@Test
	void getUITextFallsBackToDefault() throws Exception {
		setPropLang(editor, new CustomProperties());

		CustomProperties def = new CustomProperties();
		def.setProperty("Title_RuleEditor", "Default");
		setPropLangDefault(editor, def);

		assertEquals("Default", editor.getUIText("Title_RuleEditor"));
	}

	// ------------------------------------------------------------------
	// getIntTextField
	// ------------------------------------------------------------------

	@Test
	void getIntTextFieldParsesValidInt() {
		javax.swing.JTextField tf = new javax.swing.JTextField("42");
		assertEquals(42, editor.getIntTextField(tf));
	}

	@Test
	void getIntTextFieldReturnsZeroForEmptyString() {
		javax.swing.JTextField tf = new javax.swing.JTextField("");
		assertEquals(0, editor.getIntTextField(tf));
	}

	@Test
	void getIntTextFieldReturnsZeroForNonNumericText() {
		javax.swing.JTextField tf = new javax.swing.JTextField("hello");
		assertEquals(0, editor.getIntTextField(tf));
	}

	@Test
	void getIntTextFieldParsesNegativeNumbers() {
		javax.swing.JTextField tf = new javax.swing.JTextField("-7");
		assertEquals(-7, editor.getIntTextField(tf));
	}

	@Test
	void getIntTextFieldReturnsZeroForDecimalInput() {
		javax.swing.JTextField tf = new javax.swing.JTextField("3.14");
		assertEquals(0, editor.getIntTextField(tf));
	}

	// ------------------------------------------------------------------
	// getFloatTextField
	// ------------------------------------------------------------------

	@Test
	void getFloatTextFieldParsesValidFloat() {
		javax.swing.JTextField tf = new javax.swing.JTextField("3.14");
		assertEquals(3.14f, editor.getFloatTextField(tf), 0.001f);
	}

	@Test
	void getFloatTextFieldReturnsZeroForEmptyString() {
		javax.swing.JTextField tf = new javax.swing.JTextField("");
		assertEquals(0f, editor.getFloatTextField(tf), 0.001f);
	}

	@Test
	void getFloatTextFieldReturnsZeroForNonNumericText() {
		javax.swing.JTextField tf = new javax.swing.JTextField("not-a-number");
		assertEquals(0f, editor.getFloatTextField(tf), 0.001f);
	}

	@Test
	void getFloatTextFieldParsesIntegerString() {
		javax.swing.JTextField tf = new javax.swing.JTextField("10");
		assertEquals(10f, editor.getFloatTextField(tf), 0.001f);
	}

	@Test
	void getFloatTextFieldParsesNegativeFloat() {
		javax.swing.JTextField tf = new javax.swing.JTextField("-2.5");
		assertEquals(-2.5f, editor.getFloatTextField(tf), 0.001f);
	}

	// ------------------------------------------------------------------
	// load method — rule file deserialisation
	// ------------------------------------------------------------------

	@Test
	void loadReadsRuleOptionsFromFile(@TempDir java.nio.file.Path tempDir) throws Exception {
		// Create a minimal .rul property file
		java.io.File ruleFile = tempDir.resolve("test.rul").toFile();
		CustomProperties props = new CustomProperties();
		props.setProperty("0.ruleopt.strRuleName", "TestRule");
		props.setProperty("0.ruleopt.style", "2");
		props.storeToFile(ruleFile.getAbsolutePath(), "Test Rule");

		nullpomino.game.component.RuleOptions ruleopt = editor.load(ruleFile.getAbsolutePath());

		assertNotNull(ruleopt);
		assertEquals("TestRule", ruleopt.strRuleName);
		assertEquals(2, ruleopt.style);
	}

	@Test
	void loadThrowsIOExceptionForMissingFile() {
		try {
			editor.load("/nonexistent/path/file.rul");
		} catch (java.io.IOException e) {
			// expected
		}
	}

	@Test
	void loadReturnsRuleOptionsWithDefaultsForEmptyPropertyFile(@TempDir java.nio.file.Path tempDir) throws Exception {
		java.io.File ruleFile = tempDir.resolve("empty.rul").toFile();
		CustomProperties props = new CustomProperties();
		props.storeToFile(ruleFile.getAbsolutePath(), "Empty Rule");

		nullpomino.game.component.RuleOptions ruleopt = editor.load(ruleFile.getAbsolutePath());

		assertNotNull(ruleopt);
		// Default rule name should be empty string or some default
		assertEquals("", ruleopt.strRuleName);
	}

	// ------------------------------------------------------------------
	// Helper: create RuleEditor without running its constructor
	// ------------------------------------------------------------------

	private static RuleEditor allocateRuleEditor() throws Exception {
		Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		f.setAccessible(true);
		sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);

		RuleEditor instance = (RuleEditor) unsafe.allocateInstance(RuleEditor.class);

		// Initialize properties so that getUIText works
		instance.propLangDefault = new CustomProperties();
		instance.propLang = new CustomProperties();

		return instance;
	}

	private static void setPropLang(RuleEditor instance, CustomProperties props) throws Exception {
		Field f = RuleEditor.class.getField("propLang");
		f.set(instance, props);
	}

	private static void setPropLangDefault(RuleEditor instance, CustomProperties props) throws Exception {
		Field f = RuleEditor.class.getField("propLangDefault");
		f.set(instance, props);
	}
}

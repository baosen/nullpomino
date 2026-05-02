package nullpomino.tool.airankstool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins {@link AIRanksTool#getUIText}'s prop-lang fallback ladder.
 * The method first checks the localized propLang, falling back to
 * propLangDefault on miss. propLangDefault.getProperty(str, str)
 * surfaces the lookup key itself when both files miss, so a missing
 * translation shows up as the raw key in the UI rather than as a
 * blank label.
 */
class AIRanksToolTest {

	private CustomProperties originalLang;
	private CustomProperties originalLangDefault;

	@BeforeEach
	void setUp() {
		originalLang = AIRanksTool.propLang;
		originalLangDefault = AIRanksTool.propLangDefault;
		AIRanksTool.propLang = new CustomProperties();
		AIRanksTool.propLangDefault = new CustomProperties();
	}

	@AfterEach
	void tearDown() {
		AIRanksTool.propLang = originalLang;
		AIRanksTool.propLangDefault = originalLangDefault;
	}

	@Test
	void getUITextPrefersPropLangOverDefaultWhenBothPresent() {
		AIRanksTool.propLang.setProperty("Result_Title", "Localized");
		AIRanksTool.propLangDefault.setProperty("Result_Title", "Default");

		assertEquals("Localized", AIRanksTool.getUIText("Result_Title"));
	}

	@Test
	void getUITextFallsBackToDefaultWhenLocalizedIsMissing() {
		AIRanksTool.propLangDefault.setProperty("Result_Title", "Default");
		// propLang has no entry for the key.

		assertEquals("Default", AIRanksTool.getUIText("Result_Title"));
	}

	@Test
	void getUITextReturnsTheKeyVerbatimWhenBothFilesMiss() {
		// propLang and propLangDefault both empty -> the
		// getProperty(str, str) overload uses str as its own default,
		// surfacing the lookup key as the UI text.
		assertEquals("Missing_Key", AIRanksTool.getUIText("Missing_Key"));
	}

	@Test
	void getUITextWithEmptyDefaultStillFallsBackToTheKey() {
		// Even if propLangDefault has an unrelated entry, a missing
		// key-of-interest still falls through to str-as-default.
		AIRanksTool.propLangDefault.setProperty("Some_Other_Key", "value");

		assertEquals("Result_Progress_Note",
				AIRanksTool.getUIText("Result_Progress_Note"));
	}
}

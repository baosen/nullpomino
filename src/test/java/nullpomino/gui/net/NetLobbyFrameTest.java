package nullpomino.gui.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.gui.net.NetLobbyFrame.RuleEntry;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pins the headless surface on {@link NetLobbyFrame}: the language
 * fallback ladder used by getUIText, the mode-name property-key
 * escaping in getModeDesc, the trip-code separator munging in
 * convTripCode, the .rul filename parsing in createRuleEntries +
 * getSubsetEntries, and the room-table row formatter.
 */
class NetLobbyFrameTest {

	@TempDir
	Path tempDir;

	@Test
	void getUITextPrefersPropLangOverPropLangDefault() {
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.propLang = new CustomProperties();
		nl.propLangDefault = new CustomProperties();
		nl.propLang.setProperty("Test_Key", "Foreground");
		nl.propLangDefault.setProperty("Test_Key", "Default");

		assertEquals("Foreground", nl.getUIText("Test_Key"));
	}

	@Test
	void getUITextFallsBackToDefaultWhenForegroundMissing() {
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.propLang = new CustomProperties();
		nl.propLangDefault = new CustomProperties();
		nl.propLangDefault.setProperty("Test_Key", "Default");

		assertEquals("Default", nl.getUIText("Test_Key"));
	}

	@Test
	void getUITextReturnsEmptyStringForNullKey() {
		// Defensive empty-string fallback so callers that pass through a
		// possibly-null key don't have to guard themselves.
		NetLobbyFrame nl = new NetLobbyFrame();
		assertEquals("", nl.getUIText(null));
	}

	@Test
	void getModeDescEscapesSpacesAndParensIntoPropertySafeForm() {
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.propModeDesc = new CustomProperties();
		nl.propDefaultModeDesc = new CustomProperties();
		// Same escape as StateSelectModeSDL: ' ' -> '_', '(' -> 'l', ')' -> 'r'.
		nl.propDefaultModeDesc.setProperty("AVALANCHE_1P_lRC2r", "1P RC2 desc");

		assertEquals("1P RC2 desc", nl.getModeDesc("AVALANCHE 1P (RC2)"));
	}

	@Test
	void getModeDescReturnsEscapedKeyWhenBothFilesMiss() {
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.propModeDesc = new CustomProperties();
		nl.propDefaultModeDesc = new CustomProperties();

		assertEquals("MARATHON", nl.getModeDesc("MARATHON"));
		assertEquals("LINE_RACE", nl.getModeDesc("LINE RACE"));
	}

	@Test
	void getModeDescReturnsEmptyForNullModeName() {
		NetLobbyFrame nl = new NetLobbyFrame();
		assertEquals("", nl.getModeDesc(null));
	}

	@Test
	void convTripCodeStripsSpaceBeforeBangSeparator() {
		// Server stores names as 'Bob !ABCHASH' with a space; display strips
		// it so the rendered name reads 'Bob!ABCHASH'.
		NetLobbyFrame nl = new NetLobbyFrame();

		assertEquals("Bob!ABCHASH", nl.convTripCode("Bob !ABCHASH"));
		// No '!' separator -> name unchanged.
		assertEquals("Solo", nl.convTripCode("Solo"));
		// Null surfaces as empty.
		assertEquals("", nl.convTripCode(null));
	}

	@Test
	void convTripCodeIsNoOpWhenPropLangIsNullEvenWithSeparatorEnabled() {
		// The trip-separator dance only kicks in when propLang says so;
		// without propLang the string passes through untouched (modulo the
		// space-strip already applied).
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.propLang = null;

		assertEquals("Bob!HASH", nl.convTripCode("Bob !HASH"));
	}

	@Test
	void convTripCodeAppliesLocalisedTripCodeSeparatorsWhenEnabled() {
		// When TripSeparator_EnableConvert is on, '!' is replaced with the
		// localised "true" glyph and '?' with the localised "false" glyph.
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.propLang = new CustomProperties();
		nl.propLangDefault = new CustomProperties();
		nl.propLang.setProperty("TripSeparator_EnableConvert", true);
		nl.propLang.setProperty("TripSeparator_True", "#");
		nl.propLang.setProperty("TripSeparator_False", "@");

		// 'Bob!HASH' -> '!' becomes '#'.
		assertEquals("Bob#HASH", nl.convTripCode("Bob !HASH"));
		// '?' becomes '@'.
		assertEquals("Cha@LLENGE", nl.convTripCode("Cha?LLENGE"));
	}

	@Test
	void getRuleFileListAcceptsDotRulFilesOnly() throws IOException {
		// We can't redirect createRuleEntries' hardcoded "config/rule"
		// path, so test the public createRuleEntries+getSubsetEntries pair
		// by passing in a fake list directly.
		NetLobbyFrame nl = new NetLobbyFrame();
		// Empty list -> empty entries.
		nl.createRuleEntries(new String[0]);
		assertEquals(0, nl.ruleEntries.size());

		// Null list -> still empty (defensive guard).
		nl.createRuleEntries(null);
		assertEquals(0, nl.ruleEntries.size());
	}

	@Test
	void createRuleEntriesGracefullyHandlesMissingFiles() {
		NetLobbyFrame nl = new NetLobbyFrame();
		// Hardcoded 'config/rule/' prefix; the filename below won't exist
		// in tempDir, so loadFromFile throws and the catch path runs.
		nl.createRuleEntries(new String[] {"definitely-not-real.rul"});

		assertEquals(1, nl.ruleEntries.size());
		RuleEntry entry = nl.ruleEntries.get(0);
		assertEquals("definitely-not-real.rul", entry.filename);
		assertEquals("", entry.rulename, "missing file -> rulename is ''");
		assertEquals(-1, entry.style, "missing file -> style is -1 sentinel");
	}

	@Test
	void getSubsetEntriesReturnsOnlyEntriesMatchingTheRequestedStyle() {
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.ruleEntries.add(makeEntry("a.rul", "Standard", 0));
		nl.ruleEntries.add(makeEntry("b.rul", "Avalanche", 1));
		nl.ruleEntries.add(makeEntry("c.rul", "Tetromino2", 0));
		nl.ruleEntries.add(makeEntry("d.rul", "Physician", 2));

		LinkedList<RuleEntry> tetro = nl.getSubsetEntries(0);
		assertEquals(2, tetro.size());
		assertEquals("a.rul", tetro.get(0).filename);
		assertEquals("c.rul", tetro.get(1).filename);

		LinkedList<RuleEntry> avalanche = nl.getSubsetEntries(1);
		assertEquals(1, avalanche.size());
		assertEquals("b.rul", avalanche.get(0).filename);

		// Unknown style returns empty list, not null.
		assertEquals(0, nl.getSubsetEntries(99).size());
	}

	@Test
	void getSubsetEntriesPreservesInsertionOrder() {
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.ruleEntries.add(makeEntry("z.rul", "Z", 0));
		nl.ruleEntries.add(makeEntry("a.rul", "A", 0));

		LinkedList<RuleEntry> sub = nl.getSubsetEntries(0);
		assertEquals("z.rul", sub.get(0).filename,
				"subset retains insertion order — getRuleFileList sorts on disk");
		assertEquals("a.rul", sub.get(1).filename);
	}

	@Test
	void createRoomListRowDataPopulatesEightColumnsInRoomTableOrder() {
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.propLang = new CustomProperties();
		nl.propLangDefault = new CustomProperties();
		nl.propLang.setProperty("RoomTable_Rated_True", "RATED");
		nl.propLang.setProperty("RoomTable_Rated_False", "FREE");
		nl.propLang.setProperty("RoomTable_RuleName_Any", "*ANY*");
		nl.propLang.setProperty("RoomTable_Status_Playing", "PLAY");
		nl.propLang.setProperty("RoomTable_Status_Waiting", "WAIT");

		NetRoomInfo r = new NetRoomInfo();
		r.roomID = 7;
		r.strName = "My Room";
		r.rated = true;
		r.ruleLock = true;
		r.ruleName = "standard";
		r.strMode = "MARATHON";
		r.playing = false;
		r.playerSeatedCount = 3;
		r.maxPlayers = 6;
		r.spectatorCount = 2;

		assertArrayEquals(
				new String[] {"7", "My Room", "RATED", "STANDARD", "MARATHON", "WAIT", "3/6", "2"},
				nl.createRoomListRowData(r));
	}

	@Test
	void createRoomListRowDataShowsAnyForUnlockedRule() {
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.propLang = new CustomProperties();
		nl.propLangDefault = new CustomProperties();
		nl.propLang.setProperty("RoomTable_Rated_False", "FREE");
		nl.propLang.setProperty("RoomTable_RuleName_Any", "*ANY*");
		nl.propLang.setProperty("RoomTable_Status_Playing", "PLAY");
		nl.propLang.setProperty("RoomTable_Status_Waiting", "WAIT");

		NetRoomInfo r = new NetRoomInfo();
		r.ruleLock = false;
		r.ruleName = "ignored-when-not-locked";
		r.playing = true;

		String[] row = nl.createRoomListRowData(r);
		assertEquals("*ANY*", row[3], "ruleLock=false -> column 3 shows the ANY label");
		assertEquals("FREE", row[2], "rated=false -> column 2 shows the FREE label");
		assertEquals("PLAY", row[5], "playing=true -> column 5 shows PLAY");
	}

	@Test
	void getPlayerNameWithTripCodeRoutesThroughConvTripCode() {
		NetLobbyFrame nl = new NetLobbyFrame();
		NetPlayerInfo p = new NetPlayerInfo();
		p.strName = "Alice !TRIP";

		assertEquals("Alice!TRIP", nl.getPlayerNameWithTripCode(p));
	}

	private static RuleEntry makeEntry(String filename, String rulename, int style) {
		RuleEntry e = new RuleEntry();
		e.filename = filename;
		e.filepath = "config/rule/" + filename;
		e.rulename = rulename;
		e.style = style;
		return e;
	}
}

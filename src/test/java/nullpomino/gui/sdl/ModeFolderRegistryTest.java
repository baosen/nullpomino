package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nullpomino.game.subsystem.mode.GameMode;
import nullpomino.gui.sdl.ModeFolderRegistry.Folder;
import nullpomino.util.ModeManager;
import nullpomino.util.ModeRegistry;

import org.junit.jupiter.api.Test;

/**
 * Pins the folder layout shown on the Mode-Select screen against the
 * registered mode roster. Catches typos in {@link ModeFolderRegistry} and
 * silent renames in mode classes that would otherwise produce unreachable
 * folder entries.
 */
class ModeFolderRegistryTest {

	@Test
	void topLevelOrderMatchesSpec() {
		assertEquals(List.of(
				"MARATHON",
				"LINE RACE",
				"DIG RACE",
				"DIG CHALLENGE",
				"ULTRA",
				"GRADE MANIA 3",
				"SPEED MANIA 2",
				"PRACTICE",
				"GEM MANIA",
				"VS-BATTLE"),
				ModeFolderRegistry.TOP_LEVEL,
				"top-level mode order is part of the visible UI");
	}

	@Test
	void folderOrderMatchesSpec() {
		List<String> folderNames = ModeFolderRegistry.FOLDERS.stream().map(Folder::name).toList();
		assertEquals(List.of(
				"STANDARD MODES",
				"RACE MODES",
				"MANIA MODES",
				"RETRO MODES",
				"MISC MODES",
				"AVALANCHE",
				"PHYSICIAN",
				"SPF"),
				folderNames,
				"folder order is part of the visible UI");
	}

	@Test
	void everyReferencedNameResolvesToARegisteredMode() {
		ModeManager mm = new ModeManager();
		mm.loadGameModes(ModeRegistry.all());

		for(String name : ModeFolderRegistry.TOP_LEVEL) {
			assertNotNull(mm.getMode(name),
					"top-level entry " + name + " has no matching registered mode");
		}
		for(Folder folder : ModeFolderRegistry.FOLDERS) {
			for(String name : folder.modes()) {
				assertNotNull(mm.getMode(name),
						"folder " + folder.name() + " entry " + name
								+ " has no matching registered mode");
			}
		}
	}

	@Test
	void noFolderReferencesNetplayMode() {
		// StateSelectModeSDL.prepareModeList filters by getModeNames(false);
		// folder data must only reference non-netplay modes or those entries
		// would be unreachable through the menu.
		ModeManager mm = new ModeManager();
		mm.loadGameModes(ModeRegistry.all());

		for(String name : ModeFolderRegistry.TOP_LEVEL) {
			GameMode mode = mm.getMode(name);
			assertNotNull(mode, name);
			assertFalse(mode.isNetplayMode(),
					"top-level entry " + name + " is a netplay mode");
		}
		for(Folder folder : ModeFolderRegistry.FOLDERS) {
			for(String name : folder.modes()) {
				GameMode mode = mm.getMode(name);
				assertNotNull(mode, name);
				assertFalse(mode.isNetplayMode(),
						"folder " + folder.name() + " entry " + name + " is a netplay mode");
			}
		}
	}

	@Test
	void noDuplicateFolderNamesAndNoEmptyFolders() {
		Set<String> seen = new HashSet<>();
		for(Folder folder : ModeFolderRegistry.FOLDERS) {
			assertTrue(seen.add(folder.name()),
					"duplicate folder name: " + folder.name());
			assertFalse(folder.modes().isEmpty(),
					"folder " + folder.name() + " has no entries");
		}
	}
}

package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import nullpomino.game.mode.GameMode;

import org.junit.jupiter.api.Test;

/**
 * Covers the remaining branch in ModeManager.getAllModeNames()
 * when a null mode is present in the list.
 */
class ModeManagerBranchTest {

	@Test
	@SuppressWarnings("unchecked")
	void getAllModeNamesHandlesNullMode() throws Exception {
		ModeManager mm = new ModeManager();
		// A null mode lands in the list when a mode class fails to load; inject one
		// directly so the (mode == null) ternary arm in getAllModeNames is exercised.
		Field modesField = ModeManager.class.getDeclaredField("modes");
		modesField.setAccessible(true);
		List<GameMode> modes = (List<GameMode>) modesField.get(mm);
		modes.add(null);

		String[] names = mm.getAllModeNames();
		assertNotNull(names);
		assertEquals("*INVALID MODE*", names[0]);
	}

	@Test
	void getNumberOfModesReturnsCount() {
		ModeManager mm = new ModeManager();
		assertTrue(mm.getNumberOfModes(false) >= 0);
	}
}

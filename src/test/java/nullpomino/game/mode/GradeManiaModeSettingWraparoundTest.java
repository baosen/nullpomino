// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Exercises the {@link GradeManiaMode#onSetting} decision branches that the
 * existing GradeManiaMode tests leave half-covered. Unlike the other GRADE
 * MANIA variants, {@code GradeManiaMode} drives its numeric configuration
 * through {@code updateMenu} (menu items) rather than a {@code switch}, so the
 * coverage gaps in {@code onSetting} are the short-circuit complements of the
 * BUTTON_F / BUTTON_A guards and the section-time toggle.
 *
 * <p>Covered branch complements (line numbers in GradeManiaMode.java):
 * <ul>
 *   <li>L316 {@code isPush(BUTTON_F) && (menuTime >= 5)}: F pressed but menuTime &lt; 5
 *       (the {@code menuTime >= 5} false arm).</li>
 *   <li>L318 {@code isShowBestSectionTime = !isShowBestSectionTime}: the true-&gt;false toggle.</li>
 *   <li>L322 {@code isPush(BUTTON_A) && (menuTime >= 5)}: A pressed but menuTime &lt; 5
 *       (the {@code menuTime >= 5} false arm).</li>
 * </ul>
 */
class GradeManiaModeSettingWraparoundTest {

	private static GameEngine freshEngine(GradeManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	// -----------------------------------------------------------------
	// L316: BUTTON_F held but menuTime < 5 -> short-circuit false arm.
	// The toggle must NOT flip and "change" SE must not play.
	// -----------------------------------------------------------------

	@Test
	void fButtonBelowMenuTimeDoesNotToggleSectionTime() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "isShowBestSectionTime", false);
		setInt(mode, "menuTime", 0); // < 5 -> second operand of L316 is false
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1; // isPush == true

		mode.onSetting(engine, 0);

		assertFalse(readBool(mode, "isShowBestSectionTime"),
				"F with menuTime<5 must not toggle the section-time view");
	}

	// -----------------------------------------------------------------
	// L316 true / L318: BUTTON_F with menuTime >= 5 toggles true -> false.
	// The existing RemainingCoverage test only covers false -> true, so this
	// adds the second outcome of the boolean negation on L318.
	// -----------------------------------------------------------------

	@Test
	void fButtonTogglesSectionTimeBackToFalse() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "isShowBestSectionTime", true); // start true so toggle -> false
		setInt(mode, "menuTime", 5);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;

		mode.onSetting(engine, 0);

		assertFalse(readBool(mode, "isShowBestSectionTime"),
				"F with menuTime>=5 must toggle true -> false");
	}

	// -----------------------------------------------------------------
	// L322: BUTTON_A held but menuTime < 5 -> short-circuit false arm.
	// onSetting must keep running (return true), not confirm/exit.
	// -----------------------------------------------------------------

	@Test
	void aButtonBelowMenuTimeDoesNotConfirm() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuTime", 0); // < 5 -> second operand of L322 false
		setInt(mode, "sectionscomp", 7); // would be reset to 0 if confirm ran
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		boolean keepRunning = mode.onSetting(engine, 0);

		assertTrue(keepRunning, "A with menuTime<5 must not confirm the menu");
		assertEquals(7, readInt(mode, "sectionscomp"),
				"confirm side-effects must not run when menuTime<5");
	}

	// -----------------------------------------------------------------
	// L322 true: BUTTON_A with menuTime >= 5 confirms and exits (returns false).
	// This pins the true arm of L322 and its side effects.
	// -----------------------------------------------------------------

	@Test
	void aButtonAtMenuTimeConfirmsAndResetsState() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		// Avoid persisting to the gitignored mode-config file from the call site.
		engine.owner.modeConfig = new CustomProperties();
		mode.playerInit(engine, 0);

		setInt(mode, "menuTime", 5);
		setInt(mode, "sectionscomp", 7);
		setBool(mode, "isShowBestSectionTime", true);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		boolean keepRunning = mode.onSetting(engine, 0);

		assertFalse(keepRunning, "A with menuTime>=5 must confirm and leave the menu");
		assertEquals(0, readInt(mode, "sectionscomp"));
		assertFalse(readBool(mode, "isShowBestSectionTime"));
	}

	// --- reflection helpers ---

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static boolean readBool(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getBoolean(obj);
	}

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}

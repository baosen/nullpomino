package nullpomino.game.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nullpomino.util.CustomProperties;

class EventReceiverDelegationTest {

	@TempDir
	Path tempDir;

	@Test
	void drawMenuFontDelegationOverloadsRouteToEmptyStub() {
		EventReceiver er = new EventReceiver();
		er.drawMenuFont(null, 0, 0, 0, "x");
		er.drawMenuFont(null, 0, 0, 0, "x", 2.0f);
		er.drawMenuFont(null, 0, 0, 0, "x", EventReceiver.COLOR_RED);
		er.drawMenuFont(null, 0, 0, 0, "x", false, EventReceiver.COLOR_BLUE, EventReceiver.COLOR_GREEN);
		er.drawMenuFont(null, 0, 0, 0, "x", true, EventReceiver.COLOR_BLUE, EventReceiver.COLOR_GREEN);
		er.drawMenuFont(null, 0, 0, 0, "x", false);
		er.drawMenuFont(null, 0, 0, 0, "x", true);
		er.drawMenuFont(null, 0, 0, 0, "x", false, 1.5f);
		er.drawMenuFont(null, 0, 0, 0, "x", true, 1.5f);
		// playerID == 1 takes the COLOR_BLUE branch in the (flag, scale) variant.
		er.drawMenuFont(null, 1, 0, 0, "x", true, 1.5f);
	}

	@Test
	void drawTTFMenuFontDelegationOverloadsRouteToEmptyStub() {
		EventReceiver er = new EventReceiver();
		er.drawTTFMenuFont(null, 0, 0, 0, "x");
		er.drawTTFMenuFont(null, 0, 0, 0, "x", false, EventReceiver.COLOR_RED, EventReceiver.COLOR_BLUE);
		er.drawTTFMenuFont(null, 0, 0, 0, "x", true, EventReceiver.COLOR_RED, EventReceiver.COLOR_BLUE);
		er.drawTTFMenuFont(null, 0, 0, 0, "x", false);
		er.drawTTFMenuFont(null, 0, 0, 0, "x", true);
		er.drawTTFMenuFont(null, 1, 0, 0, "x", true);
	}

	@Test
	void drawScoreFontDelegationOverloadsRouteToEmptyStub() {
		EventReceiver er = new EventReceiver();
		er.drawScoreFont(null, 0, 0, 0, "x");
		er.drawScoreFont(null, 0, 0, 0, "x", 2.0f);
		er.drawScoreFont(null, 0, 0, 0, "x", EventReceiver.COLOR_RED);
		er.drawScoreFont(null, 0, 0, 0, "x", false, EventReceiver.COLOR_BLUE, EventReceiver.COLOR_GREEN);
		er.drawScoreFont(null, 0, 0, 0, "x", true, EventReceiver.COLOR_BLUE, EventReceiver.COLOR_GREEN);
		er.drawScoreFont(null, 0, 0, 0, "x", false);
		er.drawScoreFont(null, 0, 0, 0, "x", true);
		er.drawScoreFont(null, 0, 0, 0, "x", false, 1.5f);
		er.drawScoreFont(null, 0, 0, 0, "x", true, 1.5f);
		er.drawScoreFont(null, 1, 0, 0, "x", true, 1.5f);
	}

	@Test
	void drawTTFScoreFontDelegationOverloadsRouteToEmptyStub() {
		EventReceiver er = new EventReceiver();
		er.drawTTFScoreFont(null, 0, 0, 0, "x");
		er.drawTTFScoreFont(null, 0, 0, 0, "x", false, EventReceiver.COLOR_RED, EventReceiver.COLOR_BLUE);
		er.drawTTFScoreFont(null, 0, 0, 0, "x", true, EventReceiver.COLOR_RED, EventReceiver.COLOR_BLUE);
		er.drawTTFScoreFont(null, 0, 0, 0, "x", false);
		er.drawTTFScoreFont(null, 0, 0, 0, "x", true);
	}

	@Test
	void drawDirectFontDelegationOverloadsRouteToEmptyStub() {
		EventReceiver er = new EventReceiver();
		er.drawDirectFont(null, 0, 0, 0, "x");
		er.drawDirectFont(null, 0, 0, 0, "x", 2.0f);
		er.drawDirectFont(null, 0, 0, 0, "x", EventReceiver.COLOR_RED);
		er.drawDirectFont(null, 0, 0, 0, "x", false, EventReceiver.COLOR_BLUE, EventReceiver.COLOR_GREEN);
		er.drawDirectFont(null, 0, 0, 0, "x", true, EventReceiver.COLOR_BLUE, EventReceiver.COLOR_GREEN);
		er.drawDirectFont(null, 0, 0, 0, "x", false);
		er.drawDirectFont(null, 0, 0, 0, "x", true);
		er.drawDirectFont(null, 0, 0, 0, "x", false, 1.5f);
		er.drawDirectFont(null, 0, 0, 0, "x", true, 1.5f);
		er.drawDirectFont(null, 1, 0, 0, "x", true, 1.5f);
	}

	@Test
	void drawTTFDirectFontDelegationOverloadsRouteToEmptyStub() {
		EventReceiver er = new EventReceiver();
		er.drawTTFDirectFont(null, 0, 0, 0, "x");
		er.drawTTFDirectFont(null, 0, 0, 0, "x", false, EventReceiver.COLOR_RED, EventReceiver.COLOR_BLUE);
		er.drawTTFDirectFont(null, 0, 0, 0, "x", true, EventReceiver.COLOR_RED, EventReceiver.COLOR_BLUE);
		er.drawTTFDirectFont(null, 0, 0, 0, "x", false);
		er.drawTTFDirectFont(null, 0, 0, 0, "x", true);
	}

	@Test
	void drawSpeedMeterAndPlaySEAndSetGraphicsAreEmptyStubs() {
		EventReceiver er = new EventReceiver();
		er.drawSpeedMeter(null, 0, 0, 0, 5);
		er.playSE("foo");
		er.setGraphics(new Object());
	}

	@Test
	void getKeyNameByButtonIDReturnsEmptyDefault() {
		assertEquals("", new EventReceiver().getKeyNameByButtonID(null, 0));
	}

	@Test
	void getNextDisplayTypeFollowsSideAndBigSideFlags() {
		EventReceiver above = new EventReceiver();
		assertEquals(0, above.getNextDisplayType());

		EventReceiver sideSmall = new EventReceiver();
		sideSmall.sidenext = true;
		assertEquals(1, sideSmall.getNextDisplayType());

		EventReceiver sideBig = new EventReceiver();
		sideBig.sidenext = true;
		sideBig.bigsidenext = true;
		assertEquals(2, sideBig.getNextDisplayType());
	}

	@Test
	void isStickySkinOnIntReturnsFalse() {
		assertFalse(new EventReceiver().isStickySkin(0));
		assertFalse(new EventReceiver().isStickySkin(99));
	}

	@Test
	void loadModeConfigReadsModeCfgFromRunfiles() {
		// `config/setting/mode.cfg` ships with the repo and is included as a
		// runfile of the test target, so this exercises the success path
		// of loadModeConfig.
		assertNotNullCustomProperties(new EventReceiver().loadModeConfig());
	}

	private static void assertNotNullCustomProperties(CustomProperties prop) {
		if (prop == null) {
			throw new AssertionError("expected non-null CustomProperties");
		}
	}

	@Test
	void saveModeConfigSwallowsExceptionWhenWriteFails() {
		// The default working directory has no `config/setting/` folder, so
		// the underlying storeToFile throws and the catch logs and returns.
		new EventReceiver().saveModeConfig(new CustomProperties());
	}

	@Test
	void loadPropertiesReadsFileAndReturnsNullWhenMissing() throws IOException {
		EventReceiver er = new EventReceiver();
		Path file = tempDir.resolve("good.properties");
		Files.write(file, "name=x\n".getBytes(StandardCharsets.UTF_8));

		CustomProperties loaded = er.loadProperties(file.toString());
		assertEquals("x", loaded.getProperty("name"));

		assertNull(er.loadProperties(tempDir.resolve("missing.properties").toString()));
	}

	@Test
	void savePropertiesReportsSuccessAndFailure() throws IOException {
		EventReceiver er = new EventReceiver();
		CustomProperties prop = new CustomProperties();
		prop.setProperty("name", "x");

		Path file = tempDir.resolve("out.properties");
		assertTrue(er.saveProperties(file.toString(), prop));
		assertTrue(Files.exists(file));

		assertFalse(er.saveProperties("/dev/null/cannot-write/here.properties", prop));
	}

	@Test
	void lifecycleAndRenderHooksAreEmptyStubsThatAcceptNullEngine() {
		EventReceiver er = new EventReceiver();
		er.modeInit(null);
		er.playerInit(null, 0);
		er.startGame(null, 0);
		er.onFirst(null, 0);
		er.onLast(null, 0);
		er.onSetting(null, 0);
		er.onReady(null, 0);
		er.onMove(null, 0);
		er.onLockFlash(null, 0);
		er.onLineClear(null, 0);
		er.onARE(null, 0);
		er.onEndingStart(null, 0);
		er.onCustom(null, 0);
		er.onExcellent(null, 0);
		er.onGameOver(null, 0);
		er.onResult(null, 0);
		er.onFieldEdit(null, 0);
		er.renderFirst(null, 0);
		er.renderLast(null, 0);
		er.renderSetting(null, 0);
		er.renderReady(null, 0);
		er.renderMove(null, 0);
		er.renderLockFlash(null, 0);
		er.renderLineClear(null, 0);
		er.renderARE(null, 0);
		er.renderEndingStart(null, 0);
		er.renderCustom(null, 0);
		er.renderExcellent(null, 0);
		er.renderGameOver(null, 0);
		er.renderResult(null, 0);
		er.renderFieldEdit(null, 0);
		er.renderInput(null, 0);
		er.blockBreak(null, 0, 0, 0, null);
		er.calcScore(null, 0, 0);
		er.afterSoftDropFall(null, 0, 0);
		er.afterHardDropFall(null, 0, 0);
		er.fieldEditExit(null, 0);
		er.pieceLocked(null, 0, 0);
		er.lineClearEnd(null, 0);
	}

	@Test
	void saveReplayTwoArgEmptyStubAcceptsNullArguments() {
		new EventReceiver().saveReplay(null, null);
	}
}

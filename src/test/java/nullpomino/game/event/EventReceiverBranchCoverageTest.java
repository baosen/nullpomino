package nullpomino.game.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nullpomino.game.mode.AbstractMode;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

/**
 * Covers branch outcomes in {@link EventReceiver} that the other receiver
 * tests never reached: the playerID==1 (player-2 blue) font colour, the
 * side-small next-display arm, and the netplay early-return in saveReplay.
 */
class EventReceiverBranchCoverageTest {

	@TempDir
	Path tempDir;

	private static GameManager managerWithMode(AbstractMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm;
	}

	@Test
	void ttfScoreAndDirectFontUsePlayerTwoBlue() {
		EventReceiver receiver = new EventReceiver();
		GameManager gm = managerWithMode(new AbstractMode() {});

		// flag=true + playerID==1 selects COLOR_BLUE; base draw methods are no-ops
		// so this just exercises the player-2 colour branch without throwing.
		receiver.drawTTFScoreFont(gm.engine[0], 1, 0, 0, "P2", true);
		receiver.drawTTFDirectFont(gm.engine[0], 1, 0, 0, "P2", true);
	}

	@Test
	void getNextDisplayTypeReturnsSideSmallWhenSideButNotBig() throws Exception {
		EventReceiver receiver = new EventReceiver();
		setBool(receiver, "sidenext", true);
		setBool(receiver, "bigsidenext", false);

		assertEquals(1, receiver.getNextDisplayType());
	}

	@Test
	void saveReplaySkipsWritingForNetplayMode() {
		EventReceiver receiver = new EventReceiver();
		GameManager gm = managerWithMode(new AbstractMode() {
			@Override
			public boolean isNetplayMode() {
				return true;
			}
		});

		String folder = tempDir.resolve("netplay_replay").toString();
		receiver.saveReplay(gm, new CustomProperties(), folder);

		// isNetplayMode()==true returns before creating the folder or writing.
		assertFalse(Files.exists(Path.of(folder)));
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		Field f = EventReceiver.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setBoolean(obj, value);
	}
}

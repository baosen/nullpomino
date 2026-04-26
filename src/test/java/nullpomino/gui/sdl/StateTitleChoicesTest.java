package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the title screen menu surface end-to-end:
 *
 * <ul>
 *   <li>The label array (renamed first entry from "START" → "PLAY") and the
 *       parallel UI hover-text array.</li>
 *   <li>{@code maxCursor} matches the choice count so the cursor wraps over
 *       the rendered rows.</li>
 *   <li>{@link StateTitleSDL#onDecide} routes each cursor index to the
 *       expected destination state — including the "EXIT" path that sets
 *       {@code quit=true} via {@code enterState(-1)}.</li>
 *   <li>{@link StateTitleSDL#onCancel} delegates to {@code goBack()},
 *       which from an empty back-stack quits cleanly instead of leaving
 *       the user on a dead-end screen.</li>
 * </ul>
 *
 * <p>The onDecide / onCancel cases install no-op {@link BaseStateSDL}
 * stubs in {@code gameStates} so {@code doTransition()} can run without
 * SDL initialized; sound playback goes through a fresh
 * {@code SoundManagerSDL} whose {@code lib==null} guard makes
 * {@code play("decide")} a silent no-op.
 */
class StateTitleChoicesTest {

	private BaseStateSDL[] originalStates;
	private int originalCurrent;
	private boolean originalQuit;
	private Deque<Integer> originalBack;
	private Deque<Integer> originalForward;
	private SoundManagerSDL originalSound;
	private boolean originalIsTopLevel;

	@BeforeEach
	void setUp() throws Exception {
		originalStates = NullpoMinoSDL.gameStates;
		originalCurrent = NullpoMinoSDL.currentState;
		originalQuit = NullpoMinoSDL.quit;
		originalBack = snapshot("backStack");
		originalForward = snapshot("forwardStack");
		originalSound = ResourceHolderSDL.soundManager;
		originalIsTopLevel = StateSelectModeSDL.isTopLevel;

		BaseStateSDL[] stubs = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < stubs.length; i++) stubs[i] = new BaseStateSDL();
		NullpoMinoSDL.gameStates = stubs;
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_TITLE;
		NullpoMinoSDL.quit = false;
		stack("backStack").clear();
		stack("forwardStack").clear();

		// SoundManagerSDL with mixerLib=null — play() returns early.
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
		StateSelectModeSDL.isTopLevel = false;
	}

	@AfterEach
	void tearDown() throws Exception {
		NullpoMinoSDL.gameStates = originalStates;
		NullpoMinoSDL.currentState = originalCurrent;
		NullpoMinoSDL.quit = originalQuit;
		restore("backStack", originalBack);
		restore("forwardStack", originalForward);
		ResourceHolderSDL.soundManager = originalSound;
		StateSelectModeSDL.isTopLevel = originalIsTopLevel;
	}

	@Test
	void choiceLabelsMatchCommittedOrder() throws Exception {
		assertEquals("PLAY", choices()[0]);
		assertArrayEquals(
				new String[] {"PLAY", "REPLAY", "NETPLAY", "OPTIONS", "EXIT"},
				choices());
	}

	@Test
	void uiTextMatchesChoicesOneToOne() throws Exception {
		String[] uiText = (String[]) staticField("UI_TEXT").get(null);
		assertEquals(choices().length, uiText.length,
				"hover-text array must stay parallel to choices");
		assertArrayEquals(
				new String[] {"Title_Start", "Title_Replay", "Title_NetPlay", "Title_Config", "Title_Exit"},
				uiText);
	}

	@Test
	void maxCursorCoversAllChoices() throws Exception {
		StateTitleSDL state = new StateTitleSDL();
		Field f = DummyMenuChooseStateSDL.class.getDeclaredField("maxCursor");
		f.setAccessible(true);
		assertEquals(choices().length - 1, f.getInt(state));
	}

	@Test
	void onDecidePlayEntersSelectModeAtTopLevel() throws Exception {
		invokeOnDecide(0);
		assertEquals(NullpoMinoSDL.STATE_SELECTMODE, NullpoMinoSDL.currentState);
		assertTrue(StateSelectModeSDL.isTopLevel,
				"PLAY entry must mark the mode-select screen as top-level");
	}

	@Test
	void onDecideReplayEntersReplaySelect() throws Exception {
		invokeOnDecide(1);
		assertEquals(NullpoMinoSDL.STATE_REPLAYSELECT, NullpoMinoSDL.currentState);
	}

	@Test
	void onDecideNetplayEntersServerSelect() throws Exception {
		invokeOnDecide(2);
		assertEquals(NullpoMinoSDL.STATE_NET_SERVERSELECT, NullpoMinoSDL.currentState);
	}

	@Test
	void onDecideOptionsEntersConfigMainMenu() throws Exception {
		invokeOnDecide(3);
		assertEquals(NullpoMinoSDL.STATE_CONFIG_MAINMENU, NullpoMinoSDL.currentState);
	}

	@Test
	void onDecideExitSetsQuitFlag() throws Exception {
		invokeOnDecide(4);
		assertTrue(NullpoMinoSDL.quit, "EXIT must request program shutdown");
		// EXIT must not leave the user on a leaked transition.
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
	}

	@Test
	void onDecideExitSkipsTheDecideSound() throws Exception {
		// Pin the comment-documented behaviour: cursor==4 must not play
		// "decide" so the SE doesn't get cut off as audio tears down.
		// We can't observe the playback directly without SDL audio, but we
		// can pin the routing by making sure no sound call NPEs and the
		// quit flag is set — equivalent to the onDecide-Exit case but
		// verifies the sound is genuinely skipped, not just no-op'd by lib.
		ResourceHolderSDL.soundManager = new RecordingSoundManager();
		invokeOnDecide(4);
		RecordingSoundManager rec = (RecordingSoundManager) ResourceHolderSDL.soundManager;
		assertFalse(rec.played.contains("decide"),
				"EXIT path must not play the decide SE");
	}

	@Test
	void onDecideNonExitPlaysTheDecideSound() throws Exception {
		ResourceHolderSDL.soundManager = new RecordingSoundManager();
		invokeOnDecide(0);
		RecordingSoundManager rec = (RecordingSoundManager) ResourceHolderSDL.soundManager;
		assertTrue(rec.played.contains("decide"),
				"non-EXIT entries must play the decide SE");
	}

	@Test
	void onCancelFromEmptyStackQuits() throws Exception {
		// Title is the root screen, so the back stack is empty here.
		StateTitleSDL state = new StateTitleSDL();
		Method onCancel = DummyMenuChooseStateSDL.class.getDeclaredMethod("onCancel");
		onCancel.setAccessible(true);
		Object handled = onCancel.invoke(state);

		assertEquals(Boolean.TRUE, handled,
				"onCancel must claim the press so the menu base stops processing");
		assertTrue(NullpoMinoSDL.quit, "cancel from title with empty stack must quit");
	}

	@Test
	void onCancelWithBackStackUnwindsToPreviousScreen() throws Exception {
		// Simulate having reached title from another screen (rare but
		// supported: the in-source comment calls this out explicitly).
		stack("backStack").push(NullpoMinoSDL.STATE_CONFIG_MAINMENU);

		StateTitleSDL state = new StateTitleSDL();
		Method onCancel = DummyMenuChooseStateSDL.class.getDeclaredMethod("onCancel");
		onCancel.setAccessible(true);
		onCancel.invoke(state);

		assertEquals(NullpoMinoSDL.STATE_CONFIG_MAINMENU, NullpoMinoSDL.currentState);
		assertFalse(NullpoMinoSDL.quit, "non-empty back stack must navigate, not quit");
	}

	private static String[] choices() throws Exception {
		return (String[]) staticField("CHOICES").get(null);
	}

	private static Field staticField(String name) throws Exception {
		Field f = StateTitleSDL.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private static void invokeOnDecide(int cursor) throws Exception {
		StateTitleSDL state = new StateTitleSDL();
		Field cursorField = DummyMenuChooseStateSDL.class.getDeclaredField("cursor");
		cursorField.setAccessible(true);
		cursorField.setInt(state, cursor);

		Method onDecide = DummyMenuChooseStateSDL.class.getDeclaredMethod("onDecide");
		onDecide.setAccessible(true);
		onDecide.invoke(state);
	}

	@SuppressWarnings("unchecked")
	private static Deque<Integer> stack(String name) throws Exception {
		Field f = NullpoMinoSDL.class.getDeclaredField(name);
		f.setAccessible(true);
		return (Deque<Integer>) f.get(null);
	}

	private static Deque<Integer> snapshot(String name) throws Exception {
		return new ArrayDeque<>(stack(name));
	}

	private static void restore(String name, Deque<Integer> snapshot) throws Exception {
		Deque<Integer> live = stack(name);
		live.clear();
		Integer[] arr = snapshot.toArray(new Integer[0]);
		for(int i = arr.length - 1; i >= 0; i--) live.push(arr[i]);
	}

	/** Captures the names passed to {@code play()} so we can assert routing. */
	private static final class RecordingSoundManager extends SoundManagerSDL {
		final java.util.List<String> played = new java.util.ArrayList<>();
		@Override
		public void play(String name) { played.add(name); }
	}
}

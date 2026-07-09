package nullpomino.gui.sdl.binding.teavm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.KeyboardEvent;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.events.WheelEvent;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SDLStructs;

/**
 * Translates DOM keyboard/mouse input into SDL events. Listeners run on the
 * browser event loop and push into a plain {@link ArrayDeque} that the game's
 * green thread drains via SDL_PollEvent — everything is cooperatively
 * single-threaded, so no concurrent queue is needed. Mouse position/buttons
 * are tracked as plain state for SDL_GetMouseState.
 */
final class DomEventBridge {

	/**
	 * DOM codes whose browser default (scroll, tab-move, back-nav, devtools) must be
	 * suppressed on the generic queue-to-game-loop path. F11 is not here: it is
	 * handled synchronously (see keydown) and prevents its own default.
	 */
	private static final Set<String> PREVENT_DEFAULT = Set.of(
			"ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", "Space", "Tab", "Backspace",
			"F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F12");

	final Deque<SDLStructs.SDL_Event> queue = new ArrayDeque<>();

	float mouseX;
	float mouseY;
	private int mouseButtons;

	/** While true, printable keystrokes are also emitted as SDL_EVENT_TEXT_INPUT. */
	boolean textInput;

	void install(HTMLCanvasElement canvas) {
		HTMLDocument document = Window.current().getDocument();

		EventListener<KeyboardEvent> keyDown = e -> {
			// F11 toggles real browser fullscreen. This MUST run synchronously inside
			// the keydown dispatch: the browser requires a live user activation to
			// re-enter fullscreen after an exit, and a request deferred to the game
			// loop no longer qualifies (the flag flips but the canvas never goes
			// fullscreen). Handle it here and DON'T forward F11 to the game loop, so
			// its shared F11 toggle can't also fire and fight the resync below. The
			// game's fullscreen flag/config stay in sync via the fullscreenchange
			// listener. Repeats (held key) must not re-toggle, matching the game
			// loop's rising-edge detection.
			if ("F11".equals(e.getCode())) {
				e.preventDefault();
				if (!e.isRepeat()) CanvasWindow.toggleFullscreen(canvas);
				return;
			}
			int scancode = DomScancodeMap.map(e.getCode());
			if (PREVENT_DEFAULT.contains(e.getCode())) e.preventDefault();
			if (scancode >= 0) {
				SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
				ev.type = SDLConstants.SDL_EVENT_KEY_DOWN;
				ev.scancode = scancode;
				ev.keymod = DomScancodeMap.keymod(e);
				ev.keyDown = true;
				ev.keyRepeat = e.isRepeat();
				queue.add(ev);
			}
			if (textInput && !e.isCtrlKey() && !e.isMetaKey()) {
				String key = e.getKey();
				if (key != null && key.length() == 1 && key.charAt(0) >= 0x20 && key.charAt(0) != 0x7F) {
					SDLStructs.SDL_Event tev = new SDLStructs.SDL_Event();
					tev.type = SDLConstants.SDL_EVENT_TEXT_INPUT;
					tev.text = key;
					queue.add(tev);
				}
			}
		};
		EventListener<KeyboardEvent> keyUp = e -> {
			int scancode = DomScancodeMap.map(e.getCode());
			if (scancode < 0) return;
			SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
			ev.type = SDLConstants.SDL_EVENT_KEY_UP;
			ev.scancode = scancode;
			ev.keymod = DomScancodeMap.keymod(e);
			queue.add(ev);
		};
		document.addEventListener("keydown", keyDown);
		document.addEventListener("keyup", keyUp);

		// The browser can leave fullscreen on its own (e.g. Escape), which we can't
		// prevent. Feed the real state back through the SDL queue so the game's
		// fullscreen flag stays in sync.
		EventListener<Event> fsChange = e -> {
			SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
			ev.type = isBrowserFullscreen()
					? SDLConstants.SDL_EVENT_WINDOW_ENTER_FULLSCREEN
					: SDLConstants.SDL_EVENT_WINDOW_LEAVE_FULLSCREEN;
			queue.add(ev);
		};
		document.addEventListener("fullscreenchange", fsChange);
		document.addEventListener("webkitfullscreenchange", fsChange);

		// Browsers hide gamepads from navigator.getGamepads() until the user
		// presses a controller button (gamepadconnected fires then). Feed both
		// connect and disconnect through the SDL queue so the core's hotplug
		// path re-enumerates at exactly the right moments.
		EventListener<Event> padChange = e -> {
			SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
			ev.type = SDLConstants.SDL_EVENT_JOYSTICK_ADDED;
			queue.add(ev);
		};
		Window.current().addEventListener("gamepadconnected", padChange);
		Window.current().addEventListener("gamepaddisconnected", padChange);

		EventListener<MouseEvent> move = e -> {
			mouseX = e.getOffsetX();
			mouseY = e.getOffsetY();
		};
		EventListener<MouseEvent> down = e -> {
			mouseX = e.getOffsetX();
			mouseY = e.getOffsetY();
			mouseButtons |= maskFor(e.getButton());
		};
		EventListener<MouseEvent> up = e -> {
			mouseX = e.getOffsetX();
			mouseY = e.getOffsetY();
			mouseButtons &= ~maskFor(e.getButton());
		};
		canvas.addEventListener("mousemove", move);
		canvas.addEventListener("mousedown", down);
		canvas.addEventListener("mouseup", up);

		EventListener<WheelEvent> wheel = e -> {
			SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
			ev.type = SDLConstants.SDL_EVENT_MOUSE_WHEEL;
			// DOM positive deltaY = scroll down; SDL positive = scroll up.
			ev.wheelY = e.getDeltaY() > 0 ? -1f : (e.getDeltaY() < 0 ? 1f : 0f);
			queue.add(ev);
		};
		canvas.addEventListener("wheel", wheel);
	}

	void pushQuit() {
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
		ev.type = SDLConstants.SDL_EVENT_QUIT;
		queue.add(ev);
	}

	int mouseButtonMask() {
		return mouseButtons;
	}

	@JSBody(params = {}, script =
		"return !!(document.fullscreenElement || document.webkitFullscreenElement);")
	private static native boolean isBrowserFullscreen();

	private static int maskFor(short domButton) {
		switch (domButton) {
			case 0: return SDLConstants.SDL_BUTTON_LMASK;
			case 1: return SDLConstants.SDL_BUTTON_MMASK;
			case 2: return SDLConstants.SDL_BUTTON_RMASK;
			case 3: return SDLConstants.SDL_BUTTON_X1MASK;
			case 4: return SDLConstants.SDL_BUTTON_X2MASK;
			default: return 0;
		}
	}
}

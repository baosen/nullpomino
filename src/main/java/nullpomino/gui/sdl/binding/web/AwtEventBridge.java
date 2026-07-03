package nullpomino.gui.sdl.binding.web;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JComponent;

import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SDLStructs;

/**
 * Translates AWT input events into SDL events. Listeners run on the EDT and
 * push into a concurrent queue that the game thread drains via SDL_PollEvent;
 * mouse position/buttons are tracked as volatile state for SDL_GetMouseState.
 */
final class AwtEventBridge {

	final ConcurrentLinkedQueue<SDLStructs.SDL_Event> queue = new ConcurrentLinkedQueue<>();

	/** Held state per scancode, for synthesizing SDL's key-repeat flag. */
	private final boolean[] held = new boolean[SDLConstants.SDL_SCANCODE_COUNT];

	/** Panel-relative mouse position (window coordinates for GetMouseState). */
	volatile float mouseX;
	volatile float mouseY;
	private volatile int mouseButtons;

	/** While true, printable keystrokes are also emitted as SDL_EVENT_TEXT_INPUT. */
	volatile boolean textInput;

	void install(JComponent target) {
		target.setFocusable(true);
		// Let Tab reach the game as a key instead of moving Swing focus.
		target.setFocusTraversalKeysEnabled(false);

		target.addKeyListener(new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) {
				int scancode = AwtScancodeMap.map(e.getKeyCode(), e.getKeyLocation());
				if(scancode < 0) return;
				SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
				ev.type = SDLConstants.SDL_EVENT_KEY_DOWN;
				ev.scancode = scancode;
				ev.keymod = AwtScancodeMap.keymod(e.getModifiersEx());
				ev.keyDown = true;
				ev.keyRepeat = held[scancode];
				held[scancode] = true;
				queue.add(ev);
			}

			@Override public void keyReleased(KeyEvent e) {
				int scancode = AwtScancodeMap.map(e.getKeyCode(), e.getKeyLocation());
				if(scancode < 0) return;
				held[scancode] = false;
				SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
				ev.type = SDLConstants.SDL_EVENT_KEY_UP;
				ev.scancode = scancode;
				ev.keymod = AwtScancodeMap.keymod(e.getModifiersEx());
				queue.add(ev);
			}

			@Override public void keyTyped(KeyEvent e) {
				if(!textInput) return;
				char c = e.getKeyChar();
				if(c < 0x20 || c == 0x7F) return;
				SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
				ev.type = SDLConstants.SDL_EVENT_TEXT_INPUT;
				ev.text = String.valueOf(c);
				queue.add(ev);
			}
		});

		MouseAdapter mouse = new MouseAdapter() {
			@Override public void mousePressed(MouseEvent e) {
				// Click-to-focus so keyboard input works after the user
				// interacts with the canvas (important under CheerpJ).
				target.requestFocusInWindow();
				updatePosition(e);
				mouseButtons |= maskFor(e.getButton());
			}

			@Override public void mouseReleased(MouseEvent e) {
				updatePosition(e);
				mouseButtons &= ~maskFor(e.getButton());
			}

			@Override public void mouseMoved(MouseEvent e) { updatePosition(e); }
			@Override public void mouseDragged(MouseEvent e) { updatePosition(e); }

			@Override public void mouseWheelMoved(MouseWheelEvent e) {
				SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
				ev.type = SDLConstants.SDL_EVENT_MOUSE_WHEEL;
				// AWT positive = scroll down; SDL positive = scroll up.
				ev.wheelY = -(float) e.getPreciseWheelRotation();
				queue.add(ev);
			}

			private void updatePosition(MouseEvent e) {
				mouseX = e.getX();
				mouseY = e.getY();
			}
		};
		target.addMouseListener(mouse);
		target.addMouseMotionListener(mouse);
		target.addMouseWheelListener(mouse);
	}

	void pushQuit() {
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
		ev.type = SDLConstants.SDL_EVENT_QUIT;
		queue.add(ev);
	}

	int mouseButtonMask() {
		return mouseButtons;
	}

	private static int maskFor(int awtButton) {
		switch(awtButton) {
		case MouseEvent.BUTTON1: return SDLConstants.SDL_BUTTON_LMASK;
		case MouseEvent.BUTTON2: return SDLConstants.SDL_BUTTON_MMASK;
		case MouseEvent.BUTTON3: return SDLConstants.SDL_BUTTON_RMASK;
		default: return 0;
		}
	}
}

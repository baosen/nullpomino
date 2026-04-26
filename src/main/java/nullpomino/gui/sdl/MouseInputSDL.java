package nullpomino.gui.sdl;

import com.sun.jna.ptr.FloatByReference;

import nullpomino.gui.MouseInputDummy;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLConstants;

public class MouseInputSDL extends MouseInputDummy {
	public static MouseInputSDL mouseInput;

	private MouseInputSDL() {
		super();
	}

	public static void initalizeMouseInput() {
		mouseInput = new MouseInputSDL();
	}

	public void update() {
		prevMouseX = mouseX;
		prevMouseY = mouseY;

		FloatByReference mx = new FloatByReference();
		FloatByReference my = new FloatByReference();
		int buttons = SDL3.INSTANCE.SDL_GetMouseState(mx, my);

		// Map window coordinates to logical coordinates via the renderer
		FloatByReference lx = new FloatByReference();
		FloatByReference ly = new FloatByReference();
		if(SDL3.INSTANCE.SDL_RenderCoordinatesFromWindow(NullpoMinoSDL.renderer, mx.getValue(), my.getValue(), lx, ly) != 0) {
			float logX = lx.getValue();
			float logY = ly.getValue();
			mouseX = (logX >= 0 && logX < NullpoMinoSDL.LOGICAL_WIDTH) ? (int)logX : -1;
			mouseY = (logY >= 0 && logY < NullpoMinoSDL.LOGICAL_HEIGHT) ? (int)logY : -1;
		} else {
			mouseX = -1;
			mouseY = -1;
		}

		if((buttons & SDLConstants.SDL_BUTTON_LMASK) != 0) {
			mousePressed[0]++;
		} else {
			mousePressed[0] = 0;
		}
		if((buttons & SDLConstants.SDL_BUTTON_MMASK) != 0) {
			mousePressed[1]++;
		} else {
			mousePressed[1] = 0;
		}
		if((buttons & SDLConstants.SDL_BUTTON_RMASK) != 0) {
			mousePressed[2]++;
		} else {
			mousePressed[2] = 0;
		}
		if((buttons & SDLConstants.SDL_BUTTON_X1MASK) != 0) {
			mouseBackPressed++;
		} else {
			mouseBackPressed = 0;
		}
		if((buttons & SDLConstants.SDL_BUTTON_X2MASK) != 0) {
			mouseForwardPressed++;
		} else {
			mouseForwardPressed = 0;
		}
	}
}

package mu.nu.nullpo.gui.sdl;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.sun.jna.Pointer;

import mu.nu.nullpo.gui.sdl.binding.SDL3;
import mu.nu.nullpo.gui.sdl.binding.SDL3Image;
import mu.nu.nullpo.gui.sdl.binding.SDL3TTF;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;
import mu.nu.nullpo.gui.sdl.binding.SDLStructs;

import nuklear.Backend;
import nuklear.CircleCommand;
import nuklear.CircleFilledCommand;
import nuklear.Command;
import nuklear.ImageCommand;
import nuklear.LineCommand;
import nuklear.Nuklear4j;
import nuklear.RectCommand;
import nuklear.RectFilledCommand;
import nuklear.ScissorCommand;
import nuklear.TextCommand;
import nuklear.TriangleCommand;
import nuklear.TriangleFilledCommand;
import nuklear.swig.nk_buttons;
import nuklear.swig.nk_color;
import nuklear.swig.nk_context;
import nuklear.swig.nk_handle;
import nuklear.swig.nk_image;
import nuklear.swig.nk_keys;
import nuklear.swig.nuklear;

/**
 * Nuklear4j backend that renders using SDL3.
 * Replaces AWTBackend for the SDL version of NullpoMino.
 */
public class SDL3Backend implements Backend {
	private static final Logger log = Logger.getLogger(SDL3Backend.class);

	private Pointer renderer;
	private Pointer font;
	private int fontHeight;
	private int maxCharWidth;
	private int surfaceWidth;
	private int surfaceHeight;

	private int[] intBuffer = new int[500000];
	@SuppressWarnings("rawtypes")
	private Vector commandList = new Vector();

	/** SDL event queue for input handling */
	private final Vector<InputEvent> eventQueue = new Vector<InputEvent>();

	/** Image store: id -> SDL texture pointer */
	private Hashtable<Integer, Pointer> imageMap = new Hashtable<Integer, Pointer>();
	private Hashtable<Integer, int[]> imageSizeMap = new Hashtable<Integer, int[]>();
	private int nextImageId;

	/** Simple input event wrapper (avoids AWT MouseEvent/KeyEvent) */
	static class InputEvent {
		static final int MOUSE_MOVE = 0, MOUSE_DOWN = 1, MOUSE_UP = 2;
		static final int KEY_DOWN = 3, KEY_UP = 4, KEY_CHAR = 5;
		static final int KEY_REPEAT = 6;
		int type;
		int x, y;      // mouse coords or key code
		int button;     // mouse button (1=left, 3=right)
		int scancode;   // SDL scancode
		char ch;        // character for KEY_CHAR

		InputEvent(int type) { this.type = type; }
	}

	/**
	 * Create the backend. Call after SDL3 renderer and TTF font are initialized.
	 * @param renderer SDL_Renderer pointer
	 * @param font TTF_Font pointer (for text measurement and rendering)
	 */
	public SDL3Backend(Pointer renderer, Pointer font) {
		this.renderer = renderer;
		this.font = font;
		measureFont();
	}

	private void measureFont() {
		// Measure "W" to get max char width.
		// TTF_GetStringSize length param is size_t (8 bytes on 64-bit).
		// We must use long, not int, to avoid parameter misalignment.
		com.sun.jna.ptr.IntByReference wRef = new com.sun.jna.ptr.IntByReference();
		com.sun.jna.ptr.IntByReference hRef = new com.sun.jna.ptr.IntByReference();
		TTF_GetStringSize_safe(font, "W", wRef, hRef);
		maxCharWidth = wRef.getValue();
		fontHeight = hRef.getValue();

		// Fallback: if measurement returned nonsense, use the font's point size
		if (fontHeight <= 0 || fontHeight > 200) fontHeight = 16;
		if (maxCharWidth <= 0 || maxCharWidth > 200) maxCharWidth = 10;

		log.info("Font measurement: maxCharWidth=" + maxCharWidth + " fontHeight=" + fontHeight);
	}

	/**
	 * Measure text using TTF_RenderText_Blended + SDL_GetTextureSize instead of
	 * TTF_GetStringSize, to avoid any ABI issues with the size_t parameter.
	 */
	private void TTF_GetStringSize_safe(Pointer font, String text,
			com.sun.jna.ptr.IntByReference w, com.sun.jna.ptr.IntByReference h) {
		SDLStructs.SDL_Color.ByValue white = new SDLStructs.SDL_Color.ByValue(255, 255, 255);
		Pointer surface = SDL3TTF.INSTANCE.TTF_RenderText_Blended(font, text, 0, white);
		if (surface == null) {
			w.setValue(10);
			h.setValue(16);
			return;
		}
		Pointer texture = SDL3.INSTANCE.SDL_CreateTextureFromSurface(renderer, surface);
		SDL3.INSTANCE.SDL_DestroySurface(surface);
		if (texture == null) {
			w.setValue(10);
			h.setValue(16);
			return;
		}
		com.sun.jna.ptr.FloatByReference fw = new com.sun.jna.ptr.FloatByReference();
		com.sun.jna.ptr.FloatByReference fh = new com.sun.jna.ptr.FloatByReference();
		SDL3.INSTANCE.SDL_GetTextureSize(texture, fw, fh);
		SDL3.INSTANCE.SDL_DestroyTexture(texture);
		w.setValue((int) fw.getValue());
		h.setValue((int) fh.getValue());
	}

	// --- Backend interface ---

	public int getFontHeight() {
		return fontHeight;
	}

	public int getMaxCharWidth() {
		return maxCharWidth;
	}

	public void setRenderingSurface(Object graphics, int w, int h) {
		if (graphics instanceof Pointer) {
			this.renderer = (Pointer) graphics;
		}
		this.surfaceWidth = w;
		this.surfaceHeight = h;
	}

	public void clear(nk_color bgColor) {
		SDL3.setDrawColor(renderer, bgColor.getR(), bgColor.getG(), bgColor.getB(), bgColor.getA());
		SDL3.INSTANCE.SDL_RenderFillRect(renderer, new SDLStructs.SDL_FRect(0, 0, surfaceWidth, surfaceHeight));
	}

	public boolean waitEvents(long delay) {
		// In the SDL main loop we poll events ourselves, so just check the queue
		synchronized (eventQueue) {
			if (!eventQueue.isEmpty()) return true;
		}
		if (delay > 0) {
			try { Thread.sleep(delay); } catch (InterruptedException e) {}
		}
		return !eventQueue.isEmpty();
	}

	public void handleEvent(nk_context ctx) {
		nuklear.nk_input_begin(ctx);

		synchronized (eventQueue) {
			while (!eventQueue.isEmpty()) {
				InputEvent e = eventQueue.remove(0);
				switch (e.type) {
					case InputEvent.MOUSE_MOVE:
						nuklear.nk_input_motion(ctx, e.x, e.y);
						break;
					case InputEvent.MOUSE_DOWN:
						if (e.button == 1)
							nuklear.nk_input_button(ctx, nk_buttons.NK_BUTTON_LEFT, e.x, e.y, Nuklear4j.NK_TRUE);
						else if (e.button == 3)
							nuklear.nk_input_button(ctx, nk_buttons.NK_BUTTON_RIGHT, e.x, e.y, Nuklear4j.NK_TRUE);
						break;
					case InputEvent.MOUSE_UP:
						if (e.button == 1)
							nuklear.nk_input_button(ctx, nk_buttons.NK_BUTTON_LEFT, e.x, e.y, Nuklear4j.NK_FALSE);
						else if (e.button == 3)
							nuklear.nk_input_button(ctx, nk_buttons.NK_BUTTON_RIGHT, e.x, e.y, Nuklear4j.NK_FALSE);
						break;
					case InputEvent.KEY_DOWN:
						dispatchKey(ctx, e.scancode, Nuklear4j.NK_TRUE);
						break;
					case InputEvent.KEY_UP:
						dispatchKey(ctx, e.scancode, Nuklear4j.NK_FALSE);
						break;
					case InputEvent.KEY_REPEAT:
						// Nuklear needs a FALSE→TRUE transition to register a repeat
						dispatchKey(ctx, e.scancode, Nuklear4j.NK_FALSE);
						dispatchKey(ctx, e.scancode, Nuklear4j.NK_TRUE);
						break;
					case InputEvent.KEY_CHAR:
						nuklear.nk_input_char(ctx, e.ch);
						break;
				}
			}
		}

		nuklear.nk_input_end(ctx);
	}

	private void dispatchKey(nk_context ctx, int scancode, int pressed) {
		if (scancode == SDLConstants.SDL_SCANCODE_BACKSPACE)
			nuklear.nk_input_key(ctx, nk_keys.NK_KEY_BACKSPACE, pressed);
		else if (scancode == SDLConstants.SDL_SCANCODE_UP)
			nuklear.nk_input_key(ctx, nk_keys.NK_KEY_UP, pressed);
		else if (scancode == SDLConstants.SDL_SCANCODE_DOWN)
			nuklear.nk_input_key(ctx, nk_keys.NK_KEY_DOWN, pressed);
		else if (scancode == SDLConstants.SDL_SCANCODE_LEFT)
			nuklear.nk_input_key(ctx, nk_keys.NK_KEY_LEFT, pressed);
		else if (scancode == SDLConstants.SDL_SCANCODE_RIGHT)
			nuklear.nk_input_key(ctx, nk_keys.NK_KEY_RIGHT, pressed);
		else if (scancode == SDLConstants.SDL_SCANCODE_DELETE)
			nuklear.nk_input_key(ctx, nk_keys.NK_KEY_DEL, pressed);
		else if (scancode == SDLConstants.SDL_SCANCODE_RETURN)
			nuklear.nk_input_key(ctx, nk_keys.NK_KEY_ENTER, pressed);
		else if (scancode == SDLConstants.SDL_SCANCODE_LSHIFT || scancode == SDLConstants.SDL_SCANCODE_RSHIFT)
			nuklear.nk_input_key(ctx, nk_keys.NK_KEY_SHIFT, pressed);
		else if (scancode == SDLConstants.SDL_SCANCODE_TAB)
			nuklear.nk_input_key(ctx, nk_keys.NK_KEY_TAB, pressed);
		else if (scancode == SDLConstants.SDL_SCANCODE_LCTRL || scancode == SDLConstants.SDL_SCANCODE_RCTRL)
			nuklear.nk_input_key(ctx, nk_keys.NK_KEY_CTRL, pressed);
		else if (scancode == SDLConstants.SDL_SCANCODE_HOME)
			nuklear.nk_input_key(ctx, nk_keys.NK_KEY_TEXT_LINE_START, pressed);
		else if (scancode == SDLConstants.SDL_SCANCODE_END)
			nuklear.nk_input_key(ctx, nk_keys.NK_KEY_TEXT_LINE_END, pressed);
	}

	@SuppressWarnings("unchecked")
	public void render(nk_context ctx) {
		nuklear.nk_headless_render(ctx, intBuffer);
		Command.build(intBuffer, commandList);
		renderCommands(commandList);
		commandList.clear();
	}

	@SuppressWarnings("rawtypes")
	private void renderCommands(Vector commandList) {
		// Reset clip rect from previous frame before processing new commands
		SDL3.INSTANCE.SDL_SetRenderClipRect(renderer, null);
		// Enable blending for alpha support
		SDL3.INSTANCE.SDL_SetRenderDrawBlendMode(renderer, SDLConstants.SDL_BLENDMODE_BLEND);

		for (int i = 0; i < commandList.size(); i++) {
			Command command = (Command) commandList.get(i);
			int type = command.getType();

			if (type == Command.NK_COMMAND_SCISSOR) {
				ScissorCommand sc = (ScissorCommand) command;
				SDLStructs.SDL_Rect clip = new SDLStructs.SDL_Rect(sc.x, sc.y, Math.max(0, sc.w), Math.max(0, sc.h));
				SDL3.INSTANCE.SDL_SetRenderClipRect(renderer, clip);

			} else if (type == Command.NK_COMMAND_LINE) {
				LineCommand lc = (LineCommand) command;
				SDL3.setDrawColor(renderer, lc.r, lc.g, lc.b, lc.a);
				SDL3.INSTANCE.SDL_RenderLine(renderer, lc.x0, lc.y0, lc.x1, lc.y1);

			} else if (type == Command.NK_COMMAND_RECT) {
				RectCommand rc = (RectCommand) command;
				SDL3.setDrawColor(renderer, rc.r, rc.g, rc.b, rc.a);
				// SDL3 doesn't have rounded rect primitives; draw as regular rect
				SDL3.INSTANCE.SDL_RenderRect(renderer, new SDLStructs.SDL_FRect(rc.x, rc.y, rc.w, rc.h));

			} else if (type == Command.NK_COMMAND_RECT_FILLED) {
				RectFilledCommand rc = (RectFilledCommand) command;
				SDL3.setDrawColor(renderer, rc.r, rc.g, rc.b, rc.a);
				SDL3.INSTANCE.SDL_RenderFillRect(renderer, new SDLStructs.SDL_FRect(rc.x, rc.y, rc.w, rc.h));

			} else if (type == Command.NK_COMMAND_CIRCLE) {
				CircleCommand cc = (CircleCommand) command;
				SDL3.setDrawColor(renderer, cc.r, cc.g, cc.b, cc.a);
				drawEllipseOutline(cc.x, cc.y, cc.w, cc.h);

			} else if (type == Command.NK_COMMAND_CIRCLE_FILLED) {
				CircleFilledCommand cc = (CircleFilledCommand) command;
				SDL3.setDrawColor(renderer, cc.r, cc.g, cc.b, cc.a);
				drawEllipseFilled(cc.x, cc.y, cc.w, cc.h);

			} else if (type == Command.NK_COMMAND_TRIANGLE) {
				TriangleCommand tc = (TriangleCommand) command;
				SDL3.setDrawColor(renderer, tc.r, tc.g, tc.b, tc.a);
				SDL3.INSTANCE.SDL_RenderLine(renderer, tc.x0, tc.y0, tc.x1, tc.y1);
				SDL3.INSTANCE.SDL_RenderLine(renderer, tc.x1, tc.y1, tc.x2, tc.y2);
				SDL3.INSTANCE.SDL_RenderLine(renderer, tc.x2, tc.y2, tc.x0, tc.y0);

			} else if (type == Command.NK_COMMAND_TRIANGLE_FILLED) {
				TriangleFilledCommand tc = (TriangleFilledCommand) command;
				SDL3.setDrawColor(renderer, tc.r, tc.g, tc.b, tc.a);
				drawTriangleFilled(tc.x0, tc.y0, tc.x1, tc.y1, tc.x2, tc.y2);

			} else if (type == Command.NK_COMMAND_TEXT) {
				TextCommand tc = (TextCommand) command;
				// Draw background
				SDL3.setDrawColor(renderer, tc.bgR, tc.bgG, tc.bgB, tc.bgA);
				SDL3.INSTANCE.SDL_RenderFillRect(renderer, new SDLStructs.SDL_FRect(tc.x, tc.y, tc.w, tc.h));
				// Draw text
				if (tc.s != null && tc.s.length() > 0) {
					renderText(tc.s, tc.x, tc.y, tc.fgR, tc.fgG, tc.fgB, tc.fgA);
				}

			} else if (type == Command.NK_COMMAND_IMAGE) {
				ImageCommand ic = (ImageCommand) command;
				Pointer texture = imageMap.get(ic.id);
				if (texture != null) {
					SDL3.INSTANCE.SDL_RenderTexture(renderer, texture, null,
						new SDLStructs.SDL_FRect(ic.x, ic.y, ic.w, ic.h));
				}
			}
		}

		// Reset clip rect
		SDL3.INSTANCE.SDL_SetRenderClipRect(renderer, null);
	}

	private void renderText(String text, int x, int y, int r, int g, int b, int a) {
		if (text == null || text.isEmpty()) return;
		if (a == 0) a = 255;

		SDLStructs.SDL_Color.ByValue color = new SDLStructs.SDL_Color.ByValue(r, g, b, a);
		Pointer surface = SDL3TTF.INSTANCE.TTF_RenderText_Blended(font, text, 0, color);
		if (surface == null) return;

		Pointer texture = SDL3.INSTANCE.SDL_CreateTextureFromSurface(renderer, surface);
		SDL3.INSTANCE.SDL_DestroySurface(surface);
		if (texture == null) return;

		SDL3.INSTANCE.SDL_SetTextureBlendMode(texture, SDLConstants.SDL_BLENDMODE_BLEND);

		com.sun.jna.ptr.FloatByReference wRef = new com.sun.jna.ptr.FloatByReference();
		com.sun.jna.ptr.FloatByReference hRef = new com.sun.jna.ptr.FloatByReference();
		SDL3.INSTANCE.SDL_GetTextureSize(texture, wRef, hRef);

		SDL3.INSTANCE.SDL_RenderTexture(renderer, texture, null,
			new SDLStructs.SDL_FRect(x, y, wRef.getValue(), hRef.getValue()));
		SDL3.INSTANCE.SDL_DestroyTexture(texture);
	}

	/** Draw an ellipse outline using the midpoint algorithm */
	private void drawEllipseOutline(int x, int y, int w, int h) {
		float cx = x + w / 2.0f;
		float cy = y + h / 2.0f;
		float rx = w / 2.0f;
		float ry = h / 2.0f;
		int segments = Math.max(16, (int)(Math.PI * (rx + ry) / 2));
		float angleStep = (float)(2.0 * Math.PI / segments);
		float prevX = cx + rx;
		float prevY = cy;
		for (int i = 1; i <= segments; i++) {
			float angle = i * angleStep;
			float nx = cx + rx * (float)Math.cos(angle);
			float ny = cy + ry * (float)Math.sin(angle);
			SDL3.INSTANCE.SDL_RenderLine(renderer, prevX, prevY, nx, ny);
			prevX = nx;
			prevY = ny;
		}
	}

	/** Draw a filled ellipse using horizontal scan lines */
	private void drawEllipseFilled(int x, int y, int w, int h) {
		float cx = x + w / 2.0f;
		float cy = y + h / 2.0f;
		float rx = w / 2.0f;
		float ry = h / 2.0f;
		for (int dy = (int)-ry; dy <= (int)ry; dy++) {
			float halfWidth = rx * (float)Math.sqrt(1.0 - (dy * dy) / (ry * ry));
			SDL3.INSTANCE.SDL_RenderLine(renderer, cx - halfWidth, cy + dy, cx + halfWidth, cy + dy);
		}
	}

	/** Draw a filled triangle using scan-line rasterization */
	private void drawTriangleFilled(int x0, int y0, int x1, int y1, int x2, int y2) {
		// Sort vertices by Y coordinate
		if (y0 > y1) { int t; t=x0;x0=x1;x1=t; t=y0;y0=y1;y1=t; }
		if (y0 > y2) { int t; t=x0;x0=x2;x2=t; t=y0;y0=y2;y2=t; }
		if (y1 > y2) { int t; t=x1;x1=x2;x2=t; t=y1;y1=y2;y2=t; }

		for (int scanY = y0; scanY <= y2; scanY++) {
			float xA, xB;
			if (scanY < y1) {
				if (y1 == y0) xA = x0; else xA = x0 + (float)(scanY - y0) / (y1 - y0) * (x1 - x0);
			} else {
				if (y2 == y1) xA = x1; else xA = x1 + (float)(scanY - y1) / (y2 - y1) * (x2 - x1);
			}
			if (y2 == y0) xB = x0; else xB = x0 + (float)(scanY - y0) / (y2 - y0) * (x2 - x0);
			if (xA > xB) { float t = xA; xA = xB; xB = t; }
			SDL3.INSTANCE.SDL_RenderLine(renderer, xA, scanY, xB, scanY);
		}
	}

	// --- Image management ---

	public nk_image createImage(InputStream is) {
		// Read stream to temp file, load with SDL3_image
		try {
			java.io.File tmp = java.io.File.createTempFile("nk_img_", ".png");
			tmp.deleteOnExit();
			java.io.FileOutputStream fos = new java.io.FileOutputStream(tmp);
			byte[] buf = new byte[4096];
			int n;
			while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
			fos.close();

			Pointer surface = SDL3Image.INSTANCE.IMG_Load(tmp.getAbsolutePath());
			if (surface == null) return null;

			Pointer texture = SDL3.INSTANCE.SDL_CreateTextureFromSurface(renderer, surface);
			com.sun.jna.ptr.FloatByReference wRef = new com.sun.jna.ptr.FloatByReference();
			com.sun.jna.ptr.FloatByReference hRef = new com.sun.jna.ptr.FloatByReference();
			SDL3.INSTANCE.SDL_GetTextureSize(texture, wRef, hRef);
			SDL3.INSTANCE.SDL_DestroySurface(surface);

			int id = nextImageId++;
			imageMap.put(id, texture);
			imageSizeMap.put(id, new int[]{ (int)wRef.getValue(), (int)hRef.getValue() });

			nk_handle handle = new nk_handle();
			handle.setId(id);
			nk_image img = new nk_image();
			img.setW((int)wRef.getValue());
			img.setH((int)hRef.getValue());
			img.setHandle(handle);
			return img;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public nk_image createARGBImage(int w, int h) {
		// Create a blank ARGB texture
		// For now just track the size; setImageData will fill it
		int id = nextImageId++;
		imageSizeMap.put(id, new int[]{ w, h });
		// Create a streaming texture would be ideal, but for simplicity
		// we'll create it when setImageData is called
		nk_handle handle = new nk_handle();
		handle.setId(id);
		nk_image img = new nk_image();
		img.setW(w);
		img.setH(h);
		img.setHandle(handle);
		return img;
	}

	public void setImageData(nk_image nuklearImage, int[] argb) {
		// Not commonly used in the lobby UI; stub for now
	}

	public void destroyImage(nk_image nuklearImage) {
		if (nuklearImage != null) {
			int id = nuklearImage.getHandle().getId();
			Pointer tex = imageMap.remove(id);
			if (tex != null) SDL3.INSTANCE.SDL_DestroyTexture(tex);
			imageSizeMap.remove(id);
		}
	}

	// --- Input feeding (called from the SDL event loop) ---

	public void feedMouseMotion(int x, int y) {
		InputEvent e = new InputEvent(InputEvent.MOUSE_MOVE);
		e.x = x; e.y = y;
		synchronized (eventQueue) {
			// Merge consecutive mouse motion events
			if (!eventQueue.isEmpty()) {
				InputEvent last = eventQueue.lastElement();
				if (last.type == InputEvent.MOUSE_MOVE) {
					eventQueue.set(eventQueue.size() - 1, e);
					return;
				}
			}
			eventQueue.add(e);
		}
	}

	public void feedMouseButton(int button, boolean pressed, int x, int y) {
		InputEvent e = new InputEvent(pressed ? InputEvent.MOUSE_DOWN : InputEvent.MOUSE_UP);
		e.button = button; e.x = x; e.y = y;
		synchronized (eventQueue) { eventQueue.add(e); }
	}

	public void feedKey(int scancode, boolean pressed) {
		InputEvent e = new InputEvent(pressed ? InputEvent.KEY_DOWN : InputEvent.KEY_UP);
		e.scancode = scancode;
		synchronized (eventQueue) { eventQueue.add(e); }
	}

	public void feedKeyRepeat(int scancode) {
		InputEvent e = new InputEvent(InputEvent.KEY_REPEAT);
		e.scancode = scancode;
		synchronized (eventQueue) { eventQueue.add(e); }
	}

	public void feedChar(char ch) {
		InputEvent e = new InputEvent(InputEvent.KEY_CHAR);
		e.ch = ch;
		synchronized (eventQueue) { eventQueue.add(e); }
	}
}

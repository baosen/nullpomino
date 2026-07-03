package nullpomino.gui.sdl.binding.web;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SDLStructs;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;

/**
 * Java2D implementation of the SDL renderer: a back buffer the game thread
 * draws into, and a front snapshot swapped on present for the EDT to paint.
 */
final class WebRenderer implements SdlRenderer {

	final WebWindow window;

	/** Logical render size (SDL_SetRenderLogicalPresentation). */
	volatile int logicalW;
	volatile int logicalH;

	private BufferedImage backBuffer;
	private Graphics2D g;

	final Object frontLock = new Object();
	volatile BufferedImage front;

	private Color drawColor = Color.BLACK;
	private int blendMode = SDLConstants.SDL_BLENDMODE_NONE;

	WebRenderer(WebWindow window, int w, int h) {
		this.window = window;
		allocate(Math.max(w, 1), Math.max(h, 1));
		window.renderer = this;
	}

	void setLogicalSize(int w, int h) {
		allocate(w, h);
	}

	private void allocate(int w, int h) {
		logicalW = w;
		logicalH = h;
		if(g != null) g.dispose();
		backBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		g = backBuffer.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		synchronized(frontLock) {
			front = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		}
	}

	void setDrawColor(int r, int gr, int b, int a) {
		drawColor = new Color(r & 0xFF, gr & 0xFF, b & 0xFF, a & 0xFF);
	}

	void setBlendMode(int mode) {
		blendMode = mode;
	}

	void clear() {
		g.setComposite(AlphaComposite.Src);
		g.setColor(new Color(drawColor.getRed(), drawColor.getGreen(), drawColor.getBlue()));
		g.fillRect(0, 0, logicalW, logicalH);
	}

	void fillRect(SDLStructs.SDL_FRect rect) {
		applyDrawState();
		if(rect == null) {
			g.fillRect(0, 0, logicalW, logicalH);
		} else {
			g.fillRect((int) rect.x, (int) rect.y, (int) rect.w, (int) rect.h);
		}
	}

	void drawRect(SDLStructs.SDL_FRect rect) {
		applyDrawState();
		if(rect == null) {
			g.drawRect(0, 0, logicalW - 1, logicalH - 1);
		} else {
			g.drawRect((int) rect.x, (int) rect.y, (int) rect.w - 1, (int) rect.h - 1);
		}
	}

	private void applyDrawState() {
		if(blendMode == SDLConstants.SDL_BLENDMODE_BLEND) {
			g.setComposite(AlphaComposite.SrcOver);
			g.setColor(drawColor);
		} else {
			// BLENDMODE_NONE: overwrite, alpha ignored (buffer is opaque).
			g.setComposite(AlphaComposite.Src);
			g.setColor(new Color(drawColor.getRed(), drawColor.getGreen(), drawColor.getBlue()));
		}
	}

	void renderTexture(WebTexture texture, SDLStructs.SDL_FRect src, SDLStructs.SDL_FRect dst) {
		if(texture == null) return;
		BufferedImage img = texture.img;

		int sx1, sy1, sx2, sy2;
		if(src == null) {
			sx1 = 0; sy1 = 0; sx2 = img.getWidth(); sy2 = img.getHeight();
		} else {
			sx1 = (int) src.x; sy1 = (int) src.y;
			sx2 = (int) (src.x + src.w); sy2 = (int) (src.y + src.h);
		}

		int dx1, dy1, dx2, dy2;
		if(dst == null) {
			dx1 = 0; dy1 = 0; dx2 = logicalW; dy2 = logicalH;
		} else {
			dx1 = (int) dst.x; dy1 = (int) dst.y;
			dx2 = (int) (dst.x + dst.w); dy2 = (int) (dst.y + dst.h);
		}

		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, texture.alphaMod / 255f));
		g.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
	}

	BufferedImage snapshot() {
		BufferedImage copy = new BufferedImage(backBuffer.getWidth(), backBuffer.getHeight(),
			BufferedImage.TYPE_INT_RGB);
		Graphics2D cg = copy.createGraphics();
		cg.drawImage(backBuffer, 0, 0, null);
		cg.dispose();
		return copy;
	}

	void present() {
		synchronized(frontLock) {
			Graphics2D fg = front.createGraphics();
			fg.drawImage(backBuffer, 0, 0, null);
			fg.dispose();
		}
		window.panel.repaint();
	}

	void dispose() {
		if(g != null) {
			g.dispose();
			g = null;
		}
		if(window.renderer == this) window.renderer = null;
	}
}

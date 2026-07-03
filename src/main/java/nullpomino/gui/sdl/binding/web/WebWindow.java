package nullpomino.gui.sdl.binding.web;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import nullpomino.gui.sdl.binding.SdlHandles.SdlWindow;

/**
 * The single game window: a Swing frame with a panel that letterboxes the
 * renderer's front buffer. All game rendering happens off the EDT into the
 * renderer's back buffer; the EDT only ever paints the latest front snapshot.
 */
final class WebWindow implements SdlWindow {

	final AwtEventBridge bridge = new AwtEventBridge();

	JFrame frame;
	PresentPanel panel;

	/** Set by SDL_CreateRenderer; the panel paints nothing until then. */
	volatile WebRenderer renderer;

	final class PresentPanel extends JPanel {
		@Override protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, getWidth(), getHeight());

			WebRenderer r = renderer;
			if(r == null) return;
			BufferedImage front = r.front;
			Letterbox box = new Letterbox(getWidth(), getHeight(), r.logicalW, r.logicalH);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			synchronized(r.frontLock) {
				g2.drawImage(front, box.offsetX, box.offsetY, box.scaledW, box.scaledH, null);
			}
		}
	}

	WebWindow(String title, int w, int h) {
		runOnEdt(() -> {
			frame = new JFrame(title);
			frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter() {
				@Override public void windowClosing(WindowEvent e) {
					bridge.pushQuit();
				}
			});

			panel = new PresentPanel();
			panel.setPreferredSize(new Dimension(w, h));
			panel.setBackground(Color.BLACK);
			bridge.install(panel);

			frame.setContentPane(panel);
			frame.pack();
			frame.setLocationByPlatform(true);
			frame.setVisible(true);
			panel.requestFocusInWindow();
		});
	}

	void setTitle(String title) {
		SwingUtilities.invokeLater(() -> frame.setTitle(title));
	}

	void dispose() {
		SwingUtilities.invokeLater(() -> frame.dispose());
	}

	/** Current letterbox transform for input mapping (panel size is read live). */
	Letterbox letterbox() {
		WebRenderer r = renderer;
		int lw = (r != null) ? r.logicalW : panel.getWidth();
		int lh = (r != null) ? r.logicalH : panel.getHeight();
		return new Letterbox(panel.getWidth(), panel.getHeight(), Math.max(lw, 1), Math.max(lh, 1));
	}

	private static void runOnEdt(Runnable task) {
		if(SwingUtilities.isEventDispatchThread()) {
			task.run();
			return;
		}
		try {
			SwingUtilities.invokeAndWait(task);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while creating window", e);
		} catch(InvocationTargetException e) {
			throw new IllegalStateException("Window creation failed", e.getCause());
		}
	}
}

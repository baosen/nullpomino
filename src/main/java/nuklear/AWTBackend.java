package nuklear;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import nuklear.swig.nk_buttons;
import nuklear.swig.nk_color;
import nuklear.swig.nk_context;
import nuklear.swig.nk_handle;
import nuklear.swig.nk_image;
import nuklear.swig.nk_keys;
import nuklear.swig.nuklear;

public class AWTBackend implements Backend, MouseMotionListener, MouseListener, KeyListener {

	private boolean initialized = false;
	private Font font = new Font(Font.MONOSPACED, Font.PLAIN, 10);
	private FontMetrics fontMetrics;
	private Vector eventQueue = new Vector();
	private Vector commandList = new Vector();
	private int[] intBuffer = new int[10000];
	private Graphics renderingSurfaceGraphics;
	private int renderingSurfaceWidth;
	private int renderingSurfaceHeight;

	/* Components created for standalone mode only*/
	private BufferedImage offscreenImage;
	private JPanel panel;
	private JFrame frame;

	public int getFontHeight() {
		return fontMetrics.getHeight();
	}

	public int getMaxCharWidth() {
		return fontMetrics.stringWidth("W");
	}

	/**
	 * Set the rendering surface.
	 * @param h
	 * @param w
	 *
	 * @param offscreenImage
	 */
	public void setRenderingSurface(Object graphics, int w, int h) {
		this.renderingSurfaceGraphics = (Graphics) graphics;
		this.renderingSurfaceWidth = w;
		this.renderingSurfaceHeight = h;
//		if (fontMetrics == null) {
//			fontMetrics = renderingSurfaceGraphics.getFontMetrics(font);
//		}
	}

	public void setFont(Font font) {
		if (initialized) {
			throw new IllegalStateException("Font can't be changed after initialization");
		}
		this.font = font;
	}

	public void initialize() {
		initialized = true;
		// Need a graphic context to get font metrics
		BufferedImage image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB_PRE);
		fontMetrics = image.getGraphics().getFontMetrics(font);
	}

	/**
	 * Rendering will be done in the provided BufferedImage. Remember to add
	 * this class as the event listener.
	 *
	 * @param screenImage
	 */
	public void initialize(BufferedImage screenImage) {
		initialize();
		this.offscreenImage = screenImage;
		setRenderingSurface(screenImage.getGraphics(), screenImage.getWidth(), screenImage.getHeight());
		fontMetrics = renderingSurfaceGraphics.getFontMetrics(font);
	}

	/**
	 * Create a frame and render inside.
	 *
	 * @param screenWidth
	 * @param screenHeight
	 */
	public void initialize(int screenWidth, int screenHeight) {
		initialize(new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_ARGB_PRE));

		final Dimension dimension = new Dimension(screenWidth, screenHeight);
		panel = new JPanel() {

			public Dimension getMinimumSize() {
				return dimension;
			}

			public Dimension getPreferredSize() {
				return dimension;
			}

			public void paintComponent(java.awt.Graphics g) {
				super.paintComponent(g);
				g.drawImage(offscreenImage, 0, 0, null);
			}
		};
		panel.setFocusable(true);

		// Events
		panel.addMouseMotionListener(this);
		panel.addMouseListener(this);
		panel.addKeyListener(this);

		// Show
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// frame.addWindowListener(this);
		frame.add(panel);
		frame.setResizable(false);
		frame.pack();
		frame.setLocationRelativeTo(null); // Center the frame (has to be called
											// after pack)
		frame.setVisible(true);
		panel.requestFocusInWindow();

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see nuklear.Backend#clear(nuklear.swig.nk_color)
	 */
	public void clear(nk_color bgColor) {
		Color c = new Color(bgColor.getR(), bgColor.getG(), bgColor.getB());
		renderingSurfaceGraphics.setColor(c);
		if (offscreenImage == null) {
			renderingSurfaceGraphics.fillRect(0, 0, renderingSurfaceWidth, renderingSurfaceHeight);
		} else {
			renderingSurfaceGraphics.fillRect(0, 0, offscreenImage.getWidth(), offscreenImage.getHeight());
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see nuklear.Backend#handleEvent(nuklear.swig.nk_context)
	 */
	public void handleEvent(nk_context ctx) {

		nuklear.nk_input_begin(ctx);

		synchronized (eventQueue) {
			int size = eventQueue.size();
			if (size > 0) {
				Object o = eventQueue.remove(0);
				if (o instanceof MouseEvent) {
					MouseEvent me = (MouseEvent) o;

					if (me.getID() == MouseEvent.MOUSE_MOVED) {
						// System.out.println("MOUSE_MOVED");
						nuklear.nk_input_motion(ctx, me.getX(), me.getY());
					} else if (me.getID() == MouseEvent.MOUSE_PRESSED) {
						//System.out.println("MOUSE_PRESSED");
						// int Nuklear.NK_TRUE = (me.getID() ==
						// MouseEvent.MOUSE_PRESSED) ? Nuklear.NK_TRUE :
						// Nuklear.NK_FALSE;
						int modifiers = me.getModifiersEx();
						int button = me.getButton();
						if (button == MouseEvent.BUTTON1) {
							//System.out.println("MOUSE_PRESSED: BUTTON1");
							nuklear.nk_input_button(ctx, nk_buttons.NK_BUTTON_LEFT, me.getX(), me.getY(), Nuklear4j.NK_TRUE);
						} else if (button == MouseEvent.BUTTON3) {
							// System.out.println("MOUSE_PRESSED: BUTTON3");
							nuklear.nk_input_button(ctx, nk_buttons.NK_BUTTON_RIGHT, me.getX(), me.getY(), Nuklear4j.NK_TRUE);
						}
					} else if (me.getID() == MouseEvent.MOUSE_RELEASED) {
						// int Nuklear.NK_TRUE = (me.getID() ==
						// MouseEvent.MOUSE_PRESSED) ? Nuklear.NK_TRUE :
						// Nuklear.NK_FALSE;
						int button = me.getButton();
						if (button == MouseEvent.BUTTON1) {
							//System.out.println("MOUSE_RELEASED: BUTTON1");
							nuklear.nk_input_button(ctx, nk_buttons.NK_BUTTON_LEFT, me.getX(), me.getY(), Nuklear4j.NK_FALSE);
						} else if (button == MouseEvent.BUTTON3) {
							// System.out.println("MOUSE_RELEASED: BUTTON3");
							nuklear.nk_input_button(ctx, nk_buttons.NK_BUTTON_RIGHT, me.getX(), me.getY(), Nuklear4j.NK_FALSE);
						}
					} else if (me.getID() == MouseEvent.MOUSE_DRAGGED) {
						// int Nuklear.NK_TRUE = (me.getID() ==
						// MouseEvent.MOUSE_PRESSED) ? Nuklear.NK_TRUE :
						// Nuklear.NK_FALSE;
						int button = me.getButton();
						if (button == MouseEvent.BUTTON1) {
							// System.out.println("MOUSE_RELEASED: BUTTON1");
							nuklear.nk_input_button(ctx, nk_buttons.NK_BUTTON_LEFT, me.getX(), me.getY(), Nuklear4j.NK_TRUE);
						} else if (button == MouseEvent.BUTTON3) {
							// System.out.println("MOUSE_RELEASED: BUTTON3");
							nuklear.nk_input_button(ctx, nk_buttons.NK_BUTTON_RIGHT, me.getX(), me.getY(), Nuklear4j.NK_TRUE);
						}
						nuklear.nk_input_motion(ctx, me.getX(), me.getY());
					}

					// nuklear.nk_input_button(ctx, nk_buttons.NK_BUTTON_LEFT,
					// me.getX(), me.getY(), down);

				} else if (o instanceof KeyEvent) {
					KeyEvent e = (KeyEvent) o;
					char c = e.getKeyChar();
					int keyCode = e.getKeyCode();
					boolean pressed = (e.getID() == KeyEvent.KEY_PRESSED);
					int nkPressed = pressed ? Nuklear4j.NK_TRUE : Nuklear4j.NK_FALSE;
					if (keyCode == KeyEvent.VK_BACK_SPACE) {
						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_BACKSPACE, nkPressed);
					} else if (keyCode == KeyEvent.VK_UP) {
						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_UP, nkPressed);
					} else if (keyCode == KeyEvent.VK_DOWN) {
						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_DOWN, nkPressed);
					} else if (keyCode == KeyEvent.VK_LEFT) {
						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_LEFT, nkPressed);
					} else if (keyCode == KeyEvent.VK_RIGHT) {
						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_RIGHT, nkPressed);
					} else if (keyCode == KeyEvent.VK_DELETE) {
						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_DEL, nkPressed);
					} else if (keyCode == KeyEvent.VK_ENTER) {
						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_ENTER, nkPressed);
					} else if (keyCode == KeyEvent.VK_SHIFT) {
						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_SHIFT, nkPressed);
					} else if (keyCode == KeyEvent.VK_TAB) {
						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_TAB, nkPressed);
					} else if (keyCode == KeyEvent.VK_CONTROL) {
						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_CTRL, nkPressed);
					} else if (keyCode == KeyEvent.VK_END) {
						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_TEXT_LINE_END, nkPressed);
					} else if (keyCode == KeyEvent.VK_HOME) {
						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_TEXT_LINE_START, nkPressed);
					}
//					else if (keyCode == KeyEvent.VK_C && e.isControlDown() && pressed)  {
//						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_COPY, nkPressed);
//					} else if (keyCode == KeyEvent.VK_X && e.isControlDown()) {
//						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_CUT, Nuklear4j.NK_FALSE);
//					} else if (keyCode == KeyEvent.VK_V && e.isControlDown()) {
//						nuklear.nk_input_key(ctx, nk_keys.NK_KEY_PASTE, nkPressed);
//					}
					else {
						if (!pressed) {
							if (c != KeyEvent.CHAR_UNDEFINED) {
								nuklear.nk_input_char(ctx, c);
							}
						}
					}
				}
			}
		}

		nuklear.nk_input_end(ctx);

		// nk_input_motion(ctx, evt->motion.x, evt->motion.y);
	}

	private Hashtable imageMap = new Hashtable();
	private int imageId;

	private nk_image createImage(BufferedImage javaImage) {
		nk_handle imageHandle = new nk_handle();
		imageHandle.setId(imageId);
		nk_image nuklearImage = new nk_image();
		nuklearImage.setW(javaImage.getWidth());
		nuklearImage.setH(javaImage.getHeight());
		nuklearImage.setHandle(imageHandle);
		imageMap.put(imageId, javaImage);
		imageId++;
		return nuklearImage;
	}

	public nk_image createImage(InputStream is) {
		try {
			BufferedImage javaImage = ImageIO.read(is);
			return createImage(javaImage);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public nk_image createARGBImage(int w, int h) {
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		return createImage(image);
	}

	public void setImageData(nk_image nuklearImage, int[] argb) {
		BufferedImage image = (BufferedImage)imageMap.get(nuklearImage.getHandle().getId());
		if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
			throw new IllegalStateException("Image type is not ARGB");
		}
		int[] data = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(argb, 0, data, 0, argb.length);
	}

	public void destroyImage(nk_image nuklearImage) {
		if (nuklearImage != null) {
			imageMap.remove(nuklearImage.getHandle().getId());
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see nuklear.Backend#render(nuklear.swig.nk_context)
	 */
	public void render(nk_context ctx) {
		nuklear.nk_headless_render(ctx, intBuffer);
		Command.build(intBuffer, commandList);
		render(commandList);
		commandList.clear();
	}

	private void render(Vector commandList) {
		int size = commandList.size();

		if (renderingSurfaceGraphics == null) {
			return;
		}

		renderingSurfaceGraphics.setFont(font);

		for (int i = 0; i < size; i++) {
			Command command = (Command) commandList.get(i);
			if (command.getType() == Command.NK_COMMAND_SCISSOR) {
				ScissorCommand sc = (ScissorCommand) command;
				renderingSurfaceGraphics.setClip(sc.x, sc.y, sc.w + 1, sc.h);
			} else if (command.getType() == Command.NK_COMMAND_LINE) {
				LineCommand lc = (LineCommand) command;
				renderingSurfaceGraphics.setColor(new Color(lc.r, lc.g, lc.b, lc.a));
				renderingSurfaceGraphics.drawLine(lc.x0, lc.y0, lc.x1, lc.y1);
			} else if (command.getType() == Command.NK_COMMAND_RECT) {
				RectCommand rc = (RectCommand) command;
				renderingSurfaceGraphics.setColor(new Color(rc.r, rc.g, rc.b, rc.a));
				if (rc.rounded == Nuklear4j.NK_FALSE) {
					renderingSurfaceGraphics.drawRect(rc.x, rc.y, rc.w, rc.h);
				} else {
					renderingSurfaceGraphics.drawRoundRect(rc.x, rc.y, rc.w, rc.h, rc.rounded, rc.rounded);
				}
			} else if (command.getType() == Command.NK_COMMAND_RECT_FILLED) {
				RectFilledCommand rc = (RectFilledCommand) command;
				renderingSurfaceGraphics.setColor(new Color(rc.r, rc.g, rc.b, rc.a));
				if (rc.rounded == Nuklear4j.NK_FALSE) {
					renderingSurfaceGraphics.fillRect(rc.x, rc.y, rc.w, rc.h);
				} else {
					renderingSurfaceGraphics.fillRoundRect(rc.x, rc.y, rc.w, rc.h, rc.rounded, rc.rounded);
				}
			} else if (command.getType() == Command.NK_COMMAND_CIRCLE) {
				CircleCommand cc = (CircleCommand) command;
				renderingSurfaceGraphics.setColor(new Color(cc.r, cc.g, cc.b, cc.a));
				renderingSurfaceGraphics.drawOval(cc.x, cc.y, cc.w, cc.h);
			} else if (command.getType() == Command.NK_COMMAND_CIRCLE_FILLED) {
				CircleFilledCommand cc = (CircleFilledCommand) command;
				renderingSurfaceGraphics.setColor(new Color(cc.r, cc.g, cc.b, cc.a));
				renderingSurfaceGraphics.fillOval(cc.x, cc.y, cc.w, cc.h);
			} else if (command.getType() == Command.NK_COMMAND_TRIANGLE) {
				TriangleCommand tc = (TriangleCommand) command;
				int[] xPoints = { tc.x0, tc.x1, tc.x2 };
				int[] yPoints = { tc.y0, tc.y1, tc.y2 };
				renderingSurfaceGraphics.setColor(new Color(tc.r, tc.g, tc.b, tc.a));
				renderingSurfaceGraphics.drawPolygon(xPoints, yPoints, 3);
			} else if (command.getType() == Command.NK_COMMAND_TRIANGLE_FILLED) {
				TriangleFilledCommand tc = (TriangleFilledCommand) command;
				int[] xPoints = { tc.x0, tc.x1, tc.x2 };
				int[] yPoints = { tc.y0, tc.y1, tc.y2 };
				renderingSurfaceGraphics.setColor(new Color(tc.r, tc.g, tc.b, tc.a));
				renderingSurfaceGraphics.fillPolygon(xPoints, yPoints, 3);
			} else if (command.getType() == Command.NK_COMMAND_TEXT) {
				TextCommand tc = (TextCommand) command;

				int x = tc.x;
				int y = tc.y;
				int w = tc.w;
				int h = tc.h;
				// System.out.println("draw text " + x + " " + y + " " + w + " "
				// + h + " " + tc.s);

				renderingSurfaceGraphics.setColor(new Color(tc.bgR, tc.bgG, tc.bgB, tc.bgA));
				renderingSurfaceGraphics.fillRect(x, y, w, h);
				renderingSurfaceGraphics.setColor(new Color(tc.fgR, tc.fgG, tc.fgB, tc.fgA));
				int stringH = fontMetrics.getAscent();
				renderingSurfaceGraphics.drawString(tc.s, x, y + stringH);

			} else if (command.getType() == Command.NK_COMMAND_IMAGE) {
				ImageCommand ic = (ImageCommand) command;
				int x = ic.x;
				int y = ic.y;
				int w = ic.w;
				int h = ic.h;
				BufferedImage image = (BufferedImage)imageMap.get(ic.id);
				renderingSurfaceGraphics.drawImage(image, x, y, w, h, null);
			}
		}

		if (panel != null) {
			panel.paintImmediately(0, 0, offscreenImage.getWidth(), offscreenImage.getHeight());
		}

	}

	private void addAndMergeMouseMotionEvents(MouseEvent currentEvent) {
		synchronized (eventQueue) {
			Object o;
			if (!eventQueue.isEmpty() && ((o = eventQueue.lastElement()) instanceof MouseEvent)) {
				MouseEvent lastEvent = (MouseEvent) o;
				if (currentEvent.getID() == lastEvent.getID()) {
					eventQueue.set(eventQueue.size() - 1, currentEvent);
				} else {
					eventQueue.add(currentEvent);
				}
			} else {
				eventQueue.add(currentEvent);
			}
		}
	}

	public void mouseDragged(MouseEvent e) {
		addAndMergeMouseMotionEvents(e);
	}

	public void mouseMoved(MouseEvent e) {
		addAndMergeMouseMotionEvents(e);
	}

	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public void mousePressed(MouseEvent e) {
		synchronized (eventQueue) {
			eventQueue.add(e);
			// e.consume();
		}

	}

	public void mouseReleased(MouseEvent e) {
		synchronized (eventQueue) {
			eventQueue.add(e);
			// e.consume();
		}

	}

	public void keyPressed(KeyEvent e) {
		synchronized (eventQueue) {
			eventQueue.add(e);
			// e.consume();
		}
	}

	public void keyReleased(KeyEvent e) {
		synchronized (eventQueue) {
			eventQueue.add(e);
			// e.consume();
		}
	}

	public void keyTyped(KeyEvent e) {

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see nuklear.Backend#waitEvents(long)
	 */
	public boolean waitEvents(long delay) {
		synchronized (eventQueue) {
			if (eventQueue.size() > 0) {
				return true;
			}
		}

		if (delay >= 0) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return false;

	}

	public Vector getEventQueue() {
		return eventQueue;
	}

}
package nuklear.demo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

import nuklear.AWTBackend;
import nuklear.Backend;

/*
 * This second demo shows how to get more control and how draw in your own widget.
 */
public class AWTDemo2 extends AbstractDemo implements MouseMotionListener, MouseListener, KeyListener {

	private static int PANEL_WIDTH = 640;
	private static int PANEL_HEIGHT = 480;
	private AWTBackend backend;
	private JPanel panel;

	public static void main(String argv[]) {
		AWTDemo2 demo = new AWTDemo2();
		demo.initialize(PANEL_WIDTH, PANEL_HEIGHT);
		demo.createAWTGUI();
	}

	public AWTDemo2() {
		backend = new AWTBackend();
	}

	public void createAWTGUI() {

		final Dimension dimension = new Dimension(PANEL_WIDTH, PANEL_HEIGHT);

		panel = new JPanel() {

			public Dimension getMinimumSize() {
				return dimension;
			}

			public Dimension getPreferredSize() {
				return dimension;
			}

			public void paintComponent(java.awt.Graphics g) {
				super.paintComponent(g);

				// Paint yourself
				g.setColor(Color.RED);
				g.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
				g.setColor(Color.WHITE);
				g.fillArc(PANEL_WIDTH - 100, 50, 50, 50, 0, 360);

				// Let nuklear paint on top
				backend.setRenderingSurface(g, PANEL_WIDTH, PANEL_HEIGHT);
				drawOverview();
			}
		};
		panel.setFocusable(true);

		// Events
		panel.addMouseMotionListener(this);
		panel.addMouseListener(this);
		panel.addKeyListener(this);

		// Show
		JFrame frame = new JFrame();
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


	//@Override
	public Backend getBackend(int screenWidth, int screenHeight) {

		/*
		 * Uncomment if you want to play with fonts (default: Font.MONOSPACED, Font.PLAIN, 10)
		 */
		//backend.setFont(new Font(Font.SERIF, Font.PLAIN, 12));

		backend.initialize();


		/* If you want to render in your own buffer:
		 * backend.initialize(BufferedImage image);
		 * myAWTOrSwingComponent.addMouseMotionListener(backend);
		 * myAWTOrSwingComponent.addMouseListener(backend);
		 * myAWTOrSwingComponent.addKeyListener(backend);
		 */

		return backend;
	}

	private void requestRepaint() {
		panel.repaint();
		//panel.paintImmediately(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
	}

	public void mouseDragged(MouseEvent e) {
		backend.mouseDragged(e);
		requestRepaint();
	}

	public void mouseMoved(MouseEvent e) {
		backend.mouseMoved(e);
		requestRepaint();
	}

	public void mouseClicked(MouseEvent e) {
		backend.mouseClicked(e);
		requestRepaint();
	}

	public void mouseEntered(MouseEvent e) {
		backend.mouseEntered(e);
		requestRepaint();

	}

	public void mouseExited(MouseEvent e) {
		backend.mouseExited(e);
		requestRepaint();
	}

	public void mousePressed(MouseEvent e) {
		backend.mousePressed(e);
		requestRepaint();
	}

	public void mouseReleased(MouseEvent e) {
		backend.mouseReleased(e);
		requestRepaint();
	}

	public void keyPressed(KeyEvent e) {
		backend.keyPressed(e);
		requestRepaint();
	}

	public void keyReleased(KeyEvent e) {
		backend.keyReleased(e);
		requestRepaint();
	}

	public void keyTyped(KeyEvent e) {
		backend.keyTyped(e);
		requestRepaint();
	}

}

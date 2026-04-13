package nuklear.demo;

import nuklear.AWTBackend;
import nuklear.Backend;

/* Simple demo where all the work is done in the backend. You can just focus on the nuklear API.
 * It's adapted when you don't need to integrate nuklear with your own widgets */
public class AWTDemo extends AbstractDemo {

	private static int PANEL_WIDTH = 640;
	private static int PANEL_HEIGHT = 480;

	private AWTBackend backend = new AWTBackend();

	public static void main(String argv[]) {
		AWTDemo demo = new AWTDemo();
		demo.initialize(PANEL_WIDTH, PANEL_HEIGHT);
		demo.overviewLoop();
	}


	//@Override
	public Backend getBackend(int screenWidth, int screenHeight) {

		/*
		 * Uncomment if you want to play with fonts (default: Font.MONOSPACED, Font.PLAIN, 10)
		 */
		//backend.setFont(new Font(Font.SERIF, Font.PLAIN, 12));

		backend.initialize(screenWidth, screenHeight);

		/* If you want to render in your own buffer:
		 * backend.initialize(BufferedImage image);
		 * myAWTOrSwingComponent.addMouseMotionListener(backend);
		 * myAWTOrSwingComponent.addMouseListener(backend);
		 * myAWTOrSwingComponent.addKeyListener(backend);
		 */

		return backend;
	}

}

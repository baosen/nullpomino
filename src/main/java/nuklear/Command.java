package nuklear;

import java.util.Vector;

import nuklear.swig.nk_command_type;
import nuklear.swig.nuklearConstants;

public abstract class Command {

	public int getType() {
		return type;
	}

	public static final int NK_COMMAND_SCISSOR = nk_command_type.NK_COMMAND_SCISSOR;
	public static final int NK_COMMAND_LINE = nk_command_type.NK_COMMAND_LINE;
	public static final int NK_COMMAND_RECT = nk_command_type.NK_COMMAND_RECT;
	public static final int NK_COMMAND_RECT_FILLED = nk_command_type.NK_COMMAND_RECT_FILLED;
	public static final int NK_COMMAND_CIRCLE = nk_command_type.NK_COMMAND_CIRCLE;
	public static final int NK_COMMAND_CIRCLE_FILLED = nk_command_type.NK_COMMAND_CIRCLE_FILLED;
	public static final int NK_COMMAND_TRIANGLE = nk_command_type.NK_COMMAND_TRIANGLE;
	public static final int NK_COMMAND_TRIANGLE_FILLED = nk_command_type.NK_COMMAND_TRIANGLE_FILLED;
	public static final int NK_COMMAND_TEXT = nk_command_type.NK_COMMAND_TEXT;
	public static final int NK_COMMAND_IMAGE = nk_command_type.NK_COMMAND_IMAGE;
	private int type;

	public Command(int type) {
		this.type = type;
	}

	public static void build(int[] drawBuffer, Vector commandList) {

		int index = 0;
		int commandCount = drawBuffer[index++];
		Command[] commands = new Command[commandCount];

		for (int i = 0; i < commandCount; i++) {

			int commandType = drawBuffer[index++];
			int commandSize = drawBuffer[index++];

			if (commandType == Command.NK_COMMAND_SCISSOR) {
				checkSize(commandSize, nuklearConstants.NK_COMMAND_SCISSOR_SIZE);
				int x = drawBuffer[index++];
				int y = drawBuffer[index++];
				int w = drawBuffer[index++];
				int h = drawBuffer[index++];
				commandList.add(new ScissorCommand(x, y, w, h));
				// System.out.println("Command: SCISSOR " + x + " " + y + " " +
				// w + " " + h);

			} else if (commandType == Command.NK_COMMAND_LINE) {
				checkSize(commandSize, nuklearConstants.NK_COMMAND_LINE_SIZE);
				int x0 = drawBuffer[index++];
				int y0 = drawBuffer[index++];
				int x1 = drawBuffer[index++];
				int y1 = drawBuffer[index++];
				int line_thickness = drawBuffer[index++];
				int a = drawBuffer[index++];
				int r = drawBuffer[index++];
				int g = drawBuffer[index++];
				int b = drawBuffer[index++];
				commandList.add(new LineCommand(x0, y0, x1, y1, line_thickness, a, r, g, b));
				// System.out.println("Command: LINE " + x0 + " " + y0 + " " +
				// x1 + " " + y1);
			} else if (commandType == Command.NK_COMMAND_RECT) {
				checkSize(commandSize, nuklearConstants.NK_COMMAND_RECT_SIZE);
				int x = drawBuffer[index++];
				int y = drawBuffer[index++];
				int w = drawBuffer[index++];
				int h = drawBuffer[index++];
				int rounded = drawBuffer[index++];
				int line_thickness = drawBuffer[index++];
				int a = drawBuffer[index++];
				int r = drawBuffer[index++];
				int g = drawBuffer[index++];
				int b = drawBuffer[index++];
				commandList.add(new RectCommand(x, y, w, h, rounded, line_thickness, a, r, g, b));
				// System.out.println("Command: RECT " + x + " " + y + " " + w +
				// " " + h);

			} else if (commandType == Command.NK_COMMAND_RECT_FILLED) {
				checkSize(commandSize, nuklearConstants.NK_COMMAND_RECT_FILLED_SIZE);
				int x = drawBuffer[index++];
				int y = drawBuffer[index++];
				int w = drawBuffer[index++];
				int h = drawBuffer[index++];
				int rounded = drawBuffer[index++];
				int a = drawBuffer[index++];
				int r = drawBuffer[index++];
				int g = drawBuffer[index++];
				int b = drawBuffer[index++];
				commandList.add(new RectFilledCommand(x, y, w, h, rounded, a, r, g, b));
				// System.out.println("Command: RECT_FILLED " + x + " " + y + "
				// " + w + " " + h);

			} else if (commandType == Command.NK_COMMAND_CIRCLE) {
				checkSize(commandSize, nuklearConstants.NK_COMMAND_CIRCLE_SIZE);
				int x = drawBuffer[index++];
				int y = drawBuffer[index++];
				int w = drawBuffer[index++];
				int h = drawBuffer[index++];
				int line_thickness = drawBuffer[index++];
				int a = drawBuffer[index++];
				int r = drawBuffer[index++];
				int g = drawBuffer[index++];
				int b = drawBuffer[index++];
				commandList.add(new CircleCommand(x, y, w, h, line_thickness, a, r, g, b));
				// System.out.println("Command: CIRCLE " + x + " " + y + " " + w
				// + " " + h);

			} else if (commandType == Command.NK_COMMAND_CIRCLE_FILLED) {
				checkSize(commandSize, nuklearConstants.NK_COMMAND_CIRCLE_FILLED_SIZE);
				int x = drawBuffer[index++];
				int y = drawBuffer[index++];
				int w = drawBuffer[index++];
				int h = drawBuffer[index++];
				int a = drawBuffer[index++];
				int r = drawBuffer[index++];
				int g = drawBuffer[index++];
				int b = drawBuffer[index++];
				commandList.add(new CircleFilledCommand(x, y, w, h, a, r, g, b));
				// System.out.println("Command: CIRCLE_FILLED " + x + " " + y +
				// " " + w + " " + h);

			} else if (commandType == Command.NK_COMMAND_TRIANGLE) {
				checkSize(commandSize, nuklearConstants.NK_COMMAND_TRIANGLE_SIZE);
				int x0 = drawBuffer[index++];
				int y0 = drawBuffer[index++];
				int x1 = drawBuffer[index++];
				int y1 = drawBuffer[index++];
				int x2 = drawBuffer[index++];
				int y2 = drawBuffer[index++];
				int line_thickness = drawBuffer[index++];
				int a = drawBuffer[index++];
				int r = drawBuffer[index++];
				int g = drawBuffer[index++];
				int b = drawBuffer[index++];
				commandList.add(new TriangleCommand(x0, y0, x1, y1, x2, y2, line_thickness, a, r, g, b));
				// System.out.println("Command: TRIANGLE " + x0 + " " + y0 + " "
				// + x1 + " " + y1 + " " + x2 + " " + y2);

			} else if (commandType == Command.NK_COMMAND_TRIANGLE_FILLED) {
				checkSize(commandSize, nuklearConstants.NK_COMMAND_TRIANGLE_FILLED_SIZE);
				int x0 = drawBuffer[index++];
				int y0 = drawBuffer[index++];
				int x1 = drawBuffer[index++];
				int y1 = drawBuffer[index++];
				int x2 = drawBuffer[index++];
				int y2 = drawBuffer[index++];
				int a = drawBuffer[index++];
				int r = drawBuffer[index++];
				int g = drawBuffer[index++];
				int b = drawBuffer[index++];
				commandList.add(new TriangleFilledCommand(x0, y0, x1, y1, x2, y2, a, r, g, b));
				// System.out.println("Command: TRIANGLE " + x0 + " " + y0 + " "
				// + x1 + " " + y1 + " " + x2 + " " + y2);

			} else if (commandType == Command.NK_COMMAND_TEXT) {

				int x = drawBuffer[index++];
				int y = drawBuffer[index++];
				int w = drawBuffer[index++];
				int h = drawBuffer[index++];
				int len = drawBuffer[index++];
				checkSize(commandSize, nuklearConstants.NK_COMMAND_TEXT_SIZE + len);
				int bgA = drawBuffer[index++];
				int bgR = drawBuffer[index++];
				int bgG = drawBuffer[index++];
				int bgB = drawBuffer[index++];
				int fgA = drawBuffer[index++];
				int fgR = drawBuffer[index++];
				int fgG = drawBuffer[index++];
				int fgB = drawBuffer[index++];

				char[] chars = new char[len];
				for (int j = 0; j < len; j++) {
					chars[j] = (char) drawBuffer[index++];
				}
				String s = new String(chars);

				commandList.add(new TextCommand(x, y, w, h, len, bgA, bgR, bgG, bgB, fgA, fgR, fgG, fgB, s));
				// System.out.println("Command: TEXT " + x + " " + y + " " + w +
				// " " + h + " " + s);

			} else if (commandType == Command.NK_COMMAND_IMAGE) {
				checkSize(commandSize, nuklearConstants.NK_COMMAND_IMAGE_SIZE);
				int x = drawBuffer[index++];
				int y = drawBuffer[index++];
				int w = drawBuffer[index++];
				int h = drawBuffer[index++];
				int id = drawBuffer[index++];
				commandList.add(new ImageCommand(x, y, w, h, id));
//				 System.out.println("Command: IMAGE " + x + " " + y + " " + w +
//				 " " + h);

			} else {
				throw new RuntimeException("Unknown command : " + commandType);
			}
		}

	}

	private static void checkSize(int commandSize, int expectedSize) {
		if (commandSize != expectedSize) {
			System.out.println("ERROR");
			throw new RuntimeException();
		}
	}
}
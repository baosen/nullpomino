package nuklear;

public class LineCommand extends Command {
	public int x0;
	public int y0;
	public int x1;
	public int y1;
	public int line_thickness;
	public int a;
	public int r;
	public int g;
	public int b;

	public LineCommand(int x0, int y0, int x1, int y1, int line_thickness, int a, int r, int g, int b) {
		super(NK_COMMAND_LINE);
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
		this.line_thickness = line_thickness;
		this.a = a;
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public String toString() {
		return "LineCommand [x0=" + x0 + ", y0=" + y0 + ", x1=" + x1 + ", y1=" + y1 + ", line_thickness="
				+ line_thickness + ", a=" + a + ", r=" + r + ", g=" + g + ", b=" + b + "]";
	}

}
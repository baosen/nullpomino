package nuklear;

public class CircleFilledCommand extends Command {
	public int x;
	public int y;
	public int w;
	public int h;
	public int a;
	public int r;
	public int g;
	public int b;

	public CircleFilledCommand(int x, int y, int w, int h, int a, int r, int g, int b) {
		super(NK_COMMAND_CIRCLE_FILLED);
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.a = a;
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public CircleFilledCommand(int type, int x, int y, int w, int h, int a, int r, int g, int b) {
		super(type);
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.a = a;
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public String toString() {
		return "CircleFilledCommand [x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + ", a=" + a + ", r=" + r + ", g="
				+ g + ", b=" + b + "]";
	}

}
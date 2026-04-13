package nuklear;

public class ScissorCommand extends Command {
	public int x;
	public int y;
	public int w;
	public int h;

	public ScissorCommand(int x, int y, int w, int h) {
		super(NK_COMMAND_SCISSOR);
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
	}

	public String toString() {
		return "ScissorCommand [x=" + x + ", y=" + y + ", w=" + w + ", h=" + h + "]";
	}

}
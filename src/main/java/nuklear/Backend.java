package nuklear;

import java.io.InputStream;

import nuklear.swig.nk_color;
import nuklear.swig.nk_context;
import nuklear.swig.nk_image;

public interface Backend {

	public void clear(nk_color bgColor);

	public void handleEvent(nk_context ctx);

	public void setRenderingSurface(Object graphics, int w, int h);

	public void render(nk_context ctx);

	public boolean waitEvents(long delay);

	public int getFontHeight();

	public int getMaxCharWidth();

	public nk_image createImage(InputStream is);

	public nk_image createARGBImage(int w, int h);

	public void setImageData(nk_image nuklearImage, int[] argb);

	public void destroyImage(nk_image nuklearImage);

}
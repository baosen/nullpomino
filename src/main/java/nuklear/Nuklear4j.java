package nuklear;

import nuklear.swig.nk_context;
import nuklear.swig.nuklear;
import nuklear.swig.nuklearConstants;

public class Nuklear4j {

	public static int NK_TRUE;
	public static int NK_FALSE;

	public static void initializeNative() {
		System.loadLibrary("nuklear-java");

		NK_TRUE = nuklearConstants.nk_true;
		NK_FALSE = nuklearConstants.nk_false;
	}

	public static void initializeContext(nk_context ctx, int width, int height, int max_char_width, int font_height) {
		nuklear.nk_headless_init(ctx, width, height, max_char_width, font_height);
	}

	public static void convertCharArrayToIntArray(char[] inputCharArray, int[] outputIntArray) {
		for (int i = 0; i < inputCharArray.length; i++) {
			outputIntArray[i] = inputCharArray[i];
		}
	}

	public static void convertIntArrayToCharArray(int[] inputIntArray, char[] outputCharArray) {
		for (int i = 0; i < inputIntArray.length; i++) {
			outputCharArray[i] = (char)inputIntArray[i];
		}
	}

}

package nuklear.demo;

import java.io.InputStream;

import nuklear.Backend;
import nuklear.Nuklear4j;
import nuklear.swig.nk_button_behavior;
import nuklear.swig.nk_color;
import nuklear.swig.nk_context;
import nuklear.swig.nk_edit_types;
import nuklear.swig.nk_image;
import nuklear.swig.nk_layout_format;
import nuklear.swig.nk_modify;
import nuklear.swig.nk_panel;
import nuklear.swig.nk_panel_flags;
import nuklear.swig.nk_popup_type;
import nuklear.swig.nk_rect;
import nuklear.swig.nk_style_header_align;
import nuklear.swig.nk_text_alignment;
import nuklear.swig.nk_vec2;
import nuklear.swig.nuklear;

/**
 * Simple backend-agnostic demo
 */
public abstract class AbstractDemo {

	static {
		Nuklear4j.initializeNative();
	}

	protected nk_context nuklearContext;
	private Backend backend;

	// Widget params/states
	nk_color bgColor;
	boolean show_menu = true;
	boolean titlebar = true;
	boolean border = true;
	boolean resize = true;
	boolean movable = true;
	boolean no_scrollbar = true;
	boolean minimizable = true;
	boolean close = true;
	nk_panel layout;
	nk_panel menu;
	/* popups */
	int header_align = nk_style_header_align.NK_HEADER_RIGHT;
	boolean show_app_about = false;

	long window_flags = 0;

	nk_rect bounds;

	int MENU_DEFAULT = 0;
	int MENU_WINDOWS = 1;
	int[] mprog = { 60 };
	int[] mslider = { 10 };
	int[] mcheck = { Nuklear4j.NK_TRUE };

	int[] prog = { 40 };
	int[] slider = { 10 };
	int[] check = { Nuklear4j.NK_TRUE };

	int currentChoice = 0;
	String[] choices = new String[] { "Earth", "Mars", "Saturn" };
	int comboItemHeight = 25;

	int EASY = 0;
	int HARD = 1;
	int op = EASY;
	int[] property = { 20 };

	int editBufferMaxSize = 255;
	int[] editBufferCurrentLength = new int[1];
	byte[] byteBuffer;
	nk_image image;

	public void initialize(int w, int h) {
		initializeCore(w, h);
		initializeGUI();
	}

	private void initializeCore(int w, int h) {
		backend = getBackend(w, h);
		nuklearContext = new nk_context();
		Nuklear4j.initializeContext(nuklearContext, w, h, backend.getMaxCharWidth(), backend.getFontHeight());
	}

	private void initializeGUI() {

		bgColor = new nk_color();
		bgColor.setA((short) 255);
		bgColor.setR((short) 190);
		bgColor.setG((short) 190);
		bgColor.setB((short) 190);

		layout = new nk_panel();
		menu = new nk_panel();

		window_flags = 0;
		if (border)
			window_flags |= nk_panel_flags.NK_WINDOW_BORDER;
		if (resize)
			window_flags |= nk_panel_flags.NK_WINDOW_SCALABLE;
		if (movable)
			window_flags |= nk_panel_flags.NK_WINDOW_MOVABLE;
		if (no_scrollbar)
			window_flags |= nk_panel_flags.NK_WINDOW_NO_SCROLLBAR;
		if (minimizable)
			window_flags |= nk_panel_flags.NK_WINDOW_MINIMIZABLE;
		if (close)
			window_flags |= nk_panel_flags.NK_WINDOW_CLOSABLE;

		bounds = new nk_rect();
		bounds.setX(10);
		bounds.setY(10);
		bounds.setW(400);
		bounds.setH(400);

		String initString = "You can edit me !";
		StringBuffer stringBuffer = new StringBuffer(editBufferMaxSize);
		stringBuffer.append(initString);
		stringBuffer.setLength(editBufferMaxSize);
		editBufferCurrentLength[0] = initString.length();
		byteBuffer = stringBuffer.toString().getBytes();

		InputStream is = getClass().getResourceAsStream("/image7.png");
		image = backend.createImage(is);
	}

	public abstract Backend getBackend(int screenWidth, int screenHeight);

	public void overviewLoop() {

		boolean firstLoop = true;

		while (true) {
			/*
			 * We choose to render only if an event occurs
			 */
			if (backend.waitEvents(50) || firstLoop) {
				backend.clear(bgColor);
				drawOverview();
				firstLoop = false; // Ugly
			}
		}
	}

	public void drawOverview() {

		backend.handleEvent(nuklearContext);

		if (nuklear.nk_begin(nuklearContext, layout, "Overview", bounds, window_flags)) {
			if (show_menu) {
				/* menubar */
				nuklear.nk_menubar_begin(nuklearContext);
				nuklear.nk_layout_row_begin(nuklearContext, nk_layout_format.NK_STATIC, 25, 4);
				nuklear.nk_layout_row_push(nuklearContext, 70);
				if (nuklear.nk_menu_begin_label(nuklearContext, menu, "MENU", nk_text_alignment.NK_TEXT_LEFT, 120)) {

					nuklear.nk_layout_row_dynamic(nuklearContext, 25, 1);
					if (nuklear.nk_menu_item_label(nuklearContext, "Hide", nk_text_alignment.NK_TEXT_LEFT))
						show_menu = false;
					if (nuklear.nk_menu_item_label(nuklearContext, "About", nk_text_alignment.NK_TEXT_LEFT))
						show_app_about = true;
					nuklear.nk_progress(nuklearContext, prog, 100, nk_modify.NK_MODIFIABLE);
					nuklear.nk_slider_int(nuklearContext, 0, slider, 16, 1);
					nuklear.nk_checkbox_label(nuklearContext, "check", check);
					nuklear.nk_menu_end(nuklearContext);
				}
				nuklear.nk_layout_row_push(nuklearContext, 70);
				nuklear.nk_progress(nuklearContext, mprog, 100, nk_modify.NK_MODIFIABLE);
				nuklear.nk_slider_int(nuklearContext, 0, mslider, 16, 1);
				nuklear.nk_layout_row_push(nuklearContext, 90);
				nuklear.nk_checkbox_label(nuklearContext, "check", mcheck);
				nuklear.nk_menubar_end(nuklearContext);
				nuklear.nk_layout_row_end(nuklearContext);

				nuklear.nk_layout_row_begin(nuklearContext, nk_layout_format.NK_DYNAMIC, 100, 1);
				nuklear.nk_layout_row_push(nuklearContext, 1.0f);
				// nuklear.nk_edit_string(ctx, nk_edit_types.NK_EDIT_BOX,
				// stringBuffer, editBufferCurrentLength,
				// editBufferMaxSize);
				nuklear.nk_edit_string2(nuklearContext, nk_edit_types.NK_EDIT_BOX, byteBuffer, editBufferCurrentLength, editBufferMaxSize);
				nuklear.nk_layout_row_end(nuklearContext);

				nuklear.nk_layout_row_begin(nuklearContext, nk_layout_format.NK_DYNAMIC, 25, 1);
				nuklear.nk_layout_row_push(nuklearContext, 1.0f);
				currentChoice = nuklear.nk_combo(nuklearContext, choices, choices.length, currentChoice, comboItemHeight);
				nuklear.nk_layout_row_end(nuklearContext);

				nuklear.nk_layout_row_dynamic(nuklearContext, 30, 2);
				if (nuklear.nk_option_label(nuklearContext, "easy", op == EASY))
					op = EASY;
				if (nuklear.nk_option_label(nuklearContext, "hard", op == HARD))
					op = HARD;
				nuklear.nk_layout_row_dynamic(nuklearContext, 25, 1);
				nuklear.nk_property_int(nuklearContext, "Compression:", 0, property, 100, 10, 1);

				nuklear.nk_layout_row_begin(nuklearContext, nk_layout_format.NK_DYNAMIC, 50, 2);
				nuklear.nk_layout_row_push(nuklearContext, 0.3f);
				nuklear.nk_button_image(nuklearContext, image, nk_button_behavior.NK_BUTTON_DEFAULT);
				nuklear.nk_layout_row_push(nuklearContext, 0.5f);
				nuklear.nk_button_image_label(nuklearContext, image, "button with label", nk_text_alignment.NK_TEXT_CENTERED, nk_button_behavior.NK_BUTTON_DEFAULT);
				nuklear.nk_layout_row_end(nuklearContext);

			}

			if (show_app_about) {
				/* about popup */
				nk_panel popup = new nk_panel();
				nk_rect s = new nk_rect();
				s.setX(20);
				s.setY(100);
				s.setW(300);
				s.setH(190);
				if (nuklear.nk_popup_begin(nuklearContext, popup, nk_popup_type.NK_POPUP_STATIC, "About", nk_panel_flags.NK_WINDOW_CLOSABLE, s)) {
					nuklear.nk_layout_row_dynamic(nuklearContext, 20, 1);
					nuklear.nk_label(nuklearContext, "Nuklear", nk_text_alignment.NK_TEXT_LEFT);
					nuklear.nk_label(nuklearContext, "By Micha Mettke", nk_text_alignment.NK_TEXT_LEFT);
					nuklear.nk_label(nuklearContext, "nuklear is licensed under the public domain License.", nk_text_alignment.NK_TEXT_LEFT);
					nuklear.nk_popup_end(nuklearContext);
				} else
					show_app_about = false;
			}

		} // if (nuklear.nk_begin)
		nuklear.nk_end(nuklearContext);

		backend.render(nuklearContext);

	}

	// public void calculator() {
	//
	// nk_panel layout = new nk_panel();
	// long flags = nk_panel_flags.NK_WINDOW_BORDER |
	// nk_panel_flags.NK_WINDOW_MOVABLE | nk_panel_flags.NK_WINDOW_NO_SCROLLBAR;
	//
	//
	// nk_rect bounds = new nk_rect();
	// bounds.setX(10);
	// bounds.setY(10);
	// bounds.setW(180);
	// bounds.setH(250);
	// if (nuklear.nk_begin(ctx, layout, "Calculator", bounds, flags)) {
	// boolean set = false;
	// boolean prev = false;
	// boolean op = false;
	// char numbers[] = { '7', '8', '9', '4', '5', '6' ,'1', '2', '3' };
	// char ops[] = { '+', '-', '*', '/' };
	// double a = 0, b = 0;
	// double current = a;
	//
	// int i = 0;
	// boolean solve = false;
	// {
	// int[] len = { 10 };
	// int[] buffer = new int[256];
	// nuklear.nk_layout_row_dynamic(ctx, 35, 1);
	// //len = snprintf(buffer, 256, "%.2f", *current);
	// nuklear.nk_edit_string2(ctx, nk_edit_types.NK_EDIT_SIMPLE, buffer, len,
	// 255);
	// //buffer[len] = 0;
	// //*current = atof(buffer);}
	//
	//
	// nuklear.nk_layout_row_dynamic(ctx, 35, 4);
	// for (i = 0; i < 16; ++i) {
	// if (i >= 12 && i < 15) {
	// if (i > 12) continue;
	// if (nuklear.nk_button_label(ctx, "C",
	// nk_button_behavior.NK_BUTTON_DEFAULT)) {
	// a = b = 0;
	// op = false;
	// current = &a; set = false;
	// } if (nuklear.nk_button_label(ctx, "0",
	// nk_button_behavior.NK_BUTTON_DEFAULT)) {
	// *current = *current*10.0f; set = false;
	// }
	// if (nuklear.nk_button_label(ctx, "=",
	// nk_button_behavior.NK_BUTTON_DEFAULT)) {
	// solve = true; prev = op; op = 0;
	// }
	// } else if (((i+1) % 4)) {
	// if (nuklear.nk_button_text(ctx, &numbers[(i/4)*3+i%4], 1,
	// nk_button_behavior.NK_BUTTON_DEFAULT)) {
	// *current = *current * 10.0f + numbers[(i/4)*3+i%4] - '0';
	// set = false;
	// }
	// } else if (nk_button_text(ctx, &ops[i/4], 1,
	// nk_button_behavior.NK_BUTTON_DEFAULT)) {
	// if (!set) {
	// if (current != &b) {
	// current = &b;
	// } else {
	// prev = op;
	// solve = true;
	// }
	// }
	// op = ops[i/4];
	// set = false;
	// }
	// }
	// if (solve) {
	// if (prev == '+') a = a + b;
	// if (prev == '-') a = a - b;
	// if (prev == '*') a = a * b;
	// if (prev == '/') a = a / b;
	// current = &a;
	// if (set) current = &b;
	// b = 0; set = false;
	// }
	// }
	// nk_end(ctx);
	// }

	public void simple() {

		int EASY = 0;
		int HARD = 1;
		int op = EASY;
		int[] property = { 20 };
		byte[] buffer = new byte[64];
		int[] len = { 0 };
		nk_color bgColor = new nk_color();
		bgColor.setA((short) 255);
		bgColor.setR((short) 190);
		bgColor.setG((short) 190);
		bgColor.setB((short) 190);

		nk_panel layout = new nk_panel();
		// nk_rect(50, 50, 210, 250)
		nk_rect bounds = new nk_rect();
		bounds.setX(50);
		bounds.setY(50);
		bounds.setW(210);
		bounds.setH(250);
		long flags = nk_panel_flags.NK_WINDOW_BORDER | nk_panel_flags.NK_WINDOW_MOVABLE | nk_panel_flags.NK_WINDOW_SCALABLE | nk_panel_flags.NK_WINDOW_MINIMIZABLE
				| nk_panel_flags.NK_WINDOW_TITLE;

		while (true) {

			if (backend.waitEvents(50)) {
				backend.handleEvent(nuklearContext);

				if (nuklear.nk_begin(nuklearContext, layout, "Demo", bounds, flags)) {
					nuklear.nk_layout_row_static(nuklearContext, 30, 80, 1);
					if (nuklear.nk_button_label(nuklearContext, "button", nk_button_behavior.NK_BUTTON_DEFAULT)) {
						System.out.println("button pressed");
					}

					nuklear.nk_layout_row_dynamic(nuklearContext, 30, 2);
					if (nuklear.nk_option_label(nuklearContext, "easy", op == EASY))
						op = EASY;
					if (nuklear.nk_option_label(nuklearContext, "hard", op == HARD))
						op = HARD;
					nuklear.nk_layout_row_dynamic(nuklearContext, 25, 1);
					nuklear.nk_property_int(nuklearContext, "Compression:", 0, property, 100, 10, 1);
					nuklear.nk_edit_string2(nuklearContext, nk_edit_types.NK_EDIT_SIMPLE, buffer.toString().getBytes(), len, buffer.length);

				}
				nuklear.nk_end(nuklearContext);

				backend.clear(bgColor);
				backend.render(nuklearContext);
			}

		}
	}

}

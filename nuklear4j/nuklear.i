 %module nuklear

 %{
typedef enum { false, true } bool;
//#define NK_INCLUDE_FIXED_TYPES
#define NK_INCLUDE_DEFAULT_ALLOCATOR
#include "nuklear_headless.h"
extern int initialize(int w, int h);
 %}

//%include "carrays.i"
//%array_class(int, IntArray);

//%include "enums.swg"
%include "enumtypeunsafe.swg"
%javaconst(1);

#define NK_COMMAND_SCISSOR_SIZE 4
#define NK_COMMAND_LINE_SIZE 9
#define NK_COMMAND_CURVE_SIZE 1
#define NK_COMMAND_RECT_SIZE 10
#define NK_COMMAND_RECT_FILLED_SIZE 9
#define NK_COMMAND_RECT_MULTI_COLOR_SIZE 1
#define NK_COMMAND_CIRCLE_SIZE 9
#define NK_COMMAND_CIRCLE_FILLED_SIZE 8
#define NK_COMMAND_ARC_SIZE 1
#define NK_COMMAND_ARC_FILLED_SIZE 1
#define NK_COMMAND_TRIANGLE_SIZE 11
#define NK_COMMAND_TRIANGLE_FILLED_SIZE 10
#define NK_COMMAND_POLYGON_SIZE 1
#define NK_COMMAND_POLYGON_FILLED_SIZE 1
#define NK_COMMAND_POLYLINE_SIZE 1
#define NK_COMMAND_TEXT_SIZE 13
#define NK_COMMAND_IMAGE_SIZE 5

#define NK_API extern
#define NK_FLAG(x) (1 << (x))
#define NK_UTF_SIZE 4
#define NK_INPUT_MAX 16

typedef unsigned char nk_byte;
typedef short nk_short;
typedef unsigned short nk_ushort;
typedef int nk_int;
typedef unsigned int nk_uint;
typedef unsigned long nk_size;
typedef unsigned long nk_ptr;
typedef unsigned int nk_hash;
typedef unsigned int nk_flags;
typedef nk_uint nk_rune;
typedef unsigned char nk_byte;

//enum {nk_false, nk_true};
#define nk_false 0
#define nk_true 1
struct nk_color {nk_byte r,g,b,a;};
struct nk_vec2 {float x,y;};
struct nk_vec2i {short x, y;};
struct nk_rect {float x,y,w,h;};
struct nk_recti {short x,y,w,h;};
typedef char nk_glyph[NK_UTF_SIZE];
typedef union {void *ptr; int id;} nk_handle;
struct nk_image {nk_handle handle;unsigned short w,h;unsigned short region[4];};
struct nk_scroll {unsigned short x, y;};
enum nk_heading {NK_UP, NK_RIGHT, NK_DOWN, NK_LEFT};


enum nk_button_behavior {NK_BUTTON_DEFAULT,NK_BUTTON_REPEATER};
enum nk_modify          {NK_FIXED=nk_false,NK_MODIFIABLE=nk_true};
enum nk_orientation     {NK_VERTICAL,NK_HORIZONTAL};
enum nk_collapse_states {NK_MINIMIZED=nk_false,NK_MAXIMIZED = nk_true};
enum nk_show_states     {NK_HIDDEN=nk_false,NK_SHOWN=nk_true};
enum nk_chart_type      {NK_CHART_LINES,NK_CHART_COLUMN,NK_CHART_MAX};
enum nk_chart_event     {NK_CHART_HOVERING=0x01, NK_CHART_CLICKED=0x02};
enum nk_color_format    {NK_RGB, NK_RGBA};
enum nk_popup_type      {NK_POPUP_STATIC,NK_POPUP_DYNAMIC};
enum nk_layout_format   {NK_DYNAMIC,NK_STATIC};
enum nk_tree_type       {NK_TREE_NODE,NK_TREE_TAB};
enum nk_anti_aliasing   {NK_ANTI_ALIASING_OFF,NK_ANTI_ALIASING_ON};

enum nk_symbol_type {
    NK_SYMBOL_NONE,
    NK_SYMBOL_X,
    NK_SYMBOL_UNDERSCORE,
    NK_SYMBOL_CIRCLE,
    NK_SYMBOL_CIRCLE_FILLED,
    NK_SYMBOL_RECT,
    NK_SYMBOL_RECT_FILLED,
    NK_SYMBOL_TRIANGLE_UP,
    NK_SYMBOL_TRIANGLE_DOWN,
    NK_SYMBOL_TRIANGLE_LEFT,
    NK_SYMBOL_TRIANGLE_RIGHT,
    NK_SYMBOL_PLUS,
    NK_SYMBOL_MINUS,
    NK_SYMBOL_MAX
};

enum nk_keys {
    NK_KEY_NONE,
    NK_KEY_SHIFT,
    NK_KEY_CTRL,
    NK_KEY_DEL,
    NK_KEY_ENTER,
    NK_KEY_TAB,
    NK_KEY_BACKSPACE,
    NK_KEY_COPY,
    NK_KEY_CUT,
    NK_KEY_PASTE,
    NK_KEY_UP,
    NK_KEY_DOWN,
    NK_KEY_LEFT,
    NK_KEY_RIGHT,
    NK_KEY_TEXT_INSERT_MODE,
    NK_KEY_TEXT_LINE_START,
    NK_KEY_TEXT_LINE_END,
    NK_KEY_TEXT_START,
    NK_KEY_TEXT_END,
    NK_KEY_TEXT_UNDO,
    NK_KEY_TEXT_REDO,
    NK_KEY_TEXT_WORD_LEFT,
    NK_KEY_TEXT_WORD_RIGHT,
    NK_KEY_MAX
};

enum nk_buttons {
    NK_BUTTON_LEFT,
    NK_BUTTON_MIDDLE,
    NK_BUTTON_RIGHT,
    NK_BUTTON_MAX
};

enum nk_panel_flags {
    NK_WINDOW_BORDER        = NK_FLAG(0), /* Draws a border around the window to visually separate the window * from the background */
    NK_WINDOW_BORDER_HEADER = NK_FLAG(1), /* Draws a border between window header and body */
    NK_WINDOW_MOVABLE       = NK_FLAG(2), /* The movable flag indicates that a window can be moved by user input or * by dragging the window header */
    NK_WINDOW_SCALABLE      = NK_FLAG(3), /* The scalable flag indicates that a window can be scaled by user input * by dragging a scaler icon at the button of the window */
    NK_WINDOW_CLOSABLE      = NK_FLAG(4), /* adds a closable icon into the header */
    NK_WINDOW_MINIMIZABLE   = NK_FLAG(5), /* adds a minimize icon into the header */
    NK_WINDOW_DYNAMIC       = NK_FLAG(6), /* special window type growing up in height while being filled to a * certain maximum height */
    NK_WINDOW_NO_SCROLLBAR  = NK_FLAG(7), /* Removes the scrollbar from the window */
    NK_WINDOW_TITLE         = NK_FLAG(8) /* Forces a header at the top at the window showing the title */
};

/* text alignment */
enum nk_text_align {
    NK_TEXT_ALIGN_LEFT        = 0x01,
    NK_TEXT_ALIGN_CENTERED    = 0x02,
    NK_TEXT_ALIGN_RIGHT       = 0x04,
    NK_TEXT_ALIGN_TOP         = 0x08,
    NK_TEXT_ALIGN_MIDDLE      = 0x10,
    NK_TEXT_ALIGN_BOTTOM      = 0x20
};

/*
enum nk_text_alignment {
    NK_TEXT_LEFT        = NK_TEXT_ALIGN_MIDDLE|NK_TEXT_ALIGN_LEFT,
    NK_TEXT_CENTERED    = NK_TEXT_ALIGN_MIDDLE|NK_TEXT_ALIGN_CENTERED,
    NK_TEXT_RIGHT       = NK_TEXT_ALIGN_MIDDLE|NK_TEXT_ALIGN_RIGHT
};
*/

/*
#define NK_TEXT_LEFT		NK_TEXT_ALIGN_MIDDLE | NK_TEXT_ALIGN_LEFT
#define K_TEXT_CENTERED		NK_TEXT_ALIGN_MIDDLE | NK_TEXT_ALIGN_CENTERED
#define NK_TEXT_RIGHT		NK_TEXT_ALIGN_MIDDLE | NK_TEXT_ALIGN_RIGHT
*/

enum nk_edit_flags {
    NK_EDIT_DEFAULT                 = 0,
    NK_EDIT_READ_ONLY               = NK_FLAG(0),
    NK_EDIT_AUTO_SELECT             = NK_FLAG(1),
    NK_EDIT_SIG_ENTER               = NK_FLAG(2),
    NK_EDIT_ALLOW_TAB               = NK_FLAG(3),
    NK_EDIT_NO_CURSOR               = NK_FLAG(4),
    NK_EDIT_SELECTABLE              = NK_FLAG(5),
    NK_EDIT_CLIPBOARD               = NK_FLAG(6),
    NK_EDIT_CTRL_ENTER_NEWLINE      = NK_FLAG(7),
    NK_EDIT_NO_HORIZONTAL_SCROLL    = NK_FLAG(8),
    NK_EDIT_ALWAYS_INSERT_MODE      = NK_FLAG(9),
    NK_EDIT_MULTILINE               = NK_FLAG(11)
};

/*
enum nk_edit_types {
    NK_EDIT_SIMPLE  = NK_EDIT_ALWAYS_INSERT_MODE,
    NK_EDIT_FIELD   = NK_EDIT_SIMPLE|NK_EDIT_SELECTABLE,
    NK_EDIT_BOX     = NK_EDIT_ALWAYS_INSERT_MODE| NK_EDIT_SELECTABLE|
                        NK_EDIT_MULTILINE|NK_EDIT_ALLOW_TAB
};
*/

/*
#define NK_EDIT_SIMPLE  NK_EDIT_ALWAYS_INSERT_MODE
#define NK_EDIT_FIELD  NK_EDIT_SIMPLE | NK_EDIT_SELECTABLE
#define NK_EDIT_BOX  NK_EDIT_ALWAYS_INSERT_MODE| NK_EDIT_SELECTABLE | NK_EDIT_MULTILINE | NK_EDIT_ALLOW_TAB
*/

enum nk_edit_events {
    NK_EDIT_ACTIVE      = NK_FLAG(0), /* edit widget is currently being modified */
    NK_EDIT_INACTIVE    = NK_FLAG(1), /* edit widget is not active and is not being modified */
    NK_EDIT_ACTIVATED   = NK_FLAG(2), /* edit widget went from state inactive to state active */
    NK_EDIT_DEACTIVATED = NK_FLAG(3), /* edit widget went from state active to state inactive */
    NK_EDIT_COMMITED    = NK_FLAG(4) /* edit widget has received an enter and lost focus */
};

struct nk_mouse_button {
    int down;
    unsigned int clicked;
    struct nk_vec2 clicked_pos;
};

struct nk_mouse {
    struct nk_mouse_button buttons[NK_BUTTON_MAX];
    struct nk_vec2 pos;
    struct nk_vec2 prev;
    struct nk_vec2 delta;
    float scroll_delta;
};

struct nk_key {
    int down;
    unsigned int clicked;
};

struct nk_keyboard {
    struct nk_key keys[NK_KEY_MAX];
    char text[NK_INPUT_MAX];
    int text_len;
};

struct nk_input {
    struct nk_keyboard keyboard;
    struct nk_mouse mouse;
};

struct nk_context {
    struct nk_input input;
    struct nk_style style;
//    struct nk_buffer memory;
//    struct nk_clipboard clip;
    nk_flags last_widget_state;
};

enum nk_style_header_align {
    NK_HEADER_LEFT,
    NK_HEADER_RIGHT
};

struct nk_panel {
    nk_flags flags;
};

/* User Input */
NK_API void nk_input_begin(struct nk_context*);
NK_API void nk_input_motion(struct nk_context*, int x, int y);
NK_API void nk_input_key(struct nk_context*, enum nk_keys, int down);
NK_API void nk_input_button(struct nk_context*, enum nk_buttons, int x, int y, int down);
NK_API void nk_input_scroll(struct nk_context*, float y);
NK_API void nk_input_char(struct nk_context*, char c);
NK_API void nk_input_end(struct nk_context*);

/* window */
NK_API bool nk_begin(struct nk_context *ctx, struct nk_panel *layout, const char *title, struct nk_rect bounds, nk_flags flags);
NK_API void nk_end(struct nk_context*);

/* Layout */
NK_API void nk_layout_row_dynamic(struct nk_context*, float height, int cols);
NK_API void nk_layout_row_static(struct nk_context*, float height, int item_width, int cols);
NK_API void nk_layout_row_begin(struct nk_context*, enum nk_layout_format, float row_height, int cols);
NK_API void nk_layout_row_push(struct nk_context*, float value);
NK_API void nk_layout_row_end(struct nk_context*);
NK_API void nk_layout_row(struct nk_context*, enum nk_layout_format, float height, int cols, const float *ratio);

NK_API void nk_layout_space_begin(struct nk_context*, enum nk_layout_format, float height, int widget_count);
NK_API void nk_layout_space_push(struct nk_context*, struct nk_rect);
NK_API void nk_layout_space_end(struct nk_context*);

NK_API struct nk_rect nk_layout_space_bounds(struct nk_context*);
NK_API struct nk_vec2 nk_layout_space_to_screen(struct nk_context*, struct nk_vec2);
NK_API struct nk_vec2 nk_layout_space_to_local(struct nk_context*, struct nk_vec2);
NK_API struct nk_rect nk_layout_space_rect_to_screen(struct nk_context*, struct nk_rect);
NK_API struct nk_rect nk_layout_space_rect_to_local(struct nk_context*, struct nk_rect);

/* Label */
NK_API void nk_label(struct nk_context*, const char*, nk_flags);
NK_API void nk_label_colored(struct nk_context*, const char*, nk_flags align, struct nk_color);
NK_API void nk_label_wrap(struct nk_context*, const char*);
NK_API void nk_label_colored_wrap(struct nk_context*, const char*, struct nk_color);

/* Popups */
NK_API bool nk_popup_begin(struct nk_context*, struct nk_panel*, enum nk_popup_type, const char*, nk_flags, struct nk_rect bounds);
NK_API void nk_popup_close(struct nk_context*);
NK_API void nk_popup_end(struct nk_context*);

/* Widgets: Buttons */
NK_API bool nk_button_text(struct nk_context *ctx, const char *title, int len, enum nk_button_behavior);
NK_API bool nk_button_label(struct nk_context *ctx, const char *title, enum nk_button_behavior);
NK_API int  nk_button_image(struct nk_context*, struct nk_image img, enum nk_button_behavior);
NK_API int  nk_button_image_label(struct nk_context*, struct nk_image img, const char*, nk_flags text_alignment, enum nk_button_behavior);
NK_API int  nk_button_image_text(struct nk_context*, struct nk_image img, const char*, int, nk_flags alignment, enum nk_button_behavior);

/* Widgets: Radio */
NK_API bool nk_radio_label(struct nk_context*, const char*, int *active);
NK_API bool nk_radio_text(struct nk_context*, const char*, int, int *active);
NK_API bool nk_option_label(struct nk_context*, const char*, bool active);
NK_API bool nk_option_text(struct nk_context*, const char*, int, bool active);

/* Menu */
NK_API void nk_menubar_begin(struct nk_context*);
NK_API void nk_menubar_end(struct nk_context*);
NK_API bool nk_menu_begin_text(struct nk_context*, struct nk_panel*, const char*, int, nk_flags align, float width);
NK_API bool nk_menu_begin_label(struct nk_context*, struct nk_panel*, const char*, nk_flags align, float width);
NK_API bool nk_menu_begin_image(struct nk_context*, struct nk_panel*, const char*, struct nk_image, float width);
NK_API bool nk_menu_begin_image_text(struct nk_context*, struct nk_panel*, const char*, int,nk_flags align,struct nk_image, float width);
NK_API bool nk_menu_begin_image_label(struct nk_context*, struct nk_panel*, const char*, nk_flags align,struct nk_image, float width);
NK_API bool nk_menu_begin_symbol(struct nk_context*, struct nk_panel*, const char*, enum nk_symbol_type, float width);
NK_API bool nk_menu_begin_symbol_text(struct nk_context*, struct nk_panel*, const char*, int,nk_flags align,enum nk_symbol_type, float width);
NK_API bool nk_menu_begin_symbol_label(struct nk_context*, struct nk_panel*, const char*, nk_flags align,enum nk_symbol_type, float width);
NK_API bool nk_menu_item_text(struct nk_context*, const char*, int,nk_flags align);
NK_API bool nk_menu_item_label(struct nk_context*, const char*, nk_flags alignment);
NK_API bool nk_menu_item_image_label(struct nk_context*, struct nk_image, const char*, nk_flags alignment);
NK_API bool nk_menu_item_image_text(struct nk_context*, struct nk_image, const char*, int len, nk_flags alignment);
NK_API bool nk_menu_item_symbol_text(struct nk_context*, enum nk_symbol_type, const char*, int, nk_flags alignment);
NK_API bool nk_menu_item_symbol_label(struct nk_context*, enum nk_symbol_type, const char*, nk_flags alignment);
NK_API void nk_menu_close(struct nk_context*);
NK_API void nk_menu_end(struct nk_context*);

enum nk_command_type {
    NK_COMMAND_NOP,
    NK_COMMAND_SCISSOR,
    NK_COMMAND_LINE,
    NK_COMMAND_CURVE,
    NK_COMMAND_RECT,
    NK_COMMAND_RECT_FILLED,
    NK_COMMAND_RECT_MULTI_COLOR,
    NK_COMMAND_CIRCLE,
    NK_COMMAND_CIRCLE_FILLED,
    NK_COMMAND_ARC,
    NK_COMMAND_ARC_FILLED,
    NK_COMMAND_TRIANGLE,
    NK_COMMAND_TRIANGLE_FILLED,
    NK_COMMAND_POLYGON,
    NK_COMMAND_POLYGON_FILLED,
    NK_COMMAND_POLYLINE,
    NK_COMMAND_TEXT,
    NK_COMMAND_IMAGE
};

%include "arrays_java.i"
%apply int[] {int *};
%apply float[] {float *};
%apply long[] {unsigned long *};

/* Widgets: Property */
NK_API void nk_property_float(struct nk_context *layout, const char *name, float min, float *val, float max, float step, float inc_per_pixel);
NK_API void nk_property_int(struct nk_context *layout, const char *name, int min, int *val, int max, int step, int inc_per_pixel);
NK_API float nk_propertyf(struct nk_context *layout, const char *name, float min, float val, float max, float step, float inc_per_pixel);
NK_API int nk_propertyi(struct nk_context *layout, const char *name, int min, int val, int max, int step, int inc_per_pixel);

/* Widgets: Progressbar */
NK_API int nk_progress(struct nk_context*, nk_size *cur, nk_size max, int modifyable);
NK_API nk_size nk_prog(struct nk_context*, nk_size cur, nk_size max, int modifyable);

/* Widgets: Slider */
NK_API float nk_slide_float(struct nk_context*, float min, float val, float max, float step);
NK_API int nk_slide_int(struct nk_context*, int min, int val, int max, int step);
NK_API int nk_slider_float(struct nk_context*, float min, float *val, float max, float step);
NK_API int nk_slider_int(struct nk_context*, int min, int *val, int max, int step);

/* Widgets: Checkbox */
NK_API int nk_check_label(struct nk_context*, const char*, int active);
NK_API int nk_check_text(struct nk_context*, const char*, int,int active);
NK_API unsigned nk_check_flags_label(struct nk_context*, const char*, unsigned int flags, unsigned int value);
NK_API unsigned nk_check_flags_text(struct nk_context*, const char*, int, unsigned int flags, unsigned int value);
NK_API int nk_checkbox_label(struct nk_context*, const char*, int *active);
NK_API int nk_checkbox_text(struct nk_context*, const char*, int, int *active);
NK_API int nk_checkbox_flags_label(struct nk_context*, const char*, unsigned int *flags, unsigned int value);
NK_API int nk_checkbox_flags_text(struct nk_context*, const char*, int, unsigned int *flags, unsigned int value);

/* Widgets: Combobox */
NK_API int nk_combo_separator(struct nk_context*, const char *items_separated_by_separator, int separator, int selected, int count, int item_height);
NK_API int nk_combo_string(struct nk_context*, const char *items_separated_by_zeros, int selected, int count, int item_height);
//NK_API int nk_combo_callback(struct nk_context*, void(*item_getter)(void*, int, const char**), void *userdata, int selected, int count, int item_height);
NK_API void nk_combobox_string(struct nk_context*, const char *items_separated_by_zeros, int *selected, int count, int item_height);
NK_API void nk_combobox_separator(struct nk_context*, const char *items_separated_by_separator, int separator,int *selected, int count, int item_height);
//NK_API void nk_combobox_callback(struct nk_context*, void(*item_getter)(void*, int, const char**), void*, int *selected, int count, int item_height);

/* Widgets: Combobox: abstract */
NK_API int nk_combo_begin_text(struct nk_context*, struct nk_panel*, const char *selected, int, int max_height);
NK_API int nk_combo_begin_label(struct nk_context*, struct nk_panel*, const char *selected, int max_height);
NK_API int nk_combo_begin_color(struct nk_context*, struct nk_panel*, struct nk_color color, int max_height);
NK_API int nk_combo_begin_symbol(struct nk_context*, struct nk_panel*, enum nk_symbol_type,  int max_height);
NK_API int nk_combo_begin_symbol_label(struct nk_context*, struct nk_panel*, const char *selected, enum nk_symbol_type, int height);
NK_API int nk_combo_begin_symbol_text(struct nk_context*, struct nk_panel*, const char *selected, int, enum nk_symbol_type, int height);
NK_API int nk_combo_begin_image(struct nk_context*, struct nk_panel*, struct nk_image img,  int max_height);
NK_API int nk_combo_begin_image_label(struct nk_context*, struct nk_panel*, const char *selected, struct nk_image, int height);
NK_API int nk_combo_begin_image_text(struct nk_context*, struct nk_panel*, const char *selected, int, struct nk_image, int height);
NK_API int nk_combo_item_label(struct nk_context*, const char*, nk_flags alignment);
NK_API int nk_combo_item_text(struct nk_context*, const char*,int, nk_flags alignment);
NK_API int nk_combo_item_image_label(struct nk_context*, struct nk_image, const char*, nk_flags alignment);
NK_API int nk_combo_item_image_text(struct nk_context*, struct nk_image, const char*, int,nk_flags alignment);
NK_API int nk_combo_item_symbol_label(struct nk_context*, enum nk_symbol_type, const char*, nk_flags alignment);
NK_API int nk_combo_item_symbol_text(struct nk_context*, enum nk_symbol_type, const char*, int, nk_flags alignment);
NK_API void nk_combo_close(struct nk_context*);
NK_API void nk_combo_end(struct nk_context*);

/* Widgets: Contextual */
NK_API int nk_contextual_begin(struct nk_context*, struct nk_panel*, nk_flags, struct nk_vec2, struct nk_rect trigger_bounds);
NK_API int nk_contextual_item_text(struct nk_context*, const char*, int,nk_flags align);
NK_API int nk_contextual_item_label(struct nk_context*, const char*, nk_flags align);
NK_API int nk_contextual_item_image_label(struct nk_context*, struct nk_image, const char*, nk_flags alignment);
NK_API int nk_contextual_item_image_text(struct nk_context*, struct nk_image, const char*, int len, nk_flags alignment);
NK_API int nk_contextual_item_symbol_label(struct nk_context*, enum nk_symbol_type, const char*, nk_flags alignment);
NK_API int nk_contextual_item_symbol_text(struct nk_context*, enum nk_symbol_type, const char*, int, nk_flags alignment);
NK_API void nk_contextual_close(struct nk_context*);
NK_API void nk_contextual_end(struct nk_context*);

extern int initialize(int w, int h);
NK_API void nk_headless_init(struct nk_context* ctx, int w, int h, int max_char_width, int font_height);
NK_API void nk_headless_render(struct nk_context* ctx, int* draw_buffer);

%include various.i

/* Widgets: TextEdit */
//NK_API nk_flags nk_edit_string(struct nk_context*, nk_flags, char *SBUF, int *len, int max, nk_filter);
NK_API nk_flags nk_edit_string2(struct nk_context*, nk_flags, char *BYTE, int *len, int max);
NK_API nk_flags nk_edit_buffer(struct nk_context*, nk_flags, struct nk_text_edit*, nk_filter);

NK_API int nk_combo(struct nk_context*, const char **STRING_ARRAY, int count, int selected, int item_height);
NK_API void nk_combobox(struct nk_context*, const char **STRING_ARRAY, int count, int *selected, int item_height);

/*
 * Notes below
 */

/*
 * How map char* to a Java array instead of a String
 *
 * default behaviour is that of input arg, Java cannot return a value in a
 * string argument, so any changes made by f1(char*) will not be seen in the Java
 * string passed to the f1 function.
*/

//void f1(char *s);
//%include various.i
/* use the BYTE argout typemap to get around this. Changes in the string by
 * f2 can be seen in Java. */
//void f2(char *BYTE);

/* Alternative approach uses a StringBuffer typemap for argout */

/* Define the types to use in the generated JNI C code and Java code */
%typemap(jni) char *SBUF "jobject"
%typemap(jtype) char *SBUF "StringBuffer"
%typemap(jstype) char *SBUF "StringBuffer"

/* How to convert Java(JNI) type to requested C type */
%typemap(in) char *SBUF {

  $1 = NULL;
  if($input != NULL) {
    /* Get the String from the StringBuffer */
    jmethodID setLengthID;
    jclass sbufClass = (*jenv)->GetObjectClass(jenv, $input);
    jmethodID toStringID = (*jenv)->GetMethodID(jenv, sbufClass, "toString", "()Ljava/lang/String;");
    jstring js = (jstring) (*jenv)->CallObjectMethod(jenv, $input, toStringID);

    /* Convert the String to a C string */
    const char *pCharStr = (*jenv)->GetStringUTFChars(jenv, js, 0);

    /* Take a copy of the C string as the typemap is for a non const C string */
    jmethodID capacityID = (*jenv)->GetMethodID(jenv, sbufClass, "capacity", "()I");
    jint capacity = (*jenv)->CallIntMethod(jenv, $input, capacityID);
    $1 = (char *) malloc(capacity+1);
    strcpy($1, pCharStr);

    /* Release the UTF string we obtained with GetStringUTFChars */
    (*jenv)->ReleaseStringUTFChars(jenv,  js, pCharStr);

    /* Zero the original StringBuffer, so we can replace it with the result */
    setLengthID = (*jenv)->GetMethodID(jenv, sbufClass, "setLength", "(I)V");
    (*jenv)->CallVoidMethod(jenv, $input, setLengthID, (jint) 0);
  }
}

/* How to convert the C type to the Java(JNI) type */
%typemap(argout) char *SBUF {

  if($1 != NULL) {
    /* Append the result to the empty StringBuffer */
    jstring newString = (*jenv)->NewStringUTF(jenv, $1);
    jclass sbufClass = (*jenv)->GetObjectClass(jenv, $input);
    jmethodID appendStringID = (*jenv)->GetMethodID(jenv, sbufClass, "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    (*jenv)->CallObjectMethod(jenv, $input, appendStringID, newString);

    /* Clean up the string object, no longer needed */
    free($1);
    $1 = NULL;
  }
}
/* Prevent the default freearg typemap from being used */
%typemap(freearg) char *SBUF ""

/* Convert the jstype to jtype typemap type */
%typemap(javain) char *SBUF "$javainput"

/* apply the new typemap to our function */
//void f3(char *SBUF);

#include <string.h>
#include <stdio.h>

#define NK_INCLUDE_DEFAULT_ALLOCATOR
#include "nuklear_headless.h"

#define NK_IMPLEMENTATION
#include "nuklear.h"

#ifndef MAX
#define MAX(a,b) ((a) < (b) ? (b) : (a))
#endif

#define nk_headless_MAX_POINTS 128

/*
struct nk_headless_Surface {
	long handle;
	int w;
	int h;
};

typedef struct nk_headless_Surface Headless_Surface;

static Headless_Surface screen_surface;

static struct nk_headless {
    Headless_Surface *screen_surface;
    struct nk_context ctx;
} headless;
*/

static nk_headless_Font *headless_font;
static struct nk_user_font headless_user_font;  /* must outlive ctx */
/* static SDL_Rect sdl_clip_rect; */

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

#define BUFFER_INDEX_CMD_COUNT 0
#define BUFFER_INDEX_CMD_START 1

/*
 * CMD_COUNT NK_COMMAND[CMD_SIZE ...] ...
 */
static int buffer_index;
static int command_count;
static int draw_buffer_max = 499000;


#define NK_BUF_CHECK(need) do { if (buffer_index + (need) > draw_buffer_max) { \
    fprintf(stderr, "NK BUFFER OVERFLOW at index %d (need %d, max %d) cmd_count=%d\n", \
        buffer_index, (need), draw_buffer_max, command_count); return; } } while(0)

static void
nk_headless_scissor(int *draw_buffer, float x, float y, float w, float h)
{
	NK_BUF_CHECK(6);
	draw_buffer[buffer_index++] = NK_COMMAND_SCISSOR;
	draw_buffer[buffer_index++] = NK_COMMAND_SCISSOR_SIZE;
	draw_buffer[buffer_index++] = x;
	draw_buffer[buffer_index++] = y;
	draw_buffer[buffer_index++] = w;
	draw_buffer[buffer_index++] = h;

    /*
	sdl_clip_rect.x = x;
    sdl_clip_rect.y = y;
    sdl_clip_rect.w = w  + 1;
    sdl_clip_rect.h = h;
    SDL_SetClipRect(surface, &sdl_clip_rect);
	*/
}

static void
nk_headless_stroke_line(int *draw_buffer, short x0, short y0, short x1,
    short y1, unsigned int line_thickness, struct nk_color col)
{
	NK_BUF_CHECK(11);
	draw_buffer[buffer_index++] = NK_COMMAND_LINE;
	draw_buffer[buffer_index++] = NK_COMMAND_LINE_SIZE;
	draw_buffer[buffer_index++] = x0;
	draw_buffer[buffer_index++] = y0;
	draw_buffer[buffer_index++] = x1;
	draw_buffer[buffer_index++] = y1;
	draw_buffer[buffer_index++] = line_thickness;
	draw_buffer[buffer_index++] = col.a;
	draw_buffer[buffer_index++] = col.r;
	draw_buffer[buffer_index++] = col.g;
	draw_buffer[buffer_index++] = col.b;

    /* thickLineRGBA(surface, x0, y0, x1, y1, line_thickness, col.r, col.g, col.b, col.a); */
}

static void
nk_headless_stroke_rect(int *draw_buffer, short x, short y, unsigned short w,
    unsigned short h, unsigned short r, unsigned short line_thickness, struct nk_color col)
{
	NK_BUF_CHECK(12);
	draw_buffer[buffer_index++] = NK_COMMAND_RECT;
	draw_buffer[buffer_index++] = NK_COMMAND_RECT_SIZE;
	draw_buffer[buffer_index++] = x;
	draw_buffer[buffer_index++] = y;
	draw_buffer[buffer_index++] = w;
	draw_buffer[buffer_index++] = h;
	draw_buffer[buffer_index++] = r;
	draw_buffer[buffer_index++] = line_thickness;
	draw_buffer[buffer_index++] = col.a;
	draw_buffer[buffer_index++] = col.r;
	draw_buffer[buffer_index++] = col.g;
	draw_buffer[buffer_index++] = col.b;


	/*
    if (r == 0) {
        rectangleRGBA(surface, x, y, x + w, y + h, col.r, col.g, col.b, col.a);
    } else {
        roundedRectangleRGBA(surface, x, y, x + w, y + h, r, col.r, col.g, col.b, col.a);
    }
*/
}

static void
nk_headless_fill_rect(int *draw_buffer, short x, short y, unsigned short w,
    unsigned short h, unsigned short r, struct nk_color col)
{
	NK_BUF_CHECK(11);
	draw_buffer[buffer_index++] = NK_COMMAND_RECT_FILLED;
	draw_buffer[buffer_index++] = NK_COMMAND_RECT_FILLED_SIZE;
	draw_buffer[buffer_index++] = x;
	draw_buffer[buffer_index++] = y;
	draw_buffer[buffer_index++] = w;
	draw_buffer[buffer_index++] = h;
	draw_buffer[buffer_index++] = r;
	draw_buffer[buffer_index++] = col.a;
	draw_buffer[buffer_index++] = col.r;
	draw_buffer[buffer_index++] = col.g;
	draw_buffer[buffer_index++] = col.b;

/*
    if (r == 0) {
        boxRGBA(surface, x, y, x + w, y + h, col.r, col.g, col.b, col.a);
    } else {
        roundedBoxRGBA(surface, x, y, x + w, y + h, r, col.r, col.g, col.b, col.a);
    }
	*/
}

static void
nk_headless_fill_triangle(int *draw_buffer, short x0, short y0, short x1, short y1, short x2, short y2, struct nk_color col)
{
		NK_BUF_CHECK(12);
		draw_buffer[buffer_index++] = NK_COMMAND_TRIANGLE_FILLED;
		draw_buffer[buffer_index++] = NK_COMMAND_TRIANGLE_FILLED_SIZE;
		draw_buffer[buffer_index++] = x0;
		draw_buffer[buffer_index++] = y0;
		draw_buffer[buffer_index++] = x1;
		draw_buffer[buffer_index++] = y1;
		draw_buffer[buffer_index++] = x2;
		draw_buffer[buffer_index++] = y2;
		draw_buffer[buffer_index++] = col.a;
		draw_buffer[buffer_index++] = col.r;
		draw_buffer[buffer_index++] = col.g;
		draw_buffer[buffer_index++] = col.b;
/*
    filledTrigonRGBA(surface, x0, y0, x1, y1, x2, y2, col.r, col.g, col.b, col.a);
	*/
}

static void
nk_headless_stroke_triangle(int *draw_buffer, short x0, short y0, short x1,
    short y1, short x2, short y2, unsigned short line_thickness, struct nk_color col)
{
	NK_BUF_CHECK(13);
	draw_buffer[buffer_index++] = NK_COMMAND_TRIANGLE;
	draw_buffer[buffer_index++] = NK_COMMAND_TRIANGLE_SIZE;
	draw_buffer[buffer_index++] = x0;
	draw_buffer[buffer_index++] = y0;
	draw_buffer[buffer_index++] = x1;
	draw_buffer[buffer_index++] = y1;
	draw_buffer[buffer_index++] = x2;
	draw_buffer[buffer_index++] = y2;
	draw_buffer[buffer_index++] = line_thickness;
	draw_buffer[buffer_index++] = col.a;
	draw_buffer[buffer_index++] = col.r;
	draw_buffer[buffer_index++] = col.g;
	draw_buffer[buffer_index++] = col.b;


	/*
    aatrigonRGBA(surface, x0, y0, x1, y1, x2, y2, col.r, col.g, col.b, col.a);
	*/
}

static void
nk_headless_fill_polygon(int *draw_buffer, const struct nk_vec2i *pnts, int count, struct nk_color col)
{

	/* not used  by any widget currently */

    /*
	Sint16 p_x[nk_headless_MAX_POINTS];
    Sint16 p_y[nk_headless_MAX_POINTS];
    int i;
    for (i = 0; (i < count) && (i < nk_headless_MAX_POINTS); ++i) {
        p_x[i] = pnts[i].x;
        p_y[i] = pnts[i].y;
    }
    filledPolygonRGBA (surface, (Sint16 *)p_x, (Sint16 *)p_y, count, col.r, col.g, col.b, col.a);
	*/
}

static void
nk_headless_stroke_polygon(int *draw_buffer, const struct nk_vec2i *pnts, int count,
    unsigned short line_thickness, struct nk_color col)
{

	/* not used  by any widget currently */

    /* Note: thickness is not used by default */
	/*
    Sint16 p_x[nk_headless_MAX_POINTS];
    Sint16 p_y[nk_headless_MAX_POINTS];
    int i;
    for (i = 0; (i < count) && (i < nk_headless_MAX_POINTS); ++i) {
        p_x[i] = pnts[i].x;
        p_y[i] = pnts[i].y;
    }
    aapolygonRGBA(surface, (Sint16 *)p_x, (Sint16 *)p_y, count, col.r, col.g, col.b, col.a);
	*/
}

static void
nk_headless_stroke_polyline(int *draw_buffer, const struct nk_vec2i *pnts,
    int count, unsigned short line_thickness, struct nk_color col)
{

	/* not used  by any widget currently */

	/*
    int x0, y0, x1, y1;
    if (count == 1) {
        x0 = pnts[0].x;
        y0 = pnts[0].y;
        x1 = x0;
        y1 = y0;
        thickLineRGBA(surface, x0, y0, x1, y1, line_thickness, col.r, col.g, col.b, col.a);
    } else if (count >= 2) {
        int i;
        for (i = 0; i < (count - 1); i++) {
            x0 = pnts[i].x;
            y0 = pnts[i].y;
            x1 = pnts[i + 1].x;
            y1 = pnts[i + 1].y;
            thickLineRGBA(surface, x0, y0, x1, y1, line_thickness, col.r, col.g, col.b, col.a);
        }
    }*/
}

static void
nk_headless_fill_circle(int *draw_buffer, short x, short y, unsigned short w,
    unsigned short h, struct nk_color col)
{
	NK_BUF_CHECK(10);
	draw_buffer[buffer_index++] = NK_COMMAND_CIRCLE_FILLED;
	draw_buffer[buffer_index++] = NK_COMMAND_CIRCLE_FILLED_SIZE;
	draw_buffer[buffer_index++] = x;
	draw_buffer[buffer_index++] = y;
	draw_buffer[buffer_index++] = w;
	draw_buffer[buffer_index++] = h;
	draw_buffer[buffer_index++] = col.a;
	draw_buffer[buffer_index++] = col.r;
	draw_buffer[buffer_index++] = col.g;
	draw_buffer[buffer_index++] = col.b;

/*
    filledEllipseRGBA(surface,  x + w /2, y + h /2, w / 2, h / 2, col.r, col.g, col.b, col.a);
	*/
}

static void
nk_headless_stroke_circle(int *draw_buffer, short x, short y, unsigned short w,
    unsigned short h, unsigned short line_thickness, struct nk_color col)
{
	NK_BUF_CHECK(11);
	draw_buffer[buffer_index++] = NK_COMMAND_CIRCLE;
	draw_buffer[buffer_index++] = NK_COMMAND_CIRCLE_SIZE;
	draw_buffer[buffer_index++] = x;
	draw_buffer[buffer_index++] = y;
	draw_buffer[buffer_index++] = w;
	draw_buffer[buffer_index++] = h;
	draw_buffer[buffer_index++] = line_thickness;
	draw_buffer[buffer_index++] = col.a;
	draw_buffer[buffer_index++] = col.r;
	draw_buffer[buffer_index++] = col.g;
	draw_buffer[buffer_index++] = col.b;
	/*
    aaellipseRGBA (surface,  x + w /2, y + h /2, w / 2, h / 2, col.r, col.g, col.b, col.a);
	*/
}

static void
nk_headless_stroke_curve(int *draw_buffer, struct nk_vec2i p1,
    struct nk_vec2i p2, struct nk_vec2i p3, struct nk_vec2i p4, unsigned int num_segments,
    unsigned short line_thickness, struct nk_color col)
{

	/* not used  by any widget currently */

	/*
    unsigned int i_step;
    float t_step;
    struct nk_vec2i last = p1;

    num_segments = MAX(num_segments, 1);
    t_step = 1.0f/(float)num_segments;
    for (i_step = 1; i_step <= num_segments; ++i_step) {
        float t = t_step * (float)i_step;
        float u = 1.0f - t;
        float w1 = u*u*u;
        float w2 = 3*u*u*t;
        float w3 = 3*u*t*t;
        float w4 = t * t *t;
        float x = w1 * p1.x + w2 * p2.x + w3 * p3.x + w4 * p4.x;
        float y = w1 * p1.y + w2 * p2.y + w3 * p3.y + w4 * p4.y;
        nk_headless_stroke_line(surface, last.x, last.y, (short)x, (short)y, line_thickness, col);
        last.x = (short)x; last.y = (short)y;
    }*/
}

static void
nk_headless_draw_text(int *draw_buffer, short x, short y, unsigned short w, unsigned short h,
    const char *text, int len, nk_headless_Font *font, struct nk_color cbg, struct nk_color cfg)
{
	NK_BUF_CHECK(15 + len);
	draw_buffer[buffer_index++] = NK_COMMAND_TEXT;
	draw_buffer[buffer_index++] = NK_COMMAND_TEXT_SIZE + len;
	draw_buffer[buffer_index++] = x;
	draw_buffer[buffer_index++] = y;
	draw_buffer[buffer_index++] = w;
	draw_buffer[buffer_index++] = h;
	draw_buffer[buffer_index++] = len;
	draw_buffer[buffer_index++] = cbg.a;
	draw_buffer[buffer_index++] = cbg.r;
	draw_buffer[buffer_index++] = cbg.g;
	draw_buffer[buffer_index++] = cbg.b;
	draw_buffer[buffer_index++] = cfg.a;
	draw_buffer[buffer_index++] = cfg.r;
	draw_buffer[buffer_index++] = cfg.g;
	draw_buffer[buffer_index++] = cfg.b;

	//fprintf(stderr, "text native %i %i %i %i\n", x, y, w, h);

	int i;
	for (i = 0; i < len; i++) {
		draw_buffer[buffer_index++] = text[i];
	}

	/*
    int i;
    nk_headless_fill_rect(surface, x, y, len * font->width, font->height, 0, cbg);
    for (i = 0; i < len; i++) {
        characterRGBA(surface, x, y, text[i], cfg.r, cfg.g, cfg.b, cfg.a);
        x += font->width;
    }*/
}

static void
interpolate_color(struct nk_color c1, struct nk_color c2, struct nk_color *result, float fraction)
{

    float r = c1.r + (c2.r - c1.r) * fraction;
    float g = c1.g + (c2.g - c1.g) * fraction;
    float b = c1.b + (c2.b - c1.b) * fraction;
    float a = c1.a + (c2.a - c1.a) * fraction;

    result->r = (nk_byte)NK_CLAMP(0, r, 255);
    result->g = (nk_byte)NK_CLAMP(0, g, 255);
    result->b = (nk_byte)NK_CLAMP(0, b, 255);
    result->a = (nk_byte)NK_CLAMP(0, a, 255);
}

static void
nk_headless_fill_rect_multi_color(int *draw_buffer, short x, short y, unsigned short w, unsigned short h,
    struct nk_color left, struct nk_color top,  struct nk_color right, struct nk_color bottom)
{

	/* TODO: only used by nk_draw_color_picker() currently */

//    struct nk_color X1, X2, Y;
//    float fraction_x, fraction_y;
//    int i,j;
//
//    for (j = 0; j < h; j++) {
//        fraction_y = ((float)j) / h;
//        for (i = 0; i < w; i++) {
//            fraction_x = ((float)i) / w;
//            interpolate_color(left, top, &X1, fraction_x);
//            interpolate_color(right, bottom, &X2, fraction_x);
//            interpolate_color(X1, X2, &Y, fraction_y);
//            /*pixelRGBA(surface, x + i, y + j, Y.r, Y.g, Y.b, Y.a);  */
//        }
//    }
}

void
nk_headless_clear(struct nk_color col)
{
	//Headless_Surface *surface = headless.screen_surface;
	//nk_headless_fill_rect(surface, 0, 0, surface->w, surface->h, 0, col);
}

static void
nk_headless_draw_image(int *draw_buffer, short x, short y, unsigned short w, unsigned short h, struct nk_image img) {
	NK_BUF_CHECK(7);
	draw_buffer[buffer_index++] = NK_COMMAND_IMAGE;
	draw_buffer[buffer_index++] = NK_COMMAND_IMAGE_SIZE;
	draw_buffer[buffer_index++] = x;
	draw_buffer[buffer_index++] = y;
	draw_buffer[buffer_index++] = w;
	draw_buffer[buffer_index++] = h;
	draw_buffer[buffer_index++] = img.handle.id;
}

NK_API int*
nk_headless_render(struct nk_context* ctx, int *draw_buffer)
{
    const struct nk_command *cmd;

    command_count = 0;
    buffer_index = BUFFER_INDEX_CMD_START;

//    Headless_Surface *screen_surface = headless.screen_surface;
//    nk_headless_clear(screen_surface, clear);

    nk_foreach(cmd, ctx)
    {
        switch (cmd->type) {
        case NK_COMMAND_NOP: break;
        case NK_COMMAND_SCISSOR: {
            const struct nk_command_scissor *s =(const struct nk_command_scissor*)cmd;
            nk_headless_scissor(draw_buffer, s->x, s->y, s->w, s->h);
            command_count++;
        } break;
        case NK_COMMAND_LINE: {
            const struct nk_command_line *l = (const struct nk_command_line *)cmd;
            nk_headless_stroke_line(draw_buffer, l->begin.x, l->begin.y, l->end.x,
                l->end.y, l->line_thickness, l->color);
            command_count++;
        } break;
        case NK_COMMAND_RECT: {
            const struct nk_command_rect *r = (const struct nk_command_rect *)cmd;
            nk_headless_stroke_rect(draw_buffer, r->x, r->y, r->w, r->h,
                (unsigned short)r->rounding, r->line_thickness, r->color);
            command_count++;
        } break;
        case NK_COMMAND_RECT_FILLED: {
            const struct nk_command_rect_filled *r = (const struct nk_command_rect_filled *)cmd;
            nk_headless_fill_rect(draw_buffer, r->x, r->y, r->w, r->h,
                (unsigned short)r->rounding, r->color);
            command_count++;
        } break;
        case NK_COMMAND_CIRCLE: {
            const struct nk_command_circle *c = (const struct nk_command_circle *)cmd;
            nk_headless_stroke_circle(draw_buffer, c->x, c->y, c->w, c->h, c->line_thickness, c->color);
            command_count++;
        } break;
        case NK_COMMAND_CIRCLE_FILLED: {
            const struct nk_command_circle_filled *c = (const struct nk_command_circle_filled *)cmd;
            nk_headless_fill_circle(draw_buffer, c->x, c->y, c->w, c->h, c->color);
            command_count++;
        } break;
        case NK_COMMAND_TRIANGLE: {
            const struct nk_command_triangle*t = (const struct nk_command_triangle*)cmd;
            nk_headless_stroke_triangle(draw_buffer, t->a.x, t->a.y, t->b.x, t->b.y,
                t->c.x, t->c.y, t->line_thickness, t->color);
            command_count++;
        } break;
        case NK_COMMAND_TRIANGLE_FILLED: {
            const struct nk_command_triangle_filled *t = (const struct nk_command_triangle_filled *)cmd;
            nk_headless_fill_triangle(draw_buffer, t->a.x, t->a.y, t->b.x, t->b.y, t->c.x, t->c.y, t->color);
            command_count++;
        } break;
        case NK_COMMAND_POLYGON: {
            const struct nk_command_polygon *p =(const struct nk_command_polygon*)cmd;
            nk_headless_stroke_polygon(draw_buffer, p->points, p->point_count, p->line_thickness,p->color);
        } break;
        case NK_COMMAND_POLYGON_FILLED: {
            const struct nk_command_polygon_filled *p = (const struct nk_command_polygon_filled *)cmd;
            nk_headless_fill_polygon(draw_buffer, p->points, p->point_count, p->color);
        } break;
        case NK_COMMAND_POLYLINE: {
            const struct nk_command_polyline *p = (const struct nk_command_polyline *)cmd;
            nk_headless_stroke_polyline(draw_buffer, p->points, p->point_count, p->line_thickness, p->color);
        } break;
        case NK_COMMAND_TEXT: {
            const struct nk_command_text *t = (const struct nk_command_text*)cmd;
            nk_headless_draw_text(draw_buffer, t->x, t->y, t->w, t->h,
                (const char*)t->string, t->length,
                (nk_headless_Font*)t->font->userdata.ptr,
                t->background, t->foreground);
            command_count++;
        } break;
        case NK_COMMAND_CURVE: {
            const struct nk_command_curve *q = (const struct nk_command_curve *)cmd;
            nk_headless_stroke_curve(draw_buffer, q->begin, q->ctrl[0], q->ctrl[1],
                q->end, 22, q->line_thickness, q->color);
        } break;
        case NK_COMMAND_RECT_MULTI_COLOR: {
            const struct nk_command_rect_multi_color *r = (const struct nk_command_rect_multi_color *)cmd;
            nk_headless_fill_rect_multi_color(draw_buffer, r->x, r->y, r->w, r->h, r->left, r->top, r->right, r->bottom);
        } break;
		case NK_COMMAND_IMAGE: {
			const struct nk_command_image *q = (const struct nk_command_image *) cmd;
			nk_headless_draw_image(draw_buffer, q->x, q->y, q->w, q->h, q->img);
			command_count++;
		} break;
        case NK_COMMAND_ARC:
        case NK_COMMAND_ARC_FILLED:
        default: break;
        }
    }

    //nk_headless_blit(headless.screen_surface);
    nk_clear(ctx);

    static int render_debug = 0;
    if (render_debug < 5) {
        fprintf(stderr, "nk_headless_render: %d commands, buffer_index=%d\n", command_count, buffer_index);
        render_debug++;
    }

    draw_buffer[BUFFER_INDEX_CMD_COUNT] = command_count;
    return draw_buffer;

}

static void
nk_headless_clipbard_paste(nk_handle usr, struct nk_text_edit *edit)
{
    /* Not supported in SDL 1.2. Use platform specific code.  */
}

static void
nk_headless_clipbard_copy(nk_handle usr, const char *text, int len)
{
    /* Not supported in SDL 1.2. Use platform specific code.  */
}

static float
nk_headless_get_text_width(nk_handle handle, float height, const char *text, int len)
{
    return len * headless_font->width;
}

NK_API void
nk_headless_init(struct nk_context* ctx, int w, int h, int max_char_width, int font_height)
{
    headless_font = (nk_headless_Font*)calloc(1, sizeof(nk_headless_Font));
    headless_font->width = max_char_width;
    headless_font->height = font_height;
    if (!headless_font)
        printf("Can't allocate memory for font\n");

    /* Use static struct — nk_init_default stores a POINTER, not a copy */
    headless_user_font.userdata = nk_handle_ptr(headless_font);
    headless_user_font.height = (float)headless_font->height;
    headless_user_font.width = nk_headless_get_text_width;

    nk_init_default(ctx, &headless_user_font);
    ctx->clip.copy = nk_headless_clipbard_copy;
    ctx->clip.paste = nk_headless_clipbard_paste;
    ctx->clip.userdata = nk_handle_ptr(0);
}

NK_API int
nk_headless_handle_event(void)
{

	printf("nk_headless_handle_event\n");
	return nk_true;

#if 0

    struct nk_context *ctx = &headless.ctx;
    if (evt->type == SDL_VIDEORESIZE) {
        /* Do nothing */
    } else if (evt->type == SDL_KEYUP || evt->type == SDL_KEYDOWN) {
        /* key events */
        int down = evt->type == SDL_KEYDOWN;
        SDLMod state = SDL_GetModState();
        SDLKey sym = evt->key.keysym.sym;

        if (sym == SDLK_RSHIFT || sym == SDLK_LSHIFT) nk_input_key(ctx, NK_KEY_SHIFT, down);
        else if (sym == SDLK_DELETE)    nk_input_key(ctx, NK_KEY_DEL, down);
        else if (sym == SDLK_RETURN)    nk_input_key(ctx, NK_KEY_ENTER, down);
        else if (sym == SDLK_TAB)       nk_input_key(ctx, NK_KEY_TAB, down);
        else if (sym == SDLK_LEFT)      nk_input_key(ctx, NK_KEY_LEFT, down);
        else if (sym == SDLK_RIGHT)     nk_input_key(ctx, NK_KEY_RIGHT, down);
        else if (sym == SDLK_BACKSPACE) nk_input_key(ctx, NK_KEY_BACKSPACE, down);
        else if (sym == SDLK_HOME)      nk_input_key(ctx, NK_KEY_TEXT_START, down);
        else if (sym == SDLK_END)       nk_input_key(ctx, NK_KEY_TEXT_END, down);
        else if (sym == SDLK_SPACE && !down) nk_input_char(ctx, ' ');
        else {
            if (sym == SDLK_c && state == SDLK_LCTRL)
                nk_input_key(ctx, NK_KEY_COPY, down);
            else if (sym == SDLK_v && state == SDLK_LCTRL)
                nk_input_key(ctx, NK_KEY_PASTE, down);
            else if (sym == SDLK_x && state == SDLK_LCTRL)
                nk_input_key(ctx, NK_KEY_CUT, down);
            else if (sym == SDLK_z && state == SDLK_LCTRL)
                nk_input_key(ctx, NK_KEY_TEXT_UNDO, down);
            else if (sym == SDLK_r && state == SDLK_LCTRL)
                nk_input_key(ctx, NK_KEY_TEXT_REDO, down);
            else if (sym == SDLK_LEFT && state == SDLK_LCTRL)
                nk_input_key(ctx, NK_KEY_TEXT_WORD_LEFT, down);
            else if (sym == SDLK_RIGHT && state == SDLK_LCTRL)
                nk_input_key(ctx, NK_KEY_TEXT_WORD_RIGHT, down);
            else if (sym == SDLK_b && state == SDLK_LCTRL)
                nk_input_key(ctx, NK_KEY_TEXT_LINE_START, down);
            else if (sym == SDLK_e && state == SDLK_LCTRL)
                nk_input_key(ctx, NK_KEY_TEXT_LINE_END, down);
            else if (!down) {
                /* This demo does not provide full unicode support since the default
                 * sdl1.2 font only allows runes in range 0-255. But this demo
                 * already is quite limited and not really meant for full blown Apps
                 * anyway. So I think ASCII support for Debugging Tools should be enough */
                if (sym >= SDLK_0 && sym <= SDLK_9) {
                    nk_rune rune = '0' + sym - SDLK_0;
                    nk_input_unicode(ctx, rune);
                } else if (sym >= SDLK_a && sym <= SDLK_z) {
                    nk_rune rune = 'a' + sym - SDLK_a;
                    rune = ((state == KMOD_LSHIFT) ? (nk_rune)nk_to_upper((int)rune):rune);
                    nk_input_unicode(ctx, rune);
                }
            }
        }
    } else if (evt->type == SDL_MOUSEBUTTONDOWN || evt->type == SDL_MOUSEBUTTONUP) {
        /* mouse button */
        int down = evt->type == SDL_MOUSEBUTTONDOWN;
        const int x = evt->button.x, y = evt->button.y;
        if (evt->button.button == SDL_BUTTON_LEFT)
            nk_input_button(ctx, NK_BUTTON_LEFT, x, y, down);
        if (evt->button.button == SDL_BUTTON_MIDDLE)
            nk_input_button(ctx, NK_BUTTON_MIDDLE, x, y, down);
        if (evt->button.button == SDL_BUTTON_RIGHT)
            nk_input_button(ctx, NK_BUTTON_RIGHT, x, y, down);
        if (evt->button.button == SDL_BUTTON_WHEELUP)
            nk_input_scroll(ctx, 1.0f);
        if (evt->button.button == SDL_BUTTON_WHEELDOWN)
            nk_input_scroll(ctx, -1.0f);
    } else if (evt->type == SDL_MOUSEMOTION) {
        nk_input_motion(ctx, evt->motion.x, evt->motion.y);
    }

#endif
}

NK_API void
nk_headless_shutdown(void)
{
    free(headless_font);
//    nk_free(&headless.ctx);
}

NK_API nk_flags
nk_edit_string2(struct nk_context* ctx, nk_flags flags, char *buffer, int *len, int max) {
	nk_edit_string(ctx, flags, buffer, len, max, 0);
//	char* cbuffer = malloc(max);
//	for (int i = 0; i < max; i++) {
//		cbuffer[i] = buffer[i];
//	}
//	nk_edit_string(ctx, flags, cbuffer, len, max, 0);
//	for (int i = 0; i < max; i++) {
//		buffer[i] = cbuffer[i];
//	}
//	free(cbuffer);
}

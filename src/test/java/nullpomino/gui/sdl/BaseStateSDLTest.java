package nullpomino.gui.sdl;

import org.junit.jupiter.api.Test;

/**
 * BaseStateSDL is the common no-op parent for every SDL menu state.
 * Subclasses override only the methods they need, so the empty defaults
 * have to keep accepting calls without side-effects — the menu loop
 * dispatches enter/leave/render/update unconditionally.
 */
class BaseStateSDLTest {

	@Test
	void noOpLifecycleMethodsRunWithoutThrowing() {
		BaseStateSDL state = new BaseStateSDL();

		state.enter();
		state.leave();
		state.render();
		state.update();
	}
}

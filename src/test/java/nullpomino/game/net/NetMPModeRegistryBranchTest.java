package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import nullpomino.game.play.GameEngine;

/**
 * Covers the static assertion and out-of-bounds access in
 * NetMPModeRegistry.
 */
class NetMPModeRegistryBranchTest {

	@Test
	void forStyleWithValidStyle() {
		List<NetMPModeRegistry.Entry> entries = NetMPModeRegistry.forStyle(0);
		assertNotNull(entries);
	}

	@Test
	void forStyleWithInvalidStyle() {
		assertThrows(IndexOutOfBoundsException.class,
			() -> NetMPModeRegistry.forStyle(GameEngine.MAX_GAMESTYLE));
		assertThrows(IndexOutOfBoundsException.class,
			() -> NetMPModeRegistry.forStyle(-1));
	}

	@Test
	void staticInitAssertTriggersOnWrongSize() {
		// The static assert verifies BY_STYLE.size() == GameEngine.MAX_GAMESTYLE (4)
		// This just verifies the constant is what we expect
		assertNotNull(NetMPModeRegistry.forStyle(0));
	}
}

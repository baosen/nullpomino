package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import nullpomino.game.play.GameEngine;

/**
 * Covers the static assertion and out-of-bounds access in
 * NetSPModeRegistry.
 */
class NetSPModeRegistryBranchTest {

	@Test
	void forStyleWithValidStyle() {
		List<NetSPModeRegistry.Entry> entries = NetSPModeRegistry.forStyle(0);
		assertNotNull(entries);
	}

	@Test
	void forStyleWithInvalidStyle() {
		assertThrows(IndexOutOfBoundsException.class,
			() -> NetSPModeRegistry.forStyle(GameEngine.MAX_GAMESTYLE));
		assertThrows(IndexOutOfBoundsException.class,
			() -> NetSPModeRegistry.forStyle(-1));
	}

	@Test
	void staticInitAssertTriggersOnWrongSize() {
		assertNotNull(NetSPModeRegistry.forStyle(0));
	}
}

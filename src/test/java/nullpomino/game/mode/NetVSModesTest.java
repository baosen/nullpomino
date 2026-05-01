package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.play.GameEngine;

import org.junit.jupiter.api.Test;

/**
 * Pins the registry surface for the three concrete NetDummyVSMode
 * subclasses (NetVSLineRaceMode, NetVSDigRaceMode, NetVSBattleMode).
 * Each one is a netplay-specific versus mode that needs a live netplay
 * session to drive its full lifecycle, but the registry-facing
 * surface (name, player count, isNetplayMode, isVSMode, game style)
 * is testable headlessly.
 */
class NetVSModesTest {

	@Test
	void netVSLineRaceHasExpectedRegistrySurface() {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();

		assertEquals("NET-VS-LINE RACE", mode.getName());
		assertEquals(6, mode.getPlayers(),
				"NetDummyVSMode allocates NETVS_MAX_PLAYERS=6 engines for the room");
		assertTrue(mode.isNetplayMode());
		assertTrue(mode.isVSMode());
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, mode.getGameStyle());
	}

	@Test
	void netVSDigRaceHasExpectedRegistrySurface() {
		NetVSDigRaceMode mode = new NetVSDigRaceMode();

		assertEquals("NET-VS-DIG RACE", mode.getName());
		assertEquals(6, mode.getPlayers());
		assertTrue(mode.isNetplayMode());
		assertTrue(mode.isVSMode());
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, mode.getGameStyle());
	}

	@Test
	void netVSBattleHasExpectedRegistrySurfaceAndOverridesIsVSMode() {
		NetVSBattleMode mode = new NetVSBattleMode();

		assertEquals("NET-VS-BATTLE", mode.getName());
		assertEquals(6, mode.getPlayers());
		assertTrue(mode.isNetplayMode());
		// NetVSBattleMode override of isVSMode() — the only mode in the
		// trio that overrides it explicitly. Pin that the override
		// returns true so the engine still treats it as a versus mode.
		assertTrue(mode.isVSMode());
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, mode.getGameStyle());
	}

	@Test
	void allThreeShareTheNetDummyVSModePlayerCountConstant() {
		// The three modes share the parent's NETVS_MAX_PLAYERS=6 so a
		// future change to the constant propagates to all three.
		assertEquals(new NetVSLineRaceMode().getPlayers(), new NetVSDigRaceMode().getPlayers());
		assertEquals(new NetVSDigRaceMode().getPlayers(), new NetVSBattleMode().getPlayers());
	}
}

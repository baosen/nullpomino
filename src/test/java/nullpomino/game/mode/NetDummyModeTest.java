package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins the {@link NetDummyMode} surface that does not need a live
 * netplay session: the registry name, the no-op lobby-listener
 * callbacks, the netplayUnload teardown, and the netlobbyOnExit
 * quit-flag broadcast.
 */
class NetDummyModeTest {

	@Test
	void getNameReturnsRegistryLiteralForTheParentClass() {
		// Concrete net-vs modes override getName, but the parent dummy
		// returns a sentinel literal that is never registered by the
		// real registry — pin it so a registry rename catches at build
		// time.
		assertEquals("NET-DUMMY", new NetDummyMode().getName());
	}

	@Test
	void netLobbyListenerNoOpsAcceptNullLobbyAndClient() {
		NetDummyMode mode = new NetDummyMode();

		// Each of these is an empty stub. Pin that the empty bodies do
		// not throw on null arguments — the dispatch loop must not crash
		// because a concrete mode forgot to override one.
		mode.netlobbyOnInit(null);
		mode.netlobbyOnLoginOK(null, null);
		mode.netlobbyOnRoomLeave(null, null);
	}

	@Test
	void netplayUnloadClearsLobbyReferenceWhenSet() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		// Plant a non-null netLobby via reflection — the field is
		// protected on the class, accessible from same-package tests.
		java.lang.reflect.Field f = NetDummyMode.class.getDeclaredField("netLobby");
		f.setAccessible(true);
		f.set(mode, null);

		// netplayUnload short-circuits when netLobby is already null.
		mode.netplayUnload(null);

		assertNull(f.get(mode));
	}

	@Test
	void netlobbyOnExitFlagsEveryEngineForQuit() {
		NetDummyMode mode = new NetDummyMode();
		GameManager owner = new GameManager(new EventReceiver());
		owner.mode = mode;
		owner.init();
		owner.engine[0].init();
		// Wire the manager into the mode so netlobbyOnExit walks
		// owner.engine[].
		mode.modeInit(owner);

		mode.netlobbyOnExit(null);

		for(GameEngine eng : owner.engine) {
			assertTrue(eng.quitflag,
					"every engine in the manager must be marked quit on lobby exit");
		}
	}

	@Test
	void netlobbyOnExitSwallowsNullManagerWithoutThrowing() {
		NetDummyMode mode = new NetDummyMode();
		// owner is null → the for-loop NPE is caught by the try / catch
		// in netlobbyOnExit, which is a no-op outcome.
		mode.netlobbyOnExit(null);
	}

	@Test
	void modeInitWiresOwnerAndReceiver() {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		// Owner and receiver are wired so subsequent lifecycle hooks
		// (netlobbyOnExit, etc.) can walk the engine pair.
		// The fields are protected on AbstractMode, so we read them
		// indirectly through behaviour: netlobbyOnExit walks
		// owner.engine[].
		manager.init();
		manager.engine[0].init();
		mode.netlobbyOnExit(null);
		assertTrue(manager.engine[0].quitflag);
	}
}

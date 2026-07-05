// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net;

import java.util.ArrayDeque;

import nullpomino.game.net.room.RoomDispatcher;
import nullpomino.game.net.room.RoomEvent;

import org.teavm.jso.browser.Window;

/**
 * Browser {@link RoomDispatcher}: a run-to-completion trampoline on the
 * single JS event loop. Everything (DataChannel/MQTT callbacks, the game
 * green thread) shares that loop, so a plain ArrayDeque is safe; the
 * trampoline guard keeps handling strictly one-at-a-time even when
 * handling a line synchronously posts new events (e.g. the client
 * replying through {@code RoomEndpoint.sendLine}), matching the FIFO
 * semantics of the desktop dispatcher thread. The 1s tick rides
 * {@code window.setInterval}.
 */
public class WebRoomDispatcher implements RoomDispatcher {
	private final ArrayDeque<RoomEvent> queue = new ArrayDeque<RoomEvent>();
	private EventHandler handler;
	private boolean draining = false;
	private boolean shutdownRequested = false;
	private int tickerId = -1;

	@Override
	public void start(EventHandler handler) {
		this.handler = handler;
		drain();
	}

	@Override
	public void post(RoomEvent event) {
		if(shutdownRequested) return;
		queue.add(event);
		drain();
	}

	private void drain() {
		if(draining || (handler == null)) return;
		draining = true;
		try {
			RoomEvent event;
			while(!shutdownRequested && ((event = queue.poll()) != null)) {
				handler.handle(event);
			}
		} finally {
			draining = false;
		}
	}

	@Override
	public void startTicker(int periodMs) {
		if((tickerId != -1) || shutdownRequested) return;
		tickerId = Window.setInterval(() -> post(RoomEvent.tick()), periodMs);
	}

	@Override
	public void shutdown() {
		shutdownRequested = true;
		if(tickerId != -1) {
			Window.clearInterval(tickerId);
			tickerId = -1;
		}
		queue.clear();
	}
}

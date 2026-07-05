// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Desktop {@link RoomDispatcher}: a single daemon "RoomDispatcher"
 * thread draining a blocking queue, plus a "RoomTick" timer. This is
 * the session's original threading model, moved behind the seam.
 */
public class ThreadRoomDispatcher implements RoomDispatcher {
	/** Log */
	private static final Logger log = LoggerFactory.getLogger(ThreadRoomDispatcher.class);

	private final LinkedBlockingQueue<RoomEvent> queue = new LinkedBlockingQueue<RoomEvent>();
	private final Timer tickTimer = new Timer("RoomTick", true);
	private volatile boolean shutdownRequested = false;

	@Override
	public void start(EventHandler handler) {
		Thread thread = new Thread(() -> dispatchLoop(handler), "RoomDispatcher");
		thread.setDaemon(true);
		thread.start();
	}

	private void dispatchLoop(EventHandler handler) {
		try {
			while(!shutdownRequested) {
				handler.handle(queue.take());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Throwable e) {
			log.error("Room dispatcher died", e);
		}
	}

	@Override
	public void post(RoomEvent event) {
		if(shutdownRequested) return;
		queue.add(event);
	}

	@Override
	public void startTicker(int periodMs) {
		tickTimer.schedule(new TimerTask() {
			@Override public void run() {
				post(RoomEvent.tick());
			}
		}, periodMs, periodMs);
	}

	@Override
	public void shutdown() {
		shutdownRequested = true;
		tickTimer.cancel();
		// Wake a dispatcher blocked in take() so it observes the flag
		queue.add(RoomEvent.tick());
	}
}

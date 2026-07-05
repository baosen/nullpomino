// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

/**
 * Serializes {@link RoomEvent}s for a {@link RoomSession}: events may be
 * posted from any thread or callback context, and the handler runs them
 * strictly one at a time, in FIFO order. Desktop sessions use a dedicated
 * thread ({@link ThreadRoomDispatcher}); the browser build runs a
 * trampoline on the single JS event loop instead, since TeaVM's classlib
 * has no blocking queues.
 */
public interface RoomDispatcher {
	/** Consumes events one at a time; never called concurrently */
	interface EventHandler {
		void handle(RoomEvent event);
	}

	/** Begin delivering posted events to the handler */
	void start(EventHandler handler);

	/** Enqueue an event. Thread-safe, non-blocking, a no-op after shutdown. */
	void post(RoomEvent event);

	/** Post a TICK event every periodMs until shutdown */
	void startTicker(int periodMs);

	/** Stop delivering events and cancel the ticker. Idempotent; safe to call from the handler. */
	void shutdown();
}

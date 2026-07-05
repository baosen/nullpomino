// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import nullpomino.game.net.NetUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One TCP connection to another room peer. Owns two daemon threads:
 * a reader (blocking read, line reassembly via
 * {@link NetUtil#processPacketBuffer}, events into the sink) and a writer
 * draining a bounded queue - so a peer with a full TCP buffer can never
 * block the dispatcher or another peer. Queue overflow closes the link:
 * a consumer that far behind is effectively dead.
 */
public class RoomPeerLink extends RoomLink {
	/** Log */
	private static final Logger log = LoggerFactory.getLogger(RoomPeerLink.class);

	/** Read buffer size (same as NetBaseClient) */
	private static final int BUF_SIZE = 2048;

	/** Poison pill that stops the writer thread */
	private static final byte[] CLOSE_MARKER = new byte[0];

	/** Marker that closes the link after flushing everything queued before it */
	private static final byte[] FLUSH_CLOSE_MARKER = new byte[0];

	private final Socket socket;
	private final RoomEventSink sink;
	private final LinkedBlockingQueue<byte[]> writeQueue;
	private final AtomicBoolean closed = new AtomicBoolean(false);

	/** true if this side dialed the connection */
	public final boolean outbound;

	public RoomPeerLink(Socket socket, boolean outbound, RoomEventSink sink) {
		this(socket, outbound, sink, RoomProtocol.WRITE_QUEUE_MAX);
	}

	/** Package-private: tests inject a small write-queue capacity */
	RoomPeerLink(Socket socket, boolean outbound, RoomEventSink sink, int writeQueueCapacity) {
		this.socket = socket;
		this.outbound = outbound;
		this.sink = sink;
		this.writeQueue = new LinkedBlockingQueue<byte[]>(writeQueueCapacity);
	}

	/** Start the reader and writer threads */
	public void start() {
		Thread reader = new Thread(this::readLoop, "RoomLinkReader-" + getRemoteAddress());
		reader.setDaemon(true);
		reader.start();

		Thread writer = new Thread(this::writeLoop, "RoomLinkWriter-" + getRemoteAddress());
		writer.setDaemon(true);
		writer.start();
	}

	private void readLoop() {
		StringBuilder partial = null;
		byte[] buf = new byte[BUF_SIZE];

		try {
			int size;
			while((size = socket.getInputStream().read(buf)) > 0) {
				lastInboundMillis = System.currentTimeMillis();
				String message = new String(buf, 0, size, java.nio.charset.StandardCharsets.UTF_8);
				partial = NetUtil.processPacketBuffer(partial, message,
					line -> sink.onLine(this, line));
			}
			close("EOF");
		} catch (IOException e) {
			close(closed.get() ? "closed" : ("read error: " + e));
		}
	}

	private void writeLoop() {
		try {
			OutputStream out = socket.getOutputStream();
			while(true) {
				byte[] data = writeQueue.take();
				if(data == CLOSE_MARKER) return;
				if(data == FLUSH_CLOSE_MARKER) {
					close(flushCloseReason);
					return;
				}
				out.write(data);
				out.flush();
			}
		} catch (IOException e) {
			close(closed.get() ? "closed" : ("write error: " + e));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** Reason passed to close() when the flush-close marker is reached */
	private volatile String flushCloseReason = "closed after flush";

	/**
	 * Close the link once everything queued so far has been written - for
	 * farewell lines (deny/bye) that must reach the peer before the socket
	 * dies. Falls back to an immediate close when the queue is full.
	 */
	@Override
	public void closeAfterFlush(String reason) {
		if(closed.get()) return;
		flushCloseReason = reason;
		if(!writeQueue.offer(FLUSH_CLOSE_MARKER)) {
			close(reason);
		}
	}

	/**
	 * Queue a line for sending (newline appended). Non-blocking, callable
	 * from any thread; a no-op after close. Overflow closes the link.
	 */
	@Override
	public void sendLine(String line) {
		if(closed.get()) return;
		if(!writeQueue.offer(NetUtil.stringToBytes(line + "\n"))) {
			close("write queue overflow");
		}
	}

	/**
	 * Close the link. Idempotent; fires {@link RoomEventSink#onLinkClosed}
	 * exactly once, from whichever caller wins.
	 */
	@Override
	public void close(String reason) {
		if(!closed.compareAndSet(false, true)) return;

		try {
			socket.close();
		} catch (IOException e) {
			log.debug("Exception on link close", e);
		}
		writeQueue.clear();
		// The writer may be blocked in take(); the marker always fits after clear()
		writeQueue.offer(CLOSE_MARKER);

		sink.onLinkClosed(this, reason);
	}

	/** @return true once the link is closed */
	@Override
	public boolean isClosed() {
		return closed.get();
	}

	/** @return The peer's IP address as observed on this socket */
	@Override
	public String getRemoteAddress() {
		return (socket.getInetAddress() == null) ? "?" : socket.getInetAddress().getHostAddress();
	}
}

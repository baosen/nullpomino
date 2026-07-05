// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net;

import nullpomino.game.net.NetUtil;
import nullpomino.game.net.room.RoomEventSink;
import nullpomino.game.net.room.RoomLink;
import nullpomino.gui.teavm.net.rtc.RtcBindings;
import nullpomino.gui.teavm.net.rtc.RtcDataChannel;
import nullpomino.gui.teavm.net.rtc.RtcPeerConnection;

/**
 * One WebRTC DataChannel connection to another room peer. The channel
 * is ordered+reliable (the RTCDataChannel default), matching TCP's
 * per-link FIFO guarantee the room protocol depends on. Lines over
 * 16 KB are split into multiple messages (browser interop limit) and
 * reassembled on the receive side through the same
 * {@link NetUtil#processPacketBuffer} path as the TCP reader. No
 * threads: everything runs on the JS event loop.
 */
public class RtcPeerLink extends RoomLink {
	/** Browser-interop-safe DataChannel message size */
	private static final int CHUNK_SIZE = 16384;

	/** Local-buffer cap, the moral analog of RoomPeerLink's write queue max */
	private static final double BUFFERED_MAX = 1 * 1024 * 1024;

	private final RtcPeerConnection pc;
	private final RtcDataChannel channel;
	private final RoomEventSink sink;
	/** The remote peer's signaling clientId (its "address") */
	private final String remoteClientId;

	private boolean closed = false;
	private StringBuilder partial = null;

	/**
	 * Wraps an existing pc+channel pair and arms the channel handlers.
	 * Handlers must be armed before the channel opens so no message is lost.
	 */
	public RtcPeerLink(RtcPeerConnection pc, RtcDataChannel channel, String remoteClientId,
		RoomEventSink sink)
	{
		this.pc = pc;
		this.channel = channel;
		this.remoteClientId = remoteClientId;
		this.sink = sink;

		RtcBindings.onMessage(channel, this::onData);
		RtcBindings.onClose(channel, () -> close("EOF"));
		RtcBindings.onConnectionStateChange(pc, state -> {
			// Faster than the 30s ping timeout for abrupt tab closes
			if("failed".equals(state) || ("disconnected".equals(state)) || "closed".equals(state)) {
				close("connection " + state);
			}
		});
	}

	private void onData(String data) {
		if(closed) return;
		lastInboundMillis = System.currentTimeMillis();
		try {
			partial = NetUtil.processPacketBuffer(partial, data, line -> sink.onLine(this, line));
		} catch (java.io.IOException e) {
			close("read error: " + e);
		}
	}

	@Override
	public void sendLine(String line) {
		if(closed) return;
		if(channel.getBufferedAmount() > BUFFERED_MAX) {
			// A consumer that far behind is effectively dead (same policy as TCP)
			close("write queue overflow");
			return;
		}
		String data = line + "\n";
		try {
			for(int at = 0; at < data.length(); at += CHUNK_SIZE) {
				channel.send(data.substring(at, Math.min(data.length(), at + CHUNK_SIZE)));
			}
		} catch (Throwable e) {
			close("send failed: " + e.getMessage());
		}
	}

	@Override
	public void closeAfterFlush(String reason) {
		// RTCDataChannel.close() transitions to "closing" and the transport
		// keeps sending queued messages first - exactly the flush semantics
		// the farewell lines need
		close(reason);
	}

	@Override
	public void close(String reason) {
		if(closed) return;
		closed = true;
		try {
			channel.close();
		} catch (Throwable e) {
			// already closed
		}
		try {
			pc.close();
		} catch (Throwable e) {
			// already closed
		}
		sink.onLinkClosed(this, reason);
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public String getRemoteAddress() {
		return remoteClientId;
	}
}

// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import nullpomino.game.net.room.RoomEventSink;
import nullpomino.game.net.room.RoomLink;
import nullpomino.game.net.room.RoomProtocol;
import nullpomino.game.net.room.RoomTransport;
import nullpomino.game.net.web.WebSignaling;
import nullpomino.gui.teavm.net.rtc.RtcBindings;
import nullpomino.gui.teavm.net.rtc.RtcDataChannel;
import nullpomino.gui.teavm.net.rtc.RtcPeerConnection;

import org.teavm.jso.browser.Window;

/**
 * Browser {@link RoomTransport} over WebRTC DataChannels. "Listening"
 * subscribes this peer's signaling inbox on the bonded MQTT channel;
 * "dialing" runs a non-trickle offer/answer exchange against the target
 * peer's inbox, where the host string is the target's signaling clientId
 * (ports are meaningless and ignored). Everything runs on the JS event
 * loop; per-dial timeouts ride window.setTimeout.
 */
public class RtcRoomTransport implements RoomTransport {
	/** Remember this many answered callIds (bonded brokers duplicate offers) */
	private static final int ANSWERED_CAP = 64;

	private final RoomEventSink sink;
	private final WebNetHub hub;

	/** In-flight outbound dials by callId */
	private final Map<String, PendingDial> pendingDials = new HashMap<String, PendingDial>();

	/** Inbound offers already answered (msgId-style dedupe across bonded brokers) */
	private final LinkedHashSet<String> answeredCalls = new LinkedHashSet<String>();

	/** Every link this transport created, for shutdown */
	private final List<RtcPeerLink> links = new ArrayList<RtcPeerLink>();

	private MqttBondedChannel.MessageHandler inboxHandler;
	private boolean shutdownRequested = false;

	private static final class PendingDial {
		RtcPeerConnection pc;
		RtcDataChannel channel;
		DialCallback callback;
		String targetClientId;
		int timeoutId = -1;
		boolean answered = false;
	}

	public RtcRoomTransport(RoomEventSink sink) {
		this(sink, WebNetHub.get());
	}

	RtcRoomTransport(RoomEventSink sink, WebNetHub hub) {
		this.sink = sink;
		this.hub = hub;
	}

	@Override
	public int startListening(int configuredPort) {
		final String inbox = WebSignaling.topicSig(hub.topicRoot, hub.clientId);
		inboxHandler = (topic, payload) -> {
			if(!shutdownRequested && inbox.equals(topic)) {
				handleSignal(new String(payload, StandardCharsets.UTF_8));
			}
		};
		hub.connectedChannel().addHandler(inboxHandler);
		hub.channel.subscribe(inbox);
		return 0;
	}

	@Override
	public void dial(String host, int port, int timeoutMs, DialCallback callback) {
		if(shutdownRequested) {
			callback.onDialFailed("transport is shut down");
			return;
		}
		final String callId = WebSignaling.generateId(hub.rand);
		final PendingDial dial = new PendingDial();
		dial.callback = callback;
		dial.targetClientId = host;
		dial.pc = RtcBindings.newPeerConnection(joinUrls(hub.config.stunUrls));
		dial.channel = dial.pc.createDataChannel("room");
		pendingDials.put(callId, dial);

		dial.timeoutId = Window.setTimeout(() -> failDial(callId, "timeout"), timeoutMs);

		RtcBindings.createOfferGathered(dial.pc, hub.config.iceGatherTimeout, sdp -> {
			PendingDial current = pendingDials.get(callId);
			if(current == null) return;   // timed out / shut down meanwhile
			if(sdp == null) {
				failDial(callId, "offer failed");
				return;
			}
			hub.channel.publish(WebSignaling.topicSig(hub.topicRoot, current.targetClientId),
				WebSignaling.buildOffer(hub.clientId, callId, sdp)
					.getBytes(StandardCharsets.UTF_8));
		});
	}

	// ---------------------------------------------------------------- inbound signaling

	private void handleSignal(String line) {
		WebSignaling.Signal signal = WebSignaling.parse(line);
		if(signal == null) return;

		if(WebSignaling.KIND_OFFER.equals(signal.kind)) {
			onOffer(signal);
		} else if(WebSignaling.KIND_ANSWER.equals(signal.kind)) {
			onAnswer(signal);
		} else if(WebSignaling.KIND_REJECT.equals(signal.kind)) {
			failDial(signal.callId, "rejected: " + signal.payload);
		}
	}

	/** Inbound offer: answer it and accept the offerer-created channel */
	private void onOffer(WebSignaling.Signal signal) {
		// Bonded brokers deliver the same offer once per broker
		if(!answeredCalls.add(signal.callId)) return;
		while(answeredCalls.size() > ANSWERED_CAP) {
			answeredCalls.remove(answeredCalls.iterator().next());
		}

		final RtcPeerConnection pc = RtcBindings.newPeerConnection(joinUrls(hub.config.stunUrls));
		final String fromClientId = signal.fromClientId;

		RtcBindings.onDataChannel(pc, channel -> {
			// Mirrors TCP accept: the link exists (handlers armed) before any
			// hello; LINK_ACCEPTED is posted ahead of the first LINE
			RtcPeerLink link = new RtcPeerLink(pc, channel, fromClientId, sink);
			links.add(link);
			sink.onLinkAccepted(link);
		});

		RtcBindings.createAnswerGathered(pc, signal.payload, hub.config.iceGatherTimeout, sdp -> {
			if(sdp == null) {
				hub.channel.publish(WebSignaling.topicSig(hub.topicRoot, fromClientId),
					WebSignaling.buildReject(hub.clientId, signal.callId, "answer failed")
						.getBytes(StandardCharsets.UTF_8));
				pc.close();
				return;
			}
			hub.channel.publish(WebSignaling.topicSig(hub.topicRoot, fromClientId),
				WebSignaling.buildAnswer(hub.clientId, signal.callId, sdp)
					.getBytes(StandardCharsets.UTF_8));
		});
	}

	/** Answer to one of our offers: apply it, then wait for the channel to open */
	private void onAnswer(WebSignaling.Signal signal) {
		final String callId = signal.callId;
		final PendingDial dial = pendingDials.get(callId);
		if((dial == null) || dial.answered) return;   // stale or duplicate
		dial.answered = true;

		RtcBindings.onOpen(dial.channel, () -> {
			PendingDial current = pendingDials.remove(callId);
			if(current == null) return;   // timed out meanwhile
			Window.clearTimeout(current.timeoutId);
			RtcPeerLink link = new RtcPeerLink(current.pc, current.channel,
				current.targetClientId, sink);
			links.add(link);
			current.callback.onDialed(link);
		});

		RtcBindings.setRemoteAnswer(dial.pc, signal.payload, ok -> {
			if(!ok) failDial(callId, "bad answer");
		});
	}

	private void failDial(String callId, String reason) {
		PendingDial dial = pendingDials.remove(callId);
		if(dial == null) return;
		if(dial.timeoutId != -1) Window.clearTimeout(dial.timeoutId);
		try {
			dial.pc.close();
		} catch (Throwable e) {
			// already closed
		}
		dial.callback.onDialFailed(reason);
	}

	// ---------------------------------------------------------------- misc

	@Override
	public int getListenPort() {
		return 0;
	}

	@Override
	public String getDisplayAddress() {
		return hub.clientId;
	}

	@Override
	public void shutdown() {
		shutdownRequested = true;
		if(inboxHandler != null) {
			hub.channel.removeHandler(inboxHandler);
			inboxHandler = null;
		}
		for(String callId: new ArrayList<String>(pendingDials.keySet())) {
			PendingDial dial = pendingDials.remove(callId);
			if(dial.timeoutId != -1) Window.clearTimeout(dial.timeoutId);
			try {
				dial.pc.close();
			} catch (Throwable e) {
				// already closed
			}
		}
		for(RtcPeerLink link: new ArrayList<RtcPeerLink>(links)) {
			link.close(RoomProtocol.DENY_SHUTDOWN);
		}
	}

	private static String joinUrls(String[] urls) {
		StringBuilder sb = new StringBuilder();
		for(String url: urls) {
			if(sb.length() > 0) sb.append(' ');
			sb.append(url);
		}
		return sb.toString();
	}
}

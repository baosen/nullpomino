// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.web;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import nullpomino.game.net.NetUtil;

/**
 * Topic scheme and message codec for browser netplay over an MQTT
 * broker: lounge topics carrying the {@link nullpomino.game.net.NetLanDiscovery}
 * packet payloads, per-peer signaling inboxes carrying the WebRTC
 * offer/answer exchange, and the announce wrapper that prefixes the
 * arbiter's signaling clientId (the web analog of a beacon's source IP).
 * Pure string/byte work - unit-tested on the JVM, compiled under TeaVM.
 */
public final class WebSignaling {
	/** First field of every signaling message */
	public static final String MAGIC = "RTC1";

	public static final String KIND_OFFER = "offer";
	public static final String KIND_ANSWER = "answer";
	public static final String KIND_REJECT = "reject";

	private WebSignaling() {}

	// ---------------------------------------------------------------- topics

	/**
	 * @param topicRoot Configured root, e.g. "npp"
	 * @param versionMajor Game protocol major version (e.g. "7.5") - incompatible clients never meet
	 * @return The versioned topic prefix, e.g. "npp/v7.5"
	 */
	public static String root(String topicRoot, String versionMajor) {
		return topicRoot + "/v" + versionMajor;
	}

	/** Room announces; one topic per session so key-overwrite dedupe is trivial */
	public static String topicRooms(String root, String sessionId) {
		return root + "/lounge/rooms/" + sessionId;
	}

	/** Lounge chat lines */
	public static String topicChat(String root) {
		return root + "/lounge/chat";
	}

	/** Lounge presence beacons */
	public static String topicPresence(String root) {
		return root + "/lounge/presence";
	}

	/** Subscription filter covering all lounge traffic (never subscribe a bare #) */
	public static String loungeFilter(String root) {
		return root + "/lounge/#";
	}

	/** A peer's signaling inbox: WebRTC offers/answers addressed to it */
	public static String topicSig(String root, String clientId) {
		return root + "/sig/" + clientId;
	}

	// ---------------------------------------------------------------- ids

	/** @return A 16-hex-digit random id (clientIds, callIds) */
	public static String generateId(Random rand) {
		// Manual zero-pad: TeaVM's String.format support is not to be trusted
		String hex = Long.toHexString(rand.nextLong());
		StringBuilder sb = new StringBuilder(16);
		for(int i = hex.length(); i < 16; i++) sb.append('0');
		return sb.append(hex).toString();
	}

	// ---------------------------------------------------------------- signaling messages

	public static String buildOffer(String fromClientId, String callId, String sdp) {
		return build(KIND_OFFER, fromClientId, callId, sdp);
	}

	public static String buildAnswer(String fromClientId, String callId, String sdp) {
		return build(KIND_ANSWER, fromClientId, callId, sdp);
	}

	public static String buildReject(String fromClientId, String callId, String reason) {
		return build(KIND_REJECT, fromClientId, callId, reason);
	}

	private static String build(String kind, String from, String callId, String payload) {
		return MAGIC + "\t" + kind + "\t" + from + "\t" + callId + "\t" + NetUtil.urlEncode(payload);
	}

	/** One parsed signaling message */
	public static final class Signal {
		/** offer / answer / reject */
		public final String kind;
		/** Sender's signaling clientId (where the reply goes) */
		public final String fromClientId;
		/** Dial-attempt id; stale/duplicate messages are dropped by it */
		public final String callId;
		/** SDP (offer/answer) or reason (reject) */
		public final String payload;

		Signal(String kind, String fromClientId, String callId, String payload) {
			this.kind = kind;
			this.fromClientId = fromClientId;
			this.callId = callId;
			this.payload = payload;
		}
	}

	/**
	 * Parse a signaling message.
	 * @return The signal, or null when the line is not a valid RTC1 message
	 */
	public static Signal parse(String line) {
		if(line == null) return null;
		String[] parts = line.split("\t", -1);
		if(parts.length < 5) return null;
		if(!MAGIC.equals(parts[0])) return null;
		if(!KIND_OFFER.equals(parts[1]) && !KIND_ANSWER.equals(parts[1]) && !KIND_REJECT.equals(parts[1])) return null;
		if((parts[2].length() == 0) || (parts[3].length() == 0)) return null;
		return new Signal(parts[1], parts[2], parts[3], NetUtil.urlDecode(parts[4]));
	}

	// ---------------------------------------------------------------- announce wrapper

	/**
	 * Wrap a room announce for the broker: the arbiter's clientId plays the
	 * role the source IP plays on LAN, and rides in front of the packet.
	 */
	public static byte[] wrapAnnounce(String arbiterClientId, byte[] announcePacket) {
		byte[] prefix = (arbiterClientId + "\n").getBytes(StandardCharsets.UTF_8);
		byte[] out = new byte[prefix.length + announcePacket.length];
		System.arraycopy(prefix, 0, out, 0, prefix.length);
		System.arraycopy(announcePacket, 0, out, prefix.length, announcePacket.length);
		return out;
	}

	/** One unwrapped room announce */
	public static final class WrappedAnnounce {
		/** The announcing arbiter's signaling clientId (the dial target) */
		public final String arbiterClientId;
		/** The raw NetLanDiscovery announce packet */
		public final byte[] packet;

		WrappedAnnounce(String arbiterClientId, byte[] packet) {
			this.arbiterClientId = arbiterClientId;
			this.packet = packet;
		}
	}

	/**
	 * Unwrap a broker room announce.
	 * @return The parts, or null when the payload has no clientId prefix
	 */
	public static WrappedAnnounce unwrapAnnounce(byte[] payload) {
		if(payload == null) return null;
		int newline = -1;
		for(int i = 0; i < payload.length; i++) {
			if(payload[i] == '\n') { newline = i; break; }
		}
		if(newline <= 0) return null;
		String clientId = new String(payload, 0, newline, StandardCharsets.UTF_8);
		byte[] packet = new byte[payload.length - newline - 1];
		System.arraycopy(payload, newline + 1, packet, 0, packet.length);
		return new WrappedAnnounce(clientId, packet);
	}
}

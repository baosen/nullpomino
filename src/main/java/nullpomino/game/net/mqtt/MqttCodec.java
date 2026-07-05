// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mqtt;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Minimal MQTT 3.1.1 packet codec, QoS 0 only - just enough for the
 * browser lounge/signaling client (CONNECT, SUBSCRIBE, PUBLISH, ping,
 * DISCONNECT out; CONNACK, SUBACK, PUBLISH, PINGRESP in). Pure Java with
 * no I/O so it unit-tests on the JVM and compiles under TeaVM; the
 * WebSocket wiring lives in the web binding layer.
 */
public final class MqttCodec {
	/** MQTT control packet types (fixed-header high nibble) */
	public static final int CONNECT = 1, CONNACK = 2, PUBLISH = 3, SUBSCRIBE = 8,
		SUBACK = 9, PINGREQ = 12, PINGRESP = 13, DISCONNECT = 14;

	private MqttCodec() {}

	/** CONNECT with protocol level 4 ("MQTT" 3.1.1), clean session, no will/auth */
	public static byte[] encodeConnect(String clientId, int keepAliveSec) {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		writeString(body, "MQTT");
		body.write(4);                     // protocol level 4 = 3.1.1
		body.write(0x02);                  // connect flags: clean session
		body.write((keepAliveSec >> 8) & 0xFF);
		body.write(keepAliveSec & 0xFF);
		writeString(body, clientId);
		return packet(CONNECT, 0, body.toByteArray());
	}

	/** SUBSCRIBE for one topic filter at QoS 0 (fixed-header flags must be 0b0010) */
	public static byte[] encodeSubscribe(int packetId, String topicFilter) {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		body.write((packetId >> 8) & 0xFF);
		body.write(packetId & 0xFF);
		writeString(body, topicFilter);
		body.write(0);                     // requested QoS 0
		return packet(SUBSCRIBE, 0x02, body.toByteArray());
	}

	/** PUBLISH at QoS 0 (no packet id) */
	public static byte[] encodePublish(String topic, byte[] payload) {
		ByteArrayOutputStream body = new ByteArrayOutputStream();
		writeString(body, topic);
		body.write(payload, 0, payload.length);
		return packet(PUBLISH, 0, body.toByteArray());
	}

	public static byte[] encodePingReq() {
		return new byte[] { (byte) 0xC0, 0 };
	}

	public static byte[] encodeDisconnect() {
		return new byte[] { (byte) 0xE0, 0 };
	}

	/** One decoded inbound packet */
	public static final class Packet {
		/** Control packet type (high nibble of the first byte) */
		public int type;
		/** Total bytes this packet consumed from the buffer */
		public int consumed;
		/** CONNACK only: return code, 0 = connection accepted */
		public int connackCode;
		/** PUBLISH only: topic name */
		public String topic;
		/** PUBLISH only: application payload */
		public byte[] payload;
	}

	/**
	 * Decode one packet from buf[off..off+len).
	 * @return The packet, or null when the range holds an incomplete packet
	 * @throws IllegalArgumentException On a malformed packet (bad varint/length)
	 */
	public static Packet decode(byte[] buf, int off, int len) {
		if(len < 2) return null;
		int type = (buf[off] & 0xFF) >> 4;
		int flags = buf[off] & 0x0F;

		// Remaining-length varint: 1..4 bytes, 7 bits each, MSB = continue
		int remaining = 0;
		int multiplier = 1;
		int at = 1;
		while(true) {
			if(at >= len) return null;
			if(at > 4) throw new IllegalArgumentException("remaining-length varint too long");
			int digit = buf[off + at] & 0xFF;
			at++;
			remaining += (digit & 0x7F) * multiplier;
			multiplier *= 128;
			if((digit & 0x80) == 0) break;
		}
		if(len < at + remaining) return null;

		Packet packet = new Packet();
		packet.type = type;
		packet.consumed = at + remaining;
		int bodyOff = off + at;

		if(type == CONNACK) {
			if(remaining < 2) throw new IllegalArgumentException("short CONNACK");
			packet.connackCode = buf[bodyOff + 1] & 0xFF;
		} else if(type == PUBLISH) {
			if(remaining < 2) throw new IllegalArgumentException("short PUBLISH");
			int topicLen = ((buf[bodyOff] & 0xFF) << 8) | (buf[bodyOff + 1] & 0xFF);
			int afterTopic = 2 + topicLen;
			// QoS>0 PUBLISH carries a 2-byte packet id we never request but
			// tolerate (a broker must not send it for QoS 0 subscriptions)
			int qos = (flags >> 1) & 0x03;
			if(qos > 0) afterTopic += 2;
			if(remaining < afterTopic) throw new IllegalArgumentException("short PUBLISH topic");
			packet.topic = new String(buf, bodyOff + 2, topicLen, StandardCharsets.UTF_8);
			int payloadLen = remaining - afterTopic;
			packet.payload = new byte[payloadLen];
			System.arraycopy(buf, bodyOff + afterTopic, packet.payload, 0, payloadLen);
		}
		return packet;
	}

	/** Fixed header (type + flags + remaining-length varint) followed by the body */
	private static byte[] packet(int type, int flags, byte[] body) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write((type << 4) | flags);
		int remaining = body.length;
		do {
			int digit = remaining % 128;
			remaining /= 128;
			out.write((remaining > 0) ? (digit | 0x80) : digit);
		} while(remaining > 0);
		out.write(body, 0, body.length);
		return out.toByteArray();
	}

	/** UTF-8 string with 16-bit big-endian length prefix */
	private static void writeString(ByteArrayOutputStream out, String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		if(bytes.length > 0xFFFF) throw new IllegalArgumentException("string too long");
		out.write((bytes.length >> 8) & 0xFF);
		out.write(bytes.length & 0xFF);
		out.write(bytes, 0, bytes.length);
	}
}

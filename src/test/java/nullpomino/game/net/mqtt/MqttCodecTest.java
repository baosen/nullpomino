// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mqtt;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Byte-exact tests for {@link MqttCodec}: golden packet vectors against
 * the MQTT 3.1.1 spec, remaining-length varint edges, and decode over
 * split/coalesced buffers (WebSocket frames don't align with packets).
 */
class MqttCodecTest {

    // ---------------- golden encode vectors ----------------

    @Test
    void connectPacketMatchesSpec() {
        byte[] p = MqttCodec.encodeConnect("npp1", 30);
        byte[] expected = {
            0x10, 0x10,                               // CONNECT, remaining 16
            0x00, 0x04, 'M', 'Q', 'T', 'T',           // protocol name
            0x04,                                     // level 4 (3.1.1)
            0x02,                                     // clean session
            0x00, 0x1E,                               // keepalive 30
            0x00, 0x04, 'n', 'p', 'p', '1',           // client id
        };
        assertArrayEquals(expected, p);
    }

    @Test
    void subscribePacketMatchesSpec() {
        byte[] p = MqttCodec.encodeSubscribe(1, "a/b");
        byte[] expected = {
            (byte) 0x82, 0x08,                        // SUBSCRIBE (flags 0b0010), remaining 8
            0x00, 0x01,                               // packet id 1
            0x00, 0x03, 'a', '/', 'b',                // topic filter
            0x00,                                     // requested QoS 0
        };
        assertArrayEquals(expected, p);
    }

    @Test
    void publishPacketMatchesSpec() {
        byte[] p = MqttCodec.encodePublish("t", "hi".getBytes(StandardCharsets.UTF_8));
        byte[] expected = {
            0x30, 0x05,                               // PUBLISH QoS0, remaining 5
            0x00, 0x01, 't',                          // topic
            'h', 'i',                                 // payload
        };
        assertArrayEquals(expected, p);
    }

    @Test
    void pingAndDisconnectAreTwoBytes() {
        assertArrayEquals(new byte[] { (byte) 0xC0, 0x00 }, MqttCodec.encodePingReq());
        assertArrayEquals(new byte[] { (byte) 0xE0, 0x00 }, MqttCodec.encodeDisconnect());
    }

    // ---------------- remaining-length varint edges ----------------

    /** Encode a PUBLISH with a payload sized to force each varint width */
    private static byte[] publishWithRemaining(int remaining) {
        // remaining = 2 (topic len prefix) + 1 (topic "t") + payload
        return MqttCodec.encodePublish("t", new byte[remaining - 3]);
    }

    @Test
    void remainingLengthVarintEdges() {
        // 1-byte varint max: 127
        byte[] p = publishWithRemaining(127);
        assertEquals(0x7F, p[1] & 0xFF);
        assertEquals(2 + 127, p.length);

        // 2-byte varint min: 128 -> 0x80 0x01
        p = publishWithRemaining(128);
        assertEquals(0x80, p[1] & 0xFF);
        assertEquals(0x01, p[2] & 0xFF);
        assertEquals(3 + 128, p.length);

        // 2-byte varint max: 16383 -> 0xFF 0x7F
        p = publishWithRemaining(16383);
        assertEquals(0xFF, p[1] & 0xFF);
        assertEquals(0x7F, p[2] & 0xFF);

        // 3-byte varint min: 16384 -> 0x80 0x80 0x01
        p = publishWithRemaining(16384);
        assertEquals(0x80, p[1] & 0xFF);
        assertEquals(0x80, p[2] & 0xFF);
        assertEquals(0x01, p[3] & 0xFF);
    }

    @Test
    void decodeRoundTripsEveryVarintWidth() {
        for (int remaining : new int[] { 3, 127, 128, 16383, 16384 }) {
            byte[] p = publishWithRemaining(remaining);
            MqttCodec.Packet d = MqttCodec.decode(p, 0, p.length);
            assertNotNull(d);
            assertEquals(MqttCodec.PUBLISH, d.type);
            assertEquals(p.length, d.consumed);
            assertEquals("t", d.topic);
            assertEquals(remaining - 3, d.payload.length);
        }
    }

    @Test
    void overlongVarintIsRejected() {
        byte[] bad = { 0x30, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x01 };
        assertThrows(IllegalArgumentException.class, () -> MqttCodec.decode(bad, 0, bad.length));
    }

    // ---------------- decode: split and coalesced buffers ----------------

    @Test
    void incompleteBuffersReturnNull() {
        byte[] p = MqttCodec.encodePublish("topic/x", "payload".getBytes(StandardCharsets.UTF_8));
        // Every strict prefix decodes to null, never throws
        for (int cut = 0; cut < p.length; cut++) {
            assertNull(MqttCodec.decode(p, 0, cut), "prefix of length " + cut);
        }
        assertNotNull(MqttCodec.decode(p, 0, p.length));
    }

    @Test
    void coalescedPacketsDecodeSequentially() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(new byte[] { 0x20, 0x02, 0x00, 0x00 });    // CONNACK accepted
        stream.write(new byte[] { (byte) 0x90, 0x03, 0x00, 0x01, 0x00 }); // SUBACK
        stream.write(MqttCodec.encodePublish("a", "1".getBytes(StandardCharsets.UTF_8)));
        stream.write(new byte[] { (byte) 0xD0, 0x00 });         // PINGRESP
        byte[] buf = stream.toByteArray();

        int off = 0;
        MqttCodec.Packet p = MqttCodec.decode(buf, off, buf.length - off);
        assertEquals(MqttCodec.CONNACK, p.type);
        assertEquals(0, p.connackCode);
        off += p.consumed;

        p = MqttCodec.decode(buf, off, buf.length - off);
        assertEquals(MqttCodec.SUBACK, p.type);
        off += p.consumed;

        p = MqttCodec.decode(buf, off, buf.length - off);
        assertEquals(MqttCodec.PUBLISH, p.type);
        assertEquals("a", p.topic);
        assertArrayEquals("1".getBytes(StandardCharsets.UTF_8), p.payload);
        off += p.consumed;

        p = MqttCodec.decode(buf, off, buf.length - off);
        assertEquals(MqttCodec.PINGRESP, p.type);
        off += p.consumed;
        assertEquals(buf.length, off);
    }

    @Test
    void connackRejectionCodeIsExposed() {
        byte[] refused = { 0x20, 0x02, 0x00, 0x05 };            // not authorized
        MqttCodec.Packet p = MqttCodec.decode(refused, 0, refused.length);
        assertEquals(MqttCodec.CONNACK, p.type);
        assertEquals(5, p.connackCode);
    }

    @Test
    void qos1PublishSkipsPacketId() {
        // A defensive path: QoS 1 PUBLISH carries a 2-byte packet id after the topic
        byte[] p = {
            0x32, 0x07,                                          // PUBLISH QoS1, remaining 7
            0x00, 0x01, 't',
            0x12, 0x34,                                          // packet id
            'h', 'i',
        };
        MqttCodec.Packet d = MqttCodec.decode(p, 0, p.length);
        assertEquals("t", d.topic);
        assertArrayEquals("hi".getBytes(StandardCharsets.UTF_8), d.payload);
    }

    @Test
    void utf8TopicRoundTrips() {
        byte[] p = MqttCodec.encodePublish("npp/v7/lounge/chat", "héllo wörld".getBytes(StandardCharsets.UTF_8));
        MqttCodec.Packet d = MqttCodec.decode(p, 0, p.length);
        assertEquals("npp/v7/lounge/chat", d.topic);
        assertEquals("héllo wörld", new String(d.payload, StandardCharsets.UTF_8));
    }

    @Test
    void decodeHonorsOffset() {
        byte[] inner = MqttCodec.encodePublish("t", "x".getBytes(StandardCharsets.UTF_8));
        byte[] buf = new byte[inner.length + 7];
        System.arraycopy(inner, 0, buf, 7, inner.length);
        MqttCodec.Packet d = MqttCodec.decode(buf, 7, inner.length);
        assertEquals("t", d.topic);
        assertEquals(inner.length, d.consumed);
    }
}

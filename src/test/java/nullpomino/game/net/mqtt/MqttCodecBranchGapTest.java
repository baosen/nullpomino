// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mqtt;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Branch-gap tests for {@link MqttCodec}: truncated varints, short
 * CONNACK/PUBLISH bodies, and oversize length-prefixed strings.
 */
class MqttCodecBranchGapTest {

    @Test
    void truncatedRemainingLengthVarintIsIncomplete() {
        // Second byte has the continuation bit set but the buffer ends there
        assertNull(MqttCodec.decode(new byte[] { 0x30, (byte) 0x80 }, 0, 2));
    }

    @Test
    void shortConnackBodyIsMalformed() {
        // CONNACK with remaining length 1 (body must be 2 bytes)
        byte[] buf = { 0x20, 0x01, 0x00 };
        assertThrows(IllegalArgumentException.class, () -> MqttCodec.decode(buf, 0, buf.length));
    }

    @Test
    void shortPublishBodyIsMalformed() {
        // PUBLISH with remaining length 1 (topic length prefix alone needs 2)
        byte[] buf = { 0x30, 0x01, 0x00 };
        assertThrows(IllegalArgumentException.class, () -> MqttCodec.decode(buf, 0, buf.length));
    }

    @Test
    void publishTopicLongerThanBodyIsMalformed() {
        // Topic length prefix says 5 bytes but the body ends after the prefix
        byte[] buf = { 0x30, 0x02, 0x00, 0x05 };
        assertThrows(IllegalArgumentException.class, () -> MqttCodec.decode(buf, 0, buf.length));
    }

    @Test
    void oversizeTopicStringIsRejected() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 0x10000; i++) sb.append('a');
        String hugeTopic = sb.toString();
        assertThrows(IllegalArgumentException.class,
                () -> MqttCodec.encodePublish(hugeTopic, new byte[0]));
    }
}

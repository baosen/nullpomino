// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.web;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import nullpomino.game.net.NetLanDiscovery;

import org.junit.jupiter.api.Test;

/**
 * Round-trip tests for {@link WebSignaling}: topic scheme, signaling
 * message codec (SDP with tabs/newlines must survive), announce
 * wrapping, and id generation.
 */
class WebSignalingTest {

    @Test
    void topicsFollowTheScheme() {
        String root = WebSignaling.root("npp", "7.5");
        assertEquals("npp/v7.5", root);
        assertEquals("npp/v7.5/lounge/rooms/abc123", WebSignaling.topicRooms(root, "abc123"));
        assertEquals("npp/v7.5/lounge/chat", WebSignaling.topicChat(root));
        assertEquals("npp/v7.5/lounge/presence", WebSignaling.topicPresence(root));
        assertEquals("npp/v7.5/lounge/#", WebSignaling.loungeFilter(root));
        assertEquals("npp/v7.5/sig/00ff00ff00ff00ff", WebSignaling.topicSig(root, "00ff00ff00ff00ff"));
    }

    @Test
    void offerRoundTripsSdpWithTabsAndNewlines() {
        String sdp = "v=0\r\no=- 42 2 IN IP4 127.0.0.1\r\na=has\ttab\r\n";
        String line = WebSignaling.buildOffer("aaaa000011112222", "cafe123456789abc", sdp);

        WebSignaling.Signal signal = WebSignaling.parse(line);
        assertNotNull(signal);
        assertEquals(WebSignaling.KIND_OFFER, signal.kind);
        assertEquals("aaaa000011112222", signal.fromClientId);
        assertEquals("cafe123456789abc", signal.callId);
        assertEquals(sdp, signal.payload);
    }

    @Test
    void answerAndRejectRoundTrip() {
        WebSignaling.Signal answer = WebSignaling.parse(
            WebSignaling.buildAnswer("f1", "c1", "v=0\r\n"));
        assertEquals(WebSignaling.KIND_ANSWER, answer.kind);
        assertEquals("v=0\r\n", answer.payload);

        WebSignaling.Signal reject = WebSignaling.parse(
            WebSignaling.buildReject("f2", "c2", "room is full"));
        assertEquals(WebSignaling.KIND_REJECT, reject.kind);
        assertEquals("room is full", reject.payload);
    }

    @Test
    void malformedSignalsParseToNull() {
        assertNull(WebSignaling.parse(null));
        assertNull(WebSignaling.parse(""));
        assertNull(WebSignaling.parse("not a signal"));
        assertNull(WebSignaling.parse("RTC1\toffer\tfrom"));               // too few fields
        assertNull(WebSignaling.parse("RTC2\toffer\tf\tc\tp"));            // wrong magic
        assertNull(WebSignaling.parse("RTC1\tbogus\tf\tc\tp"));            // unknown kind
        assertNull(WebSignaling.parse("RTC1\toffer\t\tc\tp"));             // empty from
        assertNull(WebSignaling.parse("RTC1\toffer\tf\t\tp"));             // empty callId
    }

    @Test
    void announceWrapperRoundTripsThroughTheLanCodec() {
        NetLanDiscovery.Announce announce = new NetLanDiscovery.Announce(
            "", 1, "Alice", "7", "session0123456789", "Alice's lounge", 2,
            "My Room", true, "STANDARD", "NET-VS-BATTLE", false, 1, 4, 0);
        byte[] packet = NetLanDiscovery.encodeRoomAnnounce(announce);

        byte[] wrapped = WebSignaling.wrapAnnounce("aabbccdd00112233", packet);
        WebSignaling.WrappedAnnounce unwrapped = WebSignaling.unwrapAnnounce(wrapped);
        assertNotNull(unwrapped);
        assertEquals("aabbccdd00112233", unwrapped.arbiterClientId);

        // The inner packet decodes exactly as a LAN beacon would, with the
        // clientId taking the source address role
        NetLanDiscovery.Announce decoded = NetLanDiscovery.decodeAnnounce(
            unwrapped.packet, unwrapped.packet.length, unwrapped.arbiterClientId);
        assertNotNull(decoded);
        assertEquals("aabbccdd00112233", decoded.address);
        assertEquals("session0123456789", decoded.sessionId);
        assertEquals("My Room", decoded.roomName);
        assertEquals("NET-VS-BATTLE", decoded.mode);
        assertEquals(4, decoded.maxPlayers);
    }

    @Test
    void unwrapRejectsPayloadsWithoutPrefix() {
        assertNull(WebSignaling.unwrapAnnounce(null));
        assertNull(WebSignaling.unwrapAnnounce(new byte[0]));
        assertNull(WebSignaling.unwrapAnnounce("no newline here".getBytes(StandardCharsets.UTF_8)));
        assertNull(WebSignaling.unwrapAnnounce("\nempty client id".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void generatedIdsAreSixteenHexDigits() {
        Random rand = new Random(42);
        for (int i = 0; i < 100; i++) {
            String id = WebSignaling.generateId(rand);
            assertEquals(16, id.length());
            assertTrue(id.matches("[0-9a-f]{16}"), id);
        }
        // The zero-pad path: a small long must still be 16 digits
        Random zeroish = new Random() {
            @Override public long nextLong() { return 0xabL; }
        };
        assertEquals("00000000000000ab", WebSignaling.generateId(zeroish));
    }
}

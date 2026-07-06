// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Branch-gap tests for {@link RoomProtocol}: stat-less join hellos,
 * unknown hello variants, and malformed broadcast/direct frames.
 */
class RoomProtocolBranchGapTest {

    @Test
    void joinHelloWithoutStatsYieldsEmptyStatArrays() {
        // Old-style 7-field join hello: no ratings/playCounts/winCounts
        String[] parts = "room\thello\tjoin\t7.5\tfalse\t9202\tSomeone".split("\t", -1);
        RoomProtocol.Hello hello = RoomProtocol.parseHello(parts);
        assertNotNull(hello);
        assertTrue(hello.joinVariant);
        assertEquals("Someone", hello.name);
        assertEquals(0, hello.ratings.length);
        assertEquals(0, hello.playCounts.length);
        assertEquals(0, hello.winCounts.length);
    }

    @Test
    void longHelloWithUnknownVariantIsRejected() {
        // 8 fields, but neither "join" nor "peer": falls through both arms
        String[] parts = "room\thello\tbogus\t7.5\tfalse\ttok\t3\t9202".split("\t", -1);
        assertNull(RoomProtocol.parseHello(parts));
    }

    @Test
    void parseBroadcastRejectsMalformedFrames() {
        assertNull(RoomProtocol.parseBroadcast("room\tc\tnot a broadcast"), "wrong prefix");
        assertNull(RoomProtocol.parseBroadcast("room\tb\t123"), "no tab after seq");
        assertNull(RoomProtocol.parseBroadcast("room\tb\t123\t45"), "no tab after scope");
        RoomProtocol.Broadcast ok = RoomProtocol.parseBroadcast("room\tb\t123\t45\tpayload");
        assertNotNull(ok);
        assertEquals(123, ok.seq);
        assertEquals(45, ok.scope);
        assertEquals("payload", ok.payload);
    }

    @Test
    void unwrapDirectRejectsOtherFrames() {
        assertNull(RoomProtocol.unwrapDirect("room\tb\t1\t2\tnope"));
        assertEquals("hello", RoomProtocol.unwrapDirect(RoomProtocol.wrapDirect("hello")));
    }
}

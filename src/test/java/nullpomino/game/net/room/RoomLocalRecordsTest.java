// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetSPModeRegistry;
import nullpomino.game.net.NetSPRecord;
import nullpomino.game.net.NetUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for {@link RoomRating} (values must match NetServer's math) and
 * {@link RoomLocalRecords} (per-peer persistence + local request replies).
 */
class RoomLocalRecordsTest {

    @TempDir
    File tempDir;

    private RoomLocalRecords newRecords() {
        return new RoomLocalRecords(tempDir.getAbsolutePath());
    }

    // ---------------------------------------------------------------- rating math

    @Test
    void expectedScoreMatchesEloFormula() {
        assertEquals(0.5, RoomRating.expectedScore(1500, 1500), 1e-9);
        assertTrue(RoomRating.expectedScore(1600, 1500) > 0.5);
        assertEquals(1.0,
                RoomRating.expectedScore(1500, 1500) + RoomRating.expectedScore(1500, 1500), 1e-9);
        // Symmetry: my expectation + opponent's expectation = 1
        assertEquals(1.0,
                RoomRating.expectedScore(1700, 1400) + RoomRating.expectedScore(1400, 1700), 1e-9);
    }

    @Test
    void maxDeltaBoostsProvisionalPlayers() {
        // NetServer computes 400/(games+3) with INTEGER division - pinned here
        assertEquals(16 + 133.0, RoomRating.maxDelta(0), 1e-9);
        assertEquals(16 + 7.0, RoomRating.maxDelta(50), 1e-9);
        assertEquals(16.0, RoomRating.maxDelta(51), 1e-9);
    }

    @Test
    void rankDeltaFirstGameBetweenEqualsIsPlusMinus58() {
        // maxDelta(1) = 16 + 400/4 = 116; equal ratings -> expected 0.5
        assertEquals(58.0, RoomRating.rankDelta(1, 1500, 1500, 1), 1e-9);
        assertEquals(-58.0, RoomRating.rankDelta(1, 1500, 1500, 0), 1e-9);
    }

    // ---------------------------------------------------------------- own data

    @Test
    void myDataDefaultsThenRoundTrips() {
        // No file yet: every style starts at the default rating
        NetPlayerInfo blank = new NetPlayerInfo();
        newRecords().loadInto(blank);
        assertEquals(RoomRating.RATING_DEFAULT, blank.rating[0]);
        assertEquals(RoomRating.RATING_DEFAULT, blank.rating[1]);
        assertEquals(0, blank.playCount[0]);

        RoomLocalRecords records = newRecords();
        NetPlayerInfo me = new NetPlayerInfo();
        me.strName = "Me";
        me.rating[0] = 1622;
        me.playCount[0] = 12;
        me.winCount[0] = 7;
        records.saveFrom(me);

        NetPlayerInfo fresh = new NetPlayerInfo();
        newRecords().loadInto(fresh);
        assertEquals(1622, fresh.rating[0]);
        assertEquals(12, fresh.playCount[0]);
        assertEquals(7, fresh.winCount[0]);
    }

    // ---------------------------------------------------------------- MP leaderboard

    @Test
    void leaderboardAccumulatesAndAnswersMPRanking() {
        RoomLocalRecords records = newRecords();
        records.recordRatedResult(0, "Alice", 1558, true);
        records.recordRatedResult(0, "Bob", 1442, false);
        records.recordRatedResult(0, "Alice", 1601, true);

        NetPlayerInfo self = new NetPlayerInfo();
        self.strName = "Alice";
        String reply = records.buildMPRankingReply(0, self);

        String[] f = reply.split("\t");
        assertEquals("mpranking", f[0]);
        assertEquals("0", f[1]);
        assertEquals("0", f[2], "Alice leads the local board");

        String data = NetUtil.decompressString(f[3]);
        String[] rows = data.split("\t");
        assertEquals(2, rows.length);
        assertTrue(rows[0].startsWith("0;" + NetUtil.urlEncode("Alice") + ";1601;2;2"));
        assertTrue(rows[1].startsWith("1;" + NetUtil.urlEncode("Bob") + ";1442;1;0"));

        // Persists across instances
        String reloaded = newRecords().buildMPRankingReply(0, self);
        assertEquals(reply, reloaded);
    }

    @Test
    void unrankedSelfIsAppendedWithRankMinusOne() {
        RoomLocalRecords records = newRecords();
        records.recordRatedResult(0, "Alice", 1558, true);

        NetPlayerInfo self = new NetPlayerInfo();
        self.strName = "Stranger";
        self.rating[0] = 1500;

        String data = NetUtil.decompressString(records.buildMPRankingReply(0, self).split("\t")[3]);
        assertTrue(data.contains("-1;" + NetUtil.urlEncode("Stranger") + ";1500;0;0"));
    }

    // ---------------------------------------------------------------- SP rankings

    @Test
    void spRecordRegistersAndAnswersSPRanking() {
        String mode = NetSPModeRegistry.forStyle(0).get(0).name();

        RoomLocalRecords records = newRecords();
        NetRoomInfo room = new NetRoomInfo();
        room.singleplayer = true;
        room.rated = false;
        room.strMode = mode;
        room.style = 0;
        NetPlayerInfo me = new NetPlayerInfo();
        me.strName = "Me";

        NetSPRecord record = new NetSPRecord();
        record.stats = new nullpomino.game.component.Statistics();
        record.stats.score = 12345;
        record.stats.gamerate = 1f;
        String data = NetUtil.compressString(record.exportString());
        java.util.zip.Adler32 checksum = new java.util.zip.Adler32();
        checksum.update(NetUtil.stringToBytes(data));

        String reply = records.registerSPRecord(room, me,
                ("spsend\t" + checksum.getValue() + "\t" + data).split("\t", -1));
        assertTrue(reply.startsWith("spsendok\t0\t"), "First record ranks #0: " + reply);

        String rankingReply = records.buildSPRankingReply(
                ("spranking\t" + NetUtil.urlEncode("any") + "\t" + NetUtil.urlEncode(mode) + "\t0\tfalse").split("\t", -1),
                me);
        assertTrue(rankingReply.contains("\t1\t"), "One record listed: " + rankingReply);
        assertTrue(rankingReply.contains(NetUtil.urlEncode("Me")));

        // Persists across instances
        String reloaded = newRecords().buildSPRankingReply(
                ("spranking\t" + NetUtil.urlEncode("any") + "\t" + NetUtil.urlEncode(mode) + "\t0\tfalse").split("\t", -1),
                me);
        assertTrue(reloaded.contains(NetUtil.urlEncode("Me")));
    }

    @Test
    void spBadChecksumAndDailyDegrade() {
        RoomLocalRecords records = newRecords();
        NetRoomInfo room = new NetRoomInfo();
        room.strMode = "whatever";
        NetPlayerInfo me = new NetPlayerInfo();
        me.strName = "Me";

        assertEquals("spsendng", records.registerSPRecord(room, me,
                "spsend\t12345\tcorrupted".split("\t", -1)));

        String daily = records.buildSPRankingReply(
                ("spranking\t" + NetUtil.urlEncode("any") + "\tmode\t0\ttrue").split("\t", -1), me);
        assertTrue(daily.endsWith("\ttrue\t0\t0"), "Daily rankings answer empty: " + daily);
    }
}

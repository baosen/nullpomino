// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.zip.Adler32;

import nullpomino.game.component.Statistics;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetSPModeRegistry;
import nullpomino.game.net.NetSPRecord;
import nullpomino.game.net.NetUtil;
import nullpomino.game.play.GameEngine;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Branch-gap tests for {@link RoomLocalRecords}: out-of-range styles,
 * leaderboard capping and rating ties, skipping corrupt persisted rows,
 * rated-room rule names, unranked/worse records, and the unranked-self
 * personal-best append paths of the spranking reply.
 */
class RoomLocalRecordsBranchGapTest {

    @TempDir
    File tempDir;

    /** Style-0 mode with RANKINGTYPE_GENERIC_SCORE: higher score = better */
    private static final String MODE = NetSPModeRegistry.forStyle(0).get(0).name();
    private static final int RANKING_TYPE = NetSPModeRegistry.forStyle(0).get(0).rankingType();

    private RoomLocalRecords newRecords() {
        return new RoomLocalRecords(tempDir.getAbsolutePath());
    }

    private static NetPlayerInfo player(String name) {
        NetPlayerInfo p = new NetPlayerInfo();
        p.strName = name;
        return p;
    }

    private static NetRoomInfo spRoom(boolean rated, String ruleName) {
        NetRoomInfo room = new NetRoomInfo();
        room.singleplayer = true;
        room.rated = rated;
        room.ruleName = ruleName;
        room.strMode = MODE;
        room.style = 0;
        return room;
    }

    private static String[] spsend(int score) {
        NetSPRecord record = new NetSPRecord();
        record.stats = new Statistics();
        record.stats.score = score;
        record.stats.gamerate = 1f;
        String data = NetUtil.compressString(record.exportString());
        Adler32 checksum = new Adler32();
        checksum.update(NetUtil.stringToBytes(data));
        return ("spsend\t" + checksum.getValue() + "\t" + data).split("\t", -1);
    }

    private static String[] spranking(String rule, String mode, int gameType) {
        return ("spranking\t" + NetUtil.urlEncode(rule) + "\t" + NetUtil.urlEncode(mode)
                + "\t" + gameType + "\tfalse").split("\t", -1);
    }

    private static NetSPRecord personalBest(String name, int score) {
        NetSPRecord rec = new NetSPRecord();
        rec.strPlayerName = name;
        rec.strModeName = MODE;
        rec.strRuleName = "any";
        rec.gameType = 0;
        rec.style = 0;
        rec.stats = new Statistics();
        rec.stats.score = score;
        rec.stats.gamerate = 1f;
        rec.strTimeStamp = GeneralUtil.exportCalendarString();
        return rec;
    }

    // ---------------------------------------------------------------- MP leaderboard

    @Test
    void outOfRangeStylesAreIgnored() {
        RoomLocalRecords records = newRecords();
        records.recordRatedResult(-1, "Alice", 1600, true);
        records.recordRatedResult(GameEngine.MAX_GAMESTYLE, "Alice", 1600, true);

        String data = NetUtil.decompressString(records.buildMPRankingReply(0, null).split("\t")[3]);
        assertEquals("", data, "nothing recorded for out-of-range styles");
    }

    @Test
    void leaderboardIsCappedAtMaxEntries() {
        RoomLocalRecords records = newRecords();
        for (int i = 0; i <= RoomLocalRecords.MAX_MPRANKING; i++) {
            records.recordRatedResult(0, "P" + i, 1000 + i, false);
        }
        String data = NetUtil.decompressString(records.buildMPRankingReply(0, null).split("\t")[3]);
        String[] rows = data.split("\t");
        assertEquals(RoomLocalRecords.MAX_MPRANKING, rows.length);
        // The lowest-rated player (P0, rating 1000) fell off the end
        assertFalse(data.contains(";" + NetUtil.urlEncode("P0") + ";"));
    }

    @Test
    void tiedRatingsShareTheSameRank() {
        RoomLocalRecords records = newRecords();
        records.recordRatedResult(0, "Alice", 1500, true);
        records.recordRatedResult(0, "Bob", 1500, false);

        String data = NetUtil.decompressString(records.buildMPRankingReply(0, null).split("\t")[3]);
        String[] rows = data.split("\t");
        assertEquals(2, rows.length);
        assertTrue(rows[0].startsWith("0;"));
        assertTrue(rows[1].startsWith("0;"), "equal rating shares rank 0: " + rows[1]);
    }

    @Test
    void loadSkipsLeaderboardRowsWithoutName() throws Exception {
        CustomProperties prop = new CustomProperties();
        prop.setProperty("mpranking.0.count", 2);
        prop.setProperty("mpranking.0.0.name", NetUtil.urlEncode("Alice"));
        prop.setProperty("mpranking.0.0.rating", 1555);
        // Row 1 has no name: skipped on load
        prop.storeToFile(tempDir.getAbsolutePath() + "/netplay_mpranking.cfg", "test");

        String data = NetUtil.decompressString(newRecords().buildMPRankingReply(0, null).split("\t")[3]);
        String[] rows = data.split("\t");
        assertEquals(1, rows.length);
        assertTrue(rows[0].contains(NetUtil.urlEncode("Alice")));
    }

    // ---------------------------------------------------------------- SP record registration

    @Test
    void ratedRoomRecordsUnderItsRuleName() {
        RoomLocalRecords records = newRecords();
        String reply = records.registerSPRecord(spRoom(true, "SOMERULE"), player("Me"), spsend(500));
        assertTrue(reply.startsWith("spsendok\t0\t"), reply);

        String ranked = records.buildSPRankingReply(spranking("SOMERULE", MODE, 0), null);
        assertTrue(ranked.contains(NetUtil.urlEncode("Me")), "record filed under the rule name: " + ranked);
        String anyRule = records.buildSPRankingReply(spranking("any", MODE, 0), null);
        assertTrue(anyRule.endsWith("\t0\t0"), "nothing under 'any': " + anyRule);
    }

    @Test
    void unknownModeIsAcceptedButNotRanked() {
        RoomLocalRecords records = newRecords();
        NetRoomInfo room = spRoom(false, "");
        room.strMode = "NO-SUCH-MODE";
        String reply = records.registerSPRecord(room, player("Me"), spsend(500));
        assertEquals("spsendok\t-1\tfalse\t-1", reply);
    }

    @Test
    void worseRecordNeitherRanksNorSetsPersonalBest() {
        RoomLocalRecords records = newRecords();
        NetPlayerInfo me = player("Me");
        assertTrue(records.registerSPRecord(spRoom(false, ""), me, spsend(500)).startsWith("spsendok\t0\ttrue"));
        // Same player, lower score: rank -1, no new personal best
        assertEquals("spsendok\t-1\tfalse\t-1",
                records.registerSPRecord(spRoom(false, ""), me, spsend(100)));
    }

    // ---------------------------------------------------------------- spranking replies

    @Test
    void multiRecordReplyHandlesNullAndUnrankedSelf() {
        RoomLocalRecords records = newRecords();
        records.registerSPRecord(spRoom(false, ""), player("Alice"), spsend(200));
        records.registerSPRecord(spRoom(false, ""), player("Bob"), spsend(100));

        // Null self: rows are ';'-joined, nothing appended
        String noSelf = records.buildSPRankingReply(spranking("any", MODE, 0), null);
        assertTrue(noSelf.contains("\t2\t"), "two records: " + noSelf);
        assertFalse(noSelf.contains("-1," + NetUtil.urlEncode("Carol")));

        // Unranked self without a personal best: nothing appended
        String noPB = records.buildSPRankingReply(spranking("any", MODE, 0), player("Carol"));
        assertTrue(noPB.contains("\t2\t"), "still two records: " + noPB);
        assertFalse(noPB.contains(NetUtil.urlEncode("Carol")));

        // Unranked self WITH a personal best: appended after a ',' with rank -1
        NetPlayerInfo dave = player("Dave");
        dave.spPersonalBest.registerRecord(RANKING_TYPE, personalBest("Dave", 42));
        String withPB = records.buildSPRankingReply(spranking("any", MODE, 0), dave);
        assertTrue(withPB.contains("\t3\t"), "self appended as an extra row: " + withPB);
        assertTrue(withPB.contains(",-1," + NetUtil.urlEncode("Dave") + ","), withPB);
    }

    @Test
    void emptyRankingAppendsPersonalBestWithoutSeparator() throws Exception {
        // Hand-written index: row 0 has no mode (skipped on load), row 1 is a
        // valid but EMPTY ranking for (any, MODE, 0)
        CustomProperties prop = new CustomProperties();
        prop.setProperty("spindex.count", 2);
        prop.setProperty("spindex.0.rule", "any");
        prop.setProperty("spindex.1.rule", "any");
        prop.setProperty("spindex.1.mode", MODE);
        prop.setProperty("spindex.1.gameType", 0);
        prop.setProperty("spindex.1.style", 0);
        prop.setProperty("spindex.1.rankingType", RANKING_TYPE);
        prop.storeToFile(tempDir.getAbsolutePath() + "/netplay_spranking.cfg", "test");

        NetPlayerInfo me = player("Me");
        me.spPersonalBest.registerRecord(RANKING_TYPE, personalBest("Me", 77));

        String reply = newRecords().buildSPRankingReply(spranking("any", MODE, 0), me);
        assertTrue(reply.contains("\t1\t"), "personal best is the only row: " + reply);
        assertTrue(reply.contains("\t-1," + NetUtil.urlEncode("Me") + ","),
                "no leading ',' when the ranking was empty: " + reply);
    }

    @Test
    void rankingLookupRequiresRuleModeAndGameTypeToMatch() {
        RoomLocalRecords records = newRecords();
        records.registerSPRecord(spRoom(false, ""), player("Me"), spsend(500));

        assertTrue(records.buildSPRankingReply(spranking("otherrule", MODE, 0), null).endsWith("\t0\t0"));
        assertTrue(records.buildSPRankingReply(spranking("any", "OTHER-MODE", 0), null).endsWith("\t0\t0"));
        assertTrue(records.buildSPRankingReply(spranking("any", MODE, 1), null).endsWith("\t0\t0"));
        assertFalse(records.buildSPRankingReply(spranking("any", MODE, 0), null).endsWith("\t0\t0"));
    }
}

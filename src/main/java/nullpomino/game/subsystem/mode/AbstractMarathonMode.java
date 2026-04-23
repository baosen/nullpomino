/*
    Copyright (c) 2010, NullNoname
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright
          notice, this list of conditions and the following disclaimer in the
          documentation and/or other materials provided with the distribution.
        * Neither the name of NullNoname nor the names of its
          contributors may be used to endorse or promote products derived from
          this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
*/
package nullpomino.game.subsystem.mode;

import nullpomino.game.play.GameEngine;
import nullpomino.util.CustomProperties;

/**
 * Shared scaffolding for MARATHON and MARATHON+. Holds the fields
 * and helpers that both modes have word-for-word identical — scoring
 * event bookkeeping, T-spin/B2B/combo config, ranking tables, soft /
 * hard drop score deltas, ranking insert/lookup — so subclasses
 * differ only in the bits that actually diverge (gravity tables,
 * scoring law, bonus-level mechanics, net options layout).
 *
 * Property-backed I/O uses the subclass-supplied
 * {@link #getPropertyPrefix()} ("marathon", "marathonplus", ...) so
 * user configs and replay files keep byte-identical key names.
 */
public abstract class AbstractMarathonMode extends NetDummyMode {

	protected static final int RANKING_MAX = 10;

	/** Most recent scoring event constants */
	protected static final int EVENT_NONE = 0,
							   EVENT_SINGLE = 1,
							   EVENT_DOUBLE = 2,
							   EVENT_TRIPLE = 3,
							   EVENT_FOUR = 4,
							   EVENT_TSPIN_ZERO_MINI = 5,
							   EVENT_TSPIN_ZERO = 6,
							   EVENT_TSPIN_SINGLE_MINI = 7,
							   EVENT_TSPIN_SINGLE = 8,
							   EVENT_TSPIN_DOUBLE_MINI = 9,
							   EVENT_TSPIN_DOUBLE = 10,
							   EVENT_TSPIN_TRIPLE = 11,
							   EVENT_TSPIN_EZ = 12;

	/** Most recent increase in score */
	protected int lastscore;
	/** Time to display the most recent increase in score */
	protected int scgettime;
	/** Most recent scoring event type */
	protected int lastevent;
	/** True if most recent scoring event is a B2B */
	protected boolean lastb2b;
	/** Combo count for most recent scoring event */
	protected int lastcombo;
	/** Piece ID for most recent scoring event */
	protected int lastpiece;
	/** Current BGM */
	protected int bgmlv;

	/** Level at start time */
	protected int startlevel;

	/** T-spin config (0=off, 1=normal, 2=all-spin) */
	protected int tspinEnableType;
	/** Legacy T-spin enable flag */
	protected boolean enableTSpin;
	/** Wallkick T-spins enabled */
	protected boolean enableTSpinKick;
	/** Spin check type (4-point vs immobile) */
	protected int spinCheckType;
	/** Immobile EZ-spin enabled */
	protected boolean tspinEnableEZ;
	/** B2B enabled */
	protected boolean enableB2B;
	/** Combos enabled */
	protected boolean enableCombo;

	/** Big-blocks mode */
	protected boolean big;

	/** Persisted version stamp */
	protected int version;

	/** Current round's ranking rank (-1 if unranked) */
	protected int rankingRank;
	/** Rankings' scores */
	protected int[][] rankingScore;
	/** Rankings' line counts */
	protected int[][] rankingLines;
	/** Rankings' times */
	protected int[][] rankingTime;

	/**
	 * @return property-key prefix (without a trailing dot) used for
	 *         every CustomProperties read/write — e.g. "marathon".
	 *         Mirrors the string on-disk; do NOT rename once shipped.
	 */
	protected abstract String getPropertyPrefix();

	/**
	 * @return number of ranking game types to store — dimensions
	 *         ranking tables as {@code int[getGameTypeCount()][RANKING_MAX]}.
	 */
	protected abstract int getGameTypeCount();

	/** Allocate the three ranking tables; call from subclass playerInit. */
	protected void allocateRankingArrays() {
		int types = getGameTypeCount();
		rankingScore = new int[types][RANKING_MAX];
		rankingLines = new int[types][RANKING_MAX];
		rankingTime = new int[types][RANKING_MAX];
	}

	@Override
	public void afterSoftDropFall(GameEngine engine, int playerID, int fall) {
		engine.statistics.scoreFromSoftDrop += fall;
		engine.statistics.score += fall;
	}

	@Override
	public void afterHardDropFall(GameEngine engine, int playerID, int fall) {
		engine.statistics.scoreFromHardDrop += fall * 2;
		engine.statistics.score += fall * 2;
	}

	/**
	 * Insert (sc, li, time) into the given ranking bucket if it beats
	 * the current floor, shifting lower entries down. Sets
	 * {@link #rankingRank} to the new position, or -1 if unranked.
	 */
	protected void updateRanking(int sc, int li, int time, int type) {
		rankingRank = checkRanking(sc, li, time, type);
		if (rankingRank != -1) {
			for (int i = RANKING_MAX - 1; i > rankingRank; i--) {
				rankingScore[type][i] = rankingScore[type][i - 1];
				rankingLines[type][i] = rankingLines[type][i - 1];
				rankingTime[type][i] = rankingTime[type][i - 1];
			}
			rankingScore[type][rankingRank] = sc;
			rankingLines[type][rankingRank] = li;
			rankingTime[type][rankingRank] = time;
		}
	}

	/** @return position the new score would slot into, or -1. */
	protected int checkRanking(int sc, int li, int time, int type) {
		for (int i = 0; i < RANKING_MAX; i++) {
			if (sc > rankingScore[type][i]) return i;
			if (sc == rankingScore[type][i] && li > rankingLines[type][i]) return i;
			if (sc == rankingScore[type][i] && li == rankingLines[type][i] && time < rankingTime[type][i]) return i;
		}
		return -1;
	}

	@Override
	protected void loadRanking(CustomProperties prop, String ruleName) {
		String prefix = getPropertyPrefix();
		int types = getGameTypeCount();
		for (int i = 0; i < RANKING_MAX; i++) {
			for (int j = 0; j < types; j++) {
				rankingScore[j][i] = prop.getProperty(prefix + ".ranking." + ruleName + "." + j + ".score." + i, 0);
				rankingLines[j][i] = prop.getProperty(prefix + ".ranking." + ruleName + "." + j + ".lines." + i, 0);
				rankingTime[j][i]  = prop.getProperty(prefix + ".ranking." + ruleName + "." + j + ".time."  + i, 0);
			}
		}
	}

	/** Save the ranking tables back out under the subclass's prefix. */
	protected void saveRanking(CustomProperties prop, String ruleName) {
		String prefix = getPropertyPrefix();
		int types = getGameTypeCount();
		for (int i = 0; i < RANKING_MAX; i++) {
			for (int j = 0; j < types; j++) {
				prop.setProperty(prefix + ".ranking." + ruleName + "." + j + ".score." + i, rankingScore[j][i]);
				prop.setProperty(prefix + ".ranking." + ruleName + "." + j + ".lines." + i, rankingLines[j][i]);
				prop.setProperty(prefix + ".ranking." + ruleName + "." + j + ".time."  + i, rankingTime[j][i]);
			}
		}
	}

	/**
	 * Read the settings both Marathon variants share. Subclasses call
	 * this from their own {@code loadSetting} and add any extras
	 * (e.g. Marathon's {@code gametype}) afterward.
	 */
	protected void loadCoreSettings(CustomProperties prop) {
		String p = getPropertyPrefix();
		startlevel      = prop.getProperty(p + ".startlevel", 0);
		tspinEnableType = prop.getProperty(p + ".tspinEnableType", 1);
		enableTSpin     = prop.getProperty(p + ".enableTSpin", true);
		enableTSpinKick = prop.getProperty(p + ".enableTSpinKick", true);
		spinCheckType   = prop.getProperty(p + ".spinCheckType", 0);
		tspinEnableEZ   = prop.getProperty(p + ".tspinEnableEZ", false);
		enableB2B       = prop.getProperty(p + ".enableB2B", true);
		enableCombo     = prop.getProperty(p + ".enableCombo", true);
		big             = prop.getProperty(p + ".big", false);
		version         = prop.getProperty(p + ".version", 0);
	}

	/** Mirror of {@link #loadCoreSettings(CustomProperties)}. */
	protected void saveCoreSettings(CustomProperties prop) {
		String p = getPropertyPrefix();
		prop.setProperty(p + ".startlevel", startlevel);
		prop.setProperty(p + ".tspinEnableType", tspinEnableType);
		prop.setProperty(p + ".enableTSpin", enableTSpin);
		prop.setProperty(p + ".enableTSpinKick", enableTSpinKick);
		prop.setProperty(p + ".spinCheckType", spinCheckType);
		prop.setProperty(p + ".tspinEnableEZ", tspinEnableEZ);
		prop.setProperty(p + ".enableB2B", enableB2B);
		prop.setProperty(p + ".enableCombo", enableCombo);
		prop.setProperty(p + ".big", big);
		prop.setProperty(p + ".version", version);
	}
}

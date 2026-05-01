// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.mode;

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
		RankingHelper.insertAt(rankingRank, RANKING_MAX,
			(to, from) -> {
				rankingScore[type][to] = rankingScore[type][from];
				rankingLines[type][to] = rankingLines[type][from];
				rankingTime[type][to] = rankingTime[type][from];
			},
			rank -> {
				rankingScore[type][rank] = sc;
				rankingLines[type][rank] = li;
				rankingTime[type][rank] = time;
			});
	}

	/** @return position the new score would slot into, or -1. */
	protected int checkRanking(int sc, int li, int time, int type) {
		return RankingHelper.findRank(RANKING_MAX, i ->
			sc > rankingScore[type][i]
				|| (sc == rankingScore[type][i] && li > rankingLines[type][i])
				|| (sc == rankingScore[type][i] && li == rankingLines[type][i] && time < rankingTime[type][i]));
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

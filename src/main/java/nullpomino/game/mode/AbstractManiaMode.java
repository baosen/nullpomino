// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.mode;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;

/**
 * Shared scaffolding for the mania-family modes (GRADE MANIA 1-3,
 * SPEED MANIA 1-2, PHANTOM MANIA, FINAL, GARBAGE MANIA, SCORE ATTACK).
 * Holds the section-time / medal / grade-ranking state and helpers that
 * these modes had word-for-word identical, so subclasses keep only the
 * parts that actually diverge (speed curves, grading laws, section
 * counts). Subclasses still allocate the section arrays themselves with
 * their own SECTION_MAX, and modes with diverging table shapes (GRADE
 * MANIA 3's per-gametype 2D rankings) shadow the base fields and
 * override the helpers.
 */
public abstract class AbstractManiaMode extends AbstractMode {

	/** Number of ranking records */
	protected static final int RANKING_MAX = 10;

	/** Section Time */
	protected int[] sectiontime;

	/** This will be true if the player achieves new section time record in specific section */
	protected boolean[] sectionIsNewRecord;

	/** This will be true if the player achieves new section time record somewhere */
	protected boolean sectionAnyNewRecord;

	/** Amount of sections completed */
	protected int sectionscomp;

	/** Average section time */
	protected int sectionavgtime;

	/** Current section time */
	protected int sectionlasttime;

	/** Best section time records */
	protected int[] bestSectionTime;

	/** false:Leaderboard, true:Section time record (Push F in settings screen to flip it) */
	protected boolean isShowBestSectionTime;

	/** ST medal */
	protected int medalST;

	/** Hard-drop bonus count */
	protected int harddropBonus;

	/** Grades in ranking */
	protected int[] rankingGrade;

	/** Level records in ranking */
	protected int[] rankingLevel;

	/** Time records in ranking */
	protected int[] rankingTime;

	/** Game completed flags in ranking */
	protected int[] rankingRollclear;

	/** Rank of current play (-1 if unranked) */
	protected int rankingRank;

	/*
	 * Results screen
	 */
	@Override
	public boolean onResult(GameEngine engine, int playerID) {
		// Page change
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
			engine.statc[1]--;
			if(engine.statc[1] < 0) engine.statc[1] = 2;
			engine.playSE("change");
		}
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			engine.statc[1]++;
			if(engine.statc[1] > 2) engine.statc[1] = 0;
			engine.playSE("change");
		}
		// Flip Leaderboard/Best Section Time Records
		if(engine.ctrl.isPush(Controller.BUTTON_F)) {
			engine.playSE("change");
			isShowBestSectionTime = !isShowBestSectionTime;
		}

		return false;
	}

	/*
	 * Called when hard drop used
	 */
	@Override
	public void afterHardDropFall(GameEngine engine, int playerID, int fall) {
		if(fall * 2 > harddropBonus) harddropBonus = fall * 2;
	}

	/**
	 * Check for ST medal
	 * @param engine GameEngine
	 * @param sectionNumber Section Number
	 */
	protected void stMedalCheck(GameEngine engine, int sectionNumber) {
		int best = bestSectionTime[sectionNumber];

		if(sectionlasttime < best) {
			if(medalST < 3) {
				engine.playSE("medal");
				medalST = 3;
			}
			if(!owner.replayMode) {
				sectionIsNewRecord[sectionNumber] = true;
			}
		} else if((sectionlasttime < best + 300) && (medalST < 2)) {
			engine.playSE("medal");
			medalST = 2;
		} else if((sectionlasttime < best + 600) && (medalST < 1)) {
			engine.playSE("medal");
			medalST = 1;
		}
	}

	/**
	 * Check for new section time records
	 * @param sectionNumber Section Number
	 */
	protected void stNewRecordCheck(int sectionNumber) {
		if((sectiontime[sectionNumber] < bestSectionTime[sectionNumber]) && (!owner.replayMode)) {
			sectionIsNewRecord[sectionNumber] = true;
			sectionAnyNewRecord = true;
		}
	}

	/**
	 * Update best section time records after a game
	 */
	protected void updateBestSectionTime() {
		for(int i = 0; i < sectionIsNewRecord.length; i++) {
			if(sectionIsNewRecord[i]) {
				bestSectionTime[i] = sectiontime[i];
			}
		}
	}

	/**
	 * Get medal font color
	 * @param medalColor Medal status
	 * @return Font color constant, or -1 if no medal
	 */
	protected int getMedalFontColor(int medalColor) {
		if(medalColor == 1) return EventReceiver.COLOR_RED;
		if(medalColor == 2) return EventReceiver.COLOR_WHITE;
		if(medalColor == 3) return EventReceiver.COLOR_YELLOW;
		return -1;
	}

	/**
	 * Update rankings
	 * @param gr Grade
	 * @param lv Level
	 * @param time Time
	 * @param clear Game completed flag
	 */
	protected void updateRanking(int gr, int lv, int time, int clear) {
		rankingRank = checkRanking(gr, lv, time, clear);
		RankingHelper.insertAt(rankingRank, RANKING_MAX,
			(to, from) -> {
				rankingGrade[to] = rankingGrade[from];
				rankingLevel[to] = rankingLevel[from];
				rankingTime[to] = rankingTime[from];
				rankingRollclear[to] = rankingRollclear[from];
			},
			rank -> {
				rankingGrade[rank] = gr;
				rankingLevel[rank] = lv;
				rankingTime[rank] = time;
				rankingRollclear[rank] = clear;
			});
	}

	/**
	 * Calculate ranking position
	 * @param gr Grade
	 * @param lv Level
	 * @param time Time
	 * @param clear Game completed flag
	 * @return Position (-1 if unranked)
	 */
	protected int checkRanking(int gr, int lv, int time, int clear) {
		return RankingHelper.findRank(RANKING_MAX, i ->
			(clear > rankingRollclear[i])
				|| ((clear == rankingRollclear[i]) && (gr > rankingGrade[i]))
				|| ((clear == rankingRollclear[i]) && (gr == rankingGrade[i]) && (lv > rankingLevel[i]))
				|| ((clear == rankingRollclear[i]) && (gr == rankingGrade[i])
					&& (lv == rankingLevel[i]) && (time < rankingTime[i])));
	}
}

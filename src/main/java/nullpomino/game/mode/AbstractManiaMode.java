// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.mode;

import nullpomino.game.component.Controller;
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
}

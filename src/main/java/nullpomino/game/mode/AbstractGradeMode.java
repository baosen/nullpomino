// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.mode;

/**
 * Shared scalar-field scaffolding for GRADE MANIA 2 and GRADE MANIA 3.
 * These two modes track the same in-round telemetry (gravity index,
 * section times, grade flash, medals, ranking rank, …) with identical
 * names and types — only the surrounding scoring law, grade-table shape,
 * and ranking dimensionality differ. Moving the shared state here lets
 * GM2 and GM3 drop ~30 field declarations each without changing wire or
 * on-disk formats.
 *
 * GRADE MANIA 1 uses a different (score-based) model and is intentionally
 * NOT parented here.
 */
public abstract class AbstractGradeMode extends AbstractMode {

	/** Number of entries in rankings */
	protected static final int RANKING_MAX = 10;

	/** Number of sections */
	protected static final int SECTION_MAX = 10;

	/** Default section time */
	protected static final int DEFAULT_SECTION_TIME = 5400;

	/** Current gravity index into the gravity/denominator table */
	protected int gravityindex;
	/** Next section level (stops at level-1 until the section begins) */
	protected int nextseclv;
	/** Level-up pending flag */
	protected boolean lvupflag;
	/** The displayed grade */
	protected int grade;
	/** Frame timestamp of the most recent grade-up */
	protected int lastGradeTime;
	/** Hard-drop bonus count */
	protected int harddropBonus;
	/** Combo bonus */
	protected int comboValue;
	/** Most recent increase in score */
	protected int lastscore;
	/** Frames remaining to show the last score delta */
	protected int scgettime;
	/** Roll course elapsed time */
	protected int rolltime;
	/** Roll completion flag */
	protected int rollclear;
	/** True while the ending roll is running */
	protected boolean rollstarted;
	/** Secret (back) grade */
	protected int secretGrade;
	/** Current BGM slot */
	protected int bgmlv;
	/** Frames remaining to flash the grade display */
	protected int gradeflash;

	/** Per-section elapsed time */
	protected int[] sectiontime;
	/** True for sections that set a new best */
	protected boolean[] sectionIsNewRecord;
	/** Number of completed sections */
	protected int sectionscomp;
	/** Average section time */
	protected int sectionavgtime;
	/** Previous section's time */
	protected int sectionlasttime;

	/** True once M-ROLL (vanish roll) has begun */
	protected boolean mrollFlag;

	/** AC medal state */
	protected int medalAC;
	/** ST medal state */
	protected int medalST;
	/** SK medal state */
	protected int medalSK;
	/** CO medal state */
	protected int medalCO;

	/** True when best-section-time display is enabled */
	protected boolean isShowBestSectionTime;

	/** Persisted schema version */
	protected int version;
	/** Current round's ranking rank (-1 if unranked) */
	protected int rankingRank;
}

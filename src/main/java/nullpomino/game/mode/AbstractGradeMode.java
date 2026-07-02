// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.mode;

import nullpomino.game.play.GameEngine;

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
public abstract class AbstractGradeMode extends AbstractManiaMode {

	/** Number of sections */
	protected static final int SECTION_MAX = 10;

	/** Default section time */
	protected static final int DEFAULT_SECTION_TIME = 5400;

	/** Fall velocity table (shared by GRADE MANIA 2 and GRADE MANIA 3) */
	protected static final int[] tableGravityValue =
	{
		4, 6, 8, 10, 12, 16, 32, 48, 64, 80, 96, 112, 128, 144, 4, 32, 64, 96, 128, 160, 192, 224, 256, 512, 768, 1024, 1280, 1024, 768, -1
	};

	/** Fall velocity changes level (shared by GRADE MANIA 2 and GRADE MANIA 3) */
	protected static final int[] tableGravityChangeLevel =
	{
		30, 35, 40, 50, 60, 70, 80, 90, 100, 120, 140, 160, 170, 200, 220, 230, 233, 236, 239, 243, 247, 251, 300, 330, 360, 400, 420, 450, 500, 10000
	};

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

	/** True once M-ROLL (vanish roll) has begun */
	protected boolean mrollFlag;

	/** AC medal state */
	protected int medalAC;
	/** SK medal state */
	protected int medalSK;
	/** CO medal state */
	protected int medalCO;

	/** Persisted schema version */
	protected int version;

	/*
	 * Called when hard drop used
	 */
	@Override
	public void afterHardDropFall(GameEngine engine, int playerID, int fall) {
		if(fall * 2 > harddropBonus) harddropBonus = fall * 2;
	}

	/** Update the combo counter for a line clear (shared by GRADE MANIA 2 and GRADE MANIA 3). */
	protected void updateCombo(int lines) {
		// Combo
		if(lines == 0) {
			comboValue = 1;
		} else {
			comboValue = comboValue + (2 * lines) - 2;
			if(comboValue < 1) comboValue = 1;
		}
	}
}

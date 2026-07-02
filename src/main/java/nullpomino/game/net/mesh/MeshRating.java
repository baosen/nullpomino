// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mesh;

/**
 * ELO rating math for rated mesh games - the same formulas NetServer uses,
 * with its default parameters (mesh has no server config to tune them).
 */
public final class MeshRating {
	/** Rating floor/ceiling */
	public static final int RATING_MIN = 0;
	public static final int RATING_MAX = 99999;

	/** Starting rating (matches NetPlayerInfo.DEFAULT_MULTIPLAYER_RATING) */
	public static final int RATING_DEFAULT = 1500;

	/** K-value per game once past the provisional phase */
	public static final double NORMAL_MAX_DIFF = 16;

	/** Number of games with the boosted provisional K-value */
	public static final int PROVISIONAL_GAMES = 50;

	private MeshRating() {}

	/**
	 * Rating change for one pairwise result.
	 * @param playedGames Number of games played by the player
	 * @param myRank Player's rating
	 * @param oppRank Opponent's rating
	 * @param myScore 0:Loss, 1:Win
	 * @return Rating delta (positive or negative)
	 */
	public static double rankDelta(int playedGames, double myRank, double oppRank, double myScore) {
		return maxDelta(playedGames) * (myScore - expectedScore(myRank, oppRank));
	}

	/** Expected score against an opponent (0..1) */
	public static double expectedScore(double myRank, double oppRank) {
		return 1.0 / (1 + Math.pow(10, (oppRank - myRank) / 400.0));
	}

	/** Multiplier of rating change (boosted while provisional) */
	public static double maxDelta(int playedGames) {
		return playedGames > PROVISIONAL_GAMES
				? NORMAL_MAX_DIFF
				: NORMAL_MAX_DIFF + 400 / (playedGames + 3);
	}
}

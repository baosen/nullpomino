// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.ai;

import nullpomino.game.component.Controller;
import nullpomino.game.play.GameEngine;

/**
 * DummyAI - Base class for AI players
 */
public class DummyAI implements AIPlayer {
	/** ホールド使用予定 */
	public boolean bestHold;

	/** Plan to putX-coordinate */
	public int bestX;

	/** Plan to putY-coordinate */
	public int bestY;

	/** Plan to putDirection */
	public int bestRt;

	/** Hold Force */
	public boolean forceHold;

	/** Current piece number */
	public int thinkCurrentPieceNo;

	/** Peace of thoughts is finished number */
	public int thinkLastPieceNo;

	/** Did the thinking thread finish successfully? */
	public boolean thinkComplete;

	public String getName() {
		return "DummyAI";
	}

	public void init(GameEngine engine, int playerID) {
	}

	public void newPiece(GameEngine engine, int playerID) {
	}

	public void onFirst(GameEngine engine, int playerID) {
	}

	public void onLast(GameEngine engine, int playerID) {
	}

	public void renderState(GameEngine engine, int playerID) {
	}

	public void setControl(GameEngine engine, int playerID, Controller ctrl) {
	}

	public void shutdown(GameEngine engine, int playerID) {
	}

	public void renderHint(GameEngine engine, int playerID) {
	}
}

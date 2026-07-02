package nullpomino.game.mode;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

/**
 * NET-VS-DIG RACE mode
 */
public class NetVSDigRaceMode extends NetDummyVSMode {
	/** Number of garbage lines to clear */
	private int goalLines;	// TODO: Add option to change this

	/** Number of garbage lines left */
	private int[] playerRemainLines;

	/** Number of gems available at the start of the game (for map game) */
	private int[] playerStartGems;

	/*
	 * Mode name
	 */
	@Override
	public String getName() {
		return "NET-VS-DIG RACE";
	}

	/*
	 * Mode init
	 */
	@Override
	public void modeInit(GameManager manager) {
		super.modeInit(manager);
		goalLines = 18;
		playerRemainLines = new int[NETVS_MAX_PLAYERS];
		playerStartGems = new int[NETVS_MAX_PLAYERS];
	}

	/**
	 * Apply room settings, but ignore non-speed settings
	 */
	@Override
	protected void netvsApplyRoomSettings(GameEngine engine) {
		netvsApplySpeedSettings(engine);
	}

	/**
	 * Fill the playfield with garbage
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	private void fillGarbage(GameEngine engine, int playerID) {
		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		int hole = -1;
		int skin = engine.getSkin();
		if((playerID != 0) || netvsIsWatch()) skin = netvsPlayerSkin[playerID];
		if(skin < 0) skin = 0;

		for(int y = h - 1; y >= h - goalLines; y--) {
			if((hole == -1) || (engine.random.nextInt(100) < netCurrentRoomInfo.garbagePercent)) {
				int newhole = -1;
				do {
					newhole = engine.random.nextInt(w);
				} while(newhole == hole);
				hole = newhole;
			}

			int prevColor = -1;
			for(int x = 0; x < w; x++) {
				if(x != hole) {
					int color = Block.BLOCK_COLOR_GRAY;
					if(y == h - 1) {
						do {
							color = Block.BLOCK_COLOR_GEM_RED + engine.random.nextInt(7);
						} while(color == prevColor);
						prevColor = color;
					}
					engine.field.setBlock(x,y,new Block(color,skin,Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
				}
			}

			// Set connections
			if(y != h - 1) {
				for(int x = 0; x < w; x++) {
					if(x != hole) {
						Block blk = engine.field.getBlock(x, y);
						if(blk != null) {
							if(!engine.field.getBlockEmpty(x-1, y)) blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, true);
							if(!engine.field.getBlockEmpty(x+1, y)) blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true);
						}
					}
				}
			}
		}
	}

	/**
	 * Get number of garbage lines left
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return Number of garbage lines left
	 */
	private int getRemainGarbageLines(GameEngine engine, int playerID) {
		if((engine == null) || (engine.field == null)) return -1;

		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		int lines = 0;
		boolean hasGemBlock = false;

		for(int y = h - 1; y >= h - goalLines; y--) {
			if(!engine.field.getLineFlag(y)) {
				for(int x = 0; x < w; x++) {
					Block blk = engine.field.getBlock(x, y);

					if((blk != null) && (blk.isGemBlock())) {
						hasGemBlock = true;
					}
					if((blk != null) && (blk.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE))) {
						lines++;
						break;
					}
				}
			}
		}

		if(!hasGemBlock) return 0;

		return lines;
	}

	/**
	 * Turn all normal blocks to gem (for map game)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	private void turnAllBlocksToGem(GameEngine engine, int playerID) {
		int w = engine.field.getWidth();
		int h = engine.field.getHeight();

		for(int y = engine.field.getHighestBlockY(); y < h; y++) {
			for(int x = 0; x < w; x++) {
				Block blk = engine.field.getBlock(x, y);
				if((blk != null) && (blk.color >= Block.BLOCK_COLOR_RED) && (blk.color <= Block.BLOCK_COLOR_PURPLE)) {
					blk.color = Block.BLOCK_COLOR_GEM_RED + (blk.color - 2);
				}
			}
		}
	}

	/*
	 * Ready
	 */
	@Override
	public boolean onReady(GameEngine engine, int playerID) {
		super.onReady(engine, playerID);

		if((engine.statc[0] == 0) && netvsPlayerExist[playerID]) {
			if((netCurrentRoomInfo == null) || !netCurrentRoomInfo.useMap) {
				// Fill the field with garbage
				engine.createFieldIfNeeded();
				fillGarbage(engine, playerID);

				// Update meter
				int remainLines = getRemainGarbageLines(engine, playerID);
				playerRemainLines[playerID] = remainLines;
				engine.meterValue = remainLines * owner.receiver.getBlockGraphicsHeight(engine, playerID);
				engine.meterColor = GameEngine.METER_COLOR_GREEN;
			} else {
				// Map game
				engine.createFieldIfNeeded();
				turnAllBlocksToGem(engine, playerID);
				playerStartGems[playerID] = engine.field.getHowManyGems();
				playerRemainLines[playerID] = playerStartGems[playerID];
				engine.meterValue = owner.receiver.getMeterMax(engine);
				engine.meterColor = GameEngine.METER_COLOR_GREEN;
			}
		}
		return false;
	}

	/**
	 * Get player's place
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return Player's place
	 */
	private int getNowPlayerPlace(GameEngine engine, int playerID) {
		if(!netvsPlayerExist[playerID] || netvsPlayerDead[playerID]) return -1;

		int place = 0;

		for(int i = 0; i < getPlayers(); i++) {
			if((i != playerID) && netvsPlayerExist[i] && !netvsPlayerDead[i] && (owner.engine[i].field != null)) {
				if(playerRemainLines[playerID] > playerRemainLines[i]) {
					place++;
				} else if( (playerRemainLines[playerID] == playerRemainLines[i]) &&
						   (engine.field.getHighestBlockY() < owner.engine[i].field.getHighestBlockY()) )
				{
					place++;
				}
			}
		}

		return place;
	}

	/**
	 * Update progress meter
	 * @param engine GameEngine
	 */
	private void updateMeter(GameEngine engine) {
		int playerID = engine.playerID;
		int remainLines = 0;

		if((netCurrentRoomInfo == null) || !netCurrentRoomInfo.useMap) {
			// Normal game
			remainLines = playerRemainLines[playerID];
			engine.meterValue = remainLines * owner.receiver.getBlockGraphicsHeight(engine, playerID);
			engine.meterColor = GameEngine.METER_COLOR_GREEN;
			if(remainLines <= 14) engine.meterColor = GameEngine.METER_COLOR_YELLOW;
			if(remainLines <= 8) engine.meterColor = GameEngine.METER_COLOR_ORANGE;
			if(remainLines <= 4) engine.meterColor = GameEngine.METER_COLOR_RED;
		} else if((engine.field != null) && (playerStartGems[playerID] > 0)) {
			// Map game
			remainLines = engine.field.getHowManyGems() - engine.field.getHowManyGemClears();
			engine.meterValue = (remainLines * owner.receiver.getMeterMax(engine)) / playerStartGems[playerID];
			engine.meterColor = GameEngine.METER_COLOR_GREEN;
			if(remainLines <= playerStartGems[playerID] / 2) engine.meterColor = GameEngine.METER_COLOR_YELLOW;
			if(remainLines <= playerStartGems[playerID] / 3) engine.meterColor = GameEngine.METER_COLOR_ORANGE;
			if(remainLines <= playerStartGems[playerID] / 4) engine.meterColor = GameEngine.METER_COLOR_RED;
		}
	}

	@Override
	public void calcScore(GameEngine engine, int playerID, int lines) {
		if((lines > 0) && (playerID == 0)) {
			if((netCurrentRoomInfo == null) || !netCurrentRoomInfo.useMap) {
				playerRemainLines[playerID] = getRemainGarbageLines(engine, playerID);
			} else if(engine.field != null) {
				playerRemainLines[playerID] = engine.field.getHowManyGems() - engine.field.getHowManyGemClears();
			}
			updateMeter(engine);

			// Game Completed
			if(playerRemainLines[playerID] <= 0) {
				if(netvsIsPractice) {
					engine.stat = GameEngine.Status.EXCELLENT;
					engine.resetStatc();
				} else {
					// Send game end message
					int[] places = new int[NETVS_MAX_PLAYERS];
					int[] uidArray = new int[NETVS_MAX_PLAYERS];
					for(int i = 0; i < getPlayers(); i++) {
						places[i] = getNowPlayerPlace(owner.engine[i], i);
						uidArray[i] = -1;
					}
					for(int i = 0; i < getPlayers(); i++) {
						if((places[i] >= 0) && (places[i] < NETVS_MAX_PLAYERS)) {
							uidArray[places[i]] = netvsPlayerUID[i];
						}
					}

					String strMsg = "racewin";
					for(int i = 0; i < getPlayers(); i++) {
						if(uidArray[i] != -1) strMsg += "\t" + uidArray[i];
					}
					strMsg += "\n";
					netLobby.netPlayerClient.send(strMsg);

					// Wait until everyone dies
					engine.stat = GameEngine.Status.NOTHING;
					engine.resetStatc();
				}
			}
		}
	}

	/*
	 * Drawing processing at the end of every frame
	 */
	@Override
	public void renderLast(GameEngine engine, int playerID) {
		super.renderLast(engine, playerID);

		int x = owner.receiver.getFieldDisplayPositionX(engine, playerID);
		int y = owner.receiver.getFieldDisplayPositionY(engine, playerID);
		int fontColor = EventReceiver.COLOR_WHITE;

		if(netvsPlayerExist[playerID] && engine.isVisible) {
			if( ((netvsIsGameActive) || ((netvsIsPractice) && (playerID == 0))) && (engine.stat != GameEngine.Status.RESULT) ) {
				// Lines left
				int remainLines = Math.max(0, playerRemainLines[playerID]);
				fontColor = EventReceiver.COLOR_WHITE;
				if((remainLines <= 14) && (remainLines > 0)) fontColor = EventReceiver.COLOR_YELLOW;
				if((remainLines <=  8) && (remainLines > 0)) fontColor = EventReceiver.COLOR_ORANGE;
				if((remainLines <=  4) && (remainLines > 0)) fontColor = EventReceiver.COLOR_RED;

				String strLines = String.valueOf(remainLines);

				if(engine.displaysize != -1) {
					if(strLines.length() == 1) {
						owner.receiver.drawMenuFont(engine, playerID, 4, 21, strLines, fontColor, 2.0f);
					} else if(strLines.length() == 2) {
						owner.receiver.drawMenuFont(engine, playerID, 3, 21, strLines, fontColor, 2.0f);
					} else if(strLines.length() == 3) {
						owner.receiver.drawMenuFont(engine, playerID, 2, 21, strLines, fontColor, 2.0f);
					}
				} else {
					if(strLines.length() == 1) {
						owner.receiver.drawDirectFont(engine, playerID, x + 4 + 32, y + 168, strLines, fontColor, 1.0f);
					} else if(strLines.length() == 2) {
						owner.receiver.drawDirectFont(engine, playerID, x + 4 + 24, y + 168, strLines, fontColor, 1.0f);
					} else if(strLines.length() == 3) {
						owner.receiver.drawDirectFont(engine, playerID, x + 4 + 16, y + 168, strLines, fontColor, 1.0f);
					}
				}
			}

			// Place and games count
			int place = getNowPlayerPlace(engine, playerID);
			if(netvsPlayerDead[playerID]) place = netvsPlayerPlace[playerID];
			netvsDrawPlaceAndPlayCount(engine, playerID, x, y, place);
		}
	}

	/*
	 * Render results screen
	 */
	@Override
	public void renderResult(GameEngine engine, int playerID) {
		super.renderResult(engine, playerID);
		netvsDrawRaceResultStats(engine, playerID);
	}

	/*
	 * Send stats
	 */
	@Override
	protected void netSendStats(GameEngine engine) {
		int playerID = engine.playerID;

		if((playerID == 0) && !netvsIsPractice && !netvsIsWatch()) {
			int remainLines = playerRemainLines[playerID];
			String strMsg = "game\tstats\t" + remainLines + "\n";
			netLobby.netPlayerClient.send(strMsg);
		}
	}

	/*
	 * Receive stats
	 */
	@Override
	protected void netRecvStats(GameEngine engine, String[] message) {
		int playerID = engine.playerID;
		if(message.length > 4) playerRemainLines[playerID] = Integer.parseInt(message[4]);
		updateMeter(engine);
	}

	/*
	 * Send end-of-game stats
	 */
	@Override
	protected void netSendEndGameStats(GameEngine engine) {
		netvsSendRaceEndGameStats(engine);
	}

	/*
	 * Receive end-of-game stats
	 */
	@Override
	protected void netvsRecvEndGameStats(String[] message) {
		netvsRecvRaceEndGameStats(message);
	}
}

// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mesh;

import java.io.IOException;
import java.util.LinkedList;
import java.util.zip.Adler32;

import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetSPModeRegistry;
import nullpomino.game.net.NetSPRanking;
import nullpomino.game.net.NetSPRecord;
import nullpomino.game.net.NetUtil;
import nullpomino.game.play.GameEngine;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The server-centric netplay features, made local: each peer persists its
 * OWN ratings/play counts, a local multiplayer leaderboard built from the
 * rated results it witnessed, and local single-player netplay rankings.
 * The standard {@code mpranking}/{@code spsend}/{@code spranking} requests
 * are answered from these files instead of a server.
 *
 * <p>By construction this is honor-system data: every peer self-reports its
 * rating baseline and keeps its own view of the leaderboard.
 */
public class MeshLocalRecords {
	/** Log */
	private static final Logger log = LoggerFactory.getLogger(MeshLocalRecords.class);

	/** Leaderboard size cap (NetServer default) */
	public static final int MAX_MPRANKING = 100;

	/** SP ranking size cap (NetServer default) */
	public static final int MAX_SPRANKING = 100;

	private final String myDataFile;
	private final String mpRankingFile;
	private final String spRankingFile;

	/** Local MP leaderboard: one row per (style, name) */
	private static final class MPEntry {
		final String name;
		int rating;
		int playCount;
		int winCount;
		MPEntry(String name, int rating, int playCount, int winCount) {
			this.name = name; this.rating = rating; this.playCount = playCount; this.winCount = winCount;
		}
	}

	@SuppressWarnings("unchecked")
	private final LinkedList<MPEntry>[] mpRanking = new LinkedList[GameEngine.MAX_GAMESTYLE];

	/** Local SP rankings, created on demand per (rule, mode, gameType) */
	private final LinkedList<NetSPRanking> spRankings = new LinkedList<NetSPRanking>();

	public MeshLocalRecords() {
		this("config/setting");
	}

	/** Tests inject a temp directory */
	public MeshLocalRecords(String dir) {
		myDataFile = dir + "/netplay_mydata.cfg";
		mpRankingFile = dir + "/netplay_mpranking.cfg";
		spRankingFile = dir + "/netplay_spranking.cfg";
		for(int i = 0; i < mpRanking.length; i++) mpRanking[i] = new LinkedList<MPEntry>();
		loadMPRanking();
		loadSPRankings();
	}

	// ================================================================ own player data

	/** Seed the local player's blob from the persisted ratings/counts */
	public void loadInto(NetPlayerInfo pInfo) {
		CustomProperties prop = CustomProperties.loadFromFileOrEmpty(myDataFile);
		for(int style = 0; style < GameEngine.MAX_GAMESTYLE; style++) {
			pInfo.rating[style] = prop.getProperty("mydata." + style + ".rating", MeshRating.RATING_DEFAULT);
			pInfo.playCount[style] = prop.getProperty("mydata." + style + ".playCount", 0);
			pInfo.winCount[style] = prop.getProperty("mydata." + style + ".winCount", 0);
		}
		String pb = prop.getProperty("mydata.spPersonalBest", "");
		if(pb.length() > 0) {
			try {
				pInfo.spPersonalBest.importString(NetUtil.decompressString(pb));
			} catch (RuntimeException e) {
				log.warn("Failed to load personal bests", e);
			}
		}
	}

	/** Persist the local player's ratings/counts/personal bests */
	public void saveFrom(NetPlayerInfo pInfo) {
		CustomProperties prop = new CustomProperties();
		for(int style = 0; style < GameEngine.MAX_GAMESTYLE; style++) {
			prop.setProperty("mydata." + style + ".rating", pInfo.rating[style]);
			prop.setProperty("mydata." + style + ".playCount", pInfo.playCount[style]);
			prop.setProperty("mydata." + style + ".winCount", pInfo.winCount[style]);
		}
		prop.setProperty("mydata.spPersonalBest", NetUtil.compressString(pInfo.spPersonalBest.exportString()));
		try {
			prop.storeToFile(myDataFile, "NullpoMino P2P Netplay Player Data");
		} catch (IOException e) {
			log.warn("Failed to save player data", e);
		}
	}

	// ================================================================ MP leaderboard

	/**
	 * Record one witnessed rated result (from a {@code rating} broadcast plus
	 * the winner known from the round's {@code finish}).
	 */
	public void recordRatedResult(int style, String name, int newRating, boolean won) {
		if((style < 0) || (style >= mpRanking.length)) return;

		MPEntry entry = null;
		for(MPEntry e: mpRanking[style]) {
			if(e.name.equals(name)) { entry = e; break; }
		}
		if(entry == null) {
			entry = new MPEntry(name, newRating, 0, 0);
			mpRanking[style].add(entry);
		}
		entry.rating = newRating;
		entry.playCount++;
		if(won) entry.winCount++;

		mpRanking[style].sort((a, b) -> Integer.compare(b.rating, a.rating));
		while(mpRanking[style].size() > MAX_MPRANKING) mpRanking[style].removeLast();
		saveMPRanking();
	}

	/**
	 * Build the standard {@code mpranking} reply from the local leaderboard
	 * (same wire format as NetServer's).
	 */
	public String buildMPRankingReply(int style, NetPlayerInfo self) {
		int myRank = -1;
		for(int i = 0; i < mpRanking[style].size(); i++) {
			if((self != null) && mpRanking[style].get(i).name.equals(self.strName)) { myRank = i; break; }
		}

		StringBuilder strPData = new StringBuilder();
		int prevRating = -1;
		int nowRank = 0;
		for(int i = 0; i < mpRanking[style].size(); i++) {
			MPEntry e = mpRanking[style].get(i);
			if((i == 0) || (e.rating < prevRating)) {
				prevRating = e.rating;
				nowRank = i;
			}
			strPData.append(nowRank).append(';').append(NetUtil.urlEncode(e.name)).append(';')
				.append(e.rating).append(';').append(e.playCount).append(';').append(e.winCount).append('\t');
		}
		if((myRank == -1) && (self != null)) {
			strPData.append(-1).append(';').append(NetUtil.urlEncode(self.strName)).append(';')
				.append(self.rating[style]).append(';').append(self.playCount[style]).append(';')
				.append(self.winCount[style]).append('\t');
		}

		return "mpranking\t" + style + "\t" + myRank + "\t" + NetUtil.compressString(strPData.toString());
	}

	private void loadMPRanking() {
		CustomProperties prop = CustomProperties.loadFromFileOrEmpty(mpRankingFile);
		for(int style = 0; style < mpRanking.length; style++) {
			mpRanking[style].clear();
			int count = prop.getProperty("mpranking." + style + ".count", 0);
			for(int i = 0; i < count; i++) {
				String key = "mpranking." + style + "." + i + ".";
				String name = prop.getProperty(key + "name", "");
				if(name.length() == 0) continue;
				mpRanking[style].add(new MPEntry(NetUtil.urlDecode(name),
					prop.getProperty(key + "rating", MeshRating.RATING_DEFAULT),
					prop.getProperty(key + "playCount", 0),
					prop.getProperty(key + "winCount", 0)));
			}
		}
	}

	private void saveMPRanking() {
		CustomProperties prop = new CustomProperties();
		for(int style = 0; style < mpRanking.length; style++) {
			prop.setProperty("mpranking." + style + ".count", mpRanking[style].size());
			for(int i = 0; i < mpRanking[style].size(); i++) {
				String key = "mpranking." + style + "." + i + ".";
				MPEntry e = mpRanking[style].get(i);
				prop.setProperty(key + "name", NetUtil.urlEncode(e.name));
				prop.setProperty(key + "rating", e.rating);
				prop.setProperty(key + "playCount", e.playCount);
				prop.setProperty(key + "winCount", e.winCount);
			}
		}
		try {
			prop.storeToFile(mpRankingFile, "NullpoMino P2P Netplay Local Leaderboard");
		} catch (IOException e) {
			log.warn("Failed to save local leaderboard", e);
		}
	}

	// ================================================================ SP rankings (alltime only)

	/**
	 * Register a single-player record ({@code spsend} port, minus tripcode
	 * gating and daily rankings - local files need neither).
	 * @return The reply line to broadcast to the room ({@code spsendok\t...})
	 *         or send back ({@code spsendng})
	 */
	public String registerSPRecord(NetRoomInfo roomInfo, NetPlayerInfo pInfo, String[] message) {
		//spsend\t[CHECKSUM]\t[DATA]
		long sChecksum = Long.parseLong(message[1]);
		Adler32 checksumObj = new Adler32();
		checksumObj.update(NetUtil.stringToBytes(message[2]));

		if(sChecksum != checksumObj.getValue()) {
			return "spsendng";
		}

		String strData = NetUtil.decompressString(message[2]);
		NetSPRecord record = new NetSPRecord(strData);
		String rule = (roomInfo.rated ? roomInfo.ruleName : "any");
		record.strPlayerName = pInfo.strName;
		record.strModeName = roomInfo.strMode;
		record.strRuleName = rule;
		record.style = roomInfo.style;
		record.strTimeStamp = GeneralUtil.exportCalendarString();

		NetSPRanking ranking = getOrCreateSPRanking(rule, record.strModeName, record.gameType, roomInfo.style);
		int rank = -1;
		boolean isPB = false;
		if(ranking != null) {
			rank = ranking.registerRecord(record);
			if(rank != -1) saveSPRankings();

			isPB = pInfo.spPersonalBest.registerRecord(ranking.rankingType, record);
			if(isPB) saveFrom(pInfo);
		}

		log.info("SP record Name:{} Mode:{} AllTime:{}", pInfo.strName, record.strModeName, rank);
		return "spsendok\t" + rank + "\t" + isPB + "\t-1";
	}

	/**
	 * Build the standard {@code spranking} reply from the local rankings.
	 * Daily rankings don't exist locally and always answer empty.
	 */
	public String buildSPRankingReply(String[] message, NetPlayerInfo self) {
		//spranking\t[RULE]\t[MODE]\t[GAMETYPE]\t[DAILY]
		String strRule = NetUtil.urlDecode(message[1]);
		String strMode = NetUtil.urlDecode(message[2]);
		int gameType = Integer.parseInt(message[3]);
		boolean isDaily = Boolean.parseBoolean(message[4]);

		NetSPRanking ranking = isDaily ? null : findSPRanking(strRule, strMode, gameType);

		if(ranking != null) {
			int maxRecord = ranking.listRecord.size();
			int myRank = -1;
			StringBuilder strData = new StringBuilder();

			for(int i = 0; i < maxRecord; i++) {
				if(i > 0) strData.append(';');

				NetSPRecord record = ranking.listRecord.get(i);
				strData.append(i).append(',').append(NetUtil.urlEncode(record.strPlayerName)).append(',');
				strData.append(record.strTimeStamp).append(',').append(record.stats.gamerate).append(',');
				strData.append(record.getStatRow(ranking.rankingType));

				if((self != null) && self.strName.equals(record.strPlayerName)) {
					myRank = i;
				}
			}
			if((myRank == -1) && (self != null)) {
				NetSPRecord record = self.spPersonalBest.getRecord(strRule, strMode, gameType);

				if(record != null) {
					if(maxRecord > 0) strData.append(',');
					maxRecord++;
					strData.append(-1).append(',').append(NetUtil.urlEncode(record.strPlayerName)).append(',');
					strData.append(record.strTimeStamp).append(',').append(record.stats.gamerate).append(',');
					strData.append(record.getStatRow(ranking.rankingType));
				}
			}

			return "spranking\t" + strRule + "\t" + strMode + "\t" + gameType + "\t" + isDaily + "\t"
				+ ranking.rankingType + "\t" + maxRecord + "\t" + strData;
		}
		return "spranking\t" + strRule + "\t" + strMode + "\t" + gameType + "\t" + isDaily + "\t0\t0";
	}

	private NetSPRanking findSPRanking(String rule, String mode, int gameType) {
		for(NetSPRanking r: spRankings) {
			if(r.strRuleName.equals(rule) && r.strModeName.equals(mode) && (r.gameType == gameType)) return r;
		}
		return null;
	}

	private NetSPRanking getOrCreateSPRanking(String rule, String mode, int gameType, int style) {
		NetSPRanking existing = findSPRanking(rule, mode, gameType);
		if(existing != null) return existing;

		for(NetSPModeRegistry.Entry entry: NetSPModeRegistry.forStyle(style)) {
			if(entry.name().equals(mode)) {
				NetSPRanking ranking = new NetSPRanking(mode, rule, gameType, style,
					entry.rankingType(), MAX_SPRANKING);
				spRankings.add(ranking);
				return ranking;
			}
		}
		log.warn("Unknown SP mode, record not ranked: {}", mode);
		return null;
	}

	private void loadSPRankings() {
		CustomProperties prop = CustomProperties.loadFromFileOrEmpty(spRankingFile);
		spRankings.clear();
		int count = prop.getProperty("spindex.count", 0);
		for(int i = 0; i < count; i++) {
			String key = "spindex." + i + ".";
			String rule = prop.getProperty(key + "rule", "");
			String mode = prop.getProperty(key + "mode", "");
			if(mode.length() == 0) continue;
			NetSPRanking ranking = new NetSPRanking(mode, rule,
				prop.getProperty(key + "gameType", 0), prop.getProperty(key + "style", 0),
				prop.getProperty(key + "rankingType", 0), MAX_SPRANKING);
			ranking.readProperty(prop);
			spRankings.add(ranking);
		}
	}

	private void saveSPRankings() {
		CustomProperties prop = new CustomProperties();
		prop.setProperty("spindex.count", spRankings.size());
		for(int i = 0; i < spRankings.size(); i++) {
			NetSPRanking ranking = spRankings.get(i);
			String key = "spindex." + i + ".";
			prop.setProperty(key + "rule", ranking.strRuleName);
			prop.setProperty(key + "mode", ranking.strModeName);
			prop.setProperty(key + "gameType", ranking.gameType);
			prop.setProperty(key + "style", ranking.style);
			prop.setProperty(key + "rankingType", ranking.rankingType);
			ranking.writeProperty(prop);
		}
		try {
			prop.storeToFile(spRankingFile, "NullpoMino P2P Netplay Local SP Rankings");
		} catch (IOException e) {
			log.warn("Failed to save local SP rankings", e);
		}
	}
}

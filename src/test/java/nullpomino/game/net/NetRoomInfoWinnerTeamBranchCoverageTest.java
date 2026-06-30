package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Covers the last branches in NetRoomInfo.getWinnerTeam / the private
 * isConnectedActiveSeatedPlayer helper:
 * <ul>
 *   <li>L567 {@code for (pInfo : playerSeatNowPlaying)} reaching its natural
 *       end (the loop-exit outcome), achieved by having 2+ active seated
 *       players (so the outer {@code >= 2} guard passes and the loop is entered)
 *       none of whom is connected, so no early return fires; and</li>
 *   <li>L652 {@code isActiveSeatedPlayer(pInfo) && pInfo.connected}: the
 *       {@code connected == false} outcome while the player is otherwise an
 *       active seated player.</li>
 * </ul>
 */
class NetRoomInfoWinnerTeamBranchCoverageTest {

	private static NetPlayerInfo seatedPlaying(String team, boolean connected) {
		NetPlayerInfo p = new NetPlayerInfo();
		p.strTeam = team;
		p.playing = true;
		p.connected = connected;
		return p;
	}

	@Test
	void getWinnerTeamLoopExhaustsWhenAllActivePlayersDisconnected() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;

		// Both players are active+seated+playing (so getHowManyPlayersPlaying()
		// counts them and the >= 2 guard passes and the loop is entered) but
		// neither is connected, so isConnectedActiveSeatedPlayer is always false,
		// no inner return fires and the for-each runs to completion.
		NetPlayerInfo p1 = seatedPlaying("Red", false);
		NetPlayerInfo p2 = seatedPlaying("Red", false);
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);

		assertNull(room.getWinnerTeam());
	}
}

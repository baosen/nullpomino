/*
    Copyright (c) 2010, NullNoname
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright
          notice, this list of conditions and the following disclaimer in the
          documentation and/or other materials provided with the distribution.
        * Neither the name of NullNoname nor the names of its
          contributors may be used to endorse or promote products derived from
          this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
*/
package mu.nu.nullpo.gui.net;

import java.util.LinkedList;

import mu.nu.nullpo.game.component.RuleOptions;
import mu.nu.nullpo.game.net.NetPlayerClient;
import mu.nu.nullpo.game.net.NetPlayerInfo;
import mu.nu.nullpo.game.subsystem.mode.NetDummyMode;

/**
 * Interface for the netplay lobby, abstracting away the GUI toolkit.
 * Implementations include NetLobbyFrame (Swing) and NetLobbySDL (SDL).
 */
public interface NetLobby {
	/** Get the network player client */
	public NetPlayerClient getNetPlayerClient();

	/** Get the player's rule options */
	public RuleOptions getRuleOptPlayer();

	/** Set the player's rule options */
	public void setRuleOptPlayer(RuleOptions ruleopt);

	/** Get the locked rule options (room-enforced rules) */
	public RuleOptions getRuleOptLock();

	/** Get the map list */
	public LinkedList<String> getMapList();

	/** Update and return the list of players in the same room */
	public LinkedList<NetPlayerInfo> updateSameRoomPlayerInfoList();

	/** Get the cached list of players in the same room */
	public LinkedList<NetPlayerInfo> getSameRoomPlayerInfoList();

	/** Set the current netplay game mode */
	public void setNetDummyMode(NetDummyMode m);

	/** Get the current netplay game mode */
	public NetDummyMode getNetDummyMode();

	/** Initialize the lobby */
	public void init();

	/** Shut down the lobby */
	public void shutdown();

	/** Show or hide the lobby */
	public void setVisible(boolean visible);

	/** Add a lobby event listener */
	public void addListener(NetLobbyListener l);

	/** Remove a lobby event listener */
	public boolean removeListener(NetLobbyListener l);
}

// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.io.IOException;

/**
 * Interface for the class to do something in response to processing the received message
 */
public interface NetMessageListener {
	/**
	 * I called when receiving a message
	 * @param client Client(NetBaseClientAnd its derived classes)
	 * @param message Received Messages(Pre-tab-delimited)
	 * @throws IOException If there are any errors
	 */
	public void netOnMessage(NetBaseClient client, String[] message) throws IOException;

	/**
	 * I called at the time of disconnection
	 * @param client Client(NetBaseClientAnd its derived classes)
	 * @param ex Exception that caused the disconnection(If successful and if you do not knownull)
	 */
	public void netOnDisconnect(NetBaseClient client, Throwable ex);
}

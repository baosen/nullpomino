// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net.rtc;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * Typed view of a browser RTCPeerConnection. Create via
 * {@link RtcBindings#newPeerConnection}; all Promise-based operations
 * and event wiring go through {@link RtcBindings}.
 */
public interface RtcPeerConnection extends JSObject {
	/** Ordered+reliable is the RTCDataChannel default - no options object needed */
	RtcDataChannel createDataChannel(String label);

	/** new | connecting | connected | disconnected | failed | closed */
	@JSProperty("connectionState")
	String getConnectionState();

	/** new | gathering | complete */
	@JSProperty("iceGatheringState")
	String getIceGatheringState();

	void close();
}

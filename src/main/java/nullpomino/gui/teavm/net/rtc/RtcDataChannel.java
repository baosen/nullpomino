// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net.rtc;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * Typed view of a browser RTCDataChannel (event wiring via
 * {@link RtcBindings}).
 */
public interface RtcDataChannel extends JSObject {
	@JSProperty("label")
	String getLabel();

	/** connecting | open | closing | closed */
	@JSProperty("readyState")
	String getReadyState();

	/** Bytes queued locally but not yet handed to the transport */
	@JSProperty("bufferedAmount")
	double getBufferedAmount();

	void send(String data);

	void close();
}

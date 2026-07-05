// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net.rtc;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * Hand-written WebRTC bindings (teavm-jso-apis 0.14.1 has none). All
 * Promise handling stays inside the {@code @JSBody} scripts and reports
 * back through {@code @JSFunctor} callbacks on the JS event loop - the
 * {@code JsAsync.loadFontJs} idiom. Signaling is non-trickle: each side
 * waits for ICE gathering to complete (capped by a timeout) and ships
 * one full SDP, so the exchange is a single message each way.
 */
public final class RtcBindings {
	private RtcBindings() {}

	/** SDP result; null signals failure */
	@JSFunctor
	public interface SdpCallback extends JSObject {
		void done(String sdp);
	}

	@JSFunctor
	public interface OkCallback extends JSObject {
		void done(boolean ok);
	}

	@JSFunctor
	public interface StringCallback extends JSObject {
		void done(String value);
	}

	@JSFunctor
	public interface ChannelCallback extends JSObject {
		void done(RtcDataChannel channel);
	}

	@JSFunctor
	public interface VoidCallback extends JSObject {
		void done();
	}

	/** New RTCPeerConnection from a space-separated STUN/TURN url list */
	@JSBody(params = {"iceUrls"}, script =
		"var servers = [];"
		+ "iceUrls.split(' ').forEach(function(u) { if (u) servers.push({ urls: u }); });"
		+ "return new RTCPeerConnection({ iceServers: servers });")
	public static native RtcPeerConnection newPeerConnection(String iceUrls);

	/**
	 * Offerer: createOffer, setLocalDescription, wait for ICE gathering to
	 * complete (capped at gatherTimeoutMs - ship whatever gathered), then
	 * deliver localDescription.sdp.
	 */
	@JSBody(params = {"pc", "gatherTimeoutMs", "cb"}, script =
		"function finish() { cb(pc.localDescription ? pc.localDescription.sdp : null); }"
		+ "pc.createOffer()"
		+ "  .then(function(d) { return pc.setLocalDescription(d); })"
		+ "  .then(function() {"
		+ "    if (pc.iceGatheringState === 'complete') { finish(); return; }"
		+ "    var t = setTimeout(function() { pc.onicegatheringstatechange = null; finish(); }, gatherTimeoutMs);"
		+ "    pc.onicegatheringstatechange = function() {"
		+ "      if (pc.iceGatheringState === 'complete') {"
		+ "        clearTimeout(t); pc.onicegatheringstatechange = null; finish();"
		+ "      }"
		+ "    };"
		+ "  }, function(e) { console.warn('createOffer failed', e); cb(null); });")
	public static native void createOfferGathered(RtcPeerConnection pc, int gatherTimeoutMs, SdpCallback cb);

	/**
	 * Answerer: setRemoteDescription(offer), createAnswer,
	 * setLocalDescription, wait for gathering, deliver the answer sdp.
	 */
	@JSBody(params = {"pc", "offerSdp", "gatherTimeoutMs", "cb"}, script =
		"function finish() { cb(pc.localDescription ? pc.localDescription.sdp : null); }"
		+ "pc.setRemoteDescription({ type: 'offer', sdp: offerSdp })"
		+ "  .then(function() { return pc.createAnswer(); })"
		+ "  .then(function(d) { return pc.setLocalDescription(d); })"
		+ "  .then(function() {"
		+ "    if (pc.iceGatheringState === 'complete') { finish(); return; }"
		+ "    var t = setTimeout(function() { pc.onicegatheringstatechange = null; finish(); }, gatherTimeoutMs);"
		+ "    pc.onicegatheringstatechange = function() {"
		+ "      if (pc.iceGatheringState === 'complete') {"
		+ "        clearTimeout(t); pc.onicegatheringstatechange = null; finish();"
		+ "      }"
		+ "    };"
		+ "  }, function(e) { console.warn('createAnswer failed', e); cb(null); });")
	public static native void createAnswerGathered(RtcPeerConnection pc, String offerSdp,
		int gatherTimeoutMs, SdpCallback cb);

	/** Offerer: apply the answer that came back over signaling */
	@JSBody(params = {"pc", "answerSdp", "cb"}, script =
		"pc.setRemoteDescription({ type: 'answer', sdp: answerSdp })"
		+ "  .then(function() { cb(true); }, function(e) { console.warn('setRemoteDescription failed', e); cb(false); });")
	public static native void setRemoteAnswer(RtcPeerConnection pc, String answerSdp, OkCallback cb);

	/** Answerer receives the offerer-created channel here */
	@JSBody(params = {"pc", "cb"}, script = "pc.ondatachannel = function(e) { cb(e.channel); };")
	public static native void onDataChannel(RtcPeerConnection pc, ChannelCallback cb);

	@JSBody(params = {"pc", "cb"}, script =
		"pc.onconnectionstatechange = function() { cb(pc.connectionState); };")
	public static native void onConnectionStateChange(RtcPeerConnection pc, StringCallback cb);

	@JSBody(params = {"ch", "cb"}, script = "ch.onopen = function() { cb(); };")
	public static native void onOpen(RtcDataChannel ch, VoidCallback cb);

	@JSBody(params = {"ch", "cb"}, script = "ch.onclose = function() { cb(); };")
	public static native void onClose(RtcDataChannel ch, VoidCallback cb);

	/** The room protocol is UTF-8 text lines; binary frames are ignored */
	@JSBody(params = {"ch", "cb"}, script =
		"ch.onmessage = function(e) { if (typeof e.data === 'string') cb(e.data); };")
	public static native void onMessage(RtcDataChannel ch, StringCallback cb);
}

// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net;

import java.util.LinkedHashSet;
import java.util.Set;

import nullpomino.game.net.mqtt.MqttCodec;

import org.teavm.jso.browser.Window;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.websocket.WebSocket;

/**
 * One MQTT-over-WebSocket broker connection for the browser build:
 * CONNACK-gated connect with timeout, keepalive pings, subscription
 * replay after reconnect, backoff retry, and a reassembly buffer
 * (WebSocket frames do not align with MQTT packets). All callbacks run
 * on the JS event loop.
 */
public class MqttWebSocketClient {
	/** Receives connection state + messages; called on the JS event loop */
	public interface Listener {
		void onConnected(String url);
		void onMessage(String topic, byte[] payload);
		void onDown(String url, String reason);
	}

	/** Reconnect backoff after a failed attempt (ms) */
	private static final int RETRY_DELAY_MS = 10000;

	private final String url;
	private final String clientId;
	private final int keepAliveSec;
	private final int connectTimeoutMs;
	private final Listener listener;

	private WebSocket socket;
	private boolean connacked = false;
	private boolean shutdownRequested = false;

	/** Reassembly buffer for partial/coalesced MQTT packets */
	private byte[] rxBuf = new byte[512];
	private int rxLen = 0;

	/** Topic filters to (re)subscribe after each CONNACK */
	private final Set<String> subscriptions = new LinkedHashSet<String>();
	private int nextPacketId = 1;

	private int connectTimeoutId = -1;
	private int keepAliveIntervalId = -1;
	private int retryTimeoutId = -1;
	private double lastInboundAt = 0;

	public MqttWebSocketClient(String url, String clientId, int keepAliveSec,
		int connectTimeoutMs, Listener listener)
	{
		this.url = url;
		this.clientId = clientId;
		this.keepAliveSec = keepAliveSec;
		this.connectTimeoutMs = connectTimeoutMs;
		this.listener = listener;
	}

	/** Start connecting; retries with backoff until shutdown */
	public void connect() {
		if(shutdownRequested || (socket != null)) return;
		connacked = false;
		rxLen = 0;

		WebSocket ws;
		try {
			// The "mqtt" subprotocol is mandatory - mosquitto closes without it
			ws = new WebSocket(url, "mqtt");
		} catch (Throwable e) {
			scheduleRetry("open failed: " + e);
			return;
		}
		socket = ws;
		ws.setBinaryType("arraybuffer");

		connectTimeoutId = Window.setTimeout(() -> {
			// Gate on CONNACK, not the socket: a sick broker may accept the
			// TCP/WS handshake and never answer the MQTT CONNECT
			if(!connacked) dropAndRetry("connect timeout");
		}, connectTimeoutMs);

		ws.onOpen(e -> send(MqttCodec.encodeConnect(clientId, keepAliveSec)));
		ws.onError(e -> dropAndRetry("socket error"));
		ws.onClose(e -> dropAndRetry("socket closed"));
		ws.onMessage(e -> onFrame(e.getDataAsArray()));
	}

	public boolean isUp() {
		return connacked;
	}

	/** Subscribe a topic filter (QoS 0); remembered and replayed on reconnect */
	public void subscribe(String topicFilter) {
		if(!subscriptions.add(topicFilter)) return;
		if(connacked) send(MqttCodec.encodeSubscribe(nextPacketId++, topicFilter));
	}

	/** Publish (QoS 0); silently dropped while the broker is down */
	public void publish(String topic, byte[] payload) {
		if(connacked) send(MqttCodec.encodePublish(topic, payload));
	}

	/** Best-effort DISCONNECT, then stop retrying */
	public void shutdown() {
		shutdownRequested = true;
		if(connacked) send(MqttCodec.encodeDisconnect());
		cancelTimers();
		if(retryTimeoutId != -1) {
			Window.clearTimeout(retryTimeoutId);
			retryTimeoutId = -1;
		}
		closeSocket();
	}

	// ---------------------------------------------------------------- inbound

	private void onFrame(ArrayBuffer data) {
		byte[] chunk = new Int8Array(data).copyToJavaArray();
		ensureCapacity(rxLen + chunk.length);
		System.arraycopy(chunk, 0, rxBuf, rxLen, chunk.length);
		rxLen += chunk.length;
		lastInboundAt = nowMs();

		int off = 0;
		while(true) {
			MqttCodec.Packet packet;
			try {
				packet = MqttCodec.decode(rxBuf, off, rxLen - off);
			} catch (IllegalArgumentException e) {
				dropAndRetry("protocol error: " + e.getMessage());
				return;
			}
			if(packet == null) break;
			off += packet.consumed;
			handlePacket(packet);
			if(socket == null) return;   // handler dropped the connection
		}
		if(off > 0) {
			System.arraycopy(rxBuf, off, rxBuf, 0, rxLen - off);
			rxLen -= off;
		}
	}

	private void handlePacket(MqttCodec.Packet packet) {
		if(packet.type == MqttCodec.CONNACK) {
			if(packet.connackCode != 0) {
				dropAndRetry("connection refused: " + packet.connackCode);
				return;
			}
			connacked = true;
			if(connectTimeoutId != -1) {
				Window.clearTimeout(connectTimeoutId);
				connectTimeoutId = -1;
			}
			for(String filter: subscriptions) {
				send(MqttCodec.encodeSubscribe(nextPacketId++, filter));
			}
			startKeepAlive();
			listener.onConnected(url);
		} else if(packet.type == MqttCodec.PUBLISH) {
			listener.onMessage(packet.topic, packet.payload);
		}
		// SUBACK/PINGRESP only refresh lastInboundAt
	}

	// ---------------------------------------------------------------- liveness

	private void startKeepAlive() {
		int periodMs = keepAliveSec * 500;   // ping at half the keepalive
		keepAliveIntervalId = Window.setInterval(() -> {
			if(!connacked) return;
			if(nowMs() - lastInboundAt > keepAliveSec * 1500.0) {
				// 1.5x keepalive of silence: the broker is gone
				dropAndRetry("keepalive timeout");
				return;
			}
			send(MqttCodec.encodePingReq());
		}, periodMs);
	}

	// ---------------------------------------------------------------- teardown / retry

	private void dropAndRetry(String reason) {
		if(socket == null) return;   // already handled (onError + onClose both fire)
		boolean wasUp = connacked;
		cancelTimers();
		closeSocket();
		if(wasUp || !shutdownRequested) listener.onDown(url, reason);
		scheduleRetry(reason);
	}

	private void scheduleRetry(String reason) {
		if(shutdownRequested || (retryTimeoutId != -1)) return;
		retryTimeoutId = Window.setTimeout(() -> {
			retryTimeoutId = -1;
			connect();
		}, RETRY_DELAY_MS);
	}

	private void cancelTimers() {
		if(connectTimeoutId != -1) {
			Window.clearTimeout(connectTimeoutId);
			connectTimeoutId = -1;
		}
		if(keepAliveIntervalId != -1) {
			Window.clearInterval(keepAliveIntervalId);
			keepAliveIntervalId = -1;
		}
	}

	private void closeSocket() {
		WebSocket ws = socket;
		socket = null;
		connacked = false;
		if(ws != null) {
			try {
				ws.close();
			} catch (Throwable e) {
				// already closed
			}
		}
	}

	// ---------------------------------------------------------------- helpers

	private void send(byte[] bytes) {
		WebSocket ws = socket;
		if(ws == null) return;
		try {
			ws.send(Int8Array.copyFromJavaArray(bytes));
		} catch (Throwable e) {
			dropAndRetry("send failed");
		}
	}

	private void ensureCapacity(int needed) {
		if(rxBuf.length >= needed) return;
		int size = rxBuf.length;
		while(size < needed) size *= 2;
		byte[] bigger = new byte[size];
		System.arraycopy(rxBuf, 0, bigger, 0, rxLen);
		rxBuf = bigger;
	}

	private static double nowMs() {
		return System.currentTimeMillis();
	}
}

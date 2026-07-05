// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nullpomino.game.net.web.WebSignaling;

/**
 * Bonded connection to every configured MQTT broker at once: publishes
 * go to all connected brokers, subscriptions apply to all, and inbound
 * messages fan in to the handlers. Bonding makes "are both peers on the
 * same broker" a non-question and a single broker dying a non-event;
 * consumers dedupe per message kind (chat by msgId, announces/presence
 * by key overwrite, signaling by callId).
 */
public class MqttBondedChannel {
	/** Receives every inbound message from every broker; called on the JS event loop */
	public interface MessageHandler {
		void onMessage(String topic, byte[] payload);
	}

	private final List<MqttWebSocketClient> clients = new ArrayList<MqttWebSocketClient>();
	private final List<MessageHandler> handlers = new ArrayList<MessageHandler>();
	private boolean connected = false;

	public MqttBondedChannel(WebNetConfig config, Random rand) {
		MqttWebSocketClient.Listener listener = new MqttWebSocketClient.Listener() {
			@Override
			public void onConnected(String url) {
				System.out.println("MQTT broker up: " + url);
			}

			@Override
			public void onMessage(String topic, byte[] payload) {
				// Copy: a handler may add/remove handlers while iterating
				for(MessageHandler handler: new ArrayList<MessageHandler>(handlers)) {
					handler.onMessage(topic, payload);
				}
			}

			@Override
			public void onDown(String url, String reason) {
				System.out.println("MQTT broker down: " + url + " (" + reason + ")");
			}
		};
		for(String url: config.brokers) {
			// A fresh random client id per broker: duplicate ids on a shared
			// public broker cause mutual kick loops
			String clientId = "npp" + WebSignaling.generateId(rand);
			clients.add(new MqttWebSocketClient(url, clientId,
				config.keepAliveSec, config.connectTimeout, listener));
		}
	}

	/** Start connecting to every broker. Idempotent. */
	public void connect() {
		if(connected) return;
		connected = true;
		for(MqttWebSocketClient client: clients) {
			client.connect();
		}
	}

	/** @return true while at least one broker is up */
	public boolean isUp() {
		for(MqttWebSocketClient client: clients) {
			if(client.isUp()) return true;
		}
		return false;
	}

	/** Subscribe on every broker; remembered across reconnects */
	public void subscribe(String topicFilter) {
		for(MqttWebSocketClient client: clients) {
			client.subscribe(topicFilter);
		}
	}

	/** Publish to every connected broker (consumers dedupe) */
	public void publish(String topic, byte[] payload) {
		for(MqttWebSocketClient client: clients) {
			client.publish(topic, payload);
		}
	}

	public void addHandler(MessageHandler handler) {
		if(!handlers.contains(handler)) handlers.add(handler);
	}

	public void removeHandler(MessageHandler handler) {
		handlers.remove(handler);
	}
}

package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

class NetChatMessageRoundTripTest {

	@Test
	void exportImportPreservesEncodedFieldsAndTimestamp() {
		NetChatMessage original = messageWithFixedTimestamp();
		original.uid = 17;
		original.strUserName = "Nullpo Player";
		original.strHost = "host.example";
		original.roomID = 3;
		original.strRoomName = "Room; A";
		original.strMessage = "hello; a+b 日本語";

		NetChatMessage imported = new NetChatMessage();
		imported.importString(original.exportString());

		assertEquals(original.uid, imported.uid);
		assertEquals(original.strUserName, imported.strUserName);
		assertEquals(original.strHost, imported.strHost);
		assertEquals(original.roomID, imported.roomID);
		assertEquals(original.strRoomName, imported.strRoomName);
		assertEquals(original.timestamp.getTimeInMillis(), imported.timestamp.getTimeInMillis());
		assertEquals(original.strMessage, imported.strMessage);
	}

	@Test
	void importStringPreservesEmptyFinalMessageField() {
		NetChatMessage original = messageWithFixedTimestamp();
		original.strMessage = "";
		String exported = original.exportString();

		NetChatMessage imported = new NetChatMessage();
		imported.importString(exported);

		assertEquals("", imported.strMessage);
	}

	@Test
	void constructorsLayerMessagePlayerAndRoomFields() {
		NetPlayerInfo player = new NetPlayerInfo();
		player.uid = 7;
		player.strName = "Player";
		player.strRealHost = "real.host";
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 3;
		room.strName = "Room";

		NetChatMessage message = new NetChatMessage("hello", player, room);

		assertEquals("hello", message.strMessage);
		assertEquals(player.uid, message.uid);
		assertEquals(player.strName, message.strUserName);
		assertEquals(player.strRealHost, message.strHost);
		assertEquals(room.roomID, message.roomID);
		assertEquals(room.strName, message.strRoomName);
	}

	private static NetChatMessage messageWithFixedTimestamp() {
		NetChatMessage message = new NetChatMessage();
		message.timestamp = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		message.timestamp.set(2026, Calendar.APRIL, 26, 10, 11, 12);
		message.timestamp.set(Calendar.MILLISECOND, 0);
		return message;
	}
}

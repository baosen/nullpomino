package nullpomino.game.net;

import java.io.Serializable;
import java.util.Calendar;

import nullpomino.util.GeneralUtil;

import org.apache.log4j.Logger;

/**
 * Chat message
 */
public class NetChatMessage implements Serializable {
	/** Serial version */
	private static final long serialVersionUID = 1L;

	private static final int EXPORT_FIELD_COUNT = 7;

	/** Log */
	static final Logger log = Logger.getLogger(NetChatMessage.class);

	/** User ID */
	public int uid;

	/** Username */
	public String strUserName;

	/** Hostname */
	public String strHost;

	/** Room ID (-1:Lobby) */
	public int roomID;

	/** Room Name */
	public String strRoomName;

	/** Timestamp Calendar */
	public Calendar timestamp;

	/** Message Body */
	public String strMessage;

	/**
	 * Default Constructor
	 */
	public NetChatMessage() {
		reset();
	}

	/**
	 * Constructor
	 * @param msg Message
	 */
	public NetChatMessage(String msg) {
		this();
		strMessage = msg;
	}

	/**
	 * Constructor
	 * @param msg Message
	 * @param pInfo Player Info
	 */
	public NetChatMessage(String msg, NetPlayerInfo pInfo) {
		this(msg);
		uid = pInfo.uid;
		strUserName = pInfo.strName;
		strHost = pInfo.strRealHost;
	}

	/**
	 * Constructor
	 * @param msg Message
	 * @param pInfo Player Info
	 * @param roomInfo Room Info
	 */
	public NetChatMessage(String msg, NetPlayerInfo pInfo, NetRoomInfo roomInfo) {
		this(msg, pInfo);
		roomID = roomInfo.roomID;
		strRoomName = roomInfo.strName;
	}

	/**
	 * Reset to default values
	 */
	public void reset() {
		uid = -1;
		strUserName = "";
		strHost = "";
		roomID = -1;
		strRoomName = "";
		timestamp = Calendar.getInstance();
		strMessage = "";
	}

	/**
	 * Output to logger
	 */
	public void outputLog() {
		if(roomID == -1) {
			log.info("LobbyChat UID:" + uid + " Name:" + strUserName + " Msg:" + strMessage);
		} else {
			log.info("RoomChat Room:" + strRoomName + " UID:" + uid + " Name:" + strUserName + " Msg:" + strMessage);
		}
	}

	/**
	 * Import from String array
	 * @param s String array (String[7])
	 */
	public void importStringArray(String[] s) {
		NetStringArray.Reader reader = new NetStringArray.Reader(s);
		uid = reader.readInt();
		strUserName = reader.readEncoded();
		strHost = reader.readEncoded();
		roomID = reader.readInt();
		strRoomName = reader.readEncoded();
		timestamp = GeneralUtil.importCalendarString(reader.read());
		strMessage = reader.readEncoded();
	}

	/**
	 * Import from String (Divided by ;)
	 * @param str String
	 */
	public void importString(String str) {
		importStringArray(str.split(";", -1));
	}

	/**
	 * Export to String array
	 * @return String array (String[7])
	 */
	public String[] exportStringArray() {
		NetStringArray.Writer writer = new NetStringArray.Writer(EXPORT_FIELD_COUNT);
		writer.write(uid);
		writer.writeEncoded(strUserName);
		writer.writeEncoded(strHost);
		writer.write(roomID);
		writer.writeEncoded(strRoomName);
		writer.write(GeneralUtil.exportCalendarString(timestamp));
		writer.writeEncoded(strMessage);
		return writer.values();
	}

	/**
	 * Export to String (Divided by ;)
	 * @return String
	 */
	public String exportString() {
		return String.join(";", exportStringArray());
	}

	/**
	 * Delete this NetChatMessage
	 */
	public void delete() {
		uid = -1;
		strUserName = null;
		strHost = null;
		roomID = -1;
		strRoomName = null;
		timestamp = null;
		strMessage = null;
	}
}

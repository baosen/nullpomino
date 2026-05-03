package nullpomino.tool.netadmin;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import nullpomino.game.net.NetRoomInfo;
import nullpomino.util.CustomProperties;

/**
 * Pins NetAdmin's private constants, static helper methods, and
 * instance methods reachable without constructing the JFrame (which
 * requires a display).
 */
class NetAdminTest {

	// ------------------------------------------------------------------
	// Private static constant fields
	// ------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static <T> T getStaticField(String name) throws Exception {
		Field f = NetAdmin.class.getDeclaredField(name);
		f.setAccessible(true);
		return (T) f.get(null);
	}

	@Test
	void screenCardNamesMatchExpectedValues() throws Exception {
		String[] names = getStaticField("SCREENCARD_NAMES");
		assertEquals("Login", names[0]);
		assertEquals("Lobby", names[1]);
		assertEquals(2, names.length);
	}

	@Test
	void userTableColumnNamesAreAllPresent() throws Exception {
		String[] cols = getStaticField("USERTABLE_COLUMNNAMES");
		assertNotNull(cols);
		assertEquals(4, cols.length);
		assertEquals("UserTable_IP", cols[0]);
		assertEquals("UserTable_Hostname", cols[1]);
		assertEquals("UserTable_Type", cols[2]);
		assertEquals("UserTable_Name", cols[3]);
	}

	@Test
	void userTableTypesContainExpectedEntries() throws Exception {
		String[] types = getStaticField("USERTABLE_USERTYPES");
		assertNotNull(types);
		assertEquals(4, types.length);
		assertEquals("UserTable_Type_Guest", types[0]);
		assertEquals("UserTable_Type_Player", types[1]);
		assertEquals("UserTable_Type_Observer", types[2]);
		assertEquals("UserTable_Type_Admin", types[3]);
	}

	@Test
	void mpRankingColumnNamesAreAllPresent() throws Exception {
		String[] cols = getStaticField("MPRANKING_COLUMNNAMES");
		assertNotNull(cols);
		assertEquals(5, cols.length);
		assertEquals("MPRanking_Rank", cols[0]);
		assertEquals("MPRanking_Name", cols[1]);
		assertEquals("MPRanking_Rating", cols[2]);
		assertEquals("MPRanking_PlayCount", cols[3]);
		assertEquals("MPRanking_WinCount", cols[4]);
	}

	@Test
	void roomTableColumnNamesAreAllPresent() throws Exception {
		String[] cols = getStaticField("ROOMTABLE_COLUMNNAMES");
		assertNotNull(cols);
		assertEquals(7, cols.length);
		assertEquals("RoomTable_ID", cols[0]);
		assertEquals("RoomTable_Name", cols[1]);
		assertEquals("RoomTable_Rated", cols[2]);
		assertEquals("RoomTable_RuleName", cols[3]);
		assertEquals("RoomTable_Status", cols[4]);
		assertEquals("RoomTable_Players", cols[5]);
		assertEquals("RoomTable_Spectators", cols[6]);
	}

	@Test
	void screenCardConstantsMatchArrayIndices() throws Exception {
		int login = getStaticField("SCREENCARD_LOGIN");
		int lobby = getStaticField("SCREENCARD_LOBBY");
		assertEquals(0, login);
		assertEquals(1, lobby);
	}

	// ------------------------------------------------------------------
	// Private static getUIText method
	// ------------------------------------------------------------------

	@Test
	void getUITextReturnsKeyWhenBothPropsEmpty() throws Exception {
		// Save originals
		CustomProperties origLang = getStaticField("propLang");
		CustomProperties origDefault = getStaticField("propLangDefault");

		try {
			setStaticField("propLang", new CustomProperties());
			setStaticField("propLangDefault", new CustomProperties());

			String result = invokeGetUIText("Test_Key");
			assertEquals("Test_Key", result);
		} finally {
			setStaticField("propLang", origLang);
			setStaticField("propLangDefault", origDefault);
		}
	}

	@Test
	void getUITextPrefersPropLangOverDefault() throws Exception {
		CustomProperties origLang = getStaticField("propLang");
		CustomProperties origDefault = getStaticField("propLangDefault");

		try {
			CustomProperties lang = new CustomProperties();
			lang.setProperty("Title_ServerAdmin", "Localized");
			setStaticField("propLang", lang);
			CustomProperties def = new CustomProperties();
			def.setProperty("Title_ServerAdmin", "Default");
			setStaticField("propLangDefault", def);

			assertEquals("Localized", invokeGetUIText("Title_ServerAdmin"));
		} finally {
			setStaticField("propLang", origLang);
			setStaticField("propLangDefault", origDefault);
		}
	}

	@Test
	void getUITextFallsBackToDefault() throws Exception {
		CustomProperties origLang = getStaticField("propLang");
		CustomProperties origDefault = getStaticField("propLangDefault");

		try {
			setStaticField("propLang", new CustomProperties());
			CustomProperties def = new CustomProperties();
			def.setProperty("Title_ServerAdmin", "DefaultTitle");
			setStaticField("propLangDefault", def);

			assertEquals("DefaultTitle", invokeGetUIText("Title_ServerAdmin"));
		} finally {
			setStaticField("propLang", origLang);
			setStaticField("propLangDefault", origDefault);
		}
	}

	// ------------------------------------------------------------------
	// Private instance method: createRoomListRowData
	// ------------------------------------------------------------------

	@Test
	void createRoomListRowDataPopulatesSevenColumns() throws Exception {
		// Need a NetAdmin instance without running the JFrame constructor.
		NetAdmin admin = allocateNetAdmin();

		// Set up propLang and propLangDefault on the instance for getUIText calls
		CustomProperties lang = new CustomProperties();
		lang.setProperty("RoomTable_Rated_True", "RATED");
		lang.setProperty("RoomTable_Rated_False", "FREE");
		lang.setProperty("RoomTable_RuleName_Any", "*ANY*");
		lang.setProperty("RoomTable_Status_Playing", "PLAY");
		lang.setProperty("RoomTable_Status_Waiting", "WAIT");
		setField(admin, "propLang", lang);
		setField(admin, "propLangDefault", new CustomProperties());

		NetRoomInfo r = new NetRoomInfo();
		r.roomID = 7;
		r.strName = "My Room";
		r.rated = true;
		r.ruleLock = true;
		r.ruleName = "standard";
		r.playing = false;
		r.playerSeatedCount = 3;
		r.maxPlayers = 6;
		r.spectatorCount = 2;

		String[] row = invokeCreateRoomListRowData(admin, r);

		assertNotNull(row);
		assertEquals(7, row.length);
		assertEquals("7", row[0]);
		assertEquals("My Room", row[1]);
		assertEquals("RATED", row[2]);
		assertEquals("STANDARD", row[3]);
		assertEquals("WAIT", row[4]);
		assertEquals("3/6", row[5]);
		assertEquals("2", row[6]);
	}

	@Test
	void createRoomListRowDataShowsAnyForUnlockedRule() throws Exception {
		NetAdmin admin = allocateNetAdmin();

		CustomProperties lang = new CustomProperties();
		lang.setProperty("RoomTable_Rated_False", "FREE");
		lang.setProperty("RoomTable_RuleName_Any", "*ANY*");
		lang.setProperty("RoomTable_Status_Playing", "PLAY");
		lang.setProperty("RoomTable_Status_Waiting", "WAIT");
		setField(admin, "propLang", lang);
		setField(admin, "propLangDefault", new CustomProperties());

		NetRoomInfo r = new NetRoomInfo();
		r.ruleLock = false;
		r.playing = true;

		String[] row = invokeCreateRoomListRowData(admin, r);
		assertEquals("*ANY*", row[3]);
		assertEquals("FREE", row[2]);
		assertEquals("PLAY", row[4]);
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	@SuppressWarnings("deprecation")
	private static void setStaticField(String name, Object value) throws Exception {
		Field f = NetAdmin.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(null, value);
	}

	private static <T> T invokeGetUIText(String key) throws Exception {
		Method m = NetAdmin.class.getDeclaredMethod("getUIText", String.class);
		m.setAccessible(true);
		return (T) m.invoke(null, key);
	}

	@SuppressWarnings("deprecation")
	private static NetAdmin allocateNetAdmin() throws Exception {
		Field uf = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		uf.setAccessible(true);
		sun.misc.Unsafe unsafe = (sun.misc.Unsafe) uf.get(null);
		return (NetAdmin) unsafe.allocateInstance(NetAdmin.class);
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}

	private static String[] invokeCreateRoomListRowData(NetAdmin admin, NetRoomInfo r) throws Exception {
		Method m = NetAdmin.class.getDeclaredMethod("createRoomListRowData", NetRoomInfo.class);
		m.setAccessible(true);
		return (String[]) m.invoke(admin, r);
	}
}

package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.LinkedList;

import org.junit.jupiter.api.Test;

/**
 * Closes the remaining branch gap in {@link NetSPRecord#hasCustomStats}
 * (L310): the null-list short circuit versus the empty and populated list
 * outcomes.
 */
class NetSPRecordBranchGapTest {

	private static boolean invokeHasCustomStats(NetSPRecord record) throws Exception {
		Method m = NetSPRecord.class.getDeclaredMethod("hasCustomStats");
		m.setAccessible(true);
		return (Boolean) m.invoke(record);
	}

	@Test
	void hasCustomStatsCoversNullEmptyAndPopulatedLists() throws Exception {
		NetSPRecord record = new NetSPRecord();

		record.listCustomStats = null;
		assertEquals(false, invokeHasCustomStats(record), "null list has no custom stats");

		record.listCustomStats = new LinkedList<String>();
		assertEquals(false, invokeHasCustomStats(record), "empty list has no custom stats");

		record.listCustomStats.add("stat;1");
		assertEquals(true, invokeHasCustomStats(record), "populated list has custom stats");
	}
}

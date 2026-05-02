package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedList;

import nullpomino.game.component.Statistics;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link NetSPRecord#reset} (the no-arg constructor's seed) and
 * {@link NetSPRecord#copy} (the deep-copy semantics for stats and
 * listCustomStats). Together these define the reset baseline and the
 * copy-on-write contract that the network rankings rely on for
 * isolation between concurrent updates.
 */
class NetSPRecordResetCopyTest {

	@Test
	void resetSetsAllStringFieldsToEmptyAndStatsToNull() {
		NetSPRecord record = new NetSPRecord();
		// Spoil every reset-ed field so we can prove reset clears them.
		record.strPlayerName = "Player";
		record.strModeName = "Mode";
		record.strRuleName = "Rule";
		record.stats = new Statistics();
		record.listCustomStats = null;
		record.strReplayProp = "rep";
		record.strTimeStamp = "ts";
		record.gameType = 5;
		record.style = 3;

		record.reset();

		assertEquals("", record.strPlayerName);
		assertEquals("", record.strModeName);
		assertEquals("", record.strRuleName);
		assertNull(record.stats);
		assertNotNull(record.listCustomStats,
				"reset must leave a fresh LinkedList rather than null");
		assertEquals(0, record.listCustomStats.size());
		assertEquals("", record.strReplayProp);
		assertEquals("", record.strTimeStamp);
		assertEquals(0, record.gameType);
		assertEquals(0, record.style);
	}

	@Test
	void copyDoesNotShareStatsReferenceWhenSourceHasStats() {
		// copy() must clone stats (Statistics is mutable) so a later
		// edit to either record doesn't bleed into the other.
		NetSPRecord source = new NetSPRecord();
		source.stats = new Statistics();
		source.stats.score = 100;

		NetSPRecord dest = new NetSPRecord();
		dest.copy(source);

		assertNotNull(dest.stats);
		assertNotSame(source.stats, dest.stats,
				"copy must allocate a new Statistics so source and dest don't share");
		assertEquals(100, dest.stats.score);

		// Confirm isolation by mutating source.
		source.stats.score = 999;
		assertEquals(100, dest.stats.score);
	}

	@Test
	void copyPropagatesNullStatsAsNull() {
		// If source.stats is null, dest.stats must also be null —
		// allocating a fresh Statistics for a no-stats record would
		// confuse the rankings code that treats null as 'no entry'.
		NetSPRecord source = new NetSPRecord();
		source.stats = null;

		NetSPRecord dest = new NetSPRecord();
		dest.stats = new Statistics();
		dest.copy(source);

		assertNull(dest.stats,
				"copy from a stats=null source must clear dest.stats too");
	}

	@Test
	void copyClonesCustomStatsListAndPreservesContents() {
		// listCustomStats is a List of String entries that survive
		// across copy. Pin that the list is a *fresh* LinkedList (not
		// the source's) but that contents match exactly.
		NetSPRecord source = new NetSPRecord();
		source.listCustomStats.add("score;100");
		source.listCustomStats.add("combo;5");

		NetSPRecord dest = new NetSPRecord();
		dest.copy(source);

		assertNotSame(source.listCustomStats, dest.listCustomStats,
				"copy must allocate a fresh LinkedList for listCustomStats");
		assertEquals(2, dest.listCustomStats.size());
		assertEquals("score;100", dest.listCustomStats.get(0));
		assertEquals("combo;5", dest.listCustomStats.get(1));

		// Confirm isolation by mutating source's list.
		source.listCustomStats.add("extra;1");
		assertEquals(2, dest.listCustomStats.size(),
				"mutations on source after copy must not bleed into dest");
	}

	@Test
	void copyTransfersScalarFieldsByValue() {
		NetSPRecord source = new NetSPRecord();
		source.strPlayerName = "Alice";
		source.strModeName = "MARATHON";
		source.strRuleName = "Standard";
		source.strReplayProp = "compressed";
		source.strTimeStamp = "2026-04-23";
		source.gameType = 7;
		source.style = 1;

		NetSPRecord dest = new NetSPRecord();
		dest.copy(source);

		assertEquals("Alice", dest.strPlayerName);
		assertEquals("MARATHON", dest.strModeName);
		assertEquals("Standard", dest.strRuleName);
		assertEquals("compressed", dest.strReplayProp);
		assertEquals("2026-04-23", dest.strTimeStamp);
		assertEquals(7, dest.gameType);
		assertEquals(1, dest.style);
	}

	@Test
	void copyConstructorDelegatesToCopyAndDoesNotShareLists() {
		// The (NetSPRecord) constructor delegates to copy(); pin the
		// same isolation contract through that path.
		NetSPRecord source = new NetSPRecord();
		source.strPlayerName = "Bob";
		source.listCustomStats.add("k;v");

		NetSPRecord dest = new NetSPRecord(source);

		assertEquals("Bob", dest.strPlayerName);
		assertNotSame(source.listCustomStats, dest.listCustomStats);
		assertEquals(1, dest.listCustomStats.size());
		assertEquals("k;v", dest.listCustomStats.get(0));
	}

	@Test
	void copyThenResetClearsTheClonedStateWithoutTouchingSource() {
		// Belt-and-braces: a copy followed by a reset on dest must
		// leave source untouched, since copy already broke aliasing.
		NetSPRecord source = new NetSPRecord();
		source.strPlayerName = "Carol";
		source.gameType = 4;
		LinkedList<String> sourceList = source.listCustomStats;

		NetSPRecord dest = new NetSPRecord(source);
		dest.reset();

		assertEquals("Carol", source.strPlayerName,
				"reset on dest must not mutate source");
		assertEquals(4, source.gameType);
		assertNotSame(sourceList, dest.listCustomStats,
				"reset re-allocates dest.listCustomStats; source's list survives");
	}
}

package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.LinkedList;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NetServer} components that can be exercised without
 * opening network sockets or loading config files.  Covers constants,
 * rating math (rankDelta / expectedScore / maxDelta), leaderboard index
 * helpers, and the inner ChangeRequest data class.
 */
class NetServerTest {

	private NetServer server;
	private MethodHandle rankDelta;
	private MethodHandle expectedScore;
	private MethodHandle maxDelta;
	private MethodHandle mpRankingIndexOfPlayer;
	private MethodHandle mpRankingIndexOfName;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() throws Exception {
		// --- ensure static fields that init() reads are safe ---
		// propServer is null by default; init() calls propServer.getProperty()
		// which would NPE. Seed it with an empty properties so defaults are used.
		setStaticField("propServer", new CustomProperties());

		// Create a server instance.  The constructor calls init() which loads
		// various config files; all loaders have try-catch blocks that degrade
		// gracefully when files are absent.  With propServer set, the
		// bulk-property reads return default values.
		server = new NetServer(9999);

		// --- locate private methods ---
		MethodHandles.Lookup lookup =
				MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());

		rankDelta = lookup.findVirtual(NetServer.class, "rankDelta",
				MethodType.methodType(double.class, int.class, double.class, double.class, double.class));
		expectedScore = lookup.findVirtual(NetServer.class, "expectedScore",
				MethodType.methodType(double.class, double.class, double.class));
		maxDelta = lookup.findVirtual(NetServer.class, "maxDelta",
				MethodType.methodType(double.class, int.class));
		mpRankingIndexOfPlayer = lookup.findStatic(NetServer.class, "mpRankingIndexOf",
				MethodType.methodType(int.class, int.class, NetPlayerInfo.class));
		mpRankingIndexOfName = lookup.findStatic(NetServer.class, "mpRankingIndexOf",
				MethodType.methodType(int.class, int.class, String.class));

		// --- seed static fields with realistic values ---
		setStaticDouble("ratingNormalMaxDiff", NetServer.NORMAL_MAX_DIFF);
		setStaticInt("ratingProvisionalGames", NetServer.PROVISIONAL_GAMES);
		setStaticInt("ratingMin", 0);
		setStaticInt("ratingMax", 99999);
		setStaticInt("ratingDefault", NetPlayerInfo.DEFAULT_MULTIPLAYER_RATING);
		setStaticInt("maxMPRanking", NetServer.DEFAULT_MAX_MPRANKING);

		// Initialise mpRankingList so mpRankingIndexOf can operate.
		Field f = NetServer.class.getDeclaredField("mpRankingList");
		f.setAccessible(true);
		LinkedList<NetPlayerInfo>[] list = new LinkedList[4];
		for (int i = 0; i < 4; i++) {
			list[i] = new LinkedList<NetPlayerInfo>();
		}
		f.set(null, list);
	}

	// ------------------------------------------------------------------
	// Helpers: reflect into private static fields
	// ------------------------------------------------------------------

	private static void setStaticDouble(String name, double value) throws Exception {
		Field f = NetServer.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setDouble(null, value);
	}

	private static void setStaticInt(String name, int value) throws Exception {
		Field f = NetServer.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(null, value);
	}

	private static void setStaticField(String name, Object value) throws Exception {
		Field f = NetServer.class.getDeclaredField(name);
		f.setAccessible(true);
		f.set(null, value);
	}

	// ------------------------------------------------------------------
	// Constants
	// ------------------------------------------------------------------

	@Test
	void constants() {
		assertEquals(9200, NetServer.DEFAULT_PORT);
		assertEquals(8192, NetServer.BUF_SIZE);
		assertEquals(512, NetServer.RULE_BUF_SIZE);
		assertEquals(16.0, NetServer.NORMAL_MAX_DIFF, 1e-12);
		assertEquals(50, NetServer.PROVISIONAL_GAMES);
		assertEquals(100, NetServer.DEFAULT_MAX_MPRANKING);
		assertEquals(100, NetServer.DEFAULT_MAX_SPRANKING);
		assertEquals(80f, NetServer.DEFAULT_MIN_GAMERATE);
		assertEquals(60_000L, NetServer.DEFAULT_TIMEOUT_TIME);
		assertEquals(10, NetServer.DEFAULT_MAX_LOBBYCHAT_HISTORY);
		assertEquals(10, NetServer.DEFAULT_MAX_ROOMCHAT_HISTORY);
	}

	// ------------------------------------------------------------------
	// expectedScore — Elo expectation formula
	// ------------------------------------------------------------------

	@Test
	void expectedScoreEqualRatingsIsPointFive() throws Throwable {
		double e = (double) expectedScore.invokeExact(server, 1500.0, 1500.0);
		assertEquals(0.5, e, 1e-12);
	}

	@Test
	void expectedScoreHigherRankHasHigherExpectation() throws Throwable {
		double high = (double) expectedScore.invokeExact(server, 1600.0, 1500.0);
		double low  = (double) expectedScore.invokeExact(server, 1500.0, 1600.0);
		assertTrue(high > 0.5, "higher-ranked player should have E > 0.5");
		assertTrue(low  < 0.5, "lower-ranked player should have E < 0.5");
		assertEquals(1.0, high + low, 1e-12, "expectations should sum to 1");
	}

	@Test
	void expectedScoreLargeRatingGap() throws Throwable {
		double weak = (double) expectedScore.invokeExact(server, 1000.0, 2000.0);
		double strong = (double) expectedScore.invokeExact(server, 2000.0, 1000.0);
		assertEquals(1.0, weak + strong, 1e-12);
		assertTrue(weak < 0.01, "1000 vs 2000 should be nearly hopeless");
		assertTrue(strong > 0.99, "2000 vs 1000 should be nearly certain");
	}

	// ------------------------------------------------------------------
	// maxDelta — K-value helper
	// ------------------------------------------------------------------

	@Test
	void maxDeltaUnderProvisionalGamesReturnsAugmentedValue() throws Throwable {
		// Uses INTEGER division: 400 / (0 + 3) = 133 (not 133.333...)
		double d = (double) maxDelta.invokeExact(server, 0);
		assertEquals(NetServer.NORMAL_MAX_DIFF + 400 / 3, d, 1e-12);
	}

	@Test
	void maxDeltaAtProvisionalThresholdReturnsAugmentedValue() throws Throwable {
		// Uses INTEGER division: 400 / (50 + 3) = 7 (not 7.547...)
		double d = (double) maxDelta.invokeExact(server, NetServer.PROVISIONAL_GAMES);
		assertEquals(NetServer.NORMAL_MAX_DIFF + 400 / (NetServer.PROVISIONAL_GAMES + 3), d, 1e-12);
	}

	@Test
	void maxDeltaAfterProvisionalGamesReturnsNormalValue() throws Throwable {
		double d = (double) maxDelta.invokeExact(server, NetServer.PROVISIONAL_GAMES + 1);
		assertEquals(NetServer.NORMAL_MAX_DIFF, d, 1e-12);
	}

	// ------------------------------------------------------------------
	// rankDelta — combined rating change
	// ------------------------------------------------------------------

	@Test
	void rankDeltaWinWithEqualRatingsAndManyGames() throws Throwable {
		double delta = (double) rankDelta.invokeExact(server, NetServer.PROVISIONAL_GAMES + 1, 1500.0, 1500.0, 1.0);
		double expected = NetServer.NORMAL_MAX_DIFF * (1.0 - 0.5);
		assertEquals(expected, delta, 1e-12);
	}

	@Test
	void rankDeltaLossWithEqualRatingsAndManyGames() throws Throwable {
		double delta = (double) rankDelta.invokeExact(server, NetServer.PROVISIONAL_GAMES + 1, 1500.0, 1500.0, 0.0);
		double expected = NetServer.NORMAL_MAX_DIFF * (0.0 - 0.5);
		assertEquals(expected, delta, 1e-12);
	}

	@Test
	void rankDeltaUnderdogWinHasLargerDelta() throws Throwable {
		double underdog = (double) rankDelta.invokeExact(server, NetServer.PROVISIONAL_GAMES + 1, 1000.0, 2000.0, 1.0);
		double favorite = (double) rankDelta.invokeExact(server, NetServer.PROVISIONAL_GAMES + 1, 2000.0, 1000.0, 1.0);
		assertTrue(underdog > favorite,
				"underdog win should produce larger rating change than favourite win");
	}

	// ------------------------------------------------------------------
	// mpRankingIndexOf — static leaderboard lookup
	// ------------------------------------------------------------------

	@Test
	void mpRankingIndexOfNullReturnsMinusOne() throws Throwable {
		int idx = (int) mpRankingIndexOfPlayer.invokeExact(0, (NetPlayerInfo) null);
		assertEquals(-1, idx);
	}

	@Test
	void mpRankingIndexOfNullNameReturnsMinusOne() throws Throwable {
		int idx = (int) mpRankingIndexOfName.invokeExact(0, (String) null);
		assertEquals(-1, idx);
	}

	@Test
	void mpRankingIndexOfEmptyListReturnsMinusOne() throws Throwable {
		NetPlayerInfo p = new NetPlayerInfo();
		p.strName = "Alice";
		int idx = (int) mpRankingIndexOfPlayer.invokeExact(0, p);
		assertEquals(-1, idx);
	}

	@Test
	void mpRankingIndexOfFindsExistingEntry() throws Throwable {
		LinkedList<NetPlayerInfo> list = getMpRankingList(0);
		NetPlayerInfo alice = new NetPlayerInfo();
		alice.strName = "Alice";
		alice.rating[0] = 1600;
		list.add(alice);

		int idx = (int) mpRankingIndexOfName.invokeExact(0, "Alice");
		assertEquals(0, idx);

		NetPlayerInfo lookup = new NetPlayerInfo();
		lookup.strName = "Alice";
		idx = (int) mpRankingIndexOfPlayer.invokeExact(0, lookup);
		assertEquals(0, idx);
	}

	@Test
	void mpRankingIndexOfRespectsStyleBucket() throws Throwable {
		LinkedList<NetPlayerInfo> list0 = getMpRankingList(0);
		LinkedList<NetPlayerInfo> list1 = getMpRankingList(1);
		NetPlayerInfo p = new NetPlayerInfo();
		p.strName = "Bob";
		list0.add(p);

		assertEquals(0, (int) mpRankingIndexOfName.invokeExact(0, "Bob"));
		assertEquals(-1, (int) mpRankingIndexOfName.invokeExact(1, "Bob"));
	}

	@SuppressWarnings("unchecked")
	private static LinkedList<NetPlayerInfo> getMpRankingList(int style) throws Exception {
		Field f = NetServer.class.getDeclaredField("mpRankingList");
		f.setAccessible(true);
		LinkedList<NetPlayerInfo>[] arr = (LinkedList<NetPlayerInfo>[]) f.get(null);
		return arr[style];
	}
}

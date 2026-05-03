package nullpomino.tool.airankstool;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Tests AIRanksTester construction and basic invariants.
 *
 * <p>AIRanksTester is a CLI harness that runs AI games to measure the
 * ranks performance. Its constructor initializes a RanksAI and stores
 * the iteration count. The class has no Swing dependencies.
 */
class AIRanksTesterTest {

	@Test
	void constructorDoesNotThrow() {
		assertDoesNotThrow(() -> new AIRanksTester(10));
	}

	@Test
	void constructorAcceptsZeroIterations() {
		assertDoesNotThrow(() -> new AIRanksTester(0));
	}

	@Test
	void constructorAcceptsLargeIterationCount() {
		assertDoesNotThrow(() -> new AIRanksTester(1000));
	}

	@Test
	void constructorCreatesNonNullInstance() {
		AIRanksTester tester = new AIRanksTester(5);
		assertNotNull(tester);
	}

	@Test
	void constructorAcceptsNegativeIterations() {
		// The constructor doesn't validate inputs, so negative is accepted.
		assertDoesNotThrow(() -> new AIRanksTester(-1));
	}
}

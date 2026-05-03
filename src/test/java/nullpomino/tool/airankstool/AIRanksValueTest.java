package nullpomino.tool.airankstool;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Tests AIRanksValue class loading and instantiation.
 *
 * <p>AIRanksValue is a CLI diagnostic that loads a serialized Ranks
 * file and prints rank values for two hardcoded surface configurations.
 * It has no Swing dependencies and can be freely constructed.
 */
class AIRanksValueTest {

	@Test
	void classIsLoadable() {
		assertNotNull(AIRanksValue.class);
	}

	@Test
	void constructorDoesNotThrow() {
		assertDoesNotThrow(() -> new AIRanksValue());
	}

	@Test
	void mainDoesNotThrowWithEmptyArgs() {
		// main() expects a file argument, but should handle missing input
		// gracefully by creating a new Ranks instance.
		assertDoesNotThrow(() -> AIRanksValue.main(new String[0]));
	}
}

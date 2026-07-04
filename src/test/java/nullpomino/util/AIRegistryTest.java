package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import nullpomino.game.ai.DummyAI;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class AIRegistryTest {

	@TestFactory
	Stream<DynamicTest> everyRegistryEntryInstantiates() {
		assertTrue(!AIRegistry.all().isEmpty(), "AI registry is empty");

		return AIRegistry.all().stream().map(cls -> DynamicTest.dynamicTest(cls.getName(), () -> {
			Object instance = cls.getDeclaredConstructor().newInstance();
			assertTrue(instance instanceof DummyAI,
					cls.getName() + " is listed but does not extend " + DummyAI.class.getName());
		}));
	}

	@Test
	void suppliersAlignWithClasses() {
		assertEquals(AIRegistry.all().size(), AIRegistry.suppliers().size(),
				"suppliers() and all() must stay in lockstep");
		for (int i = 0; i < AIRegistry.all().size(); i++) {
			assertEquals(AIRegistry.all().get(i), AIRegistry.suppliers().get(i).get().getClass(),
					"supplier at index " + i + " builds the wrong class");
		}
	}
}

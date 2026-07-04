package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import nullpomino.game.ai.DummyAI;
import nullpomino.game.mode.GameMode;
import nullpomino.game.randomizer.Randomizer;
import nullpomino.game.wallkick.Wallkick;

import org.junit.jupiter.api.Test;

class FactoryDefaultsTest {

	@Test
	void installAllRegistersEveryBuiltInForReflectionFreeCreation() throws Exception {
		FactoryDefaults.installAll();

		for (Class<? extends Wallkick> cls : WallkickRegistry.all()) {
			assertInstanceOf(cls, ClassFactory.create(cls.getName(), Wallkick.class),
					cls.getName() + " not resolvable via ClassFactory after installAll()");
		}
		for (Class<? extends Randomizer> cls : RandomizerRegistry.all()) {
			assertInstanceOf(cls, ClassFactory.create(cls.getName(), Randomizer.class),
					cls.getName() + " not resolvable via ClassFactory after installAll()");
		}
		for (Class<? extends DummyAI> cls : AIRegistry.all()) {
			assertInstanceOf(cls, ClassFactory.create(cls.getName(), DummyAI.class),
					cls.getName() + " not resolvable via ClassFactory after installAll()");
		}
	}

	@Test
	void standaloneModesMatchModeRegistryMinusNetplay() throws Exception {
		List<Class<? extends GameMode>> expected = ModeRegistry.all().stream()
				.filter(cls -> {
					try {
						return !cls.getDeclaredConstructor().newInstance().isNetplayMode();
					} catch (ReflectiveOperationException e) {
						throw new AssertionError(e);
					}
				})
				.toList();

		List<? extends Class<? extends GameMode>> actual = StandaloneModeRegistry.suppliers().stream()
				.map(s -> s.get().getClass())
				.toList();

		assertEquals(expected, actual,
				"StandaloneModeRegistry must equal ModeRegistry minus the netplay modes, in order");

		for (var supplier : StandaloneModeRegistry.suppliers()) {
			GameMode mode = supplier.get();
			assertFalse(mode.isNetplayMode(),
					mode.getClass().getName() + " is a netplay mode and must not be in StandaloneModeRegistry");
		}
	}
}

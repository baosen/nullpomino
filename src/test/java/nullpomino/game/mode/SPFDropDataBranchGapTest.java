package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.RuleOptions;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

/**
 * Branch-gap tests for {@link SPFDropData#loadDropMapPreview}: the null
 * pattern with and without an existing field.
 */
class SPFDropDataBranchGapTest {

	private static GameEngine engine() {
		GameManager gm = new GameManager(new EventReceiver());
		return new GameEngine(gm, 0, new RuleOptions(), null, null);
	}

	/** Null pattern and no field: nothing to reset, no field gets created. */
	@Test
	void loadDropMapPreviewIsNoOpWithNullPatternAndNullField() {
		GameEngine engine = engine();
		assertNull(engine.field, "precondition: fresh engine has no field");

		SPFDropData.loadDropMapPreview(engine, null);

		assertNull(engine.field, "no field must be created for a null pattern");
	}

	/** Null pattern with an existing field: the field is cleared. */
	@Test
	void loadDropMapPreviewResetsExistingFieldForNullPattern() {
		GameEngine engine = engine();
		engine.field = new Field(10, 10, 0, false);
		engine.field.setBlockColor(4, 9, Block.BLOCK_COLOR_RED);

		SPFDropData.loadDropMapPreview(engine, null);

		assertTrue(engine.field.isEmpty(), "existing field is reset");
		assertEquals(Block.BLOCK_COLOR_NONE, engine.field.getBlockColor(4, 9));
	}
}

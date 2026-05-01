package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PieceTest {

	@Test
	void copyConstructorCopiesMutableArraysAndBlocksWithoutSharing() {
		Piece original = new Piece(Piece.PIECE_T);
		original.direction = Piece.DIRECTION_RIGHT;
		original.setColor(Block.BLOCK_COLOR_RED);
		original.applyOffsetArray(
				new int[] {1, 2, 3, 4},
				new int[] {5, 6, 7, 8});

		Piece copy = new Piece(original);
		original.dataX[Piece.DIRECTION_RIGHT][0] = 99;
		original.dataY[Piece.DIRECTION_RIGHT][0] = 88;
		original.dataOffsetX[Piece.DIRECTION_RIGHT] = 77;
		original.dataOffsetY[Piece.DIRECTION_RIGHT] = 66;
		original.block[0].color = Block.BLOCK_COLOR_BLUE;

		assertEquals(Piece.PIECE_T, copy.id);
		assertEquals(Piece.DIRECTION_RIGHT, copy.direction);
		assertEquals(Block.BLOCK_COLOR_RED, copy.block[0].color);
		assertEquals(2, copy.dataOffsetX[Piece.DIRECTION_RIGHT]);
		assertEquals(6, copy.dataOffsetY[Piece.DIRECTION_RIGHT]);
		assertEquals(Piece.DEFAULT_PIECE_DATA_X[Piece.PIECE_T][Piece.DIRECTION_RIGHT][0] + 2,
				copy.dataX[Piece.DIRECTION_RIGHT][0]);
		assertEquals(Piece.DEFAULT_PIECE_DATA_Y[Piece.PIECE_T][Piece.DIRECTION_RIGHT][0] + 6,
				copy.dataY[Piece.DIRECTION_RIGHT][0]);
		assertNotSame(original.block[0], copy.block[0]);
	}

	@Test
	void getPieceNameReturnsKnownNamesAndQuestionForOutOfRange() {
		assertEquals("I", Piece.getPieceName(Piece.PIECE_I));
		assertEquals("L3", Piece.getPieceName(Piece.PIECE_L3));
		assertEquals("?", Piece.getPieceName(-1));
		assertEquals("?", Piece.getPieceName(Piece.PIECE_NAMES.length));
	}

	@Test
	void defaultConstructorInitializesIPieceFacingUp() {
		Piece p = new Piece();

		assertEquals(Piece.PIECE_I, p.id);
		assertEquals(Piece.DIRECTION_UP, p.direction);
		assertFalse(p.big);
		assertFalse(p.offsetApplied);
		assertTrue(p.connectBlocks);
		assertEquals(4, p.getMaxBlock());
		assertNotNull(p.block[0]);
		assertEquals(Block.BLOCK_COLOR_NONE, p.block[0].color);
		for (int dir = 0; dir < Piece.DIRECTION_COUNT; dir++) {
			assertEquals(0, p.dataOffsetX[dir]);
			assertEquals(0, p.dataOffsetY[dir]);
		}
	}

	@Test
	void idConstructorAppliesIdAndDefaultShape() {
		Piece p = new Piece(Piece.PIECE_T);

		assertEquals(Piece.PIECE_T, p.id);
		assertEquals(4, p.getMaxBlock());
		for (int j = 0; j < 4; j++) {
			assertEquals(Piece.DEFAULT_PIECE_DATA_X[Piece.PIECE_T][Piece.DIRECTION_UP][j],
					p.dataX[Piece.DIRECTION_UP][j]);
			assertEquals(Piece.DEFAULT_PIECE_DATA_Y[Piece.PIECE_T][Piece.DIRECTION_UP][j],
					p.dataY[Piece.DIRECTION_UP][j]);
		}
	}

	@Test
	void getMaxBlockReportsBlockCountForEachPiece() {
		assertEquals(4, new Piece(Piece.PIECE_I).getMaxBlock());
		assertEquals(4, new Piece(Piece.PIECE_T).getMaxBlock());
		assertEquals(1, new Piece(Piece.PIECE_I1).getMaxBlock());
		assertEquals(2, new Piece(Piece.PIECE_I2).getMaxBlock());
		assertEquals(3, new Piece(Piece.PIECE_I3).getMaxBlock());
		assertEquals(3, new Piece(Piece.PIECE_L3).getMaxBlock());
	}

	@Test
	void setBlockCopiesTemplateOntoEveryBlock() {
		Piece p = new Piece(Piece.PIECE_T);
		Block template = new Block(Block.BLOCK_COLOR_PURPLE, 5, Block.BLOCK_ATTRIBUTE_BONE);

		p.setBlock(template);

		for (Block b : p.block) {
			assertEquals(Block.BLOCK_COLOR_PURPLE, b.color);
			assertEquals(5, b.skin);
			assertEquals(Block.BLOCK_ATTRIBUTE_BONE, b.attribute);
		}
		p.block[0].color = Block.BLOCK_COLOR_NONE;
		assertEquals(Block.BLOCK_COLOR_PURPLE, template.color);
	}

	@Test
	void setColorArrayAssignsPerBlockColors() {
		Piece p = new Piece(Piece.PIECE_T);

		p.setColor(new int[] {
				Block.BLOCK_COLOR_RED,
				Block.BLOCK_COLOR_BLUE,
				Block.BLOCK_COLOR_GREEN,
				Block.BLOCK_COLOR_YELLOW
		});

		assertArrayEquals(new int[] {
				Block.BLOCK_COLOR_RED,
				Block.BLOCK_COLOR_BLUE,
				Block.BLOCK_COLOR_GREEN,
				Block.BLOCK_COLOR_YELLOW
		}, p.getColors());
	}

	@Test
	void setColorArrayShorterThanBlocksLeavesTrailingBlocksUnchanged() {
		Piece p = new Piece(Piece.PIECE_T);
		p.setColor(Block.BLOCK_COLOR_GRAY);

		p.setColor(new int[] { Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE });

		int[] colors = p.getColors();
		assertEquals(Block.BLOCK_COLOR_RED, colors[0]);
		assertEquals(Block.BLOCK_COLOR_BLUE, colors[1]);
		assertEquals(Block.BLOCK_COLOR_GRAY, colors[2]);
		assertEquals(Block.BLOCK_COLOR_GRAY, colors[3]);
	}

	@Test
	void setSkinAndDarknessApplyToAllBlocks() {
		Piece p = new Piece(Piece.PIECE_S);

		p.setSkin(7);
		p.setDarkness(0.25f);

		for (Block b : p.block) {
			assertEquals(7, b.skin);
			assertEquals(0.25f, b.darkness);
		}
	}

	@Test
	void setAttributeFlipsBitOnEveryBlock() {
		Piece p = new Piece(Piece.PIECE_O);

		p.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		for (Block b : p.block) {
			assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		}

		p.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, false);
		for (Block b : p.block) {
			assertFalse(b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		}
	}

	@Test
	void getRotateDirectionWrapsCardinalAndHalfTurnsAroundFour() {
		Piece p = new Piece(Piece.PIECE_T);

		p.direction = Piece.DIRECTION_UP;
		assertEquals(Piece.DIRECTION_RIGHT, p.getRotateDirection(1));
		assertEquals(Piece.DIRECTION_LEFT, p.getRotateDirection(-1));
		assertEquals(Piece.DIRECTION_DOWN, p.getRotateDirection(2));

		p.direction = Piece.DIRECTION_LEFT;
		assertEquals(Piece.DIRECTION_UP, p.getRotateDirection(1));

		p.direction = Piece.DIRECTION_DOWN;
		assertEquals(Piece.DIRECTION_UP, p.getRotateDirection(2));

		p.direction = Piece.DIRECTION_LEFT;
		assertEquals(Piece.DIRECTION_RIGHT, p.getRotateDirection(2));

		assertEquals(Piece.DIRECTION_LEFT, p.getRotateDirection(-1, Piece.DIRECTION_UP));
		assertEquals(Piece.DIRECTION_UP, p.getRotateDirection(2, Piece.DIRECTION_DOWN));
		assertEquals(Piece.DIRECTION_RIGHT, p.getRotateDirection(1, Piece.DIRECTION_UP));
		assertEquals(Piece.DIRECTION_RIGHT, p.getRotateDirection(2, Piece.DIRECTION_LEFT));
	}

	@Test
	void widthAndHeightReflectShapeAndDoubleForBigPiece() {
		Piece p = new Piece(Piece.PIECE_I);
		p.direction = Piece.DIRECTION_UP;
		assertEquals(3, p.getWidth());
		assertEquals(0, p.getHeight());

		p.big = true;
		assertEquals(6, p.getWidth());
		assertEquals(0, p.getHeight());

		Piece q = new Piece(Piece.PIECE_O);
		q.direction = Piece.DIRECTION_UP;
		assertEquals(1, q.getWidth());
		assertEquals(1, q.getHeight());
	}

	@Test
	void minimumAndMaximumBlockCoordsExposeShapeBounds() {
		Piece p = new Piece(Piece.PIECE_I);
		p.direction = Piece.DIRECTION_UP;

		assertEquals(0, p.getMinimumBlockX());
		assertEquals(3, p.getMaximumBlockX());
		assertEquals(1, p.getMinimumBlockY());
		assertEquals(1, p.getMaximumBlockY());

		p.big = true;
		assertEquals(0, p.getMinimumBlockX());
		assertEquals(6, p.getMaximumBlockX());
		assertEquals(2, p.getMinimumBlockY());
		assertEquals(2, p.getMaximumBlockY());
	}

	@Test
	void isPartialLockOutDetectsBlocksAboveTopOfField() {
		Field fld = new Field();
		Piece p = new Piece(Piece.PIECE_T);
		p.direction = Piece.DIRECTION_UP;

		assertFalse(p.isPartialLockOut(0, 0, p.direction, fld));
		assertTrue(p.isPartialLockOut(0, -1, p.direction, fld));
		assertTrue(p.isPartialLockOut(0, -1, fld));
	}

	@Test
	void canPlaceToVisibleFieldDetectsAtLeastOneVisibleBlock() {
		Field fld = new Field();
		Piece p = new Piece(Piece.PIECE_T);
		p.direction = Piece.DIRECTION_UP;

		assertTrue(p.canPlaceToVisibleField(0, 0, p.direction, fld));
		assertFalse(p.canPlaceToVisibleField(0, -2, p.direction, fld));
		assertTrue(p.canPlaceToVisibleField(0, -1, fld));
	}

	@Test
	void placeToFieldStampsBlocksAndReportsVisibility() {
		Field fld = new Field();
		Piece p = new Piece(Piece.PIECE_T);
		p.setColor(Block.BLOCK_COLOR_PURPLE);

		boolean visible = p.placeToField(3, 18, fld);

		assertTrue(visible);
		assertEquals(Block.BLOCK_COLOR_PURPLE, fld.getBlockColor(4, 18));
		assertEquals(Block.BLOCK_COLOR_PURPLE, fld.getBlockColor(3, 19));
		assertEquals(Block.BLOCK_COLOR_PURPLE, fld.getBlockColor(4, 19));
		assertEquals(Block.BLOCK_COLOR_PURPLE, fld.getBlockColor(5, 19));
	}

	@Test
	void placeToFieldOverloadUsesPieceDirection() {
		Field fld = new Field();
		Piece p = new Piece(Piece.PIECE_T);
		p.direction = Piece.DIRECTION_UP;
		p.setColor(Block.BLOCK_COLOR_GREEN);

		assertTrue(p.placeToField(3, 18, fld));
		assertEquals(Block.BLOCK_COLOR_GREEN, fld.getBlockColor(4, 18));
	}

	@Test
	void placeToFieldReturnsFalseWhenAllBlocksLandAboveTop() {
		Field fld = new Field();
		Piece p = new Piece(Piece.PIECE_O);

		assertFalse(p.placeToField(0, -2, fld));
	}

	@Test
	void checkCollisionDetectsRightWallBottomLeftWallAndOccupiedCells() {
		Field fld = new Field();
		Piece p = new Piece(Piece.PIECE_O);
		p.direction = Piece.DIRECTION_UP;

		assertFalse(p.checkCollision(0, 0, p.direction, fld));
		assertTrue(p.checkCollision(0, fld.getHeight(), p.direction, fld));
		assertTrue(p.checkCollision(fld.getWidth() - 1, 0, p.direction, fld));
		assertTrue(p.checkCollision(-1, 0, p.direction, fld));

		fld.setBlock(0, 19, new Block(Block.BLOCK_COLOR_GRAY));
		assertTrue(p.checkCollision(0, 18, p.direction, fld));

		assertTrue(p.checkCollision(0, fld.getHeight(), fld));
	}

	@Test
	void getBottomDropsToFloor() {
		Field fld = new Field();
		Piece p = new Piece(Piece.PIECE_O);
		p.direction = Piece.DIRECTION_UP;

		assertEquals(18, p.getBottom(0, 0, p.direction, fld));
		assertEquals(18, p.getBottom(0, 0, fld));
	}

	@Test
	void getMostMovableLeftAndRightStopAtWalls() {
		Field fld = new Field();
		Piece p = new Piece(Piece.PIECE_O);
		p.direction = Piece.DIRECTION_UP;

		assertEquals(0, p.getMostMovableLeft(4, 0, p.direction, fld));
		assertEquals(8, p.getMostMovableRight(4, 0, p.direction, fld));
	}

	@Test
	void updateConnectDataMarksAdjacentSidesWhenConnected() {
		Piece p = new Piece(Piece.PIECE_I);
		p.direction = Piece.DIRECTION_UP;

		p.updateConnectData();

		assertTrue(p.block[0].getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT));
		assertFalse(p.block[0].getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT));
		assertTrue(p.block[3].getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT));
		assertFalse(p.block[3].getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT));
		assertTrue(p.block[1].getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT));
		assertTrue(p.block[1].getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT));
		assertFalse(p.block[0].getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN));
	}

	@Test
	void updateConnectDataMarksBrokenWhenConnectionDisabled() {
		Piece p = new Piece(Piece.PIECE_I);
		p.connectBlocks = false;

		p.updateConnectData();

		for (Block b : p.block) {
			assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN));
			assertFalse(b.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT));
			assertFalse(b.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT));
		}
	}

	@Test
	void bigHorizontalPieceExercisesLeftRightConnectionBranchesWhenPlaced() {
		Field fld = new Field(20, 25, 5);
		Piece p = new Piece(Piece.PIECE_I);
		p.direction = Piece.DIRECTION_UP;
		p.big = true;
		p.setColor(Block.BLOCK_COLOR_GRAY);

		assertTrue(p.placeToField(0, 5, p.direction, fld));
		assertEquals(Block.BLOCK_COLOR_GRAY, fld.getBlockColor(0, 7));
	}

	@Test
	void bigVerticalPieceExercisesUpDownConnectionBranchesWhenPlaced() {
		Field fld = new Field(20, 25, 5);
		Piece p = new Piece(Piece.PIECE_I);
		p.direction = Piece.DIRECTION_RIGHT;
		p.big = true;
		p.setColor(Block.BLOCK_COLOR_RED);

		assertTrue(p.placeToField(0, 0, p.direction, fld));
		assertEquals(Block.BLOCK_COLOR_RED, fld.getBlockColor(4, 0));
	}

	@Test
	void bigPieceCollisionAndPlacementChecksHandleEdgesAndOccupancy() {
		Field fld = new Field(20, 25, 5);
		Piece p = new Piece(Piece.PIECE_I);
		p.direction = Piece.DIRECTION_UP;
		p.big = true;

		assertFalse(p.isPartialLockOut(0, 5, p.direction, fld));
		assertTrue(p.isPartialLockOut(0, -5, p.direction, fld));
		assertTrue(p.isPartialLockOut(0, -5, fld));

		assertTrue(p.canPlaceToVisibleField(0, 5, p.direction, fld));
		assertFalse(p.canPlaceToVisibleField(0, -5, p.direction, fld));
		assertTrue(p.canPlaceToVisibleField(0, 5, fld));

		assertFalse(p.checkCollision(0, 5, p.direction, fld));
		assertTrue(p.checkCollision(fld.getWidth() - 1, 5, p.direction, fld));
		assertTrue(p.checkCollision(0, fld.getHeight(), p.direction, fld));
		assertTrue(p.checkCollision(-1, 5, p.direction, fld));
		assertTrue(p.checkCollision(0, fld.getHeight(), fld));

		fld.setBlock(0, 7, new Block(Block.BLOCK_COLOR_GRAY));
		assertTrue(p.checkCollision(0, 5, p.direction, fld));

		Piece overload = new Piece(Piece.PIECE_O);
		overload.big = true;
		assertTrue(overload.placeToField(0, 18, fld));
	}
}

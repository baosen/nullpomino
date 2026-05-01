package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import nullpomino.util.CustomProperties;

class RuleOptionsTest {

	private static RuleOptions populated() {
		RuleOptions r = new RuleOptions();
		r.strRuleName = "Test Rule";
		r.strWallkick = "org.example.Wallkick";
		r.strRandomizer = "org.example.Randomizer";
		r.style = 2;

		for (int i = 0; i < Piece.PIECE_COUNT; i++) {
			for (int j = 0; j < Piece.DIRECTION_COUNT; j++) {
				r.pieceOffsetX[i][j] = i + 1;
				r.pieceOffsetY[i][j] = j + 1;
				r.pieceSpawnX[i][j] = i * 2;
				r.pieceSpawnY[i][j] = j * 2;
				r.pieceSpawnXBig[i][j] = i * 3;
				r.pieceSpawnYBig[i][j] = j * 3;
			}
			r.pieceDefaultDirection[i] = i % Piece.DIRECTION_COUNT;
		}
		r.pieceEnterAboveField = false;
		r.pieceEnterMaxDistanceY = 4;

		r.fieldWidth = 12;
		r.fieldHeight = 25;
		r.fieldHiddenHeight = 5;
		r.fieldCeiling = true;
		r.fieldLockoutDeath = false;
		r.fieldPartialLockoutDeath = true;

		r.nextDisplay = 7;

		r.holdEnable = false;
		r.holdInitial = false;
		r.holdInitialLimit = true;
		r.holdResetDirection = false;
		r.holdLimit = 3;

		r.harddropEnable = false;
		r.harddropLock = false;
		r.harddropLimit = false;

		r.softdropEnable = false;
		r.softdropLock = true;
		r.softdropLimit = true;
		r.softdropSurfaceLock = true;
		r.softdropSpeed = 0.75f;
		r.softdropMultiplyNativeSpeed = true;
		r.softdropGravitySpeedLimit = true;

		r.rotateInitial = false;
		r.rotateInitialLimit = true;
		r.rotateWallkick = false;
		r.rotateInitialWallkick = false;
		r.rotateMaxUpwardWallkick = 2;
		r.rotateButtonDefaultRight = false;
		r.rotateButtonAllowReverse = false;
		r.rotateButtonAllowDouble = false;

		r.lockresetFall = false;
		r.lockresetMove = false;
		r.lockresetRotate = false;
		r.lockresetWallkick = true;
		r.lockresetLimitMove = 30;
		r.lockresetLimitRotate = 30;
		r.lockresetLimitShareCount = false;
		r.lockresetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_NOWALLKICK;

		r.lockflash = 5;
		r.lockflashOnlyFrame = false;
		r.lockflashBeforeLineClear = true;
		r.areCancelMove = true;
		r.areCancelRotate = true;
		r.areCancelHold = true;

		r.minARE = 1;
		r.maxARE = 2;
		r.minARELine = 3;
		r.maxARELine = 4;
		r.minLineDelay = 5;
		r.maxLineDelay = 6;
		r.minLockDelay = 7;
		r.maxLockDelay = 8;
		r.minDAS = 9;
		r.maxDAS = 10;

		r.dasDelay = 2;
		r.shiftLockEnable = true;

		r.dasInReady = false;
		r.dasInMoveFirstFrame = false;
		r.dasInLockFlash = false;
		r.dasInLineClear = false;
		r.dasInARE = false;
		r.dasInARELastFrame = false;
		r.dasInEndingStart = false;
		r.dasChargeOnBlockedMove = true;
		r.dasStoreChargeOnNeutral = true;
		r.dasRedirectInDelay = true;

		r.moveFirstFrame = false;
		r.moveDiagonal = false;
		r.moveUpAndDown = false;
		r.moveLeftAndRightAllow = false;
		r.moveLeftAndRightUsePreviousInput = true;

		r.lineFallAnim = false;
		r.lineCancelMove = true;
		r.lineCancelRotate = true;
		r.lineCancelHold = true;

		r.skin = 4;
		r.ghost = false;

		for (int i = 0; i < Piece.PIECE_COUNT; i++) {
			r.pieceColor[i] = (i % Block.BLOCK_COLOR_COUNT) + 1;
		}
		return r;
	}

	@Test
	void defaultConstructorAppliesKnownDefaults() {
		RuleOptions r = new RuleOptions();

		assertEquals("", r.strRuleName);
		assertEquals("", r.strWallkick);
		assertEquals("", r.strRandomizer);
		assertEquals(0, r.style);
		assertTrue(r.holdEnable);
		assertEquals(-1, r.holdLimit);
		assertEquals(0.5f, r.softdropSpeed);
		assertEquals(RuleOptions.LOCKRESET_LIMIT_OVER_INSTANT, r.lockresetLimitOver);
		assertEquals(15, r.lockresetLimitMove);
		assertEquals(15, r.lockresetLimitRotate);
		assertTrue(r.ghost);
		assertEquals(Piece.PIECE_COUNT, r.pieceColor.length);
		assertEquals(Block.BLOCK_COLOR_GRAY, r.pieceColor[Piece.PIECE_I]);
		assertEquals(Block.BLOCK_COLOR_PURPLE, r.pieceColor[Piece.PIECE_I1]);
		assertEquals(Block.BLOCK_COLOR_BLUE, r.pieceColor[Piece.PIECE_I2]);
		assertEquals(Block.BLOCK_COLOR_GREEN, r.pieceColor[Piece.PIECE_I3]);
		assertEquals(Block.BLOCK_COLOR_ORANGE, r.pieceColor[Piece.PIECE_L3]);
		assertEquals(Piece.PIECE_COUNT, r.pieceOffsetX.length);
		assertEquals(Piece.DIRECTION_COUNT, r.pieceOffsetX[0].length);
	}

	@Test
	void copyConstructorReproducesEverythingAndDoesNotShareArrays() {
		RuleOptions src = populated();

		RuleOptions copy = new RuleOptions(src);

		assertNotSame(src, copy);
		assertNotSame(src.pieceOffsetX, copy.pieceOffsetX);
		assertNotSame(src.pieceColor, copy.pieceColor);
		assertTrue(src.compare(copy, false));
		assertTrue(src.compare(copy, true));

		src.pieceOffsetX[0][0] = 999;
		src.pieceColor[0] = Block.BLOCK_COLOR_RAINBOW;
		assertEquals(populated().pieceOffsetX[0][0], copy.pieceOffsetX[0][0]);
		assertEquals(populated().pieceColor[0], copy.pieceColor[0]);
	}

	@Test
	void compareDetectsScalarFieldDifferences() {
		assertCompareFalseAfter(r -> r.strWallkick = r.strWallkick + "_x");
		assertCompareFalseAfter(r -> r.strRandomizer = r.strRandomizer + "_x");
		assertCompareFalseAfter(r -> r.style += 1);

		assertCompareFalseAfter(r -> r.pieceEnterAboveField = !r.pieceEnterAboveField);
		assertCompareFalseAfter(r -> r.pieceEnterMaxDistanceY += 1);

		assertCompareFalseAfter(r -> r.fieldWidth += 1);
		assertCompareFalseAfter(r -> r.fieldHeight += 1);
		assertCompareFalseAfter(r -> r.fieldHiddenHeight += 1);
		assertCompareFalseAfter(r -> r.fieldCeiling = !r.fieldCeiling);
		assertCompareFalseAfter(r -> r.fieldLockoutDeath = !r.fieldLockoutDeath);
		assertCompareFalseAfter(r -> r.fieldPartialLockoutDeath = !r.fieldPartialLockoutDeath);

		assertCompareFalseAfter(r -> r.nextDisplay += 1);

		assertCompareFalseAfter(r -> r.holdEnable = !r.holdEnable);
		assertCompareFalseAfter(r -> r.holdInitial = !r.holdInitial);
		assertCompareFalseAfter(r -> r.holdInitialLimit = !r.holdInitialLimit);
		assertCompareFalseAfter(r -> r.holdResetDirection = !r.holdResetDirection);
		assertCompareFalseAfter(r -> r.holdLimit += 1);

		assertCompareFalseAfter(r -> r.harddropEnable = !r.harddropEnable);
		assertCompareFalseAfter(r -> r.harddropLock = !r.harddropLock);
		assertCompareFalseAfter(r -> r.harddropLimit = !r.harddropLimit);

		assertCompareFalseAfter(r -> r.softdropEnable = !r.softdropEnable);
		assertCompareFalseAfter(r -> r.softdropLock = !r.softdropLock);
		assertCompareFalseAfter(r -> r.softdropLimit = !r.softdropLimit);
		assertCompareFalseAfter(r -> r.softdropSurfaceLock = !r.softdropSurfaceLock);
		assertCompareFalseAfter(r -> r.softdropSpeed += 0.25f);
		assertCompareFalseAfter(r -> r.softdropMultiplyNativeSpeed = !r.softdropMultiplyNativeSpeed);
		assertCompareFalseAfter(r -> r.softdropGravitySpeedLimit = !r.softdropGravitySpeedLimit);

		assertCompareFalseAfter(r -> r.rotateInitial = !r.rotateInitial);
		assertCompareFalseAfter(r -> r.rotateInitialLimit = !r.rotateInitialLimit);
		assertCompareFalseAfter(r -> r.rotateWallkick = !r.rotateWallkick);
		assertCompareFalseAfter(r -> r.rotateInitialWallkick = !r.rotateInitialWallkick);
		assertCompareFalseAfter(r -> r.rotateMaxUpwardWallkick += 1);
		assertCompareFalseAfter(r -> r.rotateButtonDefaultRight = !r.rotateButtonDefaultRight);
		assertCompareFalseAfter(r -> r.rotateButtonAllowReverse = !r.rotateButtonAllowReverse);
		assertCompareFalseAfter(r -> r.rotateButtonAllowDouble = !r.rotateButtonAllowDouble);

		assertCompareFalseAfter(r -> r.lockresetFall = !r.lockresetFall);
		assertCompareFalseAfter(r -> r.lockresetMove = !r.lockresetMove);
		assertCompareFalseAfter(r -> r.lockresetRotate = !r.lockresetRotate);
		assertCompareFalseAfter(r -> r.lockresetWallkick = !r.lockresetWallkick);
		assertCompareFalseAfter(r -> r.lockresetLimitMove += 1);
		assertCompareFalseAfter(r -> r.lockresetLimitRotate += 1);
		assertCompareFalseAfter(r -> r.lockresetLimitShareCount = !r.lockresetLimitShareCount);
		assertCompareFalseAfter(r -> r.lockresetLimitOver += 1);

		assertCompareFalseAfter(r -> r.lockflash += 1);
		assertCompareFalseAfter(r -> r.lockflashOnlyFrame = !r.lockflashOnlyFrame);
		assertCompareFalseAfter(r -> r.lockflashBeforeLineClear = !r.lockflashBeforeLineClear);
		assertCompareFalseAfter(r -> r.areCancelMove = !r.areCancelMove);
		assertCompareFalseAfter(r -> r.areCancelRotate = !r.areCancelRotate);
		assertCompareFalseAfter(r -> r.areCancelHold = !r.areCancelHold);

		assertCompareFalseAfter(r -> r.minARE += 1);
		assertCompareFalseAfter(r -> r.maxARE += 1);
		assertCompareFalseAfter(r -> r.minARELine += 1);
		assertCompareFalseAfter(r -> r.maxARELine += 1);
		assertCompareFalseAfter(r -> r.minLineDelay += 1);
		assertCompareFalseAfter(r -> r.maxLineDelay += 1);
		assertCompareFalseAfter(r -> r.minLockDelay += 1);
		assertCompareFalseAfter(r -> r.maxLockDelay += 1);
		assertCompareFalseAfter(r -> r.minDAS += 1);
		assertCompareFalseAfter(r -> r.maxDAS += 1);

		assertCompareFalseAfter(r -> r.dasDelay += 1);
		assertCompareFalseAfter(r -> r.shiftLockEnable = !r.shiftLockEnable);

		assertCompareFalseAfter(r -> r.dasInReady = !r.dasInReady);
		assertCompareFalseAfter(r -> r.dasInMoveFirstFrame = !r.dasInMoveFirstFrame);
		assertCompareFalseAfter(r -> r.dasInLockFlash = !r.dasInLockFlash);
		assertCompareFalseAfter(r -> r.dasInLineClear = !r.dasInLineClear);
		assertCompareFalseAfter(r -> r.dasInARE = !r.dasInARE);
		assertCompareFalseAfter(r -> r.dasInARELastFrame = !r.dasInARELastFrame);
		assertCompareFalseAfter(r -> r.dasInEndingStart = !r.dasInEndingStart);
		assertCompareFalseAfter(r -> r.dasChargeOnBlockedMove = !r.dasChargeOnBlockedMove);
		assertCompareFalseAfter(r -> r.dasStoreChargeOnNeutral = !r.dasStoreChargeOnNeutral);
		assertCompareFalseAfter(r -> r.dasRedirectInDelay = !r.dasRedirectInDelay);

		assertCompareFalseAfter(r -> r.moveFirstFrame = !r.moveFirstFrame);
		assertCompareFalseAfter(r -> r.moveDiagonal = !r.moveDiagonal);
		assertCompareFalseAfter(r -> r.moveUpAndDown = !r.moveUpAndDown);
		assertCompareFalseAfter(r -> r.moveLeftAndRightAllow = !r.moveLeftAndRightAllow);
		assertCompareFalseAfter(r -> r.moveLeftAndRightUsePreviousInput = !r.moveLeftAndRightUsePreviousInput);

		assertCompareFalseAfter(r -> r.lineCancelMove = !r.lineCancelMove);
		assertCompareFalseAfter(r -> r.lineCancelRotate = !r.lineCancelRotate);
		assertCompareFalseAfter(r -> r.lineCancelHold = !r.lineCancelHold);

		assertCompareFalseAfter(r -> r.ghost = !r.ghost);
	}

	@Test
	void compareDetectsArrayElementDifferences() {
		assertCompareFalseAfter(r -> r.pieceOffsetX[0][0] += 1);
		assertCompareFalseAfter(r -> r.pieceOffsetY[0][0] += 1);
		assertCompareFalseAfter(r -> r.pieceSpawnX[0][0] += 1);
		assertCompareFalseAfter(r -> r.pieceSpawnY[0][0] += 1);
		assertCompareFalseAfter(r -> r.pieceSpawnXBig[0][0] += 1);
		assertCompareFalseAfter(r -> r.pieceSpawnYBig[0][0] += 1);
		assertCompareFalseAfter(r -> r.pieceDefaultDirection[0] = (r.pieceDefaultDirection[0] + 1) % Piece.DIRECTION_COUNT);
	}

	@Test
	void ignoreGraphicsSettingMasksRuleNameColorAnimAndSkin() {
		RuleOptions src = populated();
		RuleOptions dst = new RuleOptions(src);

		dst.strRuleName = src.strRuleName + "_diff";
		dst.pieceColor[0] = src.pieceColor[0] + 1;
		dst.lineFallAnim = !src.lineFallAnim;
		dst.skin = src.skin + 1;

		assertTrue(src.compare(dst, true), "permissive compare should ignore graphics-only fields");
		assertFalse(src.compare(dst, false), "strict compare should fail when any graphics field differs");
	}

	@Test
	void compareTreatsNullStringEqualToNullStringButNotToValue() {
		RuleOptions src = populated();
		src.strWallkick = null;
		RuleOptions dst = new RuleOptions(src);
		dst.strWallkick = null;
		assertTrue(src.compare(dst, false));

		dst.strWallkick = "non-null";
		assertFalse(src.compare(dst, false));
	}

	@Test
	void writePropertyAndReadPropertyRoundTrip() {
		RuleOptions src = populated();
		CustomProperties p = new CustomProperties();
		src.writeProperty(p, 7);

		RuleOptions roundtrip = new RuleOptions();
		roundtrip.readProperty(p, 7);

		assertTrue(src.compare(roundtrip, false), "round-tripped rule options should compare equal under strict mode");
	}

	private static void assertCompareFalseAfter(Consumer<RuleOptions> mutator) {
		RuleOptions src = populated();
		RuleOptions dst = new RuleOptions(src);
		mutator.accept(dst);
		assertFalse(src.compare(dst, false), "compare(_, false) should detect mutated field");
	}
}

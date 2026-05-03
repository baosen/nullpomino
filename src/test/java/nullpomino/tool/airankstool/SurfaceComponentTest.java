package nullpomino.tool.airankstool;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;

import org.junit.jupiter.api.Test;

/**
 * Tests SurfaceComponent's non-rendering logic: surface decoding in
 * setSurface and preferred-size computation.
 *
 * <p>SurfaceComponent extends JComponent and does not need a display
 * for construction or for setSurface (which decodes the encoded surface
 * value into column-height differences, tracking min/max height).
 */
class SurfaceComponentTest {

	@Test
	void setSurfaceDecodesBasicSurface() {
		// maxJump=4, stackWidth=9, base=9, surfaceWidth=8
		// Surface encoding: each digit in base-9, offset by -maxJump
		// surface=0 → all digits are -maxJump = -4
		SurfaceComponent sc = new SurfaceComponent(4, 9, 0);

		// After construction, setSurface(0) was called.
		// Verify the component initialised without error by checking preferred size.
		assertTrue(sc.getPreferredSize().width > 0);
	}

	@Test
	void setSurfaceWithNonZeroValueProducesPreferredSizeInExpectedRange() {
		// maxJump=2, stackWidth=4, base=5, surfaceWidth=3
		// Component height = maxJump * surfaceWidth = 2 * 3 = 6
		// baseSizeX=10, baseSizeY=10
		// Preferred width  = baseSizeX*(stackWidth+1)+2*baseSizeX = 10*5+20 = 70
		// Preferred height = componentHeight*baseSizeY+2*baseSizeY = 6*10+20 = 80
		SurfaceComponent sc = new SurfaceComponent(2, 4, 123);

		Dimension pref = sc.getPreferredSize();

		assertEquals(70, pref.width);
		assertEquals(80, pref.height);
	}

	@Test
	void getPreferredSizeScalesWithMaxJumpAndStackWidth() {
		// maxJump=4, stackWidth=9, base=9, surfaceWidth=8
		// componentHeight = 4 * 8 = 32
		// baseSizeX=10, baseSizeY=10
		// width  = 10*(9+1)+20 = 120
		// height = 32*10+20 = 340
		SurfaceComponent sc = new SurfaceComponent(4, 9, 0);

		Dimension pref = sc.getPreferredSize();

		assertEquals(120, pref.width);
		assertEquals(340, pref.height);
	}

	@Test
	void setSurfaceCanBeCalledMultipleTimes() {
		SurfaceComponent sc = new SurfaceComponent(2, 3, 0);

		// setSurface(0) sets all to -maxJump
		sc.setSurface(0);

		// setSurface(24) with maxJump=2, stackWidth=3:
		// base=5, surfaceWidth=2
		// 24 in base-5: digits = 24%5=4, 24/5=4 → [4,4]
		// offset by -maxJump: [2, 2]
		sc.setSurface(24);

		// setSurface(12) with maxJump=2:
		// 12 in base-5: digits = 12%5=2, 12/5=2 → [2,2]
		// offset by -maxJump: [0, 0]
		sc.setSurface(12);

		// Verify it didn't crash and produces stable preferred size
		// width = baseSizeX*(stackWidth+1)+2*baseSizeX = 10*(3+1)+20 = 60
		// height = componentHeight*baseSizeY+2*baseSizeY = 4*10+20 = 60
		assertEquals(60, sc.getPreferredSize().width);
		assertEquals(60, sc.getPreferredSize().height);
	}

	@Test
	void constructorWithMaxJump2StackWidth3CreatesCorrectPreferredSize() {
		// maxJump=2, stackWidth=3, base=5, surfaceWidth=2
		// componentHeight = 2 * 2 = 4
		// baseSizeX=10, baseSizeY=10
		// width  = 10*(3+1)+20 = 60
		// height = 4*10+20 = 60
		SurfaceComponent sc = new SurfaceComponent(2, 3, 0);

		Dimension pref = sc.getPreferredSize();

		assertEquals(60, pref.width);
		assertEquals(60, pref.height);
	}

	@Test
	void constructorDoesNotThrowForVariousSurfaceValues() {
		// The constructor calls setSurface(surface), which decodes
		// the encoded value. Verify it handles edge values.
		SurfaceComponent scMin = new SurfaceComponent(2, 3, 0);
		assertTrue(scMin.getPreferredSize().width > 0);

		SurfaceComponent scMid = new SurfaceComponent(2, 3, 12);
		assertTrue(scMid.getPreferredSize().width > 0);

		// size-1 = 5^2 - 1 = 24, the max encoded value
		SurfaceComponent scMax = new SurfaceComponent(2, 3, 24);
		assertTrue(scMax.getPreferredSize().width > 0);
	}
}

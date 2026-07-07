package nullpomino.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Vector;

import org.junit.jupiter.api.Test;

class SwingToolUtilTest {

	@Test
	void shortClassNamesStripPackagePrefix() {
		Vector<String> names = new Vector<String>();
		names.add("net.omegaboshi.nullpomino.game.subsystem.randomizer.BagRandomizer");
		names.add("MemorylessRandomizer");

		Vector<String> shortNames = SwingToolUtil.shortClassNames(names);

		assertEquals("BagRandomizer", shortNames.get(0));
		assertEquals("MemorylessRandomizer", shortNames.get(1));
	}
}

package nullpomino.game.subsystem.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import nullpomino.util.CustomProperties;

class ModeSettingsRoundTripTest {

	@Test
	void technicianCoreSettingsKeepLegacyKeys() {
		CustomProperties in = new CustomProperties();
		in.setProperty("technician.gametype", 4);
		in.setProperty("technician.startlevel", 7);
		in.setProperty("technician.tspinEnableType", 2);
		in.setProperty("technician.enableTSpin", false);
		in.setProperty("technician.enableTSpinKick", false);
		in.setProperty("technician.spinCheckType", 1);
		in.setProperty("technician.tspinEnableEZ", true);
		in.setProperty("technician.enableB2B", false);
		in.setProperty("technician.enableCombo", false);
		in.setProperty("technician.big", true);
		in.setProperty("technician.version", 2);

		TechnicianMode mode = new TechnicianMode();
		mode.loadSetting(in);

		CustomProperties out = new CustomProperties();
		mode.saveSetting(out);

		assertEquals(4, out.getProperty("technician.gametype", -1));
		assertEquals(7, out.getProperty("technician.startlevel", -1));
		assertEquals(2, out.getProperty("technician.tspinEnableType", -1));
		assertEquals(false, out.getProperty("technician.enableTSpin", true));
		assertEquals(false, out.getProperty("technician.enableTSpinKick", true));
		assertEquals(1, out.getProperty("technician.spinCheckType", -1));
		assertEquals(true, out.getProperty("technician.tspinEnableEZ", false));
		assertEquals(false, out.getProperty("technician.enableB2B", true));
		assertEquals(false, out.getProperty("technician.enableCombo", true));
		assertEquals(true, out.getProperty("technician.big", false));
		assertEquals(2, out.getProperty("technician.version", -1));
	}
}

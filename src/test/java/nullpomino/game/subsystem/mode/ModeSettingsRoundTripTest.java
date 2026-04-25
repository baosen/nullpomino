package nullpomino.game.subsystem.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import nullpomino.util.CustomProperties;

class ModeSettingsRoundTripTest {

	@Test
	void extremeCoreSettingsKeepLegacyKeys() {
		CustomProperties in = new CustomProperties();
		in.setProperty("extreme.startlevel", 12);
		in.setProperty("extreme.tspinEnableType", 2);
		in.setProperty("extreme.enableTSpin", false);
		in.setProperty("extreme.enableTSpinKick", false);
		in.setProperty("extreme.spinCheckType", 1);
		in.setProperty("extreme.tspinEnableEZ", true);
		in.setProperty("extreme.enableB2B", false);
		in.setProperty("extreme.enableCombo", false);
		in.setProperty("extreme.endless", true);
		in.setProperty("extreme.big", true);
		in.setProperty("extreme.version", 1);

		ExtremeMode mode = new ExtremeMode();
		mode.loadSetting(in);

		CustomProperties out = new CustomProperties();
		mode.saveSetting(out);

		assertEquals(12, out.getProperty("extreme.startlevel", -1));
		assertEquals(2, out.getProperty("extreme.tspinEnableType", -1));
		assertEquals(false, out.getProperty("extreme.enableTSpin", true));
		assertEquals(false, out.getProperty("extreme.enableTSpinKick", true));
		assertEquals(1, out.getProperty("extreme.spinCheckType", -1));
		assertEquals(true, out.getProperty("extreme.tspinEnableEZ", false));
		assertEquals(false, out.getProperty("extreme.enableB2B", true));
		assertEquals(false, out.getProperty("extreme.enableCombo", true));
		assertEquals(true, out.getProperty("extreme.endless", false));
		assertEquals(true, out.getProperty("extreme.big", false));
		assertEquals(1, out.getProperty("extreme.version", -1));
	}

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

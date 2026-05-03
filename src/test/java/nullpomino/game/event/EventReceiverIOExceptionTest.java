package nullpomino.game.event;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nullpomino.util.CustomProperties;

/**
 * Forces the IOException catch blocks in EventReceiver.loadModeConfig()
 * (lines 810-811) and saveModeConfig() (lines 822-823).
 *
 * Strategy:
 * <ul>
 *   <li>For loadModeConfig: temporarily rename config/setting/mode.cfg
 *       so that FileInputStream throws FileNotFoundException.</li>
 *   <li>For saveModeConfig: temporarily rename config/setting/ directory
 *       so that FileOutputStream cannot create the file.</li>
 * </ul>
 */
class EventReceiverIOExceptionTest {

	@TempDir
	Path tempDir;

	@Test
	void loadModeConfigReturnsNullWhenConfigFileIsMissing() throws IOException {
		Path configPath = Path.of("config/setting/mode.cfg");
		if (!Files.isRegularFile(configPath)) {
			// File doesn't exist in this environment — natural IO error path.
			EventReceiver receiver = new EventReceiver();
			assertNull(receiver.loadModeConfig());
			return;
		}

		Path backup = configPath.resolveSibling("mode.cfg.bak");
		Files.move(configPath, backup);
		try {
			EventReceiver receiver = new EventReceiver();
			CustomProperties result = receiver.loadModeConfig();
			assertNull(result, "loadModeConfig should return null when file is missing");
		} finally {
			Files.move(backup, configPath);
		}
	}

	@Test
	void saveModeConfigSwallowsIOExceptionWhenConfigDirIsMissing() throws IOException {
		Path configDir = Path.of("config/setting");
		if (!Files.isDirectory(configDir)) {
			// Directory doesn't exist — natural IO error path.
			EventReceiver receiver = new EventReceiver();
			CustomProperties props = new CustomProperties();
			props.setProperty("test", "value");
			receiver.saveModeConfig(props);
			return;
		}

		// Move config/setting/ aside temporarily
		Path backupDir = configDir.resolveSibling("setting.bak");
		Files.move(configDir, backupDir);
		try {
			EventReceiver receiver = new EventReceiver();
			CustomProperties props = new CustomProperties();
			props.setProperty("test", "value");
			receiver.saveModeConfig(props);
			// Catch block swallows the IOException silently (apart from log message).
		} finally {
			Files.move(backupDir, configDir);
		}
	}
}

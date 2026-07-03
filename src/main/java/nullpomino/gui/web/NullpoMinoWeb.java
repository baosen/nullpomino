// SPDX-FileCopyrightText: 2026 baosen
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SdlBackend;
import nullpomino.gui.sdl.binding.web.Java2DBackend;
import nullpomino.util.CustomProperties;
import nullpomino.util.LogConfig;

/**
 * Browser (CheerpJ) entry point.
 *
 * Selects the pure-Java2D SDL backend, then prepares the writable working
 * directory before delegating to the regular SDL main. Under CheerpJ the
 * working directory is set to a persistent IndexedDB mount (user.dir set via
 * cheerpjInit), while read-only assets stay on the HTTP-backed /app mount:
 *
 * <ul>
 *   <li>config/** is seeded from defaults packaged inside the jar, because
 *       the game both writes settings there and enumerates directories
 *       (config/rule), neither of which the HTTP mount supports.
 *       config/setting/* is copy-if-missing so user settings, keymaps and
 *       rankings survive; everything else is refreshed on every launch so a
 *       new jar ships new defaults.</li>
 *   <li>res/** (17 MB of graphics/sounds) is NOT copied: the skin directory
 *       is pointed at the /app mount (system property nullpomino.resdir) and
 *       fetched lazily per file over HTTP.</li>
 * </ul>
 *
 * Also runnable on a desktop JVM for debugging: without nullpomino.resdir the
 * skin directory stays the working-directory-relative default.
 */
public class NullpoMinoWeb {

	/** Jar resource listing every default config file, one path per line. */
	private static final String MANIFEST_RESOURCE = "/config-manifest.txt";

	public static void main(String[] args) throws IOException {
		// Must precede any reference to NullpoMinoSDL (webMode is read at
		// class initialization) and any binding interface (backend selection).
		System.setProperty("nullpomino.web", "true");
		SdlBackend.set(new Java2DBackend());

		// Console-only logging from a packaged resource; the config tree on
		// the virtual filesystem is not consulted. CheerpJ maps stdout/stderr
		// to the browser console.
		LogConfig.configureFromResource("/web/logback-web.xml");

		seedDefaultFiles();
		configureSkinDirectory();

		NullpoMinoSDL.main(args);
	}

	/**
	 * Copy the packaged default config tree into the (persistent) working
	 * directory. User data under config/setting/ is never overwritten.
	 */
	private static void seedDefaultFiles() throws IOException {
		int seeded = 0;
		try(InputStream manifest = NullpoMinoWeb.class.getResourceAsStream(MANIFEST_RESOURCE)) {
			if(manifest == null) {
				System.err.println("NullpoMinoWeb: " + MANIFEST_RESOURCE + " missing; skipping config seeding");
				return;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(manifest, StandardCharsets.UTF_8));
			String path;
			while((path = reader.readLine()) != null) {
				if(path.isEmpty()) continue;
				File dst = new File(path);
				boolean userData = path.startsWith("config/setting/");
				if(userData && dst.exists()) continue;

				File parent = dst.getParentFile();
				if(parent != null) parent.mkdirs();
				try(InputStream in = NullpoMinoWeb.class.getResourceAsStream("/" + path)) {
					if(in == null) continue;
					try(OutputStream out = new FileOutputStream(dst)) {
						in.transferTo(out);
					}
				}
				seeded++;
			}
		}
		new File("replay").mkdirs();
		System.out.println("NullpoMinoWeb: seeded " + seeded + " config files");
	}

	/**
	 * Point the skin directory at the read-only asset mount. Written into the
	 * frontend config on every launch so a relocated deployment (different
	 * base path) heals itself.
	 */
	private static void configureSkinDirectory() {
		String resdir = System.getProperty("nullpomino.resdir");
		if(resdir == null || resdir.isEmpty()) return;

		String configFile = "config/setting/sdl.cfg";
		CustomProperties prop = CustomProperties.loadFromFileOrEmpty(configFile);
		if(resdir.equals(prop.getProperty("custom.skin.directory"))) return;
		prop.setProperty("custom.skin.directory", resdir);
		try {
			prop.storeToFile(configFile, "NullpoMino SDL-frontend Config");
		} catch(IOException e) {
			System.err.println("NullpoMinoWeb: failed to persist skin directory: " + e);
		}
	}
}

package nullpomino.gui.teavm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.teavm.jso.JSBody;

import nullpomino.gui.sdl.binding.teavm.JsAsync;
import nullpomino.util.CustomProperties;
import nullpomino.util.DataDir;

/**
 * Persistence and asset seeding for the browser build.
 *
 * TeaVM's {@code java.io.File} is backed by an in-memory virtual filesystem
 * that does not survive a reload, and its assets are plain HTTP files rather
 * than a mounted tree. So at startup this:
 * <ul>
 *   <li>restores previously written config/replays from {@code localStorage}
 *       into the VFS;</li>
 *   <li>seeds the default config tree by fetching it over HTTP (copy-if-missing
 *       for {@code config/setting/}, so restored user data wins);</li>
 *   <li>creates empty marker files for every {@code res/} asset so the
 *       frontend's {@code File.canRead()} skin probes succeed (real bytes are
 *       fetched over HTTP by the backend on demand);</li>
 *   <li>installs a {@link CustomProperties#storeListener} that mirrors every
 *       written file back into {@code localStorage}.</li>
 * </ul>
 */
public final class WebFiles {
	private WebFiles() {}

	private static final String LS_PREFIX = "nullpomino.fs:";

	/** Restore files previously mirrored into localStorage into the VFS. */
	public static void restoreFromLocalStorage() {
		int n = lsLength();
		int restored = 0;
		for (int i = 0; i < n; i++) {
			String key = lsKey(i);
			if (key == null || !key.startsWith(LS_PREFIX)) continue;
			String path = key.substring(LS_PREFIX.length());
			String value = lsGet(key);
			if (value == null) continue;
			try {
				writeFile(DataDir.file(path), value.getBytes(StandardCharsets.ISO_8859_1));
				restored++;
			} catch (IOException e) {
				System.err.println("WebFiles: failed to restore " + path + ": " + e);
			}
		}
		System.out.println("WebFiles: restored " + restored + " persisted files");
	}

	/** Fetch and write the default config tree (copy-if-missing for user data). */
	public static void seedDefaults() {
		String manifest = JsAsync.fetchText("config-manifest.txt");
		if (manifest == null) {
			System.err.println("WebFiles: config-manifest.txt missing; skipping config seeding");
			return;
		}
		int seeded = 0;
		for (String path : manifest.split("\n")) {
			path = path.trim();
			if (path.isEmpty()) continue;
			File dst = DataDir.file(path);
			boolean userData = path.startsWith("config/setting/");
			if (userData && dst.exists()) continue;
			byte[] bytes = JsAsync.fetchBytes(path);
			if (bytes == null) continue;
			try {
				writeFile(dst, bytes);
				seeded++;
			} catch (IOException e) {
				System.err.println("WebFiles: failed to seed " + path + ": " + e);
			}
		}
		DataDir.file("replay").mkdirs();
		System.out.println("WebFiles: seeded " + seeded + " config files");
	}

	/**
	 * Create empty marker files for every res/ asset so {@code File.canRead()}
	 * skin probes succeed; the real bytes are fetched over HTTP on demand.
	 */
	public static void seedResMarkers() {
		String manifest = JsAsync.fetchText("res-manifest.txt");
		if (manifest == null) {
			System.err.println("WebFiles: res-manifest.txt missing; skin probes may fail");
			return;
		}
		int markers = 0;
		for (String path : manifest.split("\n")) {
			path = path.trim();
			if (path.isEmpty()) continue;
			File marker = new File(path);
			if (marker.exists()) continue;
			try {
				writeFile(marker, new byte[0]);
				markers++;
			} catch (IOException e) {
				System.err.println("WebFiles: failed to mark " + path + ": " + e);
			}
		}
		System.out.println("WebFiles: created " + markers + " res markers");
	}

	/** Install the storeListener that mirrors written files into localStorage. */
	public static void installStoreMirror() {
		CustomProperties.storeListener = WebFiles::mirrorToLocalStorage;
	}

	private static void mirrorToLocalStorage(String filename) {
		try {
			byte[] bytes = readFile(DataDir.file(filename));
			lsSet(LS_PREFIX + filename, new String(bytes, StandardCharsets.ISO_8859_1));
		} catch (IOException e) {
			System.err.println("WebFiles: failed to mirror " + filename + ": " + e);
		}
	}

	private static void writeFile(File dst, byte[] bytes) throws IOException {
		File parent = dst.getParentFile();
		if (parent != null) parent.mkdirs();
		try (FileOutputStream out = new FileOutputStream(dst)) {
			out.write(bytes);
		}
	}

	private static byte[] readFile(File src) throws IOException {
		byte[] buf = new byte[(int) src.length()];
		try (FileInputStream in = new FileInputStream(src)) {
			int off = 0;
			while (off < buf.length) {
				int n = in.read(buf, off, buf.length - off);
				if (n < 0) break;
				off += n;
			}
		}
		return buf;
	}

	@JSBody(params = {}, script = "return localStorage.length;")
	private static native int lsLength();

	@JSBody(params = {"i"}, script = "return localStorage.key(i);")
	private static native String lsKey(int i);

	@JSBody(params = {"k"}, script = "return localStorage.getItem(k);")
	private static native String lsGet(String k);

	@JSBody(params = {"k", "v"}, script = "try { localStorage.setItem(k, v); } catch (e) {}")
	private static native void lsSet(String k, String v);
}

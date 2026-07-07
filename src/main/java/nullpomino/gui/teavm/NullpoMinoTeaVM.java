package nullpomino.gui.teavm;

import java.util.function.Supplier;

import nullpomino.game.mode.GameMode;
import nullpomino.game.net.NetPlatform;
import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.teavm.net.MqttLoungeService;
import nullpomino.gui.teavm.net.WebNetDebug;
import nullpomino.gui.teavm.net.WebRoomNet;
import nullpomino.gui.sdl.binding.SdlBackend;
import nullpomino.gui.sdl.binding.teavm.TeaVMBackend;
import nullpomino.gui.sdl.binding.teavm.WebProgress;
import nullpomino.util.DataDir;
import nullpomino.util.FactoryDefaults;
import nullpomino.util.StandaloneModeRegistry;

/**
 * Browser (TeaVM) entry point.
 *
 * Runs the same SDL frontend as the desktop build, but on the Canvas2D/WebAudio
 * backend, with netplay over WebRTC DataChannels + an MQTT broker lounge
 * instead of TCP/UDP. It deliberately does NOT call {@link NullpoMinoSDL#main}
 * (that path pulls in logback configuration and the java.net LAN networking,
 * neither of which compiles under TeaVM); instead it reuses the shared
 * {@link NullpoMinoSDL#bootstrap}, installs the web netplay backend, and
 * registers modes and states before entering the game loop. The loop itself is
 * unchanged: TeaVM green threads make the blocking {@code Thread.sleep}-paced
 * loop and DOM event delivery coexist.
 */
public final class NullpoMinoTeaVM {
	private NullpoMinoTeaVM() {}

	public static void main(String[] args) {
		NullpoMinoSDL.webMode = true;
		DataDir.setRoot("/data");

		// Select the backend before any binding interface (or NullpoMinoSDL
		// init) touches SDL3.INSTANCE.
		SdlBackend.set(new TeaVMBackend());

		// Persistence + assets: restore prior state, seed defaults, expose the
		// res tree to File.canRead() probes, then mirror future writes.
		WebFiles.restoreFromLocalStorage();
		// Fetch both manifests (memoised) and set the loading-bar denominator
		// before any per-file fetch is counted; the download phase already
		// filled 0–30%, and these fetches happen while total is still 0.
		WebProgress.setTotal(WebFiles.configEntryCount() + WebFiles.resAssetCount());
		WebFiles.seedDefaults();
		WebFiles.seedResMarkers();
		WebFiles.installStoreMirror();

		// Reflection is dead under TeaVM; register constructor suppliers so
		// wallkicks/randomizers/AIs resolve without Class.forName.
		FactoryDefaults.installAll();

		// Netplay: WebRTC DataChannel mesh + MQTT lounge/signaling
		NetPlatform.install(new WebRoomNet(), new MqttLoungeService());
		WebNetDebug.install();

		NullpoMinoSDL.bootstrap(args);
		for (Supplier<? extends GameMode> supplier : StandaloneModeRegistry.suppliers()) {
			NullpoMinoSDL.modeManager.addMode(supplier.get());
		}
		NullpoMinoSDL.registerAllStates();

		try {
			NullpoMinoSDL.init();
			NullpoMinoSDL.run();
		} catch (Throwable e) {
			// A crash mid-load would leave the overlay spinning forever; surface it.
			WebProgress.showError("Failed to load — see console");
			System.err.println("Uncaught exception in web main: " + e);
			e.printStackTrace();
		}
	}
}

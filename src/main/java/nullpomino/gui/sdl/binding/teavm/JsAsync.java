package nullpomino.gui.sdl.binding.teavm;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.webaudio.AudioBuffer;
import org.teavm.jso.webaudio.AudioContext;

/**
 * Blocking bridges over the browser's asynchronous APIs, implemented with
 * TeaVM's {@code @Async}/{@link AsyncCallback} coroutine mechanism. The whole
 * game runs as a single TeaVM green thread; these methods suspend it until the
 * underlying JS callback fires, so the SDL backend can keep the synchronous
 * "load returns the asset" contract the frontend expects.
 */
public final class JsAsync {
	private JsAsync() {}

	/** Fetch a URL as a UTF-8 string; null on any non-200 or error. */
	public static String fetchText(String url) {
		byte[] bytes = fetchBytes(url);
		return bytes == null ? null : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
	}

	/** Fetch a URL as bytes over XHR; null on any non-200 or error. */
	@Async
	public static native byte[] fetchBytes(String url);

	private static void fetchBytes(String url, AsyncCallback<byte[]> callback) {
		XMLHttpRequest xhr = XMLHttpRequest.create();
		xhr.open("GET", url);
		xhr.setResponseType("arraybuffer");
		xhr.setOnReadyStateChange(() -> {
			if (xhr.getReadyState() != XMLHttpRequest.DONE) {
				return;
			}
			if (xhr.getStatus() != 200 || xhr.getResponse() == null) {
				callback.complete(null);
				return;
			}
			Int8Array array = Int8Array.create((ArrayBuffer) xhr.getResponse());
			byte[] bytes = new byte[array.getLength()];
			for (int i = 0; i < bytes.length; i++) {
				bytes[i] = array.get(i);
			}
			WebProgress.bump(url);
			callback.complete(bytes);
		});
		xhr.send();
	}

	/** Load an image element from a URL; null if it fails to load. */
	@Async
	static native HTMLImageElement fetchImage(String url);

	private static void fetchImage(String url, AsyncCallback<HTMLImageElement> callback) {
		HTMLImageElement img = (HTMLImageElement) Window.current().getDocument().createElement("img");
		img.listenLoad(e -> {
			WebProgress.bump(url);
			callback.complete(img);
		});
		img.addEventListener("error", e -> callback.complete(null));
		img.setSrc(url);
	}

	/** Decode compressed audio bytes into an AudioBuffer; null on failure. */
	@Async
	static native AudioBuffer decodeAudio(AudioContext ctx, ArrayBuffer data);

	private static void decodeAudio(AudioContext ctx, ArrayBuffer data, AsyncCallback<AudioBuffer> callback) {
		ctx.decodeAudioData(data, callback::complete, err -> callback.complete(null));
	}

	/** Register a @font-face from a URL under {@code family}; false on failure. */
	@Async
	static native boolean loadFont(String family, String url);

	private static void loadFont(String family, String url, AsyncCallback<Boolean> callback) {
		loadFontJs(family, url, callback::complete);
	}

	/** Wrap a byte[] as a JS ArrayBuffer (for decodeAudioData). */
	static ArrayBuffer toArrayBuffer(byte[] bytes) {
		Int8Array array = Int8Array.create(bytes.length);
		for (int i = 0; i < bytes.length; i++) {
			array.set(i, bytes[i]);
		}
		return array.getBuffer();
	}

	@JSFunctor
	interface BooleanCallback extends JSObject {
		void done(boolean ok);
	}

	@JSBody(params = {"family", "url", "cb"}, script =
		"var f = new FontFace(family, 'url(' + url + ')');"
		+ "f.load().then(function() { document.fonts.add(f); cb(true); },"
		+ "              function() { cb(false); });")
	static native void loadFontJs(String family, String url, BooleanCallback cb);

	@JSBody(params = {}, script = "return new (window.AudioContext || window.webkitAudioContext)();")
	static native AudioContext createAudioContext();
}

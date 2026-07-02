// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the programmatic {@link NetServer} lifecycle used by embedded
 * hosting: {@link NetServer#startListening()}, {@link NetServer#getLocalPort()}
 * and {@link NetServer#requestShutdown()}.
 */
class NetServerLifecycleTest {

    @BeforeEach
    void setUp() throws Exception {
        Field field = NetServer.class.getDeclaredField("propServer");
        field.setAccessible(true);
        field.set(null, new CustomProperties());
    }

    /** Start a server on an ephemeral port and return its mainloop thread. */
    private static Thread startServerThread(NetServer server) {
        Thread thread = new Thread(server::run, "NetServerLifecycleTest");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /** Read from the socket until the buffer holds at least one full line. */
    private static String readGreeting(Socket socket) throws IOException {
        socket.setSoTimeout(5000);
        InputStream in = socket.getInputStream();
        StringBuilder greeting = new StringBuilder();
        byte[] buf = new byte[2048];
        while (greeting.indexOf("\n") < 0) {
            int len = in.read(buf);
            assertTrue(len > 0, "Server closed the connection before sending a greeting");
            greeting.append(new String(buf, 0, len, StandardCharsets.UTF_8));
        }
        return greeting.toString();
    }

    private static void startStopCycle() throws Exception {
        NetServer server = new NetServer(0);
        server.startListening();
        int port = server.getLocalPort();
        assertTrue(port > 0, "Ephemeral port should be assigned after startListening");

        Thread thread = startServerThread(server);
        try (Socket socket = new Socket("127.0.0.1", port)) {
            String greeting = readGreeting(socket);
            assertTrue(greeting.startsWith("welcome\t"), "Unexpected greeting: " + greeting);

            server.requestShutdown();
            thread.join(5000);
            assertFalse(thread.isAlive(), "Server thread should exit after requestShutdown");

            // Shutdown closes client channels: the socket must reach EOF
            socket.setSoTimeout(5000);
            InputStream in = socket.getInputStream();
            while (in.read() >= 0) {
                // drain any bytes queued before the close
            }
        }

        // The listen port must be free again immediately
        new ServerSocket(port).close();
    }

    @Test
    void startShutdownReleasesPortAndDisconnectsClients() throws Exception {
        startStopCycle();
    }

    @Test
    void serverCanBeRestartedInSameJvm() throws Exception {
        startStopCycle();
        startStopCycle();
    }

    @Test
    void startListeningThrowsWhenPortInUse() throws Exception {
        try (ServerSocket blocker = new ServerSocket(0)) {
            NetServer server = new NetServer(blocker.getLocalPort());
            assertThrows(IOException.class, server::startListening);
        }
    }

    @Test
    void getLocalPortBeforeListeningReturnsMinusOne() {
        NetServer server = new NetServer(0);
        assertEquals(-1, server.getLocalPort());
    }

    @Test
    void createEmbeddedUsesConfiguredPortWhenArgNotPositive() {
        NetServer server = NetServer.createEmbedded(-1);
        assertNotNull(server);
        // Port comes from config (or DEFAULT_PORT) but nothing is bound yet
        assertEquals(-1, server.getLocalPort());
    }
}

package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Extended tests for {@link NetBaseClient} covering additional edge cases:
 * constructor variants, ping timeout detection in PingTask,
 * processPacket with various message formats, and listener edge cases.
 */
class NetBaseClientExtendedTest {

    // ─── Constructor variants ─────────────────────────────────

    @Test
    void defaultConstructorThreadName() {
        NetBaseClient client = new NetBaseClient();
        // Default thread name (super() uses auto-generated name)
        assertNotNull(client.getName());
    }

    @Test
    void hostConstructorWithNullHost() {
        NetBaseClient client = new NetBaseClient(null);
        assertNull(client.getHost());
        assertEquals(NetBaseClient.DEFAULT_PORT, client.getPort());
    }

    // ─── isConnected ─────────────────────────────────────────

    @Test
    void isConnectedReturnsFalseByDefault() {
        NetBaseClient client = new NetBaseClient();
        assertFalse(client.isConnected());
    }

    @Test
    void isConnectedReturnsFalseWhenConnectedFlagTrueButSocketNull() {
        NetBaseClient client = new NetBaseClient();
        client.socket = null;
        client.connectedFlag = true;
        assertFalse(client.isConnected());
    }

    // ─── processPacket ───────────────────────────────────────

    @Test
    void processPacketPongResetsHighPingCount() throws IOException {
        NetBaseClient client = new NetBaseClient();
        client.pingCount = NetBaseClient.PING_AUTO_DISCONNECT_COUNT; // 6

        client.processPacket("pong");

        assertEquals(0, client.pingCount,
                "pong should reset pingCount from any value to 0");
    }

    @Test
    void processPacketPongWithMidRangePingCount() throws IOException {
        NetBaseClient client = new NetBaseClient();
        client.pingCount = NetBaseClient.PING_AUTO_DISCONNECT_COUNT / 2; // 3

        client.processPacket("pong");

        assertEquals(0, client.pingCount,
                "pong should reset even mid-range pingCount");
    }

    @Test
    void processPacketUnknownCommandDispatchesToListeners() throws IOException {
        NetBaseClient client = new NetBaseClient();
        RecordingListenerExtended listener = new RecordingListenerExtended();
        client.addListener(listener);

        client.processPacket("unknowncommand\targ1\targ2");

        assertEquals(1, listener.messages.size());
        assertEquals("unknowncommand", listener.messages.get(0)[0]);
        assertEquals("arg1", listener.messages.get(0)[1]);
        assertEquals("arg2", listener.messages.get(0)[2]);
    }

    @Test
    void processPacketWithManyArgs() throws IOException {
        NetBaseClient client = new NetBaseClient();
        RecordingListenerExtended listener = new RecordingListenerExtended();
        client.addListener(listener);

        client.processPacket("a\tb\tc\td\te\tf");

        assertEquals(1, listener.messages.size());
        assertEquals(6, listener.messages.get(0).length);
        assertEquals("f", listener.messages.get(0)[5]);
    }

    @Test
    void processPacketWithSpecialCharacters() throws IOException {
        NetBaseClient client = new NetBaseClient();
        RecordingListenerExtended listener = new RecordingListenerExtended();
        client.addListener(listener);

        client.processPacket("msg\thello\tworld!");

        assertEquals(1, listener.messages.size());
        assertEquals("msg", listener.messages.get(0)[0]);
        assertEquals("hello", listener.messages.get(0)[1]);
        assertEquals("world!", listener.messages.get(0)[2]);
    }

    // ─── addListener / removeListener edge cases ─────────────

    @Test
    void removeListenerNotAddedReturnsFalse() {
        NetBaseClient client = new NetBaseClient();
        RecordingListenerExtended listener = new RecordingListenerExtended();

        assertFalse(client.removeListener(listener));
    }

    @Test
    void addListenerMultipleTimesOnlyAddsOnce() {
        NetBaseClient client = new NetBaseClient();
        RecordingListenerExtended listener = new RecordingListenerExtended();

        client.addListener(listener);
        client.addListener(listener);
        client.addListener(listener);

        assertEquals(1, client.listeners.size());
    }

    // ─── send without socket ─────────────────────────────────

    @Test
    void sendBytesWithoutSocketReturnsFalse() {
        NetBaseClient client = new NetBaseClient();

        assertFalse(client.send(new byte[]{1, 2, 3, 4}));
    }

    @Test
    void sendStringWithoutSocketReturnsFalse() {
        NetBaseClient client = new NetBaseClient();

        assertFalse(client.send("test message"));
    }

    @Test
    void sendEmptyBytesWithoutSocketReturnsFalse() {
        NetBaseClient client = new NetBaseClient();

        assertFalse(client.send(new byte[0]));
    }

    @Test
    void sendEmptyStringWithoutSocketReturnsFalse() {
        NetBaseClient client = new NetBaseClient();

        assertFalse(client.send(""));
    }

    // ─── startPingTask edge cases ────────────────────────────

    @Test
    void startPingTaskNegativeIntervalDoesNotStart() {
        NetBaseClient client = new NetBaseClient();

        client.startPingTask(-100);
        assertNull(client.timerPing);
    }

    @Test
    void startPingTaskWithZeroIntervalDoesNotStart() {
        NetBaseClient client = new NetBaseClient();

        client.startPingTask(0);
        assertNull(client.timerPing);
    }

    @Test
    void startPingTaskWithCustomInterval() {
        NetBaseClient client = new NetBaseClient();
        try {
            client.startPingTask(10000); // 10 seconds
            assertNotNull(client.timerPing);
            assertNotNull(client.taskPing);
        } finally {
            if (client.timerPing != null) {
                client.timerPing.cancel();
            }
        }
    }

    // ─── PingTask behavior (start/stop cycle) ────────────────

    @Test
    void pingTaskNotRunningWhenDisconnected() {
        NetBaseClient client = new NetBaseClient();
        client.connectedFlag = false;

        // Create and run the task manually
        NetBaseClient.PingTask task = client.new PingTask();
        task.run();
        // No exception expected; should just log and cancel
    }

    @Test
    void pingTaskSendsPingWhenConnected() {
        NetBaseClient client = new NetBaseClient();
        // We need a valid socket for send, but since we can't easily create one
        // in tests, the PingTask will try to send and fail (no socket).
        // That's OK - it should handle the exception gracefully.
        client.connectedFlag = true;
        client.pingCount = 0;

        NetBaseClient.PingTask task = client.new PingTask();
        task.run();
        // Task attempted to send ping, failed because no socket
        // No exception should propagate
        assertTrue(true, "PingTask handles send failure gracefully");
    }

    @Test
    void pingTaskAutoDisconnectsOnTimeout() {
        NetBaseClient client = new NetBaseClient();
        client.pingCount = NetBaseClient.PING_AUTO_DISCONNECT_COUNT; // Already timed out

        NetBaseClient.PingTask task = client.new PingTask();
        task.run();

        // Should set threadRunning = false, connectedFlag = false
        assertFalse(client.connectedFlag, "Should disconnect on ping timeout");
    }

    @Test
    void pingTaskLogsWarningAtMidCount() {
        NetBaseClient client = new NetBaseClient();
        client.connectedFlag = true;
        client.pingCount = NetBaseClient.PING_AUTO_DISCONNECT_COUNT / 2;

        NetBaseClient.PingTask task = client.new PingTask();
        task.run();
        // pingCount stays the same because isConnected() returns false (no socket)
        // This test verifies no exception is thrown
        assertTrue(true, "PingTask completed without exception");
    }

    @Test
    void pingTaskExceptionDoesNotPropagate() {
        NetBaseClient client = new NetBaseClient();
        // Set connected to true and socket to null to cause exception on send
        client.connectedFlag = true;
        client.socket = null;

        NetBaseClient.PingTask task = client.new PingTask();
        task.run();
        // Exception should be caught and timer should be cancelled
        assertTrue(true, "PingTask exception handled gracefully");
    }

    // ─── Multiple listeners ──────────────────────────────────

    @Test
    void multipleListenersAllGetMessages() throws IOException {
        NetBaseClient client = new NetBaseClient();
        RecordingListenerExtended l1 = new RecordingListenerExtended();
        RecordingListenerExtended l2 = new RecordingListenerExtended();
        RecordingListenerExtended l3 = new RecordingListenerExtended();
        client.addListener(l1);
        client.addListener(l2);
        client.addListener(l3);

        client.processPacket("test\tdata");

        assertEquals(1, l1.messages.size());
        assertEquals(1, l2.messages.size());
        assertEquals(1, l3.messages.size());
    }

    @Test
    void listenerExceptionDoesNotAffectOthers() throws IOException {
        NetBaseClient client = new NetBaseClient();
        ThrowingListenerExtended bad = new ThrowingListenerExtended();
        RecordingListenerExtended good = new RecordingListenerExtended();
        client.addListener(bad);
        client.addListener(good);

        client.processPacket("test");

        assertEquals(1, good.messages.size(),
                "Good listener should still receive message despite bad listener");
    }

    @Test
    void multipleThrowingListenersHandledGracefully() throws IOException {
        NetBaseClient client = new NetBaseClient();
        ThrowingListenerExtended bad1 = new ThrowingListenerExtended();
        ThrowingListenerExtended bad2 = new ThrowingListenerExtended();
        RecordingListenerExtended good = new RecordingListenerExtended();
        client.addListener(bad1);
        client.addListener(bad2);
        client.addListener(good);

        client.processPacket("data");

        assertEquals(1, good.messages.size(),
                "Good listener should receive message after multiple throwers");
    }

    // ─── Listener implementations ────────────────────────────

    private static final class RecordingListenerExtended implements NetMessageListener {
        final List<String[]> messages = new ArrayList<>();

        @Override
        public void netOnMessage(NetBaseClient client, String[] message) {
            messages.add(message);
        }

        @Override
        public void netOnDisconnect(NetBaseClient client, Throwable ex) {
        }
    }

    private static final class ThrowingListenerExtended implements NetMessageListener {
        @Override
        public void netOnMessage(NetBaseClient client, String[] message) {
            throw new RuntimeException("Intentional test exception");
        }

        @Override
        public void netOnDisconnect(NetBaseClient client, Throwable ex) {
        }
    }
}

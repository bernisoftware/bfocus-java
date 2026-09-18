package br.com.bernisoftware.bfocus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Erro de rede ({@link NetworkException}): servidor fora do ar, conexão cortada, tempo esgotado. */
class NetworkTest {

    /** Porta local em que ninguém escuta. */
    private static int closedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    /**
     * Servidor TCP que lê a requisição (grava o {@code X-Request-Id}) e nunca responde: corta a conexão na hora
     * ({@code hangUp}) ou a deixa pendurada (tempo esgotado no cliente).
     */
    private static final class BlackHole implements AutoCloseable {
        final ServerSocket socket;
        final List<String> requestIds = Collections.synchronizedList(new ArrayList<>());
        final List<Socket> open = Collections.synchronizedList(new ArrayList<>());
        final boolean hangUp;

        BlackHole(boolean hangUp) throws IOException {
            this.hangUp = hangUp;
            this.socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            Thread acceptor = new Thread(this::acceptLoop, "black-hole");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        String baseUrl() {
            return "http://127.0.0.1:" + socket.getLocalPort();
        }

        private void acceptLoop() {
            while (!socket.isClosed()) {
                try {
                    Socket s = socket.accept();
                    open.add(s);
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.ISO_8859_1));
                    String line;
                    while ((line = in.readLine()) != null && !line.isEmpty()) {
                        if (line.toLowerCase(Locale.ROOT).startsWith("x-request-id:")) {
                            requestIds.add(line.substring("x-request-id:".length()).trim());
                        }
                    }
                    if (hangUp) {
                        s.close();
                    }
                } catch (IOException e) {
                    return;
                }
            }
        }

        @Override
        public void close() throws IOException {
            socket.close();
            synchronized (open) {
                for (Socket s : open) {
                    s.close();
                }
            }
        }
    }

    @Test
    void portaFechadaViraNetworkException() throws IOException {
        List<Duration> sleeps = new ArrayList<>();
        BfocusClient bf = BfocusClient.builder("bf_live_unit").baseUrl("http://127.0.0.1:" + closedPort())
                .timeout(Duration.ofSeconds(2)).build();
        bf.setSleeper(sleeps::add);
        NetworkException e = assertThrows(NetworkException.class, () -> bf.customers().get("C1"));
        assertInstanceOf(BfocusException.class, e);
        assertEquals(0, e.getStatus());
        assertEquals("NETWORK_ERROR", e.getCode());
        assertTrue(e.getMessage().contains("NETWORK_ERROR"), e.getMessage());
        assertTrue(e.getRequestId().matches("[0-9a-f]{32}"), e.getRequestId());
        assertEquals(2, sleeps.size(), "rede é repetida maxRetries vezes");
        assertTrue(sleeps.get(0).toMillis() >= 500 && sleeps.get(0).toMillis() <= 625, "backoff 1: " + sleeps.get(0));
        assertTrue(sleeps.get(1).toMillis() >= 1000 && sleeps.get(1).toMillis() <= 1250, "backoff 2: " + sleeps.get(1));

        BfocusClient noRetry = BfocusClient.builder("bf_live_unit").baseUrl("http://127.0.0.1:" + closedPort()).maxRetries(0).build();
        noRetry.setSleeper(d -> {
            throw new AssertionError("não devia esperar");
        });
        assertThrows(NetworkException.class, () -> noRetry.products().list());
    }

    @Test
    void conexaoCortadaUsaORequestIdEnviado() throws IOException {
        try (BlackHole hole = new BlackHole(true)) {
            List<Duration> sleeps = new ArrayList<>();
            BfocusClient bf = BfocusClient.builder("bf_live_unit").baseUrl(hole.baseUrl()).maxRetries(1).build();
            bf.setSleeper(sleeps::add);
            NetworkException e = assertThrows(NetworkException.class, () -> bf.kb().articles().publish("git:x"));
            assertFalse(hole.requestIds.isEmpty());
            for (String sent : hole.requestIds) {
                assertEquals(sent, e.getRequestId(), "requestId do NetworkException = o X-Request-Id enviado (em todas as tentativas)");
            }
            assertEquals(1, sleeps.size());
        }
    }

    @Test
    void tempoEsgotadoPorChamadaVenceODoCliente() throws IOException {
        try (BlackHole hole = new BlackHole(false)) {
            BfocusClient bf = BfocusClient.builder("bf_live_unit").baseUrl(hole.baseUrl()).maxRetries(0).build();
            long started = System.nanoTime();
            NetworkException e = assertThrows(NetworkException.class,
                    () -> bf.customers().get("C1", RequestOptions.timeout(Duration.ofMillis(300))));
            assertTrue(Duration.ofNanos(System.nanoTime() - started).toMillis() < 5000, "timeout de 300 ms por chamada");
            assertInstanceOf(HttpTimeoutException.class, e.getCause());
            assertTrue(e.getMessage().contains("tempo esgotado"), e.getMessage());
        }
    }

    @Test
    void tempoEsgotadoRepeteOMesmoRequestId() throws Exception {
        try (BlackHole hole = new BlackHole(false)) {
            List<Duration> sleeps = new ArrayList<>();
            BfocusClient bf = BfocusClient.builder("bf_live_unit").baseUrl(hole.baseUrl())
                    .timeout(Duration.ofMillis(200)).maxRetries(1).build();
            bf.setSleeper(sleeps::add);
            NetworkException e = assertThrows(NetworkException.class, () -> bf.customers().get("C1"));
            long deadline = System.currentTimeMillis() + 2000;
            while (hole.requestIds.size() < 2 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            assertEquals(2, hole.requestIds.size());
            assertEquals(hole.requestIds.get(0), hole.requestIds.get(1), "nova tentativa repete o X-Request-Id");
            assertEquals(hole.requestIds.get(0), e.getRequestId());
            assertEquals(1, sleeps.size());
        }
    }
}

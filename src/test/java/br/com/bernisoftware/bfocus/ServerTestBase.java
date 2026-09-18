package br.com.bernisoftware.bfocus;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/** Base dos testes que falam com o {@link FakeServer}: um servidor por classe, esperas de retry capturadas. */
abstract class ServerTestBase {
    static FakeServer server;

    /** Esperas pedidas pela SDK entre tentativas (nenhuma é dormida de verdade). */
    final List<Duration> sleeps = new ArrayList<>();

    @BeforeAll
    static void startServer() throws IOException {
        server = new FakeServer();
    }

    @AfterAll
    static void stopServer() {
        server.close();
    }

    BfocusClient client(FakeServer.Reply... replies) {
        return client(builder -> { }, replies);
    }

    BfocusClient client(Consumer<BfocusClient.Builder> options, FakeServer.Reply... replies) {
        server.reset(Arrays.asList(replies));
        sleeps.clear();
        BfocusClient.Builder builder = BfocusClient.builder("bf_live_unit").baseUrl(server.baseUrl());
        options.accept(builder);
        BfocusClient bf = builder.build();
        bf.setSleeper(sleeps::add);
        return bf;
    }

    List<FakeServer.Recorded> requests() {
        return server.requests();
    }
}

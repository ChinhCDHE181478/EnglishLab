package fu.sep490.g23.backend.service.course;

import com.sun.net.httpserver.HttpServer;
import fu.sep490.g23.backend.service.course.impl.DictionaryTranslationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DictionaryTranslationServiceImplTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void translateWord_ReturnsVietnameseMeaningFromFreeApi() throws IOException {
        startServer(200, """
                {
                  "responseData": {"translatedText": "xin chào", "match": 0.99},
                  "quotaFinished": false,
                  "responseStatus": 200
                }
                """);

        assertEquals("xin chào", service().translateWord("hello"));
    }

    @Test
    void translateWord_RejectsExhaustedFreeQuota() throws IOException {
        startServer(200, """
                {
                  "responseData": {"translatedText": "MYMEMORY WARNING: YOU USED ALL AVAILABLE FREE TRANSLATIONS"},
                  "quotaFinished": true,
                  "responseStatus": 200
                }
                """);

        assertThrows(IllegalStateException.class, () -> service().translateWord("hello"));
    }

    private DictionaryTranslationServiceImpl service() {
        DictionaryTranslationServiceImpl service = new DictionaryTranslationServiceImpl(RestClient.builder());
        ReflectionTestUtils.setField(
                service,
                "translationBaseUrl",
                "http://localhost:" + server.getAddress().getPort()
        );
        return service;
    }

    private void startServer(int status, String responseBody) throws IOException {
        byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/get", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }
}

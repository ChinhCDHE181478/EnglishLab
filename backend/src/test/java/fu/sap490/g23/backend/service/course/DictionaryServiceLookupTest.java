package fu.sap490.g23.backend.service.course;

import com.sun.net.httpserver.HttpServer;
import fu.sap490.g23.backend.dto.response.course.DictionaryEntryResponse;
import fu.sap490.g23.backend.service.course.impl.DictionaryServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryServiceLookupTest {

    private static final String SUCCESS_RESPONSE = """
            [{
              "word": "hello",
              "phonetic": "/həˈləʊ/",
              "phonetics": [{"audio": "//audio.example/hello.mp3"}],
              "meanings": [{
                "partOfSpeech": "exclamation",
                "definitions": [{
                  "definition": "Used as a greeting.",
                  "example": "Hello, everyone."
                }],
                "synonyms": ["greeting"],
                "antonyms": []
              }]
            }]
            """;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void lookup_ParsesDictionaryResponseWithJackson2Model() throws IOException {
        startServer(200, SUCCESS_RESPONSE);

        DictionaryEntryResponse response = service().lookup(" Hello ");

        assertEquals("hello", response.getWord());
        assertEquals("/həˈləʊ/", response.getPhonetic());
        assertEquals("https://audio.example/hello.mp3", response.getAudioUrl());
        assertEquals("Used as a greeting.", response.getMeanings().getFirst().getDefinitions().getFirst().getDefinition());
        assertEquals("xin chào", response.getMeaningVietnamese());
        assertTrue(response.isVietnameseMeaningAvailable());
    }

    @Test
    void lookup_KeepsEnglishDefinitionsWhenTranslationFails() throws IOException {
        startServer(200, SUCCESS_RESPONSE);
        DictionaryTranslationService unavailableTranslation = word -> {
            throw new IllegalStateException("Translation unavailable");
        };

        DictionaryEntryResponse response = service(unavailableTranslation).lookup("hello");

        assertFalse(response.isVietnameseMeaningAvailable());
        assertEquals("Used as a greeting.", response.getMeanings().getFirst().getDefinitions().getFirst().getDefinition());
        assertNull(response.getMeaningVietnamese());
    }

    @Test
    void lookup_MapsNotFoundResponseToUserMessage() throws IOException {
        startServer(404, """
                {"title":"No Definitions Found"}
                """);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service().lookup("unknownword")
        );

        assertEquals("Không tìm thấy từ “unknownword” trong từ điển.", exception.getMessage());
    }

    private DictionaryServiceImpl service() {
        DictionaryTranslationService translationService = word -> "xin chào";
        return service(translationService);
    }

    private DictionaryServiceImpl service(DictionaryTranslationService translationService) {
        DictionaryServiceImpl service = new DictionaryServiceImpl(
                null,
                null,
                RestClient.builder(),
                translationService
        );
        ReflectionTestUtils.setField(
                service,
                "dictionaryBaseUrl",
                "http://localhost:" + server.getAddress().getPort() + "/api/v2"
        );
        return service;
    }

    private void startServer(int status, String responseBody) throws IOException {
        byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/v2/entries/en", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(status, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }
}

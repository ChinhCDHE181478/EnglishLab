package fu.sep490.g23.backend.service.curriculum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helpers for reading/writing typed fields stored in {@code content_bank_items.content_data}.
 */
public final class ContentBankPayloadSupport {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ContentBankPayloadSupport() {
    }

    public static Map<String, Object> ensure(Map<String, Object> payload) {
        return payload == null ? new LinkedHashMap<>() : payload;
    }

    public static String getString(Map<String, Object> payload, String key) {
        Object value = ensure(payload).get(key);
        return value == null ? null : String.valueOf(value);
    }

    public static Integer getInteger(Map<String, Object> payload, String key) {
        Object value = ensure(payload).get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static BigDecimal getBigDecimal(Map<String, Object> payload, String key) {
        Object value = ensure(payload).get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static void put(Map<String, Object> payload, String key, Object value) {
        Map<String, Object> target = ensure(payload);
        if (value == null) {
            target.remove(key);
        } else {
            target.put(key, value);
        }
    }

    public static String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể tuần tự hóa payload JSON.", exception);
        }
    }

    public static Object readJsonNode(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, Object.class);
        } catch (JsonProcessingException exception) {
            return json;
        }
    }

    public static List<Map<String, Object>> getObjectList(Map<String, Object> payload, String key) {
        Object value = ensure(payload).get(key);
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object element : list) {
                if (element instanceof Map<?, ?> map) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    map.forEach((k, v) -> copy.put(String.valueOf(k), v));
                    result.add(copy);
                } else {
                    result.add(MAPPER.convertValue(element, new TypeReference<>() {
                    }));
                }
            }
            return result;
        }
        return MAPPER.convertValue(value, new TypeReference<>() {
        });
    }

    public static String cardsJsonFromPayload(Map<String, Object> payload) {
        Object cards = ensure(payload).get("cards");
        if (cards == null) {
            return "[]";
        }
        if (cards instanceof String text) {
            return text.isBlank() ? "[]" : text;
        }
        return writeJson(cards);
    }

    public static Object cardsFromJson(String cardsJson) {
        if (cardsJson == null || cardsJson.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(cardsJson, Object.class);
        } catch (JsonProcessingException exception) {
            return List.of(cardsJson);
        }
    }
}

package fu.sep490.g23.backend.service.course.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.course.SaveVocabularyRequest;
import fu.sep490.g23.backend.dto.request.course.UpdateSavedVocabularyRequest;
import fu.sep490.g23.backend.dto.response.course.DictionaryEntryResponse;
import fu.sep490.g23.backend.dto.response.course.SavedVocabularyResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.SavedVocabulary;
import fu.sep490.g23.backend.entity.course.enums.VocabularyMasteryStatus;
import fu.sep490.g23.backend.repository.course.SavedVocabularyRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.course.DictionaryService;
import fu.sep490.g23.backend.service.course.DictionaryTranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DictionaryServiceImpl implements DictionaryService {

    private static final Pattern ENGLISH_WORD = Pattern.compile("^[A-Za-z][A-Za-z' -]{0,118}[A-Za-z]$|^[A-Za-z]$");
    private static final int MAX_MEANINGS = 6;
    private static final int MAX_DEFINITIONS_PER_MEANING = 5;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SavedVocabularyRepository savedVocabularyRepository;
    private final ClassroomAccessHelper accessHelper;
    private final RestClient.Builder restClientBuilder;
    private final DictionaryTranslationService dictionaryTranslationService;

    @Value("${englishlab.dictionary.base-url:https://api.dictionaryapi.dev/api/v2}")
    private String dictionaryBaseUrl;

    @Override
    @Transactional(readOnly = true)
    public DictionaryEntryResponse lookup(String word) {
        String normalizedWord = normalizeWord(word);
        try {
            String responseBody = restClientBuilder.clone().baseUrl(dictionaryBaseUrl).build()
                    .get()
                    .uri("/entries/en/{word}", normalizedWord)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new IllegalArgumentException("Không tìm thấy từ “" + normalizedWord + "” trong từ điển.");
                    })
                    .body(String.class);
            JsonNode payload = OBJECT_MAPPER.readTree(responseBody);
            DictionaryEntryResponse response = mapDictionaryPayload(payload, normalizedWord);
            enrichWithVietnameseMeaning(response);
            return response;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw new RuntimeException("Dịch vụ từ điển đang tạm thời không phản hồi. Hãy thử lại sau.", exception);
        } catch (RuntimeException exception) {
            throw new RuntimeException("Không thể tra cứu từ điển lúc này. Hãy kiểm tra kết nối và thử lại.", exception);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Dữ liệu trả về từ dịch vụ từ điển không hợp lệ. Hãy thử lại sau.", exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedVocabularyResponse> listSaved(
            String userEmail,
            String keyword,
            VocabularyMasteryStatus status
    ) {
        User user = accessHelper.requireUser(userEmail);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<SavedVocabulary> items = status == null
                ? savedVocabularyRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                : savedVocabularyRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(user.getId(), status);
        return items.stream()
                .filter(item -> normalizedKeyword.isBlank()
                        || item.getWord().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || item.getPrimaryDefinition().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || Optional.ofNullable(item.getNote()).orElse("").toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SavedVocabularyResponse> pageSaved(
            String userEmail,
            String keyword,
            VocabularyMasteryStatus status,
            Pageable pageable
    ) {
        User user = accessHelper.requireUser(userEmail);
        Specification<SavedVocabulary> specification = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user").get("id"), user.getId());
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (!normalizedKeyword.isBlank()) {
            String pattern = "%" + normalizedKeyword + "%";
            specification = specification.and((root, query, criteriaBuilder) -> criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("word")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("primaryDefinition")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("note"), "")), pattern)
            ));
        }
        return savedVocabularyRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getSavedStats(String userEmail) {
        User user = accessHelper.requireUser(userEmail);
        long total = savedVocabularyRepository.count((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user").get("id"), user.getId()));
        long mastered = savedVocabularyRepository.count((root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("user").get("id"), user.getId()),
                criteriaBuilder.equal(root.get("status"), VocabularyMasteryStatus.MASTERED)
        ));
        return Map.of("total", total, "mastered", mastered, "learning", total - mastered);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSaved(String userEmail, String word) {
        User user = accessHelper.requireUser(userEmail);
        return savedVocabularyRepository.findByUserIdAndWordIgnoreCase(user.getId(), normalizeWord(word)).isPresent();
    }

    @Override
    public SavedVocabularyResponse save(SaveVocabularyRequest request, String userEmail) {
        User user = accessHelper.requireUser(userEmail);
        String normalizedWord = normalizeWord(request.getWord());
        SavedVocabulary item = savedVocabularyRepository.findByUserIdAndWordIgnoreCase(user.getId(), normalizedWord)
                .orElseGet(() -> SavedVocabulary.builder()
                        .user(user)
                        .word(normalizedWord)
                        .status(VocabularyMasteryStatus.LEARNING)
                        .build());
        item.setPhonetic(cleanNullable(request.getPhonetic()));
        item.setPrimaryDefinition(request.getPrimaryDefinition().trim());
        item.setNote(cleanNullable(request.getNote()));
        return toResponse(savedVocabularyRepository.save(item));
    }

    @Override
    public SavedVocabularyResponse update(
            Long savedVocabularyId,
            UpdateSavedVocabularyRequest request,
            String userEmail
    ) {
        User user = accessHelper.requireUser(userEmail);
        SavedVocabulary item = requireOwned(savedVocabularyId, user);
        item.setNote(cleanNullable(request.getNote()));
        item.setStatus(request.getStatus());
        return toResponse(savedVocabularyRepository.save(item));
    }

    @Override
    public void delete(Long savedVocabularyId, String userEmail) {
        User user = accessHelper.requireUser(userEmail);
        savedVocabularyRepository.delete(requireOwned(savedVocabularyId, user));
    }

    private DictionaryEntryResponse mapDictionaryPayload(JsonNode payload, String fallbackWord) {
        if (payload == null || !payload.isArray() || payload.isEmpty()) {
            throw new IllegalArgumentException("Không tìm thấy từ “" + fallbackWord + "” trong từ điển.");
        }
        JsonNode entry = payload.get(0);
        String phonetic = text(entry, "phonetic");
        String audioUrl = null;
        JsonNode phonetics = entry.path("phonetics");
        if (phonetics.isArray()) {
            for (JsonNode item : phonetics) {
                if ((phonetic == null || phonetic.isBlank()) && hasText(item, "text")) {
                    phonetic = text(item, "text");
                }
                if (audioUrl == null && hasText(item, "audio")) {
                    audioUrl = normalizeAudioUrl(text(item, "audio"));
                }
            }
        }

        List<DictionaryEntryResponse.Meaning> meanings = new ArrayList<>();
        JsonNode meaningNodes = entry.path("meanings");
        if (meaningNodes.isArray()) {
            for (JsonNode meaningNode : meaningNodes) {
                if (meanings.size() >= MAX_MEANINGS) break;
                List<DictionaryEntryResponse.Definition> definitions = new ArrayList<>();
                JsonNode definitionNodes = meaningNode.path("definitions");
                if (definitionNodes.isArray()) {
                    for (JsonNode definitionNode : definitionNodes) {
                        if (definitions.size() >= MAX_DEFINITIONS_PER_MEANING) break;
                        if (!hasText(definitionNode, "definition")) continue;
                        definitions.add(DictionaryEntryResponse.Definition.builder()
                                .definition(text(definitionNode, "definition"))
                                .example(text(definitionNode, "example"))
                                .build());
                    }
                }
                if (!definitions.isEmpty()) {
                    meanings.add(DictionaryEntryResponse.Meaning.builder()
                            .partOfSpeech(text(meaningNode, "partOfSpeech"))
                            .definitions(definitions)
                            .synonyms(stringList(meaningNode.path("synonyms"), 12))
                            .antonyms(stringList(meaningNode.path("antonyms"), 12))
                            .build());
                }
            }
        }
        if (meanings.isEmpty()) {
            throw new IllegalArgumentException("Từ “" + fallbackWord + "” chưa có phần giải nghĩa phù hợp.");
        }
        return DictionaryEntryResponse.builder()
                .word(Optional.ofNullable(text(entry, "word")).orElse(fallbackWord))
                .phonetic(phonetic)
                .audioUrl(audioUrl)
                .meanings(meanings)
                .build();
    }

    private void enrichWithVietnameseMeaning(DictionaryEntryResponse response) {
        try {
            response.setMeaningVietnamese(dictionaryTranslationService.translateWord(response.getWord()));
            response.setVietnameseMeaningAvailable(true);
        } catch (RuntimeException exception) {
            log.warn("Không thể tải nghĩa tiếng Việt cho từ '{}': {}", response.getWord(), exception.getMessage());
            response.setVietnameseMeaningAvailable(false);
        }
    }

    private SavedVocabulary requireOwned(Long id, User user) {
        SavedVocabulary item = savedVocabularyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy từ đã lưu."));
        if (!item.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền thay đổi từ vựng này.");
        }
        return item;
    }

    private SavedVocabularyResponse toResponse(SavedVocabulary item) {
        return SavedVocabularyResponse.builder()
                .id(item.getId())
                .word(item.getWord())
                .phonetic(item.getPhonetic())
                .primaryDefinition(item.getPrimaryDefinition())
                .note(item.getNote())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private String normalizeWord(String value) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (!ENGLISH_WORD.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Chỉ có thể tra cứu từ hoặc cụm từ tiếng Anh hợp lệ.");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() || !value.isTextual() || value.asText().isBlank()
                ? null
                : value.asText().trim();
    }

    private boolean hasText(JsonNode node, String field) {
        return text(node, field) != null;
    }

    private List<String> stringList(JsonNode node, int limit) {
        if (node == null || !node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (values.size() >= limit) break;
            if (item.isTextual() && !item.asText().isBlank()) values.add(item.asText().trim());
        }
        return values.stream().distinct().toList();
    }

    private String normalizeAudioUrl(String value) {
        if (value == null || value.isBlank()) return null;
        return value.startsWith("//") ? "https:" + value : value;
    }
}

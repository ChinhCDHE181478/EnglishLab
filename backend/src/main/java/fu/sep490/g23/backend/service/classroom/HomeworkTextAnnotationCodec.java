package fu.sep490.g23.backend.service.classroom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.request.classroom.HomeworkTextAnnotationRequest;
import fu.sep490.g23.backend.dto.response.classroom.HomeworkTextAnnotationResponse;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkAnnotationType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class HomeworkTextAnnotationCodec {

    private static final int MAX_ANNOTATIONS = 100;
    private static final TypeReference<List<HomeworkTextAnnotationResponse>> RESPONSE_LIST_TYPE = new TypeReference<>() { };

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String validateAndSerialize(String submissionText, List<HomeworkTextAnnotationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return null;
        }
        if (submissionText == null || submissionText.isBlank()) {
            throw new IllegalArgumentException("Chỉ có thể ghi chú trên bài làm có nội dung văn bản.");
        }
        if (requests.size() > MAX_ANNOTATIONS) {
            throw new IllegalArgumentException("Mỗi bài làm chỉ được có tối đa 100 ghi chú.");
        }

        List<HomeworkTextAnnotationResponse> normalized = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (HomeworkTextAnnotationRequest request : requests) {
            validateRequiredFields(request);
            String id = request.getId().trim();
            if (!ids.add(id)) {
                throw new IllegalArgumentException("Mã ghi chú bị trùng lặp.");
            }
            int start = request.getStartOffset();
            int end = request.getEndOffset();
            if (start >= end || end > submissionText.length()) {
                throw new IllegalArgumentException("Vị trí đoạn được ghi chú không còn khớp với bài làm.");
            }
            String actualText = submissionText.substring(start, end);
            if (!actualText.equals(request.getSelectedText())) {
                throw new IllegalArgumentException("Nội dung đoạn được ghi chú đã thay đổi. Vui lòng chọn lại đoạn văn.");
            }

            String replacement = clean(request.getReplacementText());
            String note = clean(request.getNote());
            if (request.getType() == HomeworkAnnotationType.CORRECTION && replacement == null) {
                throw new IllegalArgumentException("Vui lòng nhập nội dung sửa cho đoạn đã chọn.");
            }
            if (request.getType() == HomeworkAnnotationType.NOTE && note == null) {
                throw new IllegalArgumentException("Vui lòng nhập ghi chú cho đoạn đã chọn.");
            }
            normalized.add(HomeworkTextAnnotationResponse.builder()
                    .id(id)
                    .type(request.getType())
                    .startOffset(start)
                    .endOffset(end)
                    .selectedText(actualText)
                    .replacementText(replacement)
                    .note(note)
                    .build());
        }

        normalized.sort(Comparator.comparing(HomeworkTextAnnotationResponse::getStartOffset));
        for (int index = 1; index < normalized.size(); index++) {
            if (normalized.get(index).getStartOffset() < normalized.get(index - 1).getEndOffset()) {
                throw new IllegalArgumentException("Các đoạn ghi chú không được chồng lên nhau.");
            }
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(normalized);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể lưu ghi chú trên bài làm.", exception);
        }
    }

    public List<HomeworkTextAnnotationResponse> deserialize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(value, RESPONSE_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Dữ liệu ghi chú trên bài làm không hợp lệ.", exception);
        }
    }

    private void validateRequiredFields(HomeworkTextAnnotationRequest request) {
        if (request == null || request.getId() == null || request.getId().isBlank()
                || request.getType() == null || request.getStartOffset() == null
                || request.getEndOffset() == null || request.getSelectedText() == null
                || request.getSelectedText().isBlank()) {
            throw new IllegalArgumentException("Ghi chú trên bài làm chưa đầy đủ thông tin.");
        }
        if (request.getId().trim().length() > 64 || request.getSelectedText().length() > 2000
                || length(request.getReplacementText()) > 2000 || length(request.getNote()) > 2000) {
            throw new IllegalArgumentException("Nội dung ghi chú vượt quá độ dài cho phép.");
        }
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

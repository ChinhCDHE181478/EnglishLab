package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.HomeworkTextAnnotationRequest;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkAnnotationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HomeworkTextAnnotationCodecTest {

    private final HomeworkTextAnnotationCodec codec = new HomeworkTextAnnotationCodec();

    @Test
    void validateAndSerialize_PreservesValidCorrection() {
        String json = codec.validateAndSerialize(
                "I go to school yesterday.",
                List.of(annotation("a-1", 2, 4, "go", "went"))
        );

        assertThat(codec.deserialize(json)).singleElement().satisfies(item -> {
            assertThat(item.getSelectedText()).isEqualTo("go");
            assertThat(item.getReplacementText()).isEqualTo("went");
        });
    }

    @Test
    void validateAndSerialize_RejectsStaleSelectedText() {
        assertThatThrownBy(() -> codec.validateAndSerialize(
                "I go to school yesterday.",
                List.of(annotation("a-1", 2, 4, "do", "went"))
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã thay đổi");
    }

    @Test
    void validateAndSerialize_RejectsOverlappingAnnotations() {
        assertThatThrownBy(() -> codec.validateAndSerialize(
                "I go to school yesterday.",
                List.of(
                        annotation("a-1", 2, 4, "go", "went"),
                        annotation("a-2", 3, 9, "o to s", "replacement")
                )
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chồng lên nhau");
    }

    private HomeworkTextAnnotationRequest annotation(
            String id,
            int start,
            int end,
            String selectedText,
            String replacement
    ) {
        return HomeworkTextAnnotationRequest.builder()
                .id(id)
                .type(HomeworkAnnotationType.CORRECTION)
                .startOffset(start)
                .endOffset(end)
                .selectedText(selectedText)
                .replacementText(replacement)
                .build();
    }
}

package fu.sap490.g23.backend.dto.response.curriculum;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashcardSetResponse {
    private Long id;
    private String title;
    private String description;
    private String examCategory;
    private String skill;
    private String tags;
    private String cardsJson;
    private String status;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

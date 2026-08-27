package fu.sep490.g23.backend.entity.classroom;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassroomQuizQuestion {
    private Long id;

    @Builder.Default
    private Integer sortOrder = 0;

    private String prompt;

    private String optionsJson;

    private String correctAnswer;

    private String explanation;
}

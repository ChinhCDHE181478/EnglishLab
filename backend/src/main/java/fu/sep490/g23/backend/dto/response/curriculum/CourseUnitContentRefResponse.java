package fu.sep490.g23.backend.dto.response.curriculum;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseUnitContentRefResponse {
    private Long id;
    private String type;
    private Long resourceId;
    private String title;
    private String subtitle;
    private String skill;
    private String status;
    private String fileUrl;
    private Integer displayOrder;
    private String note;
    /** Nội dung để học viên mở trực tiếp, dùng cho flashcard và hướng dẫn bài luyện tập. */
    private String contentJson;
}

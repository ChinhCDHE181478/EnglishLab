package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HomeworkAttachmentUploadResponse {
    private String fileName;
    private String originalFileName;
    private String contentType;
    private long size;
    private String url;
}

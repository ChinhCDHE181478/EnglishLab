package fu.sap490.g23.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AttachmentUploadRequestSizeFilter extends OncePerRequestFilter {

    private static final long MAX_ATTACHMENT_REQUEST_BYTES = 21L * 1024 * 1024;
    private static final Set<String> FIXED_UPLOAD_PATHS = Set.of(
            "/api/student/classrooms/homework/attachments",
            "/api/teacher/classrooms/homework/attachments",
            "/api/content-manager/material-library/upload"
    );
    private static final Pattern TUITION_PROOF_PATH = Pattern.compile(
            "^/api/student/classrooms/\\d+/tuition-proofs$"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !FIXED_UPLOAD_PATHS.contains(path) && !TUITION_PROOF_PATH.matcher(path).matches();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long contentLength = request.getContentLengthLong();
        if (contentLength < 0) {
            response.sendError(HttpStatus.LENGTH_REQUIRED.value(), "Yêu cầu tải tệp phải có Content-Length.");
            return;
        }
        if (contentLength > MAX_ATTACHMENT_REQUEST_BYTES) {
            response.sendError(HttpStatus.PAYLOAD_TOO_LARGE.value(), "Tệp đính kèm không được vượt quá 20 MB.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}

package fu.sep490.g23.backend.controller.teacher;

import fu.sep490.g23.backend.dto.response.teacher.GoogleMeetAuthorizationResponse;
import fu.sep490.g23.backend.dto.response.teacher.TeacherGoogleMeetConnectionResponse;
import fu.sep490.g23.backend.service.classroom.GoogleMeetProperties;
import fu.sep490.g23.backend.service.classroom.TeacherGoogleMeetConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/teacher/google-meet")
@RequiredArgsConstructor
public class TeacherGoogleMeetConnectionController {

    private final TeacherGoogleMeetConnectionService connectionService;
    private final GoogleMeetProperties properties;

    @GetMapping("/connection")
    public ResponseEntity<TeacherGoogleMeetConnectionResponse> connection(Authentication authentication) {
        return ResponseEntity.ok(connectionService.getConnection(authentication.getName()));
    }

    @PostMapping("/connect")
    public ResponseEntity<GoogleMeetAuthorizationResponse> connect(Authentication authentication) {
        return ResponseEntity.ok(new GoogleMeetAuthorizationResponse(
                connectionService.createAuthorizationUrl(authentication.getName())
        ));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error
    ) {
        String redirect;
        try {
            if (error != null && !error.isBlank()) {
                throw new IllegalArgumentException("Bạn đã hủy cấp quyền Google Meet.");
            }
            redirect = connectionService.completeAuthorization(code, state);
        } catch (RuntimeException exception) {
            redirect = properties.getFrontendReturnUrl()
                    + "?googleMeet=error&message="
                    + URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        }
        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, URI.create(redirect).toString())
                .build();
    }

    @DeleteMapping("/connection")
    public ResponseEntity<Void> disconnect(Authentication authentication) {
        connectionService.disconnect(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}

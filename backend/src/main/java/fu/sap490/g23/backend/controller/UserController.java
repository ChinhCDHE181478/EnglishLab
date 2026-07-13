package fu.sap490.g23.backend.controller;

import fu.sap490.g23.backend.dto.request.ChangePasswordRequest;
import fu.sap490.g23.backend.dto.request.UpdateNotificationPreferenceRequest;
import fu.sap490.g23.backend.dto.request.UpdateProfileRequest;
import fu.sap490.g23.backend.dto.response.NotificationPreferenceResponse;
import fu.sap490.g23.backend.dto.response.UserResponse;
import fu.sap490.g23.backend.service.notification.NotificationPreferenceService;
import fu.sap490.g23.backend.service.user.AvatarStorageService;
import fu.sap490.g23.backend.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AvatarStorageService avatarStorageService;
    private final NotificationPreferenceService notificationPreferenceService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponse response = userService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse response = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> updateAvatar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("file") MultipartFile file
    ) {
        String publicUrlBase = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/user/avatars/")
                .toUriString();
        return ResponseEntity.ok(userService.updateAvatar(userDetails.getUsername(), file, publicUrlBase));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<UserResponse> deleteAvatar(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.deleteAvatar(userDetails.getUsername()));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponse> getNotificationPreferences(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(notificationPreferenceService.getForUser(userDetails.getUsername()));
    }

    @PutMapping("/me/notification-preferences")
    public ResponseEntity<NotificationPreferenceResponse> updateNotificationPreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateNotificationPreferenceRequest request
    ) {
        return ResponseEntity.ok(notificationPreferenceService.updateForUser(userDetails.getUsername(), request));
    }

    @GetMapping("/avatars/{fileName:.+}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String fileName) {
        Resource resource = avatarStorageService.load(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatarStorageService.contentType(fileName)))
                .header("Cache-Control", "public, max-age=86400")
                .body(resource);
    }
}

package fu.sep490.g23.backend.controller;

import fu.sep490.g23.backend.dto.response.ApiResponse;
import fu.sep490.g23.backend.service.home.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/home")
    public ResponseEntity<ApiResponse> home() {
        return ResponseEntity.ok(homeService.getHomeMessage());
    }
}

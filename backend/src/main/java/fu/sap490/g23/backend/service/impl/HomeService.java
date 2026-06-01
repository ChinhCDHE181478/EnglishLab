package fu.sap490.g23.backend.service.impl;

import fu.sap490.g23.backend.dto.response.ApiResponse;
import fu.sap490.g23.backend.service.IHomeService;
import org.springframework.stereotype.Service;

@Service
public class HomeService implements IHomeService {

    @Override
    public ApiResponse getHomeMessage() {
        return ApiResponse.builder()
                .message("Welcome to EnglishLab Home Page")
                .description("You are successfully authenticated.")
                .build();
    }
}

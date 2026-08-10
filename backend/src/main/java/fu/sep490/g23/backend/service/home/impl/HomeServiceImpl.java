package fu.sap490.g23.backend.service.home.impl;

import fu.sap490.g23.backend.service.home.*;

import fu.sap490.g23.backend.dto.response.ApiResponse;
import org.springframework.stereotype.Service;

@Service
public class HomeServiceImpl implements HomeService {

    @Override
    public ApiResponse getHomeMessage() {
        return ApiResponse.builder()
                .message("Welcome to EnglishLab Home Page")
                .description("You are successfully authenticated.")
                .build();
    }
}

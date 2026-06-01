package fu.sap490.g23.backend.service.impl;

import fu.sap490.g23.backend.dto.request.LoginRequest;
import fu.sap490.g23.backend.dto.request.RegisterRequest;
import fu.sap490.g23.backend.dto.response.AuthResponse;
import fu.sap490.g23.backend.dto.response.UserResponse;
import fu.sap490.g23.backend.entity.Role;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.security.JwtService;
import fu.sap490.g23.backend.service.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        UserResponse userResponse = UserResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .phoneNumber(savedUser.getPhoneNumber())
                .targetExam(savedUser.getTargetExam())
                .targetScore(savedUser.getTargetScore())
                .studyGoal(savedUser.getStudyGoal())
                .profileCompleted(savedUser.isProfileCompleted())
                .build();

        return AuthResponse.builder()
                .message("User registered successfully")
                .user(userResponse)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateToken(user);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .phoneNumber(user.getPhoneNumber())
                .targetExam(user.getTargetExam())
                .targetScore(user.getTargetScore())
                .studyGoal(user.getStudyGoal())
                .profileCompleted(user.isProfileCompleted())
                .build();

        return AuthResponse.builder()
                .message("Login successful")
                .accessToken(token)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }
}

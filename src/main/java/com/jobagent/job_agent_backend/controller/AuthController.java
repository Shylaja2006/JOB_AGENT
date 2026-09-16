package com.jobagent.job_agent_backend.controller;

import com.jobagent.job_agent_backend.dto.LoginRequest;
import com.jobagent.job_agent_backend.dto.RegisterRequest;
import com.jobagent.job_agent_backend.entity.User;
import com.jobagent.job_agent_backend.repository.UserRepository;
import com.jobagent.job_agent_backend.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthController(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            JwtService jwtService) {

        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                    .body("Email already registered");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        User existingUser = userRepository.findByEmail(email)
                .orElse(null);

        if (existingUser == null) {
            return ResponseEntity.status(401)
                    .body("Invalid email or password");
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                existingUser.getPassword())) {

            return ResponseEntity.status(401)
                    .body("Invalid email or password");
        }

        String token = jwtService.generateToken(existingUser.getEmail());

        return ResponseEntity.ok(token);
    }
}
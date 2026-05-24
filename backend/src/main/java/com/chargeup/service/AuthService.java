package com.chargeup.service;

import com.chargeup.dto.auth.AuthResponse;
import com.chargeup.dto.auth.LoginRequest;
import com.chargeup.dto.auth.RegisterRequest;
import com.chargeup.exception.BadRequestException;
import com.chargeup.entity.Role;
import com.chargeup.repository.UserRepository;
import com.chargeup.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MappingService mappingService;

    public AuthService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        MappingService mappingService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.mappingService = mappingService;
    }

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email is already registered");
        }
        if (request.role() == Role.ADMIN) {
            throw new BadRequestException("Admin accounts must be provisioned by operations");
        }

        var user = new com.chargeup.entity.User();
        user.setName(request.name());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        var saved = userRepository.save(user);
        var securityUser = User.builder()
            .username(saved.getEmail())
            .password(saved.getPassword())
            .authorities("ROLE_" + saved.getRole().name())
            .build();

        return new AuthResponse(jwtService.generateToken(securityUser), mappingService.toUserSummary(saved));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password())
        );

        var user = userRepository.findByEmail(request.email().toLowerCase())
            .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        var securityUser = User.builder()
            .username(user.getEmail())
            .password(user.getPassword())
            .authorities("ROLE_" + user.getRole().name())
            .build();

        return new AuthResponse(jwtService.generateToken(securityUser), mappingService.toUserSummary(user));
    }
}

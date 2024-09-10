package com.tibor.book_network.auth;

import com.tibor.book_network.auth.model.AuthenticationRequestDTO;
import com.tibor.book_network.auth.model.AuthenticationResponseDTO;
import com.tibor.book_network.email.EmailService;
import com.tibor.book_network.email.EmailTemplateName;
import com.tibor.book_network.role.Role;
import com.tibor.book_network.role.RoleRepository;
import com.tibor.book_network.security.JwtService;
import com.tibor.book_network.user.Token;
import com.tibor.book_network.user.TokenRepository;
import com.tibor.book_network.user.User;
import com.tibor.book_network.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    public void registerUser(RegistrationRequestDTO requestDTO) throws MessagingException {
        Role userRole = roleRepository.findByName("USER").orElseThrow(
                () -> new IllegalStateException("Role USER was not initialized"));
        User user = User.builder()
                .firstName(requestDTO.getFirstName())
                .lastName(requestDTO.getLastName())
                .email(requestDTO.getEmail())
                .password(passwordEncoder.encode(requestDTO.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .build();
        userRepository.save(user);
        sendValidationEmail(user);
    }

    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO requestDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requestDTO.getEmail(), requestDTO.getPassword())
        );
        Map<String, Object> claims = new HashMap<>();
        User user = (User) authentication.getPrincipal();
        claims.put("fullName", user.fullName());
        String jwtToken = jwtService.generateToken(claims, user);
        return AuthenticationResponseDTO.builder().token(jwtToken).build();
    }

    private void sendValidationEmail(User user) throws MessagingException {
        String newToken = generateActivationCode(6);
        Token token = Token.builder()
                .token(newToken)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();
        tokenRepository.save(token);

        emailService.sendEmail(
                user.getEmail(),
                user.getName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Account activation");
    }

    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder activationCodeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            activationCodeBuilder.append(characters.charAt(randomIndex));
        }
        return activationCodeBuilder.toString();
    }

    //@Transactional
    public void activateAccount(String token) throws MessagingException {
        Token savedToken = tokenRepository.findByToken(token).orElseThrow(
                () -> new RuntimeException("Invalid token"));
        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activate token has expired. A new token has been sent.");
        }
        User user = userRepository.findById(savedToken.getUser().getId()).orElseThrow(
                () -> new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);

        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
    }
}

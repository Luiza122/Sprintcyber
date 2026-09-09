package com.ford.fordretain.controller;

import com.ford.fordretain.dto.LoginRequestDTO;
import com.ford.fordretain.dto.LoginResponseDTO;
import com.ford.fordretain.security.JwtService;
import com.ford.fordretain.security.SecurityMetrics;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticação e geração de token JWT")
public class AuthController {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<SecurityMetrics> securityMetricsProvider;

    private static final String SENHA_HASH = "$2b$10$UhY5a/ojCNHkvkkdxdDCOOLnBWIzjYfSl8sLRVUa1/l7wfDJ0eVIW";
    private static final Map<String, String[]> USUARIOS = Map.of(
            "admin@ford.com", new String[]{SENHA_HASH, "ADMIN"},
            "analista@ford.com", new String[]{SENHA_HASH, "ANALISTA"},
            "gerente@ford.com", new String[]{SENHA_HASH, "GERENTE"}
    );

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Autentica o usuário e retorna token JWT assinado")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        String emailLog = maskEmail(request.getEmail());
        log.info("Tentativa de login | usuario={}", emailLog);
        String[] userData = USUARIOS.get(request.getEmail());
        if (userData == null || !passwordEncoder.matches(request.getSenha(), userData[0])) {
            SecurityMetrics metrics = securityMetricsProvider.getIfAvailable();
            if (metrics != null) metrics.loginFailure();
            log.warn("Falha de autenticação | usuario={}", emailLog);
            return ResponseEntity.status(401).body(Map.of("erro", "Credenciais inválidas"));
        }
        String role = userData[1];
        String token = jwtService.generateToken(request.getEmail(), role);
        SecurityMetrics metrics = securityMetricsProvider.getIfAvailable();
        if (metrics != null) metrics.loginSuccess();
        log.info("Login realizado | usuario={} | role={}", emailLog, role);
        return ResponseEntity.ok(LoginResponseDTO.builder()
                .token(token).tipo("Bearer").email(request.getEmail()).role(role)
                .expiresIn(jwtService.getExpiration()).build());
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return "***";
        int at = email.indexOf('@');
        if (at > 1) return email.charAt(0) + "***" + email.substring(at);
        return "***";
    }
}

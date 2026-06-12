/*
 * File purpose: REST controller for local exchange registration, login, logout, and current user.
 */
package com.example.exchange.interfaces.web.controller;

import com.example.exchange.application.service.AuthService;
import com.example.exchange.infra.config.ApiAuthProperties;
import com.example.exchange.infra.config.CustomerAuthProperties;
import com.example.exchange.interfaces.web.dto.ApiResponse;
import com.example.exchange.interfaces.web.dto.AuthConfigResponse;
import com.example.exchange.interfaces.web.dto.AuthResponse;
import com.example.exchange.interfaces.web.dto.LoginRequest;
import com.example.exchange.interfaces.web.dto.LogoutRequest;
import com.example.exchange.interfaces.web.dto.RegisterRequest;
import com.example.exchange.interfaces.web.dto.RegistrationResponse;
import com.example.exchange.interfaces.web.dto.VerifyEmailRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ApiAuthProperties apiAuthProperties;
    private final CustomerAuthProperties customerAuthProperties;

    /** Public auth config lets static clients render a free human-verification widget without exposing secrets. */
    @GetMapping("/config")
    public ApiResponse<AuthConfigResponse> config() {
        CustomerAuthProperties.Captcha captcha = customerAuthProperties.getCaptcha();
        return ApiResponse.ok(new AuthConfigResponse(
                captcha.isEnabled(),
                captcha.getProvider(),
                captcha.getSiteKey(),
                customerAuthProperties.getEmailVerification().isEnabled()
        ));
    }

    /** First-party registration starts email verification; third-party OAuth/wallet login is a later task. */
    @PostMapping("/register")
    public ApiResponse<RegistrationResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(RegistrationResponse.from(authService.register(
                request.email(),
                request.password(),
                request.humanVerificationToken()
        )));
    }

    /** Email verification activates a pending customer account before password login can issue tokens. */
    @PostMapping("/verify-email")
    public ApiResponse<AuthResponse.User> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ApiResponse.ok(AuthResponse.User.from(authService.verifyEmail(request.token())));
    }

    /** Password login returns access JWT plus refresh token for server-side logout/revocation. */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(AuthResponse.from(authService.login(request.email(), request.password())));
    }

    /** Logout revokes the refresh token; existing access JWTs expire naturally. */
    @PostMapping("/logout")
    public ApiResponse<Boolean> logout(@RequestBody LogoutRequest request) {
        return ApiResponse.ok(authService.logout(request == null ? null : request.refreshToken()));
    }

    /** Current user endpoint lets the exchange console restore local session state after refresh. */
    @GetMapping("/me")
    public ApiResponse<AuthResponse.User> me(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        String token = authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorization.substring(7)
                : "";
        return ApiResponse.ok(authService.currentUser(
                        token,
                        apiAuthProperties.getClockSkewSeconds()
                )
                .map(AuthResponse.User::from)
                .orElse(null));
    }
}

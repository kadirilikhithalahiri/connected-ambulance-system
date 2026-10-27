package com.abc.emergency.web;

import com.abc.emergency.dto.ApiResponse;
import com.abc.emergency.dto.LoginRequest;
import com.abc.emergency.dto.UserDTO;
import com.abc.emergency.model.User;
import com.abc.emergency.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginService loginService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(LoginService loginService) {
        this.loginService = loginService;
        this.securityContextRepository = new HttpSessionSecurityContextRepository();
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDTO>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        User user = loginService.login(request.getUsername(), request.getPassword());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(401, "Invalid username or password, or account inactive.", null));
        }

        // Prevent session fixation: if a pre-existing session was attached, rotate its ID; otherwise create new
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            httpRequest.changeSessionId();
        } else {
            session = httpRequest.getSession(true);
        }

        // Assign Spring Security authority prefixed with ROLE_
        List<GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

        // Store User domain entity as the trusted principal in the Spring Security context
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, authorities);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Persist the security context into the session through the security context repository
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
        session.setAttribute("currentUser", user);

        UserDTO userDTO = toDTO(user);

        return ResponseEntity.ok(new ApiResponse<>(200, "Login successful.", userDTO));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(
            Authentication authentication,
            HttpSession session) {

        User user = null;

        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            if (authentication.getPrincipal() instanceof User u) {
                user = u;
            }
        }

        if (user == null && session != null) {
            Object sessionUser = session.getAttribute("currentUser");
            if (sessionUser instanceof User u) {
                user = u;
            }
        }

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(401, "Not authenticated. Please sign in.", null));
        }

        return ResponseEntity.ok(new ApiResponse<>(200, "Authenticated user session active.", toDTO(user)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            Authentication authentication) {

        int userId = -1;

        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            userId = user.getUserId();
        }

        HttpSession session = request.getSession(false);
        if (userId <= 0 && session != null) {
            Object sessionUser = session.getAttribute("currentUser");
            if (sessionUser instanceof User user) {
                userId = user.getUserId();
            }
        }

        if (userId > 0) {
            // Update logout timestamp in Login_Log using trusted user ID
            loginService.logout(userId);
        }

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(new ApiResponse<>(200, "Logout successful. Session invalidated.", null));
    }

    @GetMapping("/csrf")
    public ResponseEntity<ApiResponse<Map<String, String>>> getCsrfToken(CsrfToken csrfToken) {

        Map<String, String> tokenInfo = new HashMap<>();

        if (csrfToken != null) {
            tokenInfo.put("token", csrfToken.getToken());
            tokenInfo.put("headerName", csrfToken.getHeaderName());
            tokenInfo.put("parameterName", csrfToken.getParameterName());
        }

        return ResponseEntity.ok(new ApiResponse<>(200, "CSRF token retrieved.", tokenInfo));
    }

    private UserDTO toDTO(User user) {
        return new UserDTO(
                user.getUserId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getPhone(),
                user.isActive()
        );
    }
}

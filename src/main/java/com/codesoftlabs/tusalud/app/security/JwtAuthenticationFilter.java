package com.codesoftlabs.tusalud.app.security;

import com.codesoftlabs.tusalud.app.services.interfaces.SesionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_ATTRIBUTE = "jwt_auth_error";
    public static final String TOKEN_ID_ATTRIBUTE = "jwt_token_id";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final SesionService sesionService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = jwtUtils.parseClaims(token);
            String email = claims.getSubject();
            String jti = claims.getId();

            if (claims.get(JwtUtils.CLAIM_PROPOSITO) != null) {
                request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Token inválido");
            } else if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (!userDetails.isEnabled()) {
                    request.setAttribute(AUTH_ERROR_ATTRIBUTE, "La cuenta de usuario está inactiva");
                } else if (jti != null && !sesionValida(jti)) {
                    request.setAttribute(AUTH_ERROR_ATTRIBUTE, "La sesión no es válida o fue cerrada");
                } else {
                    if (jti != null) {
                        request.setAttribute(TOKEN_ID_ATTRIBUTE, UUID.fromString(jti));
                    }
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                }
            }
        } catch (ExpiredJwtException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "El token ha expirado");
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token JWT rechazado: {}", ex.getMessage());
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Token inválido");
        } catch (UsernameNotFoundException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "El usuario del token ya no existe");
        }

        filterChain.doFilter(request, response);
    }

    private boolean sesionValida(String jti) {
        return sesionService.sesionActiva(UUID.fromString(jti));
    }
}

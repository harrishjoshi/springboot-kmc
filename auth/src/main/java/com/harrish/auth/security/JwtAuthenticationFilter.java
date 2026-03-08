package com.harrish.auth.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final var authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Skip if Authorization header is missing or not a Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token from Authorization header
            jwt = authHeader.substring(7);
            userEmail = jwtService.extractUsername(jwt);

            // If we have a username and no authentication exists yet
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load user details from database
                var userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Validate token
                if (jwtService.hasValidExpiration(jwt, userDetails)) {
                    // Create authentication token
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    // Set details
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Update security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("JWT authentication successful for user: {}", userEmail);
                } else {
                    log.debug("JWT token validation failed for user: {}", userEmail);
                }
            }
        } catch (JwtException e) {
            // Log for debugging but don't expose JWT details to client
            log.warn("JWT processing failed: {}", e.getMessage());
            // Don't set authentication - let request continue as unauthenticated
        } catch (Exception e) {
            // Catch any other exceptions to prevent filter chain interruption
            log.error("Unexpected error during JWT authentication", e);
            // Don't set authentication - let request continue as unauthenticated
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}

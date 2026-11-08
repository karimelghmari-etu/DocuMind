
package org.example.documind.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService customUserDetailsService
    ) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        System.out.println("\n========== JWT FILTER ==========");
        System.out.println("REQUEST: " + request.getMethod()
                + " " + request.getRequestURI());

        String authorization = request.getHeader("Authorization");

        System.out.println("AUTHORIZATION HEADER PRESENT: "
                + (authorization != null));

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            System.out.println("NO BEARER TOKEN FOUND");

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        try {
            String username = jwtService.extractUsername(token);

            System.out.println("JWT USERNAME: " + username);

            if (username != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                var userDetails =
                        customUserDetailsService.loadUserByUsername(username);

                System.out.println("USER FOUND IN DATABASE");

                if (jwtService.isTokenValid(token, userDetails)) {

                    System.out.println("JWT IS VALID");

                    var authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);

                    System.out.println(
                            "AUTHENTICATION SET IN SECURITY CONTEXT"
                    );

                } else {
                    System.out.println("JWT IS NOT VALID");
                }

            } else {
                System.out.println(
                        "USERNAME IS NULL OR USER ALREADY AUTHENTICATED"
                );
            }

        } catch (JwtException | IllegalArgumentException |
                 UsernameNotFoundException e) {

            System.out.println("JWT ERROR: "
                    + e.getClass().getSimpleName());

            System.out.println("ERROR MESSAGE: "
                    + e.getMessage());

            SecurityContextHolder.clearContext();
        }

        System.out.println("========== END JWT FILTER ==========\n");

        filterChain.doFilter(request, response);
    }
}

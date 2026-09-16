package com.apex.wallet.infrastructure.security;

import com.apex.wallet.domain.model.Account;
import com.apex.wallet.infrastructure.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccountRepository accountRepository;

    public JwtAuthenticationFilter(JwtService jwtService, AccountRepository accountRepository) {
        this.jwtService = jwtService;
        this.accountRepository = accountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        try {
            final String userEmail = jwtService.extractEmail(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null && jwtService.isTokenValid(jwt, userEmail)) {
                Account account = null;
                try {
                    account = accountRepository.findByEmail(userEmail).orElse(null);
                } catch (Exception dbEx) {
                    // Under database outage / degraded mode:
                    // Reconstruct authenticated principal directly from cryptographically verified JWT claims
                    Long accountId = jwtService.extractAccountId(jwt);
                    String holderName = jwtService.extractClaim(jwt, c -> (String) c.get("holderName"));
                    String role = jwtService.extractClaim(jwt, c -> (String) c.get("role"));
                    if (accountId != null && role != null) {
                        account = new Account();
                        account.setId(accountId);
                        account.setEmail(userEmail);
                        account.setHolderName(holderName != null ? holderName : "Usuario");
                        account.setRole(role);
                    }
                }

                if (account != null) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            account,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(account.getRole()))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
            // Token invalid or expired - proceed unauthenticated
        }

        filterChain.doFilter(request, response);
    }
}

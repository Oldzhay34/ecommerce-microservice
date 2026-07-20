package com.promptengineering.auth.infrastructure.security.filter;

import com.promptengineering.auth.infrastructure.security.provider.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService; // RBAC için CustomUserDetailsService enjekte edildi

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {

                // 1. Token içerisinden e-posta bilgisini alıyoruz (Subject veya Claim'den)
                String email = jwtTokenProvider.getEmailFromToken(jwt);

                // 2. Veritabanından (Adapter üzerinden) kullanıcının güncel durumunu ve rollerini çekiyoruz
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 3. Spring Security'ye kullanıcının kimliğini ve Yetkilerini (Authorities) tanıtıyoruz
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities() // "ROLE_CUSTOMER", "ROLE_ADMIN" vb.
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 4. Context'e yerleştiriyoruz (Böylece @PreAuthorize anatasyonları çalışabilecek)
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Token süresi dolmuş, imza geçersiz veya kullanıcı silinmiş olabilir. Context temiz bırakılır.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
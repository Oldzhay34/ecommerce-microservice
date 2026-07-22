package com.mediaservice.infrastructure.security;

import com.mediaservice.domain.exception.UnauthorizedMediaAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * JWT'den kimlik cikarma. Product servisiyle BIREBIR ayni desen:
 * principal, JwtAuthFilter tarafindan UUID olarak konur (Product: JwtResourceFilter).
 *
 * [Karar E] Sistemde ayri bir storeId attribute'u YOKTUR. JWT 'sub' claim'i userId'dir;
 * rol tekil 'role' claim'inde tasinir. ROLE_STORE tasiyan kullanicinin userId'si magaza
 * kimligi olarak kullanilir (MediaAsset.storeId alanina bu yazilir).
 */
public final class SecurityUtils {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_STORE = "ROLE_STORE";
    public static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";

    private SecurityUtils() {
    }

    /**
     * IDOR korumasi: kimlik yalnizca SecurityContext'ten okunur.
     * Request body, path veya X-User-Id header'indan KABUL EDILMEZ.
     */
    public static UUID extractUserIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedMediaAccessException("Bu gorsel uzerinde islem yetkiniz yok.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID userId) {
            return userId;
        }
        // Savunma amacli fallback: principal tipi degisirse sessizce patlamasin.
        if (principal != null) {
            try {
                return UUID.fromString(principal.toString());
            } catch (IllegalArgumentException ignored) {
                // asagida ortak hata firlatilir
            }
        }
        throw new UnauthorizedMediaAccessException("Bu gorsel uzerinde islem yetkiniz yok.");
    }

    public static boolean isAdmin() {
        return hasRole(ROLE_ADMIN);
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
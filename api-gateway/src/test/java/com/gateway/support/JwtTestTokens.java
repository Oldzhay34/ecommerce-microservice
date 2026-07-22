package com.gateway.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Test token uretici (api-gateway'e ozel, paylasilan modul YOK).
 *
 * Uretilen token'lar production'daki JwtValidator ile ayni HMAC-SHA algoritmasini
 * kullanir; boylece "gecerli imza" senaryosu gercekten dogrulanabilir. Bozuk imza
 * senaryosu icin farkli bir secret (WRONG_SECRET) kullanilir.
 */
public final class JwtTestTokens {

    /** application.yml icindeki app.security.jwt-secret varsayilani ile ayni. */
    public static final String SECRET =
            "EcommerceSharedJwtSecretKey2026VeryLongAndSecure32Bytes";

    /** Ayni uzunlukta ama FARKLI bir secret: imza dogrulamasinin gercekten calistigini ispatlar. */
    public static final String WRONG_SECRET =
            "CompletelyDifferentAttackerSecretKey2026PaddedTo32Bytes";

    private JwtTestTokens() {
    }

    public static SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Gecerli, 1 saat gecerlilikli token. role claim'i "role" alaninda tasinir. */
    public static String valid(String userId, String role) {
        return builder(userId)
                .claim("role", role)
                .signWith(key(SECRET))
                .compact();
    }

    /** Rol claim'i hic olmayan gecerli token. */
    public static String validWithoutRole(String userId) {
        return builder(userId).signWith(key(SECRET)).compact();
    }

    /** Rol bilgisi "roles" listesinde tasinan token. */
    public static String validWithRolesList(String userId, List<String> roles) {
        return builder(userId).claim("roles", roles).signWith(key(SECRET)).compact();
    }

    /** Rol bilgisi "authorities" claim'inde tasinan token. */
    public static String validWithAuthorities(String userId, Object authorities) {
        return builder(userId).claim("authorities", authorities).signWith(key(SECRET)).compact();
    }

    /** Subject (userId) claim'i hic set edilmemis token. */
    public static String withoutSubject() {
        return Jwts.builder()
                .claims(Map.of("role", "ROLE_USER"))
                .issuedAt(new Date(System.currentTimeMillis() - 1000))
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key(SECRET))
                .compact();
    }

    /** Suresi 1 dakika once dolmus token. */
    public static String expired(String userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .claim("role", "ROLE_USER")
                .issuedAt(new Date(now - 7_200_000))
                .expiration(new Date(now - 60_000))
                .signWith(key(SECRET))
                .compact();
    }

    /** Yapisi dogru ama BASKA bir secret ile imzalanmis token (imza gecersiz). */
    public static String signedWithWrongKey(String userId) {
        return builder(userId)
                .claim("role", "ROLE_ADMIN")
                .signWith(key(WRONG_SECRET))
                .compact();
    }

    /** Gecerli token'in imza bolumu bozulmus hali (payload degistirilmis gibi). */
    public static String tamperedSignature(String userId) {
        String token = valid(userId, "ROLE_USER");
        int lastDot = token.lastIndexOf('.');
        String signature = token.substring(lastDot + 1);
        char first = signature.charAt(0);
        char replacement = (first == 'A') ? 'B' : 'A';
        return token.substring(0, lastDot + 1) + replacement + signature.substring(1);
    }

    /** JWT olmayan, tamamen bozuk format. */
    public static String malformed() {
        return "this.is.not-a-jwt";
    }

    /** Belirli bir issuer ile imzalanmis gecerli token. */
    public static String withIssuer(String userId, String issuer) {
        return builder(userId)
                .issuer(issuer)
                .claim("role", "ROLE_USER")
                .signWith(key(SECRET))
                .compact();
    }

    private static io.jsonwebtoken.JwtBuilder builder(String userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date(now - 1000))
                .expiration(new Date(now + 3_600_000));
    }
}

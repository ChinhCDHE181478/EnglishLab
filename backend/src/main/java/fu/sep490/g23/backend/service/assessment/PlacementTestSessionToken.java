package fu.sep490.g23.backend.service.assessment;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class PlacementTestSessionToken {
    private static final String PURPOSE = "PLACEMENT_TEST_SESSION";
    private static final Duration SESSION_DURATION = Duration.ofHours(6);

    @Value("${jwt.secret}")
    private String secret;

    public String issue(String studentEmail, String examType) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(studentEmail)
                .claim("purpose", PURPOSE)
                .claim("examType", examType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(SESSION_DURATION)))
                .signWith(signingKey())
                .compact();
    }

    public boolean isValid(String token, String studentEmail, String examType) {
        if (token == null || token.isBlank()) return false;
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return PURPOSE.equals(claims.get("purpose", String.class))
                    && studentEmail.equalsIgnoreCase(claims.getSubject())
                    && examType.equalsIgnoreCase(claims.get("examType", String.class));
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}

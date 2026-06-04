package tn.esprit.arabsoftback.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tn.esprit.arabsoftback.service.UserPresenceService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final String SECRET_KEY = "8d4f7b2a9e3c1d5f6a8b0c2e4f7a9b1d3e5f7a9b2c4e6f8a0b1c2d3e4f5a6b789abcdef";
    private final String TOKEN_PREFIX = "Bearer ";
    private final String HEADER_STRING = "Authorization";

    @Autowired
    private UserPresenceService userPresenceService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        logger.debug("Checking if URI should be filtered: {}", path);
        
        boolean shouldNotFilter = path.startsWith("/api/auth/") || // Exclure tous les endpoints d'auth
                path.startsWith("/api/users/forgot-password") ||
                path.startsWith("/api/users/reset-password") ||
                path.startsWith("/api/test/") || // Exclure les endpoints de test
                path.startsWith("/api/immatriculation/") || // Exclure les endpoints d'immatriculation
                path.startsWith("/api/ocr/") || // Exclure les endpoints OCR
                path.startsWith("/api/cin-validator/") || // Exclure le validateur CIN
                path.startsWith("/api/email/") || // Exclure les endpoints email
                path.startsWith("/uploads/") || // Exclure les fichiers statiques (images)
                isPublicPublicationRead(request) ||
                (path.startsWith("/api/download-documents") && request.getMethod().equals("GET")) ||
                (path.startsWith("/api/download-documents")
                        && "POST".equals(request.getMethod())
                        && path.contains("/record-download"));
        
        if (shouldNotFilter) {
            logger.info("URI {} is excluded from JWT filtering", path);
        }
        
        return shouldNotFilter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (shouldNotFilter(request)) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HEADER_STRING);

        logger.info("Received request for URI: {}, Authorization header: {}", request.getRequestURI(), header);

        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            logger.warn("No Authorization header or invalid prefix for URI: {}", request.getRequestURI());
            chain.doFilter(request, response);
            return;
        }

        String token = header.replace(TOKEN_PREFIX, "");
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String email = claims.getSubject();
            if (email != null) {
                String role = claims.get("role", String.class);
                if (role == null) {
                    logger.warn("No role found in token for email: {}", email);
                    chain.doFilter(request, response);
                    return;
                }

                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        email, null, authorities);
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);

                Integer userId = extractUserId(claims);
                if (userId != null) {
                    userPresenceService.markPresent(userId);
                } else {
                    userPresenceService.markPresentByEmail(email);
                }

                logger.info("Successfully authenticated user: {} with role: {}", email, role);
            } else {
                logger.warn("No subject (email) found in token");
            }
        } catch (Exception e) {
            logger.error("Invalid JWT token for URI: {}, Error: {}", request.getRequestURI(), e.getMessage());
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }

    /**
     * Lectures publiques du catalogue (liste, detail, commentaires visibles).
     * Les GET admin (ex. /reports, /pending-validation, /statistics) passent par le filtre JWT.
     */
    private static boolean isPublicPublicationRead(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/publications")) {
            return false;
        }
        if ("/api/publications".equals(path)) {
            return true;
        }
        if (path.startsWith("/api/publications/filter")
                || path.startsWith("/api/publications/pinned")
                || path.startsWith("/api/publications/recent")
                || path.startsWith("/api/publications/popular")
                || path.startsWith("/api/publications/slug/")
                || path.startsWith("/api/publications/search/tag")
                || path.startsWith("/api/publications/creator/")) {
            return true;
        }
        return Pattern.compile("^/api/publications/\\d+$").matcher(path).matches()
                || Pattern.compile("^/api/publications/\\d+/comments$").matcher(path).matches();
    }

    private static Integer extractUserId(Claims claims) {
        Object raw = claims.get("userId");
        if (raw instanceof Integer integer) {
            return integer;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return null;
    }
}
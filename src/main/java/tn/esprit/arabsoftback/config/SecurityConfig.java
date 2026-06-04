package tn.esprit.arabsoftback.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/assets/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll() // Tous les endpoints d'auth sont publics
                        .requestMatchers(HttpMethod.POST, "/api/users/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cin-validator/verify").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cin-validator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/face-verification").permitAll()
                        .requestMatchers("/api/email/**").permitAll() // Endpoints email publics
                        .requestMatchers("/api/test/**").permitAll() // Endpoints de test publics
                        .requestMatchers("/api/immatriculation/**").permitAll() // Endpoints d'immatriculation publics
                        .requestMatchers("/api/ocr/**").permitAll() // Endpoints OCR publics
                        .requestMatchers("/api/trash/**").permitAll() // Endpoints de corbeille publics
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/reclamation/**").permitAll()
                        .requestMatchers("/api/demande-information/**").permitAll()
                        .requestMatchers("/api/sms/**").permitAll() // Endpoints SMS publics
                        .requestMatchers("/api/publications/**").permitAll() // Endpoints de publications publics
                        .requestMatchers(HttpMethod.GET, "/api/download-documents/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/download-documents/*/record-download").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/download-documents/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/download-documents/**").authenticated()
                        .requestMatchers("/api/users/**").permitAll() // Endpoints users publics
                        .requestMatchers("/uploads/**").permitAll() // Ressources statiques (images) publiques
                        .requestMatchers(HttpMethod.GET, "/api/training/images").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/training/upload").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/training/train").hasRole("ADMIN")
                        .requestMatchers("/api/training/**").hasRole("ADMIN") // Autoriser seulement les admins
                        .requestMatchers(HttpMethod.DELETE, "/api/training/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}



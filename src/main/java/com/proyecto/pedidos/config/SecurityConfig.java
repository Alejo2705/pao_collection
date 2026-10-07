package com.proyecto.pedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {
    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    @Bean
    UserDetailsService adminUsers(@Value("${ADMIN_USERNAME:admin}") String username,
                                 @Value("${ADMIN_PASSWORD:}") String password) {
        var users = new InMemoryUserDetailsManager();
        // Fail closed: no credentials means no account, never a default password.
        if (!password.isBlank()) {
            if (username.isBlank() || password.length() < 14 ||
                    password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                throw new IllegalArgumentException("Configure ADMIN_USERNAME y ADMIN_PASSWORD (14 caracteres mínimo, 72 bytes máximo).");
            }
            users.createUser(User.withUsername(username)
                    .password(new BCryptPasswordEncoder(12).encode(password))
                    .roles("ADMIN").build());
        }
        return users;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        var webhook = new AntPathRequestMatcher("/api/instagram/webhook", "POST");
        var entryPoints = new java.util.LinkedHashMap<org.springframework.security.web.util.matcher.RequestMatcher,
                org.springframework.security.web.AuthenticationEntryPoint>();
        entryPoints.put(new AntPathRequestMatcher("/api/**"),
                (request, response, exception) -> response.setStatus(401));
        var entryPoint = new org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint(entryPoints);
        entryPoint.setDefaultEntryPoint(new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/login.html"));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/login.html", "/login.js", "/auth/csrf",
                        "/styles.css", "/img/logo-pao-collection.jpg",
                        "/politica-privacidad.html", "/eliminacion-datos.html",
                        "/api/instagram/webhook").permitAll()
                .requestMatchers(webhook).permitAll()
                .anyRequest().hasRole("ADMIN"))
            .csrf(csrf -> csrf.ignoringRequestMatchers(webhook))
            .formLogin(login -> login.loginPage("/login.html")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/login.html?error"))
            .logout(logout -> logout.logoutUrl("/logout")
                    .logoutSuccessUrl("/login.html?logout")
                    .invalidateHttpSession(true).deleteCookies("JSESSIONID"))
            .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler((request, response, exception) -> response.setStatus(403)))
            .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
            .headers(headers -> headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                    "font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:; " +
                    "connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
            .addFilterBefore(new LoginRateLimitFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

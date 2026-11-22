package com.universal.erp.platform.security;

import com.universal.erp.platform.ui.LoginView;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(TenantAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                // static assets of the sign-in page must be reachable before login
                .requestMatchers("/login.css").permitAll()
                .requestMatchers("/register.css").permitAll()
                .requestMatchers("/login", "/register").permitAll()
                // mobile POS app sign-in (everything else under /api/** needs a session)
                .requestMatchers("/api/pos/login").permitAll()
                .requestMatchers("/api/**").authenticated());

        // Mobile app uses JSON + session cookie (SameSite=Lax), no browser form: CSRF token is not used.
        http.csrf(csrf -> csrf.ignoringRequestMatchers(PathPatternRequestMatcher.pathPattern("/api/pos/**")));

        // Runs right after the session SecurityContext is loaded, so tenant comes from the principal.
        http.addFilterAfter(new TenantContextFilter(), SecurityContextHolderFilter.class);

        // REST clients get 401 instead of a redirect to the login page.
        http.exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                PathPatternRequestMatcher.pathPattern("/api/**")));

        http.with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin.loginView(LoginView.class));

        return http.build();
    }
}
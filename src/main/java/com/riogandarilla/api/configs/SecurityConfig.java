package com.riogandarilla.api.configs;

import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.security.ApiAccessDeniedHandler;
import com.riogandarilla.api.security.ApiAuthenticationEntryPoint;
import com.riogandarilla.api.security.BearerTokenAuthenticationFilter;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Log4j2
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService webUserDetailsService(AppProperties properties) {
        return new InMemoryUserDetailsManager(
                User.withUsername(properties.webUsername())
                        .password("{noop}" + properties.webPassword())
                        .roles("ADMIN")
                        .build()
        );
    }

    @Bean
    public FilterRegistrationBean<BearerTokenAuthenticationFilter> bearerFilterRegistration(
            BearerTokenAuthenticationFilter filter
    ) {
        FilterRegistrationBean<BearerTokenAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiTokenSecurityFilterChain(
            HttpSecurity http,
            AppProperties properties
    ) throws Exception {
        http.securityMatcher("/api/auth/token")
                .csrf(AbstractHttpConfigurer::disable);
        if (!properties.securityEnabled()) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
        validateSecurityProperties(properties);
        http.httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AppProperties properties,
            BearerTokenAuthenticationFilter tokenFilter,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .exceptionHandling(errors -> errors
                        .accessDeniedHandler(accessDeniedHandler))
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'"))
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Permissions-Policy", "camera=(), microphone=(), geolocation=()")));

        if (!properties.securityEnabled()) {
            log.warn("Seguridad deshabilitada. Úsala únicamente en desarrollo local.");
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        validateSecurityProperties(properties);
        AuthenticationEntryPoint webEntryPoint = new LoginUrlAuthenticationEntryPoint("/admin/login");
        http.formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login/process")
                        .defaultSuccessUrl("/web/dashboard", true)
                        .permitAll())
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor(
                                authenticationEntryPoint,
                                new AntPathRequestMatcher("/api/**")
                        )
                        .defaultAuthenticationEntryPointFor(
                                webEntryPoint,
                                AnyRequestMatcher.INSTANCE
                        ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(properties.publicPaths().toArray(new String[0])).permitAll()
                        .requestMatchers("/", "/css/**", "/js/**", "/img/**").permitAll()
                        .requestMatchers("/admin/login", "/admin/login/process", "/error").permitAll()
                        .requestMatchers("/api/auth/token").hasRole("ADMIN")
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .requestMatchers("/web/dashboard").permitAll()
                        .requestMatchers("/web/**").hasRole("ADMIN")
                        .anyRequest().authenticated());
        http.logout(logout -> logout
                .logoutUrl("/web/logout")
                .logoutSuccessUrl("/web/dashboard?logout")
                .permitAll());

        return http.build();
    }

    private void validateSecurityProperties(AppProperties properties) {
        if (properties.apiBearerSecret().length() < 32) {
            throw new IllegalStateException(
                    "API_BEARER_SECRET debe tener al menos 32 caracteres cuando SECURITY_ENABLED=true"
            );
        }
        if (properties.webPassword().isBlank()) {
            throw new IllegalStateException("WEB_PASSWORD es obligatorio cuando SECURITY_ENABLED=true");
        }
    }
}

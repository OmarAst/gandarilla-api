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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AppProperties properties,
            BearerTokenAuthenticationFilter tokenFilter,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(authenticationEntryPoint)
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

        if (properties.apiBearerToken().isBlank()) {
            throw new IllegalStateException("API_BEARER_TOKEN es obligatorio cuando SECURITY_ENABLED=true");
        }
        if (properties.webPassword().isBlank()) {
            throw new IllegalStateException("WEB_PASSWORD es obligatorio cuando SECURITY_ENABLED=true");
        }

        BasicAuthenticationEntryPoint webEntryPoint = new BasicAuthenticationEntryPoint();
        webEntryPoint.setRealmName("Gandarilla Web");
        webEntryPoint.afterPropertiesSet();

        http.httpBasic(Customizer.withDefaults())
                .exceptionHandling(errors -> errors
                        .defaultAuthenticationEntryPointFor(
                                webEntryPoint,
                                new AntPathRequestMatcher("/web/**")
                        ))
                .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(properties.publicPaths().toArray(new String[0])).permitAll()
                        .requestMatchers("/", "/css/**", "/js/**", "/img/**").permitAll()
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .requestMatchers("/web/**").hasRole("ADMIN")
                        .anyRequest().authenticated());

        return http.build();
    }
}

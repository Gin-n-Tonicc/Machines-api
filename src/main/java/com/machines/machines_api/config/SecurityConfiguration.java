package com.machines.machines_api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.machines.machines_api.handlers.JwtAuthenticationEntryPoint;
import com.machines.machines_api.security.OAuth2LoginSuccessHandler;
import com.machines.machines_api.security.filters.JwtAuthenticationFilter;
import com.machines.machines_api.services.impl.security.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;

/**
 * Configuration class for setting up security configurations, including authentication,
 * authorization, and OAuth2 integration.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final LogoutHandler logoutHandler;
    private final ObjectMapper objectMapper;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final FrontendConfig frontendConfig;
    private final MessageSource messageSource;
    private final CustomOAuth2UserService oauthUserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(sessionManagementCustomizer -> sessionManagementCustomizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors()
                .and()
                .csrf()
                .disable()
                .exceptionHandling(httpSecurityExceptionHandlingConfigurer -> {
                    httpSecurityExceptionHandlingConfigurer.authenticationEntryPoint(new JwtAuthenticationEntryPoint(objectMapper));
                })
                // Configure authorization rules for various endpoints
                .authorizeHttpRequests()
                .anyRequest()
                .permitAll()
                .and()
                // Configure OAuth2 login
                .oauth2Login(oauth2 -> {
                    oauth2.userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig.userService(oauthUserService));
                    oauth2.loginPage(frontendConfig.getLoginUrl()).permitAll();
                    oauth2.successHandler(oAuth2LoginSuccessHandler);
                })
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(httpSecurityLogoutConfigurer -> {
                    httpSecurityLogoutConfigurer.logoutUrl("/api/v1/auth/logout");
                    httpSecurityLogoutConfigurer.addLogoutHandler(logoutHandler);
                    httpSecurityLogoutConfigurer.logoutSuccessHandler((request, response, authentication) -> SecurityContextHolder.clearContext());
                });

        return http.build();
    }
}

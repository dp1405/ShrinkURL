package org.stir.shrinkurl.config;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.stir.shrinkurl.filter.JWTAuthenticationFilter;
import org.stir.shrinkurl.utils.JWTUtil;
import org.stir.shrinkurl.utils.OAuth2Util;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2Util oAuth2Util;
    private final JWTUtil jwtUtil;
    private final JWTAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(OAuth2Util oAuth2Util, JWTUtil jwtUtil, JWTAuthenticationFilter jwtAuthenticationFilter){
        this.oAuth2Util = oAuth2Util;
        this.jwtUtil = jwtUtil;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // Or STATELESS if fully JWT
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/error", "/", "/test", "/css/**", "/js/**", "/plans", "/profile").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .successHandler(formLoginAuthenticationSuccessHandler())
                .permitAll()
            )
            .oauth2Login(oauth -> oauth
                .loginPage("/auth/login")
                .successHandler(oAuth2AuthenticationSuccessHandler())
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler(){
        return (request, response, authentication) -> {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            String jwt = jwtUtil.generateToken(oidcUser.getSubject(), oidcUser.getClaims());

            response.setContentType("application/json");
            response.getWriter().write("{\"token\": \"" + jwt + "\"}");
            SecurityContextHolder.clearContext();
        };
    }

    @Bean
    public AuthenticationSuccessHandler formLoginAuthenticationSuccessHandler(){
        return (request, response, authentication) -> {
            String username = authentication.getName();

            Map<String, Object> claims = new HashMap<>();
            claims.put("userRoles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

            String jwt = jwtUtil.generateToken(username, claims);

            response.setContentType("application/json");
            response.getWriter().write("{\"token\": \"" + jwt + "\"}");
            SecurityContextHolder.clearContext();
        };
    }

    @Bean
	public PasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder();
	}
}

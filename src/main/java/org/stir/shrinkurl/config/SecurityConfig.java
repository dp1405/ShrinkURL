package org.stir.shrinkurl.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.stir.shrinkurl.filter.JWTAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JWTAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.NEVER)) // Or STATELESS if fully JWT
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/auth/**", "/analytics/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/error/**").permitAll()
                .requestMatchers("/api/debug/**").permitAll()
                .requestMatchers("/api/admin/verify/**").permitAll()
                .requestMatchers("/api/test/**").permitAll()
                .requestMatchers("/test-dashboard").permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("/home").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .logout(logout-> logout
                .logoutSuccessUrl("/")
                .deleteCookies("access_token", "refresh_token")
                .permitAll()
            );

        return http.build();
    }

    @Bean
	public PasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder();
	}
}

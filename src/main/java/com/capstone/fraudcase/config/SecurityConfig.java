package com.capstone.fraudcase.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("agent").password(encoder.encode("agent123")).roles("AGENT").build(),
                User.withUsername("investigator").password(encoder.encode("investigator123")).roles("INVESTIGATOR").build(),
                User.withUsername("reviewer").password(encoder.encode("reviewer123")).roles("REVIEWER").build());
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/login", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cases/*/reviews").hasRole("REVIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/cases/*/investigations").hasAnyRole("INVESTIGATOR", "REVIEWER")
                        .requestMatchers(HttpMethod.POST, "/api/cases").hasAnyRole("AGENT", "INVESTIGATOR", "REVIEWER")
                        .requestMatchers("/ui/cases/*/review").hasRole("REVIEWER")
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .httpBasic(org.springframework.security.config.Customizer.withDefaults())
                .formLogin(form -> form.defaultSuccessUrl("/ui/cases", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"));
        return http.build();
    }
}

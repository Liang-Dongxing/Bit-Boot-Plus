package com.bit.monitor.admin.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

/**
 * admin 监控 安全配置
 *
 * @author Lion Li
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    private final String adminContextPath;

    public SecurityConfig(AdminServerProperties adminServerProperties) {
        this.adminContextPath = adminServerProperties.getContextPath();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(adminContextPath + "/");
        String[] requestMatchers = new String[]{adminContextPath + "/assets/**", adminContextPath + "/login", "/actuator", "/actuator/**"};
        return httpSecurity
            .headers((header) -> header.frameOptions(Customizer.withDefaults()).disable())
            .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests.requestMatchers(requestMatchers).permitAll().anyRequest().authenticated())
            .formLogin((formLogin) -> formLogin.loginPage(adminContextPath + "/login").successHandler(successHandler))
            .logout((logout) -> logout.logoutUrl(adminContextPath + "/logout"))
            .httpBasic(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .build();
    }

}

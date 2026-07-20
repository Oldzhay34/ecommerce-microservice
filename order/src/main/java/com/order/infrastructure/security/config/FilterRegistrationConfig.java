package com.order.infrastructure.security.config;

import com.order.infrastructure.security.JwtAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterRegistrationConfig {

    /**
     * JwtAuthFilter @Component olduğu için Spring Boot onu otomatik olarak
     * genel bir servlet filtresi olarak da kaydediyor (SecurityConfig'teki
     * addFilterBefore ile Spring Security zincirine eklenmesine EK olarak).
     * Bu çifte kayıt, filtrenin sıralama/çalışma zamanlaması açısından
     * beklenmedik biçimde davranmasına (authentication'ın authorization
     * kontrolüne doğru zamanda ulaşmamasına) yol açabiliyor. Bu bean,
     * otomatik genel kaydı devre dışı bırakır - filtre yalnızca
     * SecurityConfig içindeki addFilterBefore üzerinden çalışır.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> disableAutoRegistration(JwtAuthFilter jwtAuthFilter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(jwtAuthFilter);
        registration.setEnabled(false);
        return registration;
    }
}
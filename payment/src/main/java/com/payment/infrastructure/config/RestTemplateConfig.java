package com.payment.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Ödeme gateway'ine giden HTTP istemcisi.
 * <p>
 * LATENT KALIP DÜZELTMESİ: burada çıplak {@code new RestTemplate()} dönülüyordu.
 * Varsayılan {@code SimpleClientHttpRequestFactory} SINIRSIZ connect/read timeout
 * kullanır; gateway yanıt vermediğinde tahsilat isteğini işleyen thread süresiz
 * bloke olur ve @Transactional işlem (dolayısıyla DB bağlantısı) açık kalır.
 * Timeout'lar artık property ile ayarlanabilir ve varsayılan olarak sınırlıdır.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(
            @Value("${payment.gateway.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${payment.gateway.read-timeout-ms:10000}") long readTimeoutMs) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return new RestTemplate(factory);
    }
}

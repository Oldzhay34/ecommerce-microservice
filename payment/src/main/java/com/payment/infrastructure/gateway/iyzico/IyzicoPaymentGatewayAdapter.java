package com.payment.infrastructure.gateway.iyzico;

import com.payment.application.port.out.PaymentChargeCommand;
import com.payment.application.port.out.PaymentGatewayPort;
import com.payment.application.port.out.PaymentGatewayResult;
import com.payment.domain.exception.PaymentGatewayNotConfiguredException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "payment.gateway.provider", havingValue = "iyzico")
public class IyzicoPaymentGatewayAdapter implements PaymentGatewayPort {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String secretKey;

    public IyzicoPaymentGatewayAdapter(
            RestTemplate restTemplate,
            @Value("${iyzico.api-key:}") String apiKey,
            @Value("${iyzico.secret-key:}") String secretKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }

    @Override
    public PaymentGatewayResult charge(PaymentChargeCommand command) {
        throw new PaymentGatewayNotConfiguredException("Iyzico integration is not fully configured yet.");
    }
}
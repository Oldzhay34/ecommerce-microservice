package com.notificationservice;

import com.notificationservice.subsystem.AbstractNotificationSubsystemTest;
import com.notificationservice.application.port.in.SendOtpEmailUseCase;
import com.notificationservice.application.port.out.EmailSenderPort;
import com.notificationservice.application.port.out.ProcessedEventPort;
import com.notificationservice.infrastructure.messaging.listener.OtpEmailEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Context smoke testi. Uygulama gerçek altyapıya (Postgres + RabbitMQ + SMTP)
 * bağlandığı için subsystem tabanını kullanır; Docker yoksa SKIP edilir.
 */
@DisplayName("Notification Service - Uygulama context'i")
@org.junit.jupiter.api.condition.EnabledIf("com.notificationservice.support.DockerAvailability#isDockerAvailable")
class NotificationServiceApplicationTests extends AbstractNotificationSubsystemTest {

    @Autowired
    private SendOtpEmailUseCase sendOtpEmailUseCase;

    @Autowired
    private EmailSenderPort emailSenderPort;

    @Autowired
    private ProcessedEventPort processedEventPort;

    @Autowired
    private OtpEmailEventListener otpEmailEventListener;

    @Test
    @DisplayName("S0: contextLoads - Hexagonal port ve adapter bean'lerinin tamamı ayağa kalkar")
    void contextLoads_ShouldWireAllPortsAndAdapters() {
        assertThat(sendOtpEmailUseCase).isNotNull();
        assertThat(emailSenderPort).isNotNull();
        assertThat(processedEventPort).isNotNull();
        assertThat(otpEmailEventListener).isNotNull();
    }
}

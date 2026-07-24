package com.mediaservice;

import com.mediaservice.subsystem.AbstractMediaSubsystemTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Uygulama context'inin gercek altyapiya (Postgres + RabbitMQ + Redis + MinIO) karsi
 * eksiksiz ayaga kalktigini dogrular. Varsayilan (mock/gercek olmayan) datasource ile
 * calisan bir {@code @SpringBootTest} CI'da baglanti kuramazdi; bu yuzden
 * {@link AbstractMediaSubsystemTest}'i genisletir (payment/PaymentApplicationTests ile
 * ayni desen).
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 */
@EnabledIf("com.mediaservice.support.DockerAvailability#isDockerAvailable")
class MediaServiceApplicationTests extends AbstractMediaSubsystemTest {

    @Test
    void contextLoads() {
    }

}

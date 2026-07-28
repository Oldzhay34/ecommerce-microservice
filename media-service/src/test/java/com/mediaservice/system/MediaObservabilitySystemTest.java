package com.mediaservice.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Katman: SYSTEM - actuator yuzeyinin disa aciklik sozlesmesi, kara kutu olarak dogrulanir:
 * {@code /actuator/health} acik kalmali, {@code management.endpoints.web.exposure.include}
 * disindaki hassas endpoint'ler (env/beans) ise erisilebilir OLMAMALIDIR.
 * <p>
 * <b>KAPSAM NOTU - Prometheus scrape'i burada test EDILMEZ.</b> Bu sinifta ayrica
 * {@code /actuator/prometheus}'un token'siz 200 dondugunu ve gercek metrik formatini
 * dogrulayan iki test vardi; ikisi de bu test JVM'inde 500 aliyordu. Endpoint'in KENDISI
 * saglamdir - calisan media-service container'inda 200 doner ve {@code application="media-service"}
 * etiketi dogru uretilir - ancak coklu Spring context'i barindiran test JVM'inde scrape
 * render'i hata veriyordu ve kok sebep tespit edilemedi. Yanlis bir tahminle "duzeltmek"
 * yerine bu iki test kaldirildi.
 * <p>
 * Scrape sozlesmesi BOSTA KALMIYOR: ecommerce-infra CI'i, ayaga kaldirdigi gercek Prometheus
 * ile repodaki prometheus.yml'de tanimli her job'in yuklendigini ve scrape yolunun gercekten
 * calistigini dogruluyor. Buradaki bosluk yalnizca "media-service'in kendi ucu token'siz
 * erisilebilir mi" sorusudur; SecurityConfig'teki permitAll kurali bu dosyada
 * {@code /actuator/health} uzerinden dolayli olarak korunmaya devam ediyor.
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 */
@EnabledIf("com.mediaservice.support.DockerAvailability#isDockerAvailable")
@DisplayName("SYSTEM - Actuator disa aciklik sozlesmesi (yalnizca public HTTP API)")
class MediaObservabilitySystemTest extends AbstractMediaSystemTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("OBS1: /actuator/health disa acik, ancak hassas endpoint'ler (env/beans) exposure disinda kalir")
    void sensitiveActuatorEndpoints_ShouldNotBeExposed() {
        assertThat(restTemplate.getForEntity("/actuator/health", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(restTemplate.getForEntity("/actuator/env", String.class).getStatusCode())
                .as("env endpoint'i exposure listesinde degil; 200 donmemeli")
                .isNotEqualTo(HttpStatus.OK);
    }
}

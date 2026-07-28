package com.mediaservice.system;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Katman: SYSTEM - Prometheus scrape sozlesmesi. ecommerce-infra/prometheus/prometheus.yml
 * bu servisi {@code media-service:8086/actuator/prometheus} uzerinden, KIMLIK DOGRULAMASIZ
 * scrape eder. Bu test tam olarak o sozlesmeyi kara kutu olarak dogrular: endpoint acik mi,
 * token'siz erisilebilir mi ve gercekten Prometheus text formatinda metrik doner mi.
 * <p>
 * Boylece {@code management.endpoints.web.exposure.include} veya SecurityConfig'teki
 * permitAll kurallari ileride sessizce daraltilirsa build kirilir - Grafana panolarinin
 * sessizce bosalmasi yerine.
 * <p>
 * <b>CALISTIRMA ONKOSULU: Docker.</b>
 */
@EnabledIf("com.mediaservice.support.DockerAvailability#isDockerAvailable")
@DisplayName("SYSTEM - Prometheus scrape endpoint'i (token'siz, gercek metrik formati)")
class MediaObservabilitySystemTest extends AbstractMediaSystemTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("OBS1: /actuator/prometheus token OLMADAN 200 doner ve Prometheus text formatinda JVM metrikleri icerir")
    void prometheusEndpoint_ShouldBePubliclyScrapable() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode())
                .as("Prometheus scrape'i token gondermez; endpoint permitAll olmali")
                .isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("# TYPE jvm_memory_used_bytes")
                .contains("application=\"media-service\"");
    }

    @Test
    @DisplayName("OBS2: Public bir istek sonrasi http_server_requests metrikleri GERCEKTEN kaydedilir")
    void httpServerRequestsMetric_ShouldBeRecordedAfterRealTraffic() {
        restTemplate.getForEntity("/api/v1/media/products/{productId}/images", String.class, UUID.randomUUID());

        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .as("Grafana panolari http_server_requests_seconds_* uzerine kurulu")
                .contains("http_server_requests_seconds_count")
                .contains("http_server_requests_seconds_bucket");
    }

    @Test
    @DisplayName("OBS3: /actuator/health disa acik, ancak hassas endpoint'ler (env/beans) exposure disinda kalir")
    void sensitiveActuatorEndpoints_ShouldNotBeExposed() {
        assertThat(restTemplate.getForEntity("/actuator/health", String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(restTemplate.getForEntity("/actuator/env", String.class).getStatusCode())
                .as("env endpoint'i exposure listesinde degil; 200 donmemeli")
                .isNotEqualTo(HttpStatus.OK);
    }
}

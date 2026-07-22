package com.notificationservice.integration;

import com.notificationservice.domain.model.ProcessedEvent;
import com.notificationservice.domain.model.ProcessedEventStatus;
import com.notificationservice.infrastructure.persistence.adapter.ProcessedEventJdbcAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Adapter entegrasyon testi: ProcessedEventJdbcAdapter GERÇEK bir PostgreSQL'e karşı
 * (Testcontainers) çalıştırılır; SQL'in ve şemanın uyumu doğrulanır.
 * <p>
 * Docker yoksa SKIP edilir.
 */
@DisplayName("Notification Service - Integration: ProcessedEventJdbcAdapter x PostgreSQL")
@Testcontainers
@EnabledIf("com.notificationservice.support.DockerAvailability#isDockerAvailable")
class ProcessedEventJdbcAdapterIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("notification_db_test")
            .withUsername("test")
            .withPassword("test");

    private JdbcTemplate jdbcTemplate;
    private ProcessedEventJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());

        jdbcTemplate = new JdbcTemplate(dataSource);
        // Production şemasının ta kendisi (src/main/resources/schema.sql ile birebir).
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS processed_event (
                    idempotency_key VARCHAR(255) PRIMARY KEY,
                    event_type VARCHAR(100) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("TRUNCATE TABLE processed_event");

        adapter = new ProcessedEventJdbcAdapter(jdbcTemplate);
    }

    private ProcessedEvent event(String key, ProcessedEventStatus status, LocalDateTime createdAt) {
        return new ProcessedEvent(key, "auth.event.otp", status, createdAt);
    }

    @Test
    @DisplayName("I7: isEventProcessed - Kayıt yokken false döner (ilk teslimat işlenmeye devam eder)")
    void isEventProcessed_WhenKeyAbsent_ShouldReturnFalse() {
        assertThat(adapter.isEventProcessed("olmayan-key")).isFalse();
    }

    @Test
    @DisplayName("I8: save + isEventProcessed - Kaydedilen event sonrasında true döner (idempotency guard'ı çalışır)")
    void isEventProcessed_WhenEventSaved_ShouldReturnTrue() {
        adapter.save(event("key-1", ProcessedEventStatus.SENT, LocalDateTime.now()));

        assertThat(adapter.isEventProcessed("key-1")).isTrue();
        assertThat(adapter.isEventProcessed("key-2")).isFalse();
    }

    @Test
    @DisplayName("I9: save - Tüm kolonlar (event_type, status, created_at) doğru değerlerle yazılır")
    void save_WhenEventPersisted_ShouldWriteAllColumnsCorrectly() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 21, 10, 15, 30);
        adapter.save(event("key-columns", ProcessedEventStatus.SENT, createdAt));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT idempotency_key, event_type, status, created_at FROM processed_event WHERE idempotency_key = ?",
                "key-columns");

        assertThat(row.get("idempotency_key")).isEqualTo("key-columns");
        assertThat(row.get("event_type")).isEqualTo("auth.event.otp");
        assertThat(row.get("status")).isEqualTo("SENT");
        assertThat(((Timestamp) row.get("created_at")).toLocalDateTime()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("I10: save - FAILED durumu enum adıyla ('FAILED') persist edilir")
    void save_WhenStatusIsFailed_ShouldPersistEnumName() {
        adapter.save(event("key-failed", ProcessedEventStatus.FAILED, LocalDateTime.now()));

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM processed_event WHERE idempotency_key = ?", String.class, "key-failed");

        assertThat(status).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("I11: save - Aynı idempotency key ikinci kez yazılamaz; PK ihlali DuplicateKeyException'a döner")
    void save_WhenSameKeyInsertedTwice_ShouldViolatePrimaryKey() {
        adapter.save(event("key-dup", ProcessedEventStatus.SENT, LocalDateTime.now()));

        assertThatThrownBy(() -> adapter.save(event("key-dup", ProcessedEventStatus.SENT, LocalDateTime.now())))
                .isInstanceOf(DuplicateKeyException.class);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM processed_event WHERE idempotency_key = ?", Integer.class, "key-dup");
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("I12: isEventProcessed - Key eşleşmesi tam (exact) yapılır; benzer prefix'ler false döner")
    void isEventProcessed_WhenKeyIsOnlyAPrefix_ShouldReturnFalse() {
        adapter.save(event("msg-100-customer@shopbridge.com", ProcessedEventStatus.SENT, LocalDateTime.now()));

        assertThat(adapter.isEventProcessed("msg-100-customer@shopbridge.com")).isTrue();
        assertThat(adapter.isEventProcessed("msg-100")).isFalse();
        assertThat(adapter.isEventProcessed("msg-1")).isFalse();
    }

    @Test
    @DisplayName("I13: isEventProcessed - SQL injection denemesi veri değil düz parametre olarak ele alınır")
    void isEventProcessed_WhenKeyContainsSqlPayload_ShouldBeTreatedAsPlainParameter() {
        adapter.save(event("safe-key", ProcessedEventStatus.SENT, LocalDateTime.now()));

        assertThat(adapter.isEventProcessed("' OR '1'='1")).isFalse();

        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM processed_event", Integer.class);
        assertThat(total).isEqualTo(1);
    }

    @Test
    @DisplayName("I14: save - Base64 fallback hash gibi özel karakterli uzun key'ler sorunsuz saklanır")
    void save_WhenKeyIsBase64Hash_ShouldPersistAndBeFound() {
        String base64Key = "7UMmrDANO4GvCeWXlaWJ2kLdTVqyOdC57YMHEeuDD2M=";
        adapter.save(event(base64Key, ProcessedEventStatus.SENT, LocalDateTime.now()));

        assertThat(adapter.isEventProcessed(base64Key)).isTrue();
    }
}

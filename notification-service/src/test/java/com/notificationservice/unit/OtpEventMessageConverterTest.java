package com.notificationservice.unit;

import com.notificationservice.domain.model.OtpEmailPayload;
import com.notificationservice.infrastructure.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("Notification Service - Unit: RabbitMQ JSON MessageConverter (OTP event deserializasyonu)")
class OtpEventMessageConverterTest {

    private MessageConverter converter;

    @BeforeEach
    void setUp() {
        converter = new RabbitMQConfig().jsonMessageConverter();
    }

    private Message jsonMessage(String json) {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding(StandardCharsets.UTF_8.name());
        // Listener metot imzasından çıkarılan hedef tip (Spring AMQP runtime'da bunu set eder).
        props.setInferredArgumentType(OtpEmailPayload.class);
        return new Message(json.getBytes(StandardCharsets.UTF_8), props);
    }

    @Test
    @DisplayName("U24: fromMessage - Standart OTP JSON payload'ı OtpEmailPayload'a dönüştürülür")
    void fromMessage_WhenStandardOtpJson_ShouldDeserializeIntoPayload() {
        Message message = jsonMessage("{\"email\":\"customer@shopbridge.com\",\"otpCode\":\"483920\"}");

        Object result = converter.fromMessage(message);

        assertThat(result).isInstanceOf(OtpEmailPayload.class);
        OtpEmailPayload payload = (OtpEmailPayload) result;
        assertThat(payload.getEmail()).isEqualTo("customer@shopbridge.com");
        assertThat(payload.getOtpCode()).isEqualTo("483920");
    }

    @Test
    @DisplayName("U25: fromMessage - Publisher tarafı yeni alan eklerse (şema evrimi) mesaj DLQ'ya düşmeden okunabilmelidir")
    void fromMessage_WhenPayloadHasUnknownExtraFields_ShouldStillDeserialize() {
        Message message = jsonMessage(
                "{\"email\":\"customer@shopbridge.com\",\"otpCode\":\"483920\",\"channel\":\"EMAIL\",\"attempt\":2}");

        assertThatCode(() -> converter.fromMessage(message)).doesNotThrowAnyException();

        OtpEmailPayload payload = (OtpEmailPayload) converter.fromMessage(message);
        assertThat(payload.getEmail()).isEqualTo("customer@shopbridge.com");
        assertThat(payload.getOtpCode()).isEqualTo("483920");
    }

    @Test
    @DisplayName("U26: fromMessage - Publisher LocalDateTime içeren bir event gönderirse JavaTimeModule sayesinde patlamaz")
    void fromMessage_WhenPayloadContainsIso8601Timestamp_ShouldNotFailOnJavaTimeType() {
        Message message = jsonMessage(
                "{\"email\":\"customer@shopbridge.com\",\"otpCode\":\"483920\",\"issuedAt\":\"2026-07-21T10:15:30\"}");

        assertThatCode(() -> converter.fromMessage(message)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("U27: toMessage/fromMessage - LocalDateTime alanı olan nesne hatasız round-trip eder (çıplak ObjectMapper regresyon guard'ı)")
    void toMessageAndFromMessage_WhenObjectContainsLocalDateTime_ShouldRoundTripWithoutError() {
        java.time.LocalDateTime issuedAt = java.time.LocalDateTime.of(2026, 7, 21, 10, 15, 30);
        EventWithTime source = new EventWithTime("customer@shopbridge.com", issuedAt);

        // Çıplak `new ObjectMapper()` kullanılsaydı burada JavaTimeModule eksikliğinden
        // InvalidDefinitionException alınırdı.
        Message message = converter.toMessage(source, new MessageProperties());
        message.getMessageProperties().setInferredArgumentType(EventWithTime.class);

        Object result = converter.fromMessage(message);

        assertThat(result).isInstanceOf(EventWithTime.class);
        assertThat(((EventWithTime) result).issuedAt()).isEqualTo(issuedAt);
    }

    public record EventWithTime(String email, java.time.LocalDateTime issuedAt) {}

    @Test
    @DisplayName("U28: fromMessage - Tamamen bozuk (JSON olmayan) gövde hata fırlatır, mesaj DLQ'ya yönlendirilir")
    void fromMessage_WhenBodyIsNotValidJson_ShouldThrowConversionException() {
        Message message = jsonMessage("bu-bir-json-degil");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> converter.fromMessage(message))
                .isInstanceOf(org.springframework.amqp.support.converter.MessageConversionException.class);
    }
}

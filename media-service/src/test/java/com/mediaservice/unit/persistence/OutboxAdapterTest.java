package com.mediaservice.unit.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediaservice.infrastructure.persistence.adapter.OutboxAdapter;
import com.mediaservice.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.mediaservice.infrastructure.persistence.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - Transactional Outbox yazim adaptoru, repository mock'lanir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - OutboxAdapter")
class OutboxAdapterTest {

    @Mock private OutboxEventRepository repository;

    private OutboxAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OutboxAdapter(repository, new ObjectMapper());
    }

    @Test
    @DisplayName("U1: append - Payload JSON'a serilestirilir ve tum alanlar dogru doldurulur")
    void append_ShouldPersistSerializedPayloadWithAllFields() {
        UUID productId = UUID.randomUUID();
        Map<String, Object> payload = Map.of("productId", productId.toString(), "imageCount", 2);

        adapter.append("MediaAsset", productId, "MediaUploadedEvent", "media.uploaded", payload);

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        verify(repository).save(captor.capture());

        OutboxEventJpaEntity saved = captor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAggregateType()).isEqualTo("MediaAsset");
        assertThat(saved.getAggregateId()).isEqualTo(productId);
        assertThat(saved.getEventType()).isEqualTo("MediaUploadedEvent");
        assertThat(saved.getRoutingKey()).isEqualTo("media.uploaded");
        assertThat(saved.isProcessed()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getPayload()).contains(productId.toString()).contains("\"imageCount\":2");
    }

    @Test
    @DisplayName("U2: append - Serilestirme basarisiz olursa IllegalStateException firlatir, repository'ye YAZILMAZ")
    void append_WhenSerializationFails_ShouldThrowAndNotPersist() throws JsonProcessingException {
        ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.databind.JsonMappingException(null, "boom"));
        OutboxAdapter failingAdapter = new OutboxAdapter(repository, failingMapper);

        assertThatThrownBy(() -> failingAdapter.append("MediaAsset", UUID.randomUUID(),
                "MediaUploadedEvent", "media.uploaded", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Outbox payload");

        verify(repository, org.mockito.Mockito.never()).save(any());
    }
}

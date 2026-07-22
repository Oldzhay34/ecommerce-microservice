package com.cart.unit;

import com.cart.domain.model.Cart;
import com.cart.domain.model.CartItem;
import com.cart.infrastructure.persistence.adapter.CartPersistenceAdapter;
import com.cart.infrastructure.persistence.entity.CartJpaEntity;
import com.cart.infrastructure.persistence.entity.OutboxEventJpaEntity;
import com.cart.infrastructure.persistence.mapper.CartEntityMapper;
import com.cart.infrastructure.persistence.repository.CartRepository;
import com.cart.infrastructure.persistence.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT. Repository'ler mock'lanır; adapter'ın port sözleşmesini
 * mapper üzerinden doğru şekilde köprülediği doğrulanır.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - CartPersistenceAdapter")
class CartPersistenceAdapterTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private CartPersistenceAdapter adapter;
    private UUID userId;

    @BeforeEach
    void setUp() {
        adapter = new CartPersistenceAdapter(cartRepository, outboxEventRepository, new CartEntityMapper());
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("U58: save - Domain modeli entity'ye çevrilip kaydedilir ve tekrar domain olarak döner")
    void save_ShouldMapToEntityPersistAndMapBack() {
        Cart domain = new Cart(1L, userId,
                new ArrayList<>(List.of(new CartItem(null, UUID.randomUUID(), 2, new BigDecimal("10.00")))),
                new BigDecimal("20.00"));
        when(cartRepository.save(any(CartJpaEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Cart result = adapter.save(domain);

        ArgumentCaptor<CartJpaEntity> captor = ArgumentCaptor.forClass(CartJpaEntity.class);
        org.mockito.Mockito.verify(cartRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getItems()).hasSize(1);

        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("U59: saveOutboxEvent - Kayıt processed=false ve createdAt dolu olarak yazılır")
    void saveOutboxEvent_ShouldPersistUnprocessedEventWithTimestamp() {
        adapter.saveOutboxEvent(userId.toString(), "CartUpdatedEvent", "{\"a\":1}");

        ArgumentCaptor<OutboxEventJpaEntity> captor = ArgumentCaptor.forClass(OutboxEventJpaEntity.class);
        org.mockito.Mockito.verify(outboxEventRepository).save(captor.capture());
        OutboxEventJpaEntity saved = captor.getValue();

        assertThat(saved.getAggregateId()).isEqualTo(userId.toString());
        assertThat(saved.getEventType()).isEqualTo("CartUpdatedEvent");
        assertThat(saved.getPayload()).isEqualTo("{\"a\":1}");
        assertThat(saved.isProcessed()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("U60: findByUserId - Kayıt varsa domain modele çevrilmiş Optional döner")
    void findByUserId_WhenCartExists_ShouldReturnMappedDomain() {
        CartJpaEntity entity = new CartJpaEntity();
        entity.setId(3L);
        entity.setUserId(userId);
        entity.setTotalAmount(new BigDecimal("5.00"));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(entity));

        Optional<Cart> result = adapter.findByUserId(userId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(3L);
        assertThat(result.get().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("U61: findByUserId - Kayıt yoksa boş Optional döner")
    void findByUserId_WhenCartMissing_ShouldReturnEmptyOptional() {
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThat(adapter.findByUserId(userId)).isEmpty();
    }
}

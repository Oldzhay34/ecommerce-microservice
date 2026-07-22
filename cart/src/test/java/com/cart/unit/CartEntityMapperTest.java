package com.cart.unit;

import com.cart.domain.model.Cart;
import com.cart.domain.model.CartItem;
import com.cart.infrastructure.persistence.entity.CartItemJpaEntity;
import com.cart.infrastructure.persistence.entity.CartJpaEntity;
import com.cart.infrastructure.persistence.mapper.CartEntityMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Katman: UNIT. Mapper saf fonksiyon; null guard'ları ve @OneToMany geri
 * referansının (item.cart) kurulup kurulmadığı kritik - kurulmazsa kayıt
 * sırasında cart_id NOT NULL ihlali alınır.
 */
@DisplayName("UNIT - CartEntityMapper")
class CartEntityMapperTest {

    private final CartEntityMapper mapper = new CartEntityMapper();

    @Test
    @DisplayName("U21: toDomain - null entity için null döner")
    void toDomain_WhenEntityIsNull_ShouldReturnNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    @DisplayName("U22: toEntity - null domain için null döner")
    void toEntity_WhenDomainIsNull_ShouldReturnNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    @DisplayName("U23: toDomain - Tüm alanlar ve satırlar domain modeline taşınır")
    void toDomain_ShouldCopyAllFieldsAndItems() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CartJpaEntity entity = new CartJpaEntity();
        entity.setId(7L);
        entity.setUserId(userId);
        entity.setTotalAmount(new BigDecimal("42.00"));

        CartItemJpaEntity itemEntity = new CartItemJpaEntity();
        itemEntity.setId(3L);
        itemEntity.setProductId(productId);
        itemEntity.setQuantity(2);
        itemEntity.setPrice(new BigDecimal("21.00"));
        entity.addItem(itemEntity);

        Cart domain = mapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(7L);
        assertThat(domain.getUserId()).isEqualTo(userId);
        assertThat(domain.getTotalAmount()).isEqualByComparingTo("42.00");
        assertThat(domain.getItems()).hasSize(1);
        assertThat(domain.getItems().get(0).getId()).isEqualTo(3L);
        assertThat(domain.getItems().get(0).getProductId()).isEqualTo(productId);
        assertThat(domain.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(domain.getItems().get(0).getPrice()).isEqualByComparingTo("21.00");
    }

    @Test
    @DisplayName("U24: toEntity - Her satır için cart geri referansı kurulur (cart_id NOT NULL ihlali önlenir)")
    void toEntity_ShouldSetBackReferenceOnEveryItem() {
        UUID userId = UUID.randomUUID();
        Cart domain = new Cart(5L, userId,
                new ArrayList<>(List.of(
                        new CartItem(null, UUID.randomUUID(), 1, new BigDecimal("1.00")),
                        new CartItem(9L, UUID.randomUUID(), 4, new BigDecimal("2.50")))),
                new BigDecimal("11.00"));

        CartJpaEntity entity = mapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(5L);
        assertThat(entity.getUserId()).isEqualTo(userId);
        assertThat(entity.getItems()).hasSize(2);
        assertThat(entity.getItems()).allSatisfy(item -> assertThat(item.getCart()).isSameAs(entity));
    }

    @Test
    @DisplayName("U25: toEntity - Domain items null ise entity boş koleksiyonla döner")
    void toEntity_WhenDomainItemsAreNull_ShouldReturnEntityWithEmptyItems() {
        Cart domain = new Cart();
        domain.setUserId(UUID.randomUUID());
        domain.setItems(null);

        CartJpaEntity entity = mapper.toEntity(domain);

        assertThat(entity.getItems()).isEmpty();
    }

    @Test
    @DisplayName("U26: toDomain -> toEntity gidiş-dönüşü alanları bozmaz")
    void roundTrip_ShouldPreserveFields() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CartJpaEntity entity = new CartJpaEntity();
        entity.setId(1L);
        entity.setUserId(userId);
        entity.setTotalAmount(new BigDecimal("30.00"));
        CartItemJpaEntity itemEntity = new CartItemJpaEntity();
        itemEntity.setId(2L);
        itemEntity.setProductId(productId);
        itemEntity.setQuantity(3);
        itemEntity.setPrice(new BigDecimal("10.00"));
        entity.addItem(itemEntity);

        CartJpaEntity roundTripped = mapper.toEntity(mapper.toDomain(entity));

        assertThat(roundTripped.getUserId()).isEqualTo(userId);
        assertThat(roundTripped.getTotalAmount()).isEqualByComparingTo("30.00");
        assertThat(roundTripped.getItems()).hasSize(1);
        assertThat(roundTripped.getItems().get(0).getProductId()).isEqualTo(productId);
    }
}

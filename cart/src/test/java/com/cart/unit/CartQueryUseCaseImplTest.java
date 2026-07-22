package com.cart.unit;

import com.cart.api.dto.CartResponse;
import com.cart.application.port.out.CartCachePort;
import com.cart.application.port.out.CartQueryPort;
import com.cart.application.usecase.CartQueryUseCaseImpl;
import com.cart.domain.model.Cart;
import com.cart.domain.model.CartItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT.
 * Hedef: cache HIT / cache MISS+DB HIT / cache MISS+DB MISS üçlüsünün tamamı ve
 * mapToResponse'un null-items guard'ı.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - CartQueryUseCaseImpl")
class CartQueryUseCaseImplTest {

    @Mock
    private CartQueryPort cartQueryPort;

    @Mock
    private CartCachePort cartCachePort;

    @InjectMocks
    private CartQueryUseCaseImpl useCase;

    private UUID userId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    private Cart sampleCart() {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>(List.of(new CartItem(1L, productId, 2, new BigDecimal("12.50")))));
        cart.setTotalAmount(new BigDecimal("25.00"));
        return cart;
    }

    @Test
    @DisplayName("U16: getCart - Cache HIT durumunda veritabanına hiç gidilmez")
    void getCart_WhenCacheHit_ShouldNotTouchDatabase() {
        when(cartCachePort.getCartByUserId(userId)).thenReturn(Optional.of(sampleCart()));

        CartResponse response = useCase.getCart(userId);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("25.00");
        verifyNoInteractions(cartQueryPort);
        verify(cartCachePort, never()).saveCart(any());
    }

    @Test
    @DisplayName("U17: getCart - Cache MISS + DB HIT durumunda sonuç cache'e yazılır")
    void getCart_WhenCacheMissAndDatabaseHit_ShouldWarmCache() {
        Cart dbCart = sampleCart();
        when(cartCachePort.getCartByUserId(userId)).thenReturn(Optional.empty());
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.of(dbCart));

        CartResponse response = useCase.getCart(userId);

        assertThat(response.getItems()).hasSize(1);
        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartCachePort).saveCart(captor.capture());
        assertThat(captor.getValue()).isSameAs(dbCart);
    }

    @Test
    @DisplayName("U18: getCart - Cache MISS + DB MISS durumunda boş sepet döner ve boş sepet cache'lenir")
    void getCart_WhenCacheMissAndDatabaseMiss_ShouldReturnEmptyCart() {
        when(cartCachePort.getCartByUserId(userId)).thenReturn(Optional.empty());
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.empty());

        CartResponse response = useCase.getCart(userId);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalAmount()).isEqualByComparingTo("0");
        verify(cartCachePort).saveCart(any(Cart.class));
    }

    @Test
    @DisplayName("U19: getCart - Sepetin items alanı null ise response.items da null kalır (null guard)")
    void getCart_WhenCartItemsAreNull_ShouldLeaveResponseItemsNull() {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setItems(null);
        cart.setTotalAmount(BigDecimal.ZERO);
        when(cartCachePort.getCartByUserId(userId)).thenReturn(Optional.of(cart));

        CartResponse response = useCase.getCart(userId);

        assertThat(response.getItems()).isNull();
        assertThat(response.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("U20: getCart - Satır alanları (productId, quantity, price) response'a birebir taşınır")
    void getCart_ShouldMapItemFieldsOneToOne() {
        when(cartCachePort.getCartByUserId(userId)).thenReturn(Optional.of(sampleCart()));

        CartResponse response = useCase.getCart(userId);

        assertThat(response.getItems().get(0).getProductId()).isEqualTo(productId);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getItems().get(0).getPrice()).isEqualByComparingTo("12.50");
    }
}

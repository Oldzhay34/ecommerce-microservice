package com.cart.unit;

import com.cart.api.dto.AddToCartRequest;
import com.cart.api.dto.UpdateCartItemRequest;
import com.cart.application.port.out.CartCachePort;
import com.cart.application.port.out.CartCommandPort;
import com.cart.application.port.out.CartQueryPort;
import com.cart.application.usecase.CartCommandUseCaseImpl;
import com.cart.domain.model.Cart;
import com.cart.domain.model.CartItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT (saf Mockito, altyapı yok).
 * Hedef: CartCommandUseCaseImpl'in tüm branch/guard'ları - yeni sepet yaratma,
 * duplicate item birleştirme, miktar<=0 silme kuralı, boş sepet guard'ı,
 * outbox event tipi ve cache invalidation sırası.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - CartCommandUseCaseImpl")
class CartCommandUseCaseImplTest {

    @Mock
    private CartCommandPort cartCommandPort;

    @Mock
    private CartQueryPort cartQueryPort;

    @Mock
    private CartCachePort cartCachePort;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private CartCommandUseCaseImpl useCase;

    private UUID userId;
    private UUID productA;
    private UUID productB;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productA = UUID.randomUUID();
        productB = UUID.randomUUID();
    }

    private Cart cartWith(CartItem... items) {
        Cart cart = new Cart();
        cart.setId(1L);
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>(List.of(items)));
        return cart;
    }

    private AddToCartRequest addRequest(UUID productId, int quantity, String price) {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        request.setPrice(new BigDecimal(price));
        return request;
    }

    private void echoSave() {
        when(cartCommandPort.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("U1: addItemToCart - Sepet yoksa yeni sepet yaratıp ürünü ekler ve toplamı hesaplar")
    void addItemToCart_WhenCartDoesNotExist_ShouldCreateCartAndAddItem() {
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.empty());
        echoSave();

        useCase.addItemToCart(userId, addRequest(productA, 3, "10.00"));

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartCommandPort).save(captor.capture());
        Cart saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getProductId()).isEqualTo(productA);
        assertThat(saved.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("U2: addItemToCart - Aynı ürün tekrar eklenirse yeni satır açılmaz, miktar toplanır (duplicate guard)")
    void addItemToCart_WhenProductAlreadyInCart_ShouldMergeQuantityInsteadOfDuplicating() {
        Cart existing = cartWith(new CartItem(10L, productA, 2, new BigDecimal("10.00")));
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        echoSave();

        useCase.addItemToCart(userId, addRequest(productA, 5, "10.00"));

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartCommandPort).save(captor.capture());
        Cart saved = captor.getValue();

        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getQuantity()).isEqualTo(7);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("70.00");
    }

    @Test
    @DisplayName("U3: addItemToCart - Farklı ürün eklenirse yeni satır açılır ve toplam iki satırdan hesaplanır")
    void addItemToCart_WhenDifferentProduct_ShouldAppendNewLineAndRecalculateTotal() {
        Cart existing = cartWith(new CartItem(10L, productA, 2, new BigDecimal("10.00")));
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        echoSave();

        useCase.addItemToCart(userId, addRequest(productB, 1, "25.50"));

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartCommandPort).save(captor.capture());
        Cart saved = captor.getValue();

        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("45.50");
    }

    @Test
    @DisplayName("U4: addItemToCart - CartUpdatedEvent outbox kaydı yazar ve payload sepet verisini içerir")
    void addItemToCart_ShouldWriteCartUpdatedOutboxEventWithCartPayload() {
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.empty());
        echoSave();

        useCase.addItemToCart(userId, addRequest(productA, 2, "10.00"));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(cartCommandPort).saveOutboxEvent(eq(userId.toString()), eq("CartUpdatedEvent"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .contains(userId.toString())
                .contains(productA.toString());
    }

    @Test
    @DisplayName("U5: addItemToCart - Yazma sonrası Redis cache invalidate edilir")
    void addItemToCart_ShouldInvalidateCacheAfterWrite() {
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.empty());
        echoSave();

        useCase.addItemToCart(userId, addRequest(productA, 1, "5.00"));

        verify(cartCachePort).invalidateCache(userId);
    }

    @Test
    @DisplayName("U6: addItemToCart - Payload serileştirilemezse RuntimeException fırlatır ve cache invalidate edilmez")
    void addItemToCart_WhenPayloadSerializationFails_ShouldThrowAndSkipCacheInvalidation() throws Exception {
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.empty());
        echoSave();
        doThrow(new JsonProcessingException("boom") {
        }).when(objectMapper).writeValueAsString(any(Cart.class));

        assertThatThrownBy(() -> useCase.addItemToCart(userId, addRequest(productA, 1, "5.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Event serialization failed");

        verify(cartCachePort, never()).invalidateCache(any());
    }

    @Test
    @DisplayName("U7: updateCartItemQuantity - Pozitif miktar verilirse satırın miktarı güncellenir")
    void updateCartItemQuantity_WhenQuantityPositive_ShouldUpdateLine() {
        Cart existing = cartWith(new CartItem(10L, productA, 2, new BigDecimal("10.00")));
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        echoSave();

        useCase.updateCartItemQuantity(userId, productA, new UpdateCartItemRequest(4));

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartCommandPort).save(captor.capture());
        assertThat(captor.getValue().getItems()).hasSize(1);
        assertThat(captor.getValue().getItems().get(0).getQuantity()).isEqualTo(4);
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("U8: updateCartItemQuantity - Miktar 0 verilirse satır sepetten çıkarılır")
    void updateCartItemQuantity_WhenQuantityZero_ShouldRemoveLine() {
        Cart existing = cartWith(
                new CartItem(10L, productA, 2, new BigDecimal("10.00")),
                new CartItem(11L, productB, 1, new BigDecimal("7.00")));
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        echoSave();

        useCase.updateCartItemQuantity(userId, productA, new UpdateCartItemRequest(0));

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartCommandPort).save(captor.capture());
        assertThat(captor.getValue().getItems()).hasSize(1);
        assertThat(captor.getValue().getItems().get(0).getProductId()).isEqualTo(productB);
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("7.00");
    }

    @Test
    @DisplayName("U9: updateCartItemQuantity - Negatif miktar da satırı sepetten çıkarır (miktar<=0 guard)")
    void updateCartItemQuantity_WhenQuantityNegative_ShouldRemoveLine() {
        Cart existing = cartWith(new CartItem(10L, productA, 2, new BigDecimal("10.00")));
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        echoSave();

        useCase.updateCartItemQuantity(userId, productA, new UpdateCartItemRequest(-3));

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartCommandPort).save(captor.capture());
        assertThat(captor.getValue().getItems()).isEmpty();
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("U10: updateCartItemQuantity - Sepette olmayan ürün için hiçbir satır değişmez (sessiz no-op)")
    void updateCartItemQuantity_WhenProductNotInCart_ShouldLeaveItemsUnchanged() {
        Cart existing = cartWith(new CartItem(10L, productA, 2, new BigDecimal("10.00")));
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        echoSave();

        useCase.updateCartItemQuantity(userId, productB, new UpdateCartItemRequest(9));

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartCommandPort).save(captor.capture());
        assertThat(captor.getValue().getItems()).hasSize(1);
        assertThat(captor.getValue().getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("U11: removeCartItem - Belirtilen ürün silinir ve toplam yeniden hesaplanır")
    void removeCartItem_WhenProductExists_ShouldRemoveAndRecalculateTotal() {
        Cart existing = cartWith(
                new CartItem(10L, productA, 2, new BigDecimal("10.00")),
                new CartItem(11L, productB, 3, new BigDecimal("5.00")));
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        echoSave();

        useCase.removeCartItem(userId, productA);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartCommandPort).save(captor.capture());
        assertThat(captor.getValue().getItems()).hasSize(1);
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("15.00");
        verify(cartCachePort).invalidateCache(userId);
    }

    @Test
    @DisplayName("U12: removeCartItem - Sepette olmayan ürün silinmeye çalışılırsa sepet aynı kalır")
    void removeCartItem_WhenProductNotInCart_ShouldKeepCartUnchanged() {
        Cart existing = cartWith(new CartItem(10L, productA, 2, new BigDecimal("10.00")));
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        echoSave();

        useCase.removeCartItem(userId, productB);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartCommandPort).save(captor.capture());
        assertThat(captor.getValue().getItems()).hasSize(1);
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("U13: clearCart - Sepet varsa tüm satırlar silinir, toplam sıfırlanır ve CartClearedEvent yazılır")
    void clearCart_WhenCartExists_ShouldEmptyCartAndPublishClearedEvent() {
        Cart existing = cartWith(new CartItem(10L, productA, 2, new BigDecimal("10.00")));
        existing.setTotalAmount(new BigDecimal("20.00"));
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(cartCommandPort.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.clearCart(userId);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartCommandPort).save(captor.capture());
        assertThat(captor.getValue().getItems()).isEmpty();
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("0");
        verify(cartCommandPort).saveOutboxEvent(eq(userId.toString()), eq("CartClearedEvent"), any());
        verify(cartCachePort).invalidateCache(userId);
    }

    @Test
    @DisplayName("U14: clearCart - Sepet yoksa hiçbir yazma yapılmaz (boş sepet guard'ı)")
    void clearCart_WhenCartDoesNotExist_ShouldDoNothing() {
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.empty());

        useCase.clearCart(userId);

        verify(cartCommandPort, never()).save(any());
        verify(cartCommandPort, never()).saveOutboxEvent(any(), any(), any());
        verify(cartCachePort, never()).invalidateCache(any());
    }

    @Test
    @DisplayName("U15: clearCart - Aynı userId için iki kez çağrılması hata üretmez (listener idempotency güvencesi)")
    void clearCart_WhenCalledTwice_ShouldRemainIdempotentWithoutError() {
        Cart existing = cartWith(new CartItem(10L, productA, 2, new BigDecimal("10.00")));
        when(cartQueryPort.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(cartCommandPort.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.clearCart(userId);
        useCase.clearCart(userId);

        verify(cartCommandPort, org.mockito.Mockito.times(2)).save(any(Cart.class));
        assertThat(existing.getItems()).isEmpty();
        assertThat(existing.getTotalAmount()).isEqualByComparingTo("0");
    }
}

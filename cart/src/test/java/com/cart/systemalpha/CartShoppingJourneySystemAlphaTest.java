package com.cart.systemalpha;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Katman: SYSTEM ALPHA - uçtan uca, BLACK-BOX.
 *
 * Kural: teste SADECE public HTTP API'den girilir. Hiçbir repository, use case
 * veya RabbitTemplate bean'i inject EDİLMEZ. Doğrulama ya HTTP yanıtından ya da
 * RabbitMQ kuyruğundan (container bilgisiyle kurulan ham AMQP istemcisi
 * üzerinden) yapılır.
 */
@DisplayName("SYSTEM ALPHA - Sepet alışveriş akışı (tam stack, black-box)")
class CartShoppingJourneySystemAlphaTest extends AbstractCartSystemAlphaTest {

    private static final String VERIFICATION_QUEUE = "e2e.verification.cart.events.q";

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UUID userId;
    private UUID productA;
    private UUID productB;

    private Connection amqpConnection;
    private Channel amqpChannel;

    @BeforeEach
    void setUp() throws Exception {
        userId = UUID.randomUUID();
        productA = UUID.randomUUID();
        productB = UUID.randomUUID();

        // Uygulamanın AMQP bean'lerini KULLANMADAN, container bilgisinden ham
        // bir bağlantı kuruyoruz - dışarıdan bakan bir gözlemci gibi.
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ.getHost());
        factory.setPort(RABBITMQ.getAmqpPort());
        factory.setUsername("guest");
        factory.setPassword("guest");

        amqpConnection = factory.newConnection();
        amqpChannel = amqpConnection.createChannel();
        amqpChannel.exchangeDeclare("cart.exchange", "topic", true);
        amqpChannel.queueDeclare(VERIFICATION_QUEUE, true, false, false, null);
        amqpChannel.queueBind(VERIFICATION_QUEUE, "cart.exchange", "cart.#");
        amqpChannel.queuePurge(VERIFICATION_QUEUE);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (amqpChannel != null && amqpChannel.isOpen()) {
            amqpChannel.close();
        }
        if (amqpConnection != null && amqpConnection.isOpen()) {
            amqpConnection.close();
        }
    }

    // --- HTTP yardımcıları (yalnızca public API) ----------------------------

    private ResponseEntity<String> addItem(UUID cartOwner, UUID actor, String role, UUID productId, Integer quantity, String price) {
        String body = "{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + ",\"price\":" + price + "}";
        return restTemplate.exchange("/api/carts/{userId}/items", HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(actor, role)), String.class, cartOwner);
    }

    private ResponseEntity<String> updateItem(UUID cartOwner, UUID actor, String role, UUID productId, int quantity) {
        String body = "{\"quantity\":" + quantity + "}";
        return restTemplate.exchange("/api/carts/{userId}/items/{productId}", HttpMethod.PUT,
                new HttpEntity<>(body, jsonHeaders(actor, role)), String.class, cartOwner, productId);
    }

    private ResponseEntity<String> removeItem(UUID cartOwner, UUID actor, String role, UUID productId) {
        return restTemplate.exchange("/api/carts/{userId}/items/{productId}", HttpMethod.DELETE,
                new HttpEntity<>(jsonHeaders(actor, role)), String.class, cartOwner, productId);
    }

    private ResponseEntity<String> clearCart(UUID cartOwner, UUID actor, String role) {
        return restTemplate.exchange("/api/carts/{userId}", HttpMethod.DELETE,
                new HttpEntity<>(jsonHeaders(actor, role)), String.class, cartOwner);
    }

    private ResponseEntity<String> getCart(UUID cartOwner, UUID actor, String role) {
        return restTemplate.exchange("/api/carts/{userId}", HttpMethod.GET,
                new HttpEntity<>(jsonHeaders(actor, role)), String.class, cartOwner);
    }

    private JsonNode cartBody(UUID cartOwner) throws Exception {
        ResponseEntity<String> response = getCart(cartOwner, cartOwner, "ROLE_CUSTOMER");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody());
    }

    private String drainOneEvent() throws Exception {
        GetResponse response = amqpChannel.basicGet(VERIFICATION_QUEUE, true);
        return response == null ? null : new String(response.getBody(), StandardCharsets.UTF_8);
    }

    // --- senaryolar ---------------------------------------------------------

    @Test
    @DisplayName("A1: Uçtan uca - Sepete ekle -> güncelle -> oku -> sil akışının tamamı yalnızca HTTP API ile doğrulanır")
    void shoppingJourney_AddUpdateReadRemove_ShouldBehaveConsistentlyOverHttpOnly() throws Exception {
        // 1) İki farklı ürün sepete eklenir
        assertThat(addItem(userId, userId, "ROLE_CUSTOMER", productA, 2, "10.00").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(addItem(userId, userId, "ROLE_CUSTOMER", productB, 1, "25.50").getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode afterAdd = cartBody(userId);
        assertThat(afterAdd.get("userId").asText()).isEqualTo(userId.toString());
        assertThat(afterAdd.get("items")).hasSize(2);
        assertThat(afterAdd.get("totalAmount").decimalValue()).isEqualByComparingTo("45.50");

        // 2) Bir ürünün miktarı artırılır
        assertThat(updateItem(userId, userId, "ROLE_CUSTOMER", productA, 5).getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode afterUpdate = cartBody(userId);
        assertThat(afterUpdate.get("totalAmount").decimalValue()).isEqualByComparingTo("75.50");

        // 3) Bir ürün sepetten çıkarılır
        assertThat(removeItem(userId, userId, "ROLE_CUSTOMER", productB).getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode afterRemove = cartBody(userId);
        assertThat(afterRemove.get("items")).hasSize(1);
        assertThat(afterRemove.get("items").get(0).get("productId").asText()).isEqualTo(productA.toString());
        assertThat(afterRemove.get("totalAmount").decimalValue()).isEqualByComparingTo("50.00");

        // 4) Sepet tamamen boşaltılır
        assertThat(clearCart(userId, userId, "ROLE_CUSTOMER").getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode afterClear = cartBody(userId);
        assertThat(afterClear.get("items")).isEmpty();
        assertThat(afterClear.get("totalAmount").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("A2: Uçtan uca - Aynı ürün iki kez eklenirse API tek satır ve birleşik miktar döner")
    void shoppingJourney_WhenSameProductAddedTwice_ShouldExposeSingleMergedLine() throws Exception {
        addItem(userId, userId, "ROLE_CUSTOMER", productA, 2, "10.00");
        addItem(userId, userId, "ROLE_CUSTOMER", productA, 3, "10.00");

        JsonNode cart = cartBody(userId);

        assertThat(cart.get("items")).hasSize(1);
        assertThat(cart.get("items").get(0).get("quantity").asInt()).isEqualTo(5);
        assertThat(cart.get("totalAmount").decimalValue()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("A3: Uçtan uca - Sepete ekleme sonrası CartUpdatedEvent gerçekten RabbitMQ kuyruğuna düşer")
    void shoppingJourney_AfterAddingItem_ShouldPublishCartUpdatedEventToRabbitMq() {
        addItem(userId, userId, "ROLE_CUSTOMER", productA, 2, "10.00");

        // Outbox scheduler'ı burada AÇIK; olayın kendiliğinden yayınlanmasını bekliyoruz.
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1)).untilAsserted(() -> {
            String payload = drainOneEvent();
            assertThat(payload)
                    .as("CartUpdatedEvent cart.exchange üzerinden yayınlanmalı")
                    .isNotNull()
                    .contains(userId.toString())
                    .contains(productA.toString());
        });
    }

    @Test
    @DisplayName("A4: Uçtan uca - Başka bir kullanıcının sepetine HTTP üzerinden erişilemez ve veri sızmaz")
    void shoppingJourney_WhenAccessingAnotherUsersCart_ShouldNotLeakData() throws Exception {
        UUID victim = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();

        addItem(victim, victim, "ROLE_CUSTOMER", productA, 2, "10.00");

        ResponseEntity<String> stolen = getCart(victim, attacker, "ROLE_CUSTOMER");

        assertThat(stolen.getStatusCode().is2xxSuccessful())
                .as("Saldırgan kurbanın sepetini okuyamamalı")
                .isFalse();
        assertThat(stolen.getBody() == null || !stolen.getBody().contains(productA.toString()))
                .as("Hata gövdesi bile başka kullanıcının ürün bilgisini sızdırmamalı")
                .isTrue();

        // Kurbanın sepeti bozulmamış olmalı.
        assertThat(cartBody(victim).get("items")).hasSize(1);
    }

    @Test
    @DisplayName("A5: Uçtan uca - Token'sız istek reddedilir ve sepet oluşturulmaz")
    void shoppingJourney_WhenRequestUnauthenticated_ShouldBeRejectedWithoutSideEffects() throws Exception {
        String body = "{\"productId\":\"" + productA + "\",\"quantity\":2,\"price\":10.00}";

        ResponseEntity<String> response = restTemplate.exchange("/api/carts/{userId}/items", HttpMethod.POST,
                new HttpEntity<>(body, new org.springframework.http.HttpHeaders()), String.class, userId);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();

        // Yan etki olmamalı: sepet hâlâ boş görünmeli.
        assertThat(cartBody(userId).get("items")).isEmpty();
    }

    @Test
    @DisplayName("A6: Uçtan uca - Geçersiz gövde (quantity yok) 400 döner ve sepete hiçbir şey eklenmez")
    void shoppingJourney_WhenQuantityMissing_ShouldReturn400WithoutTouchingCart() throws Exception {
        String body = "{\"productId\":\"" + productA + "\",\"price\":10.00}";

        ResponseEntity<String> response = restTemplate.exchange("/api/carts/{userId}/items", HttpMethod.POST,
                new HttpEntity<>(body, jsonHeaders(userId, "ROLE_CUSTOMER")), String.class, userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(cartBody(userId).get("items")).isEmpty();
    }
}

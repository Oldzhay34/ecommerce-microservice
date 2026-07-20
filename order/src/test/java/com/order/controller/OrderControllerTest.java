package com.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.api.controller.OrderController;
import com.order.api.dto.CreateOrderRequest;
import com.order.api.dto.OrderItemDto;
import com.order.api.dto.UpdateOrderStatusRequest;
import com.order.application.port.in.OrderCommandUseCase;
import com.order.application.port.in.OrderQueryUseCase;
import com.order.domain.exception.UnauthorizedOrderAccessException;
import com.order.domain.model.Order;
import com.order.domain.model.OrderItem;
import com.order.domain.model.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

// Spring Boot 4.x özel paket yolları (product-service testleriyle tutarlı)
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NOT: @AutoConfigureMockMvc(addFilters = false) ile Spring Security filtre
 * zinciri (dolayısıyla JwtAuthFilter ve ExceptionTranslationFilter) devre dışı
 * bırakılmıştır. Bu yüzden burada @PreAuthorize/manuel yetkilendirme ihlalleri
 * gerçek bir HTTP 403 olarak değil, fırlatılan exception olarak gözlemlenir.
 * Gerçek filtre zinciriyle (403 status kodu dahil) doğrulama subsystem/
 * integration test katmanında yapılacaktır.
 *
 * NOT 2: addFilters=false olduğu için Spring Security'nin
 * SecurityContextHolderAwareRequestFilter'ı da devre dışı kalır. Bu filtre
 * normalde HttpServletRequest#getUserPrincipal() değerini doldurur ve
 * controller'daki `Authentication authentication` parametresi bu değerden
 * çözülür.
 *
 * SecurityMockMvcRequestPostProcessors.user(...) SADECE SecurityContextHolder/
 * session'ı doldurur - request.getUserPrincipal()'ı DOLDURMAZ (bunu normalde
 * SecurityContextHolderAwareRequestFilter yapar, o da devre dışı). Bu yüzden
 * burada request.getUserPrincipal()'ı DOĞRUDAN dolduran authAs(...) yardımcı
 * metodu kullanılır - bu, filtre zincirinden tamamen bağımsız çalışır.
 */
@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    /**
     * @WebMvcTest slice'ı, uygulamanın asıl SecurityConfig sınıfını (ve
     * dolayısıyla @EnableMethodSecurity'yi) otomatik yüklemez - bu yüzden
     * @PreAuthorize hiç tetiklenmiyordu (bkz. W8 testi). Gerçek
     * SecurityConfig'i (ve onun JwtAuthFilter bağımlılığını) buraya import
     * etmek yerine, sadece method-security AOP işlemesini aktive eden minik
     * bir @TestConfiguration tanımlıyoruz. @WebMvcTest bu iç sınıfı otomatik
     * algılar, ekstra @Import gerekmez.
     */
    @TestConfiguration
    @EnableMethodSecurity(prePostEnabled = true)
    static class MethodSecurityTestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private OrderCommandUseCase orderCommandUseCase;

    @MockitoBean
    private OrderQueryUseCase orderQueryUseCase;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        OrderItem item = new OrderItem("item-1", "prod-1", "store-1", 2, BigDecimal.valueOf(50));
        sampleOrder = new Order("order-123", "user-1", List.of(item), OrderStatus.PENDING, BigDecimal.valueOf(100), LocalDateTime.now());
    }

    /**
     * Hem request.getUserPrincipal()'ı HEM DE SecurityContextHolder'ı dolduran
     * RequestPostProcessor. İkisi de gerekli çünkü:
     *  - Controller'daki `Authentication authentication` parametresi
     *    request.getUserPrincipal()'dan çözülür (normalde
     *    SecurityContextHolderAwareRequestFilter doldurur - addFilters=false
     *    olduğu için devre dışı, o yüzden burada elle set ediyoruz).
     *  - @PreAuthorize AOP interceptor'ı ise SecurityContextHolder'a bakar
     *    (normalde SecurityContextHolderFilter doldurur - o da addFilters=false
     *    ile devre dışı, o yüzden burada da elle set ediyoruz).
     * addFilters=false olduğundan context'i temizleyecek bir filtre de
     * çalışmıyor - bu yüzden her testten sonra tearDown() içinde
     * SecurityContextHolder.clearContext() çağırıyoruz.
     */
    private RequestPostProcessor authAs(String username, String role) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                username, "N/A", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        return request -> {
            request.setUserPrincipal(token);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(token);
            SecurityContextHolder.setContext(context);
            return request;
        };
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("W1: createOrder - Kendi userId'si ile sipariş oluşturan kullanıcı 201 alır")
    void createOrder_WhenUserIdMatchesAuthUser_ShouldReturn201() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("user-1",
                List.of(new OrderItemDto("prod-1", "store-1", 2, BigDecimal.valueOf(50))));

        when(orderCommandUseCase.createOrder(any())).thenReturn(sampleOrder);

        mockMvc.perform(post("/api/orders")
                        .with(authAs("user-1", "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("user-1"));
    }

    @Test
    @DisplayName("W2: createOrder - Başkası adına sipariş oluşturmaya çalışırsa IDOR hatası fırlatır")
    void createOrder_WhenUserIdDoesNotMatchAuthUser_ShouldThrowUnauthorized() {
        CreateOrderRequest request = new CreateOrderRequest("another-user",
                List.of(new OrderItemDto("prod-1", "store-1", 2, BigDecimal.valueOf(50))));

        assertThatThrownBy(() -> mockMvc.perform(post("/api/orders")
                .with(authAs("user-1", "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))))
                .hasCauseInstanceOf(UnauthorizedOrderAccessException.class);
    }

    @Test
    @DisplayName("W3: createOrder - Boş item listesiyle 400 Bad Request döner")
    void createOrder_WithEmptyItems_ShouldReturn400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest("user-1", List.of());

        mockMvc.perform(post("/api/orders")
                        .with(authAs("user-1", "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("W4: getOrderById - Sipariş sahibi kendi siparişini görebilir")
    void getOrderById_WhenOwner_ShouldReturn200() throws Exception {
        when(orderQueryUseCase.getOrderById("order-123", "user-1", "ROLE_CUSTOMER")).thenReturn(sampleOrder);

        mockMvc.perform(get("/api/orders/order-123")
                        .with(authAs("user-1", "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("order-123"));
    }

    @Test
    @DisplayName("W5: getCustomerOrders - ADMIN başka kullanıcının siparişlerini de görebilir")
    void getCustomerOrders_WhenRequestedByAdmin_ShouldReturn200() throws Exception {
        when(orderQueryUseCase.getOrdersForCustomer("user-1", "user-1")).thenReturn(List.of(sampleOrder));

        mockMvc.perform(get("/api/orders/customer/user-1")
                        .with(authAs("admin-1", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("W6: getCustomerOrders - Başkasının siparişlerini isteyen CUSTOMER IDOR hatası alır")
    void getCustomerOrders_WhenMismatchedNonAdmin_ShouldThrowUnauthorized() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/orders/customer/user-1")
                .with(authAs("user-2", "CUSTOMER"))))
                .hasCauseInstanceOf(UnauthorizedOrderAccessException.class);
    }

    @Test
    @DisplayName("W7: getStoreOrders - Kendi mağazası olmayan bir storeId istenirse IDOR hatası alır")
    void getStoreOrders_WhenNotOwnStore_ShouldThrowUnauthorized() {
        assertThatThrownBy(() -> mockMvc.perform(get("/api/orders/store/store-1")
                .with(authAs("store-2", "STORE"))))
                .hasCauseInstanceOf(UnauthorizedOrderAccessException.class);
    }

    @Test
    @DisplayName("W8: getAllOrders - CUSTOMER rolü ADMIN-only endpoint'e erişemez (AccessDenied)")
    void getAllOrders_WhenNotAdmin_ShouldThrowAccessDenied() {
        // MethodSecurityTestConfig sayesinde @PreAuthorize artık bu slice'ta
        // da aktif olduğundan, CUSTOMER rolüyle ADMIN-only endpoint'e
        // erişim AccessDeniedException fırlatmalı.
        assertThatThrownBy(() -> mockMvc.perform(get("/api/orders")
                .with(authAs("user-1", "CUSTOMER"))))
                .hasCauseInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("W9: updateOrderStatus - STORE rolü siparişi SHIPPED yapabilir")
    void updateOrderStatus_ShipByStore_ShouldReturn200() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.SHIPPED);
        Order shipped = new Order("order-123", "user-1", sampleOrder.getItems(), OrderStatus.SHIPPED, BigDecimal.valueOf(100), LocalDateTime.now());

        when(orderCommandUseCase.shipOrder("order-123", "store-1")).thenReturn(shipped);

        mockMvc.perform(patch("/api/orders/order-123/status")
                        .with(authAs("store-1", "STORE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    @DisplayName("W10: updateOrderStatus - CUSTOMER rolü siparişi SHIPPED yapamaz")
    void updateOrderStatus_ShipByCustomer_ShouldThrowUnauthorized() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.SHIPPED);

        assertThatThrownBy(() -> mockMvc.perform(patch("/api/orders/order-123/status")
                .with(authAs("user-1", "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))))
                .hasCauseInstanceOf(UnauthorizedOrderAccessException.class);
    }

    @Test
    @DisplayName("W11: updateOrderStatus - PENDING gibi doğrudan geçişe izin verilmeyen statü istenirse IllegalArgumentException fırlatır")
    void updateOrderStatus_WithDisallowedDirectStatus_ShouldThrowIllegalArgument() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.PENDING);

        assertThatThrownBy(() -> mockMvc.perform(patch("/api/orders/order-123/status")
                .with(authAs("user-1", "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))))
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
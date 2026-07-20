package com.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.product.api.controller.ProductController;
import com.product.api.dto.CreateProductRequest;
import com.product.application.usecase.ProductCommandUseCase;
import com.product.application.usecase.ProductQueryUseCase;
import com.product.domain.model.Product;
import com.product.infrastructure.security.filter.JwtResourceFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Spring Boot 4.x özel paket yolları
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false) // Security Filter'ları test ortamında kapatıyoruz
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper(); // Manuel oluşturarak Spring kısıtlamalarından kaçıyoruz

    @MockitoBean
    private ProductCommandUseCase commandUseCase;

    @MockitoBean
    private ProductQueryUseCase queryUseCase;

    @MockitoBean
    private JwtResourceFilter jwtResourceFilter; // SecurityConfig'in patlamaması için mockluyoruz

    @Test
    @DisplayName("W1: searchProducts - Başarılı liste döner")
    void searchProducts_ShouldReturn200() throws Exception {
        when(queryUseCase.search("Laptop", null)).thenReturn(List.of(new Product()));

        mockMvc.perform(get("/api/v1/products/search")
                        .param("keyword", "Laptop")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(queryUseCase).search("Laptop", null);
    }

    @Test
    @DisplayName("W2: createProduct - Geçersiz DTO ile 400 Bad Request döner")
    void createProduct_WithNegativePrice_ShouldReturn400() throws Exception {
        // Price negatif olamaz validasyonu
        CreateProductRequest request = new CreateProductRequest("Laptop", "Tech", BigDecimal.valueOf(-100), 5);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
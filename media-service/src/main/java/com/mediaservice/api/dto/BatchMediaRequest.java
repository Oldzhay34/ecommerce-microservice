package com.mediaservice.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public class BatchMediaRequest {

    @NotEmpty(message = "productIds bos olamaz")
    @Size(max = 100, message = "En fazla 100 urun sorgulanabilir.")
    private List<UUID> productIds;

    public BatchMediaRequest() {
    }

    public BatchMediaRequest(List<UUID> productIds) {
        this.productIds = productIds;
    }

    public List<UUID> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<UUID> productIds) {
        this.productIds = productIds;
    }
}
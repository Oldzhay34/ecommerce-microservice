package com.product.infrastructure.search.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.math.BigDecimal;
import java.util.UUID;

// Elasticsearch'teki "products" indeksine eşlenir
@Document(indexName = "products")
public class ProductDocument {

    @Id
    private UUID id;

    // storeId, sadece kesin eşleşmeler (filter) için kullanılacağından keyword olarak işaretlenir
    @Field(type = FieldType.Keyword)
    private UUID storeId;

    // Full-text search (tam metin arama) için text, sort ve exact match için keyword field
    @Field(type = FieldType.Text, analyzer = "turkish")
    private String name;

    // Kategori bazlı filtreleme için keyword
    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer stock;

    // Hexagonal gereği manuel constructor ve getter/setter'lar
    public ProductDocument() {}

    public ProductDocument(UUID id, UUID storeId, String name, String category, BigDecimal price, Integer stock) {
        this.id = id;
        this.storeId = storeId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
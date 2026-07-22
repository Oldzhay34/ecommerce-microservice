package com.mediaservice.api.controller;

import com.mediaservice.api.dto.BatchMediaRequest;
import com.mediaservice.api.dto.BatchMediaResponse;
import com.mediaservice.api.dto.MediaAssetResponse;
import com.mediaservice.api.dto.ReorderImagesRequest;
import com.mediaservice.api.dto.UploadMediaResponse;
import com.mediaservice.application.port.in.MediaCommandUseCase;
import com.mediaservice.application.port.in.MediaQueryUseCase;
import com.mediaservice.domain.exception.ImageConversionException;
import com.mediaservice.domain.model.MediaAsset;
import com.mediaservice.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Controller DTO mapping yapar; JPA Entity DONDURMEZ.
 * IDOR: storeId request'ten alinmaz, yalnizca SecurityUtils uzerinden JWT'den cikarilir.
 */
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaCommandUseCase commandUseCase;
    private final MediaQueryUseCase queryUseCase;

    public MediaController(MediaCommandUseCase commandUseCase, MediaQueryUseCase queryUseCase) {
        this.commandUseCase = commandUseCase;
        this.queryUseCase = queryUseCase;
    }

    /**
     * 1. Gorsel Yukleme - ROLE_STORE (yalnizca kendi urunu)
     */
    @PostMapping(path = "/products/{productId}/images", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('STORE')")
    public ResponseEntity<UploadMediaResponse> uploadProductImage(
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file) {

        UUID actorUserId = SecurityUtils.extractUserIdFromSecurityContext();
        boolean isAdmin = SecurityUtils.isAdmin();

        MediaAsset asset = commandUseCase.uploadProductImage(
                productId,
                actorUserId,
                isAdmin,
                file.getContentType(),
                readBytes(file));

        return ResponseEntity.status(HttpStatus.CREATED).body(toUploadResponse(asset));
    }

    /**
     * 2. Gorsel Silme - ROLE_STORE (kendi) / ROLE_ADMIN
     */
    @DeleteMapping("/images/{assetId}")
    @PreAuthorize("hasAnyRole('STORE', 'ADMIN')")
    public ResponseEntity<Void> deleteProductImage(@PathVariable UUID assetId) {
        commandUseCase.deleteProductImage(
                assetId,
                SecurityUtils.extractUserIdFromSecurityContext(),
                SecurityUtils.isAdmin());
        return ResponseEntity.noContent().build();
    }

    /**
     * 3. Kapak Gorseli Ayarlama - ROLE_STORE (kendi) / ROLE_ADMIN
     */
    @PutMapping("/images/{assetId}/primary")
    @PreAuthorize("hasAnyRole('STORE', 'ADMIN')")
    public ResponseEntity<MediaAssetResponse> setPrimaryImage(@PathVariable UUID assetId) {
        MediaAsset asset = commandUseCase.setPrimaryImage(
                assetId,
                SecurityUtils.extractUserIdFromSecurityContext(),
                SecurityUtils.isAdmin());
        return ResponseEntity.ok(toResponse(asset));
    }

    /**
     * 4. Siralama - ROLE_STORE (kendi) / ROLE_ADMIN
     */
    @PutMapping("/products/{productId}/images/order")
    @PreAuthorize("hasAnyRole('STORE', 'ADMIN')")
    public ResponseEntity<List<MediaAssetResponse>> reorderProductImages(
            @PathVariable UUID productId,
            @Valid @RequestBody ReorderImagesRequest request) {

        List<MediaAsset> assets = commandUseCase.reorderProductImages(
                productId,
                request.getAssetIds(),
                SecurityUtils.extractUserIdFromSecurityContext(),
                SecurityUtils.isAdmin());

        List<MediaAssetResponse> body = new ArrayList<>(assets.size());
        for (MediaAsset asset : assets) {
            body.add(toResponse(asset));
        }
        return ResponseEntity.ok(body);
    }

    /**
     * 5. Urun Gorsellerini Getirme - public (@PreAuthorize YOK).
     * Gorseli yoksa BOS DIZI doner, 404 DONDURULMEZ.
     */
    @GetMapping("/products/{productId}/images")
    public ResponseEntity<List<MediaAssetResponse>> getProductMedia(@PathVariable UUID productId) {
        return ResponseEntity.ok(queryUseCase.getProductMedia(productId));
    }

    /**
     * 6. Toplu Getirme - public. Max 100 id (BatchMediaRequest @Size).
     */
    @PostMapping("/products/images/batch")
    public ResponseEntity<BatchMediaResponse> getProductMediaBatch(
            @Valid @RequestBody BatchMediaRequest request) {
        return ResponseEntity.ok(
                new BatchMediaResponse(queryUseCase.getProductMediaBatch(request.getProductIds())));
    }

    // ------------------------------------------------------------------
    // DTO mapping
    // ------------------------------------------------------------------

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ImageConversionException("Gorsel islenemedi.", e);
        }
    }

    private UploadMediaResponse toUploadResponse(MediaAsset asset) {
        return new UploadMediaResponse(
                asset.getId(),
                asset.getProductId(),
                asset.getUrl(),
                asset.getThumbUrl(),
                asset.getOriginalUrl(),
                asset.getContentType(),
                asset.getSizeBytes(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getSortOrder(),
                asset.isPrimary(),
                asset.getCreatedAt());
    }

    private MediaAssetResponse toResponse(MediaAsset asset) {
        return new MediaAssetResponse(
                asset.getId(),
                asset.getProductId(),
                asset.getUrl(),
                asset.getThumbUrl(),
                asset.getOriginalUrl(),
                asset.getContentType(),
                asset.getSizeBytes(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getSortOrder(),
                asset.isPrimary(),
                asset.getCreatedAt());
    }
}
package com.mediaservice.unit.domain;

import com.mediaservice.domain.model.MediaStatus;
import com.mediaservice.support.MediaTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Katman: UNIT - saf domain modeli, framework/mock yok.
 */
@DisplayName("UNIT - MediaAsset domain invaryantlari")
class MediaAssetTest {

    private final UUID productId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();

    @Test
    @DisplayName("U1: validateWebpInvariant - Gecerli .webp URL'leri ve content-type ile hata firlatmaz")
    void validateWebpInvariant_WhenAllWebp_ShouldNotThrow() {
        var asset = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);
        assertThatCode(asset::validateWebpInvariant).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("U2: validateWebpInvariant - url .webp ile bitmiyorsa IllegalStateException firlatir")
    void validateWebpInvariant_WhenUrlNotWebp_ShouldThrow() {
        var asset = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);
        asset.setUrl("https://cdn.test/image.png");

        assertThatThrownBy(asset::validateWebpInvariant)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("url");
    }

    @Test
    @DisplayName("U3: validateWebpInvariant - contentType image/webp degilse IllegalStateException firlatir")
    void validateWebpInvariant_WhenContentTypeNotWebp_ShouldThrow() {
        var asset = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);
        asset.setContentType("image/png");

        assertThatThrownBy(asset::validateWebpInvariant)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contentType");
    }

    @Test
    @DisplayName("U4: markDeleted - status DELETED olur ve primary bayragi dusurulur")
    void markDeleted_ShouldSetDeletedStatusAndClearPrimary() {
        var asset = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, true);

        asset.markDeleted();

        assertThat(asset.getStatus()).isEqualTo(MediaStatus.DELETED);
        assertThat(asset.isPrimary()).isFalse();
        assertThat(asset.isActive()).isFalse();
    }

    @Test
    @DisplayName("U5: isOwnedBy - eslesen storeId true, farkli/null storeId false doner")
    void isOwnedBy_ShouldCompareStoreId() {
        var asset = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, false);

        assertThat(asset.isOwnedBy(storeId)).isTrue();
        assertThat(asset.isOwnedBy(UUID.randomUUID())).isFalse();
        assertThat(asset.isOwnedBy(null)).isFalse();
    }

    @Test
    @DisplayName("U6: isActive - yalnizca status ACTIVE oldugunda true doner")
    void isActive_ShouldReflectStatus() {
        var active = MediaTestFixtures.activeAsset(UUID.randomUUID(), productId, storeId, 0, false);
        var deleted = MediaTestFixtures.deletedAsset(UUID.randomUUID(), productId, storeId);

        assertThat(active.isActive()).isTrue();
        assertThat(deleted.isActive()).isFalse();
    }
}

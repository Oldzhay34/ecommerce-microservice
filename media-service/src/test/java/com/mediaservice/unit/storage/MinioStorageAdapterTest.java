package com.mediaservice.unit.storage;

import com.mediaservice.domain.exception.ImageConversionException;
import com.mediaservice.domain.model.ImageBinary;
import com.mediaservice.domain.model.MediaVariant;
import com.mediaservice.infrastructure.storage.adapter.MinioStorageAdapter;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Katman: UNIT - MinioClient mock'lanir, StoragePort implementasyonu izole test edilir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UNIT - MinioStorageAdapter")
class MinioStorageAdapterTest {

    @Mock private MinioClient minioClient;

    @Test
    @DisplayName("U1: upload - MinioClient.putObject dogru bucket/key/content-type ile cagirilir, public URL doner")
    void upload_ShouldPutObjectAndReturnPublicUrl() throws Exception {
        MinioStorageAdapter adapter = new MinioStorageAdapter(minioClient, "shopbridge-media",
                "http://localhost:9000/shopbridge-media");
        ImageBinary binary = new ImageBinary(new byte[]{1, 2, 3, 4}, 100, 80, MediaVariant.MEDIUM);

        String url = adapter.upload("products/p1/a1_medium.webp", binary);

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("shopbridge-media");
        assertThat(captor.getValue().object()).isEqualTo("products/p1/a1_medium.webp");
        assertThat(captor.getValue().contentType()).isEqualTo("image/webp");
        assertThat(url).isEqualTo("http://localhost:9000/shopbridge-media/products/p1/a1_medium.webp");
    }

    @Test
    @DisplayName("U2: buildPublicUrl - Yapilandirilan base URL sonunda '/' varsa TEK kez temizlenir")
    void buildPublicUrl_ShouldStripTrailingSlashExactlyOnce() {
        MinioStorageAdapter adapter = new MinioStorageAdapter(minioClient, "shopbridge-media",
                "http://localhost:9000/shopbridge-media/");

        assertThat(adapter.buildPublicUrl("products/p1/a1.webp"))
                .isEqualTo("http://localhost:9000/shopbridge-media/products/p1/a1.webp");
    }

    @Test
    @DisplayName("U3: upload - MinIO hata firlatirsa ImageConversionException'a sarmalanir")
    void upload_WhenMinioThrows_ShouldWrapAsImageConversionException() throws Exception {
        MinioStorageAdapter adapter = new MinioStorageAdapter(minioClient, "shopbridge-media",
                "http://localhost:9000/shopbridge-media");
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("connection refused"));
        ImageBinary binary = new ImageBinary(new byte[]{1}, 10, 10, MediaVariant.THUMB);

        assertThatThrownBy(() -> adapter.upload("products/p1/a1_thumb.webp", binary))
                .isInstanceOf(ImageConversionException.class);
    }
}

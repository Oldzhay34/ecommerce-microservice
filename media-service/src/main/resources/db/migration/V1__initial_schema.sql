-- ShopBridge Media Service - initial schema
-- Konvansiyon: tablolar ve kolonlar snake_case.

CREATE TABLE media_asset (
                             id                    UUID         PRIMARY KEY,
                             product_id            UUID         NOT NULL,
                             store_id              UUID         NOT NULL,
                             url                   VARCHAR(1024) NOT NULL,
                             thumb_url             VARCHAR(1024) NOT NULL,
                             original_url          VARCHAR(1024) NOT NULL,
                             storage_key           VARCHAR(512) NOT NULL,
                             storage_key_thumb     VARCHAR(512) NOT NULL,
                             storage_key_medium    VARCHAR(512) NOT NULL,
                             storage_key_original  VARCHAR(512) NOT NULL,
                             content_type          VARCHAR(64)  NOT NULL DEFAULT 'image/webp',
                             size_bytes            BIGINT       NOT NULL,
                             width                 INTEGER      NOT NULL,
                             height                INTEGER      NOT NULL,
                             sort_order            INTEGER      NOT NULL DEFAULT 0,
                             is_primary            BOOLEAN      NOT NULL DEFAULT FALSE,
                             status                VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
                             created_at            TIMESTAMPTZ  NOT NULL,
                             version               BIGINT       NOT NULL DEFAULT 0,

                             CONSTRAINT ck_media_asset_status      CHECK (status IN ('ACTIVE', 'DELETED')),
                             CONSTRAINT ck_media_asset_webp_url    CHECK (url LIKE '%.webp'),
                             CONSTRAINT ck_media_asset_webp_thumb  CHECK (thumb_url LIKE '%.webp'),
                             CONSTRAINT ck_media_asset_webp_orig   CHECK (original_url LIKE '%.webp'),
                             CONSTRAINT ck_media_asset_ctype       CHECK (content_type = 'image/webp')
);

CREATE TABLE outbox_event (
                              id             UUID         PRIMARY KEY,
                              aggregate_type VARCHAR(64)  NOT NULL,
                              aggregate_id   UUID         NOT NULL,
                              event_type     VARCHAR(64)  NOT NULL,
                              routing_key    VARCHAR(128) NOT NULL,
                              payload        TEXT         NOT NULL,
                              processed      BOOLEAN      NOT NULL DEFAULT FALSE,
                              created_at     TIMESTAMPTZ  NOT NULL,
                              processed_at   TIMESTAMPTZ
);

-- ZORUNLU INDEKSLER
CREATE INDEX idx_media_asset_product_status_sort
    ON media_asset (product_id, status, sort_order);

CREATE INDEX idx_media_asset_store
    ON media_asset (store_id);

CREATE UNIQUE INDEX uq_media_asset_primary
    ON media_asset (product_id)
    WHERE is_primary = true AND status = 'ACTIVE';

CREATE INDEX idx_outbox_event_unprocessed
    ON outbox_event (processed, created_at);
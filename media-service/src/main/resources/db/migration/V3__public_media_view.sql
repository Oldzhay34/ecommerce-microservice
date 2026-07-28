-- Public, read-only view over media_asset for short-lived / high-frequency,
-- unauthenticated reads (GetProductMedia, GetProductMediaBatch).
--
-- Amac: (1) yalnizca ACTIVE satirlari gostererek gereksiz satir taramasini
-- engellemek, (2) internal/erisim-kontrolu icin kullanilan kolonlari
-- (store_id, version, storage_key*) public okuma yolundan tamamen gizlemek.
-- Bu, tablo yerine view kullanarak bellek/erisim yuzeyini kucultme
-- pratiginin bu serviste uygulandigi somut orgnektir (bkz. sunum notlari).
--
-- Yazma islemleri (upload/delete/setPrimary/reorder) her zaman media_asset
-- tablosuna dogrudan yapilir; bu view SADECE okuma icindir.

CREATE OR REPLACE VIEW v_media_asset_public AS
SELECT
    id,
    product_id,
    url,
    thumb_url,
    original_url,
    content_type,
    size_bytes,
    width,
    height,
    sort_order,
    is_primary,
    created_at
FROM media_asset
WHERE status = 'ACTIVE';

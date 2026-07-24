-- ShopBridge Media Service - guvenlik sertlestirme trigger'lari
--
-- AMAC: SQL injection'a KARSI degil (tum sorgular zaten JPA parameter binding /
-- named-param kullaniyor, bkz. MediaAssetRepository/OutboxEventRepository - bu dosya
-- bir SQL injection filtresi DEGILDIR, trigger'lar sorgu calistiktan SONRA satir
-- seviyesinde tetiklenir ve enjeksiyonu yakalayamaz). Buradaki amac: uygulama
-- katmanini bypass eden veya kotuye kullanan bir cagirici (kompromize servis, bug,
-- dogrudan DB erisimi) icin son savunma hatti. Her kural, MASUM bir kullanicinin
-- ASLA ihtiyac duymayacagi bir seyi engeller - serbest metin alanlarina (url'ler
-- disinda) DOKUNULMAZ.
--
-- Flyway TEK SEFER calistigi icin (checksum ile takip edilir) idempotency guard'a
-- gerek yok; yine de diger servislerle (spring.sql.init.mode=always altinda HER
-- baslangicta yeniden calisan script'ler) AYNI desen kullanilsin diye fonksiyon/
-- trigger'lar CREATE OR REPLACE / DROP-IF-EXISTS ile yazildi.

-- ------------------------------------------------------------------
-- 1) media_asset: fiziksel DELETE tamamen yasak (soft-delete-only sozlesmesi)
-- ------------------------------------------------------------------
-- Uygulama zaten hicbir zaman DELETE calistirmiyor (markDeleted() + status=DELETED
-- kullaniliyor, bkz. MediaCommandUseCaseImpl). Bu trigger, o sozlesmeyi DB seviyesinde
-- ZORUNLU kilar - dogrudan DB erisimiyle bile fiziksel silme yapilamaz.
CREATE OR REPLACE FUNCTION fn_media_asset_block_delete() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'media_asset satirlari fiziksel olarak silinemez; soft delete (status=DELETED) kullanin.'
        USING ERRCODE = 'raise_exception';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_media_asset_block_delete ON media_asset;
CREATE TRIGGER trg_media_asset_block_delete
    BEFORE DELETE ON media_asset
    FOR EACH ROW EXECUTE FUNCTION fn_media_asset_block_delete();

-- ------------------------------------------------------------------
-- 2) media_asset: boyut/oran alanlari her zaman pozitif olmali
-- ------------------------------------------------------------------
-- Gercek bir yuklenen gorsel icin size_bytes/width/height ASLA 0 veya negatif olamaz
-- (WebpImageConverter her zaman gecerli, decode edilmis bir gorselden uretir).
CREATE OR REPLACE FUNCTION fn_media_asset_validate_dimensions() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.size_bytes <= 0 THEN
        RAISE EXCEPTION 'media_asset.size_bytes pozitif olmalidir (verilen: %)', NEW.size_bytes;
    END IF;
    IF NEW.width <= 0 OR NEW.height <= 0 THEN
        RAISE EXCEPTION 'media_asset.width/height pozitif olmalidir (verilen: %x%)', NEW.width, NEW.height;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_media_asset_validate_dimensions ON media_asset;
CREATE TRIGGER trg_media_asset_validate_dimensions
    BEFORE INSERT OR UPDATE ON media_asset
    FOR EACH ROW EXECUTE FUNCTION fn_media_asset_validate_dimensions();

-- ------------------------------------------------------------------
-- 3) outbox_event: payload GECERLI JSON olmak zorunda
-- ------------------------------------------------------------------
-- MediaEventFactory zaten her zaman gecerli JSON uretir (ObjectMapper); bu trigger,
-- bozuk/yarim payload'in RabbitMQ tuketicilerine hic ulasmadan (yayinlanmadan ONCE,
-- yaziliken) reddedilmesini garanti eder.
CREATE OR REPLACE FUNCTION fn_media_outbox_validate_payload() RETURNS TRIGGER AS $$
BEGIN
    BEGIN
        PERFORM NEW.payload::jsonb;
    EXCEPTION WHEN OTHERS THEN
        RAISE EXCEPTION 'outbox_event.payload gecerli JSON olmalidir';
    END;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_media_outbox_validate_payload ON outbox_event;
CREATE TRIGGER trg_media_outbox_validate_payload
    BEFORE INSERT OR UPDATE ON outbox_event
    FOR EACH ROW EXECUTE FUNCTION fn_media_outbox_validate_payload();

-- ------------------------------------------------------------------
-- 4) outbox_event: processed=true olan satirlarin CEKIRDEK alanlari degistirilemez
-- ------------------------------------------------------------------
-- OutboxEventPublisher yalnizca processed/processed_at alanlarini gunceller
-- (markProcessed). Zaten yayinlanmis bir olayin govdesinin/routing key'inin sessizce
-- degistirilmesi (mesaj kaynagi tahrifati) hicbir mesru akista GEREKMEZ.
CREATE OR REPLACE FUNCTION fn_media_outbox_block_mutation_after_processed() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.processed = TRUE AND (
        NEW.aggregate_type IS DISTINCT FROM OLD.aggregate_type OR
        NEW.aggregate_id   IS DISTINCT FROM OLD.aggregate_id OR
        NEW.event_type     IS DISTINCT FROM OLD.event_type OR
        NEW.routing_key    IS DISTINCT FROM OLD.routing_key OR
        NEW.payload        IS DISTINCT FROM OLD.payload OR
        NEW.created_at     IS DISTINCT FROM OLD.created_at
    ) THEN
        RAISE EXCEPTION 'Yayinlanmis (processed=true) bir outbox_event satirinin govdesi degistirilemez.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_media_outbox_block_mutation_after_processed ON outbox_event;
CREATE TRIGGER trg_media_outbox_block_mutation_after_processed
    BEFORE UPDATE ON outbox_event
    FOR EACH ROW EXECUTE FUNCTION fn_media_outbox_block_mutation_after_processed();

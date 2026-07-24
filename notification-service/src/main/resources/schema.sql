CREATE TABLE IF NOT EXISTS processed_event (
                                               idempotency_key VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
    );

-- Guvenlik sertlestirme: bu dosya spring.sql.init.mode=always ile HER baslangicta
-- yeniden calisir, bu yuzden fonksiyon/trigger tanimlari idempotent olmalidir
-- (CREATE OR REPLACE / DROP TRIGGER IF EXISTS). AMAC SQL injection filtresi DEGILDIR
-- (ProcessedEventJdbcAdapter zaten tum sorgularda ? parametre binding kullanir) -
-- uygulama katmanini bypass eden bir cagiriciya karsi son savunma hattidir.
--
-- processed_event, ProcessedEventJdbcAdapter tarafindan yalnizca INSERT + SELECT ile
-- kullanilir (hicbir UPDATE yolu yoktur, bkz. ProcessedEventPort) - bu yuzden satirlar
-- TAMAMEN degismez olabilir; masum hicbir akis bu tabloyu guncellemeye ihtiyac duymaz.
CREATE OR REPLACE FUNCTION fn_notification_processed_event_block_update() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'processed_event satirlari degismezdir (immutable); guncelleme desteklenmiyor.';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_notification_processed_event_block_update ON processed_event;
CREATE TRIGGER trg_notification_processed_event_block_update
    BEFORE UPDATE ON processed_event
    FOR EACH ROW EXECUTE FUNCTION fn_notification_processed_event_block_update();
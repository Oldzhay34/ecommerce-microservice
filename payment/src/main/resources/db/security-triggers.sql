-- payment-service - guvenlik sertlestirme trigger'lari
--
-- AMAC: SQL injection'a KARSI degil (PaymentRepository/OutboxRepository yalnizca
-- Spring Data derived query metotlari kullanir, native/raw SQL yoktur - bkz. guvenlik
-- denetimi). Trigger'lar sorgu calistiktan SONRA satir seviyesinde tetiklenir,
-- enjeksiyonu yakalayamaz; buradaki amac uygulama katmanini bypass eden bir
-- cagiriciya (kompromize servis, bug, dogrudan DB erisimi) karsi son savunma hattidir,
-- ozellikle PARA ile ilgili tahrifata karsi.
--
-- Bu dosya spring.sql.init.mode=always ile HER baslangicta yeniden calisir; bu yuzden
-- tum tanimlar idempotent olmalidir (CREATE OR REPLACE FUNCTION / DROP TRIGGER IF
-- EXISTS + CREATE TRIGGER).

-- ------------------------------------------------------------------
-- 1) payments: amount pozitif olmali; PENDING'den cikildiktan sonra order_id/
--    customer_id/amount ASLA degistirilemez (odenen tutarin tahrifatina karsi en
--    kritik kural); REFUNDED terminal durumdur, ikinci kez iade edilemez
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_payment_validate_payments() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.amount <= 0 THEN
        RAISE EXCEPTION 'payments.amount pozitif olmalidir (verilen: %)', NEW.amount;
    END IF;
    IF TG_OP = 'UPDATE' THEN
        IF OLD.status != 'PENDING' AND (
            NEW.order_id IS DISTINCT FROM OLD.order_id OR
            NEW.customer_id IS DISTINCT FROM OLD.customer_id OR
            NEW.amount IS DISTINCT FROM OLD.amount
        ) THEN
            RAISE EXCEPTION 'PENDING disina cikmis bir odemenin order_id/customer_id/amount alanlari degistirilemez.';
        END IF;
        IF OLD.status = 'REFUNDED' AND NEW.status IS DISTINCT FROM OLD.status THEN
            RAISE EXCEPTION 'REFUNDED durumundaki bir odeme ikinci kez durum degistiremez.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_payment_validate_payments ON payments;
CREATE TRIGGER trg_payment_validate_payments
    BEFORE INSERT OR UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION fn_payment_validate_payments();

-- ------------------------------------------------------------------
-- 2) outbox_event: payload gecerli JSON olmali
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_payment_validate_outbox() RETURNS TRIGGER AS $$
BEGIN
    BEGIN
        PERFORM NEW.payload::jsonb;
    EXCEPTION WHEN OTHERS THEN
        RAISE EXCEPTION 'outbox_event.payload gecerli JSON olmalidir';
    END;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_payment_validate_outbox ON outbox_event;
CREATE TRIGGER trg_payment_validate_outbox
    BEFORE INSERT OR UPDATE ON outbox_event
    FOR EACH ROW EXECUTE FUNCTION fn_payment_validate_outbox();

-- order-service - guvenlik sertlestirme trigger'lari
--
-- AMAC: SQL injection'a KARSI degil (OrderRepository/OutboxEventRepository yalnizca
-- Spring Data derived query metotlari kullanir, native/raw SQL yoktur - bkz. guvenlik
-- denetimi). Trigger'lar sorgu calistiktan SONRA satir seviyesinde tetiklenir,
-- enjeksiyonu yakalayamaz; buradaki amac uygulama katmanini bypass eden bir
-- cagiriciya (kompromize servis, bug, dogrudan DB erisimi) karsi son savunma hattidir.
--
-- Bu dosya spring.sql.init.mode=always ile HER baslangicta yeniden calisir; bu yuzden
-- tum tanimlar idempotent olmalidir (CREATE OR REPLACE FUNCTION / DROP TRIGGER IF
-- EXISTS + CREATE TRIGGER).
--
-- NOT: bu servis ayrica spring-modulith-events-jpa kullaniyor (kendi ic
-- event_publication tablosunu yonetir) - o tabloya KASITLI OLARAK dokunulmadi,
-- framework'un kendi yasam dongusunu bozma riski tasir.

-- ------------------------------------------------------------------
-- 1) orders: total_amount negatif olamaz, id/user_id degistirilemez, CANCELLED
--    durumdan cikis yasak (terminal state)
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_order_validate_orders() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.total_amount < 0 THEN
        RAISE EXCEPTION 'orders.total_amount negatif olamaz (verilen: %)', NEW.total_amount;
    END IF;
    IF TG_OP = 'UPDATE' THEN
        IF NEW.id IS DISTINCT FROM OLD.id THEN
            RAISE EXCEPTION 'orders.id degistirilemez.';
        END IF;
        IF NEW.user_id IS DISTINCT FROM OLD.user_id THEN
            RAISE EXCEPTION 'orders.user_id degistirilemez (siparis baska kullaniciya devredilemez).';
        END IF;
        IF OLD.status = 'CANCELLED' AND NEW.status IS DISTINCT FROM OLD.status THEN
            RAISE EXCEPTION 'CANCELLED durumundaki bir siparisin durumu degistirilemez.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_order_validate_orders ON orders;
CREATE TRIGGER trg_order_validate_orders
    BEFORE INSERT OR UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION fn_order_validate_orders();

-- ------------------------------------------------------------------
-- 2) order_items: quantity pozitif, price negatif olamaz; hangi siparise/urune ait
--    oldugu olusturulduktan sonra degistirilemez
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_order_validate_order_items() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.quantity <= 0 THEN
        RAISE EXCEPTION 'order_items.quantity pozitif olmalidir (verilen: %)', NEW.quantity;
    END IF;
    IF NEW.price < 0 THEN
        RAISE EXCEPTION 'order_items.price negatif olamaz (verilen: %)', NEW.price;
    END IF;
    IF TG_OP = 'UPDATE' THEN
        IF NEW.order_id IS DISTINCT FROM OLD.order_id THEN
            RAISE EXCEPTION 'order_items.order_id degistirilemez.';
        END IF;
        IF NEW.product_id IS DISTINCT FROM OLD.product_id THEN
            RAISE EXCEPTION 'order_items.product_id degistirilemez.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_order_validate_order_items ON order_items;
CREATE TRIGGER trg_order_validate_order_items
    BEFORE INSERT OR UPDATE ON order_items
    FOR EACH ROW EXECUTE FUNCTION fn_order_validate_order_items();

-- ------------------------------------------------------------------
-- 3) outbox_event: payload gecerli JSON olmali; processed=true satirlarin
--    cekirdek alanlari degistirilemez
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_order_validate_outbox() RETURNS TRIGGER AS $$
BEGIN
    BEGIN
        PERFORM NEW.payload::jsonb;
    EXCEPTION WHEN OTHERS THEN
        RAISE EXCEPTION 'outbox_event.payload gecerli JSON olmalidir';
    END;

    IF TG_OP = 'UPDATE' AND OLD.processed = TRUE THEN
        IF NEW.aggregate_id IS DISTINCT FROM OLD.aggregate_id OR
           NEW.aggregate_type IS DISTINCT FROM OLD.aggregate_type OR
           NEW.event_type IS DISTINCT FROM OLD.event_type OR
           NEW.payload IS DISTINCT FROM OLD.payload OR
           NEW.created_at IS DISTINCT FROM OLD.created_at THEN
            RAISE EXCEPTION 'Yayinlanmis (processed=true) bir outbox_event satirinin govdesi degistirilemez.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_order_validate_outbox ON outbox_event;
CREATE TRIGGER trg_order_validate_outbox
    BEFORE INSERT OR UPDATE ON outbox_event
    FOR EACH ROW EXECUTE FUNCTION fn_order_validate_outbox();

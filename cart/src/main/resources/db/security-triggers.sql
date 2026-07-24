-- cart-service - guvenlik sertlestirme trigger'lari
--
-- AMAC: SQL injection'a KARSI degil (CartRepository/OutboxEventRepository yalnizca
-- Spring Data derived query metotlari kullanir, native/raw SQL yoktur - bkz. guvenlik
-- denetimi). Trigger'lar sorgu calistiktan SONRA satir seviyesinde tetiklenir,
-- enjeksiyonu yakalayamaz; buradaki amac uygulama katmanini bypass eden bir
-- cagiriciya (kompromize servis, bug, dogrudan DB erisimi) karsi son savunma hattidir.
--
-- Bu dosya spring.sql.init.mode=always ile HER baslangicta yeniden calisir; bu yuzden
-- tum tanimlar idempotent olmalidir (CREATE OR REPLACE FUNCTION / DROP TRIGGER IF
-- EXISTS + CREATE TRIGGER).
--
-- NOT (statement separator): bu dosya cift noktali virgul ayiricisiyla parcalaniyor (bkz.
-- application.yaml -> spring.sql.init.separator). Spring'in varsayilan tek ';'
-- ayiricisi, $$ dolar-quote fonksiyon govdesi icindeki sıradan ';' karakterlerini de
-- YANLISLIKLA ayirici sanip script'i ortadan boler. Fonksiyon govdesi icindeki TEK ';'
-- karakterlerine DOKUNULMADI - yalnizca en disaridaki (top-level) her ifadenin sonuna
-- ikinci bir ';' eklendi.

-- ------------------------------------------------------------------
-- 1) carts: total_amount negatif olamaz (0 gecerlidir - bos sepet); user_id
--    olusturulduktan sonra degistirilemez (sepet baska kullaniciya devredilemez)
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_cart_validate_carts() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.total_amount < 0 THEN
        RAISE EXCEPTION 'carts.total_amount negatif olamaz (verilen: %)', NEW.total_amount;
    END IF;
    IF TG_OP = 'UPDATE' AND NEW.user_id IS DISTINCT FROM OLD.user_id THEN
        RAISE EXCEPTION 'carts.user_id degistirilemez.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;;

DROP TRIGGER IF EXISTS trg_cart_validate_carts ON carts;;
CREATE TRIGGER trg_cart_validate_carts
    BEFORE INSERT OR UPDATE ON carts
    FOR EACH ROW EXECUTE FUNCTION fn_cart_validate_carts();;

-- ------------------------------------------------------------------
-- 2) cart_items: quantity pozitif, price negatif olamaz; hangi sepete/urune ait
--    oldugu olusturulduktan sonra degistirilemez
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_cart_validate_cart_items() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.quantity <= 0 THEN
        RAISE EXCEPTION 'cart_items.quantity pozitif olmalidir (verilen: %)', NEW.quantity;
    END IF;
    IF NEW.price < 0 THEN
        RAISE EXCEPTION 'cart_items.price negatif olamaz (verilen: %)', NEW.price;
    END IF;
    IF TG_OP = 'UPDATE' THEN
        IF NEW.cart_id IS DISTINCT FROM OLD.cart_id THEN
            RAISE EXCEPTION 'cart_items.cart_id degistirilemez.';
        END IF;
        IF NEW.product_id IS DISTINCT FROM OLD.product_id THEN
            RAISE EXCEPTION 'cart_items.product_id degistirilemez.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;;

DROP TRIGGER IF EXISTS trg_cart_validate_cart_items ON cart_items;;
CREATE TRIGGER trg_cart_validate_cart_items
    BEFORE INSERT OR UPDATE ON cart_items
    FOR EACH ROW EXECUTE FUNCTION fn_cart_validate_cart_items();;

-- ------------------------------------------------------------------
-- 3) outbox_event: payload gecerli JSON olmali
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_cart_validate_outbox() RETURNS TRIGGER AS $$
BEGIN
    BEGIN
        PERFORM NEW.payload::jsonb;
    EXCEPTION WHEN OTHERS THEN
        RAISE EXCEPTION 'outbox_event.payload gecerli JSON olmalidir';
    END;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;;

DROP TRIGGER IF EXISTS trg_cart_validate_outbox ON outbox_event;;
CREATE TRIGGER trg_cart_validate_outbox
    BEFORE INSERT OR UPDATE ON outbox_event
    FOR EACH ROW EXECUTE FUNCTION fn_cart_validate_outbox();;

-- review-service - guvenlik sertlestirme trigger'lari
--
-- AMAC: SQL injection'a KARSI degil (ReviewRepository/PurchaseEligibilityRepository/
-- OutboxEventRepository yalnizca Spring Data derived query metotlari kullanir,
-- native/raw SQL yoktur - bkz. guvenlik denetimi). Trigger'lar sorgu calistiktan
-- SONRA satir seviyesinde tetiklenir, enjeksiyonu yakalayamaz; buradaki amac uygulama
-- katmanini bypass eden bir cagiriciya (kompromize servis, bug, dogrudan DB erisimi)
-- karsi son savunma hattidir.
--
-- Bu dosya spring.sql.init.mode=always ile HER baslangicta yeniden calisir; bu yuzden
-- tum tanimlar idempotent olmalidir (CREATE OR REPLACE FUNCTION / DROP TRIGGER IF
-- EXISTS + CREATE TRIGGER).
--
-- NOT: comment/store_reply_text serbest metin alanlarina HICBIR kisitlama
-- eklenmemistir (masum kullanici girdisini etkilememesi icin); yalnizca uzunluk
-- zaten @Column(length=1000) ile Hibernate tarafinda VARCHAR(1000) olarak siniirli.
--
-- NOT (statement separator): bu dosya cift noktali virgul ayiricisiyla parcalaniyor (bkz.
-- application.yaml -> spring.sql.init.separator). Spring'in varsayilan tek ';'
-- ayiricisi, $$ dolar-quote fonksiyon govdesi icindeki sıradan ';' karakterlerini de
-- YANLISLIKLA ayirici sanip script'i ortadan boler. Fonksiyon govdesi icindeki TEK ';'
-- karakterlerine DOKUNULMADI - yalnizca en disaridaki (top-level) her ifadenin sonuna
-- ikinci bir ';' eklendi.

-- ------------------------------------------------------------------
-- 1) reviews: rating 1-5 araliginda olmali; hangi urune/musteriye ait oldugu ve
--    olusturulma zamani degistirilemez (yorum baskasina/baska urune devredilemez)
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_review_validate_reviews() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.rating < 1 OR NEW.rating > 5 THEN
        RAISE EXCEPTION 'reviews.rating 1 ile 5 arasinda olmalidir (verilen: %)', NEW.rating;
    END IF;
    IF TG_OP = 'UPDATE' THEN
        IF NEW.product_id IS DISTINCT FROM OLD.product_id THEN
            RAISE EXCEPTION 'reviews.product_id degistirilemez.';
        END IF;
        IF NEW.customer_id IS DISTINCT FROM OLD.customer_id THEN
            RAISE EXCEPTION 'reviews.customer_id degistirilemez.';
        END IF;
        IF NEW.created_at IS DISTINCT FROM OLD.created_at THEN
            RAISE EXCEPTION 'reviews.created_at degistirilemez.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;;

DROP TRIGGER IF EXISTS trg_review_validate_reviews ON reviews;;
CREATE TRIGGER trg_review_validate_reviews
    BEFORE INSERT OR UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION fn_review_validate_reviews();;

-- ------------------------------------------------------------------
-- 2) purchase_eligibility: hangi siparise/musteriye/urune ait oldugu degistirilemez
--    (yalnizca status PENDING_REVIEW -> REVIEWED gecisi icin var olan alanlar sabit
--    kalmali)
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_review_validate_purchase_eligibility() RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.order_id IS DISTINCT FROM OLD.order_id OR
           NEW.customer_id IS DISTINCT FROM OLD.customer_id OR
           NEW.product_id IS DISTINCT FROM OLD.product_id THEN
            RAISE EXCEPTION 'purchase_eligibility.order_id/customer_id/product_id degistirilemez.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;;

DROP TRIGGER IF EXISTS trg_review_validate_purchase_eligibility ON purchase_eligibility;;
CREATE TRIGGER trg_review_validate_purchase_eligibility
    BEFORE UPDATE ON purchase_eligibility
    FOR EACH ROW EXECUTE FUNCTION fn_review_validate_purchase_eligibility();;

-- ------------------------------------------------------------------
-- 3) outbox_event: payload gecerli JSON olmali
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_review_validate_outbox() RETURNS TRIGGER AS $$
BEGIN
    BEGIN
        PERFORM NEW.payload::jsonb;
    EXCEPTION WHEN OTHERS THEN
        RAISE EXCEPTION 'outbox_event.payload gecerli JSON olmalidir';
    END;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;;

DROP TRIGGER IF EXISTS trg_review_validate_outbox ON outbox_event;;
CREATE TRIGGER trg_review_validate_outbox
    BEFORE INSERT OR UPDATE ON outbox_event
    FOR EACH ROW EXECUTE FUNCTION fn_review_validate_outbox();;

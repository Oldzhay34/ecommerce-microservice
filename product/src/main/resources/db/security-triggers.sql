-- product-service - guvenlik sertlestirme trigger'lari
--
-- AMAC: SQL injection'a KARSI degil (ProductRepository/ReviewRepository JPA derived
-- query kullanir, ProductPersistenceAdapter'daki tek JdbcTemplate cagrisi ? parametre
-- binding ile parametrelenmis - bkz. guvenlik denetimi). Trigger'lar sorgu calistiktan
-- SONRA satir seviyesinde tetiklenir, enjeksiyonu yakalayamaz; buradaki amac uygulama
-- katmanini bypass eden bir cagiriciya (kompromize servis, bug, dogrudan DB erisimi)
-- karsi son savunma hattidir.
--
-- Bu dosya spring.sql.init.mode=always ile HER baslangicta yeniden calisir; bu yuzden
-- tum tanimlar idempotent olmalidir (CREATE OR REPLACE FUNCTION / DROP TRIGGER IF
-- EXISTS + CREATE TRIGGER). ALTER TABLE ADD CONSTRAINT kullanilmiyor - Postgres'te
-- idempotent degildir ve ikinci calistirmada hata verirdi; onun yerine trigger
-- fonksiyonlari icinde esdeger kontroller yapilir.
--
-- NOT: products/reviews tablolarindaki serbest metin alanlarina (name, comment vb.)
-- HICBIR kisitlama eklenmemistir - masum kullanici girdisini etkilememesi icin.

-- ------------------------------------------------------------------
-- 1) products: price/stock negatif olamaz, id/store_id olusturulduktan sonra
--    degistirilemez (bir urun sessizce baska bir magazaya devredilemez)
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_product_validate_products() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.price < 0 THEN
        RAISE EXCEPTION 'products.price negatif olamaz (verilen: %)', NEW.price;
    END IF;
    IF NEW.stock < 0 THEN
        RAISE EXCEPTION 'products.stock negatif olamaz (verilen: %)', NEW.stock;
    END IF;
    IF TG_OP = 'UPDATE' THEN
        IF NEW.id IS DISTINCT FROM OLD.id THEN
            RAISE EXCEPTION 'products.id degistirilemez.';
        END IF;
        IF NEW.store_id IS DISTINCT FROM OLD.store_id THEN
            RAISE EXCEPTION 'products.store_id degistirilemez (urun baska magazaya devredilemez).';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_product_validate_products ON products;
CREATE TRIGGER trg_product_validate_products
    BEFORE INSERT OR UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION fn_product_validate_products();

-- ------------------------------------------------------------------
-- 2) reviews (product-service'in kendi kopyasi): rating 1-5 araliginda olmali,
--    hangi urune/musteriye ait oldugu olusturulduktan sonra degistirilemez
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_product_validate_reviews() RETURNS TRIGGER AS $$
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
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_product_validate_reviews ON reviews;
CREATE TRIGGER trg_product_validate_reviews
    BEFORE INSERT OR UPDATE ON reviews
    FOR EACH ROW EXECUTE FUNCTION fn_product_validate_reviews();

-- ------------------------------------------------------------------
-- NOT: outbox_event.payload zaten "jsonb" tipinde tanimli (bkz. OutboxJpaEvent) -
-- Postgres bu kolona gecersiz JSON yazilmasini SUTUN TIPI seviyesinde zaten
-- reddeder; ayrica bir JSON dogrulama trigger'i GEREKSIZDIR.
-- ------------------------------------------------------------------

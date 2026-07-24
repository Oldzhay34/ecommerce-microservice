-- auth (identity) service - guvenlik sertlestirme trigger'lari
--
-- AMAC: SQL injection'a KARSI degil (UserRepository yalnizca bir derived-name metodu
-- kullanir; OutboxAdapter/OutboxEventPublisher'daki JdbcTemplate cagrilari zaten ?
-- parametre binding ile parametrelenmis - bkz. guvenlik denetimi). Trigger'lar sorgu
-- calistiktan SONRA satir seviyesinde tetiklenir, enjeksiyonu yakalayamaz; buradaki
-- amac uygulama katmanini bypass eden bir cagiriciya (kompromize servis, bug,
-- dogrudan DB erisimi) karsi son savunma hattidir.
--
-- Bu dosya spring.sql.init.mode=always ile HER baslangicta yeniden calisir (yalnizca
-- dev/prod profillerinde - test profili H2 kullandigi icin BILEREK devre disi
-- birakildi); bu yuzden tum tanimlar idempotent olmalidir (CREATE OR REPLACE
-- FUNCTION / DROP TRIGGER IF EXISTS + CREATE TRIGGER).
--
-- NOT: users.name ve users.email_encrypted @Convert(AesEncryptionConverter) ile
-- ŞİFRELİ saklanir - DB bu kolonlarda YALNIZCA sifreli metni (rastgele gorunumlu
-- byte dizisini) gorur. Bu yuzden bu iki alana format/kontrol karakteri dogrulamasi
-- eklenMEDI: sifreli veri uzerinde boyle bir kontrol anlamsizdir ve rastgele
-- sifreleme ciktisina bagli olarak sahte pozitif uretebilirdi.

-- ------------------------------------------------------------------
-- 1) users: role_name yalnizca bilinen rollerden biri olabilir (yetki yukseltme
--   savunmasi); id olusturulduktan sonra degistirilemez
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_auth_validate_users() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.role_name NOT IN ('CUSTOMER', 'STORE', 'ADMIN') THEN
        RAISE EXCEPTION 'users.role_name gecersiz: % (yalnizca CUSTOMER/STORE/ADMIN kabul edilir)', NEW.role_name;
    END IF;
    IF TG_OP = 'UPDATE' AND NEW.id IS DISTINCT FROM OLD.id THEN
        RAISE EXCEPTION 'users.id degistirilemez.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auth_validate_users ON users;
CREATE TRIGGER trg_auth_validate_users
    BEFORE INSERT OR UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_auth_validate_users();

-- ------------------------------------------------------------------
-- 2) customers: loyalty_points negatif olamaz
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_auth_validate_customers() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.loyalty_points < 0 THEN
        RAISE EXCEPTION 'customers.loyalty_points negatif olamaz (verilen: %)', NEW.loyalty_points;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auth_validate_customers ON customers;
CREATE TRIGGER trg_auth_validate_customers
    BEFORE INSERT OR UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION fn_auth_validate_customers();

-- ------------------------------------------------------------------
-- 3) outbox_event: payload GECERLI JSON olmak zorunda (OutboxAdapter'daki
--    String.format ile kacissiz JSON uretimi hatasinin DB seviyesinde yakalanmasi
--    icin - bkz. guvenlik denetiminde bulunan sorun); published_at set edildikten
--    sonra satir govdesi degistirilemez
-- ------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_auth_validate_outbox() RETURNS TRIGGER AS $$
BEGIN
    BEGIN
        PERFORM NEW.payload::jsonb;
    EXCEPTION WHEN OTHERS THEN
        RAISE EXCEPTION 'outbox_event.payload gecerli JSON olmalidir';
    END;

    IF TG_OP = 'UPDATE' AND OLD.published_at IS NOT NULL THEN
        IF NEW.event_type IS DISTINCT FROM OLD.event_type OR
           NEW.payload IS DISTINCT FROM OLD.payload OR
           NEW.created_at IS DISTINCT FROM OLD.created_at THEN
            RAISE EXCEPTION 'Yayinlanmis (published_at dolu) bir outbox_event satirinin govdesi degistirilemez.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_auth_validate_outbox ON outbox_event;
CREATE TRIGGER trg_auth_validate_outbox
    BEFORE INSERT OR UPDATE ON outbox_event
    FOR EACH ROW EXECUTE FUNCTION fn_auth_validate_outbox();

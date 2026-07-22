# API Gateway (Thin Edge Validation)

E-ticaret mikroservis platformu için Spring Cloud Gateway. Mevcut servislere **dokunmadan** devreye alınır: JWT'yi kenarda doğrular, `Authorization` header'ını olduğu gibi downstream'e geçirir, servisler kendi güvenlik filtreleriyle çalışmaya devam eder.

## Route Tablosu

| Gateway Path | Hedef (network içi) | JWT |
|---|---|---|
| `/api/v1/auth/**` | `auth-service:8082` | Muaf (public) |
| `/api/v1/products/search` | `product-service:8080` | Muaf (public) |
| `/api/v1/products/**` | `product-service:8080` | Zorunlu |
| `/api/orders/**` | `order-service:8081` | Zorunlu |
| `/api/carts/**` | `cart-service:8080` | Zorunlu (dikkat: çoğul) |
| `/api/payments/**` | `payment-service:8084` | Zorunlu |
| `/api/reviews/**` | `review-service:8084` | Zorunlu |

Gateway host portu: **8080** → Frontend `http://localhost:8080` adresine istek atar.

## Kurulumdan ÖNCE yapılması gerekenler (kritik ön koşullar)

1. **JWT_SECRET birleştirme.** Gateway'in `JWT_SECRET`'ı, `auth-service`'in token'ı imzaladığı secret ile **birebir aynı** olmalı. Şu an backend'de secret değerleri servisten servise farklı — bunları tek bir değerde toplayın ve gateway ile auth-service aynı değeri kullansın. Eşleşmezse gateway tüm token'ları 401 ile reddeder.

2. **@CrossOrigin kaldırma.** `auth-service` ve `product-service` controller'larındaki `@CrossOrigin` anotasyonlarını silin. CORS artık gateway'de merkezî; ikisi birden olursa çift header hatası çıkar.

3. **Redis DB index.** Gateway rate limiter Redis DB index **5** kullanır (diğer servisler 0–4). Çakışma yok, ama Redis paylaşımlı olduğu için not edin.

## Çalıştırma

`docker-compose.gateway.yml` içindeki `api-gateway` bloğunu ana compose dosyanıza ekleyin ve `.env`'e `JWT_SECRET_KEY` tanımlayın:

```
docker compose up -d --build api-gateway
```

Health check: `http://localhost:8080/actuator/health`

## Route stratejisi: yml mi Java mı?

Route'lar **application.yml** içinde aktiftir (varsayılan). `RouteConfig.java` ise programatik alternatiftir ve `gateway.route-config.java-enabled=true` verilmedikçe **devre dışıdır**. İkisini aynı anda açmayın.

## Bilinen teknik borç (gateway kapsamı DIŞI)

- **userId tip tutarsızlığı:** cart `Long`, payment `UUID`, order/review `String`. Gateway ham string geçirir; dönüşüm her serviste kalır. Subject formatı değişirse bir serviste runtime hata riski var. Kalıcı çözüm: tüm servislerde userId tipini standartlaştırmak.

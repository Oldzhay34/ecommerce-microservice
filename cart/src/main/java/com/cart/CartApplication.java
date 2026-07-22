package com.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BUG FIX: @EnableScheduling eksikti. OutboxEventPublisher#publishEvents
 * @Scheduled ile işaretli olmasına rağmen scheduling altyapısı hiç
 * kurulmadığı için ASLA çalışmıyordu: outbox tablosuna yazılan
 * CartUpdatedEvent/CartClearedEvent kayıtları processed=false olarak
 * sonsuza kadar birikiyor, RabbitMQ'ya hiçbir sepet olayı yayınlanmıyordu.
 * (Aynı kalıbı kullanan order/product/media servislerinin hepsinde bu
 * anotasyon mevcut.)
 */
@SpringBootApplication
@EnableScheduling
public class CartApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartApplication.class, args);
    }

}

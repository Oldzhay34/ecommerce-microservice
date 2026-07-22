package com.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BUG DÜZELTMESİ: @EnableScheduling yoktu. OutboxEventPublisher#publishOutboxEvents
 * @Scheduled ile işaretli olmasına rağmen scheduling altyapısı hiç kurulmadığı için
 * ASLA tetiklenmiyordu; outbox_event tablosundaki PaymentCompleted/PaymentFailed/
 * PaymentRefunded kayıtları birikiyor, RabbitMQ'ya tek bir ödeme olayı bile
 * yayınlanmıyordu (sipariş servisi ödemenin tamamlandığını hiç öğrenemiyordu).
 */
@SpringBootApplication
@EnableScheduling
public class PaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }

}

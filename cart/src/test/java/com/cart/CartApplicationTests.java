package com.cart;

import com.cart.subsystem.AbstractCartSubsystemTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Çıplak @SpringBootTest ile context yüklenemiyordu: cart servisi ayağa
 * kalkarken Postgres, RabbitMQ ve Redis bağlantılarına ihtiyaç duyar. Bu yüzden
 * (order servisindeki OrderApplicationTests kalıbında olduğu gibi) test
 * container'larını sağlayan AbstractCartSubsystemTest'ten türetilmiştir.
 *
 * ÇALIŞTIRMA ÖNKOŞULU: Docker.
 */
@DisplayName("SUBSYSTEM - CartApplication context yüklenmesi")
class CartApplicationTests extends AbstractCartSubsystemTest {

    @Test
    @DisplayName("S10: contextLoads - Tüm bean grafiği gerçek altyapıyla ayağa kalkar")
    void contextLoads() {
    }
}

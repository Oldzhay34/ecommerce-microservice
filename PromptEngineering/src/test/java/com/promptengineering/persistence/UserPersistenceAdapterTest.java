package com.promptengineering.persistence;

import com.promptengineering.auth.domain.model.Customer;
import com.promptengineering.auth.domain.model.Store;
import com.promptengineering.auth.domain.model.User;
import com.promptengineering.auth.infrastructure.persistence.adapter.UserPersistenceAdapter;
import com.promptengineering.auth.infrastructure.persistence.entity.AesEncryptionConverter;
import com.promptengineering.auth.infrastructure.persistence.mapper.UserEntityMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// Spring Boot 4.x: @DataJpaTest YENI paket -> org.springframework.boot.data.jpa.test.autoconfigure
// (spring-boot-starter-data-jpa-test dependency'si GEREKLI - pom.xml'e eklendi)
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DEGISIKLIK 1 (Spring Boot 4.x): @DataJpaTest import paketi degisti:
 *   ESKI: org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
 *   YENI: org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
 *   Bunun icin pom.xml'e spring-boot-starter-data-jpa-test (test scope) EKLENDI.
 *
 * DEGISIKLIK 2 (AES secret okunusu): AesEncryptionConverter artik System.getenv YERINE
 *   @Value("${app.security.aes-secret}") okuyor. Bu yuzden:
 *    - Ortam degiskeni (AES_SECRET_KEY) ayarlama zorunlulugu KALKTI.
 *    - Anahtar test icin @TestPropertySource ile app.security.aes-secret olarak veriliyor (32 byte).
 *    - Converter bir @Component oldugu icin @Import ile context'e ekleniyor.
 *
 * @DataJpaTest sadece JPA slice'ini yukler; adapter, mapper ve converter @Component oldugundan
 * hepsi @Import ile manuel eklenir.
 */
@DataJpaTest
@Import({UserPersistenceAdapter.class, UserEntityMapper.class, AesEncryptionConverter.class})
@TestPropertySource(properties = {
        "app.security.aes-secret=12345678901234567890123456789012",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:authtestdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
})
@DisplayName("UserPersistenceAdapter Integration Tests (H2)")
class UserPersistenceAdapterTest {

    @Autowired
    private UserPersistenceAdapter userPersistenceAdapter;

    @Test
    @DisplayName("C1: save + findByEmail - Customer ayni veriyle geri okunur")
    void save_thenFindByEmail_shouldReturnCustomerWithSameData() {
        Customer customer = new Customer(
                UUID.randomUUID(), "Ali Veli", "ali@example.com", "hashed-pw",
                true, "Istanbul", "555-1234", 100);

        userPersistenceAdapter.save(customer);
        Optional<User> found = userPersistenceAdapter.findByEmail("ali@example.com");

        assertThat(found).isPresent();
        User result = found.get();
        assertThat(result.getEmail()).isEqualTo("ali@example.com");
        assertThat(result.getName()).isEqualTo("Ali Veli");
        assertThat(result.getRole()).isEqualTo("ROLE_CUSTOMER");
        assertThat(result.isVerified()).isTrue();
        assertThat(result).isInstanceOf(Customer.class);
    }

    @Test
    @DisplayName("C2: findByEmail - kayit yoksa bos Optional doner")
    void findByEmail_whenNotExists_shouldReturnEmptyOptional() {
        Optional<User> found = userPersistenceAdapter.findByEmail("yok@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("C3: save - dogru rol ve alt tip persist edilir (Store)")
    void save_shouldPersistCorrectRoleAndSubtype() {
        Store store = new Store(
                UUID.randomUUID(), "Market X", "store@example.com", "hashed-pw",
                true, "Market X Ltd", "1234567890", 4.5);

        userPersistenceAdapter.save(store);
        Optional<User> found = userPersistenceAdapter.findByEmail("store@example.com");

        assertThat(found).isPresent();
        assertThat(found.get()).isInstanceOf(Store.class);
        assertThat(found.get().getRole()).isEqualTo("ROLE_STORE");
    }
}
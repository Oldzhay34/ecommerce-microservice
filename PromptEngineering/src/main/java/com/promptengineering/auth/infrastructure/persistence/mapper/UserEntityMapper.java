package com.promptengineering.auth.infrastructure.persistence.mapper;

import com.promptengineering.auth.domain.model.Admin;
import com.promptengineering.auth.domain.model.Customer;
import com.promptengineering.auth.domain.model.Store;
import com.promptengineering.auth.domain.model.User;
import com.promptengineering.auth.infrastructure.persistence.entity.AdminJpaEntity;
import com.promptengineering.auth.infrastructure.persistence.entity.CustomerJpaEntity;
import com.promptengineering.auth.infrastructure.persistence.entity.StoreJpaEntity;
import com.promptengineering.auth.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserEntityMapper {

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        if (entity instanceof CustomerJpaEntity customerEntity) {
            return new Customer(
                    customerEntity.getId(),
                    customerEntity.getName(),
                    customerEntity.getEmail(),
                    customerEntity.getPasswordHash(),
                    customerEntity.isVerified(),
                    customerEntity.getShippingAddress(),
                    customerEntity.getPhoneNumber(),
                    customerEntity.getLoyaltyPoints()
            );
        } else if (entity instanceof StoreJpaEntity storeEntity) {
            return new Store(
                    storeEntity.getId(),
                    storeEntity.getName(),
                    storeEntity.getEmail(),
                    storeEntity.getPasswordHash(),
                    storeEntity.isVerified(),
                    storeEntity.getStoreName(),
                    storeEntity.getTaxNumber(),
                    storeEntity.getStoreRating()
            );
        } else if (entity instanceof AdminJpaEntity adminEntity) {
            return new Admin(
                    adminEntity.getId(),
                    adminEntity.getName(),
                    adminEntity.getEmail(),
                    adminEntity.getPasswordHash(),
                    adminEntity.isVerified(),
                    adminEntity.getDepartment(),
                    adminEntity.getAccessLevel()
            );
        }

        throw new IllegalArgumentException("Bilinmeyen UserJpaEntity tipi: " + entity.getClass().getName());
    }

    public UserJpaEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }

        if (domain instanceof Customer customer) {
            CustomerJpaEntity entity = new CustomerJpaEntity();
            setCommonEntityFields(entity, customer);
            entity.setShippingAddress(customer.getShippingAddress());
            entity.setPhoneNumber(customer.getPhoneNumber());
            entity.setLoyaltyPoints(customer.getLoyaltyPoints());
            return entity;
        } else if (domain instanceof Store store) {
            StoreJpaEntity entity = new StoreJpaEntity();
            setCommonEntityFields(entity, store);
            entity.setStoreName(store.getStoreName());
            entity.setTaxNumber(store.getTaxNumber());
            entity.setStoreRating(store.getStoreRating());
            return entity;
        } else if (domain instanceof Admin admin) {
            AdminJpaEntity entity = new AdminJpaEntity();
            setCommonEntityFields(entity, admin);
            entity.setDepartment(admin.getDepartment());
            entity.setAccessLevel(admin.getAccessLevel());
            return entity;
        }

        throw new IllegalArgumentException("Bilinmeyen User domain tipi: " + domain.getClass().getName());
    }

    // Ortak alanların atamasını yapan yardımcı metot
    private void setCommonEntityFields(UserJpaEntity entity, User domain) {
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setEmail(domain.getEmail());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setRole(domain.getRole());
        entity.setVerified(domain.isVerified());
    }
}
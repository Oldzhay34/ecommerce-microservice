package com.mediaservice.application.port.out;

import java.util.UUID;

public interface OutboxPort {

    /**
     * Ayni transaction icinde outbox_event tablosuna yazar.
     * Servis RabbitMQ'ya DOGRUDAN mesaj atmaz.
     */
    void append(String aggregateType,
                UUID aggregateId,
                String eventType,
                String routingKey,
                Object payload);
}
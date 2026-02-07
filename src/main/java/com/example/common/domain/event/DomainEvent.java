package com.example.common.domain.event;

import java.time.Instant;

public interface DomainEvent {
    Instant occurredAt();
}
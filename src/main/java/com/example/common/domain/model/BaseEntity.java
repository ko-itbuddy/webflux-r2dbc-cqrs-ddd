package com.example.common.domain.model;

import com.example.common.domain.event.DomainEvent;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.Instant;
import java.util.Collection;

@Getter
@MappedSuperclass
public abstract class BaseEntity<T extends AbstractAggregateRoot<T>> extends AbstractAggregateRoot<T> {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    protected Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    protected Instant updatedAt;

    @Column(name = "deleted_at")
    protected Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void delete() {
        this.deletedAt = Instant.now();
    }

    @Transient
    public Collection<Object> getRawEvents() {
        return domainEvents();
    }

    public void clearEvents() {
        this.clearDomainEvents();
    }
}
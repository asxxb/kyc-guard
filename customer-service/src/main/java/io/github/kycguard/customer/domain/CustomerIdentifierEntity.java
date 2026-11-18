package io.github.kycguard.customer.domain;

import io.github.kycguard.common.enums.IdentifierType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customer_identifier")
public class CustomerIdentifierEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IdentifierType type;

    @Column(name = "identifier_value", nullable = false, length = 100)
    private String identifierValue;

    protected CustomerIdentifierEntity() {
    }

    public CustomerIdentifierEntity(UUID id, CustomerEntity customer, IdentifierType type, String identifierValue) {
        this.id = id != null ? id : UUID.randomUUID();
        this.customer = customer;
        this.type = type;
        this.identifierValue = identifierValue;
    }

    public UUID getId() {
        return id;
    }

    public CustomerEntity getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerEntity customer) {
        this.customer = customer;
    }

    public IdentifierType getType() {
        return type;
    }

    public String getIdentifierValue() {
        return identifierValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerIdentifierEntity that = (CustomerIdentifierEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

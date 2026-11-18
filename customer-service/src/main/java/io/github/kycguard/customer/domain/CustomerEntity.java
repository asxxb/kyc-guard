package io.github.kycguard.customer.domain;

import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.common.enums.RiskBand;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "customer")
public class CustomerEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerType type;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "customer_alias", joinColumns = @JoinColumn(name = "customer_id"))
    @Column(name = "alias", nullable = false)
    private Set<String> aliases = new HashSet<>();

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "incorporation_date")
    private LocalDate incorporationDate;

    @Column(nullable = false, length = 2)
    private String country;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CustomerAddressEntity> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CustomerIdentifierEntity> identifiers = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CustomerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_band", length = 20)
    private RiskBand riskBand;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerEntity() {
    }

    public CustomerEntity(
            UUID id,
            CustomerType type,
            String fullName,
            Set<String> aliases,
            LocalDate dateOfBirth,
            LocalDate incorporationDate,
            String country,
            CustomerStatus status,
            RiskBand riskBand,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.type = type;
        this.fullName = fullName;
        if (aliases != null) {
            this.aliases.addAll(aliases);
        }
        this.dateOfBirth = dateOfBirth;
        this.incorporationDate = incorporationDate;
        this.country = country;
        this.status = status != null ? status : CustomerStatus.PENDING_SCREENING;
        this.riskBand = riskBand;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public void addAddress(CustomerAddressEntity address) {
        address.setCustomer(this);
        this.addresses.add(address);
    }

    public void addIdentifier(CustomerIdentifierEntity identifier) {
        identifier.setCustomer(this);
        this.identifiers.add(identifier);
    }

    public void updateStatus(CustomerStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void updateRiskBand(RiskBand newRiskBand) {
        this.riskBand = newRiskBand;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public CustomerType getType() {
        return type;
    }

    public String getFullName() {
        return fullName;
    }

    public Set<String> getAliases() {
        return aliases;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public LocalDate getIncorporationDate() {
        return incorporationDate;
    }

    public String getCountry() {
        return country;
    }

    public List<CustomerAddressEntity> getAddresses() {
        return addresses;
    }

    public List<CustomerIdentifierEntity> getIdentifiers() {
        return identifiers;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public RiskBand getRiskBand() {
        return riskBand;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerEntity that = (CustomerEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

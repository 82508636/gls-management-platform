package pt.glsmanagement.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "recipients")
class Recipient {
    @Id private UUID id;
    @Column(name = "deduplication_key", nullable = false, unique = true, length = 64) private String deduplicationKey;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "contact_name", length = 200) private String contactName;
    @Column(nullable = false, length = 500) private String address;
    @Column(name = "postal_code", nullable = false, length = 20) private String postalCode;
    @Column(nullable = false, length = 120) private String locality;
    @Column(nullable = false, length = 2) private String country;
    @Column(length = 254) private String email;
    @Column(length = 50) private String phone;
    @Column(length = 50) private String mobile;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "last_used_at", nullable = false) private OffsetDateTime lastUsedAt;

    protected Recipient() {}

    static Recipient create(String key, RecipientRegistration input) {
        var recipient = new Recipient();
        recipient.id = UUID.randomUUID();
        recipient.deduplicationKey = key;
        recipient.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        recipient.apply(input);
        return recipient;
    }

    void reuse(RecipientRegistration input) { apply(input); }

    private void apply(RecipientRegistration input) {
        name = input.name().trim(); contactName = optional(input.contactName()); address = input.address().trim();
        postalCode = input.postalCode().trim(); locality = input.locality().trim(); country = input.country().trim().toUpperCase();
        email = optional(input.email()); phone = optional(input.phone()); mobile = optional(input.mobile());
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC); lastUsedAt = updatedAt;
    }

    private static String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    UUID id() { return id; } String name() { return name; } String contactName() { return contactName; }
    String address() { return address; } String postalCode() { return postalCode; } String locality() { return locality; }
    String country() { return country; } String email() { return email; } String phone() { return phone; }
    String mobile() { return mobile; } OffsetDateTime lastUsedAt() { return lastUsedAt; }
}

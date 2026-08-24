package pt.glsmanagement.platform.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RecipientResponse(
        UUID id, String name, String contactName, String address, String postalCode, String locality,
        String country, String email, String phone, String mobile, OffsetDateTime lastUsedAt
) {
    static RecipientResponse from(Recipient recipient) {
        return new RecipientResponse(recipient.id(), recipient.name(), recipient.contactName(), recipient.address(),
                recipient.postalCode(), recipient.locality(), recipient.country(), recipient.email(), recipient.phone(),
                recipient.mobile(), recipient.lastUsedAt());
    }
}

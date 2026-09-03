package pt.glsmanagement.workforce.reference;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

final class ReferenceCatalogDtos {
    private ReferenceCatalogDtos() {}

    record CreateRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String id,
            @NotBlank @Size(max = 120) String designation) {}
    record UpdateRequest(@NotBlank @Size(max = 120) String designation) {}
    record StatusRequest(@NotNull Boolean active) {}

    record Response(
            String id, String designation, boolean active,
            OffsetDateTime createdAt, String createdBy, OffsetDateTime updatedAt, String updatedBy) {
        static Response from(AccountProfile value) {
            return new Response(value.id(), value.designation(), value.active(), value.createdAt(), value.createdBy(),
                    value.updatedAt(), value.updatedBy());
        }

        static Response from(ProfessionalCategory value) {
            return new Response(value.id(), value.designation(), value.active(), value.createdAt(), value.createdBy(),
                    value.updatedAt(), value.updatedBy());
        }
    }
}

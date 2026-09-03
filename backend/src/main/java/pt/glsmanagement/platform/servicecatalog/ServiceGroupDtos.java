package pt.glsmanagement.platform.servicecatalog;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

final class ServiceGroupDtos {
    private ServiceGroupDtos() {}
    record CreateRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String id,
                         @NotBlank @Size(max = 160) String designation) {}
    record UpdateRequest(@NotBlank @Size(max = 160) String designation) {}
    record StatusRequest(@NotNull Boolean active) {}
    record Response(String id, String designation, boolean active, OffsetDateTime createdAt, String createdBy,
                    OffsetDateTime updatedAt, String updatedBy, long version) {
        static Response from(ServiceGroup value) {
            return new Response(value.id(), value.designation(), value.active(), value.createdAt(), value.createdBy(),
                    value.updatedAt(), value.updatedBy(), value.version());
        }
    }
}

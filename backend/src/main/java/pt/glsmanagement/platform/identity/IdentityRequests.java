package pt.glsmanagement.platform.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record JoinerRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull PlatformRole role,
        @NotBlank @Size(min = 12, max = 128) String temporaryPassword
) {}

record MoverRequest(@NotNull PlatformRole role) {}

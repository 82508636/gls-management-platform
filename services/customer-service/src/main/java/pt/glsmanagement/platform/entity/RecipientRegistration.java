package pt.glsmanagement.platform.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecipientRegistration(
        @Size(max = 30) String code,
        @NotBlank @Size(max = 200) String designation,
        @Size(max = 200) String contactName,
        @NotBlank @Size(max = 500) String address,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Size(max = 120) String locality,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String country,
        @Email @Size(max = 254) String email,
        @Size(max = 50) String phone,
        @Size(max = 50) String mobile
) {}

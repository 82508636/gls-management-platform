package pt.glsmanagement.platform.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VatValidationRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String countryCode,
        @NotBlank @Size(max = 32) String vatNumber,
        @NotNull VatValidationSubjectType subjectType
) {}

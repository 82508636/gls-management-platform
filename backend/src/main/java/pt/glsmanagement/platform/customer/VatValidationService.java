package pt.glsmanagement.platform.customer;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;

@Service
class VatValidationService {
    private static final Set<String> VIES_COUNTRIES = Set.of(
            "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "EL", "ES", "FI", "FR",
            "HR", "HU", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO",
            "SE", "SI", "SK", "XI"
    );

    private final PortugueseVatNumberValidator portugueseValidator;
    private final VatValidationGateway gateway;

    VatValidationService(PortugueseVatNumberValidator portugueseValidator, VatValidationGateway gateway) {
        this.portugueseValidator = portugueseValidator;
        this.gateway = gateway;
    }

    VatValidationResponse validate(VatValidationRequest request) {
        String countryCode = normalizeCountry(request.countryCode());
        String vatNumber = normalizeVatNumber(countryCode, request.vatNumber());
        boolean formatValid = isFormatValid(countryCode, vatNumber);

        if (!formatValid) {
            return response(countryCode, vatNumber, false, VatValidationStatus.NOT_CHECKED);
        }
        if (request.subjectType() == VatValidationSubjectType.PRIVATE || !VIES_COUNTRIES.contains(countryCode)) {
            return response(countryCode, vatNumber, true, VatValidationStatus.NOT_APPLICABLE);
        }

        try {
            var result = gateway.validate(countryCode, vatNumber);
            var status = result.valid() ? VatValidationStatus.VALID : VatValidationStatus.NOT_VALID;
            return response(countryCode, vatNumber, true, status);
        } catch (VatValidationProviderUnavailableException exception) {
            return response(countryCode, vatNumber, true, VatValidationStatus.UNAVAILABLE);
        }
    }

    private boolean isFormatValid(String countryCode, String vatNumber) {
        if (!vatNumber.matches("[A-Z0-9]{2,20}")) {
            return false;
        }
        return !"PT".equals(countryCode) || portugueseValidator.isStructurallyValid(vatNumber);
    }

    private static String normalizeCountry(String value) {
        String countryCode = value.trim().toUpperCase(Locale.ROOT);
        return "GR".equals(countryCode) ? "EL" : countryCode;
    }

    private static String normalizeVatNumber(String countryCode, String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s.\\-]", "");
        if ("EL".equals(countryCode) && normalized.startsWith("GR") && normalized.length() > 2) {
            return normalized.substring(2);
        }
        if (normalized.startsWith(countryCode) && normalized.length() > countryCode.length()) {
            return normalized.substring(countryCode.length());
        }
        return normalized;
    }

    private static VatValidationResponse response(
            String countryCode,
            String vatNumber,
            boolean formatValid,
            VatValidationStatus status
    ) {
        return new VatValidationResponse(
                countryCode,
                vatNumber,
                formatValid,
                status,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}

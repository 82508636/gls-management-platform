package pt.glsmanagement.platform.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;

@Service
class VatValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VatValidationService.class);
    private static final Set<String> VIES_COUNTRIES = Set.of(
            "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "EL", "ES", "FI", "FR",
            "HR", "HU", "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO",
            "SE", "SI", "SK", "XI");

    private final PortugueseVatNumberValidator portugueseValidator;
    private final VatValidationGateway gateway;

    VatValidationService(PortugueseVatNumberValidator portugueseValidator, VatValidationGateway gateway) {
        this.portugueseValidator = portugueseValidator;
        this.gateway = gateway;
    }

    VatValidationResponse validate(VatValidationRequest request) {
        var country = normalizeCountry(request.countryCode());
        var vat = normalizeVatNumber(country, request.vatNumber());
        var formatValid = isFormatValid(country, vat);
        if (!formatValid) return response(country, vat, false, VatValidationStatus.NOT_CHECKED);
        if (request.subjectType() == VatValidationSubjectType.PRIVATE || !VIES_COUNTRIES.contains(country)) {
            return response(country, vat, true, VatValidationStatus.NOT_APPLICABLE);
        }
        try {
            var result = gateway.validate(country, vat);
            if (!result.valid()) return response(country, vat, true, VatValidationStatus.NOT_VALID);
            return response(country, vat, true, VatValidationStatus.VALID,
                    VatRegistrationDetailsMapper.map(country, result.registeredName(), result.registeredAddress(),
                            result.registeredStreet(), result.registeredPostalCode(), result.registeredLocality()));
        } catch (VatValidationProviderUnavailableException exception) {
            LOGGER.warn("VIES validation provider unavailable for country {} ({})",
                    country, rootCauseName(exception));
            return response(country, vat, true, VatValidationStatus.UNAVAILABLE);
        }
    }

    private boolean isFormatValid(String country, String vat) {
        return vat.matches("[A-Z0-9]{2,20}")
                && (!"PT".equals(country) || portugueseValidator.isStructurallyValid(vat));
    }

    private static String normalizeCountry(String value) {
        var country = value.trim().toUpperCase(Locale.ROOT);
        return "GR".equals(country) ? "EL" : country;
    }

    private static String normalizeVatNumber(String country, String value) {
        var vat = value.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s.\\-]", "");
        if ("EL".equals(country) && vat.startsWith("GR") && vat.length() > 2) return vat.substring(2);
        if (vat.startsWith(country) && vat.length() > country.length()) return vat.substring(country.length());
        return vat;
    }

    private static VatValidationResponse response(
            String country, String vat, boolean formatValid, VatValidationStatus status) {
        return response(country, vat, formatValid, status, VatRegistrationDetailsMapper.Details.empty());
    }

    private static VatValidationResponse response(
            String country, String vat, boolean formatValid, VatValidationStatus status,
            VatRegistrationDetailsMapper.Details details) {
        return new VatValidationResponse(country, vat, formatValid, status,
                details.name(), details.address(), details.postalCode(), details.locality(),
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    private static String rootCauseName(Throwable exception) {
        var cause = exception;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getClass().getSimpleName();
    }
}

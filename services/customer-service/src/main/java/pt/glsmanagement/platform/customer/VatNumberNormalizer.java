package pt.glsmanagement.platform.customer;

import java.util.Locale;

final class VatNumberNormalizer {
    private VatNumberNormalizer() {}

    static NormalizedVat normalize(String countryValue, String vatValue) {
        if (countryValue == null || countryValue.isBlank() || vatValue == null || vatValue.isBlank()) {
            throw new IllegalArgumentException("VAT country and number are required");
        }
        var country = countryValue.trim().toUpperCase(Locale.ROOT);
        if ("GR".equals(country)) country = "EL";
        if (!country.matches("[A-Z]{2}")) throw new IllegalArgumentException("Invalid VAT country");
        var number = vatValue.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s.\\-/]", "");
        if (number.startsWith(country) && number.length() > country.length()) {
            number = number.substring(country.length());
        } else if ("EL".equals(country) && number.startsWith("GR") && number.length() > 2) {
            number = number.substring(2);
        }
        if (number.isBlank() || !number.matches("[A-Z0-9]+")) {
            throw new IllegalArgumentException("Invalid VAT number");
        }
        return new NormalizedVat(country, number, country + ":" + number);
    }

    record NormalizedVat(String country, String number, String key) {}
}

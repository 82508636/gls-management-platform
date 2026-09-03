package pt.glsmanagement.platform.customer;

import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class VatRegistrationDetailsMapper {
    private static final Pattern PORTUGUESE_POSTAL_LINE =
            Pattern.compile("^(\\d{4})[-\\s]?(\\d{3})\\s+(.+)$");
    private static final Pattern PORTUGUESE_POSTAL_INLINE =
            Pattern.compile("^(.*?)[,\\s]+(\\d{4}-\\d{3})\\s+(.+)$");

    private VatRegistrationDetailsMapper() {}

    static Details map(
            String country, String name, String fullAddress, String street,
            String postalCode, String locality) {
        var cleanName = meaningful(name);
        var cleanFullAddress = meaningfulAddress(fullAddress);
        var cleanStreet = meaningful(street);
        var cleanPostalCode = meaningful(postalCode);
        var cleanLocality = meaningful(locality);
        if ((cleanStreet == null || cleanPostalCode == null || cleanLocality == null)
                && cleanFullAddress != null) {
            var parsed = parseAddress(country, cleanFullAddress);
            if (cleanStreet == null) cleanStreet = parsed.address();
            if (cleanPostalCode == null) cleanPostalCode = parsed.postalCode();
            if (cleanLocality == null) cleanLocality = parsed.locality();
        }
        return new Details(cleanName, cleanStreet != null ? cleanStreet : cleanFullAddress,
                cleanPostalCode, cleanLocality);
    }

    private static Details parseAddress(String country, String value) {
        var normalized = value.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (!"PT".equals(country)) {
            return new Details(null, normalized.lines().map(String::trim)
                    .filter(line -> !line.isBlank()).collect(Collectors.joining(", ")), null, null);
        }
        var lines = normalized.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
        for (int index = lines.size() - 1; index >= 0; index--) {
            var match = PORTUGUESE_POSTAL_LINE.matcher(lines.get(index));
            if (match.matches()) {
                return new Details(null, meaningful(String.join(", ", lines.subList(0, index))),
                        match.group(1) + "-" + match.group(2), meaningful(match.group(3)));
            }
        }
        var inline = PORTUGUESE_POSTAL_INLINE.matcher(normalized.replace('\n', ' '));
        if (inline.matches()) {
            return new Details(null, meaningful(inline.group(1)), inline.group(2), meaningful(inline.group(3)));
        }
        return new Details(null, normalized, null, null);
    }

    private static String meaningful(String value) {
        if (value == null) return null;
        var normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() || "---".equals(normalized) ? null : normalized;
    }

    private static String meaningfulAddress(String value) {
        if (value == null) return null;
        var normalized = value.replace("\r\n", "\n").replace('\r', '\n').lines()
                .map(String::trim).filter(line -> !line.isBlank()).collect(Collectors.joining("\n"));
        return normalized.isBlank() || "---".equals(normalized) ? null : normalized;
    }

    record Details(String name, String address, String postalCode, String locality) {
        static Details empty() { return new Details(null, null, null, null); }
    }
}

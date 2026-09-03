package pt.glsmanagement.platform.entity;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@Validated
public class RecipientRegistrationService {
    private final RecipientRepository repository;

    RecipientRegistrationService(RecipientRepository repository) { this.repository = repository; }

    @Transactional
    public RecipientResponse registerFromShipment(@Valid RecipientRegistration input) { return register(input); }

    @Transactional
    public RecipientResponse registerFromCustomer(@Valid RecipientRegistration input) {
        if (input.code() == null || input.code().isBlank()) {
            throw new IllegalArgumentException("Customer recipient code is required");
        }
        return register(input);
    }

    private RecipientResponse register(RecipientRegistration input) {
        var key = key(input);
        var recipient = repository.findByDeduplicationKey(key).orElseGet(() -> Recipient.create(key, input));
        recipient.reuse(input);
        return RecipientResponse.from(repository.save(recipient));
    }

    @Transactional(readOnly = true)
    RecipientPageResponse list(int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by("lastUsedAt").descending());
        return RecipientPageResponse.from(repository.findAll(pageable));
    }

    private static String key(RecipientRegistration input) {
        var normalized = input.code() == null || input.code().isBlank()
                ? String.join("|", "RECIPIENT", normalize(input.designation()), normalize(input.address()),
                        normalize(input.postalCode()), normalize(input.locality()), normalize(input.country()))
                : String.join("|", "CUSTOMER", normalize(input.code()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toUpperCase();
    }
}

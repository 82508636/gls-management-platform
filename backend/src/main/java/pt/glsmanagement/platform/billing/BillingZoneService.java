package pt.glsmanagement.platform.billing;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

import static pt.glsmanagement.platform.billing.BillingZoneDtos.*;

@Service
class BillingZoneService implements BillingZoneLookup {
    private final BillingZoneRepository repository;
    BillingZoneService(BillingZoneRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    List<Response> list(Boolean active) {
        var values = active == null ? repository.findAll(Sort.by("designation"))
                : repository.findAllByActiveOrderByDesignation(active);
        return values.stream().map(Response::from).toList();
    }

    @Transactional
    Response create(CreateRequest request, String actor) {
        var code = code(request.code());
        var designation = text(request.designation());
        if (repository.existsByCodeIgnoreCase(code) || repository.existsByDesignationIgnoreCase(designation))
            throw new BillingZoneException(BillingZoneException.Reason.DUPLICATE);
        var value = BillingZone.create(code, designation, request.zoneType(), country(request.country()),
                nullableText(request.groupName()), patterns(request.postalCodePatterns()), actor);
        return Response.from(repository.save(value));
    }

    @Transactional
    Response update(UUID id, UpdateRequest request, String actor) {
        var value = find(id);
        var code = code(request.code());
        var designation = text(request.designation());
        if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)
                || repository.existsByDesignationIgnoreCaseAndIdNot(designation, id))
            throw new BillingZoneException(BillingZoneException.Reason.DUPLICATE);
        value.update(code, designation, request.zoneType(), country(request.country()), nullableText(request.groupName()),
                patterns(request.postalCodePatterns()), actor);
        return Response.from(value);
    }

    @Transactional
    Response status(UUID id, boolean active, String actor) {
        var value = find(id);
        value.setActive(active, actor);
        return Response.from(value);
    }

    BillingZone requireActive(UUID id) {
        var zone = find(id);
        if (!zone.active()) throw new BillingZoneException(BillingZoneException.Reason.NOT_FOUND);
        return zone;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean allActive(Set<UUID> ids) {
        return ids.isEmpty() || repository.countByIdInAndActiveTrue(ids) == ids.size();
    }

    private BillingZone find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new BillingZoneException(BillingZoneException.Reason.NOT_FOUND));
    }
    private static String code(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private static String country(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private static String text(String value) { return value.trim().replaceAll("\\s+", " "); }
    private static String nullableText(String value) { return value == null || value.isBlank() ? null : text(value); }
    private static Set<String> patterns(Set<String> values) {
        var result = new TreeSet<String>();
        values.forEach(value -> result.add(value.trim().toUpperCase(Locale.ROOT)));
        return result;
    }
}

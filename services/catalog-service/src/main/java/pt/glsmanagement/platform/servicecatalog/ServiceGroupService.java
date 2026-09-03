package pt.glsmanagement.platform.servicecatalog;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

import static pt.glsmanagement.platform.servicecatalog.ServiceGroupDtos.*;

@Service
class ServiceGroupService {
    private final ServiceGroupRepository repository;

    ServiceGroupService(ServiceGroupRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    List<Response> list(Boolean active) {
        return repository.findAll(Sort.by("designation")).stream()
                .filter(value -> active == null || value.active() == active)
                .map(Response::from)
                .toList();
    }

    @Transactional
    Response create(CreateRequest request, String actor) {
        var id = code(request.id());
        var designation = text(request.designation());
        if (repository.existsById(id) || repository.existsByDesignationIgnoreCase(designation))
            throw new OperationalCatalogException(OperationalCatalogException.Reason.DUPLICATE);
        return Response.from(repository.save(ServiceGroup.create(id, designation, actor)));
    }

    @Transactional
    Response update(String id, UpdateRequest request, String actor) {
        var normalizedId = code(id);
        var value = find(normalizedId);
        var designation = text(request.designation());
        if (repository.existsByDesignationIgnoreCaseAndIdNot(designation, normalizedId))
            throw new OperationalCatalogException(OperationalCatalogException.Reason.DUPLICATE);
        value.update(designation, actor);
        return Response.from(value);
    }

    @Transactional
    Response status(String id, boolean active, String actor) {
        var value = find(code(id));
        value.setActive(active, actor);
        return Response.from(value);
    }

    ServiceGroup requireActive(String id) {
        var value = find(code(id));
        if (!value.active()) throw new OperationalCatalogException(OperationalCatalogException.Reason.INVALID_CONFIGURATION);
        return value;
    }

    private ServiceGroup find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new OperationalCatalogException(OperationalCatalogException.Reason.NOT_FOUND));
    }

    private static String code(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private static String text(String value) { return value.trim().replaceAll("\\s+", " "); }
}


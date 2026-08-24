package pt.glsmanagement.platform.entity;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
class PickupPointService {
    private final PickupPointRepository repository;
    PickupPointService(PickupPointRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    PickupPointPageResponse list(int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("designation").ascending());
        return PickupPointPageResponse.from(repository.findAll(pageable));
    }

    @Transactional
    PickupPointResponse create(PickupPointRequest request) {
        if (repository.existsByCodeIgnoreCase(request.code().trim())) throw new EntityOperationException("Duplicate pickup code");
        return PickupPointResponse.from(repository.save(PickupPoint.create(request)));
    }

    @Transactional
    PickupPointResponse update(UUID id, PickupPointRequest request) {
        var point = find(id);
        if (repository.existsByCodeIgnoreCaseAndIdNot(request.code().trim(), id)) throw new EntityOperationException("Duplicate pickup code");
        point.update(request);
        return PickupPointResponse.from(repository.save(point));
    }

    @Transactional
    PickupPointResponse updateStatus(UUID id, boolean active) { var point = find(id); point.setActive(active); return PickupPointResponse.from(repository.save(point)); }

    @Transactional
    void delete(UUID id) { if (!repository.existsById(id)) throw new EntityNotFoundException(); repository.deleteById(id); }

    private PickupPoint find(UUID id) { return repository.findById(id).orElseThrow(EntityNotFoundException::new); }
}

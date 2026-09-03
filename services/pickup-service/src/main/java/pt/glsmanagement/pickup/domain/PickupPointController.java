package pt.glsmanagement.pickup.domain;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/pickup-points")
class PickupPointController {
    private final PickupPointService service;

    PickupPointController(PickupPointService service) {
        this.service = service;
    }

    @GetMapping
    PickupPointPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.list(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PickupPointResponse create(@Valid @RequestBody PickupPointRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    PickupPointResponse update(@PathVariable UUID id, @Valid @RequestBody PickupPointRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    PickupPointResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody PickupPointStatusRequest request) {
        return service.updateStatus(id, request.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}

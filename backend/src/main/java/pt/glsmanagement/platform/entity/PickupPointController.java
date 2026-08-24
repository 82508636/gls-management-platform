package pt.glsmanagement.platform.entity;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/pickup-points")
class PickupPointController {
    private final PickupPointService service;
    PickupPointController(PickupPointService service) { this.service = service; }

    @GetMapping PickupPointPageResponse list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) { return service.list(page, size); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) PickupPointResponse create(@Valid @RequestBody PickupPointRequest request) { return service.create(request); }
    @PutMapping("/{id}") PickupPointResponse update(@PathVariable UUID id, @Valid @RequestBody PickupPointRequest request) { return service.update(id, request); }
    @PatchMapping("/{id}/status") PickupPointResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody PickupPointStatusRequest request) { return service.updateStatus(id, request.active()); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@PathVariable UUID id) { service.delete(id); }
}

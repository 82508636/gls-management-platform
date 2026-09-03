package pt.glsmanagement.platform.entity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipients")
class RecipientController {
    private final RecipientRegistrationService service;
    RecipientController(RecipientRegistrationService service) { this.service = service; }

    @GetMapping
    RecipientPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.list(page, size);
    }
}

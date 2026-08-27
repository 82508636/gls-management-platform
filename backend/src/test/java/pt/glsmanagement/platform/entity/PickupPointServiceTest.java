package pt.glsmanagement.platform.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickupPointServiceTest {
    @Mock PickupPointRepository repository;
    @InjectMocks PickupPointService service;

    @Test
    void createsPickupPointWithCompleteSchedule() {
        var request = request("LTFT-PU-001");
        when(repository.save(any(PickupPoint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(request);

        assertThat(result.code()).isEqualTo("LTFT-PU-001");
        assertThat(result.morningOpen()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.openSaturday()).isTrue();
    }

    @Test
    void rejectsDuplicatePickupCode() {
        var request = request("LTFT-PU-001");
        when(repository.existsByCodeIgnoreCase("LTFT-PU-001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request)).isInstanceOf(EntityOperationException.class);
    }

    @Test
    void limitsPickupPagesToFiftyItems() {
        var pageable = PageRequest.of(0, 50, Sort.by("designation").ascending());
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.list(0, 200);

        verify(repository).findAll(pageable);
    }

    @Test
    void rejectsOverlappingMorningAndAfternoonSchedules() {
        assertThatThrownBy(() -> new PickupPointRequest("LTFT-PU-002", "Ponto", LocalTime.of(9, 0),
                LocalTime.of(14, 30), LocalTime.of(14, 0), LocalTime.of(18, 0), "Rua", "4820-001", "Fafe",
                "PT", null, null, null, false, false, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsAdjacentMorningAndAfternoonSchedules() {
        var request = new PickupPointRequest("LTFT-PU-003", "Ponto", LocalTime.of(9, 0), LocalTime.of(13, 0),
                LocalTime.of(13, 0), LocalTime.of(18, 0), "Rua", "4820-001", "Fafe", "PT", null, null, null,
                false, false, true);

        assertThat(request.afternoonOpen()).isEqualTo(request.morningClose());
    }

    private static PickupPointRequest request(String code) {
        return new PickupPointRequest(code, "Ponto Fafe", LocalTime.of(9, 0), LocalTime.of(12, 30),
                LocalTime.of(14, 0), LocalTime.of(18, 0), "Rua Central 1", "4820-001", "Fafe", "PT",
                "pickup@example.test", "253000000", "910000000", true, false, true);
    }
}

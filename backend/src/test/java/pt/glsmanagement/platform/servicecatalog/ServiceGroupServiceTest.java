package pt.glsmanagement.platform.servicecatalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pt.glsmanagement.platform.servicecatalog.ServiceGroupDtos.*;

class ServiceGroupServiceTest {
    private ServiceGroupRepository repository;
    private ServiceGroupService service;

    @BeforeEach void setUp() {
        repository = mock(ServiceGroupRepository.class);
        service = new ServiceGroupService(repository);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void createsANormalizedAuditedGroup() {
        var response = service.create(new CreateRequest(" parcel ", " Encomendas   expresso "), "admin");
        assertThat(response.id()).isEqualTo("PARCEL");
        assertThat(response.designation()).isEqualTo("Encomendas expresso");
        assertThat(response.active()).isTrue();
        assertThat(response.createdBy()).isEqualTo("admin");
    }

    @Test void rejectsDuplicateIdAndDesignation() {
        when(repository.existsById("PARCEL")).thenReturn(true);
        assertDuplicate(new CreateRequest("parcel", "Parcel"));

        reset(repository);
        when(repository.existsByDesignationIgnoreCase("Parcel")).thenReturn(true);
        assertDuplicate(new CreateRequest("new", " Parcel "));
    }

    @Test void inactiveOrMissingGroupsCannotBeAssignedToAService() {
        var inactive = ServiceGroup.create("OLD", "Antigo", "admin");
        inactive.setActive(false, "admin");
        when(repository.findById("OLD")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.requireActive(" old "))
                .isInstanceOf(OperationalCatalogException.class)
                .extracting(error -> ((OperationalCatalogException) error).reason())
                .isEqualTo(OperationalCatalogException.Reason.INVALID_CONFIGURATION);
        assertThatThrownBy(() -> service.requireActive("missing"))
                .isInstanceOf(OperationalCatalogException.class)
                .extracting(error -> ((OperationalCatalogException) error).reason())
                .isEqualTo(OperationalCatalogException.Reason.NOT_FOUND);
    }

    private void assertDuplicate(CreateRequest request) {
        assertThatThrownBy(() -> service.create(request, "admin"))
                .isInstanceOf(OperationalCatalogException.class)
                .extracting(error -> ((OperationalCatalogException) error).reason())
                .isEqualTo(OperationalCatalogException.Reason.DUPLICATE);
        verify(repository, never()).save(any());
    }
}

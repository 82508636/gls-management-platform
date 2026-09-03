package pt.glsmanagement.workforce.reference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceCatalogServiceTest {
    @Mock AccountProfileRepository profiles;
    @Mock ProfessionalCategoryRepository categories;
    @InjectMocks ReferenceCatalogService service;

    @Test
    void normalizesProfileIdentityAndDesignation() {
        when(profiles.save(any(AccountProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createProfile(
                new ReferenceCatalogDtos.CreateRequest(" driver_extra ", "  Motorista   Auditor  "), "admin");

        assertThat(result.id()).isEqualTo("DRIVER_EXTRA");
        assertThat(result.designation()).isEqualTo("Motorista Auditor");
        assertThat(result.active()).isTrue();
        assertThat(result.createdBy()).isEqualTo("admin");
    }

    @Test
    void rejectsDuplicateDesignationCaseInsensitively() {
        when(categories.existsByDesignationIgnoreCase("Motoristas Pesados")).thenReturn(true);

        assertThatThrownBy(() -> service.createCategory(
                new ReferenceCatalogDtos.CreateRequest("7", "Motoristas Pesados"), "admin"))
                .isInstanceOf(ReferenceCatalogException.class);
    }

    @Test
    void preservesAuditAndUpdatesTheActorWhenStatusChanges() {
        var category = ProfessionalCategory.create("7", "Auditoria", "creator");
        when(categories.findById("7")).thenReturn(Optional.of(category));

        var result = service.setCategoryStatus("7", false, "reviewer");

        assertThat(result.active()).isFalse();
        assertThat(result.createdBy()).isEqualTo("creator");
        assertThat(result.updatedBy()).isEqualTo("reviewer");
    }
}

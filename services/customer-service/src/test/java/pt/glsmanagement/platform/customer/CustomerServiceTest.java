package pt.glsmanagement.platform.customer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import pt.glsmanagement.platform.entity.RecipientRegistration;
import pt.glsmanagement.platform.entity.RecipientRegistrationService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock CustomerRepository repository;
    @Mock CustomerCodeGenerator codeGenerator;
    @Mock RecipientRegistrationService recipientRegistrationService;
    @InjectMocks CustomerService service;

    @Test
    void rejectsDuplicateNormalizedVatBeforeAllocatingACode() {
        var request = request("Cliente", "LTFT01", "PT 501.234.567");
        when(repository.existsByVatKey("PT:501234567")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateCustomerVatNumberException.class);

        verify(codeGenerator, never()).next(any());
        verify(recipientRegistrationService, never()).registerFromCustomer(any());
    }

    @Test
    void assignsAgencyCodeCreatesInactiveCustomerAndSynchronizesRecipient() {
        var request = request("Cliente Taipas", "LTFT02", "PT999");
        when(codeGenerator.next("LTFT02")).thenReturn("200001");
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(request);

        assertThat(result.customerCode()).isEqualTo("200001");
        assertThat(result.active()).isFalse();
        var recipient = org.mockito.ArgumentCaptor.forClass(RecipientRegistration.class);
        verify(recipientRegistrationService).registerFromCustomer(recipient.capture());
        assertThat(recipient.getValue().code()).isEqualTo("200001");
        assertThat(recipient.getValue().designation()).isEqualTo("Cliente Taipas");
        assertThat(recipient.getValue().address()).isEqualTo("Rua do Cliente 1");
    }

    @Test
    void updatesRecipientWithoutChangingApprovalStatus() {
        var customerId = UUID.randomUUID();
        var original = Customer.create(request("Cliente inicial", "LTFT01", "501234567"), "100001",
                VatNumberNormalizer.normalize("PT", "501234567"));
        original.setActive(true);
        when(repository.findById(customerId)).thenReturn(Optional.of(original));

        var result = service.update(customerId, request("Cliente atualizado", "LTFT01", "PT 501-234-567"));

        assertThat(result.active()).isTrue();
        var recipient = org.mockito.ArgumentCaptor.forClass(RecipientRegistration.class);
        verify(recipientRegistrationService).registerFromCustomer(recipient.capture());
        assertThat(recipient.getValue().code()).isEqualTo("100001");
        assertThat(recipient.getValue().designation()).isEqualTo("Cliente atualizado");
    }

    @Test
    void rejectsAgencyChanges() {
        var customerId = UUID.randomUUID();
        var original = Customer.create(request("Cliente", "LTFT01", "501234567"), "100001",
                VatNumberNormalizer.normalize("PT", "501234567"));
        when(repository.findById(customerId)).thenReturn(Optional.of(original));

        assertThatThrownBy(() -> service.update(customerId, request("Cliente", "LTFT02", "501234567")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clampsPagesAndNormalizesSearch() {
        Pageable expectedPage = PageRequest.of(2, 50,
                org.springframework.data.domain.Sort.by("shippingName").ascending());
        when(repository.search("norte digital", true, expectedPage))
                .thenReturn(new PageImpl<>(java.util.List.of(), expectedPage, 0));

        service.list(2, 500, "  Norte Digital  ", true);

        verify(repository).search("norte digital", true, expectedPage);
    }

    private static CustomerRequest request(String shippingName, String agency, String vatNumber) {
        return new CustomerRequest(
                null, null, shippingName, agency, "Rua do Cliente 1", "4800-001", "Guimarães", "PT",
                null, null, null, "PT", vatNumber, null, null, null, null, null, null,
                CustomerRequest.CustomerType.COMPANY, null, null, "INVOICE", null, "EUR",
                false, true, true);
    }
}

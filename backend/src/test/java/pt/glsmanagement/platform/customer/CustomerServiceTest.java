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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerCodeGenerator codeGenerator;

    @Mock
    private RecipientRegistrationService recipientRegistrationService;

    @InjectMocks
    private CustomerService service;

    @Test
    void rejectsDuplicateVatNumber() {
        var request = request("Cliente", "LTFT01", "PT 123");
        when(repository.existsByVatKey("PT:123")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateCustomerVatNumberException.class)
                .hasMessageContaining("123");
        verify(recipientRegistrationService, never()).registerFromCustomer(any());
    }

    @Test
    void assignsTheNextAgencyCodeOnlyWhenCreatingTheCustomer() {
        var request = request("Cliente Taipas", "LTFT02", "PT999");
        when(codeGenerator.next("LTFT02")).thenReturn("200001");
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(request);

        assertThat(result.customerCode()).isEqualTo("200001");
        verify(codeGenerator).next("LTFT02");
        var recipient = org.mockito.ArgumentCaptor.forClass(RecipientRegistration.class);
        verify(recipientRegistrationService).registerFromCustomer(recipient.capture());
        assertThat(recipient.getValue().code()).isEqualTo("200001");
        assertThat(recipient.getValue().designation()).isEqualTo("Cliente Taipas");
        assertThat(recipient.getValue().address()).isEqualTo("Rua do Cliente 1");
        assertThat(recipient.getValue().postalCode()).isEqualTo("4800-001");
        assertThat(recipient.getValue().locality()).isEqualTo("Guimarães");
        assertThat(recipient.getValue().country()).isEqualTo("PT");
    }

    @Test
    void createsCustomerInactiveUntilAnAdministratorApprovesIt() {
        var requestAskingForActiveCustomer = request("Cliente pendente", "LTFT01", "PT998");
        when(codeGenerator.next("LTFT01")).thenReturn("100001");
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(requestAskingForActiveCustomer);

        assertThat(result.active()).isFalse();
    }

    @Test
    void limitsCustomerPagesToFiftyItems() {
        Pageable expectedPage = PageRequest.of(0, 50, org.springframework.data.domain.Sort.by("shippingName").ascending());
        when(repository.search(null, null, expectedPage)).thenReturn(new PageImpl<>(java.util.List.of(), expectedPage, 0));

        var result = service.list(0, 200, "  ", null);

        assertThat(result.content()).isEmpty();
        verify(repository).search(null, null, expectedPage);
    }

    @Test
    void normalizesVatNumberBeforePersisting() {
        var request = request("Cliente", "LTFT01", "pt 501.234.567");
        when(codeGenerator.next("LTFT01")).thenReturn("100001");
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(request);

        assertThat(result.vatNumber()).isEqualTo("501234567");
        verify(repository).existsByVatKey("PT:501234567");
    }

    @Test
    void updatesTheRecipientWhenCustomerContactDataChanges() {
        var customerId = UUID.randomUUID();
        var original = Customer.create(request("Cliente inicial", "LTFT01", "501234567"), "100001",
                VatNumberNormalizer.normalize("PT", "501234567"));
        when(repository.findById(customerId)).thenReturn(Optional.of(original));

        service.update(customerId, request("Cliente atualizado", "LTFT01", "PT 501-234-567"));

        var recipient = org.mockito.ArgumentCaptor.forClass(RecipientRegistration.class);
        verify(recipientRegistrationService).registerFromCustomer(recipient.capture());
        assertThat(recipient.getValue().code()).isEqualTo("100001");
        assertThat(recipient.getValue().designation()).isEqualTo("Cliente atualizado");
        verify(repository).existsByVatKeyAndIdNot("PT:501234567", customerId);
    }

    @Test
    void normalizesSearchAndPassesTheActiveFilterToTheRepository() {
        Pageable expectedPage = PageRequest.of(2, 20, org.springframework.data.domain.Sort.by("shippingName").ascending());
        when(repository.search("norte digital", true, expectedPage))
                .thenReturn(new PageImpl<>(java.util.List.of(), expectedPage, 0));

        service.list(2, 20, "  Norte Digital  ", true);

        verify(repository).search("norte digital", true, expectedPage);
    }

    private static CustomerRequest request(String shippingName, String agency, String vatNumber) {
        return new CustomerRequest(
                null, null, shippingName, agency, "Rua do Cliente 1", "4800-001", "Guimarães", "PT", null, null, null,
                "PT", vatNumber, null, null, null, null, null, null,
                CustomerRequest.CustomerType.COMPANY, null, null, "INVOICE", null, "EUR",
                false, true, true
        );
    }
}

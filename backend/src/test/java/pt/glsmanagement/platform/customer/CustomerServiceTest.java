package pt.glsmanagement.platform.customer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock
    private CustomerRepository repository;

    @Mock
    private CustomerCodeGenerator codeGenerator;

    @InjectMocks
    private CustomerService service;

    @Test
    void rejectsDuplicateVatNumber() {
        var request = request("Cliente", "LTFT01", "PT123");
        when(repository.existsByVatNumber("PT123")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateCustomerVatNumberException.class)
                .hasMessageContaining("PT123");
    }

    @Test
    void assignsTheNextAgencyCodeOnlyWhenCreatingTheCustomer() {
        var request = request("Cliente Taipas", "LTFT02", "PT999");
        when(codeGenerator.next("LTFT02")).thenReturn("200001");
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(request);

        assertThat(result.customerCode()).isEqualTo("200001");
        verify(codeGenerator).next("LTFT02");
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
        when(repository.findAll(expectedPage)).thenReturn(new PageImpl<>(java.util.List.of(), expectedPage, 0));

        var result = service.list(0, 200);

        assertThat(result.content()).isEmpty();
        verify(repository).findAll(expectedPage);
    }

    private static CustomerRequest request(String shippingName, String agency, String vatNumber) {
        return new CustomerRequest(
                null, null, shippingName, agency, null, null, null, "PT", null, null, null,
                "PT", vatNumber, null, null, null, null, null, null,
                CustomerRequest.CustomerType.COMPANY, null, null, "INVOICE", null, "EUR",
                false, true, true
        );
    }
}

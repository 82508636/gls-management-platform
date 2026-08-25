package pt.glsmanagement.platform.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipientRegistrationServiceTest {
    @Mock RecipientRepository repository;
    @InjectMocks RecipientRegistrationService service;

    @Test
    void storesANewRecipientFromShipmentData() {
        var input = input();
        when(repository.findByDeduplicationKey(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(Recipient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.registerFromShipment(input);

        assertThat(result.code()).isNull();
        assertThat(result.designation()).isEqualTo("Loja Destino");
        assertThat(result.country()).isEqualTo("PT");
        verify(repository).save(any(Recipient.class));
    }

    @Test
    void storesTheCustomerCodeWhenRegisteringFromCustomerData() {
        var input = new RecipientRegistration("100123", "Cliente Fafe", null, "Rua Central 1", "4820-001", "Fafe",
                "PT", "cliente@example.test", "253000001", "910000001");
        when(repository.findByDeduplicationKey(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(Recipient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.registerFromCustomer(input);

        assertThat(result.code()).isEqualTo("100123");
        assertThat(result.designation()).isEqualTo("Cliente Fafe");
    }

    @Test
    void keepsCustomersWithTheSameAddressAsSeparateRecipients() {
        var first = new RecipientRegistration("100123", "Cliente Fafe", null, "Rua Central 1", "4820-001", "Fafe",
                "PT", null, null, null);
        var second = new RecipientRegistration("100124", "Cliente Fafe", null, "Rua Central 1", "4820-001", "Fafe",
                "PT", null, null, null);
        when(repository.findByDeduplicationKey(anyString())).thenReturn(Optional.empty());
        when(repository.save(any(Recipient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.registerFromCustomer(first);
        service.registerFromCustomer(second);

        var keys = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository, org.mockito.Mockito.times(2)).findByDeduplicationKey(keys.capture());
        assertThat(keys.getAllValues()).doesNotHaveDuplicates();
    }

    private static RecipientRegistration input() {
        return new RecipientRegistration(null, "Loja Destino", "Ana Silva", "Rua do Mercado 7", "4700-001", "Braga", "pt",
                "destino@example.test", null, "910000001");
    }
}

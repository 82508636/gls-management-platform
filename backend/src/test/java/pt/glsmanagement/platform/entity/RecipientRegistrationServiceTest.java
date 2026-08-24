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

        assertThat(result.name()).isEqualTo("Loja Destino");
        assertThat(result.country()).isEqualTo("PT");
        verify(repository).save(any(Recipient.class));
    }

    private static RecipientRegistration input() {
        return new RecipientRegistration("Loja Destino", "Ana Silva", "Rua do Mercado 7", "4700-001", "Braga", "pt",
                "destino@example.test", null, "910000001");
    }
}

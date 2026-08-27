package pt.glsmanagement.platform.identity;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IdentityAccessControlTest {
    private final IdentityAccessStateRepository repository = mock(IdentityAccessStateRepository.class);
    private final IdentityAccessControl control = new IdentityAccessControl(repository);

    @Test void allowsPreExistingUsersWithoutManagedState() {
        when(repository.findById("legacy-user")).thenReturn(Optional.empty());
        assertThat(control.isTokenAllowed("legacy-user", Set.of(PlatformRole.OPERATOR))).isTrue();
    }

    @Test void moveImmediatelyRejectsThePreviousRole() {
        var state = IdentityAccessState.active("user-1", PlatformRole.ACCOUNTING);
        when(repository.findById("user-1")).thenReturn(Optional.of(state));
        assertThat(control.isTokenAllowed("user-1", Set.of(PlatformRole.OPERATOR))).isFalse();
        assertThat(control.isTokenAllowed("user-1", Set.of(PlatformRole.ACCOUNTING))).isTrue();
    }

    @Test void leaverImmediatelyRejectsEveryToken() {
        var state = IdentityAccessState.active("user-1", PlatformRole.OPERATOR);
        state.disable();
        when(repository.findById("user-1")).thenReturn(Optional.of(state));
        assertThat(control.isTokenAllowed("user-1", Set.of(PlatformRole.OPERATOR))).isFalse();
    }

    @Test void rejectsAmbiguousPlatformRolesAndTokensWithoutSubject() {
        var state = IdentityAccessState.active("user-1", PlatformRole.OPERATOR);
        when(repository.findById("user-1")).thenReturn(Optional.of(state));
        assertThat(control.isTokenAllowed("user-1", Set.of(PlatformRole.OPERATOR, PlatformRole.ADMIN))).isFalse();
        assertThat(control.isTokenAllowed("", Set.of(PlatformRole.OPERATOR))).isFalse();
    }
}

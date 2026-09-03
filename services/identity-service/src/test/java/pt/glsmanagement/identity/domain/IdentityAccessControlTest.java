package pt.glsmanagement.identity.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityAccessControlTest {
    private final IdentityAccessStateRepository repository = mock(IdentityAccessStateRepository.class);
    private final IdentityAccessControl accessControl = new IdentityAccessControl(repository);

    @Test
    void rejectsMissingSubject() {
        assertThat(accessControl.isTokenAllowed(null, Set.of(PlatformRole.ADMIN))).isFalse();
        assertThat(accessControl.isTokenAllowed(" ", Set.of(PlatformRole.ADMIN))).isFalse();
    }

    @Test
    void temporarilyAllowsUsersThatHaveNotYetBeenManagedByJml() {
        when(repository.findById("legacy-user")).thenReturn(Optional.empty());

        assertThat(accessControl.isTokenAllowed("legacy-user", Set.of(PlatformRole.OPERATOR))).isTrue();
    }

    @Test
    void immediatelyRejectsAStoredDisabledUser() {
        var state = IdentityAccessState.active("leaver", PlatformRole.OPERATOR);
        state.disable();
        when(repository.findById("leaver")).thenReturn(Optional.of(state));

        assertThat(accessControl.isTokenAllowed("leaver", Set.of(PlatformRole.OPERATOR))).isFalse();
    }

    @Test
    void rejectsOldRoleAndMultiplePlatformRolesAfterAMove() {
        var state = IdentityAccessState.active("mover", PlatformRole.ACCOUNTING);
        when(repository.findById("mover")).thenReturn(Optional.of(state));

        assertThat(accessControl.isTokenAllowed("mover", Set.of(PlatformRole.OPERATOR))).isFalse();
        assertThat(accessControl.isTokenAllowed(
                "mover", Set.of(PlatformRole.OPERATOR, PlatformRole.ACCOUNTING))).isFalse();
        assertThat(accessControl.isTokenAllowed("mover", Set.of(PlatformRole.ACCOUNTING))).isTrue();
    }
}

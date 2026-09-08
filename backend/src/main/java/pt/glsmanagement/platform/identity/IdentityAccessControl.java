package pt.glsmanagement.platform.identity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.Set;

@Service
public class IdentityAccessControl {
    private final IdentityAccessStateRepository repository;

    IdentityAccessControl(IdentityAccessStateRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void activate(String userId, PlatformRole role) {
        var state = repository.findById(userId).orElseGet(() -> IdentityAccessState.active(userId, role));
        state.activate(role);
        repository.save(state);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void disable(String userId) {
        var state = repository.findById(userId).orElseGet(() -> IdentityAccessState.active(userId, PlatformRole.CUSTOMER));
        state.disable();
        repository.save(state);
    }

    @Transactional(readOnly = true)
    public boolean isTokenAllowed(String userId, Set<PlatformRole> tokenRoles) {
        if (userId == null || userId.isBlank()) return false;
        return repository.findById(userId).map(state -> state.allows(tokenRoles)).orElse(true);
    }
}

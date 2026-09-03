package pt.glsmanagement.platform.customer;

import java.util.Set;
import java.util.UUID;

public interface CustomerReferenceLookup {
    boolean allExist(Set<UUID> ids);
}

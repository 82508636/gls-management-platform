package pt.glsmanagement.platform.customer;

import java.util.*;

public interface CustomerReferenceLookup {
    boolean allExist(Set<UUID> ids);
}

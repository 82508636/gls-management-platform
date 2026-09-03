package pt.glsmanagement.platform.billing;

import java.util.*;

public interface BillingZoneLookup {
    boolean allActive(Set<UUID> ids);
}

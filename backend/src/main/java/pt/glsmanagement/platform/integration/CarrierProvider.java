package pt.glsmanagement.platform.integration;

import java.util.List;
import java.util.Optional;

public interface CarrierProvider {
    List<CarrierShipment> getShipments();

    Optional<CarrierShipment> getShipment(String externalId);
}


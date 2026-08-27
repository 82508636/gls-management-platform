package pt.glsmanagement.platform.shipment;

import org.springframework.data.domain.Page;
import java.util.List;

public record ShipmentPageResponse(
        List<ShipmentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    static ShipmentPageResponse from(Page<Shipment> result) {
        return new ShipmentPageResponse(result.getContent().stream().map(ShipmentResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast());
    }
}

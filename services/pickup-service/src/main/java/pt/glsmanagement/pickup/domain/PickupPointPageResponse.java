package pt.glsmanagement.pickup.domain;

import org.springframework.data.domain.Page;

import java.util.List;

public record PickupPointPageResponse(
        List<PickupPointResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    static PickupPointPageResponse from(Page<PickupPoint> result) {
        return new PickupPointPageResponse(
                result.getContent().stream().map(PickupPointResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast());
    }
}

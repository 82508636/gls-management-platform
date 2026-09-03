package pt.glsmanagement.platform.entity;

import org.springframework.data.domain.Page;

import java.util.List;

public record RecipientPageResponse(
        List<RecipientResponse> content, int page, int size, long totalElements,
        int totalPages, boolean first, boolean last
) {
    static RecipientPageResponse from(Page<Recipient> result) {
        return new RecipientPageResponse(
                result.getContent().stream().map(RecipientResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast());
    }
}

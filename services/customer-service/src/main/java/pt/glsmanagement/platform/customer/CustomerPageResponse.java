package pt.glsmanagement.platform.customer;

import org.springframework.data.domain.Page;

import java.util.List;

public record CustomerPageResponse(
        List<CustomerResponse> content, int page, int size, long totalElements,
        int totalPages, boolean first, boolean last
) {
    static CustomerPageResponse from(Page<Customer> customers) {
        return new CustomerPageResponse(
                customers.getContent().stream().map(CustomerResponse::from).toList(),
                customers.getNumber(), customers.getSize(), customers.getTotalElements(),
                customers.getTotalPages(), customers.isFirst(), customers.isLast());
    }
}

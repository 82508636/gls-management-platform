package pt.glsmanagement.platform.customer;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.glsmanagement.platform.entity.RecipientRegistration;
import pt.glsmanagement.platform.entity.RecipientRegistrationService;

import java.util.*;

@Service
public class CustomerService implements CustomerReferenceLookup {
    private final CustomerRepository repository;
    private final CustomerCodeGenerator codeGenerator;
    private final RecipientRegistrationService recipientRegistrationService;

    CustomerService(CustomerRepository repository, CustomerCodeGenerator codeGenerator,
                    RecipientRegistrationService recipientRegistrationService) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.recipientRegistrationService = recipientRegistrationService;
    }

    @Transactional(readOnly = true)
    public CustomerPageResponse list(int page, int requestedSize, String query, Boolean active) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(requestedSize, 1), 50);
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "shippingName"));
        var normalizedQuery = query == null || query.isBlank() ? null : query.trim().toLowerCase(java.util.Locale.ROOT);
        var result = normalizedQuery == null
                ? (active == null ? repository.findAll(pageable) : repository.findAllByActive(active, pageable))
                : repository.search(normalizedQuery, active, pageable);
        return CustomerPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        return CustomerResponse.from(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean allExist(Set<UUID> ids) {
        return ids.isEmpty() || repository.countByIdIn(ids) == ids.size();
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        var vat = VatNumberNormalizer.normalize(vatCountry(request), request.vatNumber());
        if (repository.existsByVatKey(vat.key())) {
            throw new DuplicateCustomerVatNumberException(vat.number());
        }
        var customerCode = codeGenerator.next(request.agency());
        var customer = repository.save(Customer.create(request, customerCode, vat));
        synchronizeRecipient(customer);
        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest request) {
        var customer = find(id);
        if (!customer.agency().equals(request.agency())) throw new IllegalArgumentException("Customer agency is immutable");
        var vat = VatNumberNormalizer.normalize(vatCountry(request), request.vatNumber());
        if (repository.existsByVatKeyAndIdNot(vat.key(), id)) {
            throw new DuplicateCustomerVatNumberException(vat.number());
        }
        customer.updateDetails(request, vat);
        synchronizeRecipient(customer);
        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse updateStatus(UUID id, boolean active) {
        var customer = find(id);
        customer.setActive(active);
        return CustomerResponse.from(customer);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(find(id));
    }

    private Customer find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id));
    }

    private void synchronizeRecipient(Customer customer) {
        recipientRegistrationService.registerFromCustomer(new RecipientRegistration(
                customer.customerCode(), customer.shippingName(), null, customer.address(), customer.postalCode(),
                customer.locality(), customer.country(), customer.contactEmail(), customer.phone(), customer.mobile()
        ));
    }

    private static String vatCountry(CustomerRequest request) {
        return request.billingCountry() == null || request.billingCountry().isBlank()
                ? request.country() : request.billingCountry();
    }
}

package pt.glsmanagement.platform.customer;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.glsmanagement.platform.entity.RecipientRegistration;
import pt.glsmanagement.platform.entity.RecipientRegistrationService;

import java.util.UUID;

@Service
public class CustomerService {
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
    public CustomerPageResponse list(int page, int requestedSize) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(requestedSize, 1), 50);
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "shippingName"));
        return CustomerPageResponse.from(repository.findAll(pageable));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(UUID id) {
        return CustomerResponse.from(find(id));
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        var vatNumber = request.vatNumber().trim();
        if (repository.existsByVatNumber(vatNumber)) {
            throw new DuplicateCustomerVatNumberException(vatNumber);
        }
        var customerCode = codeGenerator.next(request.agency());
        var customer = repository.save(Customer.create(request, customerCode));
        recipientRegistrationService.registerFromCustomer(new RecipientRegistration(
                customer.customerCode(), customer.shippingName(), null, customer.address(), customer.postalCode(),
                customer.locality(), customer.country(), customer.contactEmail(), customer.phone(), customer.mobile()
        ));
        return CustomerResponse.from(customer);
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerRequest request) {
        var customer = find(id);
        if (!customer.agency().equals(request.agency())) throw new IllegalArgumentException("Customer agency is immutable");
        var vatNumber = request.vatNumber().trim();
        if (repository.existsByVatNumberAndIdNot(vatNumber, id)) {
            throw new DuplicateCustomerVatNumberException(vatNumber);
        }
        customer.updateDetails(request);
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
}

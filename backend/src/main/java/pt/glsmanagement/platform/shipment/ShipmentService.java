package pt.glsmanagement.platform.shipment;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.glsmanagement.platform.entity.RecipientRegistrationService;
import pt.glsmanagement.platform.pricing.PricingQuoteRequest;
import pt.glsmanagement.platform.pricing.PricingQuoteService;

import java.util.List;
import java.util.UUID;

@Service
class ShipmentService {
    private final ShipmentRepository shipments;
    private final CustomerShipmentLookup customers;
    private final RecipientRegistrationService recipients;
    private final PricingQuoteService pricing;

    ShipmentService(ShipmentRepository shipments, CustomerShipmentLookup customers,
                    RecipientRegistrationService recipients, PricingQuoteService pricing) {
        this.shipments = shipments;
        this.customers = customers;
        this.recipients = recipients;
        this.pricing = pricing;
    }

    @Transactional
    ShipmentResponse create(ShipmentRequest request, String actor) {
        validateDates(request);
        var customer = customers.customer(request.customerId());
        var quote = pricing.quote(new PricingQuoteRequest(request.pricingPlanId(), request.routeCode(),
                request.actualWeightKg(), request.parcelCount(), request.lengthCm(), request.widthCm(),
                request.heightCm()));
        if (!request.recipient().country().equalsIgnoreCase(quote.destinationCountry())) {
            throw ShipmentException.invalidData();
        }
        var recipient = recipients.registerFromShipment(request.recipient());
        var shipmentNumber = customers.nextShipmentNumber(request.shipmentDate());
        var normalizedActor = actor == null || actor.isBlank() ? "Sistema" : actor.trim();
        return ShipmentResponse.from(shipments.save(Shipment.create(
                shipmentNumber, customer, recipient, request, quote, normalizedActor)));
    }

    @Transactional(readOnly = true)
    ShipmentPageResponse list(int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Order.desc("shipmentDate"), Sort.Order.desc("createdAt")));
        return ShipmentPageResponse.from(shipments.findAll(pageable));
    }

    @Transactional(readOnly = true)
    List<CustomerAccountServiceResponse> customerServices(UUID customerId) {
        customers.customer(customerId);
        return shipments.findByCustomerIdOrderByShipmentDateDescCreatedAtDesc(customerId).stream()
                .map(CustomerAccountServiceResponse::from)
                .toList();
    }

    private static void validateDates(ShipmentRequest request) {
        if (request.dueDate() != null && request.dueDate().isBefore(request.shipmentDate())) {
            throw ShipmentException.invalidData();
        }
    }
}

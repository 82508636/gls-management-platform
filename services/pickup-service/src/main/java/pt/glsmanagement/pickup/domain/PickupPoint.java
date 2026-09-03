package pt.glsmanagement.pickup.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "pickup_points")
class PickupPoint {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 30) private String code;
    @Column(nullable = false, length = 200) private String designation;
    @Column(name = "morning_open") private LocalTime morningOpen;
    @Column(name = "morning_close") private LocalTime morningClose;
    @Column(name = "afternoon_open") private LocalTime afternoonOpen;
    @Column(name = "afternoon_close") private LocalTime afternoonClose;
    @Column(nullable = false, length = 500) private String address;
    @Column(name = "postal_code", nullable = false, length = 20) private String postalCode;
    @Column(nullable = false, length = 120) private String locality;
    @Column(nullable = false, length = 2) private String country;
    @Column(length = 254) private String email;
    @Column(length = 50) private String phone;
    @Column(length = 50) private String mobile;
    @Column(name = "open_saturday", nullable = false) private boolean openSaturday;
    @Column(name = "open_sunday", nullable = false) private boolean openSunday;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    protected PickupPoint() {}

    static PickupPoint create(PickupPointRequest request) {
        var point = new PickupPoint();
        point.id = UUID.randomUUID();
        point.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        point.apply(request);
        return point;
    }

    void update(PickupPointRequest request) {
        apply(request);
    }

    void setActive(boolean value) {
        active = value;
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private void apply(PickupPointRequest request) {
        code = request.code().trim().toUpperCase();
        designation = request.designation().trim();
        morningOpen = request.morningOpen();
        morningClose = request.morningClose();
        afternoonOpen = request.afternoonOpen();
        afternoonClose = request.afternoonClose();
        address = request.address().trim();
        postalCode = request.postalCode().trim();
        locality = request.locality().trim();
        country = request.country().trim().toUpperCase();
        email = optional(request.email());
        phone = optional(request.phone());
        mobile = optional(request.mobile());
        openSaturday = request.openSaturday();
        openSunday = request.openSunday();
        active = request.active();
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    UUID id() { return id; }
    String code() { return code; }
    String designation() { return designation; }
    LocalTime morningOpen() { return morningOpen; }
    LocalTime morningClose() { return morningClose; }
    LocalTime afternoonOpen() { return afternoonOpen; }
    LocalTime afternoonClose() { return afternoonClose; }
    String address() { return address; }
    String postalCode() { return postalCode; }
    String locality() { return locality; }
    String country() { return country; }
    String email() { return email; }
    String phone() { return phone; }
    String mobile() { return mobile; }
    boolean openSaturday() { return openSaturday; }
    boolean openSunday() { return openSunday; }
    boolean active() { return active; }
    OffsetDateTime createdAt() { return createdAt; }
    OffsetDateTime updatedAt() { return updatedAt; }
}

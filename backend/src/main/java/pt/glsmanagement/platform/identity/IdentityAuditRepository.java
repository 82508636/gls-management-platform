package pt.glsmanagement.platform.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

interface IdentityAuditRepository extends JpaRepository<IdentityAuditEvent, UUID> {}

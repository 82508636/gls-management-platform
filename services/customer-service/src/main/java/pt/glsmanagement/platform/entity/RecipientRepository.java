package pt.glsmanagement.platform.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface RecipientRepository extends JpaRepository<Recipient, UUID> {
    Optional<Recipient> findByDeduplicationKey(String deduplicationKey);
}

package pt.glsmanagement.platform.identity;

import org.springframework.data.jpa.repository.JpaRepository;

interface IdentityAccessStateRepository extends JpaRepository<IdentityAccessState, String> {}

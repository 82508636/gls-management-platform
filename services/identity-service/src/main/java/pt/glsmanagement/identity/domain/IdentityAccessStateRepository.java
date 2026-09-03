package pt.glsmanagement.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;

interface IdentityAccessStateRepository extends JpaRepository<IdentityAccessState, String> {}

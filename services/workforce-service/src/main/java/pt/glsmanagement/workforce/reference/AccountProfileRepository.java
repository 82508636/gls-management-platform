package pt.glsmanagement.workforce.reference;

import org.springframework.data.jpa.repository.JpaRepository;

interface AccountProfileRepository extends JpaRepository<AccountProfile, String> {
    boolean existsByDesignationIgnoreCase(String designation);
    boolean existsByDesignationIgnoreCaseAndIdNot(String designation, String id);
}

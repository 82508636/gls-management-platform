package pt.glsmanagement.workforce.reference;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProfessionalCategoryRepository extends JpaRepository<ProfessionalCategory, String> {
    boolean existsByDesignationIgnoreCase(String designation);
    boolean existsByDesignationIgnoreCaseAndIdNot(String designation, String id);
}

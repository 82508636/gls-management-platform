package pt.glsmanagement.platform.collaborator;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProfessionalCategoryRepository extends JpaRepository<ProfessionalCategory, String> {
    boolean existsByDesignationIgnoreCase(String designation);
    boolean existsByDesignationIgnoreCaseAndIdNot(String designation, String id);
}

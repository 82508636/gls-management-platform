package pt.glsmanagement.platform.servicecatalog;

import org.springframework.data.jpa.repository.JpaRepository;

interface ServiceGroupRepository extends JpaRepository<ServiceGroup, String> {
    boolean existsByDesignationIgnoreCase(String designation);
    boolean existsByDesignationIgnoreCaseAndIdNot(String designation, String id);
}

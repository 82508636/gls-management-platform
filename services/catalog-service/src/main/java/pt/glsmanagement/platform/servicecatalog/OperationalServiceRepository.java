package pt.glsmanagement.platform.servicecatalog;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

interface OperationalServiceRepository extends JpaRepository<OperationalService, UUID> {
    List<OperationalService> findAllByOrderByDesignation();
    List<OperationalService> findAllByActiveOrderByDesignation(boolean active);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}


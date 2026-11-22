package com.universal.erp.platform.organization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    boolean existsByCompanyIdAndCodeIgnoreCase(UUID companyId, String code);

    List<Department> findByCompanyIdOrderByNameAsc(UUID companyId);

    List<Department> findByParentDepartmentId(UUID parentDepartmentId);
}

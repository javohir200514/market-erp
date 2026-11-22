package com.universal.erp.platform.organization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    List<Branch> findByCompanyIdOrderByNameAsc(UUID companyId);

    List<Branch> findByActiveTrueOrderByNameAsc();
}

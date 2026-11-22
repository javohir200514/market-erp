package com.universal.erp.platform.organization;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

/** Application service for company departments and their optional hierarchy. */
@Service
@Validated
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                             CompanyRepository companyRepository,
                             BranchRepository branchRepository) {
        this.departmentRepository = departmentRepository;
        this.companyRepository = companyRepository;
        this.branchRepository = branchRepository;
    }

    @PreAuthorize("hasAuthority('platform.department:create')")
    @Transactional
    public OrganizationDto.DepartmentResponse create(@Valid OrganizationDto.DepartmentCreate request) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new OrganizationNotFoundException("Company not found"));
        if (!company.isActive()) {
            throw new OrganizationConflictException("Cannot add a department to an inactive company");
        }

        String code = request.code().trim().toUpperCase(java.util.Locale.ROOT);
        if (departmentRepository.existsByCompanyIdAndCodeIgnoreCase(company.getId(), code)) {
            throw new OrganizationConflictException("Department code is already in use in this company");
        }

        Department department = new Department();
        department.setCompany(company);
        department.setCode(code);
        department.setName(request.name().trim());

        if (request.branchId() != null) {
            Branch branch = branchRepository.findById(request.branchId())
                    .orElseThrow(() -> new OrganizationNotFoundException("Branch not found"));
            if (!branch.getCompany().getId().equals(company.getId())) {
                throw new OrganizationConflictException("Branch must belong to the selected company");
            }
            if (!branch.isActive()) {
                throw new OrganizationConflictException("Cannot assign department to an inactive branch");
            }
            department.setBranch(branch);
        }

        if (request.parentDepartmentId() != null) {
            Department parent = getEntity(request.parentDepartmentId());
            if (!parent.getCompany().getId().equals(company.getId())) {
                throw new OrganizationConflictException("Parent department must belong to the same company");
            }
            department.setParentDepartment(parent);
        }

        return OrganizationDto.DepartmentResponse.from(departmentRepository.save(department));
    }

    @PreAuthorize("hasAuthority('platform.department:read')")
    @Transactional(readOnly = true)
    public OrganizationDto.DepartmentResponse getById(UUID id) {
        return OrganizationDto.DepartmentResponse.from(getEntity(id));
    }

    @PreAuthorize("hasAuthority('platform.department:read')")
    @Transactional(readOnly = true)
    public List<OrganizationDto.DepartmentResponse> getByCompany(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new OrganizationNotFoundException("Company not found");
        }
        return departmentRepository.findByCompanyIdOrderByNameAsc(companyId).stream()
                .map(OrganizationDto.DepartmentResponse::from).toList();
    }

    @PreAuthorize("hasAuthority('platform.department:update')")
    @Transactional
    public OrganizationDto.DepartmentResponse update(UUID id, @Valid OrganizationDto.DepartmentUpdate request) {
        Department department = getEntity(id);
        checkVersion(department.getVersion(), request.version());

        Branch branch = null;
        if (request.branchId() != null) {
            branch = branchRepository.findById(request.branchId())
                    .orElseThrow(() -> new OrganizationNotFoundException("Branch not found"));
            if (!branch.getCompany().getId().equals(department.getCompany().getId())) {
                throw new OrganizationConflictException("Branch must belong to the department's company");
            }
            if (!branch.isActive()) {
                throw new OrganizationConflictException("Cannot assign department to an inactive branch");
            }
        }

        Department parent = null;
        if (request.parentDepartmentId() != null) {
            if (request.parentDepartmentId().equals(id)) {
                throw new OrganizationConflictException("Department cannot be its own parent");
            }
            parent = getEntity(request.parentDepartmentId());
            if (!parent.getCompany().getId().equals(department.getCompany().getId())) {
                throw new OrganizationConflictException("Parent department must belong to the same company");
            }
            ensureNoCycle(department, parent);
        }

        department.setName(request.name().trim());
        department.setBranch(branch);
        department.setParentDepartment(parent);
        department.setActive(request.active());

        return OrganizationDto.DepartmentResponse.from(department);
    }

    @PreAuthorize("hasAuthority('platform.department:delete')")
    @Transactional
    public void deactivate(UUID id, long expectedVersion) {
        Department department = getEntity(id);
        checkVersion(department.getVersion(), expectedVersion);
        department.setActive(false);
    }

    private void ensureNoCycle(Department current, Department proposedParent) {
        Department cursor = proposedParent;
        while (cursor != null) {
            if (cursor.getId().equals(current.getId())) {
                throw new OrganizationConflictException("Department hierarchy cannot contain a cycle");
            }
            cursor = cursor.getParentDepartment();
        }
    }

    private Department getEntity(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException("Department not found"));
    }

    private static void checkVersion(long actual, long expected) {
        if (actual != expected) {
            throw new OrganizationConflictException("Department was changed by another request; reload and retry");
        }
    }
}
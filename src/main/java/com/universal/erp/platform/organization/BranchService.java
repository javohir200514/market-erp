package com.universal.erp.platform.organization;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

/** Application service for branches. Company ownership is always resolved server-side. */
@Service
@Validated
public class BranchService {
    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;

    public BranchService(BranchRepository branchRepository, CompanyRepository companyRepository) {
        this.branchRepository = branchRepository;
        this.companyRepository = companyRepository;
    }

    @PreAuthorize("hasAuthority('platform.branch:create')")
    @Transactional
    public OrganizationDto.BranchResponse create(@Valid OrganizationDto.BranchCreate request) {
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new OrganizationNotFoundException("Company not found"));
        if (!company.isActive()) {
            throw new OrganizationConflictException("Cannot add a branch to an inactive company");
        }

        String code = request.code().trim().toUpperCase(java.util.Locale.ROOT);
        if (branchRepository.existsByCodeIgnoreCase(code)) {
            throw new OrganizationConflictException("Branch code is already in use in this tenant");
        }

        Branch branch = new Branch();
        branch.setCompany(company);
        branch.setCode(code);
        branch.setName(request.name().trim());
        branch.setAddress(trimToNull(request.address()));
        branch.setCity(trimToNull(request.city()));
        branch.setPhone(trimToNull(request.phone()));

        return OrganizationDto.BranchResponse.from(branchRepository.save(branch));
    }

    @PreAuthorize("hasAuthority('platform.branch:read')")
    @Transactional(readOnly = true)
    public OrganizationDto.BranchResponse getById(UUID id) {
        return OrganizationDto.BranchResponse.from(getEntity(id));
    }

    @PreAuthorize("hasAuthority('platform.branch:read')")
    @Transactional(readOnly = true)
    public List<OrganizationDto.BranchResponse> getByCompany(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new OrganizationNotFoundException("Company not found");
        }
        return branchRepository.findByCompanyIdOrderByNameAsc(companyId).stream()
                .map(OrganizationDto.BranchResponse::from).toList();
    }

    @PreAuthorize("hasAuthority('platform.branch:update')")
    @Transactional
    public OrganizationDto.BranchResponse update(UUID id, @Valid OrganizationDto.BranchUpdate request) {
        Branch branch = getEntity(id);
        checkVersion(branch.getVersion(), request.version());

        branch.setName(request.name().trim());
        branch.setAddress(trimToNull(request.address()));
        branch.setCity(trimToNull(request.city()));
        branch.setPhone(trimToNull(request.phone()));
        branch.setActive(request.active());

        return OrganizationDto.BranchResponse.from(branch);
    }

    @PreAuthorize("hasAuthority('platform.branch:delete')")
    @Transactional
    public void deactivate(UUID id, long expectedVersion) {
        Branch branch = getEntity(id);
        checkVersion(branch.getVersion(), expectedVersion);
        branch.setActive(false);
    }

    private Branch getEntity(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException("Branch not found"));
    }

    private static void checkVersion(long actual, long expected) {
        if (actual != expected) {
            throw new OrganizationConflictException("Branch was changed by another request; reload and retry");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
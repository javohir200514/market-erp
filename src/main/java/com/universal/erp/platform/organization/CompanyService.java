package com.universal.erp.platform.organization;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/** Application service for tenant-scoped legal entities. */
@Service
@Validated
public class CompanyService {
    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @PreAuthorize("hasAuthority('platform.company:create')")
    @Transactional
    public OrganizationDto.CompanyResponse create(@Valid OrganizationDto.CompanyCreate request) {
        String code = normalizeCode(request.code());
        if (companyRepository.existsByCodeIgnoreCase(code)) {
            throw new OrganizationConflictException("Company code is already in use");
        }

        Company company = new Company();
        company.setCode(code);
        company.setLegalName(request.legalName().trim());
        company.setDisplayName(request.displayName().trim());
        company.setTaxId(trimToNull(request.taxId()));
        company.setCountryCode(upperOrNull(request.countryCode()));
        company.setBaseCurrency(request.baseCurrency().trim().toUpperCase(java.util.Locale.ROOT));

        if (request.parentCompanyId() != null) {
            Company parent = getEntity(request.parentCompanyId());
            if (!parent.isActive()) {
                throw new OrganizationConflictException("Parent company is inactive");
            }
            company.setParentCompany(parent);
        }

        return OrganizationDto.CompanyResponse.from(companyRepository.save(company));
    }

    @PreAuthorize("hasAuthority('platform.company:read')")
    @Transactional(readOnly = true)
    public OrganizationDto.CompanyResponse getById(UUID id) {
        return OrganizationDto.CompanyResponse.from(getEntity(id));
    }

    @PreAuthorize("hasAuthority('platform.company:read')")
    @Transactional(readOnly = true)
    public Page<OrganizationDto.CompanyResponse> search(String query, Pageable pageable) {
        String q = query == null ? "" : query.trim();
        return companyRepository.search(q, pageable).map(OrganizationDto.CompanyResponse::from);
    }

    @PreAuthorize("hasAuthority('platform.company:update')")
    @Transactional
    public OrganizationDto.CompanyResponse update(UUID id, @Valid OrganizationDto.CompanyUpdate request) {
        Company company = getEntity(id);
        checkVersion(company.getVersion(), request.version());

        company.setLegalName(request.legalName().trim());
        company.setDisplayName(request.displayName().trim());
        company.setTaxId(trimToNull(request.taxId()));
        company.setCountryCode(upperOrNull(request.countryCode()));
        company.setActive(request.active());

        return OrganizationDto.CompanyResponse.from(company);
    }

    @PreAuthorize("hasAuthority('platform.company:delete')")
    @Transactional
    public void deactivate(UUID id, long expectedVersion) {
        Company company = getEntity(id);
        checkVersion(company.getVersion(), expectedVersion);
        company.setActive(false);
    }

    private Company getEntity(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new OrganizationNotFoundException("Company not found"));
    }

    private static void checkVersion(long actual, long expected) {
        if (actual != expected) {
            throw new OrganizationConflictException("Company was changed by another request; reload and retry");
        }
    }

    private static String normalizeCode(String value) {
        return value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String upperOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
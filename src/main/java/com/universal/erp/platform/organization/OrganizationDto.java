package com.universal.erp.platform.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** DTOs for Company, Branch and Department. tenantId is intentionally never accepted from clients. */
public final class OrganizationDto {

    private OrganizationDto() {
    }

    // ---- Company ----

    public record CompanyCreate(
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 250) String legalName,
            @NotBlank @Size(max = 200) String displayName,
            @Size(max = 50) String taxId,
            @Pattern(regexp = "[A-Z]{2}") String countryCode,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String baseCurrency,
            UUID parentCompanyId) {
    }

    public record CompanyUpdate(
            @NotBlank @Size(max = 250) String legalName,
            @NotBlank @Size(max = 200) String displayName,
            @Size(max = 50) String taxId,
            @Pattern(regexp = "[A-Z]{2}") String countryCode,
            boolean active,
            long version) {
    }

    public record CompanyResponse(
            UUID id, String code, String legalName, String displayName, String taxId,
            String countryCode, String baseCurrency, UUID parentCompanyId, boolean active, long version) {

        public static CompanyResponse from(Company c) {
            return new CompanyResponse(c.getId(), c.getCode(), c.getLegalName(), c.getDisplayName(),
                    c.getTaxId(), c.getCountryCode(), c.getBaseCurrency(),
                    c.getParentCompany() == null ? null : c.getParentCompany().getId(),
                    c.isActive(), c.getVersion());
        }
    }

    // ---- Branch ----

    public record BranchCreate(
            @NotNull UUID companyId,
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 300) String address,
            @Size(max = 100) String city,
            @Size(max = 50) String phone) {
    }

    public record BranchUpdate(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 300) String address,
            @Size(max = 100) String city,
            @Size(max = 50) String phone,
            boolean active,
            long version) {
    }

    public record BranchResponse(
            UUID id, UUID companyId, String code, String name, String address,
            String city, String phone, boolean active, long version) {

        public static BranchResponse from(Branch b) {
            return new BranchResponse(b.getId(), b.getCompany().getId(), b.getCode(), b.getName(),
                    b.getAddress(), b.getCity(), b.getPhone(), b.isActive(), b.getVersion());
        }
    }

    // ---- Department ----

    public record DepartmentCreate(
            @NotNull UUID companyId,
            UUID branchId,
            UUID parentDepartmentId,
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 200) String name) {
    }

    public record DepartmentUpdate(
            @NotBlank @Size(max = 200) String name,
            UUID branchId,
            UUID parentDepartmentId,
            boolean active,
            long version) {
    }

    public record DepartmentResponse(
            UUID id, UUID companyId, UUID branchId, UUID parentDepartmentId,
            String code, String name, boolean active, long version) {

        public static DepartmentResponse from(Department d) {
            return new DepartmentResponse(d.getId(), d.getCompany().getId(),
                    d.getBranch() == null ? null : d.getBranch().getId(),
                    d.getParentDepartment() == null ? null : d.getParentDepartment().getId(),
                    d.getCode(), d.getName(), d.isActive(), d.getVersion());
        }
    }
}

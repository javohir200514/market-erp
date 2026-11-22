package com.universal.erp.platform.organization;

import com.universal.erp.shared.common.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A legal entity inside a tenant. Companies can form a hierarchy (holding -> subsidiary). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "companies")
public class Company extends TenantAwareEntity {

    @Column(nullable = false, length = 30)
    private String code;

    @Column(name = "legal_name", nullable = false, length = 250)
    private String legalName;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    /** STIR / INN / VAT number etc. */
    @Column(name = "tax_id", length = 50)
    private String taxId;

    /** ISO 3166-1 alpha-2. */
    @Column(name = "country_code", length = 2)
    private String countryCode;

    /** ISO 4217 base (functional) currency of the company's books. */
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency = "UZS";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_company_id")
    private Company parentCompany;

    @Column(nullable = false)
    private boolean active = true;
}

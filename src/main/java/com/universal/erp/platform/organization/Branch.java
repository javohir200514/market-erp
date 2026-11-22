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

/** A physical or logical location (office, shop, warehouse site) belonging to one company. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "branches")
public class Branch extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 300)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;
}

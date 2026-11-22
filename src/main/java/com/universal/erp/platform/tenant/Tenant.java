
package com.universal.erp.platform.tenant;

import com.universal.erp.shared.common.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tenants",
    indexes = {
        @Index(
            name = "idx_tenant_code",
            columnList = "code"
        ),
        @Index(
            name = "idx_tenant_slug",
            columnList = "slug"
        ),
        @Index(
            name = "idx_tenant_status",
            columnList = "status"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Tenant extends BaseEntity {

    @NotBlank
    @Size(max = 100)
    @Column(
        name = "name",
        nullable = false,
        length = 100
    )
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(
        name = "code",
        nullable = false,
        unique = true,
        length = 100
    )
    private String code;

    @NotBlank
    @Size(max = 100)
    @Column(
        name = "slug",
        nullable = false,
        unique = true,
        length = 100
    )
    private String slug;

    @Email
    @Size(max = 255)
    @Column(name = "email", length = 255)
    private String email;

    @Size(max = 30)
    @Column(name = "phone", length = 30)
    private String phone;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 30
    )
    private TenantStatus status = TenantStatus.PENDING;

    @Size(max = 10)
    @Column(name = "default_locale", length = 10)
    private String defaultLocale = "uz";

    @Size(max = 50)
    @Column(name = "default_time_zone", length = 50)
    private String defaultTimeZone = "Asia/Tashkent";

    @Size(max = 3)
    @Column(name = "default_currency", length = 3)
    private String defaultCurrency = "UZS";

    public void activate() {
        this.status = TenantStatus.ACTIVE;
    }

    public void suspend() {
        this.status = TenantStatus.SUSPENDED;
    }

    public void disable() {
        this.status = TenantStatus.DISABLED;
    }

    public void cancel() {
        this.status = TenantStatus.CANCELLED;
    }

    public boolean isActive() {
        return this.status == TenantStatus.ACTIVE;
    }
}

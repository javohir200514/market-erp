package com.universal.erp.platform.security;

import java.io.Serializable;
import java.util.UUID;

/** A permission granted only within one company or one branch. */
public record ScopedPermission(String permissionCode, UUID companyId, UUID branchId) implements Serializable {
}
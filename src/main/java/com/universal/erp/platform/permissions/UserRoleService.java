package com.universal.erp.platform.permissions;

import com.universal.erp.platform.organization.Branch;
import com.universal.erp.platform.organization.BranchRepository;
import com.universal.erp.platform.organization.Company;
import com.universal.erp.platform.organization.CompanyRepository;
import com.universal.erp.platform.security.GrantGuard;
import com.universal.erp.platform.user.exps.OrganizationReferenceNotFoundException;
import com.universal.erp.platform.user.entity.User;
import com.universal.erp.platform.user.exps.UserNotFoundException;
import com.universal.erp.platform.user.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Assigns roles to users. Authorization is enforced HERE (Vaadin views call this service directly).
 *
 * Anti-escalation: a caller may grant or revoke a role only if they hold ALL of its permissions
 * tenant-wide. Without this, anyone with "platform.user:update" could make themselves TENANT_ADMIN,
 * or strip the administrator role from an administrator.
 */
@Service
@Transactional
public class UserRoleService {
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final GrantGuard grantGuard;

    public UserRoleService(UserRoleRepository userRoleRepository, UserRepository userRepository,
                           RoleRepository roleRepository, CompanyRepository companyRepository,
                           BranchRepository branchRepository, GrantGuard grantGuard) {
        this.userRoleRepository = userRoleRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.companyRepository = companyRepository;
        this.branchRepository = branchRepository;
        this.grantGuard = grantGuard;
    }

    @PreAuthorize("hasAuthority('platform.user:read')")
    @Transactional(readOnly = true)
    public List<UserRoleDto.Response> listForUser(UUID userId) {
        requireUser(userId);
        return userRoleRepository.findAllByUserId(userId).stream()
                .map(UserRoleDto.Response::from).toList();
    }

    @PreAuthorize("hasAuthority('platform.user:update')")
    public UserRoleDto.Response grant(UUID userId, UserRoleDto.Grant request) {
        User user = requireUser(userId);
        Role role = roleRepository.findWithPermissionsById(request.roleId())
                .orElseThrow(() -> new RoleNotFoundException("Role topilmadi: " + request.roleId()));
        grantGuard.assertCanGrant(role.getPermissions().stream().map(Permission::getCode).toList());

        Company company = null;
        Branch branch = null;
        if (request.companyId() != null) {
            company = companyRepository.findById(request.companyId())
                    .orElseThrow(() -> new OrganizationReferenceNotFoundException("Company topilmadi"));
            if (!company.isActive()) throw new OrganizationReferenceNotFoundException("Company faol emas");
        }
        if (request.branchId() != null) {
            branch = branchRepository.findById(request.branchId())
                    .orElseThrow(() -> new OrganizationReferenceNotFoundException("Branch topilmadi"));
            if (!branch.isActive()) throw new OrganizationReferenceNotFoundException("Branch faol emas");
            if (company == null || !branch.getCompany().getId().equals(company.getId())) {
                throw new OrganizationReferenceNotFoundException("Branch tanlangan company tarkibida emas");
            }
        }
        UserRole grant = new UserRole();
        grant.setUser(user);
        grant.setRole(role);
        grant.setCompany(company);
        grant.setBranch(branch);
        return UserRoleDto.Response.from(userRoleRepository.save(grant));
    }

    @PreAuthorize("hasAuthority('platform.user:update')")
    public void revoke(UUID userId, UUID userRoleId) {
        requireUser(userId);
        UserRole grant = userRoleRepository.findById(userRoleId)
                .orElseThrow(() -> new UserRoleNotFoundException("Role biriktirishi topilmadi"));
        if (!grant.getUser().getId().equals(userId)) {
            throw new UserRoleNotFoundException("Role biriktirishi ushbu foydalanuvchiga tegishli emas");
        }
        grantGuard.assertCanGrant(grant.getRole().getPermissions().stream().map(Permission::getCode).toList());
        userRoleRepository.delete(grant);
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Foydalanuvchi topilmadi: " + id));
    }
}
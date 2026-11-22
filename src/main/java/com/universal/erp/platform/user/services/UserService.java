package com.universal.erp.platform.user.services;

import com.universal.erp.platform.organization.Branch;
import com.universal.erp.platform.organization.BranchRepository;
import com.universal.erp.platform.organization.Company;
import com.universal.erp.platform.organization.CompanyRepository;
import com.universal.erp.platform.permissions.Permission;
import com.universal.erp.platform.permissions.Role;
import com.universal.erp.platform.permissions.RoleNotFoundException;
import com.universal.erp.platform.permissions.RoleRepository;
import com.universal.erp.platform.permissions.UserRole;
import com.universal.erp.platform.permissions.UserRoleRepository;
import com.universal.erp.platform.security.CurrentUser;
import com.universal.erp.platform.security.GrantGuard;
import com.universal.erp.platform.user.dto.UserDto;
import com.universal.erp.platform.user.entity.User;
import com.universal.erp.platform.user.enums.UserStatus;
import com.universal.erp.platform.user.exps.InvalidCredentialsException;
import com.universal.erp.platform.user.exps.OrganizationReferenceNotFoundException;
import com.universal.erp.platform.user.exps.UserConflictException;
import com.universal.erp.platform.user.exps.UserNotFoundException;
import com.universal.erp.platform.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final GrantGuard grantGuard;
    private final CurrentUser currentUser;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository, CompanyRepository companyRepository,
                       BranchRepository branchRepository, PasswordEncoder passwordEncoder,
                       GrantGuard grantGuard, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.companyRepository = companyRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
        this.grantGuard = grantGuard;
        this.currentUser = currentUser;
    }

    @PreAuthorize("hasAuthority('platform.user:create')")
    public UserDto.Response invite(UserDto.Invite request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new UserConflictException("Bu email bilan foydalanuvchi mavjud");
        }

        User user = new User();
        user.setEmail(email);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(normalize(request.phone()));
        user.setStatus(UserStatus.INVITED);
        user = userRepository.save(user);

        for (UserDto.RoleAssignment assignment : request.roles()) {
            grantRole(user, assignment);
        }
        return UserDto.Response.from(user);
    }

    @PreAuthorize("hasAuthority('platform.user:read')")
    @Transactional(readOnly = true)
    public UserDto.Response get(UUID userId) {
        return UserDto.Response.from(requireUser(userId));
    }

    @PreAuthorize("hasAuthority('platform.user:read')")
    @Transactional(readOnly = true)
    public Page<UserDto.Response> search(String query, Pageable pageable) {
        String q = query == null ? "" : query.trim();
        return userRepository.search(q, pageable).map(UserDto.Response::from);
    }

    @PreAuthorize("hasAuthority('platform.user:update') or @currentUser.isSelf(#userId)")
    public UserDto.Response updateProfile(UUID userId, UserDto.UpdateProfile request) {
        User user = requireUser(userId);
        checkVersion(user.getVersion(), request.version());
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(normalize(request.phone()));
        user.setLocale(request.locale().trim());
        user.setTimeZone(request.timeZone().trim());
        return UserDto.Response.from(userRepository.save(user));
    }

    @PreAuthorize("hasAuthority('platform.user:delete')")
    public UserDto.Response disable(UUID userId, long expectedVersion) {
        if (userId.equals(currentUser.userId())) {
            throw new UserConflictException("O'z hisobingizni o'chira olmaysiz");
        }
        User user = requireUser(userId);
        checkVersion(user.getVersion(), expectedVersion);
        user.setStatus(UserStatus.DISABLED);
        return UserDto.Response.from(userRepository.save(user));
    }

    /** Only the user themself. Never changes status (a SUSPENDED/DISABLED user stays so). */
    @PreAuthorize("@currentUser.isSelf(#userId)")
    public UserDto.Response changePassword(UUID userId, UserDto.ChangePassword request) {
        User user = requireUser(userId);
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Joriy parol noto'g'ri");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        return UserDto.Response.from(userRepository.save(user));
    }

    private void grantRole(User user, UserDto.RoleAssignment assignment) {
        Role role = roleRepository.findWithPermissionsById(assignment.roleId())
                .orElseThrow(() -> new RoleNotFoundException("Role topilmadi"));
        grantGuard.assertCanGrant(role.getPermissions().stream().map(Permission::getCode).toList());

        Company company = null;
        Branch branch = null;
        if (assignment.companyId() != null) {
            company = companyRepository.findById(assignment.companyId())
                    .orElseThrow(() -> new OrganizationReferenceNotFoundException("Company topilmadi"));
            if (!company.isActive()) throw new OrganizationReferenceNotFoundException("Company faol emas");
        }
        if (assignment.branchId() != null) {
            branch = branchRepository.findById(assignment.branchId())
                    .orElseThrow(() -> new OrganizationReferenceNotFoundException("Branch topilmadi"));
            if (!branch.isActive()) throw new OrganizationReferenceNotFoundException("Branch faol emas");
            if (company == null || !branch.getCompany().getId().equals(company.getId())) {
                throw new OrganizationReferenceNotFoundException("Branch tanlangan company tarkibida emas");
            }
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setCompany(company);
        userRole.setBranch(branch);
        userRoleRepository.save(userRole);
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Foydalanuvchi topilmadi"));
    }

    private static void checkVersion(long actual, long expected) {
        if (actual != expected) throw new UserConflictException("Ma'lumot eskirgan. Sahifani yangilang.");
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
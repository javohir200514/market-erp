package com.universal.erp.platform.permissions;

import com.universal.erp.platform.security.GrantGuard;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service for tenant roles. Authorization is enforced HERE (not only in controllers),
 * because Vaadin views call this service directly.
 *
 * Anti-escalation rule: a user can put a permission on a role only if they hold it tenant-wide themselves.
 */
@Service
@Transactional
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final GrantGuard grantGuard;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository,
                       UserRoleRepository userRoleRepository, GrantGuard grantGuard) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRoleRepository = userRoleRepository;
        this.grantGuard = grantGuard;
    }

    @PreAuthorize("hasAuthority('platform.role:create')")
    public RoleDto.Response create(RoleDto.Create request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (roleRepository.existsByCode(code)) {
            throw new RoleConflictException("Role kodi band: " + code);
        }
        Set<Permission> permissions = resolvePermissions(request.permissionCodes());
        grantGuard.assertCanGrant(codesOf(permissions));

        Role role = new Role();
        role.setCode(code);
        role.setName(request.name().trim());
        role.setDescription(normalize(request.description()));
        role.setSystemRole(false);
        role.setPermissions(permissions);
        return RoleDto.Response.from(roleRepository.save(role));
    }

    @PreAuthorize("hasAuthority('platform.role:read')")
    @Transactional(readOnly = true)
    public RoleDto.Response get(UUID id) {
        return RoleDto.Response.from(requireRoleWithPermissions(id));
    }

    @PreAuthorize("hasAuthority('platform.role:read')")
    @Transactional(readOnly = true)
    public List<RoleDto.Response> list() {
        return roleRepository.findAll().stream()
                .map(role -> roleRepository.findWithPermissionsById(role.getId()).orElse(role))
                .map(RoleDto.Response::from)
                .toList();
    }

    @PreAuthorize("hasAuthority('platform.role:update')")
    public RoleDto.Response update(UUID id, RoleDto.Update request) {
        Role role = requireRoleWithPermissions(id);
        if (role.isSystemRole()) {
            throw new RoleConflictException("System role'ni tahrirlash mumkin emas");
        }
        if (role.getVersion() != request.version()) {
            throw new RoleConflictException("Ma'lumot eskirgan. Sahifani yangilang.");
        }

        Set<Permission> after = resolvePermissions(request.permissionCodes());
        Set<String> added = codesOf(after);
        added.removeAll(codesOf(role.getPermissions()));
        grantGuard.assertCanGrant(added); // only NEWLY added permissions must be held by the caller

        role.setName(request.name().trim());
        role.setDescription(normalize(request.description()));
        role.setPermissions(after);
        return RoleDto.Response.from(roleRepository.save(role));
    }

    @PreAuthorize("hasAuthority('platform.role:delete')")
    public void delete(UUID id) {
        Role role = requireRoleWithPermissions(id);
        if (role.isSystemRole()) throw new RoleConflictException("System role o'chirilmaydi");
        if (userRoleRepository.existsByRoleId(id)) {
            throw new RoleConflictException("Role foydalanuvchilarga biriktirilgan; avval biriktirishlarni olib tashlang");
        }
        roleRepository.delete(role);
    }

    private Set<Permission> resolvePermissions(Set<String> codes) {
        if (codes == null || codes.isEmpty()) return new HashSet<>();
        Set<String> normalized = codes.stream().map(String::trim).collect(Collectors.toSet());
        List<Permission> found = permissionRepository.findByCodeIn(normalized);
        Set<String> foundCodes = found.stream().map(Permission::getCode).collect(Collectors.toSet());
        if (foundCodes.size() != normalized.size()) {
            Set<String> missing = new HashSet<>(normalized);
            missing.removeAll(foundCodes);
            throw new PermissionNotFoundException("Permission kodlari topilmadi: " + missing);
        }
        return new HashSet<>(found);
    }

    private static Set<String> codesOf(Set<Permission> permissions) {
        return permissions.stream().map(Permission::getCode).collect(Collectors.toCollection(HashSet::new));
    }

    private Role requireRoleWithPermissions(UUID id) {
        return roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role topilmadi: " + id));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
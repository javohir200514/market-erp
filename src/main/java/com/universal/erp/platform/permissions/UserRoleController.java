package com.universal.erp.platform.permissions;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/roles")
public class UserRoleController {
    private final UserRoleService service;
    public UserRoleController(UserRoleService service) { this.service = service; }

    @GetMapping @PreAuthorize("hasAuthority('platform.user:read')")
    public List<UserRoleDto.Response> list(@PathVariable UUID userId) {
        return service.listForUser(userId);
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('platform.user:update')")
    public UserRoleDto.Response grant(@PathVariable UUID userId,
            @Valid @RequestBody UserRoleDto.Grant body) {
        return service.grant(userId, body);
    }

    @DeleteMapping("/{userRoleId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('platform.user:update')")
    public void revoke(@PathVariable UUID userId, @PathVariable UUID userRoleId) {
        service.revoke(userId, userRoleId);
    }
}

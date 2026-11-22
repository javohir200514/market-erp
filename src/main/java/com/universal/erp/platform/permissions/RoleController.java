package com.universal.erp.platform.permissions;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {
    private final RoleService service;
    public RoleController(RoleService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('platform.role:create')")
    public RoleDto.Response create(@Valid @RequestBody RoleDto.Create body) { return service.create(body); }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('platform.role:read')")
    public RoleDto.Response get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping @PreAuthorize("hasAuthority('platform.role:read')")
    public List<RoleDto.Response> list() { return service.list(); }

    @PutMapping("/{id}") @PreAuthorize("hasAuthority('platform.role:update')")
    public RoleDto.Response update(@PathVariable UUID id, @Valid @RequestBody RoleDto.Update body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('platform.role:delete')")
    public void delete(@PathVariable UUID id) { service.delete(id); }
}

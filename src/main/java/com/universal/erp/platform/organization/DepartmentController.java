package com.universal.erp.platform.organization;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {
    private final DepartmentService service;
    public DepartmentController(DepartmentService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('platform.department:create')")
    public OrganizationDto.DepartmentResponse create(@Valid @RequestBody OrganizationDto.DepartmentCreate body) {
        return service.create(body);
    }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('platform.department:read')")
    public OrganizationDto.DepartmentResponse get(@PathVariable UUID id) { return service.getById(id); }

    @GetMapping @PreAuthorize("hasAuthority('platform.department:read')")
    public List<OrganizationDto.DepartmentResponse> byCompany(@RequestParam UUID companyId) {
        return service.getByCompany(companyId);
    }

    @PutMapping("/{id}") @PreAuthorize("hasAuthority('platform.department:update')")
    public OrganizationDto.DepartmentResponse update(@PathVariable UUID id,
            @Valid @RequestBody OrganizationDto.DepartmentUpdate body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('platform.department:delete')")
    public void deactivate(@PathVariable UUID id, @RequestParam long version) {
        service.deactivate(id, version);
    }
}

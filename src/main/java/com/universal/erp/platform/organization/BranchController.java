package com.universal.erp.platform.organization;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {
    private final BranchService service;
    public BranchController(BranchService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('platform.branch:create')")
    public OrganizationDto.BranchResponse create(@Valid @RequestBody OrganizationDto.BranchCreate body) {
        return service.create(body);
    }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('platform.branch:read')")
    public OrganizationDto.BranchResponse get(@PathVariable UUID id) { return service.getById(id); }

    @GetMapping @PreAuthorize("hasAuthority('platform.branch:read')")
    public List<OrganizationDto.BranchResponse> byCompany(@RequestParam UUID companyId) {
        return service.getByCompany(companyId);
    }

    @PutMapping("/{id}") @PreAuthorize("hasAuthority('platform.branch:update')")
    public OrganizationDto.BranchResponse update(@PathVariable UUID id,
            @Valid @RequestBody OrganizationDto.BranchUpdate body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('platform.branch:delete')")
    public void deactivate(@PathVariable UUID id, @RequestParam long version) {
        service.deactivate(id, version);
    }
}

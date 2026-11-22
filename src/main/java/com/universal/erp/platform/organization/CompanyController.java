package com.universal.erp.platform.organization;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {
    private final CompanyService service;
    public CompanyController(CompanyService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('platform.company:create')")
    public OrganizationDto.CompanyResponse create(@Valid @RequestBody OrganizationDto.CompanyCreate body) {
        return service.create(body);
    }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('platform.company:read')")
    public OrganizationDto.CompanyResponse get(@PathVariable UUID id) { return service.getById(id); }

    @GetMapping @PreAuthorize("hasAuthority('platform.company:read')")
    public Page<OrganizationDto.CompanyResponse> search(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "displayName") Pageable pageable) {
        return service.search(q, pageable);
    }

    @PutMapping("/{id}") @PreAuthorize("hasAuthority('platform.company:update')")
    public OrganizationDto.CompanyResponse update(@PathVariable UUID id,
            @Valid @RequestBody OrganizationDto.CompanyUpdate body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('platform.company:delete')")
    public void deactivate(@PathVariable UUID id, @RequestParam long version) {
        service.deactivate(id, version);
    }
}

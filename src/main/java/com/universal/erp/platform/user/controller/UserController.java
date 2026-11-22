package com.universal.erp.platform.user.controller;

import com.universal.erp.platform.user.services.UserService;
import com.universal.erp.platform.user.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('platform.user:create')")
    public UserDto.Response invite(@Valid @RequestBody UserDto.Invite body) { return service.invite(body); }

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('platform.user:read')")
    public UserDto.Response get(@PathVariable UUID id) { return service.get(id); }

    @GetMapping @PreAuthorize("hasAuthority('platform.user:read')")
    public Page<UserDto.Response> search(@RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return service.search(q, pageable);
    }

    @PutMapping("/{id}/profile") @PreAuthorize("hasAuthority('platform.user:update')")
    public UserDto.Response updateProfile(@PathVariable UUID id,
            @Valid @RequestBody UserDto.UpdateProfile body) {
        return service.updateProfile(id, body);
    }

    @PostMapping("/{id}/disable") @PreAuthorize("hasAuthority('platform.user:delete')")
    public UserDto.Response disable(@PathVariable UUID id, @RequestParam long version) {
        return service.disable(id, version);
    }

    @PostMapping("/{id}/change-password")
    public UserDto.Response changePassword(@PathVariable UUID id,
            @Valid @RequestBody UserDto.ChangePassword body) {
        return service.changePassword(id, body);
    }
}

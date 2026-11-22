package com.universal.erp.platform.security;

import com.universal.erp.platform.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Binds TenantContext from the AUTHENTICATED principal only - never from request data.
 * Registered inside the Spring Security chain (see SecurityConfig), not as a servlet-container bean.
 */
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof ErpUserDetails user) {
                TenantContext.setTenantId(user.getTenantId());
            } else {
                TenantContext.clear();
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
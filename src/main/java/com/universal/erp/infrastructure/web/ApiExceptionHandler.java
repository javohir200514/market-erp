package com.universal.erp.infrastructure.web;

import com.universal.erp.platform.organization.OrganizationConflictException;
import com.universal.erp.platform.organization.OrganizationNotFoundException;
import com.universal.erp.platform.permissions.PermissionNotFoundException;
import com.universal.erp.platform.permissions.RoleConflictException;
import com.universal.erp.platform.permissions.RoleNotFoundException;
import com.universal.erp.platform.permissions.UserRoleNotFoundException;
import com.universal.erp.platform.tenant.TenantAlreadyExistsException;
import com.universal.erp.platform.tenant.TenantNotFoundException;
import com.universal.erp.platform.user.exps.InvalidCredentialsException;
import com.universal.erp.platform.user.exps.OrganizationReferenceNotFoundException;
import com.universal.erp.platform.user.exps.UserConflictException;
import com.universal.erp.platform.user.exps.UserNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Consistent error responses (RFC 9457 ProblemDetail). AccessDenied is left to Spring Security (403). */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({OrganizationNotFoundException.class, UserNotFoundException.class,
            RoleNotFoundException.class, UserRoleNotFoundException.class,
            TenantNotFoundException.class, PermissionNotFoundException.class})
    ProblemDetail notFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({OrganizationConflictException.class, UserConflictException.class,
            RoleConflictException.class, TenantAlreadyExistsException.class})
    ProblemDetail conflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail stale(ObjectOptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Data was changed by someone else; reload and retry");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrity(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Data conflicts with existing records");
    }

    @ExceptionHandler({OrganizationReferenceNotFoundException.class, InvalidCredentialsException.class,
            ConstraintViolationException.class})
    ProblemDetail badRequest(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}